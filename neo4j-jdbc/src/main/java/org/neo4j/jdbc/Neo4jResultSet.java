/*
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
 * Created on 03/02/16
 */
package org.neo4j.jdbc;

import org.neo4j.jdbc.utils.ExceptionBuilder;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Neo4jResultSet implements ResultSet {

	public static final int DEFAULT_TYPE        = TYPE_FORWARD_ONLY;
	public static final int DEFAULT_CONCURRENCY = CONCUR_READ_ONLY;
	public static final int DEFAULT_HOLDABILITY = CLOSE_CURSORS_AT_COMMIT;

	protected static final int    DEFAULT_FETCH_SIZE = 1;
	protected static final String COLUMN_NOT_PRESENT = "Column not present in ResultSet";

	/**
	 * Close state of this ResultSet.
	 */
	protected boolean isClosed         = false;
	protected int     currentRowNumber = 0;
	protected int     type;
	protected int     concurrency;
	protected int     holdability;

	protected Statement statement;

	/**
	 * Is the last read column was null.
	 */
	protected boolean wasNull = false;

	public Neo4jResultSet(Statement statement, int... params) {
		this.statement = statement;
		this.type = params.length > 0 ? params[0] : TYPE_FORWARD_ONLY;
		this.concurrency = params.length > 1 ? params[1] : CONCUR_READ_ONLY;
		this.holdability = params.length > 2 ? params[2] : CLOSE_CURSORS_AT_COMMIT;
	}

	/*----------------------------------------*/
	/*       Some useful, check method        */
	/*----------------------------------------*/

	/**
	 * Check if the connection is closed or not.
	 * If it is, we throw an exception.
	 *
	 * @throws SQLException if the {@link Neo4jResultSet} is closed
	 */
	protected void checkClosed() throws SQLException {
		if (this.isClosed()) {
			throw new SQLException("ResultSet already closed");
		}
	}

	/**
	 * Check if the ResultSet is closed.
	 *
	 * @return <code>true</code> if <code>ResultSet</code> is closed.
	 * @throws SQLException in case of problems when checking if closed.
	 */
	@Override public boolean isClosed() throws SQLException {
		return this.isClosed;
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	@Override public SQLWarning getWarnings() throws SQLException {
		checkClosed();
		return null;
	}

	@Override public void clearWarnings() throws SQLException {
		checkClosed();
	}

	@Override public boolean next() throws SQLException {
		boolean result = innerNext();
		if (result) {
			currentRowNumber++;
		}
		return result;
	}

	@Override public void setFetchSize(int rows) throws SQLException {
		this.checkClosed();
		if (rows < 0) {
			throw new SQLException("Fetch size must be >= 0");
		}
	}

	@Override public int getFetchSize() throws SQLException {
		this.checkClosed();
		return DEFAULT_FETCH_SIZE;
	}

	protected abstract boolean innerNext() throws SQLException;

	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	@Override public byte getByte(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public byte[] getBytes(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Date getDate(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Time getTime(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Timestamp getTimestamp(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public byte getByte(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public byte[] getBytes(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Date getDate(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Time getTime(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Timestamp getTimestamp(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public String getCursorName() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Reader getCharacterStream(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Reader getCharacterStream(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean isBeforeFirst() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean isAfterLast() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean isFirst() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean isLast() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void beforeFirst() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void afterLast() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean first() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean last() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public final int getRow() throws SQLException {
		return currentRowNumber;
	}

	@Override public boolean absolute(int row) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean relative(int rows) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean previous() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setFetchDirection(int direction) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public int getFetchDirection() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean rowUpdated() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean rowInserted() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean rowDeleted() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNull(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateByte(int columnIndex, byte x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateShort(int columnIndex, short x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateInt(int columnIndex, int x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateLong(int columnIndex, long x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateFloat(int columnIndex, float x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateDouble(int columnIndex, double x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateString(int columnIndex, String x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateDate(int columnIndex, Date x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateTime(int columnIndex, Time x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateObject(int columnIndex, Object x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNull(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateByte(String columnLabel, byte x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateShort(String columnLabel, short x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateInt(String columnLabel, int x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateLong(String columnLabel, long x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateFloat(String columnLabel, float x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateDouble(String columnLabel, double x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateString(String columnLabel, String x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateDate(String columnLabel, Date x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateTime(String columnLabel, Time x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateObject(String columnLabel, Object x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void insertRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void deleteRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void refreshRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void cancelRowUpdates() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void moveToInsertRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void moveToCurrentRow() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public java.sql.Statement getStatement() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Ref getRef(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Blob getBlob(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Clob getClob(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Ref getRef(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Blob getBlob(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Clob getClob(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public URL getURL(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public URL getURL(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateArray(int columnIndex, java.sql.Array x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateArray(String columnLabel, java.sql.Array x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public RowId getRowId(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public RowId getRowId(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNString(int columnIndex, String nString) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNString(String columnLabel, String nString) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public NClob getNClob(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public NClob getNClob(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public String getNString(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public String getNString(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}
}
