/*
 * Copyright (c) 2023 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.internal.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.driver.jdbc.internal.bolt.response.SummaryCounters;

class StatementImpl implements Statement {

	static final int DEFAULT_FETCH_SIZE = 1000;

	private Connection connection;

	private final BoltConnection boltConnection;

	private final boolean autoCommit;

	private int fetchSize = DEFAULT_FETCH_SIZE;

	private ResultSet resultSet;

	private int queryTimeout;

	private boolean poolable;

	private boolean closeOnCompletion;

	private boolean closed;

	StatementImpl(BoltConnection boltConnection, boolean autoCommit) {
		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.autoCommit = autoCommit;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		if (this.resultSet != null) {
			this.resultSet.close();
		}
		var transactionType = this.autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var beginFuture = this.boltConnection
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false)
			.toCompletableFuture();
		var runFuture = this.boltConnection.run(sql, parameters(), false).toCompletableFuture();
		var pullFuture = this.boltConnection.pull(runFuture, this.fetchSize).toCompletableFuture();
		var joinedFuture = CompletableFuture.allOf(beginFuture, runFuture).thenCompose(ignored -> pullFuture);
		PullResponse pullResponse;
		try {
			pullResponse = (this.queryTimeout > 0) ? joinedFuture.get(this.queryTimeout, TimeUnit.SECONDS)
					: joinedFuture.get();
		}
		catch (InterruptedException | ExecutionException ex) {
			this.boltConnection.reset(true).toCompletableFuture().join();
			var cause = ex.getCause();
			throw new SQLException("An error occured when running the query", (cause != null) ? cause : ex);
		}
		catch (TimeoutException ignored) {
			this.boltConnection.reset(true).toCompletableFuture().join();
			throw new SQLTimeoutException("Query timeout has been exceeded");
		}
		this.resultSet = new ResultSetImpl(this, runFuture.join(), pullResponse, this.fetchSize);
		return this.resultSet;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		// this is prototyping only
		if (this.resultSet != null) {
			this.resultSet.close();
		}
		var transactionType = this.autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var beginFuture = this.boltConnection
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false)
			.toCompletableFuture();
		var runFuture = this.boltConnection.run(sql, parameters(), false).toCompletableFuture();
		var discardFuture = this.boltConnection.discard(-1, false).toCompletableFuture();
		var commitFuture = this.boltConnection.commit().toCompletableFuture();
		var joinedFuture = CompletableFuture.allOf(beginFuture, runFuture, discardFuture, commitFuture);
		joinedFuture.join();
		return discardFuture.join()
			.resultSummary()
			.map(ResultSummary::counters)
			.map(SummaryCounters::totalCount)
			.orElse(0);
	}

	@Override
	public void close() throws SQLException {
		if (this.closed) {
			return;
		}
		if (this.resultSet != null) {
			this.resultSet.close();
		}
		this.closed = true;
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {

	}

	@Override
	public int getMaxRows() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {

	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {

	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return this.queryTimeout;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		this.queryTimeout = Math.max(seconds, 0);
	}

	@Override
	public void cancel() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {

	}

	@Override
	public void setCursorName(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		return false;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return this.resultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return 0;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// this hint is not supported
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		this.fetchSize = (rows > 0) ? rows : DEFAULT_FETCH_SIZE;
	}

	@Override
	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public void addBatch(String sql) throws SQLException {

	}

	@Override
	public void clearBatch() throws SQLException {

	}

	@Override
	public int[] executeBatch() throws SQLException {
		return new int[0];
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return this.closed;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		this.poolable = poolable;
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return this.poolable;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		this.closeOnCompletion = true;
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return this.closeOnCompletion;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	BoltConnection getBoltConnection() {
		return this.boltConnection;
	}

	boolean isAutoCommit() {
		return this.autoCommit;
	}

	protected Map<String, Object> parameters() {
		return Collections.emptyMap();
	}

}
