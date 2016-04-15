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
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;
import it.larusba.neo4j.jdbc.http.driver.Neo4jStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public class HttpConnection extends Connection implements Loggable {

    private boolean readOnly = false;
    private boolean isClosed = false;
    private boolean loggable = false;
    private Properties properties;
    private CypherExecutor executor;

    /**
     * Default constructor.
     *
     * @param host       Hostname of the Neo4j instance.
     * @param port       HTTP port of the Neo4j instance.
     * @param properties Properties of the url connection.
     * @throws SQLException
     */
    public HttpConnection(String host, Integer port, Properties properties) throws SQLException {
        this.properties = properties;
        this.executor = new CypherExecutor(host, port, properties);
    }

    /**
     * Execute a cypher query.
     *
     * @param query      Cypher query
     * @param parameters Parameter of the cypher query
     * @param stats      Do we need to include stats ?
     * @return
     * @throws SQLException
     */
    public Neo4jResponse executeQuery(final String query, Map<String, Object> parameters, Boolean stats) throws SQLException {
        checkClosed();
        checkReadOnly(query);
        return executor.executeQuery(new Neo4jStatement(query, parameters, stats));
    }

    /**
     * Check if can execute the query into the current mode (ie. readonly or not).
     * If we can't an SQLException is throw.
     *
     * @param query Cypher query
     * @throws SQLException
     */
    private void checkReadOnly(String query) throws SQLException {
        if (readOnly && isMutating(query)) {
            throw new SQLException("Mutating Query in readonly mode: " + query);
        }
    }

    /**
     * Detect some cypher keyword to know if this query mutated the graph.
     * /!\ This not enough now due to procedure procedure.
     *
     * @param query Cypher query
     * @return
     */
    private boolean isMutating(String query) {
        return query.matches("(?is).*\\b(create|relate|delete|set)\\b.*");
    }

    @Override
    public java.sql.Statement createStatement() throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
    }

    @Override
    public PreparedStatement prepareStatement(String cypher) throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, nativeSQL(cypher)), this.isLoggable());
    }

    @Override
    public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency) throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, nativeSQL(cypher)), this.isLoggable());
    }

    @Override
    public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        this.checkClosed();
        return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, nativeSQL(cypher)), this.isLoggable());
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        //TODO : make some query modification for some software
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.executor.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return this.executor.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        checkClosed();
        if (getAutoCommit()) {
            throw new SQLException("Commit called on auto-committed connection");
        }
        executor.commit();
    }

    @Override
    public void rollback() throws SQLException {
        checkClosed();
        if (this.getAutoCommit()) {
            throw new SQLException("Rollback called on auto-committed connection");
        }
        executor.rollback();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed;
    }

    /**
     * Check if this connection is closed or not.
     * If it's closed, then we throw a SQLException, otherwise we do nothing.
     *
     * @throws SQLException
     */
    private void checkClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLException("Connection is closed.");
        }
    }

    @Override
    public void close() throws SQLException {
        checkClosed();
        if(!this.getAutoCommit()) {
            executor.rollback();
        }
        executor.close();
        isClosed = true;
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
        this.checkClosed();
    }

    @Override
    public String getCatalog() throws SQLException {
        this.checkClosed();
        return "Default";
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        //nothing
    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isLoggable() {
        return loggable;
    }

    @Override
    public void setLoggable(boolean loggable) {
        this.loggable = loggable;
    }

    public static boolean hasDebug(Properties properties) {
        return "true".equalsIgnoreCase(properties.getProperty("debug", "false"));
    }
}
