package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import raf.thesis.query.dialect.Dialect;

@Getter
@Setter
@AllArgsConstructor
public class UnaryOp implements Expression{
    private Expression exp;
    private UnaryOpCode code;

    @Override
    public String toSql(Dialect dialect) {
        return dialect.generateUnaryOperationExp(this);
    }
}
