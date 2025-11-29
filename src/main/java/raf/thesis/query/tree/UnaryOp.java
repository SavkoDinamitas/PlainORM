package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UnaryOp implements Expression{
    private Expression exp;
    private UnaryOpCode code;
}
