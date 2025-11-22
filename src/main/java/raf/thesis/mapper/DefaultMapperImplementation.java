package raf.thesis.mapper;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raf.thesis.mapper.exceptions.ClassInstantiationException;
import raf.thesis.mapper.exceptions.ResultSetAccessException;
import raf.thesis.mapper.exceptions.TypeConversionException;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.storage.MetadataStorage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class DefaultMapperImplementation implements RowMapper {
    private static final Logger log = LoggerFactory.getLogger(DefaultMapperImplementation.class);

    @Override
    public <T> T map(ResultSet rs, Class<T> clazz) {
        return singleRowMap(rs, clazz);
    }

    @Override
    public <T> List<T> mapList(ResultSet rs, Class<T> clazz) {
        List<T> instances = new ArrayList<>();
        try {
            while (rs.next()) {
                instances.add(singleRowMap(rs, clazz));
            }
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        }
        return instances;
    }

    //result set is in specific format that my query builder will make
    //for each row, i should make instances of objects by navigating through relations to find the right one
    //for each object, i need to handle duplicates by putting them in map with list of path, .class and PK as key
    public <T> List<T> mapWithRelations(ResultSet rs, Class<T> clazz) {
        Map<List<Object>, Object> madeObjects = new HashMap<>();
        try {
            while (rs.next()) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int columnCount = rsMeta.getColumnCount();
                //constructing objects
                Map<String, Object> rowInstances = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsMeta.getColumnLabel(i);
                    List<String> path = Arrays.stream(columnName.split("\\.")).toList();
                    String fieldName = path.getLast();
                    path.removeLast();
                    String joinedPath = String.join(".", path);

                    Object instance = rowInstances.get(joinedPath);
                    if (instance == null) {
                        var type = findInstanceType(path, clazz);
                        instance = type.getDeclaredConstructor().newInstance();
                        rowInstances.put(joinedPath, instance);
                    }
                    mapSingleProperty(instance, instance.getClass(), columnName, fieldName, rs, MetadataStorage.get(instance.getClass()));
                }
                //deduplicating objects by inserting in madeObjects map
                for (var entry : rowInstances.entrySet()) {
                    madeObjects.putIfAbsent(List.of(entry.getKey(), getPrimaryKey(entry.getValue())), entry.getValue());
                }
                //solve relations
                for (var freshObjectEntry : rowInstances.entrySet()) {
                    List<String> path = Arrays.stream(freshObjectEntry.getKey().split("\\.")).toList();
                    if (path.size() == 1) {
                        continue;
                    }
                    List<Object> myKey = List.of(freshObjectEntry.getKey(), getPrimaryKey(freshObjectEntry.getValue()));
                    String relation = path.getLast();
                    path.removeLast();
                    List<Object> parentKey = List.of(path, getPrimaryKey(rowInstances.get(String.join(".", path))));
                    solveRelations(madeObjects.get(parentKey), madeObjects.get(myKey), relation);
                }

            }
            List<T> instances = new ArrayList<>();
            for (var entry : madeObjects.entrySet()) {
                if (((String) entry.getKey().getFirst()).split("\\.").length == 1) {
                    instances.add((T) entry.getValue());
                }
            }
            return instances;
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new ClassInstantiationException(e);
        }
    }

    //depending on the type of relation, populate missing objects
    private void solveRelations(Object parent, Object child, String relationName) {
        Class<?> parentClass = parent.getClass();
        EntityMetadata parentMetadata = MetadataStorage.get(parentClass);
        RelationMetadata relation = parentMetadata.getRelations().stream().filter(rel -> rel.getRelationName().equals(relationName)).findFirst().orElse(null);
        if (relation == null) {
            throw new RuntimeException("Relation " + relationName + " not found");
        }
        try {
            if (relation.getRelationType() == RelationType.ONE_TO_MANY || relation.getRelationType() == RelationType.MANY_TO_MANY) {
                Field fk = relation.getForeignField();
                fk.setAccessible(true);
                Object listObject = fk.get(parent);
                if (listObject == null) {
                    fk.set(parent, List.of(child));
                } else {
                    ((List) listObject).add(child);
                }
            } else {
                Field fk = relation.getForeignField();
                fk.setAccessible(true);
                fk.set(parent, child);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error populating the relationship " + relationName, e);
        }

    }

    //traverse through path to find right class
    private Class<?> findInstanceType(List<String> path, Class<?> start) {
        Class<?> current = start;
        for (int i = 1; i < path.size(); i++) {
            EntityMetadata currMeta = MetadataStorage.get(current);
            for (var relation : currMeta.getRelations()) {
                if (relation.getRelationName().equalsIgnoreCase(path.get(i))) {
                    current = relation.getForeignClass();
                    break;
                }
            }
        }
        return current;
    }

    private <T> T singleRowMap(ResultSet rs, Class<T> clazz) {
        EntityMetadata entityMetadata = MetadataStorage.get(clazz);

        if (entityMetadata == null) {
            //TODO: map custom objects from ResultSet
            return null;
        }

        //for each column in result set, find the designated field
        // and do the conversion to java datatype
        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ClassInstantiationException(e);
        }
        try {
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsMeta.getColumnLabel(i).toLowerCase();
                mapSingleProperty(instance, clazz, columnName, columnName, rs, entityMetadata);
            }

            return instance;
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        }
    }

    private void mapSingleProperty(Object instance, Class<?> clazz, String resultSetColumnName, String columnName, ResultSet rs, EntityMetadata entityMetadata) {
        try {
            ColumnMetadata columnMetadata = entityMetadata.getColumns().get(columnName);

            if (columnMetadata == null) {
                // Column doesn't belong to this entity -> skip for now
                log.warn("Column '{}' does not exist in entity '{}'; skipping.", columnName, clazz.getSimpleName());
                return;
            }

            Field field = columnMetadata.getField();
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            //convert primitive types to java wrappers for JDBC
            Object value = rs.getObject(resultSetColumnName, javaPrimitiveTypes(fieldType));
            //populate field
            BeanUtils.setProperty(instance, fieldName, value);
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new TypeConversionException(e);
        }
    }

    private List<Object> getPrimaryKey(Object instance) {
        try {
            EntityMetadata objectMetadata = MetadataStorage.get(instance.getClass());
            List<Object> keys = new ArrayList<>();
            keys.add(instance.getClass());
            var pkFields = objectMetadata.getIdFields();
            for (var pk : pkFields) {
                pk.setAccessible(true);
                keys.add(pk.get(instance));
            }
            return keys;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exctract primary key from object " + instance.getClass(), e);
        }
    }

    private Class<?> javaPrimitiveTypes(Class<?> clazz) {
        Map<Class<?>, Class<?>> primitiveTypes = new HashMap<>();
        primitiveTypes.put(boolean.class, Boolean.class);
        primitiveTypes.put(byte.class, Byte.class);
        primitiveTypes.put(char.class, Character.class);
        primitiveTypes.put(short.class, Short.class);
        primitiveTypes.put(int.class, Integer.class);
        primitiveTypes.put(long.class, Long.class);
        primitiveTypes.put(float.class, Float.class);
        primitiveTypes.put(double.class, Double.class);
        return primitiveTypes.getOrDefault(clazz, clazz);
    }
}
