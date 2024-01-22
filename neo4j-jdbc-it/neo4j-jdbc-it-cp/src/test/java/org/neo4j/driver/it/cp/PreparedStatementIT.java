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
package org.neo4j.driver.it.cp;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.jdbc.Neo4jPreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;

class PreparedStatementIT extends IntegrationTestBase {

	@Test
	void simpleBatchShouldWork() throws SQLException {
		try (var connection = getConnection(false, false);
				var statement = connection.prepareStatement("CREATE (n:BatchTestSimple {idx: $1})")) {
			for (int i = 0; i < 3; ++i) {
				statement.setInt(1, i);
				statement.addBatch();
			}
			var result = statement.executeBatch();
			assertThat(result).hasSize(4);
			assertThat(result).containsExactly(3, 3, 3, Statement.SUCCESS_NO_INFO);
		}
	}

	@Test
	void rewrittenBatchShouldWork() throws SQLException {
		try (var connection = getConnection(false, true);
				var statement = connection.prepareStatement("CREATE (n:BatchTestSimple {idx: $1})")) {
			for (int i = 0; i < 3; ++i) {
				statement.setInt(1, i);
				statement.addBatch();
			}
			var result = statement.executeBatch();
			assertThat(result).hasSize(1);
			assertThat(result).containsExactly(9);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldExecuteUpdateWithoutAutoCommit(boolean commit) throws SQLException {
		var limit = 5;
		var testId = UUID.randomUUID().toString();
		try (var connection = getConnection()) {
			connection.setAutoCommit(false);
			for (int i = 0; i < 2; ++i) {
				try (var statement = connection
					.prepareStatement("UNWIND range(1, $1) AS x CREATE (n:Test {testId: $2})")) {
					statement.setInt(1, limit);
					statement.setString(2, testId);
					statement.executeUpdate();
				}
			}

			if (commit) {
				connection.commit();
			}
			else {
				connection.rollback();
			}

			try (var statement = connection.prepareStatement("MATCH (n:Test {testId: $1}) RETURN count(n)")) {
				statement.setString(1, testId);
				var resultSet = statement.executeQuery();
				resultSet.next();
				assertThat(resultSet.getInt(1)).isEqualTo((commit) ? limit * 2 : 0);
			}
		}
	}

	@Test
	void shouldExecuteQuery() throws SQLException {
		var limit = 10_000;
		try (var connection = getConnection();
				var statement = connection.prepareStatement("UNWIND range(1, $1) AS x RETURN x")) {
			statement.setFetchSize(5);
			statement.setInt(1, limit);
			var resultSet = statement.executeQuery();
			for (var i = 1; i <= 17; i++) {
				assertThat(resultSet.next()).isTrue();
				assertThat(resultSet.getInt(1)).isEqualTo(i);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldExecuteQueryWithoutAutoCommit(boolean commit) throws SQLException {
		var limit = 5;
		var testId = UUID.randomUUID().toString();
		try (var connection = getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.prepareStatement("UNWIND range(1, $1) AS x CREATE (n:Test {testId: $2})")) {
				statement.setInt(1, limit);
				statement.setString(2, testId);
				var resultSet = statement.executeQuery();
			}

			if (commit) {
				connection.commit();
			}
			else {
				connection.rollback();
			}

			try (var statement = connection.prepareStatement("MATCH (n:Test {testId: $1}) RETURN count(n)")) {
				statement.setString(1, testId);
				var resultSet = statement.executeQuery();
				resultSet.next();
				assertThat(resultSet.getInt(1)).isEqualTo((commit) ? limit : 0);
			}
		}
	}

	@Test
	void shouldBeAbleToUseNamedParameters() throws SQLException {

		try (var connection = getConnection()) {
			try (var statement = connection.prepareStatement("CREATE (n:Test {testId: $id}) RETURN n.testId")) {
				var neo4jStatement = statement.unwrap(Neo4jPreparedStatement.class);

				var id = UUID.randomUUID().toString();
				neo4jStatement.setString("id", id);
				var resultSet = statement.executeQuery();
				assertThat(resultSet.next()).isTrue();
				assertThat(resultSet.getString(1)).isEqualTo(id);
			}
		}
	}

	@Test
	void executeShouldAutomaticallyTranslate() throws SQLException {

		try (var connection = getConnection(true, false)) {

			try (var ps = connection.prepareStatement("INSERT INTO Movie(name) VALUES (?)")) {
				ps.setString(1, "Praxis Dr. Hasenbein");
				var cnt = ps.executeUpdate();
				assertThat(cnt).isEqualTo(3); // one node, one label, one property
			}

			try (var ps = connection.prepareStatement("SELECT elementId(m) AS id FROM Movie m WHERE m.name = ?")) {
				ps.setString(1, "Praxis Dr. Hasenbein");
				try (var result = ps.executeQuery()) {
					assertThat(result.next()).isTrue();
					assertThat(result.getString("id")).matches("\\d+:.+:\\d+");
				}
			}
		}
	}

}
