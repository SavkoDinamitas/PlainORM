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
    private final Set<String> joinTables = new HashSet<>();
    protected final Dialect dialect = new ANSISQLDialect();
    private boolean pdoQuery = false;
    /**
     * Set root object of query builder to specify type that is returned
     * @param object .class of entity a query should return
     * @return new query builder instance with set root
     */
    public static QueryBuilder select(Class<?> object){
        SelectNode sn = new SelectNode(object, MetadataStorage.get(object).getTableName());
        return new QueryBuilder(sn);
    }

    /**
     * Use this constructor only for returning Plain Data Objects, it doesn't support entities mapping
     * @param object .class of object which table will be used in FROM clause
     * @param a1 a column that query should return but with given custom alias (should be same name as PTO object field)
     * @param columns other columns query should return
     * @return new query builder instance with set root table and returning columns
     */
    public static QueryBuilder select(Class<?> object, AliasedColumn a1, AliasedColumn... columns){
        SelectNode sn = new SelectNode(object, MetadataStorage.get(object).getTableName());
        return new QueryBuilder(sn, Stream.concat(Stream.of(a1), Stream.of(columns)).toList());
    }

    /**
     * Special constructor for SubQuery builder, do not use for regular query building
     * @param object root table for subquery builder
     * @param e1 column subquery should return
     * @param exps other columns subquery should return
     * @return new instance of subquery builder
     */
    public static SubQueryBuilder subQuery(Class<?> object, Expression e1, Expression... exps){
        return new SubQueryBuilder(object, Stream.concat(Stream.of(e1), Stream.of(exps)).toList());
    }

    protected QueryBuilder(SelectNode rootSelectNode){
        this.rootSelectNode = rootSelectNode;
        handleRootColumns(rootSelectNode.getRoot());
    }

    private QueryBuilder(SelectNode rootSelectNode, List<AliasedColumn> columns){
        List<Expression> cols = new ArrayList<>(columns);
        rootSelectNode.setSelectFieldNodes(cols);
        this.rootSelectNode = rootSelectNode;
        this.pdoQuery = true;
    }
    /**
     * Add distinct keyword in query
     * @return updated query builder
     */
    public QueryBuilder distinct(){
        rootSelectNode.setDistinct(true);
        return this;
    }

    /**
     * Specify which relations should be populated in returning objects (inner join by default)
     * @param relationPath dot separated relation path from root entity
     * @return updated query builder
     */
    public QueryBuilder join(String relationPath){
        rootSelectNode.addJoinNode(generateJoinNode(rootSelectNode.getRoot(), relationPath, INNER));
        if(!pdoQuery)
            handleJoinedTableColumns(relationPath);
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
        if(!pdoQuery)
            handleJoinedTableColumns(relationPath);
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
     * Specify having clause for query, use only for queries that map to PDO-s
     * @param expression expression inside having clause
     * @return updated query builder
     */
    public QueryBuilder having(Expression expression){
        rootSelectNode.setHavingNode(new HavingNode(expression));
        return this;
    }

    /**
     * Specify groupBy clause for query, use only for queries that map to PDO-s
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
        return generateSelectClause() + "\n" + generateJoinClauses() + generateWhereClause() + generateGroupByClause() + generateHavingClause() + generateOrderByClause() + ";";
    }

    private List<JoinNode> generateJoinNode(Class<?> root, String joiningRelationPath, Join joinType){
        //fill foreign table fields
        String foreignTableAlias;
        //find alias for old table (old -> already joined or root)
        if(joiningRelationPath.split("\\.").length > 1)
            foreignTableAlias = joiningRelationPath.substring(0, joiningRelationPath.lastIndexOf("."));
        else
            foreignTableAlias = rootSelectNode.getBaseAlias();
        //locate relation metadata of given path
        Class<?> foreignClass = findInstanceType(foreignTableAlias, root);
        EntityMetadata foreignMetadata = MetadataStorage.get(foreignClass);
        String relationName = joiningRelationPath.substring(joiningRelationPath.lastIndexOf(".") + 1);
        Optional<RelationMetadata> relationMetadata = foreignMetadata.getRelations().stream().filter(x -> x.getRelationName().equals(relationName)).findFirst();
        //check if relation exists
        if(relationMetadata.isEmpty())
            throw new InvalidRelationPathException(joiningRelationPath);
        //2 join nodes for many_to_many relations
        if(relationMetadata.get().getRelationType() == RelationType.MANY_TO_MANY){
            List<JoinNode> result = new ArrayList<>();
            //make node for joining table on first time joinedTableNameUse
            //no need to make new one for other side join in same query
            RelationMetadata rel = relationMetadata.get();
            String joinedTableName = rel.getJoinedTableName();
            if(joinTables.add(joinedTableName)){
                List<String> joiningTablePk = rel.getMyJoinedTableFks();
                result.add(new JoinNode(joinType, joinedTableName, joinedTableName, joiningTablePk, foreignTableAlias, extractKeys(foreignMetadata)));
            }
            //get table name for joining class
            Class<?> joiningClass = findInstanceType(joiningRelationPath, root);
            EntityMetadata joiningMetadata = MetadataStorage.get(joiningClass);
            String tableName = joiningMetadata.getTableName();
            //make list of 2 nodes for n:m relations
            List<String> secondTablePk = extractKeys(joiningMetadata);
            result.add(new JoinNode(INNER, tableName, joiningRelationPath, secondTablePk, joinedTableName, rel.getForeignKeyNames()));
            return result;
        }
        else{
            //fill joining table fields
            Class<?> joiningClass = findInstanceType(joiningRelationPath, root);
            EntityMetadata joiningMetadata = MetadataStorage.get(joiningClass);
            String tableName = joiningMetadata.getTableName();
            //two cases: @MANY_TO_ONE and others
            //@MANY_TO_ONE -> old table has the foreign key and new one uses its pk for join
            //others -> joining table has the fk that joins on old table pk
            List<String> joiningTablePk;
            List<String> foreignTableKeys;
            var relation = relationMetadata.get();
            if(relation.getRelationType() == RelationType.MANY_TO_ONE){
                foreignTableKeys = relation.getForeignKeyNames();
                joiningTablePk = extractKeys(joiningMetadata);
            }
            else{
                foreignTableKeys = extractKeys(foreignMetadata);
                joiningTablePk = relation.getForeignKeyNames();
            }
            return List.of(new JoinNode(joinType, tableName, joiningRelationPath, joiningTablePk, foreignTableAlias, foreignTableKeys));
        }
    }

    //traverse through path to find right class
    private Class<?> findInstanceType(String path, Class<?> start) {
        Class<?> current = start;
        if(path.equals(rootSelectNode.getBaseAlias()))
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

    //handles root columns
    private void handleRootColumns(Class<?> root){
        extractColumns(MetadataStorage.get(root), rootSelectNode.getBaseAlias());
    }

    private void handleJoinedTableColumns(String relationPath){
        Class<?> entity = findInstanceType(relationPath, rootSelectNode.getRoot());
        extractColumns(MetadataStorage.get(entity), relationPath);
    }

    //makes list of FieldNodes and adds to select clause
    private void extractColumns(EntityMetadata metadata, String tableAlias){
        for(var column : metadata.getColumns().values()){
            FieldNode node = new FieldNode(column.getColumnName(), tableAlias);
            rootSelectNode.addSelectClauseColumn(node);
        }
    }

    public String generateJoinClauses(){
        StringBuilder builder = new StringBuilder();
        for(var join : rootSelectNode.getJoinNodes()){
            builder.append(dialect.generateJoinClause(join));
            builder.append("\n");
        }
        return builder.toString();
    }

    public String generateSelectClause(){
        return dialect.generateSelectClause(rootSelectNode);
    }

    public String generateWhereClause(){
        return rootSelectNode.getWhereNode() != null ? dialect.generateWhereClause(rootSelectNode.getWhereNode()) + "\n" : "";
    }

    public String generateGroupByClause(){
        return rootSelectNode.getGroupByNode() != null ? dialect.generateGroupByClause(rootSelectNode.getGroupByNode()) + "\n" : "";
    }

    public String generateHavingClause(){
        return rootSelectNode.getHavingNode() != null ? dialect.generateHavingClause(rootSelectNode.getHavingNode()) + "\n" : "";
    }

    public String generateOrderByClause(){
        return rootSelectNode.getOrderByNodes() != null ? dialect.generateOrderByClause(rootSelectNode.getOrderByNodes()) : "";
    }

    public final Join LEFT = Join.LEFT;
    public final Join INNER = Join.INNER;
    public final Join RIGHT = Join.RIGHT;
    public final Join FULL = Join.FULL;
}
