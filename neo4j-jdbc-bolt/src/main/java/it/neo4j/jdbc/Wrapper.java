package it.neo4j.jdbc;

import java.sql.SQLException;

/**
 * Created by marcofalcier on 03/02/16.
 */
public class Wrapper implements java.sql.Wrapper {
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
