package raf.thesis.query;

import lombok.AllArgsConstructor;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.ANSISQLDialect;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.exceptions.EntityObjectRequiredForInsertionException;
import raf.thesis.query.exceptions.IdInRelatedObjectsCantBeNullException;
import raf.thesis.query.tree.Literal;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class InsertSolver {
    public Dialect dialect;

    public PreparedStatementQuery generateInsert(Object obj) {
        List<PreparedStatementQuery> queries = new ArrayList<>();

        EntityMetadata meta = MetadataStorage.get(obj.getClass());
        if (meta == null)
            throw new EntityObjectRequiredForInsertionException("Given object: " + obj.getClass().getName() + " is not an entity");
        //required arguments to generate query
        List<String> columnNames = new ArrayList<>();
        List<Literal> columnValues = new ArrayList<>();

        //helper map for detecting generated id-s
        Map<Field, Boolean> generated = new HashMap<>();
        for(int i = 0; i < meta.getIdFields().size(); i++){
            generated.put(meta.getIdFields().get(i), meta.getGeneratedId().get(i));
        }
        //add all columns in object
        for (var col : meta.getColumns().values()) {
            //if key is generated, skip it in insert
            if(generated.containsKey(col.getField()) && generated.get(col.getField()))
                continue;
            try {
                columnNames.add(col.getColumnName());
                columnValues.add(makeLiteral(col.getField().get(obj)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        //solve relations
        for (var relation : meta.getRelations()) {
            Object relatedObject = null;
            try {
                relatedObject = relation.getForeignField().get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if(relatedObject == null)
                continue;


            if (relation.getRelationType() == RelationType.MANY_TO_ONE || relation.getRelationType() == RelationType.ONE_TO_ONE) {
                columnNames.addAll(relation.getForeignKeyNames());
                EntityMetadata relationEntity = MetadataStorage.get(relatedObject.getClass());
                getKeyValues(relationEntity, relatedObject, columnValues);
            }
        }

        return new PreparedStatementQuery(dialect.generateInsertClause(columnNames, meta.getTableName()), columnValues);
    }

    public List<PreparedStatementQuery> generateManyToManyInserts(Object obj, Object returnedKeys) {
        List<PreparedStatementQuery> queries = new ArrayList<>();
        EntityMetadata meta = MetadataStorage.get(obj.getClass());
        if (meta == null)
            throw new EntityObjectRequiredForInsertionException("Given object: " + obj.getClass().getName() + " is not an entity");

        for (var relation : meta.getRelations()) {
            Object relatedObject = null;
            try {
                relatedObject = relation.getForeignField().get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (relatedObject == null)
                continue;
            if(!(relatedObject instanceof List<?> relatedObjectList))
                throw new IllegalStateException("Scanner should have already prevented this");


            if (relation.getRelationType() == RelationType.MANY_TO_MANY) {
                List<String> cols = new ArrayList<>();
                cols.addAll(relation.getMyJoinedTableFks());
                cols.addAll(relation.getForeignKeyNames());
                for(var instance : relatedObjectList){
                    List<Literal> colValues = new ArrayList<>();
                    //fill values for my entity
                    getKeyValues(meta, returnedKeys, colValues);

                    //fill values for related entity
                    EntityMetadata relationEntity = MetadataStorage.get(instance.getClass());
                    getKeyValues(relationEntity, instance, colValues);

                    queries.add(new PreparedStatementQuery(dialect.generateInsertClause(cols, relation.getJoinedTableName()), colValues));
                }
            }
        }
        return queries;
    }

    private void getKeyValues(EntityMetadata meta, Object obj, List<Literal> columnValues) {
        for(var key : meta.getIdFields()){
            Object value = null;
            try {
                value = key.get(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if(value == null)
                throw new IdInRelatedObjectsCantBeNullException("Id fields in related object: " + obj.getClass().getName() + " is not set!");
            columnValues.add(makeLiteral(value));
        }
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

            case LocalDate date -> new Literal.DateCnst(date);
            case LocalDateTime dateTime -> new Literal.DateTimeCnst(dateTime);
            case LocalTime time -> new Literal.TimeCnst(time);

            default -> throw new IllegalArgumentException("Unsupported literal type: " + obj.getClass());
        };
    }
}
