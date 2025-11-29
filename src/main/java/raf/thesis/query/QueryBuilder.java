package raf.thesis.query;


import lombok.NoArgsConstructor;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.exceptions.InvalidRelationPathException;
import raf.thesis.query.tree.JoinNode;
import raf.thesis.query.tree.SelectNode;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
public class QueryBuilder {
    private SelectNode rootSelectNode;
    /**
     * Set root object of query builder to specify type that is returned
     * @param object .class of entity a query should return
     * @return updated query builder
     */
    public QueryBuilder select(Class<?> object){
        rootSelectNode = new SelectNode(object);
        return this;
    }

    /**
     * Add distinct keyword in query
     * @return updated query builder
     */
    public QueryBuilder distinct(){
        return this;
    }

    /**
     * Specify which relations should be populated in returning objects (inner join by default)
     * @param relationPath dot separated relation path from root entity
     * @return updated query builder
     */
    public QueryBuilder join(String relationPath){
        rootSelectNode.addJoinNode(generateJoinNode(rootSelectNode.getRoot(), relationPath, INNER));
        return this;
    }

    /**
     * Specify which relations should be populated in returning objects
     * @param relationPath dot separated relation path from root entity
     * @param join determine type of join, LEFT, INNER, RIGHT or FULL
     * @return updated query builder
     */
    public QueryBuilder join(String relationPath, Join join){
        rootSelectNode.addJoinNode(generateJoinNode(rootSelectNode.getRoot(), relationPath, join));
        return this;
    }

    /**
     * Specify where clause for query
     * @param condition condition inside where clause
     * @return updated query builder
     */
    public QueryBuilder where(ConditionBuilder condition){
        return this;
    }

    /**
     * Specify having clause for query
     * @param condition condition inside having clause
     * @return updated query builder
     */
    public QueryBuilder having(ConditionBuilder condition){
        return this;
    }

    /**
     * Specify groupBy clause for query
     * @param fieldPath dot separated relation path to field
     * @return updated query builder
     */
    public QueryBuilder groupBy(String... fieldPath){
        return this;
    }

    /**
     * Specify orderBy clause for query
     * @param fieldPath dot separated relation path to field
     * @return updated query builder
     */
    public QueryBuilder orderBy(String... fieldPath){
        return this;
    }

    /**
     * Specify limit of number of rows that should be returned
     * @param limit maximum number of rows
     * @return updated query builder
     */
    public QueryBuilder limit(int limit){
        return this;
    }

    /**
     * Specify from which row should DB return the result
     * @param offset offset from start
     * @return updated query builder
     */
    public QueryBuilder offset(int offset){
        return this;
    }

    /**
     * Generate SQL query from builder
     * @return built SQL query
     */
    public String build(){
        return "";
    }

    private JoinNode generateJoinNode(Class<?> root, String joiningRelationPath, Join joinType){
        //fill joining table fields
        Class<?> joiningClass = findInstanceType(joiningRelationPath, root);
        EntityMetadata joiningMetadata = MetadataStorage.get(joiningClass);
        String tableName = joiningMetadata.getTableName();
        List<String> joiningTablePk = extractKeys(joiningMetadata);

        //fill foreign table fields
        String foreignTableAlias = joiningRelationPath.substring(0, joiningRelationPath.lastIndexOf("."));
        Class<?> foreignClass = findInstanceType(foreignTableAlias, root);
        EntityMetadata foreignMetadata = MetadataStorage.get(foreignClass);
        List<String> foreignTableFk = extractKeys(foreignMetadata);
        return new JoinNode(joinType, tableName, joiningRelationPath, joiningTablePk, foreignTableAlias, foreignTableFk);
    }

    //traverse through path to find right class
    private Class<?> findInstanceType(String path, Class<?> start) {
        Class<?> current = start;
        for (String s : path.split("\\.")) {
            EntityMetadata currMeta = MetadataStorage.get(current);
            boolean found = false;
            for (var relation : currMeta.getRelations()) {
                if (relation.getRelationName().equalsIgnoreCase(s)) {
                    current = relation.getForeignClass();
                    found = true;
                    break;
                }
            }
            if(!found)
                throw new InvalidRelationPathException("Invalid relation path: " + path);
        }
        return current;
    }

    //returns the list of columns that are primary keys in given entity
    private List<String> extractKeys(EntityMetadata metadata){
        List<String> keys = new ArrayList<>();
        for(var column : metadata.getColumns().values()){
            if(metadata.getIdFields().contains(column.getField())){
                keys.add(column.getColumnName());
            }
        }
        return keys;
    }

    public final Join LEFT = Join.LEFT;
    public final Join INNER = Join.INNER;
    public final Join RIGHT = Join.RIGHT;
    public final Join FULL = Join.FULL;
}
