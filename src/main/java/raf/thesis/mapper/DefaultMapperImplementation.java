package raf.thesis.mapper;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import raf.thesis.mapper.exceptions.ClassInstantiationException;
import raf.thesis.mapper.exceptions.ResultSetAccessException;
import raf.thesis.mapper.exceptions.TypeConversionException;
import raf.thesis.metadata.ColumnMetadata;
import raf.thesis.metadata.EntityMetadata;
import raf.thesis.metadata.storage.MetadataStorage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DefaultMapperImplementation implements RowMapper {
    @Override
    public <T> T map(ResultSet rs, Class<T> clazz) {
        EntityMetadata entityMetadata = MetadataStorage.get(clazz);

        if (entityMetadata == null) {
            //TODO: map custom objects from ResultSet
            return null;
        }

        //for each column in result set, find the designated field
        // and do the conversion to java datatype
        try{
            T instance = clazz.getDeclaredConstructor().newInstance();

            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsMeta.getColumnName(i);
                ColumnMetadata columnMetadata = entityMetadata.getColumns().get(columnName.toLowerCase());

                if (columnMetadata == null) {
                    // Column doesn't belong to this entity -> skip
                    //TODO: log some warning for skipping
                    continue;
                }

                Field field = columnMetadata.getField();
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                //convert primitive types to java wrappers for JDBC
                Object value = rs.getObject(columnName, javaPrimitiveTypes(fieldType));
                //populate field
                BeanUtils.setProperty(instance, fieldName, value);
            }

            return instance;
        }catch (SQLException  e) {
            throw new ResultSetAccessException(e.getMessage());
        }
        catch(IllegalAccessException | InvocationTargetException e){
            throw new TypeConversionException(e.getMessage());
        }
        catch (Exception e){
            throw new ClassInstantiationException(e.getMessage());
        }
    }

    private Class<?> javaPrimitiveTypes(Class<?> clazz) {
        Map<Class<?>, Class<?>> primitiveTypes = new HashMap<>();
        primitiveTypes.put(boolean.class, Boolean.class);
        primitiveTypes.put(byte.class, Byte.class);
        primitiveTypes.put(char.class, Character.class);
        primitiveTypes.put(short.class, Short.class);
        primitiveTypes.put(int.class, Integer.class);
        primitiveTypes.put(long.class, Long.class);
        primitiveTypes.put(float.class, Float.class);
        primitiveTypes.put(double.class, Double.class);
        return primitiveTypes.getOrDefault(clazz, clazz);
    }
}
