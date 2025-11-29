package raf.thesis.query;

import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.ANSISQLDialect;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.exceptions.InvalidRelationPathException;
import raf.thesis.query.tree.*;

import java.util.*;
import java.util.stream.Stream;


public class QueryBuilder {
    protected final SelectNode rootSelectNode;
    private final Map<String, String> joinTableAliases = new HashMap<>();
    protected final String baseAlias = "%root";
    protected final Dialect dialect = new ANSISQLDialect();
    /**
     * Set root object of query builder to specify type that is returned
     * @param object .class of entity a query should return
     * @return new query builder instance with set root
     */
    public static QueryBuilder select(Class<?> object){
        SelectNode sn = new SelectNode(object);
        return new QueryBuilder(sn);
    }

    /**
     * Special constructor for SubQuery builder, do not use for regular query building
     * @param object root table for subquery builder
     * @param columns columns subquery should return
     * @return new instance of subquery builder
     */
    public static SubQueryBuilder select(Class<?> object, List<String> columns){
        return new SubQueryBuilder(object, columns);
    }

    protected QueryBuilder(SelectNode rootSelectNode){
        this.rootSelectNode = rootSelectNode;
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
     * @param expression expression inside where clause
     * @return updated query builder
     */
    public QueryBuilder where(Expression expression){
        rootSelectNode.setWhereNode(new WhereNode(expression));
        return this;
    }

    /**
     * Specify having clause for query
     * @param expression expression inside having clause
     * @return updated query builder
     */
    public QueryBuilder having(Expression expression){
        rootSelectNode.setHavingNode(new HavingNode(expression));
        return this;
    }

    /**
     * Specify groupBy clause for query
     * @param e1 first expression
     * @param expressions other expressions
     * @return updated query builder
     */
    public QueryBuilder groupBy(Expression e1, Expression... expressions){
        rootSelectNode.setGroupByNode(new GroupByNode(Stream.concat(Stream.of(e1), Stream.of(expressions)).toList()));
        return this;
    }

    /**
     * Specify orderBy clause for query, use {@link ConditionBuilder#asc} and {@link ConditionBuilder#desc} static functions to create them
     * @param orderByNode first order by node for sorting
     * @param others other order by nodes
     * @return updated query builder
     */
    public QueryBuilder orderBy(OrderByNode orderByNode, OrderByNode... others){
        rootSelectNode.setOrderByNodes(Stream.concat(Stream.of(orderByNode), Stream.of(others)).toList());
        return this;
    }

    /**
     * Specify limit of number of rows that should be returned
     * @param limit maximum number of rows
     * @return updated query builder
     */
    public QueryBuilder limit(int limit){
        rootSelectNode.setLimit(limit);
        return this;
    }

    /**
     * Specify from which row should DB return the result
     * @param offset offset from start
     * @return updated query builder
     */
    public QueryBuilder offset(int offset){
        rootSelectNode.setOffset(offset);
        return this;
    }

    /**
     * Generate SQL query from builder
     * @return built SQL query
     */
    public String build(){
        return "";
    }

    private List<JoinNode> generateJoinNode(Class<?> root, String joiningRelationPath, Join joinType){
        //fill foreign table fields
        String foreignTableAlias;
        if(joiningRelationPath.split("\\.").length > 1)
            foreignTableAlias = joiningRelationPath.substring(0, joiningRelationPath.lastIndexOf("."));
        else
            foreignTableAlias = baseAlias;
        Class<?> foreignClass = findInstanceType(foreignTableAlias, root);
        EntityMetadata foreignMetadata = MetadataStorage.get(foreignClass);
        String relationName = joiningRelationPath.substring(joiningRelationPath.lastIndexOf(".") + 1);
        Optional<RelationMetadata> relationMetadata = foreignMetadata.getRelations().stream().filter(x -> x.getRelationName().equals(relationName)).findFirst();
        //check if relation exists
        if(relationMetadata.isEmpty())
            throw new InvalidRelationPathException(joiningRelationPath);
        //2 join nodes for many_to_many relations
        if(relationMetadata.get().getRelationType() == RelationType.MANY_TO_MANY){
            RelationMetadata rel = relationMetadata.get();
            String joinedTableName = rel.getJoinedTableName();
            List<String> joiningTablePk = rel.getMyJoinedTableFks();
            String alias = joinTableAliases.get(joinedTableName) == null ? joinedTableName : joinTableAliases.get(joinedTableName) + "I";
            joinTableAliases.put(joinedTableName, alias);
            JoinNode node1 = new JoinNode(joinType, joinedTableName, alias, joiningTablePk, foreignTableAlias, extractKeys(foreignMetadata));
            Class<?> joiningClass = findInstanceType(joiningRelationPath, root);
            EntityMetadata joiningMetadata = MetadataStorage.get(joiningClass);
            String tableName = joiningMetadata.getTableName();
            List<String> secondTablePk = extractKeys(joiningMetadata);
            JoinNode node2 = new JoinNode(INNER, tableName, joiningRelationPath, secondTablePk, alias, rel.getForeignKeyNames());
            return List.of(node1, node2);
        }
        else{
            //fill joining table fields
            Class<?> joiningClass = findInstanceType(joiningRelationPath, root);
            EntityMetadata joiningMetadata = MetadataStorage.get(joiningClass);
            String tableName = joiningMetadata.getTableName();
            List<String> joiningTablePk = extractKeys(joiningMetadata);
            return List.of(new JoinNode(joinType, tableName, joiningRelationPath, joiningTablePk, foreignTableAlias, relationMetadata.get().getForeignKeyNames()));
        }
    }

    //traverse through path to find right class
    private Class<?> findInstanceType(String path, Class<?> start) {
        Class<?> current = start;
        if(path.equals(baseAlias))
            return start;
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

    public String generateJoinClauses(){
        StringBuilder builder = new StringBuilder();
        for(var join : rootSelectNode.getJoinNodes()){
            builder.append(dialect.generateJoinClause(join));
            builder.append("\n");
        }
        return builder.toString();
    }

    public final Join LEFT = Join.LEFT;
    public final Join INNER = Join.INNER;
    public final Join RIGHT = Join.RIGHT;
    public final Join FULL = Join.FULL;
}
