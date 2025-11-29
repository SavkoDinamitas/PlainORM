package raf.thesis.query.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.storage.MetadataStorage;
import raf.thesis.query.Join;
import raf.thesis.query.dialect.Dialect;
import raf.thesis.query.exceptions.InvalidRelationPathException;

import java.util.ArrayList;
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
