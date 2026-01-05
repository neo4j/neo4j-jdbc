/*
 * Copyright (c) 2023-2026 "Neo4j,"
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
import java.sql.Array;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * A Neo4j specific extension of a {@link PreparedStatement}. It may be referred to for
 * use with {@link #unwrap(Class)} to access specific Neo4j functionality.
 *
 * @author Michael J. Simons
 * @author Conor Watson
 * @author Dmitriy Tverdiakov
 * @since 6.0.0
 */
public sealed interface Neo4jPreparedStatement extends PreparedStatement permits PreparedStatementImpl {

	/**
	 * Named-parameter version of {@link #setString(int, String)}.
	 * @param parameterName the parameter name
	 * @param string the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setString(int, String)
	 */
	void setString(String parameterName, String string) throws SQLException;

	/**
	 * Named-parameter version of {@link #setInt(int, int)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setInt(int, int)
	 */
	void setInt(String parameterName, int value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setDate(int, Date)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setDate(int, Date)
	 */
	void setDate(String parameterName, Date value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setTime(int, Time)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setTime(int, Time)
	 */
	void setTime(String parameterName, Time value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setTimestamp(int, Timestamp)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setTimestamp(int, Timestamp)
	 */
	void setTimestamp(String parameterName, Timestamp value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setObject(int, Object)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setObject(int, Object)
	 */
	void setObject(String parameterName, Object value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setCharacterStream(int, Reader)}.
	 * @param parameterName the parameter name
	 * @param reader the reader to read from
	 * @throws SQLException when a connection or database error occurs
	 * @see #setCharacterStream(int, Reader)
	 */
	void setCharacterStream(String parameterName, Reader reader) throws SQLException;

	/**
	 * Named-parameter version of {@link #setAsciiStream(int, InputStream)}.
	 * @param parameterName the parameter name
	 * @param stream the stream to read from
	 * @throws SQLException when a connection or database error occurs
	 * @see #setAsciiStream(int, InputStream)
	 */
	void setAsciiStream(String parameterName, InputStream stream) throws SQLException;

	/**
	 * Named-parameter version of {@link #setBinaryStream(int, InputStream)}.
	 * @param parameterName the parameter name
	 * @param stream the stream to read from
	 * @throws SQLException when a connection or database error occurs
	 * @see #setBinaryStream(int, InputStream)
	 */
	void setBinaryStream(String parameterName, InputStream stream) throws SQLException;

	/**
	 * Named-parameter version of {@link #setNull(int, int)}.
	 * @param parameterName the parameter name
	 * @param sqlType the SQL type
	 * @throws SQLException when a connection or database error occurs
	 * @see #setNull(int, int)
	 */
	void setNull(String parameterName, int sqlType) throws SQLException;

	/**
	 * Named-parameter version of {@link #setBoolean(int, boolean)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setBoolean(int, boolean)
	 */
	void setBoolean(String parameterName, boolean value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setByte(int, byte)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setByte(int, byte)
	 */
	void setByte(String parameterName, byte value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setShort(int, short)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setShort(int, short)
	 */
	void setShort(String parameterName, short value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setLong(int, long)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setLong(int, long)
	 */
	void setLong(String parameterName, long value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setFloat(int, float)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setFloat(int, float)
	 */
	void setFloat(String parameterName, float value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setDouble(int, double)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setDouble(int, double)
	 */
	void setDouble(String parameterName, double value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setBigDecimal(int, BigDecimal)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setBigDecimal(int, BigDecimal)
	 */
	void setBigDecimal(String parameterName, BigDecimal value) throws SQLException;

	/**
	 * Named-parameter version of {@link #setBytes(int, byte[])}.
	 * @param parameterName the parameter name
	 * @param bytes the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @see #setBytes(int, byte[])
	 */
	void setBytes(String parameterName, byte[] bytes) throws SQLException;

	/**
	 * Named-parameter version of {@link #setArray(int, Array)}.
	 * @param parameterName the parameter name
	 * @param value the parameter value
	 * @throws SQLException when a connection or database error occurs
	 * @since 6.4.0
	 * @see #setArray(int, Array)
	 */
	void setArray(String parameterName, Array value) throws SQLException;

}
