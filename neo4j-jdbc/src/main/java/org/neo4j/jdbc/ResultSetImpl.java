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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.Neo4jTransaction.PullResponse;
import org.neo4j.jdbc.Neo4jTransaction.RunResponse;
import org.neo4j.jdbc.events.Neo4jEvent;
import org.neo4j.jdbc.events.ResultSetListener;
import org.neo4j.jdbc.events.ResultSetListener.IterationDoneEvent;
import org.neo4j.jdbc.events.ResultSetListener.IterationStartedEvent;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.UncoercibleException;
import org.neo4j.jdbc.values.Value;

import static org.neo4j.jdbc.Neo4jException.withReason;

final class ResultSetImpl implements Neo4jResultSet {

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.result-set");

	/**
	 * A constant for the only holdability we support.
	 */
	static final int SUPPORTED_HOLDABILITY = ResultSet.CLOSE_CURSORS_AT_COMMIT;

	/**
	 * A constant for the only result type we support.
	 */
	static final int SUPPORTED_TYPE = ResultSet.TYPE_FORWARD_ONLY;

	/**
	 * A constant for the only concurrency we support.
	 */
	static final int SUPPORTED_CONCURRENCY = ResultSet.CONCUR_READ_ONLY;

	/**
	 * A constant for the only fetch direction we support.
	 */
	static final int SUPPORTED_FETCH_DIRECTION = ResultSet.FETCH_FORWARD;

	static final EnumSet<Type> NO_TO_STRING_SUPPORT = EnumSet.of(Type.NODE, Type.RELATIONSHIP, Type.PATH);

	private final StatementImpl statement;

	private final List<String> keys;

	private final int maxFieldSize;

	private final Cursor cursor;

	private Value value;

	private boolean closed;

	private final AtomicBoolean beforeFirst = new AtomicBoolean(true);

	private final AtomicReference<Boolean> first = new AtomicReference<>();

	private final AtomicBoolean last = new AtomicBoolean(false);

	private final AtomicBoolean afterLast = new AtomicBoolean(false);

	private final Set<ResultSetListener> listeners = new HashSet<>();

	private boolean openedEventFired;

	private boolean closedEventFired;

	ResultSetImpl(StatementImpl statement, int maxFieldSize, Neo4jTransaction transaction, RunResponse runResponse,
			PullResponse batchPullResponse, int fetchSize, int maxRowLimit) {
		this.statement = Objects.requireNonNull(statement);
		this.maxFieldSize = maxFieldSize;

		this.cursor = Cursor.of(Objects.requireNonNull(transaction), Objects.requireNonNull(runResponse),
				(maxRowLimit > 0) ? maxRowLimit : -1, fetchSize, Objects.requireNonNull(batchPullResponse),
				this::onNextBatch);

		var sampleRecord = this.cursor.getSampleRecord();
		this.keys = (sampleRecord != null) ? sampleRecord.keys() : runResponse.keys();
	}

	ResultSetImpl(StatementImpl statement, int maxFieldSize, List<Record> records) {
		this.statement = Objects.requireNonNull(statement);
		this.maxFieldSize = maxFieldSize;

		this.cursor = Cursor.of(records);

		var sampleRecord = this.cursor.getSampleRecord();
		this.keys = (sampleRecord != null) ? sampleRecord.keys() : List.of();
	}

	@Override
	public Record getCurrentRecord() {
		return this.cursor.getCurrentRecord();
	}

	@Override
	public void addListener(ResultSetListener resultSetListener) {
		this.listeners.add(Objects.requireNonNull(resultSetListener));
	}

	@Override
	public boolean next() throws SQLException {
		LOGGER.log(Level.FINER, () -> "next");
		if (this.closed) {
			throw new Neo4jException(withReason("This result set is closed"));
		}
		if (this.beforeFirst.compareAndSet(true, false) && !this.openedEventFired) {
			Events.notify(this.listeners, listener -> listener
				.onIterationStarted(new IterationStartedEvent(Long.toString(System.identityHashCode(this)))));
			this.openedEventFired = true;
		}
		var result = this.cursor.next();
		if (result) {
			// this.currentRecord = this.cursor.getCurrentRecord();
			if (!this.first.compareAndSet(null, true)) {
				this.first.compareAndSet(true, false);
			}
			this.last.compareAndSet(false, !this.cursor.isLast());
		}
		else {
			this.first.compareAndSet(true, false);
			this.last.compareAndSet(true, false);
			if (this.afterLast.compareAndSet(false, true) && this.openedEventFired && !this.closedEventFired) {
				Events.notify(this.listeners, listener -> listener
					.onIterationDone(new IterationDoneEvent(Long.toString(System.identityHashCode(this)), true)));
				this.closedEventFired = true;
			}
		}
		return result;
	}

	void onNextBatch() {
		Events.notify(this.listeners, listener -> listener.on(new Neo4jEvent(Neo4jEvent.Type.PULLED_NEXT_BATCH,
				Map.of("source", this.getClass(), "id", Long.toString(System.identityHashCode(this))))));
	}

	@Override
	public void close() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Closing");
		if (this.closed) {
			return;
		}
		this.cursor.close();
		if (this.openedEventFired && !this.closedEventFired) {
			Events.notify(this.listeners, listener -> listener.onIterationDone(
					new IterationDoneEvent(Long.toString(System.identityHashCode(this)), this.isAfterLast())));
			this.closedEventFired = true;
		}
		this.closed = true;
		if (this.statement.isCloseOnCompletion()) {
			this.statement.close();
		}
	}

	@Override
	public boolean wasNull() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting was null state");
		assertIsOpen();
		if (this.value == null) {
			throw new Neo4jException(withReason("No column has been read prior to this call"));
		}
		return Type.NULL.isTypeOf(this.value);
	}

	static void logGet(String type, int columnIndex) {
		LOGGER.log(Level.FINEST, () -> "Getting %s at %d".formatted(type, columnIndex));
	}

	static void logGet(String type, String columnLabel) {
		LOGGER.log(Level.FINEST, () -> "Getting %s at `%s`".formatted(type, columnLabel));
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		logGet("String", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToString(value, this.maxFieldSize));
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		logGet("Boolean", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToBoolean);
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		logGet("Byte", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToByte);
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		logGet("Short", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToShort);
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		logGet("Int", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToInteger);
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		logGet("Int", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToLong);
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		logGet("Int", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToFloat);
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		logGet("Int", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToDouble);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		LOGGER.log(Level.FINEST, () -> "Getting %s at %d with scale %d".formatted("BigDecimal", columnIndex, scale));
		return getValueByColumnIndex(columnIndex, v -> mapToBigDecimal(v, scale));
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		logGet("Bytes", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToBytes(value, this.maxFieldSize));
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		logGet("Date", columnIndex);
		return getValueByColumnIndex(columnIndex, Neo4jConversions::asDate);
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		logGet("Time", columnIndex);
		return getValueByColumnIndex(columnIndex, Neo4jConversions::asTime);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		logGet("Timestamp", columnIndex);
		return getValueByColumnIndex(columnIndex, Neo4jConversions::asTimestamp);
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		logGet("AsciiStream", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToAsciiStream(value, this.maxFieldSize));
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		logGet("UnicodeStream", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToBinaryStream(value, this.maxFieldSize));
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		logGet("String", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToString(value, this.maxFieldSize));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		logGet("Boolean", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToBoolean);
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		logGet("Byte", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToByte);
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		logGet("Short", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToShort);
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		logGet("Int", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToInteger);
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		logGet("Long", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToLong);
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		logGet("Float", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToFloat);
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		logGet("Double", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToDouble);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		LOGGER.log(Level.FINEST, () -> "Getting %s at `%s` with scale %d".formatted("BigDecimal", columnLabel, scale));
		return getValueByColumnLabel(columnLabel, v -> mapToBigDecimal(v, scale));
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		logGet("Bytes", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToBytes(value, this.maxFieldSize));
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		logGet("Date", columnLabel);
		return getValueByColumnLabel(columnLabel, Neo4jConversions::asDate);
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		logGet("Time", columnLabel);
		return getValueByColumnLabel(columnLabel, Neo4jConversions::asTime);
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		logGet("Timestamp", columnLabel);
		return getValueByColumnLabel(columnLabel, Neo4jConversions::asTimestamp);
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		logGet("AsciiStream", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToAsciiStream(value, this.maxFieldSize));
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		logGet("BinaryStream", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToBinaryStream(value, this.maxFieldSize));
	}

	@Override
	public SQLWarning getWarnings() {
		// warnings are not supported
		return null;
	}

	@Override
	public void clearWarnings() {
		// warnings are not supported
	}

	@Override
	public String getCursorName() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting meta data");
		var connection = this.statement.getConnection();
		return new ResultSetMetaDataImpl(connection.getSchema(), connection.getCatalog(), this.keys,
				this.cursor.getSampleRecord());
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		logGet("Object", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToObject(value, this.maxFieldSize));
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		logGet("Object", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToObject(value, this.maxFieldSize));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Finding column with label `%s`".formatted(columnLabel));
		assertIsOpen();
		var index = this.keys.indexOf(columnLabel);
		if (index == -1) {
			throw new Neo4jException(GQLError.$22N63.withTemplatedMessage(columnLabel));
		}
		return ++index;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		logGet("CharacterStream", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> mapToReader(value, this.maxFieldSize));
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		logGet("CharacterStream", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> mapToReader(value, this.maxFieldSize));
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		logGet("BigDecimal", columnIndex);
		return getValueByColumnIndex(columnIndex, v -> mapToBigDecimal(v, null));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		logGet("BigDecimal", columnLabel);
		return getValueByColumnLabel(columnLabel, v -> mapToBigDecimal(v, null));
	}

	@Override
	public boolean isBeforeFirst() {
		LOGGER.log(Level.FINER, () -> "Getting before first state");
		return this.beforeFirst.get();
	}

	@Override
	public boolean isAfterLast() {
		LOGGER.log(Level.FINER, () -> "Getting after last state");
		return this.afterLast.get();
	}

	@Override
	public boolean isFirst() {
		LOGGER.log(Level.FINER, () -> "Getting first state");
		return Boolean.TRUE.equals(this.first.get());
	}

	@Override
	public boolean isLast() {
		LOGGER.log(Level.FINER, () -> "Getting last state");
		return this.last.get();
	}

	@Override
	public void beforeFirst() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Moving before first");
		if (this.beforeFirst.compareAndSet(false, false)) {
			throw new SQLFeatureNotSupportedException(
					"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support beforeFirst after it has been iterated"
						.formatted(SUPPORTED_TYPE));
		}
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public void afterLast() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Moving after last");
		while (this.next()) {
			// Discard everything
		}
	}

	@Override
	public boolean first() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Moving to first");
		if (this.beforeFirst.compareAndSet(false, false)) {
			throw new SQLFeatureNotSupportedException(
					"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support first after it has been iterated"
						.formatted(SUPPORTED_TYPE));
		}
		return next();
	}

	@Override
	public boolean last() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Moving to last");
		if (this.afterLast.compareAndSet(true, true)) {
			throw new SQLFeatureNotSupportedException(
					"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support last after it has been fully iterated"
						.formatted(SUPPORTED_TYPE));
		}
		while (!isLast()) {
			this.next();
		}
		return true;
	}

	@Override
	public int getRow() {
		LOGGER.log(Level.FINER, () -> "Getting row");
		return this.cursor.getCurrentRowNum();
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		throw new SQLFeatureNotSupportedException(
				"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support absolute scrolling"
					.formatted(SUPPORTED_TYPE));
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		throw new SQLFeatureNotSupportedException(
				"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support relative scrolling"
					.formatted(SUPPORTED_TYPE));
	}

	@Override
	public boolean previous() throws SQLException {
		throw new SQLFeatureNotSupportedException(
				"This result set is of type TYPE_FORWARD_ONLY (%d) and does not support previous scrolling"
					.formatted(SUPPORTED_TYPE));
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		LOGGER.log(Level.WARNING, () -> "Setting fetch direction to %d (ignored)".formatted(direction));
		assertIsOpen();
		if (direction != SUPPORTED_FETCH_DIRECTION) {
			throw new SQLFeatureNotSupportedException("Only forward fetching is supported");
		}
	}

	@Override
	public int getFetchDirection() {
		LOGGER.log(Level.FINER, () -> "Getting fetch direction");
		return SUPPORTED_FETCH_DIRECTION;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		LOGGER.log(Level.FINER, () -> "Setting fetch size to %d".formatted(rows));
		this.cursor.setFetchSize((rows > 0) ? rows : Neo4jStatement.DEFAULT_FETCH_SIZE);
	}

	@Override
	public int getFetchSize() {
		LOGGER.log(Level.FINER, () -> "Getting fetch size");
		return this.cursor.getFetchSize();
	}

	@Override
	public int getType() {
		LOGGER.log(Level.FINER, () -> "Getting type");
		return SUPPORTED_TYPE;
	}

	@Override
	public int getConcurrency() {
		LOGGER.log(Level.FINER, () -> "Getting concurrency");
		return SUPPORTED_CONCURRENCY;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean rowInserted() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void insertRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void deleteRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void refreshRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		throw new SQLFeatureNotSupportedException(
				"This result sets concurrency is of type CONCUR_READ_ONLY (%d) and does not support moving to insert row"
					.formatted(SUPPORTED_CONCURRENCY));
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		throw new SQLFeatureNotSupportedException(
				"This result sets concurrency is of type CONCUR_READ_ONLY (%d) and does not support moving to current row"
					.formatted(SUPPORTED_CONCURRENCY));
	}

	@Override
	public Statement getStatement() {
		LOGGER.log(Level.FINER, () -> "Getting statement");
		return this.statement;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getObject(columnIndex);
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		logGet("Array", columnIndex);
		return getValueByColumnIndex(columnIndex, v -> ArrayImpl.of(this.statement.getConnection(), v));
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getObject(columnLabel);
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		logGet("Array", columnLabel);
		return getValueByColumnLabel(columnLabel, v -> ArrayImpl.of(this.statement.getConnection(), v));
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		logGet("Date", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> Neo4jConversions.asDate(value, cal));
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		logGet("Date", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> Neo4jConversions.asDate(value, cal));
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		logGet("Time", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> Neo4jConversions.asTime(value, cal));
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		logGet("Time", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> Neo4jConversions.asTime(value, cal));
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		logGet("Timestamp", columnIndex);
		return getValueByColumnIndex(columnIndex, value -> Neo4jConversions.asTimestamp(value, cal));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		logGet("Timestamp", columnLabel);
		return getValueByColumnLabel(columnLabel, value -> Neo4jConversions.asTimestamp(value, cal));
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		logGet("URL", columnIndex);
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToUrl);
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		logGet("URL", columnLabel);
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToUrl);
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getHoldability() {
		LOGGER.log(Level.FINER, () -> "Getting holdability");
		return SUPPORTED_HOLDABILITY;
	}

	@Override
	public boolean isClosed() {
		LOGGER.log(Level.FINER, () -> "Getting closed state");
		return this.closed;
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		logGet("Object", columnIndex);
		return getValueByColumnIndex(columnIndex, valueMapperFor(type, this.maxFieldSize));
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		logGet("Object", columnLabel);
		return getValueByColumnLabel(columnLabel, valueMapperFor(type, this.maxFieldSize));
	}

	private static <T> ValueMapper<T> valueMapperFor(Class<T> type, int maxFieldSize) {
		return value -> {
			if (type.isInstance(value)) {
				return type.cast(value);
			}
			var optionalJSONMapper = JSONMappers.INSTANCE.getMapper(type.getName());
			return optionalJSONMapper.map(mapper -> {
				Object json = mapper.toJson(value);
				try {
					return type.cast(json);
				}
				catch (ClassCastException ex) {
					throw new RuntimeException(
							"Resulting type after mapping is incompatible, use %s or %s for reification"
								.formatted(json.getClass().getName(), mapper.getBaseType().getName()));
				}
			}).or(() -> {
				var obj = mapToObject(value, maxFieldSize);
				if (type.isInstance(obj)) {
					return Optional.of(type.cast(obj));
				}
				return Optional.empty();
			})
				.orElseThrow(() -> new Neo4jException(
						GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), type.getName())));
		};
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

	private void assertCurrentRecordIsNotNull() throws SQLException {
		if (this.getCurrentRecord() == null) {
			throw new Neo4jException(withReason("Invalid cursor position"));
		}
	}

	private void assertIsOpen() throws SQLException {
		if (this.closed) {
			throw new Neo4jException(withReason("The result set is closed"));
		}
	}

	private void assertColumnIndexIsPresent(int columnIndex) throws SQLException {
		if (columnIndex < 1 || columnIndex > this.getCurrentRecord().size()) {
			throw new Neo4jException(withReason("Invalid column index value"));
		}
	}

	private void assertColumnLabelIsPresent(String columnLabel) throws SQLException {
		if (!this.getCurrentRecord().containsKey(columnLabel)) {
			throw new Neo4jException(withReason("Invalid column label value"));
		}
	}

	private <T> T getValueByColumnIndex(int columnIndex, ValueMapper<T> valueMapper) throws SQLException {
		assertIsOpen();
		assertCurrentRecordIsNotNull();
		assertColumnIndexIsPresent(columnIndex);
		columnIndex--;
		this.value = this.getCurrentRecord().get(columnIndex);
		return valueMapper.map(this.value);
	}

	private <T> T getValueByColumnLabel(String columnLabel, ValueMapper<T> valueMapper) throws SQLException {
		assertIsOpen();
		assertCurrentRecordIsNotNull();
		assertColumnLabelIsPresent(columnLabel);
		this.value = this.getCurrentRecord().get(columnLabel);
		return valueMapper.map(this.value);
	}

	private static String mapToString(Value value, int maxFieldSize) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			return truncate(value.asString(), maxFieldSize);
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}

		try {
			if (NO_TO_STRING_SUPPORT.stream().anyMatch(t -> t.isTypeOf(value))) {
				throw new UncoercibleException(value.type().name(), "String");
			}
			return value.toString();
		}
		catch (UncoercibleException ex) {
			throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "String"));
		}
	}

	// Go home, Sonar, you are drunk.
	// URL is part of the JDBC API.
	@SuppressWarnings("squid:S1874")
	private static URL mapToUrl(Value value) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			try {
				return new URL(value.asString());
			}
			catch (MalformedURLException ex) {
				throw new Neo4jException(
						GQLError.$22N37.causedBy(ex).withTemplatedMessage(value.toDisplayString(), "URL"));
			}
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "URL"));
	}

	private static boolean mapToBoolean(Value value) throws SQLException {
		if (Type.BOOLEAN.isTypeOf(value)) {
			return value.asBoolean();
		}
		if (Type.NULL.isTypeOf(value)) {
			return false;
		}
		if (Type.INTEGER.isTypeOf(value)) {
			var number = value.asNumber().longValue();
			if (number == 0) {
				return false;
			}
			else if (number == 1) {
				return true;
			}
			else {
				throw new Neo4jException(GQLError.$22N37
					.withMessage("Number values can not be mapped to boolean aside from 0 and 1 values"));
			}
		}
		if (Type.STRING.isTypeOf(value)) {
			var string = value.asString();
			if ("0".equals(string)) {
				return false;
			}
			else if ("1".equals(string)) {
				return true;
			}
			else {
				throw new Neo4jException(GQLError.$22N37
					.withMessage("String values can not be mapped to boolean aside from '0' and '1' values"));
			}
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "boolean"));
	}

	private static Byte mapToByte(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Byte.MIN_VALUE && longValue <= Byte.MAX_VALUE) {
				return (byte) longValue;
			}
			throw new Neo4jException(GQLError.$22003.withTemplatedMessage(longValue));
		}
		if (Type.NULL.isTypeOf(value)) {
			return (byte) 0;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "byte"));
	}

	private static Short mapToShort(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
				return (short) longValue;
			}
			throw new Neo4jException(GQLError.$22003.withTemplatedMessage(longValue));
		}
		if (Type.NULL.isTypeOf(value)) {
			return (short) 0;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "short"));

	}

	private static int mapToInteger(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
				return (int) longValue;
			}
			throw new Neo4jException(GQLError.$22003.withTemplatedMessage(longValue));
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0;
		}
		if (Type.STRING.isTypeOf(value)) {
			try {
				return Integer.parseInt(value.asString());
			}
			catch (NumberFormatException ex) {
				throw new Neo4jException(
						GQLError.$22N37.causedBy(ex).withTemplatedMessage(value.toDisplayString(), "int"));
			}
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "int"));
	}

	private static long mapToLong(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			return value.asNumber().longValue();
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0L;
		}
		if (Type.STRING.isTypeOf(value)) {
			try {
				return Long.parseLong(value.asString());
			}
			catch (NumberFormatException ex) {
				throw new Neo4jException(
						GQLError.$22N37.causedBy(ex).withTemplatedMessage(value.toDisplayString(), "long"));
			}
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "long"));
	}

	private static float mapToFloat(Value value) throws SQLException {
		if (Type.FLOAT.isTypeOf(value)) {
			var doubleValue = value.asNumber().doubleValue();
			var floatValue = (float) doubleValue;
			if (Double.compare(doubleValue, floatValue) == 0) {
				return floatValue;
			}
			throw new Neo4jException(GQLError.$22003.withTemplatedMessage(value));
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0.0f;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "float"));
	}

	private static double mapToDouble(Value value) throws SQLException {
		if (Type.FLOAT.isTypeOf(value)) {
			return value.asNumber().doubleValue();
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0.0;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "double"));
	}

	@SuppressWarnings("squid:S1168")
	private static byte[] mapToBytes(Value value, int maxFieldSize) throws SQLException {
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		if (Type.BYTES.isTypeOf(value)) {
			return truncate(value.asByteArray(), maxFieldSize);
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "byte array"));
	}

	private static Reader mapToReader(Value value, int maxFieldSize) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			return new StringReader(truncate(value.asString(), maxFieldSize));
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "a reader"));
	}

	@SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
	private static BigDecimal mapToBigDecimal(Value value, Integer scale) throws SQLException {

		try {
			var result = switch (value.type()) {
				case STRING -> new BigDecimal(value.asString());
				case INTEGER -> BigDecimal.valueOf(value.asLong());
				case FLOAT -> BigDecimal.valueOf(value.asDouble());
				case NULL -> null;
				default -> throw new Neo4jException(
						GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.math.BigDecimal"));
			};

			if (result != null && scale != null) {
				return result.setScale(scale);
			}
			return result;
		}
		catch (NumberFormatException | ArithmeticException ex) {
			throw new Neo4jException(
					GQLError.$22N37.causedBy(ex).withTemplatedMessage(value.toDisplayString(), "java.math.BigDecimal"));
		}
	}

	private static InputStream mapToAsciiStream(Value value, int maxFieldSize) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			return new ByteArrayInputStream(
					truncate(value.asString(), maxFieldSize).getBytes(StandardCharsets.US_ASCII));
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.io.InputStream"));
	}

	private static InputStream mapToBinaryStream(Value value, int maxFieldSize) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			return new ByteArrayInputStream(truncate(value.asString(), maxFieldSize).getBytes(StandardCharsets.UTF_8));
		}
		if (Type.BYTES.isTypeOf(value)) {
			return new ByteArrayInputStream(truncate(value.asByteArray(), maxFieldSize));
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		throw new Neo4jException(GQLError.$22N37.withTemplatedMessage(value.toDisplayString(), "java.io.InputStream"));
	}

	private static Object mapToObject(Value value, int maxFieldSize) {
		if (Type.STRING.isTypeOf(value)) {
			return truncate(value.asString(), maxFieldSize);
		}
		if (Type.BYTES.isTypeOf(value)) {
			return truncate(value.asByteArray(), maxFieldSize);
		}
		return value.asObject();
	}

	private static String truncate(String string, int limit) {
		if (limit > 0) {
			var bytes = string.getBytes(StandardCharsets.UTF_8);
			if (bytes.length > limit) {
				string = new String(truncateBytes(bytes, limit), StandardCharsets.UTF_8);
			}
		}
		return string;
	}

	private static byte[] truncate(byte[] bytes, int limit) {
		return (limit > 0 && bytes.length > limit) ? truncateBytes(bytes, limit) : bytes;
	}

	private static byte[] truncateBytes(byte[] bytes, int limit) {
		var truncatedBytes = new byte[limit];
		System.arraycopy(bytes, 0, truncatedBytes, 0, limit);
		return truncatedBytes;
	}

	@FunctionalInterface
	private interface ValueMapper<T> {

		T map(Value value) throws SQLException;

	}

}
