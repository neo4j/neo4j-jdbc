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
 * Created on 15/4/2016
 */
package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.*;
import it.larusba.neo4j.jdbc.http.driver.CypherExecutor;
import it.larusba.neo4j.jdbc.http.driver.Neo4jResponse;
import it.larusba.neo4j.jdbc.http.driver.Neo4jStatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public class HttpConnection extends Connection implements Loggable {

	protected CypherExecutor executor;
	private boolean isClosed = false;
	private boolean loggable = false;

	/**
	 * Default constructor.
	 *
	 * @param host       Hostname of the Neo4j instance.
	 * @param port       HTTP port of the Neo4j instance.
	 * @param properties Properties of the url connection.
	 * @throws SQLException
	 */
	public HttpConnection(String host, Integer port, Properties properties) throws SQLException {
		super(properties, ResultSet.CLOSE_CURSORS_AT_COMMIT);
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

	@Override public DatabaseMetaData getMetaData() throws SQLException {
		return new HttpDatabaseMetaData(this);
	}

	/*------------------------------*/
	/*       Commit, rollback       */
	/*------------------------------*/

	@Override public void setAutoCommit(boolean autoCommit) throws SQLException {
		this.executor.setAutoCommit(autoCommit);
	}

	@Override public boolean getAutoCommit() throws SQLException {
		return this.executor.getAutoCommit();
	}

	@Override public void commit() throws SQLException {
		checkClosed();
		checkAutoCommit();
		executor.commit();
	}

	@Override public void rollback() throws SQLException {
		checkClosed();
		checkAutoCommit();
		executor.rollback();
	}

	/*-------------------------*/
	/*       Holdability       */
	/*-------------------------*/

	@Override public void setHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	/*------------------------------*/
	/*       Create Statement       */
	/*------------------------------*/

	@Override public java.sql.Statement createStatement() throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
	}

	@Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
	}

	@Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpStatement.class, new HttpStatement(this), this.isLoggable());
	}

	/*-------------------------------*/
	/*       Prepare Statement       */
	/*-------------------------------*/

	@Override public PreparedStatement prepareStatement(String cypher) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, cypher), this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency) throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, cypher), this.isLoggable());
	}

	@Override public PreparedStatement prepareStatement(String cypher, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		this.checkClosed();
		return InstanceFactory.debug(HttpPreparedStatement.class, new HttpPreparedStatement(this, cypher), this.isLoggable());
	}

	/*-------------------*/
	/*       Close       */
	/*-------------------*/

	@Override public boolean isClosed() throws SQLException {
		return isClosed;
	}

	@Override public void close() throws SQLException {
		if (!this.getAutoCommit() && executor.getOpenTransactionId() > 0) {
			executor.rollback();
		}
		executor.close();
		isClosed = true;
	}

	/*--------------------*/
	/*       Logger       */
	/*--------------------*/

	@Override public boolean isLoggable() {
		return loggable;
	}

	@Override public void setLoggable(boolean loggable) {
		this.loggable = loggable;
	}

}
