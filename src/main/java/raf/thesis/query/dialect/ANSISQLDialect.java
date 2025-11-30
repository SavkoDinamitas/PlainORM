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
        //TODO: change for expressions after implementing .toSql()
        for(Expression exp : fieldNodes){
            FieldNode fieldNode = (FieldNode) exp;
            String row = "%s.%s AS %s".formatted(quote(fieldNode.getTableAlias()),
                    fieldNode.getFieldName(),
                    quote("%s%s.%s".formatted(handleRootField(fieldNode.getTableAlias(), baseAlias), fieldNode.getTableAlias(), fieldNode.getFieldName())));
            sb.append(row).append(",\n");
        }
        sb.delete(sb.length()-2, sb.length());
        return sb.toString();
    }

    private String handleRootField(String tableAlias, String baseRoot){
        if(tableAlias.equals(baseRoot)){
            return "";
        }
        return baseRoot + ".";
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
}
