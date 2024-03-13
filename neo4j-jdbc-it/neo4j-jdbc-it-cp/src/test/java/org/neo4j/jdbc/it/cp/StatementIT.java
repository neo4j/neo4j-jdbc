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

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.values.Path;
import org.neo4j.jdbc.values.Value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementIT extends IntegrationTestBase {

	@Test
	void shouldAllowSubsequentQueriesAfterTransactionErrorInAutoCommit() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			assertThatThrownBy(() -> statement.executeQuery("UNWIND [1, 1, 1, 1, 0] AS x RETURN 1/x"))
				.isExactlyInstanceOf(SQLException.class);
			var resultSet = statement.executeQuery("RETURN 1");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isEqualTo(1);
		}
	}

	@Test
	void shouldExpectRollbackInExplicitTransaction() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			assertThatThrownBy(() -> statement.executeQuery("UNWIND [1, 1, 1, 1, 0] AS x RETURN 1/x"))
				.isExactlyInstanceOf(SQLException.class);
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1")).isExactlyInstanceOf(SQLException.class);
			connection.rollback();
			var resultSet = statement.executeQuery("RETURN 1");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isEqualTo(1);
		}
	}

	@Test
	void shouldFailOnMultipleOpenAutoCommit() throws SQLException {
		try (var connection = getConnection();
				var statement1 = connection.createStatement();
				var statement2 = connection.createStatement()) {
			statement1.setFetchSize(5);
			statement2.setFetchSize(5);
			var resultSet1 = statement1.executeQuery("UNWIND range(1, 10000) AS x RETURN x");
			assertThatThrownBy(() -> statement2.executeQuery("UNWIND range(1, 10000) AS x RETURN x"))
				.isExactlyInstanceOf(SQLException.class);
		}
	}

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

		try (var connection = getConnection(true, false); var statement = connection.createStatement()) {
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

	// GH-398
	@SuppressWarnings("deprecation")
	@Test
	void elementIdAndIdShouldBeSupported() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery("CREATE (n:Test) RETURN n")) {
			assertThat(result.next()).isTrue();
			var node = result.getObject(1, Value.class).asNode();
			assertThat(node.elementId()).isNotNull();
			assertThat(node.id()).isNotNull();
		}
	}

	@Test
	void orderOrGroupByUnrelated() throws SQLException {

		try (var connection = getConnection(true, false)) {

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

	@Test
	void resultSetIt0Record() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("WITH 1 AS i WHERE false RETURN i")) {

			assertThat(rs.isBeforeFirst()).isTrue();

			assertThat(rs.next()).isFalse();
			assertThat(rs.isBeforeFirst()).isFalse();
			assertThat(rs.isFirst()).isFalse();
			assertThat(rs.isLast()).isFalse();
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	@Test
	void resultSetIt1Record() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN 1 as i")) {

			assertThat(rs.isBeforeFirst()).isTrue();

			assertThat(rs.next()).isTrue();
			assertThat(rs.isBeforeFirst()).isFalse();
			assertThat(rs.isFirst()).isTrue();
			assertThat(rs.getInt("i")).isOne();
			assertThat(rs.isAfterLast()).isFalse();

			assertThat(rs.isLast()).isTrue();
			assertThat(rs.getInt("i")).isOne();
			assertThat(rs.isAfterLast()).isFalse();

			assertThat(rs.next()).isFalse();
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getInt("i"))
				.withMessage("Invalid cursor position");
			assertThat(rs.isFirst()).isFalse();
			assertThat(rs.isLast()).isFalse();
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	@Test
	void resultSetItNRecords() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("UNWIND range(1,10) AS i RETURN i")) {

			assertThat(rs.isBeforeFirst()).isTrue();

			int cnt = 1;
			while (rs.next()) {
				assertThat(rs.getInt("i")).isEqualTo(cnt);
				if (cnt == 1) {
					assertThat(rs.isFirst()).isTrue();
				}
				else if (cnt == 10) {
					assertThat(rs.isLast()).isTrue();
				}
				else {
					assertThat(rs.isFirst()).isFalse();
					assertThat(rs.isLast()).isFalse();
				}
				++cnt;
			}

			assertThat(rs.next()).isFalse();
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> rs.getInt("i"))
				.withMessage("Invalid cursor position");
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	private Stream<Arguments> getExecuteWithRowLimitArgs() {
		return Stream.of(Arguments.of(5, 15, 15), Arguments.of(5, 13, 13), Arguments.of(100, 100, 100),
				Arguments.of(100, 0, 1000), Arguments.of(1000, 0, 1000), Arguments.of(1000, 1000, 1000),
				Arguments.of(1000, 5000, 1000), Arguments.of(0, 0, 1000));
	}

	// GH-397
	@Test
	void noPropertyNamesShouldBeSpecial() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery(
						"CREATE (n:Test {name: 'A', _id: 'id1', _labels: ['L1', 'L2']}) -[r:RELATES_TO {name: 'R', _id: 'id2', _startId: 4711, _endId: 23, _type: 'Trololo'}] -> (m:AnotherTest {name: 'B', _id: 'id3'}) RETURN *")) {
			assertThat(result.next()).isTrue();
			var nodeA = result.getObject("n", Value.class).asNode();
			var rel = result.getObject("r", Value.class).asRelationship();
			var nodeB = result.getObject("m", Value.class).asNode();
			assertThat(nodeA.asMap())
				.containsExactlyInAnyOrderEntriesOf(Map.of("name", "A", "_id", "id1", "_labels", List.of("L1", "L2")));
			assertThat(rel.asMap()).containsExactlyInAnyOrderEntriesOf(
					Map.of("name", "R", "_id", "id2", "_startId", 4711L, "_endId", 23L, "_type", "Trololo"));
			assertThat(nodeB.asMap()).containsExactlyInAnyOrderEntriesOf(Map.of("name", "B", "_id", "id3"));
		}
	}

	// GH-81
	@Test
	void nullPropertiesShallNotFailWhenBeAccessed() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement();) {
			connection.setAutoCommit(false);
			stmt.executeUpdate("CREATE (n:mynode {order_prop:1, aninteger: 42})");
			stmt.executeUpdate("CREATE (n:mynode {order_prop:2})");
			connection.commit();
		}
		try (var connection = getConnection(); var stmt = connection.createStatement();) {
			try (var result = stmt.executeQuery("MATCH (n:mynode) RETURN n ORDER BY n.order_prop ASC")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getObject("n", Value.class).get("aninteger").asInt()).isEqualTo(42);
				assertThat(result.next()).isTrue();
				assertThat(result.getObject("n", Value.class).get("aninteger").isNull()).isTrue();
				assertThat(result.next()).isFalse();
			}

			try (var result = stmt.executeQuery("MATCH (n:mynode) RETURN n.aninteger ORDER BY n.order_prop ASC")) {
				assertThat(result.next()).isTrue();
				assertThat(result.getInt(1)).isEqualTo(42);
				assertThat(result.next()).isTrue();
				assertThat(result.getInt(1)).isZero();
				assertThat(result.wasNull()).isTrue();
				assertThat(result.next()).isFalse();
			}
		}
	}

	// GH-254
	@Test
	void getObjectMustReturnTheSmallestTypePossibleForInteger() throws SQLException {
		// Avro is very specific about this, otherwise you'll end up with something like
		// Caused by: org.apache.avro.UnresolvedUnionException: Not in union
		// ["null","long"]
		// It determines the final type based on the precision, and would downvote a long
		// to an int if the precision is unset (being equal to 0).
		// From the datatype however it will expect a long, and now ends up with an
		// integer.
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN 1 AS i, 2147483648 AS l")) {
			assertThat(rs.next()).isTrue();
			var meta = rs.getMetaData();
			assertThat(meta.getColumnType(1)).isEqualTo(Types.INTEGER);
			assertThat(meta.getPrecision(1)).isEqualTo(String.valueOf(Long.MAX_VALUE).length());
			assertThat(meta.getColumnType(2)).isEqualTo(Types.INTEGER);
			assertThat(meta.getPrecision(2)).isEqualTo(String.valueOf(Long.MAX_VALUE).length());
			var o = rs.getObject(1);
			assertThat(o).isInstanceOf(Long.class).isEqualTo(1L);
			o = rs.getObject(2);
			assertThat(o).isInstanceOf(Long.class).isEqualTo(2147483648L);
		}
	}

	// GH-401
	@Test
	void noSurpriseGetString() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery("RETURN {a: '1', b: '2'} AS m, [1,2,3] AS l")) {

			assertThat(result.next()).isTrue();
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> result.getString("m"))
				.withMessage("MAP value can not be mapped to String");
			assertThat(result.getObject("m")).isInstanceOf(Map.class);
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> result.getString("l"))
				.withMessage("LIST OF ANY? value can not be mapped to String");
			assertThat(result.getObject("l")).isInstanceOf(List.class);
		}
	}

	@Test
	void pathsShouldBeAccessible() throws SQLException {
		try (var connection = getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery(
						"CREATE p = (:Show {name: 'The Last Of Us'}) <-[:WATCHED_BY]- (:Person {name: 'Michael'}) RETURN p")) {

			assertThat(result.next()).isTrue();
			var path = result.getObject("p", Path.class);
			assertThat(path.start().get("name").asString()).isEqualTo("The Last Of Us");
			assertThat(path.end().get("name").asString()).isEqualTo("Michael");
		}
	}

}
