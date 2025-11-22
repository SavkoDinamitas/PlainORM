package raf.thesis.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EntityMetadata {
    private String tableName;
    private Class<?> entityClass;
    private List<Field> idFields;
    //TODO: make these keys lowercase always!!
    private Map<String, ColumnMetadata> columns = new HashMap<>();
    private List<RelationMetadata> relations = new ArrayList<>();
}
