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
import java.util.stream.Collectors;

public class DefaultMapperImplementation implements RowMapper {
    private static final Logger log = LoggerFactory.getLogger(DefaultMapperImplementation.class);

    @Override
    public <T> T map(ResultSet rs, Class<T> clazz) {
        return singleRowMap(rs, clazz);
    }

    @Override
    public <T> T map(ResultSet rs, T instance){
        return singleRowMap(rs, instance);
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
    @Override
    public <T> List<T> mapWithRelations(ResultSet rs, Class<T> clazz) {
        Map<List<Object>, Object> madeObjects = new HashMap<>();
        //linked hash set to preserve order from DB
        Set<Object> returningObjects = new LinkedHashSet<>();
        Set<List<Object>> relationDeduplication = new HashSet<>();
        try {
            while (rs.next()) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int columnCount = rsMeta.getColumnCount();
                //constructing objects
                Map<String, Object> rowInstances = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsMeta.getColumnLabel(i);
                    List<String> path = Arrays.stream(columnName.split("\\."))
                            .collect(Collectors.toList());
                    String fieldName = path.getLast();
                    path.removeLast();
                    String joinedPath = String.join(".", path);

                    Object instance = rowInstances.get(joinedPath);
                    if (instance == null) {
                        var type = findInstanceType(path, clazz);
                        instance = type.getDeclaredConstructor().newInstance();
                        rowInstances.put(joinedPath, instance);
                    }
                    //map column in instance, handle null objects from DB
                    if (!mapSingleProperty(instance, columnName, fieldName, rs))
                        rowInstances.put(joinedPath, NullMarker.NULL);
                }
                //deduplicating objects by inserting in madeObjects map
                for (var entry : rowInstances.entrySet()) {
                    //skip null objects
                    if (entry.getValue().equals(NullMarker.NULL)) {
                        continue;
                    }
                    madeObjects.putIfAbsent(getPrimaryKey(entry.getValue()), entry.getValue());
                }
                //solve relations
                for (var freshObjectEntry : rowInstances.entrySet()) {
                    List<String> path = Arrays.stream(freshObjectEntry.getKey().split("\\.")).collect(Collectors.toList());
                    //skip mapping null objects
                    if (freshObjectEntry.getValue().equals(NullMarker.NULL)) {
                        continue;
                    }
                    //these are original object required to return
                    if (path.size() == 1) {
                        returningObjects.add(madeObjects.get(getPrimaryKey(freshObjectEntry.getValue())));
                        continue;
                    }
                    List<Object> myKey = getPrimaryKey(freshObjectEntry.getValue());
                    String relation = path.getLast();
                    path.removeLast();
                    String joinedPath = String.join(".", path);
                    List<Object> parentKey = getPrimaryKey(rowInstances.get(joinedPath));
                    Object parent = madeObjects.get(parentKey);
                    Object child = madeObjects.get(myKey);
                    //relation deduplication
                    if (relationDeduplication.add(List.of(parent, child, relation))) {
                        solveRelations(parent, child, relation);
                    }

                }

            }
            List<T> instances = new ArrayList<>();
            for (var entry : returningObjects) {
                if (clazz.isInstance(entry)) {
                    instances.add((T) entry);
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
                    List newList = new ArrayList<>();
                    newList.add(child);
                    fk.set(parent, newList);
                } else {
                    ((List) listObject).add(child);
                }
                //fill both sides if foreignRelation is present
                /*if(relation.getForeignRelationName() != null) {
                    solveRelations(child, parent, relation.getForeignRelationName());
                }*/
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
            return null;
        }


        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ClassInstantiationException(e);
        }
        return singleRowMap(rs, instance);
    }

    private <T> T singleRowMap(ResultSet rs, T instance){
        //for each column in result set, find the designated field
        // and do the conversion to java datatype
        try {
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsMeta.getColumnLabel(i).toLowerCase();
                mapSingleProperty(instance, columnName, columnName, rs);
            }

            return instance;
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        }
    }

    //maps single column value to property in instance, returns false if DB null object mapping is detected
    private boolean mapSingleProperty(Object instance, String resultSetColumnName, String columnName, ResultSet rs) {
        try {
            Class<?> clazz = instance.getClass();
            EntityMetadata entityMetadata = MetadataStorage.get(clazz);
            //check if instance is nullObject
            if (instance.equals(NullMarker.NULL))
                return true;

            ColumnMetadata columnMetadata = entityMetadata.getColumns().get(columnName);

            if (columnMetadata == null) {
                // Column doesn't belong to this entity -> skip for now
                log.warn("Column '{}' does not exist in entity '{}'; skipping.", columnName, clazz.getSimpleName());
                return true;
            }

            Field field = columnMetadata.getField();
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            Object value;
            //check if it is enum
            if(fieldType.isEnum()){
                value = enumFromString(fieldType, rs.getString(resultSetColumnName));
            }
            //convert primitive types to java wrappers for JDBC
            else
                value = rs.getObject(resultSetColumnName, javaPrimitiveTypes(fieldType));
            //check if field is PK and null -> db null object
            List<Field> pkFields = entityMetadata.getIdFields();
            if (pkFields.contains(field) && value == null) {
                //flag for null object
                return false;
            }
            //populate field
            BeanUtils.setProperty(instance, fieldName, value);
            return true;
        } catch (SQLException e) {
            throw new ResultSetAccessException(e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new TypeConversionException(e);
        }
    }

    //construct instance of enum that is given
    private <E extends Enum<E>> E enumFromString(Class<?> enumClass, String value) {
        assert enumClass.isEnum();
        if (value == null) return null;
        return Enum.valueOf((Class<E>) enumClass, value);
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

    private static final Map<Class<?>, Class<?>> primitiveTypes = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class
            );

    private Class<?> javaPrimitiveTypes(Class<?> clazz) {
        return primitiveTypes.getOrDefault(clazz, clazz);
    }

    private enum NullMarker {NULL}

    ;
}
