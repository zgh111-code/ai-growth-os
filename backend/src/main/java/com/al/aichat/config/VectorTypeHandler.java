package com.al.aichat.config;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.List;

/**
 * MyBatis-Plus 类型处理器：Java float[] ↔ PostgreSQL VECTOR
 * 由 type-handlers-package 自动注册
 */
@MappedTypes(float[].class)
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, new PGvector(parameter));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        PGvector v = (PGvector) rs.getObject(columnName);
        return v != null ? v.toArray() : null;
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        PGvector v = (PGvector) rs.getObject(columnIndex);
        return v != null ? v.toArray() : null;
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        PGvector v = (PGvector) cs.getObject(columnIndex);
        return v != null ? v.toArray() : null;
    }
}
