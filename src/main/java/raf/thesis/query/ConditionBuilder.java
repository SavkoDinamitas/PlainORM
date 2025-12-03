package raf.thesis.query;

import lombok.NoArgsConstructor;
import raf.thesis.query.tree.*;

import java.util.stream.Stream;

@SuppressWarnings("ClassEscapesDefinedScope")
@NoArgsConstructor
public class ConditionBuilder {
    /**
     * Specify field for condition check
     * @param fieldPath dot separated relation path to field
     * @return updated condition builder
     */
    public static FieldNode field(String fieldPath) {
        String[] path = fieldPath.split("\\.");
        String fieldName = path[path.length - 1];
        int index = fieldPath.lastIndexOf(".");
        String alias = index == -1 ? "%root" : fieldPath.substring(0, index);
        return new FieldNode(alias, fieldName);
    }

    public static Literal lit(String value){
        return new Literal.StringCnst(value);
    }

    public static Literal lit(long value){
        return new Literal.LongCnst(value);
    }

    public static Literal lit(double value){
        return new Literal.DoubleCnst(value);
    }

    public static TupleNode tuple(Expression e1, Expression e2, Expression... expressions){
        return new TupleNode(Stream.concat(Stream.of(e1, e2), Stream.of(expressions)).toList());
    }

    public static OrderByNode asc(Expression expression){
        return new OrderByNode(expression, Ordering.ASC);
    }

    public static OrderByNode desc(Expression expression){
        return new OrderByNode(expression, Ordering.DESC);
    }

    public static FunctionNode avg(Expression expression, Distinct distinct){
        return new FunctionNode(expression, FunctionCode.AVG, true);
    }
    public static FunctionNode avg(Expression expression){
        return new FunctionNode(expression, FunctionCode.AVG, false);
    }

    public static FunctionNode sum(Expression expression, Distinct distinct){
        return new FunctionNode(expression, FunctionCode.SUM, true);
    }
    public static FunctionNode sum(Expression expression){
        return new FunctionNode(expression, FunctionCode.SUM, false);
    }

    public static FunctionNode max(Expression expression, Distinct distinct){
        return new FunctionNode(expression, FunctionCode.MAX, true);
    }
    public static FunctionNode max(Expression expression){
        return new FunctionNode(expression, FunctionCode.MAX, false);
    }

    public static FunctionNode min(Expression expression, Distinct distinct){
        return new FunctionNode(expression, FunctionCode.MIN, true);
    }
    public static FunctionNode min(Expression expression){
        return new FunctionNode(expression, FunctionCode.MIN, false);
    }

    public static FunctionNode count(Expression expression, Distinct distinct){
        return new FunctionNode(expression, FunctionCode.COUNT, true);
    }
    public static FunctionNode count(Expression expression){
        return new FunctionNode(expression, FunctionCode.COUNT, false);
    }

    public static BinaryOp and(Expression c1, Expression c2, Expression... cRest){
        if (cRest.length > 0) {
            var result = cRest[cRest.length - 1];
            for (int i = cRest.length - 2; i >= 0; i--)
                result = new BinaryOp(cRest[i], result, BinaryOpCode.AND);
            return new BinaryOp(c1, new BinaryOp(c2, result, BinaryOpCode.AND), BinaryOpCode.AND);
        }
        return new BinaryOp(c1, c2, BinaryOpCode.AND);
    }

    public static BinaryOp or(Expression c1, Expression c2, Expression... cRest){
        if (cRest.length > 0) {
            var result = cRest[cRest.length - 1];
            for (int i = cRest.length - 2; i >= 0; i--)
                result = new BinaryOp(cRest[i], result, BinaryOpCode.OR);
            return new BinaryOp(c1, new BinaryOp(c2, result, BinaryOpCode.OR), BinaryOpCode.OR);
        }
        return new BinaryOp(c1, c2, BinaryOpCode.OR);
    }

    public static UnaryOp not(Expression expression){
        return new UnaryOp(expression, UnaryOpCode.NOT);
    }

    private static class Distinct { public String toString() { return "DISTINCT"; } }
    public static final Distinct DISTINCT = new Distinct();
}
