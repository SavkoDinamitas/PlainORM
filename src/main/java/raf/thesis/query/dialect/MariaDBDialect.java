package raf.thesis.query.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MariaDBDialect extends ANSISQLDialect implements Dialect.UsesInsertReturning{
    @Override
    protected String quote(String value){
        return "`" + value.replaceAll("`", "``") + "`";
    }

    private String insertHelper(List<String> columns, String tableName) {
        String s = generateInsertQuery(columns, tableName);
        return s.substring(0, s.length()-1);
    }

    public String generateReturningClause(List<String> keys){
        return keys.stream().collect(Collectors.joining(", ", " RETURNING ", ""));
    }

    @Override
    public String generateInsertQuery(List<String> columns, String tableName, List<String> returningKeys) {
        return insertHelper(columns, tableName) + generateReturningClause(returningKeys);
    }
}
