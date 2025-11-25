package raf.thesis.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RelationMetadata {
    Field foreignField;
    String relationName;
    RelationType relationType;
    Class<?> foreignClass;
    //fields for query building
    List<String> foreignKeyNames;
    String joinedTableName;
    List<String> myJoinedTableFks;
    //potential eager n:m loading
    String foreignRelationName;

    public RelationMetadata(Field foreignField, String relationName, RelationType relationType, Class<?> foreignClass) {
        this.foreignField = foreignField;
        this.relationName = relationName;
        this.relationType = relationType;
        this.foreignClass = foreignClass;
    }

    public RelationMetadata(Field foreignField, String relationName, RelationType relationType, Class<?> foreignClass, String foreignRelationName) {
        this.foreignField = foreignField;
        this.relationName = relationName;
        this.relationType = relationType;
        this.foreignClass = foreignClass;
        this.foreignRelationName = foreignRelationName;
    }
}
