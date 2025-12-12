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

    //names of foreign key columns in DB - unrelated to type of relation, if Many-to-many -> foreign key names in JT to foreign entity
    List<String> foreignKeyNames;
    //name of joined table
    String joinedTableName;
    //names of foreign keys in joined table that refer to me
    List<String> myJoinedTableFks;
    //potential eager n:m loading
    String foreignRelationName;
    //only for ONE-TO-ONE relations -> is the fk in my table?
    Boolean mySideKey;

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

    public RelationMetadata(Field foreignField, String relationName, RelationType relationType, Class<?> foreignClass, List<String> foreignKeyNames, String joinedTableName, List<String> myJoinedTableFks, String foreignRelationName) {
        this.foreignField = foreignField;
        this.relationName = relationName;
        this.relationType = relationType;
        this.foreignClass = foreignClass;
        this.foreignKeyNames = foreignKeyNames;
        this.joinedTableName = joinedTableName;
        this.myJoinedTableFks = myJoinedTableFks;
        this.foreignRelationName = foreignRelationName;
    }
}
