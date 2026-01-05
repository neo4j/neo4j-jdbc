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
package org.neo4j.jdbc.docs;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.neo4j.jdbc.Neo4jPreparedStatement;

public final class NamedParameters {

	private static final Logger LOGGER = Logger.getLogger(NamedParameters.class.getPackageName());

	private NamedParameters() {
	}

	// This is about Sonar not recognizing our named parameter variant; also, let's throw
	// all the things
	@SuppressWarnings({ "squid:S2695", "squid:S2096", "squid:S2068" })
	public static void main(String... a) throws SQLException {
		var url = "jdbc:neo4j://localhost:7687";
		var username = "neo4j";
		var password = "verysecret";

		// tag::index[]
		var cypher = "CREATE (m:Movie {title: $1})";
		try (var con = DriverManager.getConnection(url, username, password);
				PreparedStatement stmt = con.prepareStatement(cypher)) {
			stmt.setString(1, "Test");
			stmt.executeUpdate();
		}
		// end::index[]

		// tag::index-sql[]
		var sql = "INSERT INTO Movie (title) VALUES (?)";
		try (var con = DriverManager.getConnection(url + "?enableSQLTranslation=true", username, password);
				PreparedStatement stmt = con.prepareStatement(sql)) {
			stmt.setString(1, "Test");
			stmt.executeUpdate();
		}
		// end::index-sql[]

		// tag::index-np[]
		var match = "MATCH (n:Movie {title: $title}) RETURN n.title AS title";
		try (var con = DriverManager.getConnection(url, username, password);
				Neo4jPreparedStatement stmt = con.prepareStatement(match).unwrap(Neo4jPreparedStatement.class)) {
			stmt.setString("title", "Test");
			try (var resultSet = stmt.executeQuery()) {
				while (resultSet.next()) {
					LOGGER.info(resultSet.getString("title"));
				}
			}
		}
		// end::index-np[]
	}

}
