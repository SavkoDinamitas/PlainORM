package raf.thesis.query.tree;

import raf.thesis.query.dialect.Dialect;

public sealed interface Literal extends Expression{
    public record DoubleCnst(double x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
    public record LongCnst(long x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
    public record StringCnst(String x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
}
