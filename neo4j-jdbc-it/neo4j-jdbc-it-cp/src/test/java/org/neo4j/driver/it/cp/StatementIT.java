/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.it.cp;

import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatementIT extends IntegrationTestBase {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldExecuteQuery(boolean getByLabel) throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			statement.setFetchSize(5);
			var resultSet = statement.executeQuery("UNWIND range(1, 10000) AS x RETURN x");
			for (var i = 1; i <= 17; i++) {
				assertThat(resultSet.next()).isTrue();
				var value = getByLabel ? resultSet.getInt("x") : resultSet.getInt(1);
				assertThat(value).isEqualTo(i);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldExecuteQueryWithoutAutoCommit(boolean commit) throws SQLException {
		var testId = UUID.randomUUID().toString();
		try (var connection = getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.createStatement()) {
				var resultSet = statement
					.executeQuery(String.format("UNWIND range(1, 5) AS x CREATE (n:Test {testId: '%s'})", testId));
			}

			if (commit) {
				connection.commit();
			}
			else {
				connection.rollback();
			}

			try (var statement = connection.createStatement()) {
				var resultSet = statement
					.executeQuery(String.format("MATCH (n:Test {testId: '%s'}) RETURN count(n)", testId));
				resultSet.next();
				assertThat(resultSet.getInt(1)).isEqualTo((commit) ? 5 : 0);
			}
		}
	}

	@Test
	void shouldExecuteUpdate() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			var num = statement.executeUpdate("UNWIND range(1, 5) AS x CREATE ()");
			assertThat(num).isEqualTo(5);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void closingOnCompletionShouldWork(boolean enableBeforeExecute) throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			if (enableBeforeExecute) {
				stmt.closeOnCompletion();
			}
			try (var rs = stmt.executeQuery("MATCH (n) RETURN count(n)")) {
				assertThat(rs).isNotNull();
				if (!enableBeforeExecute) {
					stmt.closeOnCompletion();
				}
			}
			assertThat(stmt.isClosed()).isTrue();
		}
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			foo,`foo`,true
			foo,foo,false
			das ist ein test,`das ist ein test`,false
			""")
	void shouldQuoteIdentifier(String identifier, String expected, boolean alwaysQuote) throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			assertThat(stmt.enquoteIdentifier(identifier, alwaysQuote)).isEqualTo(expected);
		}
	}

	@ParameterizedTest
	@MethodSource("getExecuteWithRowLimitArgs")
	void shouldExecuteWithRowLimit(int fetchSize, int maxRows, int expectedNumber) throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			statement.setFetchSize(fetchSize);
			statement.setMaxRows(maxRows);
			var resultSet = statement.executeQuery("UNWIND range(1, 1000) AS x RETURN x");
			var number = 0;
			while (resultSet.next()) {
				number++;
			}
			assertThat(number).isEqualTo(expectedNumber);
		}
	}

	@Test
	void executeShouldAutomaticallyTranslate() throws SQLException {

		try (var connection = getConnection(true); var statement = connection.createStatement()) {
			var isUpdate = statement.execute("INSERT INTO Movie(name) VALUES ('Praxis Dr. Hasenbein')");
			assertThat(isUpdate).isFalse();

			String id;
			try (var result = statement
				.executeQuery("SELECT elementId(m) FROM Movie m WHERE m.name = 'Praxis Dr. Hasenbein'")) {
				assertThat(result.next()).isTrue();
				id = result.getString(1);
				assertThat(id).isNotNull();
			}

			var updated = statement.executeUpdate(
					"UPDATE Movie SET name = '00 Schneider – Jagd auf Nihil Baxter' WHERE elementId(movie) = '%s'"
						.formatted(id));
			assertThat(updated).isOne();

			var cnt = statement.executeQuery("/*+ NEO4J FORCE_CYPHER */ MATCH (n:Movie) RETURN count(n)");

			assertThat(cnt.next()).isTrue();
			assertThat(cnt.getInt(1)).isGreaterThan(0);
		}
	}

	@Test
	void orderOrGroupByUnrelated() throws SQLException {

		try (var connection = getConnection(true)) {

			record Movie(String title, int year) {
			}

			try (var ps = connection.prepareStatement("INSERT INTO Movie(title, year) VALUES (?, ?)")) {
				for (var movie : new Movie[] { new Movie("Praxis Dr. Hasenbein", 1997),
						new Movie("Jazzclub - Der frühe Vogel fängt den Wurm", 2004) }) {
					ps.setString(1, movie.title);
					ps.setInt(2, movie.year);
					ps.executeUpdate();
				}
			}

			// supportsOrderByUnrelated
			try (var ps = connection.prepareStatement("SELECT m.title FROM Movie m ORDER BY m.year DESC")) {
				try (var result = ps.executeQuery()) {
					assertThat(result.next()).isTrue();
					assertThat(result.getString(1)).startsWith("Jazzclub");
					assertThat(result.next()).isTrue();
					assertThat(result.getString(1)).startsWith("Praxis");
				}
			}

			// supportsGroupByUnrelated
			try (var ps = connection.prepareStatement("SELECT m.title FROM Movie m GROUP BY m.year")) {
				try (var result = ps.executeQuery()) {
					int cnt = 0;
					while (result.next()) {
						++cnt;
					}
					assertThat(cnt).isEqualTo(2);
				}
			}

			// supportsGroupByBeyondSelect
			try (var ps = connection.prepareStatement("SELECT m.title FROM Movie m GROUP BY m.title, m.year")) {
				try (var result = ps.executeQuery()) {
					int cnt = 0;
					while (result.next()) {
						++cnt;
					}
					assertThat(cnt).isEqualTo(2);
				}
			}
		}
	}

	private Stream<Arguments> getExecuteWithRowLimitArgs() {
		return Stream.of(Arguments.of(5, 15, 15), Arguments.of(5, 13, 13), Arguments.of(100, 100, 100),
				Arguments.of(100, 0, 1000), Arguments.of(1000, 0, 1000), Arguments.of(1000, 1000, 1000),
				Arguments.of(1000, 5000, 1000), Arguments.of(0, 0, 1000));
	}

}
