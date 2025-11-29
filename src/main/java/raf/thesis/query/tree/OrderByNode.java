package raf.thesis.query.tree;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OrderByNode {
    private Expression exp;
    private Ordering order;
}
