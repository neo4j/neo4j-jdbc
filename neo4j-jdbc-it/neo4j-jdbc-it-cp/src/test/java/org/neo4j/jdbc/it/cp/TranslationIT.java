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
package org.neo4j.jdbc.it.cp;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jooq.impl.ParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.values.Node;
import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TranslationIT extends IntegrationTestBase {

	@Test
	void shouldLoadTranslatorDirectly() throws SQLException {

		var url = "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		properties.put("translatorFactory", "default");
		properties.put("enableSQLTranslation", "true");
		properties.put("cacheSQLTranslations", "true");

		try (var con = driver.connect(url, properties);
				var stmt = con.createStatement();
				var rs = stmt.executeQuery("SELECT 1")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getInt(1)).isOne();
		}

	}

	@Test
	void shouldFailWhenLoadingInvalidClassDirectly() throws SQLException {

		var url = "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		properties.put("translatorFactory", "asd");
		properties.put("enableSQLTranslation", "true");

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect(url, properties))
			.withMessage("general processing exception - No translators available");

	}

	@Test
	void shouldTranslateAsterisk() throws SQLException {

		var title = "JDBC the Sequel";

		try (var connection = getConnection(true, false)) {
			try (var statement = (Neo4jPreparedStatement) connection
				.prepareStatement("/*+ NEO4J FORCE_CYPHER */ CREATE (m:Movie {title:  $title, released: $released})")) {
				statement.setString("title", title);
				statement.setInt("released", 2024);
				statement.execute();
			}

			try (var statement = connection.createStatement();
					var rs = statement.executeQuery("SELECT * FROM \"Movie\"")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getString("title")).isEqualTo(title);
				assertThat(rs.getInt("released")).isEqualTo(2024);
			}
		}

	}

	@Test
	void shouldInsertRelationshipBasedOnTemplate() throws SQLException {

		try (var connection = getConnection(true, true)) {
			try (var statement = connection.prepareStatement(
					"/*+ NEO4J FORCE_CYPHER */ CREATE (a:Person {name: 'Jaret Leto'})-[:ACTED_IN {role: 'Ares'}]->(m:Movie {title: 'TRON Ares'})")) {
				statement.executeUpdate();
			}

			try (var statement = connection
				.prepareStatement("INSERT INTO Person_ACTED_IN_Movie(name, role, title) VALUES(?, ?, ?)")) {
				statement.setString(1, "Jodie Turner-Smith");
				statement.setString(2, "Athena");
				statement.setString(3, "TRON Ares");
				statement.execute();
			}

			try (var statement = connection
				.prepareStatement("INSERT INTO Person_ACTED_IN_Movie(name, role, title) VALUES (?, ?, ?)")) {
				statement.setString(1, "Jeff Bridges");
				statement.setString(2, "Kevin Flynn");
				statement.setString(3, "TRON Ares");
				statement.addBatch();
				statement.setString(1, "Greta Lee");
				statement.setString(2, "Eve Kim");
				statement.setString(3, "TRON Ares");
				statement.executeBatch();
			}

			try (var statement = connection.prepareStatement("""
					INSERT INTO Person_ACTED_IN_Movie(name, role, title)
					VALUES ('Gillian Anderson', 'Elisabeth Dillinger', 'TRON Ares'),
					('Arturo Castro', 'Seth Flores', 'TRON Ares')
					""")) {

				statement.executeUpdate();
			}

			try (var statement = connection
				.prepareStatement("INSERT INTO Person_ACTED_IN_Movie(name, role, title) VALUES(?, ?, ?)")) {
				statement.setString(1, "Jodie Turner-Smith");
				statement.setString(2, "Elisha James");
				statement.setString(3, "The Independent");
				statement.execute();
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH (m:Movie {title: 'TRON Ares'}) <-[a:ACTED_IN]-(p:Person)
					RETURN m, collect(a.role) AS roles
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getObject("roles", Value.class).asList(Value::asString)).containsExactlyInAnyOrder(
						"Eve Kim", "Kevin Flynn", "Athena", "Ares", "Seth Flores", "Elisabeth Dillinger");
				assertThat(rs.next()).isFalse();
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH (m:Movie) <-[a:ACTED_IN]-(p:Person {name: "Jodie Turner-Smith"})
					RETURN p, collect(m.title) AS titles
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getObject("titles", Value.class).asList(Value::asString))
					.containsExactlyInAnyOrder("TRON Ares", "The Independent");
				assertThat(rs.next()).isFalse();
			}
		}
	}

	@Test
	void shouldInsertRelationshipBasedOnAutomaticDetection() throws SQLException {

		try (var con = getConnection(true, true)) {
			// No template relationship needed, assignment of node to properties via
			// qualified names
			try (var stmt = con.prepareStatement("""
					INSERT INTO Person_ACTED_IN_Movie(Person.name, ACTED_IN.role, Movie.title)
					VALUES
					    ('Jaret Leto', 'Ares', 'TRON Ares'),
						('Greta Lee', 'Eve Kim', 'TRON Ares'),
						('Jodie Turner-Smith', 'Athena', 'TRON Ares')
					""")) {

				stmt.executeUpdate();
			}

			// Above has run, database metadata knows the relationship now, no more
			// explicit column assignments
			try (var stmt = con
				.prepareStatement("INSERT INTO Person_ACTED_IN_Movie(name, role, title) VALUES(?, ?, ?)")) {
				stmt.setString(1, "Jodie Turner-Smith");
				stmt.setString(2, "Elisha James");
				stmt.setString(3, "The Independent");
				stmt.execute();
			}

			// Verify start and ends have been merged
			try (var stmt = con.createStatement(); var rs = stmt.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH (m:Movie {title: 'TRON Ares'}) <-[a:ACTED_IN]-(p:Person)
					RETURN m, collect(a.role) AS roles
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getObject("roles", Value.class).asList(Value::asString))
					.containsExactlyInAnyOrder("Eve Kim", "Athena", "Ares");
				assertThat(rs.next()).isFalse();
			}
			try (var stmt = con.createStatement(); var rs = stmt.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH (m:Movie) <-[a:ACTED_IN]-(p:Person {name: "Jodie Turner-Smith"})
					RETURN p, collect(m.title) AS titles
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getObject("titles", Value.class).asList(Value::asString))
					.containsExactlyInAnyOrder("TRON Ares", "The Independent");
				assertThat(rs.next()).isFalse();
			}
		}
	}

	@Test
	void shouldUpdateRelationship() throws SQLException {

		try (var con = getConnection(true, true)) {
			try (var stmt = con.prepareStatement("""
					INSERT INTO Person_ACTED_IN_Movie(Person.name, ACTED_IN.role, Movie.title)
					VALUES
					    ('Jaret Leto', 'Ares', 'Morbius'),
						('Greta Lee', 'Eve Kim', 'TRON Ares'),
						('Jodie Turner-Smith', 'Elisha James', 'TRON Ares')
					""")) {

				stmt.executeUpdate();
			}

			try (var stmt = con.prepareStatement("UPDATE Person_ACTED_IN_Movie SET role = ? WHERE name = ?")) {
				stmt.setString(1, "Athena");
				stmt.setString(2, "Jodie Turner-Smith");
				stmt.executeUpdate();
			}

			try (var stmt = con.createStatement()) {

				stmt.executeUpdate("UPDATE Person_ACTED_IN_Movie SET title = 'TRON Ares' WHERE name = 'Jaret Leto'");

				try (var rs = stmt.executeQuery("SELECT count(*) FROM Movie WHERE title = 'TRON Ares'")) {

					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(2);
				}
			}

			try (var stmt = con.createStatement();
					var rs = stmt.executeQuery("SELECT DISTINCT role FROM Person_ACTED_IN_Movie")) {
				var roles = new HashSet<>();
				while (rs.next()) {
					roles.add(rs.getString("role"));
				}
				assertThat(roles).containsExactlyInAnyOrder("Ares", "Eve Kim", "Athena");
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void relationshipsShallBeDeletable() throws SQLException {
		try (var con = getConnection(true, true)) {
			try (var stmt = con.prepareStatement("""
					INSERT INTO Person_ACTED_IN_Movie(Person.name, ACTED_IN.role, Movie.title)
					VALUES
					    ('Jeff Bridges', 'Kevin Flynn', 'TRON Ares'),
					    ('Jeff Bridges', 'Kevin Flynn', 'TRON: Legacy'),
						('Garrett Hedlund', 'Sam Flynn', 'TRON Ares'),
						('Garrett Hedlund', 'Sam Flynn', 'TRON: Legacy'),
						('Samuel L. Jackson', 'himself', 'TRON: Legacy')
					""")) {

				stmt.executeUpdate();
			}

			try (var stmt = con.createStatement()) {
				try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM Movie WHERE title LIKE 'TRON%'")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(2);
				}
				try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM Person")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(3);
				}
				try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM Person_ACTED_IN_Movie")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(5);
				}
				stmt.executeUpdate("""
						DELETE FROM Person_ACTED_IN_Movie
						WHERE title = 'TRON: Legacy'
						  AND name = 'Samuel L. Jackson'
						  AND role = 'himself'
						""");

				try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM Movie WHERE title LIKE 'TRON%'")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(2);
				}
				try (var rs = stmt.executeQuery("""
						/*+ NEO4J FORCE_CYPHER */
						MATCH (p:Person {name: 'Samuel L. Jackson'})
						RETURN p.name, COUNT {(p)-[:ACTED_IN]->(:Movie)} AS cnt
						""")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getString(1)).isEqualTo("Samuel L. Jackson");
					assertThat(rs.getInt(2)).isEqualTo(0);
				}
				try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM Person_ACTED_IN_Movie")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt(1)).isEqualTo(4);
				}

				stmt.executeUpdate("TRUNCATE Person_ACTED_IN_Movie");

				try (var rs = stmt.executeQuery("""
							/*+ NEO4J FORCE_CYPHER */
							MATCH (n)
							OPTIONAL MATCH (n)-[r]->(m)
							WITH labels(n) + coalesce(labels(m), []) + coalesce(type(r), []) AS row
							UNWIND row AS label
							RETURN COLLECT(DISTINCT label)
						""")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getObject(1, List.class)).containsOnly("Movie", "Person");
				}
			}

		}
	}

	@Test
	void mustNotInferWhenNodeExists() throws SQLException {

		try (var con = getConnection(true, true)) {
			try (var stmt = con.createStatement()) {
				stmt.executeUpdate("/*+ NEO4J FORCE_CYPHER */ CREATE (n:Person_ACTED_IN_Movie {initial: 'true'})");
			}

			try (var stmt = con.prepareStatement("""
					INSERT INTO Person_ACTED_IN_Movie(Person.name, ACTED_IN.role, Movie.title)
					VALUES
					    ('Jaret Leto', 'Ares', 'TRON Ares'),
						('Greta Lee', 'Eve Kim', 'TRON Ares'),
						('Jodie Turner-Smith', 'Athena', 'TRON Ares')
					""")) {

				stmt.executeUpdate();
			}

			try (var stmt = con.createStatement(); var rs = stmt.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH (n:Person_ACTED_IN_Movie) WHERE n.initial IS NULL
					RETURN n""")) {

				int cnt = 0;
				while (rs.next()) {
					var node = rs.getObject("n", Node.class);
					assertThat(node.asMap()).containsOnlyKeys("name", "role", "title");
					++cnt;
				}
				assertThat(cnt).isEqualTo(3);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2 })
	void allPropsOnRelWithoutQualification(int numRows) throws SQLException {

		try (var con = getConnection(true, true)) {

			var base = new StringBuilder("INSERT INTO A_RELATES_TO_B (a, b, c) VALUES ");

			for (int i = 1; i <= numRows; ++i) {
				base.append("(");
				for (var s : new String[] { "a", "b", "c" }) {
					base.append("'").append(s).append(i).append("',");
				}
				base.replace(base.length() - 1, base.length(), "), ");
			}

			try (var stmt = con.prepareStatement(base.substring(0, base.length() - 2))) {
				stmt.executeUpdate();
			}

			try (var stmt = con.createStatement(); var rs = stmt.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					MATCH ()-[r:RELATES_TO]->()
					RETURN r""")) {

				int cnt = 0;
				while (rs.next()) {
					var node = rs.getObject("r", Relationship.class);
					var idx = cnt + 1;
					assertThat(node.asMap())
						.containsExactlyInAnyOrderEntriesOf(Map.of("a", "a" + idx, "b", "b" + idx, "c", "c" + idx));
					++cnt;
				}
				assertThat(cnt).isEqualTo(numRows);
			}
		}
	}

	@Test
	void shouldTranslateExtract() throws SQLException {

		var now = LocalDateTime.now();
		try (var connection = getConnection(true, false)) {
			try (var statement = ((Neo4jPreparedStatement) connection.prepareStatement(
					"SELECT year(:now), month(:now), day(:now), hour(:now), minute(:now), second(:now), millisecond(:now)"))) {
				statement.setTimestamp("now", Timestamp.valueOf(now));
				var rs = statement.executeQuery();
				assertThat(rs.next()).isTrue();
				assertThat(rs.getInt(1)).isEqualTo(now.getYear());
				assertThat(rs.getInt(2)).isEqualTo(now.getMonthValue());
				assertThat(rs.getInt(3)).isEqualTo(now.getDayOfMonth());
				assertThat(rs.getInt(4)).isEqualTo(now.getHour());
				assertThat(rs.getInt(5)).isEqualTo(now.getMinute());
				assertThat(rs.getInt(6)).isEqualTo(now.getSecond());
			}

		}
	}

	@SuppressWarnings("SqlDialectInspection")
	@Test
	void joinsOnVirtualTables() throws Exception {

		var title = "JDBC The SQL strikes back";

		try (var connection = getConnection(true, false)) {
			try (var statement = ((Neo4jPreparedStatement) connection.prepareStatement("""
					/*+ NEO4J FORCE_CYPHER */
					CREATE (m:Movie {title:  $title, released: $released})
					CREATE (d:Person {name: 'Donald D. Chamberlin'})
					CREATE (r:Person {name: 'Raymond F. Boyce'})
					CREATE (e:Person {name: 'Edgar F. Codd'})
					CREATE (r)-[:ACTED_IN {roles: ['Researcher']}]->(m)
					CREATE (d)-[:ACTED_IN {roles: ['Designer']}]->(m)
					CREATE (e)-[:ACTED_IN {roles: ['Influencer']}]->(m)
					"""))) {
				statement.setString("title", title);
				statement.setInt("released", 1974);
				statement.execute();
			}

			// LTR
			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT * FROM Person p
					JOIN Person_ACTED_IN_Movie pm ON pm.v$person_id = p.v$id
					ORDER BY name
					""")) {

				var results = new ArrayList<NameAndRoles>();
				while (rs.next()) {
					results.add(new NameAndRoles(rs.getString("name"),
							rs.getObject("roles", Value.class).asList(Value::asString)));
				}
				assertThat(results).hasSize(3);
				assertThat(results).containsExactly(new NameAndRoles("Donald D. Chamberlin", List.of("Designer")),
						new NameAndRoles("Edgar F. Codd", List.of("Influencer")),
						new NameAndRoles("Raymond F. Boyce", List.of("Researcher")));
			}

			// RTL
			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT * FROM Movie m
					JOIN Person_ACTED_IN_Movie pm ON pm.v$movie_id = m.v$id
					ORDER BY pm.roles[0]
					""")) {

				record MovieAndRole(String title, List<String> roles) {
				}
				var results = new ArrayList<MovieAndRole>();
				while (rs.next()) {
					results.add(new MovieAndRole(rs.getString("title"),
							rs.getObject("roles", Value.class).asList(Value::asString)));
				}
				assertThat(results).hasSize(3);
				assertThat(results).containsExactly(new MovieAndRole(title, List.of("Designer")),
						new MovieAndRole(title, List.of("Influencer")), new MovieAndRole(title, List.of("Researcher")));
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT * FROM Person p
					JOIN Person_ACTED_IN_Movie pm ON pm.v$person_id = p.v$id
					JOIN Movie m ON m.v$id = pm.v$movie_id ORDER BY title, name
					""")) {

				var results = new ArrayList<NameAndRoles>();
				while (rs.next()) {
					results.add(new NameAndRoles(rs.getString("title"),
							rs.getObject("roles", Value.class).asList(Value::asString)));
				}
				assertThat(results).hasSize(3);
				assertThat(results).containsExactly(new NameAndRoles(title, List.of("Designer")),
						new NameAndRoles(title, List.of("Influencer")), new NameAndRoles(title, List.of("Researcher")));
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT *
					FROM Person p, Person_ACTED_IN_Movie pm, Movie m
					WHERE pm.v$person_id = p.v$id AND m.v$id = pm.v$movie_id
					ORDER BY title, name
					""")) {

				var results = new ArrayList<NameAndRoles>();
				while (rs.next()) {
					results.add(new NameAndRoles(rs.getString("title"),
							rs.getObject("roles", Value.class).asList(Value::asString)));
				}
				assertThat(results).hasSize(3);
				assertThat(results).containsExactly(new NameAndRoles(title, List.of("Designer")),
						new NameAndRoles(title, List.of("Influencer")), new NameAndRoles(title, List.of("Researcher")));
			}
		}
	}

	@SuppressWarnings({ "SqlDialectInspection" })
	@Test
	void joins() throws IOException, SQLException {

		try (var connection = getConnection(true, false)) {
			TestUtils.createMovieGraph(connection);

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT name AS name, title AS title FROM Person p JOIN Movie m on (m = p.ACTED_IN)
					ORDER BY name, released, title
					""")) {

				var personAndTitle = new ArrayList<PersonAndTitle>();
				while (rs.next()) {
					personAndTitle.add(new PersonAndTitle(rs.getString("name"), rs.getString("title")));
				}
				assertThat(personAndTitle).hasSize(172);
				assertThat(personAndTitle).containsSequence(new PersonAndTitle("Carrie-Anne Moss", "The Matrix"),
						new PersonAndTitle("Carrie-Anne Moss", "The Matrix Reloaded"),
						new PersonAndTitle("Carrie-Anne Moss", "The Matrix Revolutions"));
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT COUNT(*) FROM Person p
					NATURAL JOIN Movie m
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getInt(1)).isEqualTo(250);
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT p.name AS name, r.roles AS roles FROM Person p
					NATURAL JOIN ACTED_IN r
					NATURAL JOIN Movie m
					WHERE title = 'The Matrix'
					ORDER BY name
					""")) {

				var results = new ArrayList<NameAndRoles>();
				while (rs.next()) {
					results.add(new NameAndRoles(rs.getString("name"),
							rs.getObject("roles", Value.class).asList(Value::asString)));
				}
				assertThat(results).containsExactly(new NameAndRoles("Carrie-Anne Moss", List.of("Trinity")),
						new NameAndRoles("Emil Eifrem", List.of("Emil")),
						new NameAndRoles("Hugo Weaving", List.of("Agent Smith")),
						new NameAndRoles("Keanu Reeves", List.of("Neo")),
						new NameAndRoles("Laurence Fishburne", List.of("Morpheus")));
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT p.name AS name, m.title AS title
					FROM Person p
					JOIN Movie m ON m.id = p.directed
					WHERE p.name like '%Wachowski%'
					""")) {

				var results = new ArrayList<PersonAndTitle>();
				while (rs.next()) {
					results.add(new PersonAndTitle(rs.getString("name"), rs.getString("title")));
				}
				assertThat(results).hasSize(10);
				assertThat(results).map(PersonAndTitle::title)
					.containsOnly("The Matrix", "The Matrix", "The Matrix Reloaded", "The Matrix Revolutions",
							"Cloud Atlas", "Speed Racer");
			}
		}

	}

	@Test
	void shouldUnwrapCauseOfTranslationException() throws SQLException {

		try (var connection = getConnection(true, false);
				var stmt = connection.prepareStatement("CREATE (m:Movie {title:  $title, released: $released})")) {
			assertThatExceptionOfType(SQLException.class).isThrownBy(stmt::execute)
				.withCauseInstanceOf(ParserException.class)
				.withMessageStartingWith(
						"general processing exception - FUNCTION, GENERATOR, GLOBAL TEMPORARY TABLE, INDEX, OR ALTER, OR REPLACE, PROCEDURE, SCHEMA, SEQUENCE, TABLE, TEMPORARY TABLE, TRIGGER, TYPE, UNIQUE INDEX, or VIEW expected");
		}

	}

	@SuppressWarnings("SqlDialectInspection")
	@Test
	void shouldTranslateVIDColumns() throws Exception {

		try (var connection = getConnection(true, false)) {
			try (var statement = connection.prepareStatement(
					"/*+ NEO4J FORCE_CYPHER */ CREATE (m:Movie {title:  'Terminator 2'})<-[:ACTED_IN {role: 'The Terminator'}]-(:Person {name: 'Arnold Schwarzenegger'})")) {
				statement.execute();
			}

			try (var statement = connection.createStatement();
					var result = statement.executeQuery(
							"SELECT v$id, v$person_id, role, src.v$movie_id FROM Person_ACTED_IN_Movie src")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getString(1)).isNotNull();
				assertThat(result.getString(2)).isNotNull();
				assertThat(result.getString(3)).isEqualTo("The Terminator");
				assertThat(result.getString(4)).isNotNull();
			}

			try (var statement = connection.createStatement(); var result = statement.executeQuery("""
					SELECT p.v$id, v$person_id, role, pm.v$movie_id, v$id
					FROM Person p
					JOIN Person_ACTED_IN_Movie pm ON pm.v$person_id = p.v$id
					JOIN Movie m ON m.v$id = pm.v$movie_id
					ORDER BY title, name, v$id
					""")) {

				assertThat(result.next()).isTrue();
				assertThat(result.getString(1)).isNotNull();
				assertThat(result.getString(2)).isNotNull();
				assertThat(result.getString(3)).isEqualTo("The Terminator");
				assertThat(result.getString(4)).isNotNull();
				assertThat(result.getString(5)).isEqualTo(result.getString(1));
			}

		}
	}

	@Test
	void shouldMatchCorrectVid() throws SQLException, IOException {
		try (var connection = getConnection(true, false)) {
			TestUtils.createMovieGraph(connection);

			var cypher = connection.nativeSQL("""
					  SELECT TOP 100 `Person_ACTED_IN_Movie`.`roles` AS `roles`,
					     `Person_ACTED_IN_Movie`.`v$id` AS `v_id`,
					     `Person_ACTED_IN_Movie`.`v$movie_id` AS `v_movie_id`,
					     `Person_ACTED_IN_Movie`.`v$person_id` AS `v_person_id`
					   FROM `public`.`Person_ACTED_IN_Movie` `Person_ACTED_IN_Movie`
					""");
			assertThat(cypher).isEqualTo(
					"MATCH (_lhs:Person)-[person_acted_in_movie:ACTED_IN]->(_rhs:Movie) RETURN person_acted_in_movie.roles AS roles, elementId(person_acted_in_movie) AS v_id, elementId(_rhs) AS v_movie_id, elementId(_lhs) AS v_person_id LIMIT 100");
		}

	}

	@SuppressWarnings("SqlNoDataSourceInspection")
	@Test
	void innerJoinWrongColumn() throws SQLException, IOException {
		// Those queries will return empty results, but will not fail and are essentially
		// correct as formulated in SQL
		try (var connection = getConnection(true, false)) {
			TestUtils.createMovieGraph(connection);
			var cypher = connection.nativeSQL("""
					SELECT `Person`.`name` AS `name`, `Person_DIRECTED_Movie`.`v$movie_id` AS `v_movie_id`
					FROM `public`.`Person` `Person`
					INNER JOIN `public`.`Person_DIRECTED_Movie` `Person_DIRECTED_Movie`
					ON (`Person`.`v$id` = `Person_DIRECTED_Movie`.`v$id`) GROUP BY `name`, `v_movie_id`""");
			assertThat(cypher).isEqualTo(
					"MATCH (person:Person)-[person_directed_movie:DIRECTED WHERE elementId(person) = elementId(person_directed_movie)]->(_rhs:Movie) RETURN person.name AS name, elementId(_rhs) AS v_movie_id");

			cypher = connection.nativeSQL("""
					SELECT `Movie`.`title` AS `title`, `Person_DIRECTED_Movie`.`v$movie_id` AS `v_movie_id`
					FROM `public`.`Movie` `Movie`
					INNER JOIN `public`.`Person_DIRECTED_Movie` `Person_DIRECTED_Movie`
					ON (`Movie`.`v$id` = `Person_DIRECTED_Movie`.`v$id`) GROUP BY `name`, `v_movie_id`""");
			assertThat(cypher).isEqualTo(
					"MATCH (_lhs:Person)-[person_directed_movie:DIRECTED WHERE elementId(movie) = elementId(person_directed_movie)]->(movie) RETURN movie.title AS title, elementId(movie) AS v_movie_id");
		}

	}

	@Test // GH-814
	void multipleHops() throws SQLException {
		try (var connection = getConnection(true, false)) {
			var createGraph = """
					/*+ NEO4J FORCE_CYPHER */
					CREATE (p:Person {name: 'A'})
					CREATE (pr:Project {serial: '1'})
					CREATE (c:Company {name: 'B'})
					CREATE (p)-[:PARTICIPATES_IN]->(pr)
					CREATE (p)-[:WORKS_FOR]->(c);
					""";

			var sql = """
					SELECT a.*,c.*,e.*
					FROM Person a
					JOIN Person_PARTICIPATES_IN_Project b
						ON b."v$person_id" = a."v$id"
					JOIN Project c
						ON c."v$id" = b."v$project_id"
					JOIN Person_WORKS_FOR_Company d
						ON d."v$person_id"= a."v$id"
					JOIN Company e
						ON e."v$id" = d."v$company_id"
					""";

			try (var statement = connection.createStatement()) {
				statement.executeUpdate(createGraph);
			}

			var cypher = connection.nativeSQL(sql);
			assertThat(cypher).isEqualTo(
					"MATCH (a:Person)-[b:PARTICIPATES_IN]->(c:Project), (a)-[d:WORKS_FOR]->(e:Company) RETURN elementId(a) AS `v$id`, a.name AS name, elementId(c) AS `v$id1`, c.serial AS serial, elementId(e) AS `v$id2`, e.name AS name1");
			try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
				assertThat(result.next()).isTrue();
				assertThat(result.getString("name")).isEqualTo("A");
				assertThat(result.getString("name1")).isEqualTo("B");
				assertThat(result.next()).isFalse();
			}

		}

	}

	@SuppressWarnings({ "SqlNoDataSourceInspection", "unchecked" })
	@Test
	void sparkUnwrapRewrap() throws Exception {

		var title = "JDBC The SQL strikes back";

		try (var connection = getConnection(true, false)) {
			try (var statement = ((Neo4jPreparedStatement) connection.prepareStatement("""
					/*+ NEO4J FORCE_CYPHER */
					CREATE (m:Movie {title:  $title, released: $released})
					CREATE (d:Person {name: 'Donald D. Chamberlin'})
					CREATE (r:Person {name: 'Raymond F. Boyce'})
					CREATE (e:Person {name: 'Edgar F. Codd'})
					CREATE (r)-[:ACTED_IN {roles: ['Researcher']}]->(m)
					CREATE (d)-[:ACTED_IN {roles: ['Designer']}]->(m)
					CREATE (e)-[:ACTED_IN {roles: ['Influencer']}]->(m)
					"""))) {
				statement.setString("title", title);
				statement.setInt("released", 1974);
				statement.execute();
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT * FROM (
					MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
					RETURN m.title AS title, collect(p.name) AS actors
					ORDER BY m.title
					) SPARK_GEN_SUBQ_0 WHERE 1=0
					""")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getString("title")).isEqualTo(title);
				assertThat((List<String>) rs.getObject("actors")).containsExactlyInAnyOrder("Donald D. Chamberlin",
						"Raymond F. Boyce", "Edgar F. Codd");
				assertThat(rs.next()).isFalse();
			}

			try (var statement = connection.createStatement(); var rs = statement.executeQuery("""
					SELECT * FROM (
					SELECT name FROM Person SPARK_GEN_SUBQ_0 ORDER BY name DESC
					) SPARK_GEN_SUBQ_0 WHERE 1=0
					""")) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getString(1)).isEqualTo("Raymond F. Boyce");
				assertThat(rs.next()).isFalse();
			}

		}
	}

	record PersonAndTitle(String name, String title) {
	}

	record NameAndRoles(String name, List<String> roles) {
	}

}
