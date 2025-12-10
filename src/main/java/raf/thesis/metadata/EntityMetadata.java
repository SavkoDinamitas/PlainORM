package raf.thesis.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class EntityMetadata {
    private String tableName;
    private Class<?> entityClass;
    private List<Field> idFields = new ArrayList<>();
    //TODO: make these keys lowercase always!!
    private Map<String, ColumnMetadata> columns = new LinkedHashMap<>();
    private List<RelationMetadata> relations = new ArrayList<>();
    private List<Boolean> generatedId = new ArrayList<>();
}
