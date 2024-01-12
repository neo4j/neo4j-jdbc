/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.neo4j.cypherdsl.support.schema_name.SchemaNames;
import org.neo4j.driver.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.driver.jdbc.internal.bolt.response.SummaryCounters;

class StatementImpl implements Statement {

	static final int DEFAULT_FETCH_SIZE = 1000;

	// Adding the comment /*+ NEO4J FORCE_CYPHER */ to your Cypher statement will make the
	// JDBC driver opt-out from translating it to Cypher, even if the driver has been
	// configured for automatic translation.
	private static final Pattern PATTERN_ENFORCE_CYPHER = Pattern
		.compile("(['`\"])?[^'`\"]*/\\*\\+ NEO4J FORCE_CYPHER \\*/[^'`\"]*(['`\"])?");

	private static final Logger LOGGER = Logger.getLogger(StatementImpl.class.getCanonicalName());

	private final Connection connection;

	private final Neo4jTransactionSupplier transactionSupplier;

	private int fetchSize = DEFAULT_FETCH_SIZE;

	private int maxRows;

	private int maxFieldSize;

	private ResultSet resultSet;

	private int updateCount = -1;

	private boolean multipleResultsApi;

	private int queryTimeout;

	protected boolean poolable;

	private boolean closeOnCompletion;

	private boolean closed;

	private final UnaryOperator<String> sqlProcessor;

	StatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			UnaryOperator<String> sqlProcessor) {
		this.connection = Objects.requireNonNull(connection);
		this.transactionSupplier = Objects.requireNonNull(transactionSupplier);
		this.sqlProcessor = Objects.requireNonNullElseGet(sqlProcessor, UnaryOperator::identity);
	}

	/**
	 * This is for use with LocalStatement.
	 */
	StatementImpl() {
		this.connection = null;
		this.transactionSupplier = null;
		this.sqlProcessor = UnaryOperator.identity();
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		assertIsOpen();
		closeResultSet();
		this.updateCount = -1;
		this.multipleResultsApi = false;
		var transaction = this.transactionSupplier.getTransaction();
		sql = processSQL(sql);
		var fetchSize = (this.maxRows > 0) ? Math.min(this.maxRows, this.fetchSize) : this.fetchSize;
		var runAndPull = transaction.runAndPull(sql, parameters(), fetchSize, this.queryTimeout);
		this.resultSet = new ResultSetImpl(this, transaction, runAndPull.runResponse(), runAndPull.pullResponse(),
				this.fetchSize, this.maxRows, this.maxFieldSize);
		return this.resultSet;
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		assertIsOpen();
		closeResultSet();
		this.updateCount = -1;
		this.multipleResultsApi = false;
		var transaction = this.transactionSupplier.getTransaction();
		sql = processSQL(sql);
		return transaction.runAndDiscard(sql, parameters(), this.queryTimeout, transaction.isAutoCommit())
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
		var transaction = this.transactionSupplier.getTransaction();
		sql = processSQL(sql);
		var fetchSize = (this.maxRows > 0) ? Math.min(this.maxRows, this.fetchSize) : this.fetchSize;
		var runAndPull = transaction.runAndPull(sql, parameters(), fetchSize, this.queryTimeout);
		var pullResponse = runAndPull.pullResponse();
		this.resultSet = new ResultSetImpl(this, transaction, runAndPull.runResponse(), pullResponse, this.fetchSize,
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

	/**
	 * This extension method can be used for any derived statement implementations to
	 * supply parameters to {@link Neo4jTransaction#runAndPull(String, Map, int, int)} and
	 * {@link Neo4jTransaction#runAndDiscard(String, Map, int)}.
	 * @return parameters to this statement if any
	 */
	protected Map<String, Object> parameters() {
		return Collections.emptyMap();
	}

	@Override
	public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
		return SchemaNames.sanitize(identifier, alwaysQuote)
			.orElseThrow(() -> new SQLException("Cannot quote identifier " + identifier));
	}

	protected void assertIsOpen() throws SQLException {
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

	private String processSQL(String sql) {
		var processor = forceCypher(sql) ? UnaryOperator.<String>identity() : this.sqlProcessor;
		var processedSQL = processor.apply(sql);
		if (LOGGER.isLoggable(Level.FINEST) && !processedSQL.equals(sql)) {
			LOGGER.log(Level.FINEST, "Processed {0} into {1}", new Object[] { sql, processedSQL });
		}
		return processedSQL;
	}

	static boolean forceCypher(String sql) {
		var matcher = PATTERN_ENFORCE_CYPHER.matcher(sql);
		while (matcher.find()) {
			if (matcher.group(1) != null && matcher.group(1).equals(matcher.group(2))) {
				continue;
			}
			return true;
		}
		return false;
	}

}
