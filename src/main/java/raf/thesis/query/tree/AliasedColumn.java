package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import raf.thesis.query.dialect.Dialect;

@Getter@AllArgsConstructor
public class AliasedColumn implements Expression{
    private Expression expression;
    private String colAlias;
    @Override
    public String toSql(Dialect dialect) {
        return dialect.generateAliasedFieldExp(this);
    }
}
