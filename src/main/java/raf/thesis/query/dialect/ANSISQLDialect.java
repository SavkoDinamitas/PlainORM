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
        return "";
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
