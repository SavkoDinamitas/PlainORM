package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import raf.thesis.query.Join;
import java.util.List;

@Getter
@AllArgsConstructor
public class JoinNode {
    private Join joinType;
    //joining table is new table inside join clause
    private String tableName;
    private String joiningTableAlias;
    private List<String> joiningTablePk;
    //foreign table is table on which new one is joining
    private String foreignTableAlias;
    private List<String> foreignTableFk;
}
