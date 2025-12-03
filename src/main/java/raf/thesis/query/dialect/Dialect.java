package raf.thesis.query.dialect;

import raf.thesis.query.tree.*;

public interface Dialect {
    String generateSelectClause(SelectNode select);

    String generateJoinClause(JoinNode joinNode);

    String generateWhereClause(WhereNode whereNode);

    String generateHavingClause(HavingNode havingNode);

    String generateGroupByClause(GroupByNode groupByNode);

    String generateOrderByClause(OrderByNode orderByNode);

    String generateLimitClause(LimitNode limitNode);

    String generateBinaryOperationExp(BinaryOp operation);

    String generateUnaryOperationExp(UnaryOp operation);

    String generateLiteralExp(Literal literal);

    String generateFunctionExp(FunctionNode functionNode);

    String generateTupleExp(TupleNode tupleNode);

    String generateFieldExp(FieldNode fieldNode);
}
