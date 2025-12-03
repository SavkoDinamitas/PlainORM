package raf.thesis.query;

import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.tree.Expression;
import raf.thesis.query.tree.SelectNode;


import java.util.List;

public class SubQueryBuilder extends QueryBuilder implements Expression {
    List<Expression> columns;
    SubQueryBuilder(Class<?> root, List<Expression> columns) {
        super(new SelectNode(root, MetadataStorage.get(root).getTableName()));
        this.columns = columns;
    }

    @Override
    public String toSql(Dialect dialect) {
        rootSelectNode.setSelectFieldNodes(columns);
        StringBuilder result = new StringBuilder();
        result.append("(");
        result.append(build());
        result.append(")");
        return result.toString();
    }
}
