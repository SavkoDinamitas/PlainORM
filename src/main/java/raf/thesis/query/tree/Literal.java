package raf.thesis.query.tree;

public sealed interface Literal extends Expression{
    public record DoubleCnst(double x) implements Literal {}
    public record LongCnst(long x) implements Literal {}
    public record StringCnst(String x) implements Literal {}
}
