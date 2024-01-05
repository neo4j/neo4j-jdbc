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
package org.neo4j.driver.jdbc.docs;

// tag::imports[]

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

// end::imports[]

public final class SQLTranslator {

	private static final Logger LOGGER = Logger.getAnonymousLogger();

	private SQLTranslator() {
	}

	public static void main(String... args) throws SQLException {
		caseByCase();
		alwaysWithUrlConfig();
		escapeHatch();
		config1();
		config2();
		config3();
	}

	static void caseByCase() throws SQLException {

		var url = "jdbc:neo4j://localhost:7687";
		var username = "neo4j";
		var password = "verysecret";

		// tag::pt1[]
		try (var connection = DriverManager.getConnection(url, username, password)) {
			var sql = connection.nativeSQL("SELECT * FROM Movie n");
			assert """
					MATCH (n:Movie)
					RETURN *""".equals(sql);
		}
		// end::pt1[]
	}

	static void alwaysWithUrlConfig() throws SQLException {

		var username = "neo4j";
		var password = "verysecret";

		// tag::pt2[]
		var url = "jdbc:neo4j://localhost:7687?sql2cypher=true";
		try (var connection = DriverManager.getConnection(url, username, password);
				var stmnt = connection.createStatement();
				var result = stmnt.executeQuery("SELECT n.title FROM Movie n")) {
			while (result.next()) {
				LOGGER.info(result.getString("n.title"));
			}
		}
		// end::pt2[]
	}

	static void escapeHatch() throws SQLException {

		var username = "neo4j";
		var password = "verysecret";

		// tag::force-cypher[]
		var url = "jdbc:neo4j://localhost:7687?sql2cypher=true";
		var query = """
				/*+ NEO4J FORCE_CYPHER */
				MATCH (:Station { name: 'Denmark Hill' })<-[:CALLS_AT]-(d:Stop)
					((:Stop)-[:NEXT]->(:Stop)){1,3}
					(a:Stop)-[:CALLS_AT]->(:Station { name: 'Clapham Junction' })
				RETURN localtime(d.departs) AS departureTime,
					localtime(a.arrives) AS arrivalTime
				""";
		try (var connection = DriverManager.getConnection(url, username, password);
				var stmnt = connection.createStatement();
				var result = stmnt.executeQuery(query)) {
			while (result.next()) {
				LOGGER.info(result.getTime("departureTime").toString());
			}
		}
		// end::force-cypher[]
	}

	static void config1() throws SQLException {

		// tag::config1[]
		var properties = new Properties();
		properties.put("username", "neo4j");
		properties.put("password", "verysecret");
		properties.put("sql2cypher", "true");
		properties.put("s2c.prettyPrint", "false");
		properties.put("s2c.alwaysEscapeNames", "false");
		properties.put("s2c.tableToLabelMappings", "people:Person;movies:Movie;movie_actors:ACTED_IN");

		var url = "jdbc:neo4j://localhost:7687";
		var query = """
				SELECT p.name, m.title
				FROM people p
				JOIN movie_actors r ON r.person_id = p.id
				JOIN movies m ON m.id = r.person_id""";
		try (var connection = DriverManager.getConnection(url, properties)) {
			var sql = connection.nativeSQL(query);
			assert "MATCH (p:Person)-[r:ACTED_IN]->(m:Movie) RETURN p.name, m.title".equals(sql);
		}
		// end::config1[]
	}

	static void config2() throws SQLException {

		// tag::config2[]
		var properties = new Properties();
		properties.put("username", "neo4j");
		properties.put("password", "verysecret");
		properties.put("sql2cypher", "true");
		properties.put("s2c.parseNameCase", "UPPER");

		var url = "jdbc:neo4j://localhost:7687";
		var query = "SELECT * FROM people";
		try (var connection = DriverManager.getConnection(url, properties)) {
			var sql = connection.nativeSQL(query);
			assert """
					MATCH (people:PEOPLE)
					RETURN *""".equals(sql);
		}
		// end::config2[]
	}

	static void config3() throws SQLException {

		// tag::config3[]
		var properties = new Properties();
		properties.put("username", "neo4j");
		properties.put("password", "verysecret");
		properties.put("sql2cypher", "true");
		properties.put("s2c.parseNamedParamPrefix", "$");
		properties.put("s2c.joinColumnsToTypeMappings", "people.movie_id:DIRECTED");

		var url = "jdbc:neo4j://localhost:7687";
		var query = """
				SELECT *
				FROM people p
				JOIN movies m ON m.id = p.movie_id
				WHERE p.name = $1
				""";
		try (var connection = DriverManager.getConnection(url, properties)) {
			var sql = connection.nativeSQL(query);
			assert """
					MATCH (p:people)-[:DIRECTED]->(m:movies)
					WHERE p.name = $1
					RETURN *""".equals(sql);
		}
		// end::config3[]
	}

}
