package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import raf.thesis.query.dialect.Dialect;

@AllArgsConstructor
@Getter @Setter
@NoArgsConstructor
public class BinaryOp implements Expression{
    private Expression left, right;
    private BinaryOpCode code;

    @Override
    public String toSql(Dialect dialect) {
        return dialect.generateBinaryOperationExp(this);
    }
}
