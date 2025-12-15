package raf.thesis.query.dialect;

import java.util.List;
import java.util.stream.Collectors;

public class MSSQLServerDialect extends ANSISQLDialect implements Dialect.UsesInsertReturning{

    private String generateOutputClause(List<String> keys){
        return keys.stream().map("INSERTED."::concat).collect(Collectors.joining(", ", "\nOUTPUT ", "\n"));
    }

    @Override
    protected String generateOffset(Integer offset){
        return offset == null ? "OFFSET 0 ROWS" : "OFFSET %s ROWS".formatted(offset);
    }

    @Override
    public String generateInsertQuery(List<String> columns, String tableName, List<String> returningKeys) {
        return "INSERT INTO %s (%s)%sVALUES (%s);".formatted(tableName, generateInsertColumnParenthesis(columns), generateOutputClause(returningKeys), generateQuestionMarks(columns.size()));
    }
}
