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
 */
public sealed interface Neo4jPreparedStatement
		extends PreparedStatement permits PreparedStatementImpl, CallableStatementImpl {

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

	// TODO remaining mutators

}
