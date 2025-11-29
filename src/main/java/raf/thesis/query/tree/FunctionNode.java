package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class FunctionNode implements Expression{
    private Expression exp;
    private FunctionCode code;
    private boolean distinct;
}
