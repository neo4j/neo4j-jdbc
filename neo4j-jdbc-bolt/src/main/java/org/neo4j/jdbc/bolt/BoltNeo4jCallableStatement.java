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
 * Created on 21/04/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.jdbc.Neo4jCallableStatement;
import org.neo4j.jdbc.Neo4jConnection;

import java.sql.ParameterMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
class BoltNeo4jCallableStatement extends Neo4jCallableStatement {
	/**
	 * Default constructor with connection and statement.
	 *
	 * @param connection   The JDBC connection
	 * @param rawStatement The prepared statement
	 */
	protected BoltNeo4jCallableStatement(Neo4jConnection connection, String rawStatement) {
		super(connection, rawStatement);
	}

	@Override public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	@Override public ParameterMetaData getParameterMetaData() throws SQLException {
		return null;
	}
}
