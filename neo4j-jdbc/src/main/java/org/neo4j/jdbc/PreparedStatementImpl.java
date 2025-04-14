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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.ValueException;
import org.neo4j.jdbc.values.Values;

sealed class PreparedStatementImpl extends StatementImpl implements Neo4jPreparedStatement
		permits CallableStatementImpl {

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.prepared-statement");

	private static final Pattern SQL_PLACEHOLDER_PATTERN = Pattern.compile("\\?(?=[^\"]*(?:\"[^\"]*\"[^\"]*)*$)");

	// We did not consider using concurrent datastructures as the `PreparedStatement` is
	// usually not treated as thread-safe
	private final Deque<Map<String, Object>> parameters = new ArrayDeque<>();

	private final boolean rewriteBatchedStatements;

	private final String sql;

	private final AtomicBoolean cursorMoved = new AtomicBoolean(false);

	static String rewritePlaceholders(String raw) {
		int index = 1;

		var matcher = SQL_PLACEHOLDER_PATTERN.matcher(raw);

		var sb = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(sb, "\\$" + index++);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	PreparedStatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			UnaryOperator<String> translator, Warnings localWarnings, Consumer<Class<? extends Statement>> onClose,
			boolean rewriteBatchedStatements, String sql) {
		super(connection, transactionSupplier, translator, localWarnings, onClose);
		this.rewriteBatchedStatements = rewriteBatchedStatements;
		this.sql = sql;
		this.poolable = true;
		this.parameters.add(new HashMap<>());
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Executing query");
		assertIsOpen();
		return super.executeQuery0(this.sql, true, getCurrentBatch());
	}

	protected final Map<String, Object> getCurrentBatch() {
		return this.parameters.getLast();
	}

	@Override
	public int executeUpdate() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Executing update");
		assertIsOpen();
		return super.executeUpdate0(this.sql, true, getCurrentBatch());
	}

	@Override
	public final ResultSet executeQuery(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final int executeUpdate(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final boolean execute(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void addBatch() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Adding batch");
		assertIsOpen();
		this.parameters.addLast(new HashMap<>());
	}

	@Override
	public void clearParameters() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Clearing parameters");
		assertIsOpen();
		getCurrentBatch().clear();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Executing batch");
		assertIsOpen();
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
				// The boundary of the regex works only reliable with indexed
				// ordinalParameters,
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
		LOGGER.log(Level.FINER, () -> "Clearing batch");
		assertIsOpen();
		this.parameters.clear();
		this.parameters.add(new HashMap<>());
	}

	final void setParameter(String key, Object value) {
		LOGGER.log(Level.FINER, () -> {
			Object valueLogged;
			String type;
			if (value != null) {
				valueLogged = "******";
				if (value instanceof Value hlp) {
					type = hlp.type().name();
				}
				else {
					type = value.getClass().getName();
				}
			}
			else {
				valueLogged = "(literal) null";
				type = Void.class.getName();
			}
			return "Setting parameter `%s` to `%s` (%s)".formatted(key, valueLogged, type);
		});
		getCurrentBatch().put(Objects.requireNonNull(key), value);
	}

	@Override
	public final int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final boolean execute(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final long executeLargeUpdate(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public final long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void setNull(int parameterIndex, int ignored) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.NULL);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setInt(String parameterName, int value) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(parameterName);
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setInt(int parameterIndex, int value) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(value));
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(x));
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), (x != null) ? Values.value(x.toString()) : Values.NULL);
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
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(string));
	}

	@Override
	public void setBytes(int parameterIndex, byte[] bytes) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(Objects.requireNonNull(bytes)));
	}

	@Override
	public void setDate(int parameterIndex, Date value) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(value.toLocalDate()));
	}

	@Override
	public void setDate(String parameterName, Date value) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(value);
		setParameter(parameterName, Values.value(value.toLocalDate()));
	}

	@Override
	public void setTime(int parameterIndex, Time value) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(value.toLocalTime()));
	}

	@Override
	public void setTime(String parameterName, Time value) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(value);
		setParameter(parameterName, Values.value(value.toLocalTime()));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp value) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.value(value.toLocalDateTime()));
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(Objects.requireNonNull(value).toLocalDateTime()));
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		assertValidStreamLength("character", parameterIndex, length);
		setAsciiStream0(computeParameterName(parameterIndex), inputStream, length);
	}

	private static void assertValidStreamLength(String name, int parameterIndex, int length) throws SQLException {
		if (length < 0) {
			throw new SQLException(
					"Invalid length %d for %s stream at index %d".formatted(length, name, parameterIndex));
		}
	}

	final void setAsciiStream0(String parameterName, InputStream inputStream, int length) throws SQLException {
		byte[] bytes;
		try (var in = Objects.requireNonNull(inputStream)) {
			bytes = in.readNBytes(length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(parameterName, Values.value(new String(bytes, DEFAULT_ASCII_CHARSET_FOR_INCOMING_STREAM)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		setCharacterStream(parameterIndex, new InputStreamReader(x, StandardCharsets.UTF_8), length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		assertValidStreamLength("binary", parameterIndex, length);
		setBinaryStream0(computeParameterName(parameterIndex), inputStream, length);
	}

	final void setBinaryStream0(String parameterName, InputStream inputStream, int length) throws SQLException {
		byte[] bytes;
		try (var in = Objects.requireNonNull(inputStream)) {
			bytes = in.readNBytes(length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(parameterName, Values.value(bytes));
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setObject(int parameterIndex, Object value) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setObjectParameter(computeParameterName(parameterIndex), value);
	}

	@Override
	public void setObject(String parameterName, Object value) throws SQLException {
		assertIsOpen();
		setObjectParameter(parameterName, value);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, reader);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream stream) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, new InputStreamReader(stream, DEFAULT_ASCII_CHARSET_FOR_INCOMING_STREAM));
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream stream) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, stream);
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.NULL);
	}

	@Override
	public void setBoolean(String parameterName, boolean value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setByte(String parameterName, byte value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setShort(String parameterName, short value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setLong(String parameterName, long value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setFloat(String parameterName, float value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setDouble(String parameterName, double value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(value));
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal value) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, (value != null) ? Values.value(value.toString()) : Values.NULL);
	}

	@Override
	public void setBytes(String parameterName, byte[] bytes) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(Objects.requireNonNull(bytes)));
	}

	private void setObjectParameter(String parameterName, Object value) throws SQLException {
		if (value instanceof Date date) {
			setDate(parameterName, date);
		}
		else if (value instanceof Time time) {
			setTime(parameterName, time);
		}
		else if (value instanceof Timestamp timestamp) {
			setTimestamp(parameterName, timestamp);
		}
		else if (value instanceof Value neo4jValue) {
			setParameter(parameterName, neo4jValue);
		}
		else {
			try {
				setParameter(parameterName, Values.value(value));
			}
			catch (ValueException ex) {
				throw new SQLException(ex);
			}
		}
	}

	@Override
	public boolean execute() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Executing");
		return super.execute0(this.sql, getCurrentBatch());
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		assertValidStreamLength("character", parameterIndex, length);
		setCharacterStream0(computeParameterName(parameterIndex), reader, length);
	}

	final void setCharacterStream0(String parameterName, Reader reader, int length) throws SQLException {
		var charBuffer = new char[length];
		int lengthRead;
		try (var in = Objects.requireNonNull(reader)) {
			lengthRead = in.read(charBuffer, 0, length);
		}
		catch (IOException ex) {
			throw new SQLException(ex);
		}
		setParameter(parameterName, Values.value((lengthRead != -1) ? new String(charBuffer, 0, lengthRead) : ""));
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
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setArray0(computeParameterName(parameterIndex), x);
	}

	@Override
	public void setArray(String parameterName, Array x) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(parameterName);
		setArray0(parameterName, x);
	}

	private void setArray0(String parameterName, Array x) throws SQLException {
		if (x == null) {
			setParameter(parameterName, Values.NULL);
			return;
		}

		Value value;
		if ("BYTES".equals(x.getBaseTypeName())) {
			value = Values.value(x.getArray());
		}
		else {
			var hlp = new ArrayList<Value>();
			try (var rs = x.getResultSet()) {
				while (rs.next()) {
					hlp.add(rs.getObject(2, Value.class));
				}
			}
			value = Values.value(hlp);
		}
		setParameter(parameterName, value);
	}

	@SuppressWarnings("resource")
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		LOGGER.log(Level.FINER, () -> "Getting meta data");
		assertCallAndPositionAtFirstRow();
		return super.resultSet.getMetaData();
	}

	@Override
	public void setDate(int parameterIndex, Date date, Calendar cal) throws SQLException {
		assertValidParameterIndex(parameterIndex);
		setDate0(computeParameterName(parameterIndex), date, cal);
	}

	protected final void setDate0(String parameterName, Date date, Calendar cal) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Values.value(Neo4jConversions.asValue(date, cal)));
	}

	@Override
	public void setTime(int parameterIndex, Time time, Calendar cal) throws SQLException {
		assertValidParameterIndex(parameterIndex);
		setTime0(computeParameterName(parameterIndex), time, cal);
	}

	protected final void setTime0(String parameterName, Time time, Calendar cal) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Neo4jConversions.asValue(time, cal));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp, Calendar cal) throws SQLException {
		assertValidParameterIndex(parameterIndex);
		setTimestamp0(computeParameterName(parameterIndex), timestamp, cal);
	}

	protected final void setTimestamp0(String parameterName, Timestamp timestamp, Calendar cal) throws SQLException {
		assertIsOpen();
		setParameter(parameterName, Neo4jConversions.asValue(timestamp, cal));
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), Values.NULL);
	}

	@Override
	public void setURL(int parameterIndex, URL url) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), (url != null) ? Values.value(url.toString()) : Values.NULL);
	}

	@Override
	public ParameterMetaData getParameterMetaData() {
		LOGGER.log(Level.FINER, () -> "Getting parameter meta data");
		return new ParameterMetaDataImpl();
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	/**
	 * <strong>Note</strong> In the Neo4j JDBC driver there is no special treatment for a
	 * national character string.
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter marker in
	 * the SQL statement; if the driver can detect that a data conversion error could
	 * occur; if a database access error occurs; or this method is called on a closed
	 * {@code PreparedStatement}
	 */
	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		setString(1, value);
	}

	/**
	 * <strong>Note</strong> In the Neo4j JDBC driver there is no special treatment for a
	 * national character stream.
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value the parameter value
	 * @param length the number of characters in the parameter data.
	 * @throws SQLException if parameterIndex does not correspond to a parameter marker in
	 * the SQL statement; if the driver can detect that a data conversion error could
	 * occur; if a database access error occurs; or this method is called on a closed
	 * {@code PreparedStatement}
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		setCharacterStream(parameterIndex, value, length);
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
	public void setAsciiStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		setAsciiStream(parameterIndex, inputStream, getLengthAsInt(length));
	}

	static int getLengthAsInt(long length) throws SQLException {
		var lengthAsInt = (int) length;
		if (lengthAsInt != length) {
			throw new SQLException("length larger than integer max value is not supported");
		}
		return lengthAsInt;
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		setBinaryStream(parameterIndex, inputStream, getLengthAsInt(length));
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		setCharacterStream(parameterIndex, reader, getLengthAsInt(length));
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream stream) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex),
				new InputStreamReader(stream, DEFAULT_ASCII_CHARSET_FOR_INCOMING_STREAM));
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), x);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		assertIsOpen();
		assertValidParameterIndex(parameterIndex);
		setParameter(computeParameterName(parameterIndex), reader);
	}

	/**
	 * <strong>Note</strong> In the Neo4j JDBC driver there is no special treatment for a
	 * national character stream.
	 * @param parameterIndex of the first parameter is 1, the second is 2, ...
	 * @param value the parameter value
	 * @throws SQLException if parameterIndex does not correspond to a parameter marker in
	 * the SQL statement; if the driver can detect that a data conversion error could
	 * occur; if a database access error occurs; or this method is called on a closed
	 * {@code PreparedStatement}
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		setCharacterStream(parameterIndex, value);
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

	protected final ResultSet assertCallAndPositionAtFirstRow() throws SQLException {
		if (resultSet == null) {
			throw new SQLException("#execute has not been called");
		}
		if (this.cursorMoved.compareAndSet(false, true)) {
			this.resultSet.next();
		}
		return resultSet;
	}

	private static String computeParameterName(int parameterIndex) {
		return String.valueOf(parameterIndex);
	}

	static SQLException newIllegalMethodInvocation() {
		return new SQLException("This method must not be called on PreparedStatement");
	}

	private static void assertValidParameterIndex(int index) throws SQLException {
		if (index < 1) {
			throw new SQLException("Parameter index must be equal or more than 1");
		}
	}

	protected void setURL(String parameterName, URL value) throws SQLException {
		assertIsOpen();
		Objects.requireNonNull(parameterName);
		setParameter(parameterName, (value != null) ? Values.value(value.toString()) : Values.NULL);
	}

}
