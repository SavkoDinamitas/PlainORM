package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import raf.thesis.query.dialect.Dialect;

@AllArgsConstructor
@Getter
public class FieldNode implements Expression{
    private String fieldName;
    private String tableAlias;

    @Override
    public String toSql(Dialect dialect) {
        return dialect.generateFieldExp(this);
    }
}
