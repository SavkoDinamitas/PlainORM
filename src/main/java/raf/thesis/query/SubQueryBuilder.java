package raf.thesis.query;

import raf.thesis.query.tree.Expression;
import raf.thesis.query.tree.SelectNode;


import java.util.List;

public class SubQueryBuilder extends QueryBuilder implements Expression {
    List<String> columns;
    SubQueryBuilder(Class<?> root, List<String> columns) {
        super(new SelectNode(root));
        this.columns = columns;
    }
}
