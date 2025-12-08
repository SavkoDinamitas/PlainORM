package raf.thesis.query.tree;

import raf.thesis.query.dialect.Dialect;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    public record DateCnst(LocalDate x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
    public record DateTimeCnst(LocalDateTime x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
    public record TimeCnst(LocalTime x) implements Literal {
        @Override
        public String toSql(Dialect dialect) {
            return dialect.generateLiteralExp(this);
        }
    }
}
