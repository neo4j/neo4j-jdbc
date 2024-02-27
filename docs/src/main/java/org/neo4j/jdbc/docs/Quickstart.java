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
package org.neo4j.jdbc.docs;

// tag::pt1[]

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

// end::pt1[]

import org.neo4j.jdbc.Neo4jDriver;

@SuppressWarnings("unchecked")
// tag::pt1[]
public final class Quickstart {

	// end::pt1[]

	private Quickstart() {
	}

	public static void main(String... args) {
		queryWithCypher();
		queryWithSQL();
		connectFromEnv();
	}

	// tag::pt1[]
	static void queryWithCypher() {
		var query = """
				MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title
				""";

		var url = "jdbc:neo4j://localhost:7687";
		var username = "neo4j";
		var password = "verysecret";

		try (var con = DriverManager.getConnection(url, username, password); // <.>
				var stmt = con.createStatement(); // <.>
				var result = stmt.executeQuery(query)) { // <.>

			while (result.next()) { // <.>
				var movie = result.getString(1); // <.>
				var actors = (List<String>) result.getObject("actors"); // <.>
				System.out.printf("%s%n", movie);
				actors.forEach(actor -> System.out.printf("\t * %s%n", actor));
			}
		}
		catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	// end::pt1[]

	static void queryWithSQL() {

		var username = "neo4j";
		var password = "verysecret";

		// tag::pt2[]
		var query = """
				SELECT m.title AS title, collect(p.name) AS actors
				FROM Person as p
				JOIN Movie as m ON (m.id = p.ACTED_IN)
				ORDER BY m.title
				"""; // <.>

		var url = "jdbc:neo4j://localhost:7687?enableSQLTranslation=true";

		try (var con = DriverManager.getConnection(url, username, password);
				var stmt = con.createStatement();
				var result = stmt.executeQuery(query)) {

			while (result.next()) {
				var movie = result.getString(1);
				var actors = (List<String>) result.getObject("actors");
				System.out.printf("%s%n", movie);
				actors.forEach(actor -> System.out.printf("\t * %s%n", actor));
			}
		}
		// end::pt2[]
		catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	static void connectFromEnv() {
		var query = """
				MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title
				""";

		// tag::pt3[]
		// import org.neo4j.jdbc.Neo4jDriver;

		try (var con = Neo4jDriver.fromEnv().orElseThrow(); // <.>
				var stmt = con.createStatement();
				var result = stmt.executeQuery(query)) {

			// Same loop as earlier
			// end::pt3[]
			while (result.next()) {
				var movie = result.getString(1);
				var actors = (List<String>) result.getObject("actors");
				System.out.printf("%s%n", movie);
				actors.forEach(actor -> System.out.printf("\t * %s%n", actor));
			}
			// tag::pt3[]
		}
		catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
		// end::pt3[]
	}

	// tag::pt1[]

}
// end::pt1[]
