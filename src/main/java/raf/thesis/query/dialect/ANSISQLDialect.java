package raf.thesis.query.dialect;

import raf.thesis.query.tree.JoinNode;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ANSISQLDialect implements Dialect {
    private String quote(String value){
        return "\"" + value.replaceAll("\"", "\"\"") + "\"";
    }

    @Override
    public String generateJoinClause(JoinNode joinNode){
        String onClause = "%s = %s".formatted(generateKeyTuple(joinNode.getJoiningTableAlias(), joinNode.getJoiningTablePk()),
                generateKeyTuple(joinNode.getForeignTableAlias(), joinNode.getForeignTableFk()));
        return "%s JOIN %s AS %s ON (%s)".formatted(joinNode.getJoinType().name(), joinNode.getTableName(), joinNode.getJoiningTableAlias(), onClause);
    }

    private String generateKeyTuple(String tableAlias, List<String> tableColumns){
        return "(%s)".formatted(tableColumns.stream().map("."::concat).map(quote(tableAlias)::concat));
    }
}
