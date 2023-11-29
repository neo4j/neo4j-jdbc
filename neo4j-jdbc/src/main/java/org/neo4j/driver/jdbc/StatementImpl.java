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

import org.neo4j.cypherdsl.support.schema_name.SchemaNames;
import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.response.DiscardResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.SummaryCounters;

class StatementImpl implements Statement {

	static final int DEFAULT_FETCH_SIZE = 1000;

	private final Connection connection;

	private final BoltConnection boltConnection;

	private final boolean autoCommit;

	private int fetchSize = DEFAULT_FETCH_SIZE;

	private int maxRows = 0;

	private int maxFieldSize = 0;

	private ResultSet resultSet;

	private int updateCount = -1;

	private boolean multipleResultsApi = false;

	private int queryTimeout;

	private boolean poolable;

	private boolean closeOnCompletion;

	private boolean closed;

	StatementImpl(Connection connection, BoltConnection boltConnection, boolean autoCommit) {
		this.connection = Objects.requireNonNull(connection);
		this.boltConnection = Objects.requireNonNull(boltConnection);
		this.autoCommit = autoCommit;
	}

	/**
	 * This is for use with LocalStatement.
	 */
	StatementImpl() {
		this.connection = null;
		this.boltConnection = null;
		this.autoCommit = false;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		assertIsOpen();
		closeResultSet();
		this.updateCount = -1;
		this.multipleResultsApi = false;
		var queryResponses = sendQuery(sql, true);
		this.resultSet = new ResultSetImpl(this, queryResponses.runResponse(), queryResponses.pullResponse(),
				this.fetchSize, this.maxRows, this.maxFieldSize);
		return this.resultSet;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		assertIsOpen();
		closeResultSet();
		this.updateCount = -1;
		this.multipleResultsApi = false;
		return sendQuery(sql, false).discardResponse()
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
		closeResultSet();
		this.closed = true;
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		assertIsOpen();
		return this.maxFieldSize;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		assertIsOpen();
		if (max < 0) {
			throw new SQLException("Max field size can not be negative.");
		}
		this.maxFieldSize = max;
	}

	@Override
	public int getMaxRows() throws SQLException {
		assertIsOpen();
		return this.maxRows;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		assertIsOpen();
		if (max < 0) {
			throw new SQLException("Max rows can not be negative.");
		}
		this.maxRows = max;
	}

	@Override
	public void setEscapeProcessing(boolean ignored) throws SQLException {
		assertIsOpen();
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		assertIsOpen();
		return this.queryTimeout;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		assertIsOpen();
		if (seconds < 0) {
			throw new SQLException("Query timeout can not be negative.");
		}
		this.queryTimeout = seconds;
	}

	@Override
	public void cancel() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		assertIsOpen();
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
		assertIsOpen();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		assertIsOpen();
		closeResultSet();
		this.updateCount = -1;
		this.multipleResultsApi = true;
		var queryResponses = sendQuery(sql, true);
		var pullResponse = queryResponses.pullResponse();
		this.resultSet = new ResultSetImpl(this, queryResponses.runResponse(), pullResponse, this.fetchSize,
				this.maxRows, this.maxFieldSize);
		this.updateCount = pullResponse.resultSummary()
			.map(summary -> summary.counters().totalCount())
			.filter(count -> count > 0)
			.orElse(-1);
		return this.updateCount == -1;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		assertIsOpen();
		return (this.multipleResultsApi && this.updateCount == -1) ? this.resultSet : null;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		assertIsOpen();
		return (this.multipleResultsApi) ? this.updateCount : -1;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		assertIsOpen();
		if (this.multipleResultsApi) {
			closeResultSet();
			this.updateCount = -1;
		}
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		assertIsOpen();
		// this hint is not supported
	}

	@Override
	public int getFetchDirection() throws SQLException {
		assertIsOpen();
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		assertIsOpen();
		if (rows < 0) {
			throw new SQLException("Fetch size can not be negative.");
		}
		this.fetchSize = (rows > 0) ? rows : DEFAULT_FETCH_SIZE;
	}

	@Override
	public int getFetchSize() throws SQLException {
		assertIsOpen();
		return this.fetchSize;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		assertIsOpen();
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		assertIsOpen();
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new SQLException("Not supported");
	}

	@Override
	public void clearBatch() throws SQLException {
		throw new SQLException("Not supported");
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new SQLException("Not supported");
	}

	@Override
	public Connection getConnection() throws SQLException {
		assertIsOpen();
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
		assertIsOpen();
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		assertIsOpen();
		this.poolable = poolable;
	}

	@Override
	public boolean isPoolable() throws SQLException {
		assertIsOpen();
		return this.poolable;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		assertIsOpen();
		this.closeOnCompletion = true;
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		assertIsOpen();
		return this.closeOnCompletion;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new SQLException("This object does not implement the given interface.");
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
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

	@Override
	public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
		return SchemaNames.sanitize(identifier, alwaysQuote)
			.orElseThrow(() -> new SQLException("Cannot quote identifier " + identifier));
	}

	private void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new SQLException("The statement set is closed");
		}
	}

	private void closeResultSet() throws SQLException {
		if (this.resultSet != null) {
			this.resultSet.close();
			this.resultSet = null;
		}
	}

	private QueryResponses sendQuery(String sql, boolean pull) throws SQLException {
		var transactionType = this.autoCommit ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		var beginFuture = this.boltConnection
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false)
			.toCompletableFuture();
		var runFuture = this.boltConnection.run(sql, parameters(), false).toCompletableFuture();
		CompletableFuture<QueryResponses> joinedFuture;

		if (pull) {
			var fetchSize = (this.maxRows > 0) ? Math.min(this.maxRows, this.fetchSize) : this.fetchSize;
			var pullFuture = this.boltConnection.pull(runFuture, fetchSize).toCompletableFuture();
			joinedFuture = CompletableFuture.allOf(beginFuture, runFuture)
				.thenCompose(ignored -> pullFuture)
				.thenApply(pullResponse -> new QueryResponses(runFuture.join(), pullResponse, null));
		}
		else {
			var discardFuture = this.boltConnection.discard(-1, false).toCompletableFuture();
			var commitFuture = this.boltConnection.commit().toCompletableFuture();
			joinedFuture = CompletableFuture.allOf(beginFuture, runFuture, discardFuture, commitFuture)
				.thenApply(ignored -> new QueryResponses(runFuture.join(), null, discardFuture.join()));
		}

		try {
			return (this.queryTimeout > 0) ? joinedFuture.get(this.queryTimeout, TimeUnit.SECONDS) : joinedFuture.get();
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
	}

	private record QueryResponses(RunResponse runResponse, PullResponse pullResponse, DiscardResponse discardResponse) {
	}

}
