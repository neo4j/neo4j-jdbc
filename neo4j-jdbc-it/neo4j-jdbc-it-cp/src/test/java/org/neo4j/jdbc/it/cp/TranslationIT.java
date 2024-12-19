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
package org.neo4j.jdbc.it.cp;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jooq.impl.ParserException;
import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.Neo4jPreparedStatement;
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
			.withMessage("No translators available");

	}

	@Test
	void shouldTranslateAsterisk() throws SQLException {

		var title = "JDBC the Sequel";

		try (var connection = getConnection(true, false)) {
			try (var statement = ((Neo4jPreparedStatement) connection.prepareStatement(
					"/*+ NEO4J FORCE_CYPHER */ CREATE (m:Movie {title:  $title, released: $released})"))) {
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
						"org.jooq.impl.ParserException: FUNCTION, GENERATOR, GLOBAL TEMPORARY TABLE, INDEX, OR ALTER, OR REPLACE, PROCEDURE, SCHEMA, SEQUENCE, TABLE, TEMPORARY TABLE, TRIGGER, TYPE, UNIQUE INDEX, or VIEW expected");
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

	record PersonAndTitle(String name, String title) {
	}

	record NameAndRoles(String name, List<String> roles) {
	}

}
