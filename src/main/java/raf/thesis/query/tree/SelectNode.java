package raf.thesis.query.tree;

import lombok.Getter;
import raf.thesis.query.Join;

import java.util.ArrayList;
import java.util.List;

public class SelectNode {
    @Getter
    private final Class<?> root;
    private List<JoinNode> joinNodes = new ArrayList<>();
    private WhereNode whereNode;
    private HavingNode havingNode;
    private GroupByNode groupByNode;
    private List<OrderByNode> orderByNodes = new ArrayList<>();
    private LimitNode limitNode;

    public SelectNode(Class<?> root) {
        this.root = root;
    }

    public void addJoinNode(JoinNode jn){
        joinNodes.add(jn);
    }
}
