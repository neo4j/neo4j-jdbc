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
import org.neo4j.jdbc.utils.PreparedStatementBuilder;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.*;

import static java.sql.Types.*;

/**
 * Don't forget to close some attribute (like currentResultSet and currentUpdateCount) or your implementation.
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Neo4jPreparedStatement extends Neo4jStatement implements PreparedStatement {

	protected String                    statement;
	protected HashMap<String, Object>   parameters;
	protected List<Map<String, Object>> batchParameters;
	private   int                       parametersNumber;
	private static final List<Integer> UNSUPPORTED_TYPES = Collections .unmodifiableList(
			Arrays.asList(
					ARRAY,
					BLOB,
					CLOB,
					DATALINK,
					JAVA_OBJECT,
					NCHAR,
					NCLOB,
					NVARCHAR,
					LONGNVARCHAR,
					REF,
					ROWID,
					SQLXML,
					STRUCT
			)
	);

	/**
	 * Default constructor with connection and statement.
	 *
	 * @param connection   The JDBC connection
	 * @param rawStatement The prepared statement
	 */
	protected Neo4jPreparedStatement(Neo4jConnection connection, String rawStatement) {
		super(connection);
		this.statement = PreparedStatementBuilder.replacePlaceholders(rawStatement);
		this.parametersNumber = PreparedStatementBuilder.namedParameterCount(statement);
		this.parameters = new HashMap<>(this.parametersNumber);
		this.batchParameters = new ArrayList<>();
	}

	/*----------------------------------------*/
	/*       Some useful, check method        */
	/*----------------------------------------*/

	/**
	 * Check if the given parameter index is not out of bound.
	 * If its is we throw an exception.
	 *
	 * @param parameterIndex The index parameter to check
	 */
	protected void checkParamsNumber(int parameterIndex) throws SQLException {
		if (parameterIndex > this.parametersNumber) {
			throw new SQLException("ParameterIndex does not correspond to a parameter marker in the SQL statement");
		}
	}

	/**
	 * Check if the given object is a valid type that Neo4J can handle.
	 * If it's not we throw an exception.
	 *
	 * @param obj The object to check
	 */
	private void checkValidObject(Object obj) throws SQLException {
		// TODO: this may belong into org.neo4j.driver.v1.Values
		if (!(obj == null ||
				obj instanceof Boolean ||
				obj instanceof String ||
				obj instanceof Character ||
				obj instanceof Long ||
				obj instanceof Short ||
				obj instanceof Byte ||
				obj instanceof Integer ||
				obj instanceof Double ||
				obj instanceof Float ||
				obj instanceof List ||
				obj instanceof Iterable ||
				obj instanceof Map ||
				obj instanceof Iterator ||
				obj instanceof boolean[] ||
				obj instanceof String[] ||
				obj instanceof long[] ||
				obj instanceof int[] ||
				obj instanceof double[] ||
				obj instanceof float[] ||
				obj instanceof Object[])) {
			throw new SQLException("Object of type '" + obj.getClass() + "' isn't supported");
		}
	}

	/**
	 * Insert a parameter into the map.
	 *
	 * @param index The index/key of the parameter
	 * @param obj   The value of the parameter
	 */
	protected void insertParameter(int index, Object obj) {
		this.parameters.put(Integer.toString(index), obj);
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	@Override public void setNull(int parameterIndex, int sqlType) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		if (UNSUPPORTED_TYPES.contains(sqlType)) {
			throw new SQLFeatureNotSupportedException("The Type you specified is not supported");
		}
		this.insertParameter(parameterIndex, null);
	}

	@Override public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setShort(int parameterIndex, short x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setInt(int parameterIndex, int x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setLong(int parameterIndex, long x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setFloat(int parameterIndex, float x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setDouble(int parameterIndex, double x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setString(int parameterIndex, String x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void setObject(int parameterIndex, Object x) throws SQLException {
		this.checkClosed();
		this.checkParamsNumber(parameterIndex);
		this.checkValidObject(x);
		this.insertParameter(parameterIndex, x);
	}

	@Override public void clearParameters() throws SQLException {
		this.checkClosed();
		this.parameters.clear();
	}

	@Override public void addBatch() throws SQLException {
		this.checkClosed();
		this.batchParameters.add(new HashMap<>(this.parameters));
		this.parameters.clear();
	}

	@Override public void clearBatch() throws SQLException {
		this.checkClosed();
		this.batchParameters.clear();
	}

	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	@Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLException("Method execute(String, int) cannot be called on PreparedStatement");
	}

	@Override public boolean execute(String sql) throws SQLException {
		throw new SQLException("Method execute(String) cannot be called on PreparedStatement");
	}

	@Override public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLException("Method execute(String, int[]) cannot be called on PreparedStatement");
	}

	@Override public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLException("Method execute(String, String[]) cannot be called on PreparedStatement");
	}

	@Override public ResultSet executeQuery(String sql) throws SQLException {
		throw new SQLException("Method executeQuery(String) cannot be called on PreparedStatement");
	}

	@Override public int executeUpdate(String sql) throws SQLException {
		throw new SQLException("Method executeUpdate(String) cannot be called on PreparedStatement");
	}

	@Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLException("Method executeUpdate(String, int) cannot be called on PreparedStatement");
	}

	@Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLException("Method executeUpdate(String, int[]) cannot be called on PreparedStatement");
	}

	@Override public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw new SQLException("Method executeUpdate(String, String[]) cannot be called on PreparedStatement");
	}

	@Override public void setByte(int parameterIndex, byte x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setDate(int parameterIndex, Date x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setTime(int parameterIndex, Time x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setRef(int parameterIndex, Ref x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBlob(int parameterIndex, Blob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setClob(int parameterIndex, Clob x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setArray(int parameterIndex, java.sql.Array x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setURL(int parameterIndex, URL x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setRowId(int parameterIndex, RowId x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNString(int parameterIndex, String value) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNClob(int parameterIndex, NClob value) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setClob(int parameterIndex, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void addBatch(String sql) throws SQLException {
		throw new SQLException("Method addBatch(String sql) cannot be called on PreparedStatement");
	}

}
