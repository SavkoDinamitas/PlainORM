package raf.thesis.query.tree;

import raf.thesis.query.ToSql;

public interface Expression extends ToSql {
    default Expression gt(Expression expr) {
        return new BinaryOp(this, expr, BinaryOpCode.GT);
    }

    default Expression lt(Expression expr) {
        return new BinaryOp(this, expr, BinaryOpCode.LT);
    }

    default Expression eq(Expression expr) {
        return new BinaryOp(this, expr, BinaryOpCode.EQ);
    }

    default Expression like(String pattern) {
        return new BinaryOp(this, new Literal.StringCnst(pattern), BinaryOpCode.LIKE);
    }

    default Expression in(Expression expr) {
        return new BinaryOp(this, expr, BinaryOpCode.IN);
    }

    default Expression isNull(){
        return new UnaryOp(this, UnaryOpCode.IS_NULL);
    }
}
