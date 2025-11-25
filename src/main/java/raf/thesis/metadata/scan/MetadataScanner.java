package raf.thesis.metadata.scan;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.Scanners;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.RelationMetadata;
import raf.thesis.metadata.RelationType;
import raf.thesis.metadata.annotations.*;
import raf.thesis.metadata.exception.ListFieldRequiredException;
import raf.thesis.metadata.exception.RequiredFieldException;
import raf.thesis.metadata.exception.UnsupportedRelationException;
import raf.thesis.metadata.storage.MetadataStorage;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MetadataScanner {
    private boolean initialized = false;
    private final List<RelationMetadata> solveForeignKeys = new ArrayList<>();
    private final List<RelationMetadata> madeRelations = new ArrayList<>();

    public synchronized void discoverMetadata(String... basePackages) {
        if (initialized) {
            return;
        }

        //initialize reflections scanner
        ConfigurationBuilder builder = new ConfigurationBuilder();
        for (String basePackage : basePackages) {
            builder.addUrls(ClasspathHelper.forPackage(basePackage));
        }
        builder.setScanners(Scanners.values());

        Reflections reflections = new Reflections(builder);

        //locate all @Entity annotated class files on classpath
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class<?> entityClass : entities) {
            //process annotations inside class
            processEntity(entityClass);
        }

        //check if all relations are valid
        for (var relation : madeRelations) {
            if (!MetadataStorage.contains(relation.getForeignClass())) {
                throw new UnsupportedRelationException("Class " + relation.getForeignClass().getName() + " inside " + relation.getForeignRelationName() + " is not an Entity");
            }
        }

        //solve missing foreignKeys
        for (var relation : solveForeignKeys)
            solveRelationWithoutFK(relation);

        initialized = true;
    }

    private void processEntity(Class<?> clazz) {
        Entity entityAnn = clazz.getAnnotation(Entity.class);

        EntityMetadata meta = new EntityMetadata();
        assert entityAnn != null;
        if (entityAnn.tableName() == null) {
            throw new RequiredFieldException("Entity " + clazz.getSimpleName() + " requires table name");
        }
        meta.setTableName(entityAnn.tableName());
        meta.setEntityClass(clazz);

        //process each field of class further for relations and column names
        for (var field : clazz.getDeclaredFields()) {
            processField(field, clazz, meta);
        }
        MetadataStorage.register(meta);
    }

    private void processField(Field field, Class<?> clazz, EntityMetadata meta) {
        boolean columnMade = false;
        if (field.isAnnotationPresent(Id.class)) {
            meta.getIdFields().add(field);
        }
        if (field.isAnnotationPresent(Column.class)) {
            Column columnAnn = field.getAnnotation(Column.class);
            ColumnMetadata columnMeta = new ColumnMetadata();

            assert columnAnn != null;
            columnMeta.setColumnName(!columnAnn.columnName().isEmpty() ? columnAnn.columnName() : field.getName());
            columnMeta.setField(field);
            meta.getColumns().put(columnMeta.getColumnName(), columnMeta);
            columnMade = true;
        }
        if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne oneAnn = field.getAnnotation(OneToOne.class);
            RelationMetadata relationMetadata = new RelationMetadata();

            assert oneAnn != null;
            relationMetadata.setRelationName(!oneAnn.relationName().isEmpty() ? oneAnn.relationName() : field.getName());
            relationMetadata.setRelationType(RelationType.ONE_TO_ONE);
            relationMetadata.setForeignField(field);
            relationMetadata.setForeignClass(field.getType());
            if (oneAnn.foreignKey().length == 0) {
                solveForeignKeys.add(relationMetadata);
            } else {
                relationMetadata.setForeignKeyNames(List.of(oneAnn.foreignKey()));
            }
            madeRelations.add(relationMetadata);
            meta.getRelations().add(relationMetadata);
            columnMade = true;
        }
        if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany ann = field.getAnnotation(OneToMany.class);
            RelationMetadata relationMetadata = new RelationMetadata();

            assert ann != null;
            relationMetadata.setRelationName(!ann.relationName().isEmpty() ? ann.relationName() : field.getName());
            relationMetadata.setRelationType(RelationType.ONE_TO_MANY);
            relationMetadata.setForeignField(field);
            relationMetadata.setForeignClass(getListElementType(clazz, field, RelationType.ONE_TO_MANY));
            if (ann.foreignKey().length == 0) {
                solveForeignKeys.add(relationMetadata);
            } else {
                relationMetadata.setForeignKeyNames(List.of(ann.foreignKey()));
            }
            madeRelations.add(relationMetadata);
            meta.getRelations().add(relationMetadata);
            columnMade = true;
        }
        if (field.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne ann = field.getAnnotation(ManyToOne.class);
            RelationMetadata relationMetadata = new RelationMetadata();

            assert ann != null;
            relationMetadata.setRelationName(!ann.relationName().isEmpty() ? ann.relationName() : field.getName());
            relationMetadata.setRelationType(RelationType.MANY_TO_ONE);
            relationMetadata.setForeignField(field);
            relationMetadata.setForeignClass(field.getType());
            if (ann.foreignKey().length == 0) {
                solveForeignKeys.add(relationMetadata);
            }
            relationMetadata.setForeignKeyNames(List.of(ann.foreignKey()));
            madeRelations.add(relationMetadata);
            meta.getRelations().add(relationMetadata);
            columnMade = true;
        }
        if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany ann = field.getAnnotation(ManyToMany.class);
            RelationMetadata relationMetadata = new RelationMetadata();
            assert ann != null;
            relationMetadata.setRelationName(!ann.relationName().isEmpty() ? ann.relationName() : field.getName());
            relationMetadata.setRelationType(RelationType.MANY_TO_MANY);
            relationMetadata.setForeignField(field);
            relationMetadata.setForeignClass(getListElementType(clazz, field, RelationType.MANY_TO_MANY));
            if (ann.joinedTableName() == null) {
                throw new RequiredFieldException("ManyToMany " + field.getName() + " in " + clazz.getSimpleName() + " requires joined table name");
            }
            relationMetadata.setJoinedTableName(ann.joinedTableName());
            boolean solve = false;
            if (ann.myKey().length == 0) {
                solve = true;
            } else {
                relationMetadata.setMyJoinedTableFks(List.of(ann.myKey()));
            }
            if (ann.theirKey().length == 0) {
                solve = true;
            } else {
                relationMetadata.setForeignKeyNames(List.of(ann.theirKey()));
            }
            if (solve)
                solveForeignKeys.add(relationMetadata);

            madeRelations.add(relationMetadata);
            meta.getRelations().add(relationMetadata);
            columnMade = true;
        }

        //No @Column or @Relation annotations for this field
        if (!columnMade) {
            ColumnMetadata columnMeta = new ColumnMetadata();
            columnMeta.setColumnName(field.getName());
            columnMeta.setField(field);
            meta.getColumns().put(columnMeta.getColumnName(), columnMeta);
        }
    }

    //extract class type from list
    private Class<?> getListElementType(Class<?> clazz, Field field, RelationType relationType) {
        // 1. Must be assignable to List
        if (!List.class.isAssignableFrom(field.getType())) {
            throw new ListFieldRequiredException("Field " + field.getName() + " of class " +
                    clazz.getSimpleName() + " must be a list for " + relationType + " relation");
        }

        // 2. Must have generic type info
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType pType)) {
            throw new ListFieldRequiredException("Incorrect type for list for field " + field.getName() + " in class " + clazz.getSimpleName());
        }

        // 3. List has one type argument: List<X>
        Type[] typeArguments = pType.getActualTypeArguments();
        if (typeArguments.length != 1) {
            throw new ListFieldRequiredException("Only one type inside list for field: " + field.getName() + " in class " + clazz.getSimpleName() + " required");
        }

        Type elementType = typeArguments[0];

        // 4. Element must be a class
        if (elementType instanceof Class<?>) {
            return (Class<?>) elementType;
        }

        throw new RuntimeException("Getting list elements type failed!");
    }

    //fill foreign keys by default with primary keys of their related classes
    private void solveRelationWithoutFK(RelationMetadata relationMetadata) {
        EntityMetadata foreignEntity = MetadataStorage.get(relationMetadata.getForeignClass());
        if (relationMetadata.getForeignKeyNames() == null) {
            relationMetadata.setForeignKeyNames(new ArrayList<>());
            for (var pk : foreignEntity.getIdFields()) {
                relationMetadata.getForeignKeyNames().add(pk.getName());
            }
        }
        if (relationMetadata.getRelationType() == RelationType.MANY_TO_MANY) {
            EntityMetadata myEntity = MetadataStorage.get(relationMetadata.getForeignField().getDeclaringClass());
            if (relationMetadata.getMyJoinedTableFks() == null) {
                relationMetadata.setMyJoinedTableFks(new ArrayList<>());
                for (var pk : myEntity.getIdFields()) {
                    relationMetadata.getMyJoinedTableFks().add(pk.getName());
                }
            }
        }
    }
}
