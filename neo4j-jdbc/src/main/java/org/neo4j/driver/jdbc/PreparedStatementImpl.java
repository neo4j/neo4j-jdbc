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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.ValueException;
import org.neo4j.driver.jdbc.values.Values;

sealed class PreparedStatementImpl extends StatementImpl
		implements Neo4jPreparedStatement permits CallableStatementImpl {

	private static final Logger LOGGER = Logger.getLogger(Neo4jPreparedStatement.class.getCanonicalName());

	// We did not consider using concurrent datastructures as the `PreparedStatement` is
	// usually not treated as thread-safe
	private final Deque<Map<String, Object>> parameters = new ArrayDeque<>();

	private final UnaryOperator<Integer> indexProcessor;

	private final boolean rewriteBatchedStatements;

	private final String sql;

	PreparedStatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			UnaryOperator<String> sqlProcessor, UnaryOperator<Integer> indexProcessor, boolean rewriteBatchedStatements,
			String sql) {
		super(connection, transactionSupplier, sqlProcessor);
		this.indexProcessor = Objects.requireNonNullElseGet(indexProcessor, UnaryOperator::identity);
		this.rewriteBatchedStatements = rewriteBatchedStatements;
		this.sql = sql;
		this.poolable = true;
		this.parameters.add(new HashMap<>());
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		assertIsOpen();
		return super.executeQuery0(this.sql, true, getCurrentBatch());
	}

	protected final Map<String, Object> getCurrentBatch() {
		return this.parameters.getLast();
	}

	@Override
	public int executeUpdate() throws SQLException {
		assertIsOpen();
		return super.executeUpdate0(this.sql, true, getCurrentBatch());
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final void addBatch() throws SQLException {
		assertIsOpen();
		this.parameters.addLast(new HashMap<>());
	}

	@Override
	public void clearParameters() throws SQLException {
		assertIsOpen();
		getCurrentBatch().clear();
	}

	@Override
	public int[] executeBatch() throws SQLException {

		// Apply any SQL to Cypher transformation upfront and assume a simple
		// CREATE statement provided and not something that already does an unwind.
		// But even without rewriting the batch, it's fast as things don't have
		// to be parsed twice.
		var processedSql = processSQL(this.sql);
		int[] result;
		if (this.rewriteBatchedStatements) {
			// No, can't use the comparator constructor here, as that one would be used
			// to check then for equality as well
			var keys = new HashSet<String>();
			var validParameters = new ArrayList<Map<String, Object>>();
			for (var parameter : this.parameters) {
				if (parameter.isEmpty()) {
					continue;
				}
				keys.addAll(parameter.keySet());
				validParameters.add(parameter);
			}
			for (String key : keys.stream().sorted(Comparator.comparing(String::length).reversed()).toList()) {
				// The boundary of the regex works only reliable with indexed parameters,
				// for named we sorted them descending by length, to make sure the longest
				// are replaced first.
				processedSql = processedSql.replaceAll(Pattern.quote("$" + key) + "(?!\\d)",
						"__parameter['" + key + "']");
			}
			processedSql = "UNWIND $__parameters AS __parameter " + processedSql;
			LOGGER.log(Level.INFO, "Rewrite batch statements is in effect, statement {0} has been rewritten into {1}",
					new Object[] { this.sql, processedSql });
			result = new int[] { super.executeUpdate0(processedSql, false, Map.of("__parameters", validParameters)) };
		}
		else {
			result = new int[this.parameters.size()];
			Arrays.fill(result, SUCCESS_NO_INFO);
			int i = 0;
			for (var parameter : this.parameters) {
				if (parameter.isEmpty()) {
					continue;
				}
				result[i++] = super.executeUpdate0(processedSql, false, parameter);
			}
		}

		this.clearBatch();
		return result;
	}

	@Override
	public void clearBatch() throws SQLException {
		assertIsOpen();
		this.parameters.clear();
		this.parameters.add(new HashMap<>());
	}

	protected final void setParameter(String key, Object value) {
		getCurrentBatch().put(key, value);
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public long executeLargeUpdate(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void setNull(int parameterIndex, int ignored) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.NULL);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setInt(String parameterName, int value) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(parameterName);
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setInt(int parameterIndex, int value) throws SQLException {

		assertValidParameterIndex(parameterIndex);
		setInt(computeParameterIndex(parameterIndex), value);
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterIndex(parameterIndex), Values.value(x));
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		throw new SQLException("BigDecimal is not supported");
	}

	@Override
	public void setString(String parameterName, String string) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(parameterName);
		Objects.requireNonNull(string);
		setParameter(parameterName, Values.value(string));
	}

	@Override
	public void setString(int parameterIndex, String string) throws SQLException {

		assertValidParameterIndex(parameterIndex);
		setString(computeParameterIndex(parameterIndex), string);
	}

	private String computeParameterIndex(int parameterIndex) {
		return String.valueOf(this.indexProcessor.apply(parameterIndex));
	}

	@Override
	public void setBytes(int parameterIndex, byte[] bytes) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(bytes);
		setParameter(computeParameterIndex(parameterIndex), Values.value(bytes));
	}

	@Override
	public void setDate(int parameterIndex, Date date) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(date);
		setParameter(computeParameterIndex(parameterIndex), Values.value(date.toLocalDate()));
	}

	@Override
	public void setTime(int parameterIndex, Time time) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(time);
		setParameter(computeParameterIndex(parameterIndex), Values.value(time.toLocalTime()));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(timestamp);
		setParameter(computeParameterIndex(parameterIndex), Values.value(timestamp.toLocalDateTime()));
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(inputStream);
		byte[] bytes;
		try (inputStream) {
			bytes = inputStream.readNBytes(length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(computeParameterIndex(parameterIndex), Values.value(new String(bytes, StandardCharsets.US_ASCII)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(inputStream);
		byte[] bytes;
		try (inputStream) {
			bytes = inputStream.readNBytes(length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(computeParameterIndex(parameterIndex), Values.value(bytes));
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(int parameterIndex, Object object) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		if (object instanceof Date date) {
			setDate(parameterIndex, date);
		}
		else if (object instanceof Time time) {
			setTime(parameterIndex, time);
		}
		else if (object instanceof Timestamp timestamp) {
			setTimestamp(parameterIndex, timestamp);
		}
		else {
			Value value;
			try {
				value = Values.value(object);
			}
			catch (ValueException ex) {
				throw new SQLException(ex);
			}
			setParameter(computeParameterIndex(parameterIndex), value);
		}
	}

	@Override
	public boolean execute() throws SQLException {
		return super.execute0(this.sql, true, getCurrentBatch());
	}

	@Override
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(reader);
		var charBuffer = new char[length];
		try (reader) {
			reader.read(charBuffer, 0, length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(computeParameterIndex(parameterIndex), Values.value(new String(charBuffer)));
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setDate(int parameterIndex, Date date, Calendar cal) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(date);
		if (cal == null) {
			cal = Calendar.getInstance();
		}
		var localDate = date.toLocalDate();
		var zonedDateTime = ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, cal.getTimeZone().toZoneId());
		setParameter(computeParameterIndex(parameterIndex), Values.value(zonedDateTime));
	}

	@Override
	public void setTime(int parameterIndex, Time time, Calendar cal) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(time);
		if (cal == null) {
			cal = Calendar.getInstance();
		}
		var offsetMillis = cal.getTimeZone().getRawOffset();
		var offsetSeconds = offsetMillis / 1000;
		var zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
		var offsetTime = time.toLocalTime().atOffset(zoneOffset);
		setParameter(computeParameterIndex(parameterIndex), Values.value(offsetTime));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp, Calendar cal) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(timestamp);
		if (cal == null) {
			cal = Calendar.getInstance();
		}
		var localDateTime = timestamp.toLocalDateTime();
		var zonedDateTime = ZonedDateTime.of(localDateTime, cal.getTimeZone().toZoneId());
		setParameter(computeParameterIndex(parameterIndex), Values.value(zonedDateTime));
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public ParameterMetaData getParameterMetaData() {
		return new ParameterMetaDataImpl();
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(inputStream);
		var lengthAsInt = (int) length;
		if (lengthAsInt != length) {
			throw new SQLException("length larger than integer max value is not supported");
		}
		setAsciiStream(parameterIndex, inputStream, lengthAsInt);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(inputStream);
		var lengthAsInt = (int) length;
		if (lengthAsInt != length) {
			throw new SQLException("length larger than integer max value is not supported");
		}
		setBinaryStream(parameterIndex, inputStream, lengthAsInt);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		Objects.requireNonNull(reader);
		var lengthAsInt = (int) length;
		if (lengthAsInt != length) {
			throw new SQLException("length larger than integer max value is not supported");
		}
		setCharacterStream(parameterIndex, reader, lengthAsInt);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	private static SQLException newIllegalMethodInvocation() throws SQLException {
		throw new SQLException("This method must not be called on PreparedStatement");
	}

	private static void assertValidParameterIndex(int index) throws SQLException {
		if (index < 1) {
			throw new SQLException("Parameter index must be equal or more than 1");
		}
	}

}
