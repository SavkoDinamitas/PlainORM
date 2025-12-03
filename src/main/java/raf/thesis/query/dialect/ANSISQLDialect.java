package raf.thesis.query.dialect;

import raf.thesis.query.tree.*;

import java.util.List;
import java.util.stream.Collectors;

public class ANSISQLDialect implements Dialect {
    private String quote(String value){
        return "\"" + value.replaceAll("\"", "\"\"") + "\"";
    }

    @Override
    public String generateSelectClause(SelectNode select) {
        return "SELECT\n%s\n FROM %s AS %s".formatted(generateFields(select.getSelectFieldNodes(), select.getBaseAlias()), select.getBaseTableName(), quote(select.getBaseAlias()));
    }

    private String generateFields(List<Expression> fieldNodes, String baseAlias){
        StringBuilder sb = new StringBuilder();
        for(Expression exp : fieldNodes){
            //generate fields with needed aliases for mapper
            if(exp instanceof FieldNode fn){
                sb.append(generateFieldExpWithAlias(fn, baseAlias));
            }
            else
                sb.append(exp.toSql(this));
            sb.append(",\n");
        }
        sb.delete(sb.length()-2, sb.length());
        return sb.toString();
    }

    @Override
    public String generateJoinClause(JoinNode joinNode){
        String onClause = "%s = %s".formatted(generateKeyTuple(joinNode.getJoiningTableAlias(), joinNode.getJoiningTablePk()),
                generateKeyTuple(joinNode.getForeignTableAlias(), joinNode.getForeignTableFk()));
        return "%s JOIN %s AS %s ON (%s)".formatted(joinNode.getJoinType().name(), joinNode.getTableName(), quote(joinNode.getJoiningTableAlias()), onClause);
    }

    private String generateKeyTuple(String tableAlias, List<String> tableColumns){
        return "(%s)".formatted(tableColumns.stream().map("."::concat).map(quote(tableAlias)::concat).collect(Collectors.joining()));
    }

    @Override
    public String generateWhereClause(WhereNode whereNode) {
        return "";
    }

    @Override
    public String generateHavingClause(HavingNode havingNode) {
        return "";
    }

    @Override
    public String generateGroupByClause(GroupByNode groupByNode) {
        return "";
    }

    @Override
    public String generateOrderByClause(OrderByNode orderByNode) {
        return "";
    }

    @Override
    public String generateLimitClause(LimitNode limitNode) {
        return "";
    }

    @Override
    public String generateBinaryOperationExp(BinaryOp operation) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        result.append(operation.getLeft().toSql(this));
        result.append(")");
        switch (operation.getCode()){
            case AND -> result.append(" AND ");
            case OR -> result.append(" OR ");
            case EQ -> result.append(" = ");
            case GT -> result.append(" > ");
            case LT -> result.append(" < ");
            case LIKE -> result.append(" LIKE ");
            case IN -> result.append(" IN ");
        }
        result.append("(");
        result.append(operation.getRight().toSql(this));
        result.append(")");
        return result.toString();
    }

    @Override
    public String generateUnaryOperationExp(UnaryOp operation) {
        StringBuilder result = new StringBuilder();
        switch (operation.getCode()){
            case NOT:
                result.append("NOT (");
                result.append(operation.getExp().toSql(this));
                result.append(")");
                break;
            case IS_NULL:
                result.append(operation.getExp().toSql(this));
                result.append(" IS NULL");
        }
        return result.toString();
    }

    @Override
    public String generateLiteralExp(Literal literal) {
        return switch(literal){
            case Literal.DoubleCnst d -> String.valueOf(d.x());
            case Literal.LongCnst l -> String.valueOf(l.x());
            case Literal.StringCnst s -> "'" + s + "'";
        };
    }

    @Override
    public String generateFunctionExp(FunctionNode functionNode) {
        StringBuilder result = new StringBuilder();
        switch (functionNode.getCode()){
            case COUNT -> result.append("COUNT(");
            case SUM -> result.append("SUM(");
            case AVG -> result.append("AVG(");
            case MIN -> result.append("MIN(");
            case MAX -> result.append("MAX(");
        }
        result.append(functionNode.getExp().toSql(this));
        result.append(")");
        return result.toString();
    }

    @Override
    public String generateTupleExp(TupleNode tupleNode) {
        StringBuilder result = new StringBuilder();
        result.append("(");
        for(var x : tupleNode.getOperands()){
            result.append(x.toSql(this));
            result.append(",");
        }
        result.deleteCharAt(result.length()-1);
        result.append(")");
        return result.toString();
    }

    @Override
    public String generateFieldExp(FieldNode fieldNode) {
        return "%s.%s".formatted(quote(fieldNode.getTableAlias()), fieldNode.getFieldName());
    }

    private String generateFieldExpWithAlias(FieldNode fieldNode, String baseAlias) {
        return "%s AS %s".formatted(generateFieldExp(fieldNode),
                quote("%s%s.%s".formatted(handleRootField(fieldNode.getTableAlias(), baseAlias), fieldNode.getTableAlias(), fieldNode.getFieldName())));
    }

    private String handleRootField(String tableAlias, String baseRoot){
        if(tableAlias.equals(baseRoot)){
            return "";
        }
        return baseRoot + ".";
    }
}
