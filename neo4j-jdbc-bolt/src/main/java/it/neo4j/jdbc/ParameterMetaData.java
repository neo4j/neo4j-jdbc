package it.neo4j.jdbc;

import java.sql.SQLException;

/**
 * Created by marcofalcier on 03/02/16.
 */
public class ParameterMetaData implements java.sql.ParameterMetaData {
    public int getParameterCount() throws SQLException {
        return 0;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public int isNullable(int param) throws SQLException {
        return 0;
    }

    public boolean isSigned(int param) throws SQLException {
        return false;
    }

    public int getPrecision(int param) throws SQLException {
        return 0;
    }

    public int getScale(int param) throws SQLException {
        return 0;
    }

    public int getParameterType(int param) throws SQLException {
        return 0;
    }

    public String getParameterTypeName(int param) throws SQLException {
        return null;
    }

    public String getParameterClassName(int param) throws SQLException {
        return null;
    }

    public int getParameterMode(int param) throws SQLException {
        return 0;
    }
}
