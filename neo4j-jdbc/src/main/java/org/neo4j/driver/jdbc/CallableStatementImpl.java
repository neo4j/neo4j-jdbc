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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.neo4j.driver.jdbc.values.Values;

final class CallableStatementImpl extends PreparedStatementImpl implements CallableStatement {

	private ParameterType parameterType;

	CallableStatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			UnaryOperator<String> sqlProcessor, UnaryOperator<Integer> indexProcessor, boolean rewriteBatchedStatements,
			String sql) {
		super(connection, transactionSupplier, sqlProcessor, indexProcessor, rewriteBatchedStatements, sql);
	}

	@Override
	public void clearParameters() throws SQLException {
		super.clearParameters();
		this.parameterType = null;
	}

	@Override
	public void clearBatch() throws SQLException {
		super.clearBatch();
		this.parameterType = null;
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean wasNull() throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public String getString(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public byte getByte(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public short getShort(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public int getInt(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public long getLong(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public float getFloat(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public double getDouble(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Date getDate(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Time getTime(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Object getObject(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Ref getRef(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob getBlob(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob getClob(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Array getArray(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public URL getURL(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setURL(String parameterName, URL value) throws SQLException {
		setNamedParameter(parameterName, value.toString());
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		setNamedParameter(parameterName, Values.NULL);
	}

	@Override
	public void setBoolean(String parameterName, boolean value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setByte(String parameterName, byte value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setShort(String parameterName, short value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setInt(String parameterName, int value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setLong(String parameterName, long value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setFloat(String parameterName, float value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setDouble(String parameterName, double value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setString(String parameterName, String value) throws SQLException {
		setNamedParameter(parameterName, value);
	}

	@Override
	public void setBytes(String parameterName, byte[] bytes) throws SQLException {
		setNamedParameter(parameterName, bytes);
	}

	@Override
	public void setDate(String parameterName, Date date) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setDate(parameterName, date);
	}

	@Override
	public void setTime(String parameterName, Time time) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setTime(parameterName, time);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp timestamp) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setTimestamp(parameterName, timestamp);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		var value = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.US_ASCII)).lines()
			.collect(Collectors.joining("\n"));
		setParameter(parameterName, value);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		byte[] value;
		try {
			value = inputStream.readAllBytes();
		}
		catch (IOException ex) {
			throw new SQLException("Failed to read bytes.", ex);
		}
		setParameter(parameterName, value);
	}

	@Override
	public void setObject(String parameterName, Object object, int targetSqlType, int scale) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(String parameterName, Object object, int targetSqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(String parameterName, Object object) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setObject(parameterName, object);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		var value = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
		setParameter(parameterName, value);
	}

	@Override
	public void setDate(String parameterName, Date date, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setDateParameter(parameterName, date, calendar);
	}

	@Override
	public void setTime(String parameterName, Time time, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setTimeParameter(parameterName, time, calendar);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp timestamp, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		super.setTimestampParameter(parameterName, timestamp, calendar);
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getString(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public boolean getBoolean(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public byte getByte(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public short getShort(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int getInt(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public long getLong(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public float getFloat(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public double getDouble(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public byte[] getBytes(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Time getTime(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Object getObject(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Ref getRef(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Blob getBlob(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Clob getClob(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Array getArray(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public URL getURL(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public RowId getRowId(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public RowId getRowId(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNString(String parameterName, String value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(String parameterName, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob getNClob(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public NClob getNClob(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getNString(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public String getNString(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		throw newOutParametersNotSupported();
	}

	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(String parameterName, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(String parameterName, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(String parameterName, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	// ----- ordinal setter overrides begin -----

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setNull(parameterIndex, sqlType);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setBoolean(parameterIndex, value);
	}

	@Override
	public void setByte(int parameterIndex, byte value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setByte(parameterIndex, value);
	}

	@Override
	public void setShort(int parameterIndex, short value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setShort(parameterIndex, value);
	}

	@Override
	public void setInt(int parameterIndex, int value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setInt(parameterIndex, value);
	}

	@Override
	public void setLong(int parameterIndex, long value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setLong(parameterIndex, value);
	}

	@Override
	public void setFloat(int parameterIndex, float value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setFloat(parameterIndex, value);
	}

	@Override
	public void setDouble(int parameterIndex, double value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setDouble(parameterIndex, value);
	}

	@Override
	public void setString(int parameterIndex, String value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setString(parameterIndex, value);
	}

	@Override
	public void setBytes(int parameterIndex, byte[] bytes) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setBytes(parameterIndex, bytes);
	}

	@Override
	public void setDate(int parameterIndex, Date date) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setDate(parameterIndex, date);
	}

	@Override
	public void setTime(int parameterIndex, Time time) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setTime(parameterIndex, time);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setTimestamp(parameterIndex, timestamp);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setAsciiStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setBinaryStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setObject(int parameterIndex, Object object) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setObject(parameterIndex, object);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setDate(int parameterIndex, Date date, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setDate(parameterIndex, date, calendar);
	}

	@Override
	public void setTime(int parameterIndex, Time time, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setTime(parameterIndex, time, calendar);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp, Calendar calendar) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setTimestamp(parameterIndex, timestamp, calendar);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setAsciiStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setBinaryStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.ORDINAL);
		super.setCharacterStream(parameterIndex, reader, length);
	}

	// ----- ordinal setter overrides end -----

	private void setNamedParameter(String name, Object value) throws SQLException {
		assertIsOpen();
		assertParameterType(ParameterType.NAMED);
		setParameter(name, value);
	}

	private void assertParameterType(ParameterType parameterType) throws SQLException {
		if (this.parameterType == null) {
			this.parameterType = parameterType;
		}
		else {
			if (this.parameterType != parameterType) {
				throw new SQLException(String.format("%s parameter can not be mixed with %s parameter(s).",
						parameterType, this.parameterType));
			}
		}
	}

	private SQLException newOutParametersNotSupported() {
		return new SQLException("Out parameters are not supported.");
	}

	private enum ParameterType {

		ORDINAL, NAMED

	}

}
