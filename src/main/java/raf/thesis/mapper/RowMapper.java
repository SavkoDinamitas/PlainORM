package raf.thesis.mapper;

import java.sql.ResultSet;

public interface RowMapper {
    <T> T map(ResultSet rs, Class<T> clazz);
}
