package raf.thesis.metadata.storage;

import raf.thesis.metadata.EntityMetadata;

import java.util.HashMap;
import java.util.Map;

public class MetadataStorage {
    private static final Map<Class<?>, EntityMetadata> ENTITIES = new HashMap<>();

    public static void register(EntityMetadata metadata) {
        ENTITIES.put(metadata.getEntityClass(), metadata);
    }
    public static EntityMetadata get(Class<?> clazz) {
        return ENTITIES.get(clazz);
    }
    public static boolean contains(Class<?> clazz) {
        return ENTITIES.containsKey(clazz);
    }
    public static Map<Class<?>, EntityMetadata> getAllData(){
        return ENTITIES;
    }
    public static void removeAll(){
        ENTITIES.clear();
    }
}
