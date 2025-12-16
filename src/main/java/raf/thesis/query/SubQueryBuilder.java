package raf.thesis.query;

import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.tree.Expression;
import raf.thesis.query.tree.OrderByNode;
import raf.thesis.query.tree.SelectNode;

import java.util.List;

/**
 * Helper class for {@link QueryBuilder}.
 * Builds subquery expressions for use in conditional expressions in {@code WHERE} clause.
 */
public class SubQueryBuilder extends QueryBuilder implements Expression {
    List<Expression> columns;

    /**
    * Use default super constructor and set SELECT clause to given column expressions
    */
    SubQueryBuilder(Class<?> root, List<Expression> columns) {
        super(new SelectNode(root, MetadataStorage.get(root).getTableName()));
        this.columns = columns;
    }

    /**
     * Adds {@code DISTINCT} keyword in select query.
     *
     * @return this subquery builder with the {@code DISTINCT} keyword applied
     */
    public SubQueryBuilder distinct() {
        super.distinct();
        return this;
    }

    /**
     * Specifies which relations should be populated in the returned objects.
     * Uses an {@link Join#INNER} by default.
     *
     * @param relationPath dot-separated relation path from the root entity
     * @return this subquery builder with the added {@code JOIN} clause
     */
    public SubQueryBuilder join(String relationPath) {
        super.join(relationPath);
        return this;
    }

    /**
     * Specifies which relations should be populated in the returned objects.
     *
     * @param relationPath dot-separated relation path from the root entity
     * @param join         the join type to use {@link Join}
     * @return this subquery builder with the added {@code JOIN} clause
     */
    public SubQueryBuilder join(String relationPath, Join join) {
        super.join(relationPath, join);
        return this;
    }

    /**
     * Specifies the {@code WHERE} clause for the subquery.
     *
     * @param expression the {@code WHERE} condition expression
     * @return this subquery builder with the {@code WHERE} clause applied
     */
    public SubQueryBuilder where(Expression expression) {
        super.where(expression);
        return this;
    }

    /**
     * Specifies the {@code HAVING} clause for the subquery.
     *
     * @param expression the {@code HAVING} condition expression
     * @return this subquery builder with the {@code HAVING} clause applied
     */
    public SubQueryBuilder having(Expression expression) {
        super.having(expression);
        return this;
    }

    /**
     * Specifies the {@code GROUP BY} clause for the subquery.
     *
     * @param e1          required primary grouping expression
     * @param expressions optional additional grouping expressions
     * @return this subquery builder with the grouping applied
     */
    public SubQueryBuilder groupBy(Expression e1, Expression... expressions) {
        super.groupBy(e1, expressions);
        return this;
    }

    /**
     * Specifies the {@code ORDER BY} clause for the subquery.
     * Use {@link ConditionBuilder#asc} and {@link ConditionBuilder#desc} static functions to create ordering nodes.
     *
     * @param orderByNode required primary ordering node
     * @param others      optional additional ordering nodes
     * @return this subquery builder with the ordering applied
     */
    public SubQueryBuilder orderBy(OrderByNode orderByNode, OrderByNode... others) {
        super.orderBy(orderByNode, others);
        return this;
    }

    /**
     * Specifies the maximum number of rows that should be returned beginning from the offset.
     * If offset is not specified, returns the rows starting from the beginning.
     *
     * @param limit maximum number of rows to return
     * @return this subquery builder with the limit applied
     */
    public SubQueryBuilder limit(int limit) {
        super.limit(limit);
        return this;
    }

    /**
     * Specifies the starting row (offset) from which the database should return result rows.
     *
     * @param offset offset number of rows to skip before returning results
     * @return this subquery builder with the offset applied
     */
    public QueryBuilder offset(int offset) {
        super.offset(offset);
        return this;
    }

    /**
     * Generated SQL for the subquery is .build() in brackets without ;
     */
    @Override
    public String toSql(Dialect dialect) {
        rootSelectNode.setSelectFieldNodes(columns);
        StringBuilder result = new StringBuilder();
        result.append("\n(");
        result.append(build(dialect));
        //remove ; at the end of build
        result.deleteCharAt(result.length() - 1);
        result.append(")");
        return result.toString();
    }
}
