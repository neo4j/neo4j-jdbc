/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 17/02/16
 */
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.Connection;
import it.larusba.neo4j.jdbc.DatabaseMetaData;
import it.larusba.neo4j.jdbc.http.driver.CypherExecutor;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 */
public class HttpConnection extends Connection implements Loggable {

    private boolean readOnly = false;
    private boolean loggable = false;
    private Properties properties;
    private CypherExecutor executor;

    /**
     * Default constructor.
     *
     * @param host Hostname of the Neo4j instance.
     * @param port HTTP port of the Neo4j instance.
     * @param properties Properties of the url connection.
     */
    public HttpConnection(String host, Integer port, Properties properties) throws SQLException {
        this.properties = properties;
        this.executor = new CypherExecutor(host, port, properties);
    }

    @Override
    public java.sql.Statement createStatement() throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return null;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {

    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    @Override
    public void commit() throws SQLException {
        executor.commit();
    }

    @Override
    public void rollback() throws SQLException {
        executor.rollback();
    }

    @Override
    public void close() throws SQLException {
        executor.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override public DatabaseMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.readOnly;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {

    }

    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {

    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;
    }

    @Override
    public boolean isLoggable() {
        return false;
    }

    @Override
    public void setLoggable(boolean loggable) {

    }

    public static boolean hasDebug(Properties properties) {
        return "true".equalsIgnoreCase(properties.getProperty("debug", "false"));
    }
}
