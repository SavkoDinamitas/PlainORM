package raf.thesis.query;

import lombok.AllArgsConstructor;
import org.apache.commons.beanutils.PropertyUtils;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.exceptions.EntityObjectRequiredException;
import raf.thesis.query.exceptions.IdInRelatedObjectsCantBeNullException;
import raf.thesis.query.exceptions.InvalidRelationPathException;
import raf.thesis.query.exceptions.MissingIdException;
import raf.thesis.query.tree.Literal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@AllArgsConstructor
public class DBUpdateSolver {
    public Dialect dialect;

    public PreparedStatementQuery generateInsert(Object obj) {
        List<PreparedStatementQuery> queries = new ArrayList<>();

        EntityMetadata meta = MetadataStorage.get(obj.getClass());
        if (meta == null)
            throw new EntityObjectRequiredException("Object: " + obj + " is not an entity!");
        //required arguments to generate query
        List<String> columnNames = new ArrayList<>();
        List<Literal> columnValues = new ArrayList<>();

        //helper map for detecting generated id-s
        Map<Field, Boolean> generated = new HashMap<>();
        for (int i = 0; i < meta.getIdFields().size(); i++) {
            generated.put(meta.getIdFields().get(i), meta.getGeneratedId().get(i));
        }
        //add all columns in object
        for (var col : meta.getColumns().values()) {
            //if key is generated, skip it in insert
            if (generated.containsKey(col.getField()) && generated.get(col.getField()))
                continue;
            columnNames.add(col.getColumnName());
            columnValues.add(makeLiteral(col.getField(), obj));
        }

        //solve relations
        for (var relation : meta.getRelations()) {
            Object relatedObject = extractFieldValue(relation.getForeignField(), obj);
            if (relatedObject == null)
                continue;

            if (relation.getRelationType() == RelationType.MANY_TO_ONE || (relation.getRelationType() == RelationType.ONE_TO_ONE && relation.getMySideKey())) {
                columnNames.addAll(relation.getForeignKeyNames());
                EntityMetadata relationEntity = MetadataStorage.get(relatedObject.getClass());
                getKeyValues(relationEntity, relatedObject, columnValues);
            }
        }
        String query = dialect instanceof Dialect.UsesInsertReturning d ? d.generateInsertQuery(columnNames, meta.getTableName(), extractKeys(meta)) : dialect.generateInsertQuery(columnNames, meta.getTableName());
        return new PreparedStatementQuery(query, columnValues);
    }

    public List<PreparedStatementQuery> generateManyToManyInserts(Object obj) {
        List<PreparedStatementQuery> queries = new ArrayList<>();
        EntityMetadata meta = MetadataStorage.get(obj.getClass());
        if (meta == null)
            throw new EntityObjectRequiredException("Object: " + obj + " is not an entity");

        for (var relation : meta.getRelations()) {
            Object relatedObject = extractFieldValue(relation.getForeignField(), obj);
            if (relatedObject == null)
                continue;

            if (relation.getRelationType() == RelationType.MANY_TO_MANY) {
                if (!(relatedObject instanceof List<?> relatedObjectList))
                    throw new IllegalStateException("Scanner should have already prevented this");
                List<String> cols = new ArrayList<>();
                cols.addAll(relation.getMyJoinedTableFks());
                cols.addAll(relation.getForeignKeyNames());
                for (var instance : relatedObjectList) {
                    List<Literal> colValues = new ArrayList<>();
                    //fill values for my entity, use the object got from returning keyword in original insert to cover generated id case
                    getKeyValues(meta, obj, colValues);

                    //fill values for related entity
                    EntityMetadata relationEntity = MetadataStorage.get(instance.getClass());
                    getKeyValues(relationEntity, instance, colValues);

                    queries.add(new PreparedStatementQuery(dialect.generateInsertQuery(cols, relation.getJoinedTableName()), colValues));
                }
            }
        }
        return queries;
    }

    public PreparedStatementQuery updateObject(Object object, boolean ignoreNulls) {
        EntityMetadata meta = MetadataStorage.get(object.getClass());
        if (meta == null)
            throw new EntityObjectRequiredException("Object: " + object + " is not an entity!");
        List<String> columnNames = new ArrayList<>();
        List<String> keyColumnNames = new ArrayList<>();
        List<Literal> columnValues = new ArrayList<>();
        List<Literal> keyColumnValues = new ArrayList<>();

        for (var col : meta.getColumns().values()) {
            //PK field
            if (meta.getIdFields().contains(col.getField())) {
                extractColumnNameAndValue(keyColumnNames, keyColumnValues, col, object);
            }
            //normal column
            else {
                columnNames.add(col.getColumnName());
                Literal value = makeLiteral(col.getField(), object);
                if (value instanceof Literal.NullCnst && ignoreNulls)
                    columnNames.removeLast();
                else
                    columnValues.add(value);
            }
        }
        columnValues.addAll(keyColumnValues);
        return new PreparedStatementQuery(dialect.generateUpdateQuery(columnNames, meta.getTableName(), keyColumnNames), columnValues);
    }

    public PreparedStatementQuery deleteObject(Object obj) {
        EntityMetadata meta = MetadataStorage.get(obj.getClass());
        if (meta == null)
            throw new EntityObjectRequiredException("Object: " + obj + " is not an entity!");
        List<String> keyColumnNames = new ArrayList<>();
        List<Literal> keyColumnValues = new ArrayList<>();

        for (var col : meta.getColumns().values()) {
            //PK field
            if (meta.getIdFields().contains(col.getField())) {
                extractColumnNameAndValue(keyColumnNames, keyColumnValues, col, obj);
            }
        }
        return new PreparedStatementQuery(dialect.generateDeleteQuery(keyColumnNames, meta.getTableName()), keyColumnValues);
    }

    public PreparedStatementQuery connect(Object obj1, Object obj2, String relationName) {
        EntityMetadata meta1 = MetadataStorage.get(obj1.getClass());
        EntityMetadata meta2 = MetadataStorage.get(obj2.getClass());
        if (meta1 == null)
            throw new EntityObjectRequiredException("Object: " + obj1 + " is not an entity!");
        if (meta2 == null)
            throw new EntityObjectRequiredException("Object: " + obj2 + " is not an entity!");

        Optional<RelationMetadata> relationOp = meta1.getRelations().stream().filter(x -> x.getRelationName().equals(relationName)).findFirst();
        if (relationOp.isEmpty())
            throw new InvalidRelationPathException("Given relation name doesn't exist in object: " + obj1);
        RelationMetadata rel = relationOp.get();

        //case MANY-TO-ONE and ONE-TO-ONE with containsFK = true -> update foreign key in obj1 table
        if (rel.getRelationType() == RelationType.MANY_TO_ONE || (rel.getRelationType() == RelationType.ONE_TO_ONE && rel.getMySideKey())) {
            //joined values, first ones in SET clause than ones in WHERE clause
            List<Literal> columnValues = new ArrayList<>();
            //columns to put in SET clause of UPDATE query
            List<String> columnNames = new ArrayList<>(rel.getForeignKeyNames());
            //columns to put in WHERE clause of UPDATE query
            List<String> columnKeyNames = new ArrayList<>(extractKeys(meta1));
            getKeyValues(meta2, obj2, columnValues);
            getKeyValues(meta1, obj1, columnValues);
            return new PreparedStatementQuery(dialect.generateUpdateQuery(columnNames, meta1.getTableName(), columnKeyNames), columnValues);
        }
        //case ONE-TO-MANY or ONE-TO-ONE with containsFK = false -> update foreign key in obj2 table
        if (rel.getRelationType() == RelationType.ONE_TO_MANY || rel.getRelationType() == RelationType.ONE_TO_ONE) {
            //joined values, first ones in SET clause than ones in WHERE clause
            List<Literal> columnValues = new ArrayList<>();
            //columns to put in SET clause of UPDATE query
            List<String> columnNames = new ArrayList<>(rel.getForeignKeyNames());
            //columns to put in WHERE clause of UPDATE query
            List<String> columnKeyNames = new ArrayList<>(extractKeys(meta2));
            getKeyValues(meta1, obj1, columnValues);
            getKeyValues(meta2, obj2, columnValues);
            return new PreparedStatementQuery(dialect.generateUpdateQuery(columnNames, meta2.getTableName(), columnKeyNames), columnValues);
        }
        //case MANY-TO-MANY -> insert both keys in joined table
        List<String> columnNames = new ArrayList<>(rel.getMyJoinedTableFks());
        columnNames.addAll(rel.getForeignKeyNames());
        List<Literal> columnValues = new ArrayList<>();
        getKeyValues(meta1, obj1, columnValues);
        getKeyValues(meta2, obj2, columnValues);
        return new PreparedStatementQuery(dialect.generateInsertQuery(columnNames, rel.getJoinedTableName()), columnValues);
    }

    public PreparedStatementQuery disconnect(Object obj1, Object obj2, String relationName) {
        EntityMetadata meta1 = MetadataStorage.get(obj1.getClass());
        if (meta1 == null)
            throw new EntityObjectRequiredException("Object: " + obj1 + " is not an entity!");

        Optional<RelationMetadata> relationOp = meta1.getRelations().stream().filter(x -> x.getRelationName().equals(relationName)).findFirst();
        if (relationOp.isEmpty())
            throw new InvalidRelationPathException("Given relation name" + relationName + "doesn't exist in object: " + obj1);
        RelationMetadata rel = relationOp.get();

        //case MANY-TO-ONE and ONE-TO-ONE with containsFK = true -> remove foreign key in obj1 table
        if (rel.getRelationType() == RelationType.MANY_TO_ONE || (rel.getRelationType() == RelationType.ONE_TO_ONE && rel.getMySideKey())) {
            //joined values, first ones in SET clause than ones in WHERE clause
            List<Literal> columnValues = new ArrayList<>();
            //columns to put in SET clause of UPDATE query
            List<String> columnNames = new ArrayList<>(rel.getForeignKeyNames());
            //columns to put in WHERE clause of UPDATE query
            List<String> columnKeyNames = new ArrayList<>(extractKeys(meta1));
            //fill set values to null
            for (int i = 0; i < columnNames.size(); i++) {
                columnValues.add(new Literal.NullCnst());
            }
            getKeyValues(meta1, obj1, columnValues);
            return new PreparedStatementQuery(dialect.generateUpdateQuery(columnNames, meta1.getTableName(), columnKeyNames), columnValues);
        }
        //case ONE-TO-MANY or ONT_TO_ONE with containsFK = false -> update foreign key in obj2 table
        if (rel.getRelationType() == RelationType.ONE_TO_MANY || rel.getRelationType() == RelationType.ONE_TO_ONE) {
            if (obj2 == null) {
                throw new NullPointerException("Both objects in ONE_TO_MANY relation must be given!");
            }
            EntityMetadata meta2 = MetadataStorage.get(obj2.getClass());
            //joined values, first ones in SET clause than ones in WHERE clause
            List<Literal> columnValues = new ArrayList<>();
            //columns to put in SET clause of UPDATE query
            List<String> columnNames = new ArrayList<>(rel.getForeignKeyNames());
            //columns to put in WHERE clause of UPDATE query
            List<String> columnKeyNames = new ArrayList<>(extractKeys(meta2));
            //fill set values to null
            for (int i = 0; i < columnNames.size(); i++) {
                columnValues.add(new Literal.NullCnst());
            }
            getKeyValues(meta2, obj2, columnValues);
            return new PreparedStatementQuery(dialect.generateUpdateQuery(columnNames, meta2.getTableName(), columnKeyNames), columnValues);
        }
        //case MANY-TO-MANY -> insert both keys in joined table
        if (obj2 == null) {
            throw new NullPointerException("Both objects in MANY_TO_MANY relation must be given!");
        }
        EntityMetadata meta2 = MetadataStorage.get(obj2.getClass());
        List<String> keyColumns = new ArrayList<>(rel.getMyJoinedTableFks());
        keyColumns.addAll(rel.getForeignKeyNames());
        List<Literal> keyColValues = new ArrayList<>();
        getKeyValues(meta1, obj1, keyColValues);
        getKeyValues(meta2, obj2, keyColValues);
        return new PreparedStatementQuery(dialect.generateDeleteQuery(keyColumns, rel.getJoinedTableName()), keyColValues);
    }

    //returns the list of columns that are primary keys in given entity
    private List<String> extractKeys(EntityMetadata metadata) {
        List<String> keys = new ArrayList<>();
        for (var column : metadata.getColumns().values()) {
            if (metadata.getIdFields().contains(column.getField())) {
                keys.add(column.getColumnName());
            }
        }
        return keys;
    }

    private void extractColumnNameAndValue(List<String> columnNames, List<Literal> columnValues, ColumnMetadata col, Object instance) {
        columnNames.add(col.getColumnName());
        Literal value = makeLiteral(col.getField(), instance);
        if (value instanceof Literal.NullCnst)
            throw new MissingIdException("Given object: " + instance + "has no set primary keys!");
        columnValues.add(value);
    }

    private void getKeyValues(EntityMetadata meta, Object obj, List<Literal> columnValues) {
        for (var key : meta.getIdFields()) {
            Object value = extractFieldValue(key, obj);
            if (value == null)
                throw new IdInRelatedObjectsCantBeNullException("Id fields in related object: " + obj.getClass().getName() + " is not set!");
            columnValues.add(makeLiteral(value));
        }
    }

    private Object extractFieldValue(Field field, Object instance) {
        try {
            return PropertyUtils.getProperty(instance, field.getName());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Literal makeLiteral(Field field, Object instance) {
        return makeLiteral(extractFieldValue(field, instance));
    }

    private Literal makeLiteral(Object obj) {
        return switch (obj) {
            case null -> new Literal.NullCnst();

            case Double d -> new Literal.DoubleCnst(d);
            case Float f -> new Literal.DoubleCnst(f.doubleValue());

            case Long l -> new Literal.LongCnst(l);
            case Integer i -> new Literal.LongCnst(i.longValue());
            case Short s -> new Literal.LongCnst(s.longValue());
            case Byte b -> new Literal.LongCnst(b.longValue());

            case Boolean bool -> new Literal.BoolCnst(bool);

            case String str -> new Literal.StringCnst(str);
            case Enum<?> e -> new Literal.StringCnst(e.name());

            case LocalDate date -> new Literal.DateCnst(date);
            case LocalDateTime dateTime -> new Literal.DateTimeCnst(dateTime);
            case LocalTime time -> new Literal.TimeCnst(time);

            default -> throw new IllegalArgumentException("Unsupported literal type: " + obj.getClass());
        };
    }
}
