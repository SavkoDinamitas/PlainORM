package raf.thesis.query;

import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.tree.Expression;
import raf.thesis.query.tree.OrderByNode;
import raf.thesis.query.tree.SelectNode;


import java.util.List;

public class SubQueryBuilder extends QueryBuilder implements Expression {
    List<Expression> columns;
    SubQueryBuilder(Class<?> root, List<Expression> columns) {
        super(new SelectNode(root, MetadataStorage.get(root).getTableName()));
        this.columns = columns;
    }

    /**
     * Add distinct keyword in query
     * @return updated subquery builder
     */
    public SubQueryBuilder distinct(){
        super.distinct();
        return this;
    }

    /**
     * Specify which relations should be populated in returning objects (inner join by default)
     * @param relationPath dot separated relation path from root entity
     * @return updated subquery builder
     */
    public SubQueryBuilder join(String relationPath){
        super.join(relationPath);
        return this;
    }

    /**
     * Specify which relations should be populated in returning objects
     * @param relationPath dot separated relation path from root entity
     * @param join determine type of join, LEFT, INNER, RIGHT or FULL
     * @return updated subquery builder
     */
    public SubQueryBuilder join(String relationPath, Join join){
        super.join(relationPath, join);
        return this;
    }

    /**
     * Specify where clause for query
     * @param expression expression inside where clause
     * @return updated subquery builder
     */
    public SubQueryBuilder where(Expression expression){
        super.where(expression);
        return this;
    }

    /**
     * Specify having clause for query
     * @param expression expression inside having clause
     * @return updated subquery builder
     */
    public SubQueryBuilder having(Expression expression){
        super.having(expression);
        return this;
    }

    /**
     * Specify groupBy clause for query
     * @param e1 first expression
     * @param expressions other expressions
     * @return updated subquery builder
     */
    public SubQueryBuilder groupBy(Expression e1, Expression... expressions){
        super.groupBy(e1, expressions);
        return this;
    }

    /**
     * Specify orderBy clause for query, use {@link ConditionBuilder#asc} and {@link ConditionBuilder#desc} static functions to create them
     * @param orderByNode first order by node for sorting
     * @param others other order by nodes
     * @return updated subquery builder
     */
    public SubQueryBuilder orderBy(OrderByNode orderByNode, OrderByNode... others){
        super.orderBy(orderByNode, others);
        return this;
    }

    /**
     * Specify limit of number of rows that should be returned
     * @param limit maximum number of rows
     * @return updated subquery builder
     */
    public SubQueryBuilder limit(int limit){
        super.limit(limit);
        return this;
    }

    /**
     * Specify from which row should DB return the result
     * @param offset offset from start
     * @return updated subquery builder
     */
    public QueryBuilder offset(int offset){
        super.offset(offset);
        return this;
    }

    @Override
    public String toSql(Dialect dialect) {
        rootSelectNode.setSelectFieldNodes(columns);
        StringBuilder result = new StringBuilder();
        result.append("\n(");
        result.append(build());
        //remove ; at the end of build
        result.deleteCharAt(result.length() - 1);
        result.append(")");
        return result.toString();
    }
}
