package raf.thesis.query.tree;

import lombok.Getter;
import lombok.Setter;
import raf.thesis.query.Join;

import java.util.ArrayList;
import java.util.List;

public class SelectNode {
    @Getter
    private final Class<?> root;
    @Getter
    private List<JoinNode> joinNodes = new ArrayList<>();
    private List<Expression> selectFieldNodes = new ArrayList<>();
    @Setter
    private WhereNode whereNode;
    @Setter
    private HavingNode havingNode;
    @Setter
    private GroupByNode groupByNode;
    @Setter
    private List<OrderByNode> orderByNodes;
    private LimitNode limitNode;

    public SelectNode(Class<?> root) {
        this.root = root;
    }

    public void addJoinNode(List<JoinNode> jn){
        joinNodes.addAll(jn);
    }

    public void setLimit(int limit){
        limitNode = limitNode == null ? new LimitNode() : limitNode;
        limitNode.setLimit(limit);
    }

    public void setOffset(int offset){
        limitNode = limitNode == null ? new LimitNode() : limitNode;
        limitNode.setOffset(offset);
    }
}
