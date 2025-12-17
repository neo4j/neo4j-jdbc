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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.neo4j.jdbc.Neo4jException.GQLError;

import static org.neo4j.jdbc.Neo4jException.withReason;

final class CallableStatementImpl extends PreparedStatementImpl implements Neo4jCallableStatement {

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.callable-statement");

	static CallableStatementImpl prepareCall(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			Consumer<Class<? extends Statement>> onClose, boolean rewriteBatchedStatements, String sql)
			throws SQLException {

		// We should cache the descriptor if this gets widely used.

		var descriptor = parse(sql);
		var parameterOrder = new HashMap<String, Integer>();
		var meta = connection.getMetaData();

		// We might not be able to spot all the function calls
		if (descriptor.isFunctionCall() == null) {
			boolean isFunction;
			try (var procedures = meta.getProcedures(null, null, descriptor.fqn())) {
				isFunction = !procedures.next();
			}
			descriptor = new Descriptor(descriptor.fqn, descriptor.returnType, descriptor.yieldedValues,
					descriptor.parameterList, isFunction);
		}

		var parameterTypes = new HashMap<Integer, String>();

		try (var columns = descriptor.isFunctionCall() ? meta.getFunctionColumns(null, null, descriptor.fqn(), null)
				: meta.getProcedureColumns(null, null, descriptor.fqn(), null)) {
			while (columns.next()) {
				var type = columns.getInt("COLUMN_TYPE");
				var ordinalPosition = columns.getInt("ORDINAL_POSITION");

				parameterOrder.put(columns.getString("COLUMN_NAME"), ordinalPosition);

				// It might be that those JDBC constants are actually the same right
				// now,
				// but that might as well change
				// in a different JDK or "the future"
				// noinspection ConditionCoveredByFurtherCondition,ConstantValue
				if (type == DatabaseMetaData.procedureColumnIn || type == DatabaseMetaData.functionColumnIn) {
					parameterTypes.put(ordinalPosition, columns.getString("DATA_TYPE"));
				}
			}
		}

		if (descriptor.isUsingNamedParameters()) {
			for (String value : descriptor.parameterList.namedParameters().values()) {
				if (!parameterOrder.containsKey(value)) {
					throw new Neo4jException(GQLError.$42N51.withMessage(
							"Procedure `" + descriptor.fqn() + "` does not have a named parameter `" + value + "`"));
				}
			}
		}

		// We can always store the descriptor with the statement to check for yielded /
		// return values if wished / needed
		return new CallableStatementImpl(connection, transactionSupplier, onClose, rewriteBatchedStatements,
				descriptor.toCypher(parameterOrder), new ParameterMetaDataImpl(parameterTypes));
	}

	private final AtomicBoolean cursorMoved = new AtomicBoolean(false);

	private final ParameterMetaData parameterMetaData;

	private ParameterType parameterType;

	private ResultSet parameterResultSet;

	CallableStatementImpl(Connection connection, Neo4jTransactionSupplier transactionSupplier,
			Consumer<Class<? extends Statement>> onClose, boolean rewriteBatchedStatements, String sql,
			ParameterMetaData parameterMetaData) {
		super(connection, transactionSupplier, UnaryOperator.identity(), null, onClose, false, rewriteBatchedStatements,
				Statement.NO_GENERATED_KEYS, sql);

		this.parameterMetaData = parameterMetaData;
	}

	@Override
	public ParameterMetaData getParameterMetaData() {
		LOGGER.log(Level.FINER, () -> "Getting parameter meta data");
		return this.parameterMetaData;
	}

	@Override
	public boolean execute() throws SQLException {
		clearParameterResultSet();
		this.parameterResultSet = DatabaseMetadataImpl.resultSetForParameters(getConnection(), getCurrentBatch());
		return super.execute();
	}

	private void clearParameterResultSet() throws SQLException {
		if (this.parameterResultSet == null) {
			return;
		}
		this.parameterResultSet.close();
		this.parameterResultSet = null;
		this.cursorMoved.set(false);
	}

	ResultSet assertCallAndPositionAtFirstRow() throws SQLException {

		if (this.parameterResultSet == null) {
			throw new Neo4jException(withReason("#execute has not been called"));
		}
		if (this.cursorMoved.compareAndSet(false, true)) {
			this.parameterResultSet.next();
		}
		return this.parameterResultSet;
	}

	@Override
	public void clearParameters() throws SQLException {
		super.clearParameters();
		this.parameterType = null;
		clearParameterResultSet();
	}

	@Override
	public void clearBatch() throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType) {
		LOGGER.log(Level.WARNING,
				() -> "Registering out parameter %d with type %d (ignored)".formatted(parameterIndex, sqlType));
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale) {
		LOGGER.log(Level.WARNING, () -> "Registering out parameter %d with type %d and scale %d (ignored)"
			.formatted(parameterIndex, sqlType, scale));
	}

	@SuppressWarnings("resource")
	@Override
	public boolean wasNull() throws SQLException {
		return assertCallAndPositionAtFirstRow().wasNull();
	}

	@SuppressWarnings("resource")
	@Override
	public String getString(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getString(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBoolean(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public byte getByte(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getByte(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public short getShort(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getShort(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public int getInt(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getInt(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public long getLong(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getLong(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public float getFloat(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getFloat(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public double getDouble(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDouble(parameterIndex);
	}

	@Override
	@SuppressWarnings({ "deprecation", "resource" })
	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBigDecimal(parameterIndex, scale);
	}

	@SuppressWarnings("resource")
	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBytes(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Date getDate(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDate(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Time getTime(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTime(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTimestamp(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Object getObject(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBigDecimal(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterIndex, map);
	}

	@SuppressWarnings("resource")
	@Override
	public Ref getRef(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getRef(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Blob getBlob(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBlob(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Clob getClob(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getClob(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Array getArray(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getArray(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDate(parameterIndex, cal);
	}

	@SuppressWarnings("resource")
	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTime(parameterIndex, cal);
	}

	@SuppressWarnings("resource")
	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTimestamp(parameterIndex, cal);
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, String typeName) {
		LOGGER.log(Level.WARNING, () -> "Registering out parameter %d with type %d and type %s (ignored)"
			.formatted(parameterIndex, sqlType, typeName));
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType) {
		LOGGER.log(Level.WARNING,
				() -> "Registering out parameter `%s` with type %d (ignored)".formatted(parameterName, sqlType));
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, int scale) {
		LOGGER.log(Level.WARNING, () -> "Registering out parameter `%s` with type %d and scale %d (ignored)"
			.formatted(parameterName, sqlType, scale));
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, String typeName) {
		LOGGER.log(Level.WARNING, () -> "Registering out parameter `%s` with type %d and type %s (ignored)"
			.formatted(parameterName, sqlType, typeName));
	}

	@Override
	public URL getURL(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getURL(parameterIndex);
	}

	@Override
	public void setURL(String parameterName, URL value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		Objects.requireNonNull(parameterName);
		super.setURL(parameterName, value);
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setNull(parameterName, sqlType);
	}

	@Override
	public void setBoolean(String parameterName, boolean value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setBoolean(parameterName, value);
	}

	@Override
	public void setByte(String parameterName, byte value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setByte(parameterName, value);
	}

	@Override
	public void setShort(String parameterName, short value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setShort(parameterName, value);
	}

	@Override
	public void setInt(String parameterName, int value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setInt(parameterName, value);
	}

	@Override
	public void setLong(String parameterName, long value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setLong(parameterName, value);
	}

	@Override
	public void setFloat(String parameterName, float value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setFloat(parameterName, value);
	}

	@Override
	public void setDouble(String parameterName, double value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setDouble(parameterName, value);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBigDecimal(parameterIndex, x);
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setBigDecimal(parameterName, value);
	}

	@Override
	public void setString(String parameterName, String value) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setString(parameterName, value);
	}

	@Override
	public void setBytes(String parameterName, byte[] bytes) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setBytes(parameterName, bytes);
	}

	@Override
	public void setDate(String parameterName, Date date) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setDate(parameterName, date);
	}

	@Override
	public void setTime(String parameterName, Time time) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setTime(parameterName, time);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp timestamp) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setTimestamp(parameterName, timestamp);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream inputStream, int length) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setAsciiStream0(parameterName, inputStream, length);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream inputStream, int length) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setBinaryStream0(parameterName, inputStream, length);
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
		assertParameterType(ParameterType.NAMED);
		super.setObject(parameterName, object);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setCharacterStream0(parameterName, reader, length);
	}

	@Override
	public void setDate(String parameterName, Date date, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setDate0(parameterName, date, calendar);
	}

	@Override
	public void setTime(String parameterName, Time time, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setTime0(parameterName, time, calendar);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp timestamp, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setTimestamp0(parameterName, timestamp, calendar);
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setNull(parameterName, sqlType);
	}

	@SuppressWarnings("resource")
	@Override
	public String getString(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getString(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public boolean getBoolean(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBoolean(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public byte getByte(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getByte(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public short getShort(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getShort(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public int getInt(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getInt(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public long getLong(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getLong(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public float getFloat(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getFloat(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public double getDouble(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDouble(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public byte[] getBytes(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBytes(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Date getDate(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDate(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Time getTime(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTime(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTimestamp(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Object getObject(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBigDecimal(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterName, map);
	}

	@SuppressWarnings("resource")
	@Override
	public Ref getRef(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getRef(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Blob getBlob(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getBlob(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Clob getClob(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getClob(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Array getArray(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getArray(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getDate(parameterName, cal);
	}

	@SuppressWarnings("resource")
	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTime(parameterName, cal);
	}

	@SuppressWarnings("resource")
	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		return assertCallAndPositionAtFirstRow().getTimestamp(parameterName, cal);
	}

	@SuppressWarnings("resource")
	@Override
	public URL getURL(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getURL(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public RowId getRowId(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getRowId(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public RowId getRowId(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getRowId(parameterName);
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setNString(String parameterName, String value) throws SQLException {
		setString(parameterName, value);
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

	@SuppressWarnings("resource")
	@Override
	public NClob getNClob(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNClob(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public NClob getNClob(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNClob(parameterName);
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@SuppressWarnings("resource")
	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getSQLXML(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getSQLXML(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public String getNString(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNString(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public String getNString(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNString(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNCharacterStream(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getNCharacterStream(parameterName);
	}

	@SuppressWarnings("resource")
	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		return assertCallAndPositionAtFirstRow().getCharacterStream(parameterIndex);
	}

	@SuppressWarnings("resource")
	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException {
		return assertCallAndPositionAtFirstRow().getCharacterStream(parameterName);
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
		setAsciiStream(parameterName, x, getLengthAsInt(length));
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
		setBinaryStream(parameterName, x, getLengthAsInt(length));
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
		setCharacterStream(parameterName, reader, getLengthAsInt(length));
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setAsciiStream(parameterIndex, x);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setAsciiStream(parameterName, x);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBinaryStream(parameterIndex, x);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setBinaryStream(parameterName, x);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setCharacterStream(parameterName, reader);
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader reader) throws SQLException {
		setCharacterStream(parameterName, reader);
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

	@SuppressWarnings("resource")
	@Override
	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterIndex, type);
	}

	@SuppressWarnings("resource")
	@Override
	public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		return assertCallAndPositionAtFirstRow().getObject(parameterName, type);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setNull(parameterIndex, sqlType);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBoolean(parameterIndex, value);
	}

	@Override
	public void setByte(int parameterIndex, byte value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setByte(parameterIndex, value);
	}

	@Override
	public void setShort(int parameterIndex, short value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setShort(parameterIndex, value);
	}

	@Override
	public void setInt(int parameterIndex, int value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setInt(parameterIndex, value);
	}

	@Override
	public void setLong(int parameterIndex, long value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setLong(parameterIndex, value);
	}

	@Override
	public void setFloat(int parameterIndex, float value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setFloat(parameterIndex, value);
	}

	@Override
	public void setDouble(int parameterIndex, double value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setDouble(parameterIndex, value);
	}

	@Override
	public void setString(int parameterIndex, String value) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setString(parameterIndex, value);
	}

	@Override
	public void setURL(int parameterIndex, URL url) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setURL(parameterIndex, url);
	}

	@Override
	public void setBytes(int parameterIndex, byte[] bytes) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBytes(parameterIndex, bytes);
	}

	@Override
	public void setDate(int parameterIndex, Date date) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setDate(parameterIndex, date);
	}

	@Override
	public void setTime(int parameterIndex, Time time) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setTime(parameterIndex, time);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setTimestamp(parameterIndex, timestamp);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setAsciiStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, int length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBinaryStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setObject(int parameterIndex, Object object) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setObject(parameterIndex, object);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setDate(int parameterIndex, Date date, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setDate(parameterIndex, date, calendar);
	}

	@Override
	public void setTime(int parameterIndex, Time time, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setTime(parameterIndex, time, calendar);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp timestamp, Calendar calendar) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setTimestamp(parameterIndex, timestamp, calendar);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setAsciiStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setBinaryStream(parameterIndex, inputStream, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setCharacterStream(parameterIndex, reader, length);
	}

	private void assertParameterType(ParameterType parameterType) throws SQLException {
		if (this.parameterType == null) {
			this.parameterType = parameterType;
		}
		else if (this.parameterType != parameterType) {
			var hlp = new RuntimeException();
			for (var stackTraceElement : hlp.getStackTrace()) {
				if (stackTraceElement.getMethodName().equals("setObjectParameter")) {
					return;
				}
			}
			throw new Neo4jException(GQLError.$42N51.withMessage(String
				.format("%s parameter can not be mixed with %s parameter(s)", parameterType, this.parameterType)));
		}
	}

	@Override
	public int executeUpdate() throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void addBatch() throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw newIllegalMethodInvocation();
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		assertParameterType(ParameterType.ORDINAL);
		super.setArray(parameterIndex, x);
	}

	@Override
	public void setArray(String parameterName, Array x) throws SQLException {
		assertParameterType(ParameterType.NAMED);
		super.setArray(parameterName, x);
	}

	private enum ParameterType {

		/**
		 * Denotates parameter defined by their numeric index.
		 */
		ORDINAL,
		/**
		 * Parameters given by name (that is, in Cypher format such as {@code $name}.
		 */
		NAMED

	}

	@SuppressWarnings({ "squid:S3776" })
	static ParameterListDescriptor parseParameterList(String parameterList) {

		if (parameterList == null) {
			return new ParameterListDescriptor(Map.of(), Map.of(), Map.of());
		}

		var ordinalParameters = new HashMap<Integer, Integer>();
		var namedParameters = new HashMap<Integer, String>();
		var constants = new HashMap<Integer, String>();

		int cnt = 0;
		for (String s : PARAMETER_LIST_SPLITTER.split(parameterList.trim())) {
			++cnt;
			var possibleParameter = s.trim();
			if (possibleParameter.isEmpty()) {
				continue;
			}
			if ("?".equals(possibleParameter)) {
				ordinalParameters.put(cnt, -1);
			}
			else if (IS_NUMBER.test(possibleParameter)) {
				ordinalParameters.put(cnt, Integer.parseInt(possibleParameter.replace("$", "")));
			}
			else if (possibleParameter.startsWith("$") || possibleParameter.startsWith(":")) {
				var v = possibleParameter.substring(1);
				var matcher = VALID_IDENTIFIER_PATTERN.matcher(v);
				if (matcher.matches() || "0".equals(v)) {
					namedParameters.put(cnt, v);
				}
			}
			else {
				constants.put(cnt, possibleParameter);
			}
		}

		assertEitherOrdinalOrNamedParameters(ordinalParameters, namedParameters);
		makeDense(ordinalParameters);

		return new ParameterListDescriptor(ordinalParameters, namedParameters, constants);
	}

	private static void makeDense(HashMap<Integer, Integer> ordinalParameters) {

		var used = ordinalParameters.values().stream().filter(i -> i > 0).collect(Collectors.toSet());
		int max = 1;
		for (Map.Entry<Integer, Integer> entry : ordinalParameters.entrySet()) {
			Integer key = entry.getKey();
			Integer v = entry.getValue();
			if (v < 0) {
				while (used.contains(max)) {
					++max;
				}
				v = max++;
			}
			ordinalParameters.put(key, v);
		}
	}

	private static void assertEitherOrdinalOrNamedParameters(Map<Integer, Integer> ordinalParameters,
			Map<Integer, String> namedParameters) {
		if (!(ordinalParameters.isEmpty() || namedParameters.isEmpty())) {
			throw new IllegalArgumentException("Index- and named ordinalParameters cannot be mixed");
		}
	}

	/**
	 * Parses a statement into a {@link Descriptor descriptor}. Supported formats are
	 * <ul>
	 * <li>The JDBC syntax <code>{call fqn(&lt;?, ...&gt;)}</code> or
	 * <code>{? = call fqn(&lt;?, ...&gt;)}</code></li>
	 * <li>The Cypher simplified variant <code>{CALL fqn(&lt;?, ...&gt;)</code></li>
	 * <li>Cypher function calls <code>RETURN fqn(&lt;?, ...&gt;)</code>
	 * </ul>
	 * Other formats are not supported
	 * @param statement the statement to be parsed
	 * @return a descriptor to be used within this {@link CallableStatementImpl callable
	 * statement implementation}
	 * @throws IllegalArgumentException if the statement cannot be parsed
	 */
	static Descriptor parse(String statement) {

		if (Objects.requireNonNull(statement, "Callable statements cannot be null").isBlank()) {
			throw new IllegalArgumentException("Callable statements cannot be blank");
		}

		statement = statement.trim();
		var matcher = JDBC_CALL.matcher(statement);
		try {
			if (matcher.matches()) {
				return describeJdbcCall(matcher);
			}

			matcher = CYPHER_RETURN_CALL.matcher(statement);
			if (matcher.matches()) {
				return describeCypherReturnCall(matcher);
			}

			matcher = CYPHER_YIELD_CALL.matcher(statement);
			if (matcher.matches()) {
				return describeCypherYieldCall(matcher);
			}

			matcher = CYPHER_SIDE_EFFECT_CALL.matcher(statement);
			if (matcher.matches()) {
				return describeCypherSideEffectCall(matcher);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(ex.getMessage() + ": `" + statement + "`");
		}

		throw new IllegalArgumentException("Cannot create a callable statement from `" + statement + "`");
	}

	private static Descriptor describeCypherSideEffectCall(Matcher matcher) {
		var parameterList = parseParameterList(matcher.group("parameterList"));
		return new Descriptor(matcher.group("fqn"), ReturnType.NONE, null, parameterList, false);
	}

	private static Descriptor describeCypherYieldCall(Matcher matcher) {
		var yieldedStuff = Optional.ofNullable(matcher.group("yieldedValues")).map(String::trim).orElse("");
		List<String> returnParameterName = new ArrayList<>();
		for (String s : PARAMETER_LIST_SPLITTER.split(yieldedStuff)) {
			if (!s.isBlank()) {
				returnParameterName.add(s.trim());
			}
		}

		var returnType = returnParameterName.isEmpty() ? ReturnType.ORDINAL : ReturnType.NAMED;
		var parameterList = parseParameterList(matcher.group("parameterList"));
		return new Descriptor(matcher.group("fqn"), returnType, returnParameterName, parameterList, false);
	}

	private static Descriptor describeCypherReturnCall(Matcher matcher) {
		var parameterList = parseParameterList(matcher.group("parameterList"));
		return new Descriptor(matcher.group("fqn"), ReturnType.ORDINAL, null, parameterList, true);
	}

	private static Descriptor describeJdbcCall(Matcher matcher) {
		var returnParameter = Optional.ofNullable(matcher.group("returnParameter")).map(String::trim).orElse("");
		var returnType = ReturnType.NONE;
		List<String> returnParameterName = new ArrayList<>();
		if (!returnParameter.isBlank()) {
			Optional.ofNullable(matcher.group("returnParameterName"))
				.map(String::trim)
				.map(s -> s.substring(1))
				.ifPresent(returnParameterName::add);
			returnType = returnParameterName.isEmpty() ? ReturnType.ORDINAL : ReturnType.NAMED;
		}
		var parameterList = parseParameterList(matcher.group("parameterList"));
		return new Descriptor(matcher.group("fqn"), returnType, returnParameterName, parameterList, null);
	}

	private static final Pattern PARAMETER_LIST_SPLITTER = Pattern
		.compile(",(?=(?:[^\"']*[\"'][^\"']*[\"'])*[^\"']*\\Z)");

	private static final String VALID_IDENTIFIER = "\\p{javaJavaIdentifierStart}[.\\p{javaJavaIdentifierPart}]*";

	private static final Predicate<String> IS_NUMBER = Pattern.compile("\\$?[1-9]+").asMatchPredicate();

	private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile(VALID_IDENTIFIER);

	private static final String WS = "\\s*+";

	private static final String RETURN_PARAMETER = "(?<returnParameter>" + WS + "(?:\\?|(?<returnParameterName>[$:]"
			+ VALID_IDENTIFIER_PATTERN.pattern() + "))" + WS + "=" + WS + ")?";

	private static final String PARAMETER_LIST = "(?:\\((?<parameterList>.*)\\))?";

	/**
	 * A regular expression for the fully qualified method name and its parameter list.
	 * @deprecated No replacement, not to be used externally
	 */
	@Deprecated(forRemoval = true, since = "6.5.0")
	public static final String FQN_AND_PARAMETER_LIST = "(?<fqn>" + VALID_IDENTIFIER + ")" + WS + PARAMETER_LIST;

	private static final Pattern JDBC_CALL = Pattern
		.compile("(?i)" + WS + "\\{" + RETURN_PARAMETER + "call " + WS + FQN_AND_PARAMETER_LIST + WS + "}");

	private static final Pattern CYPHER_RETURN_CALL = Pattern
		.compile("(?i)" + WS + "RETURN " + WS + FQN_AND_PARAMETER_LIST);

	private static final Pattern CYPHER_YIELD_CALL = Pattern
		.compile("(?i)" + WS + "CALL " + WS + FQN_AND_PARAMETER_LIST + WS + "YIELD " + WS + "(\\*|(?<yieldedValues>"
				+ VALID_IDENTIFIER_PATTERN.pattern() + "(?:," + WS + VALID_IDENTIFIER_PATTERN.pattern() + ")*))");

	private static final Pattern CYPHER_SIDE_EFFECT_CALL = Pattern
		.compile("(?i)" + WS + "CALL " + WS + FQN_AND_PARAMETER_LIST);

	enum ReturnType {

		/**
		 * The statement won't return anything.
		 */
		NONE,
		/**
		 * The statement returns data to be retrieved by index.
		 */
		ORDINAL,
		/**
		 * The statement returns data by name.
		 */
		NAMED

	}

	/**
	 * Tuple needed for describing the parameter list.
	 *
	 * @param ordinalParameters all ordinal parameters
	 * @param namedParameters all named parameters
	 * @param constants all constant values
	 */
	record ParameterListDescriptor(Map<Integer, Integer> ordinalParameters, Map<Integer, String> namedParameters,
			Map<Integer, String> constants) {

		String toCypher(Map<String, Integer> parameterOrder) {

			if (isEmpty()) {
				return "";
			}

			var all = new TreeMap<Integer, String>();
			this.ordinalParameters.forEach((k, v) -> all.put(k, "$" + v));
			this.namedParameters.forEach((k, v) -> {
				var idx = parameterOrder.getOrDefault(v, k);
				all.put(idx, "$" + v);
			});
			all.putAll(this.constants);
			return all.values().stream().collect(Collectors.joining(", ", "(", ")"));
		}

		boolean isEmpty() {
			return this.ordinalParameters.isEmpty() && this.namedParameters().isEmpty() && this.constants.isEmpty();
		}
	}

	/**
	 * A descriptor of a callable statement containing the fully qualified name of the
	 * function or method to be called as well as the list of ordinalParameters and their
	 * type (in, out, inout).
	 *
	 * @param fqn the fully qualified name of the function or procedure to call
	 * @param returnType return type for the statement
	 * @param yieldedValues optional name for the out (return parameter)
	 * @param parameterList parameter list
	 * @param isFunctionCall {@literal true} when the statement is represented as `RETURN
	 * xyz()` function call
	 */
	record Descriptor(String fqn, ReturnType returnType, List<String> yieldedValues,
			ParameterListDescriptor parameterList, Boolean isFunctionCall) {

		Descriptor {
			if (yieldedValues != null && !yieldedValues.isEmpty() && returnType != ReturnType.NAMED) {
				throw new IllegalArgumentException(
						"A name for the return parameter is only supported with named returns");
			}
			if (returnType == ReturnType.NAMED && !parameterList.ordinalParameters.isEmpty()
					|| !(parameterList.ordinalParameters.isEmpty() || parameterList.namedParameters().isEmpty())) {
				throw new IllegalArgumentException("Index- and named ordinalParameters cannot be mixed");
			}
			if (!(parameterList.namedParameters.isEmpty() || parameterList.constants.isEmpty())) {
				throw new IllegalArgumentException("Named parameters cannot be used together with constant arguments");
			}
		}

		boolean isUsingNamedParameters() {
			return !this.parameterList.namedParameters.isEmpty();
		}

		String toCypher(Map<String, Integer> parameterOrder) {
			var sb = new StringBuilder();
			var isSafeFunctionCall = Boolean.TRUE.equals(this.isFunctionCall);
			if (isSafeFunctionCall) {
				sb.append("RETURN");
			}
			else {
				sb.append("CALL");
			}
			sb.append(" ").append(this.fqn).append(this.parameterList.toCypher(parameterOrder));

			if (this.returnType == ReturnType.ORDINAL && !isSafeFunctionCall) {
				sb.append(" YIELD *");
			}
			else if (this.returnType == ReturnType.NAMED) {
				sb.append(" YIELD ").append(String.join(", ", this.yieldedValues));
			}

			return sb.toString();
		}

		boolean hasParameters() {
			return !this.parameterList.isEmpty();
		}
	}

}
