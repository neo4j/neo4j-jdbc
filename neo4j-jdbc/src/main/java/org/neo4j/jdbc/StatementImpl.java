/*
 * Copyright (c) 2023-2025 "Neo4j,"
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
package org.neo4j.jdbc;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.bolt.connection.SummaryCounters;
import org.neo4j.cypherdsl.support.schema_name.SchemaNames;
import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.Neo4jTransaction.ResultSummary;
import org.neo4j.jdbc.Neo4jTransaction.RunAndPullResponses;
import org.neo4j.jdbc.events.Neo4jEvent;
import org.neo4j.jdbc.events.ResultSetListener;
import org.neo4j.jdbc.events.StatementListener;
import org.neo4j.jdbc.events.StatementListener.ExecutionEndedEvent;
import org.neo4j.jdbc.events.StatementListener.ExecutionStartedEvent;
import org.neo4j.jdbc.events.StatementListener.ExecutionStartedEvent.ExecutionMode;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Values;

import static org.neo4j.jdbc.Neo4jException.withCause;
import static org.neo4j.jdbc.Neo4jException.withReason;

non-sealed class StatementImpl implements Neo4jStatement {

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.statement");

	private static final Logger SQL_LOGGER = Logger.getLogger("org.neo4j.jdbc.statement.SQL");

	private static final Map<String, AtomicLong> ID_GENERATORS = new ConcurrentHashMap<>();

	static final int DEFAULT_BUFFER_SIZE_FOR_INCOMING_STREAMS = 4096;
	static final Charset DEFAULT_ASCII_CHARSET_FOR_INCOMING_STREAM = StandardCharsets.ISO_8859_1;

	private static final HexFormat HEX_FORMAT = HexFormat.of();

	private static final Base64.Encoder ENCODER = Base64.getEncoder();

	private final Connection connection;

	private final Neo4jTransactionSupplier transactionSupplier;

	private int fetchSize = DEFAULT_FETCH_SIZE;

	private int maxRows;

	private int maxFieldSize;

	protected ResultSetHolder resultSet;

	private int updateCount = -1;

	private boolean multipleResultsApi;

	private int queryTimeout;

	protected boolean poolable;

	private boolean closeOnCompletion;

	private boolean closed;

	private final UnaryOperator<String> sqlProcessor;

	private final Warnings warnings;

	private final AtomicBoolean resultSetAcquired = new AtomicBoolean(false);

	private final Map<String, Object> transactionMetadata = new ConcurrentHashMap<>();

	private final Consumer<Class<? extends Statement>> onClose;

	private final Set<StatementListener> listeners = new HashSet<>();

	StatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			UnaryOperator<String> sqlProcessor, Warnings localWarnings, Consumer<Class<? extends Statement>> onClose) {
		this.connection = Objects.requireNonNull(connection);
		this.transactionSupplier = Objects.requireNonNull(transactionSupplier);
		this.sqlProcessor = Objects.requireNonNullElseGet(sqlProcessor, UnaryOperator::identity);
		this.warnings = Objects.requireNonNullElseGet(localWarnings, Warnings::new);
		this.onClose = Objects.requireNonNullElse(onClose, type -> {
		});
	}

	/**
	 * This is for use with LocalStatement.
	 */
	StatementImpl() {
		this.connection = null;
		this.transactionSupplier = null;
		this.sqlProcessor = UnaryOperator.identity();
		this.warnings = new Warnings();
		this.onClose = type -> {
		};
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return executeQuery0(sql, true, Map.of());
	}

	protected final ResultSet executeQuery0(String sql, boolean applyProcessor, Map<String, Object> parameters)
			throws SQLException {
		assertIsOpen();
		closeResultSet();
		return recordEvent(sql, ExecutionMode.QUERY, context -> {
			this.updateCount = -1;
			this.multipleResultsApi = false;
			var processedSQL = applyProcessor ? processSQL(sql) : sql;
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.SQL_PROCESSED, context)));
			var transaction = this.transactionSupplier.getTransaction(this.transactionMetadata);
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.TRANSACTION_ACQUIRED, context)));
			var responses = runAndPull(transaction, processedSQL, parameters, context);
			this.resultSet = newResultSet(transaction, responses, Kind.DEFAULT);
			this.resultSetAcquired.set(false);
			return this.resultSet.value();
		});
	}

	private RunAndPullResponses runAndPull(Neo4jTransaction transaction, String processedSQL,
			Map<String, Object> parameters, Map<String, Object> context) throws SQLException {
		var finalFetchSize = (this.maxRows > 0) ? Math.min(this.maxRows, this.fetchSize) : this.fetchSize;
		var runAndPull = transaction.runAndPull(processedSQL, getParameters(parameters), finalFetchSize,
				this.queryTimeout);
		Events.notify(this.listeners,
				listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.RUN_AND_PULL_RESPONSE_ACQUIRED, context)));
		return runAndPull;
	}

	private ResultSetHolder newResultSet(Neo4jTransaction transaction, RunAndPullResponses responses, Kind kind) {
		var newResultSet = new ResultSetImpl(this, this.maxFieldSize, transaction, responses.runResponse(),
				responses.pullResponse(), this.fetchSize, this.maxRows);
		this.listeners.forEach(listener -> {
			if (listener instanceof ResultSetListener resultSetListener) {
				newResultSet.addListener(resultSetListener);
			}
		});
		return new ResultSetHolder(newResultSet, kind);
	}

	private ResultSetHolder newResultSet(List<Record> records, Kind kind) {

		var newResultSet = new ResultSetImpl(this, this.maxFieldSize, records);
		this.listeners.forEach(listener -> {
			if (listener instanceof ResultSetListener resultSetListener) {
				newResultSet.addListener(resultSetListener);
			}
		});
		return new ResultSetHolder(newResultSet, kind);
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		return executeUpdate(sql, Statement.NO_GENERATED_KEYS);
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Executing update with auto generated keys set to %d".formatted(autoGeneratedKeys));
		return executeUpdate0(sql, true, Map.of(), autoGeneratedKeys);
	}

	protected final int executeUpdate0(String sql, boolean applyProcessor, Map<String, Object> parameters,
			int autoGeneratedKeys) throws SQLException {
		assertIsOpen();
		assertAutoGeneratedKeys(autoGeneratedKeys);

		var records = new ArrayList<Record>();
		var returnGeneratedKeys = autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS;
		if (returnGeneratedKeys) {
			records.addAll(pullAllGeneratedKeys(this.resultSet));
		}
		closeResultSet();
		return recordEvent(sql, ExecutionMode.UPDATE, context -> {
			this.updateCount = -1;
			this.multipleResultsApi = false;
			var processedSQL = applyProcessor ? processSQL(sql) : sql;
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.SQL_PROCESSED, context)));
			var transaction = this.transactionSupplier.getTransaction(this.transactionMetadata);
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.TRANSACTION_ACQUIRED, context)));
			Optional<SummaryCounters> counters;
			if (returnGeneratedKeys) {
				var responses = runAndPull(transaction, processedSQL, parameters, context);
				var nextResultSet = newResultSet(transaction, responses, Kind.GENERATED_KEYS);
				if (records.isEmpty()) {
					this.resultSet = nextResultSet;
				}
				else {
					records.addAll(pullAllGeneratedKeys(nextResultSet));
					this.resultSet = newResultSet(records, Kind.GENERATED_KEYS);
				}
				this.resultSetAcquired.set(false);
				counters = responses.pullResponse().resultSummary().map(ResultSummary::counters);
			}
			else {
				var discardResponse = transaction.runAndDiscard(processedSQL, getParameters(parameters),
						this.queryTimeout, transaction.isAutoCommit());
				Events.notify(this.listeners,
						listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.DISCARD_RESPONSE_ACQUIRED, context)));
				counters = discardResponse.resultSummary().map(ResultSummary::counters);
			}

			return counters.map(StatementImpl::countUpdates).orElse(0);
		});
	}

	private static Integer countUpdates(SummaryCounters c) {
		var rowCount = c.nodesCreated() + c.nodesDeleted() + c.relationshipsCreated() + c.relationshipsDeleted();
		if (rowCount == 0 && c.containsUpdates()) {
			var labelsAndProperties = c.labelsAdded() + c.labelsRemoved() + c.propertiesSet();
			rowCount = (labelsAndProperties > 0) ? 1 : 0;
		}
		return rowCount;
	}

	private static List<Record> pullAllGeneratedKeys(ResultSetHolder holder) throws SQLException {
		var records = new ArrayList<Record>();
		if (holder != null && holder.kind() == Kind.GENERATED_KEYS) {
			try (var rs = holder.value().unwrap(Neo4jResultSet.class)) {
				while (rs.next()) {
					records.add(rs.getCurrentRecord());
				}
			}
		}
		return records;
	}

	@Override
	public void close() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Closing");
		if (this.closed) {
			return;
		}
		closeResultSet();
		this.closed = true;
		this.onClose.accept(this.getType());
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting max field size");
		assertIsOpen();
		return this.maxFieldSize;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting max field size to %d".formatted(max));
		assertIsOpen();
		if (max < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("max field size", max));
		}
		this.maxFieldSize = max;
	}

	@Override
	public int getMaxRows() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting max rows");
		assertIsOpen();
		return this.maxRows;
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting max rows to %d".formatted(max));
		assertIsOpen();
		if (max < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("max rows", max));
		}
		this.maxRows = max;
	}

	@Override
	public void setEscapeProcessing(boolean ignored) throws SQLException {
		LOGGER.log(Level.WARNING, () -> "Setting escape processing to %s (ignored)".formatted(ignored));
		assertIsOpen();
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting query timeout");
		assertIsOpen();
		return this.queryTimeout;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting query timeout to %d seconds".formatted(seconds));
		assertIsOpen();
		if (seconds < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("query timeout", seconds));
		}
		this.queryTimeout = seconds;
	}

	@Override
	public void cancel() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting warnings");
		assertIsOpen();
		return this.warnings.get();
	}

	@Override
	public void clearWarnings() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Clearing warnings");
		assertIsOpen();
		this.warnings.clear();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		return execute(sql, Statement.NO_GENERATED_KEYS);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Executing `%s` with auto generated keys set to %d".formatted(sql, autoGeneratedKeys));
		return execute0(sql, Map.of(), autoGeneratedKeys);
	}

	protected final boolean execute0(String sql, Map<String, Object> parameters, int autoGeneratedKeys)
			throws SQLException {
		assertIsOpen();
		assertAutoGeneratedKeys(autoGeneratedKeys);
		closeResultSet();
		return recordEvent(sql, ExecutionMode.PLAIN, context -> {
			this.updateCount = -1;
			this.multipleResultsApi = true;
			var processedSQL = processSQL(sql);
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.SQL_PROCESSED, context)));
			var transaction = this.transactionSupplier.getTransaction(this.transactionMetadata);
			Events.notify(this.listeners,
					listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.TRANSACTION_ACQUIRED, context)));
			var responses = runAndPull(transaction, processedSQL, parameters, context);
			this.updateCount = responses.pullResponse()
				.resultSummary()
				.map(summary -> summary.counters().totalCount())
				.filter(count -> count > 0)
				.orElse(-1);

			var containsUpdates = this.updateCount != -1;
			this.resultSet = newResultSet(transaction, responses,
					(!containsUpdates || (autoGeneratedKeys != Statement.RETURN_GENERATED_KEYS)) ? Kind.DEFAULT
							: Kind.GENERATED_KEYS);
			return !containsUpdates;
		});
	}

	static void assertAutoGeneratedKeys(int autoGeneratedKeys) throws SQLException {
		if (!(autoGeneratedKeys == Statement.NO_GENERATED_KEYS
				|| autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS)) {
			throw new Neo4jException(
					withReason("Invalid value %d for parameter `autoGeneratedKeys`".formatted(autoGeneratedKeys)));
		}
	}

	private <T> T recordEvent(String statement, ExecutionMode executionType, SqlCallable<T> callable)
			throws SQLException {

		if (this.listeners.isEmpty()) {
			return callable.call(Map.of());
		}

		var id = statementId();
		var s = System.nanoTime();
		var databaseURL = this.connection.unwrap(Neo4jConnection.class).getDatabaseURL();
		var startEvent = new ExecutionStartedEvent(id, databaseURL, getType(), executionType, statement);
		Events.notify(this.listeners, listener -> listener.onExecutionStarted(startEvent));

		var context = Map.<String, Object>of("source", getType(), "id", id);
		var state = ExecutionEndedEvent.State.FAILED;
		try {
			var result = callable.call(context);
			state = ExecutionEndedEvent.State.SUCCESSFUL;
			return result;
		}
		finally {
			final long e = System.nanoTime();
			var endEvent = new ExecutionEndedEvent(id, databaseURL, state, Duration.ofNanos(e - s));
			Events.notify(this.listeners, listener -> listener.onExecutionEnded(endEvent));
		}
	}

	private String statementId() {
		var type = getType().getSimpleName();
		return type + "@"
				+ HEX_FORMAT.formatHex(ENCODER.encode(Long
					.toString(ID_GENERATORS.computeIfAbsent(type, ignored -> new AtomicLong(0)).getAndIncrement())
					.getBytes(StandardCharsets.UTF_8)));
	}

	private static Map<String, Object> getParameters(Map<String, Object> parameters) throws SQLException {
		var result = Objects.requireNonNullElseGet(parameters, Map::<String, Object>of);
		for (Map.Entry<String, Object> entry : result.entrySet()) {
			if (entry.getValue() instanceof Reader reader) {
				try (reader) {
					StringBuilder buf = new StringBuilder();
					char[] buffer = new char[DEFAULT_BUFFER_SIZE_FOR_INCOMING_STREAMS];
					int len;
					while ((len = reader.read(buffer)) != -1) {
						buf.append(buffer, 0, len);
					}
					entry.setValue(Values.value(buf.toString()));
				}
				catch (IOException ex) {
					throw new Neo4jException(Neo4jException.withInternal(ex));
				}
			}
			else if (entry.getValue() instanceof InputStream inputStream) {
				try (var in = new BufferedInputStream(inputStream); var out = new ByteArrayOutputStream()) {
					in.transferTo(out);
					entry.setValue(Values.value(out.toByteArray()));
				}
				catch (IOException ex) {
					throw new Neo4jException(withCause(ex));
				}
			}
		}
		return result;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting result set");
		assertIsOpen();
		if (this.resultSet == null) {
			return null;
		}
		if (this.resultSet.kind() != Kind.DEFAULT) {
			throw new IllegalStateException("Only generated keys are available");
		}
		if (!this.resultSetAcquired.compareAndSet(false, true)) {
			throw new Neo4jException(withReason("Result set has already been acquired"));
		}
		return (this.multipleResultsApi && this.updateCount == -1) ? this.resultSet.value() : null;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting update count");
		assertIsOpen();
		return (this.multipleResultsApi) ? this.updateCount : -1;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting more results state");
		assertIsOpen();
		if (this.multipleResultsApi) {
			closeResultSet();
			this.updateCount = -1;
		}
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		LOGGER.log(Level.WARNING, () -> "Setting fetch direction to %d (ignored)".formatted(direction));
		assertIsOpen();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting fetch direction");
		assertIsOpen();
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting fetch size to %d".formatted(rows));
		assertIsOpen();
		if (rows < 0) {
			throw new Neo4jException(GQLError.$22N02.withTemplatedMessage("fetch size", rows));
		}
		this.fetchSize = (rows > 0) ? rows : DEFAULT_FETCH_SIZE;
	}

	@Override
	public int getFetchSize() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting fetch size");
		assertIsOpen();
		return this.fetchSize;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting result set concurrency");
		assertIsOpen();
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting result set type");
		assertIsOpen();
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void clearBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Connection getConnection() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting connection");
		assertIsOpen();
		return this.connection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting generated keys");
		assertIsOpen();
		if (this.resultSet == null || this.resultSet.kind() != Kind.GENERATED_KEYS) {
			throw new Neo4jException(withReason("Generated keys have not been returned"));
		}
		if (!this.resultSetAcquired.compareAndSet(false, true)) {
			throw new Neo4jException(withReason("Result set has already been acquired"));
		}
		return this.resultSet.value();
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
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting result set holdability");
		assertIsOpen();
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() {
		LOGGER.log(Level.FINER, () -> "Getting closed state");
		return this.closed;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting poolable to %s".formatted(poolable));
		assertIsOpen();
		this.poolable = poolable;
	}

	@Override
	public boolean isPoolable() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting poolable state");
		assertIsOpen();
		return this.poolable;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting close on completion to %s".formatted(true));
		assertIsOpen();
		this.closeOnCompletion = true;
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting close on completion state");
		assertIsOpen();
		return this.closeOnCompletion;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Unwrapping `%s` into `%s`".formatted(getClass().getCanonicalName(), iface.getCanonicalName()));
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
	}

	@Override
	public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
		LOGGER.log(Level.FINER,
				() -> "Enquoting identifier `%s` with always quoting set to %s".formatted(identifier, alwaysQuote));
		return SchemaNames.sanitize(identifier, alwaysQuote)
			.orElseThrow(() -> new Neo4jException(withReason("Cannot quote identifier " + identifier)));
	}

	protected void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new Neo4jException(withReason("The statement set is closed"));
		}
	}

	private void closeResultSet() throws SQLException {
		if (this.resultSet != null) {
			this.resultSet.value().close();
			this.resultSet = null;
			this.resultSetAcquired.set(false);
		}
	}

	protected final String processSQL(String sql) throws SQLException {
		try {
			var processedSQL = this.sqlProcessor.apply(sql);
			if (SQL_LOGGER.isLoggable(Level.FINE) && !processedSQL.equals(sql)) {
				SQL_LOGGER.log(Level.FINE, "Processed ''{0}'' into ''{1}''", new Object[] { sql, processedSQL });
			}
			return processedSQL;
		}
		catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException ex) {
			throw new Neo4jException(withCause(Optional.ofNullable(ex.getCause()).orElse(ex)));
		}
	}

	@Override
	public Neo4jStatement withMetadata(Map<String, Object> metadata) {
		LOGGER.log(Level.FINER, () -> "Adding new transaction metadata");
		if (metadata != null) {
			this.transactionMetadata.putAll(metadata);
		}
		return this;
	}

	@Override
	public void addListener(StatementListener statementListener) {
		this.listeners.add(Objects.requireNonNull(statementListener));
	}

	Class<? extends Statement> getType() {
		if (this instanceof CallableStatement) {
			return CallableStatement.class;
		}
		else if (this instanceof PreparedStatement) {
			return PreparedStatement.class;
		}
		return Statement.class;
	}

	@FunctionalInterface
	interface SqlCallable<V> {

		/**
		 * Computes a result, or throws an exception if unable to do so.
		 * @param context the context in which this method is called
		 * @return computed result
		 * @throws SQLException if unable to compute a result
		 */
		V call(Map<String, Object> context) throws SQLException;

	}

	/**
	 * Describes a result sets kind.
	 *
	 * @since 6.10.0
	 */
	protected enum Kind {

		/**
		 * Default result set, i.e. "normal" result data
		 */
		DEFAULT,
		/**
		 * Represents generated keys.
		 */
		GENERATED_KEYS

	}

	protected record ResultSetHolder(ResultSetImpl value, Kind kind) {
	}

}
