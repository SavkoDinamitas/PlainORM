package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FieldNode implements Expression{
    private String fieldName;
    private String tableAlias;
}
