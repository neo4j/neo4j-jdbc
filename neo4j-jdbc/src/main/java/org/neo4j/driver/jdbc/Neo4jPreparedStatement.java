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

import java.sql.PreparedStatement;
import java.sql.SQLException;

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

	// TODO remaining mutators

}
