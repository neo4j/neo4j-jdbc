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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.driver.jdbc.internal.bolt.values.Record;
import org.neo4j.driver.jdbc.internal.bolt.values.Type;
import org.neo4j.driver.jdbc.internal.bolt.values.Value;

final class ResultSetImpl implements ResultSet {

	private final StatementImpl statement;

	private final BoltConnection boltConnection;

	private final RunResponse runResponse;

	private PullResponse pullResponse;

	private int fetchSize;

	private Iterator<Record> recordInterator;

	private Record currentRecord;

	private boolean closed;

	ResultSetImpl(StatementImpl statement, RunResponse runResponse, PullResponse pullResponse, int fetchSize) {
		this.statement = Objects.requireNonNull(statement);
		this.boltConnection = Objects.requireNonNull(this.statement.getBoltConnection());
		this.runResponse = Objects.requireNonNull(runResponse);
		this.pullResponse = Objects.requireNonNull(pullResponse);
		this.fetchSize = (fetchSize > 0) ? fetchSize : StatementImpl.DEFAULT_FETCH_SIZE;
		this.recordInterator = pullResponse.records().iterator();
	}

	@Override
	public boolean next() throws SQLException {
		if (this.closed) {
			throw new SQLException("This result set is closed.");
		}
		if (this.recordInterator.hasNext()) {
			this.currentRecord = this.recordInterator.next();
			return true;
		}
		if (this.pullResponse.hasMore()) {
			this.pullResponse = this.boltConnection.pull(this.runResponse, this.fetchSize).toCompletableFuture().join();
			this.recordInterator = this.pullResponse.records().iterator();
			return next();
		}
		this.currentRecord = null;
		return false;
	}

	@Override
	public void close() throws SQLException {
		if (this.closed) {
			return;
		}
		var autocommit = this.statement.isAutoCommit();
		var flush = !autocommit;
		var discardFuture = (this.pullResponse.hasMore())
				? this.boltConnection.discard(this.runResponse, -1, flush).toCompletableFuture()
				: CompletableFuture.completedFuture(null);
		var commitStage = autocommit ? this.boltConnection.commit().toCompletableFuture()
				: CompletableFuture.completedFuture(null);
		CompletableFuture.allOf(discardFuture, commitStage).join();
		if (this.statement.isCloseOnCompletion()) {
			this.statement.close();
		}
		this.closed = true;
	}

	@Override
	public boolean wasNull() throws SQLException {
		return false;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToString);
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToBoolean);
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToByte);
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToShort);
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToInteger);
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToLong);
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToFloat);
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToDouble);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return getValueByColumnIndex(columnIndex, ResultSetImpl::mapToBytes);
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToString);
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToBoolean);
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToByte);
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToShort);
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToInteger);
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToLong);
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToFloat);
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToDouble);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return getValueByColumnLabel(columnLabel, ResultSetImpl::mapToBytes);
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {

	}

	@Override
	public String getCursorName() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		return 0;
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return null;
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return null;
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isFirst() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean isLast() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void beforeFirst() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void afterLast() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean first() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean last() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean previous() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// this hint is not supported
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		this.fetchSize = (rows > 0) ? rows : StatementImpl.DEFAULT_FETCH_SIZE;
	}

	@Override
	public int getFetchSize() throws SQLException {
		return this.fetchSize;
	}

	@Override
	public int getType() throws SQLException {
		return TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() throws SQLException {
		return CONCUR_READ_ONLY;
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
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Statement getStatement() throws SQLException {
		return this.statement;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return null;
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		throw new SQLFeatureNotSupportedException();
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
	public int getHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
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
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	private void verifyCursorPosition() throws SQLException {
		if (this.currentRecord == null) {
			throw new SQLException("Invalid cursor position");
		}
	}

	private void verifyColumnIndex(int columnIndex) throws SQLException {
		if (columnIndex < 1 || columnIndex > this.currentRecord.size()) {
			throw new SQLException("Invalid column index value");
		}
	}

	private void verifyColumnLabel(String columnLabel) throws SQLException {
		if (this.currentRecord.containsKey(columnLabel)) {
			throw new SQLException("Invalid column index value");
		}
	}

	private <T> T getValueByColumnIndex(int columnIndex, ValueMapper<T> valueMapper) throws SQLException {
		verifyCursorPosition();
		verifyColumnIndex(columnIndex);
		columnIndex--;
		return valueMapper.map(this.currentRecord.get(columnIndex));
	}

	private <T> T getValueByColumnLabel(String columnLabel, ValueMapper<T> valueMapper) throws SQLException {
		verifyCursorPosition();
		verifyColumnLabel(columnLabel);
		return valueMapper.map(this.currentRecord.get(columnLabel));
	}

	private static String mapToString(Value value) throws SQLException {
		if (Type.STRING.isTypeOf(value)) {
			return value.asString();
		}
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		throw new SQLException(String.format("%s value can not be mapped to String.", value.type()));
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
				throw new SQLException("Number values can not be mapped to boolean aside from 0 and 1 values.");
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
				throw new SQLException("String values can not be mapped to boolean aside from '0' and '1' values.");
			}
		}
		throw new SQLException(String.format("%s value can not be mapped to boolean.", value.type()));
	}

	private static Byte mapToByte(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Byte.MIN_VALUE && longValue <= Byte.MAX_VALUE) {
				return (byte) longValue;
			}
			throw new SQLException("The number is out of byte range.");
		}
		if (Type.NULL.isTypeOf(value)) {
			return (byte) 0;
		}
		throw new SQLException(String.format("%s value can not be mapped to byte.", value.type()));
	}

	private static Short mapToShort(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
				return (short) longValue;
			}
			throw new SQLException("The number is out of short range.");
		}
		if (Type.NULL.isTypeOf(value)) {
			return (short) 0;
		}
		throw new SQLException(String.format("%s value can not be mapped to short.", value.type()));
	}

	private static int mapToInteger(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			var longValue = value.asNumber().longValue();
			if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
				return (int) longValue;
			}
			throw new SQLException("The number is out of int range.");
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0;
		}
		throw new SQLException(String.format("%s value can not be mapped to int.", value.type()));
	}

	private static long mapToLong(Value value) throws SQLException {
		if (Type.INTEGER.isTypeOf(value)) {
			return value.asNumber().longValue();
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0L;
		}
		throw new SQLException(String.format("%s value can not be mapped to long.", value.type()));
	}

	private static float mapToFloat(Value value) throws SQLException {
		if (Type.FLOAT.isTypeOf(value)) {
			var doubleValue = value.asNumber().doubleValue();
			var floatValue = (float) doubleValue;
			if (doubleValue == floatValue) {
				return floatValue;
			}
			throw new SQLException("The number is out of float range.");
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0.0f;
		}
		throw new SQLException(String.format("%s value can not be mapped to float.", value.type()));
	}

	private static double mapToDouble(Value value) throws SQLException {
		if (Type.FLOAT.isTypeOf(value)) {
			return value.asNumber().doubleValue();
		}
		if (Type.NULL.isTypeOf(value)) {
			return 0.0;
		}
		throw new SQLException(String.format("%s value can not be mapped to double.", value.type()));
	}

	private static byte[] mapToBytes(Value value) throws SQLException {
		if (Type.NULL.isTypeOf(value)) {
			return null;
		}
		if (Type.BYTES.isTypeOf(value)) {
			return value.asByteArray();
		}
		throw new SQLException(String.format("%s value can not be mapped to byte array.", value.type()));
	}

	@FunctionalInterface
	private interface ValueMapper<T> {

		T map(Value value) throws SQLException;

	}

}
