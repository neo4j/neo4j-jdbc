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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CallableStatementIT extends IntegrationTestBase {

	@BeforeEach
	void prepareSomeData() throws SQLException {
		try (var stmt = this.getConnection().createStatement()) {
			stmt.execute("""
					CREATE (n:A {a: 'X', b: 123, c: date()})
					""");
		}
	}

	@Test
	void shouldExecuteQueryWithOrdinalParameters() throws SQLException {
		try (var connection = getConnection();
				var statement = connection.prepareCall("CALL dbms.routing.getRoutingTable(?, ?)")) {
			statement.setObject(1, Collections.emptyMap());
			statement.setString(2, "neo4j");

			var resultSet = statement.executeQuery();

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isGreaterThan(0);
			assertThat((List<?>) resultSet.getObject(2)).hasSize(3);
			assertThat(resultSet.next()).isFalse();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "CALL dbms.routing.getRoutingTable($database, $context)",
			"CALL dbms.routing.getRoutingTable($context, $database)" })
	void shouldExecuteQueryWithNamedParameters(String sql) throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall(sql)) {
			statement.setObject("context", Collections.emptyMap());
			statement.setString("database", "neo4j");

			var resultSet = statement.executeQuery();

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isGreaterThan(0);
			assertThat((List<?>) resultSet.getObject(2)).hasSize(3);
			assertThat(resultSet.next()).isFalse();
		}
	}

	@Test
	void shouldCheckExistenceOfNamedParameter() throws SQLException {
		try (var connection = getConnection()) {
			assertThatExceptionOfType(SQLException.class)
				.isThrownBy(() -> connection.prepareCall("CALL dbms.routing.getRoutingTable($database, $foo)"))
				.withMessage(
						"syntax error or access rule violation - Procedure `dbms.routing.getRoutingTable` does not have a named parameter `foo`");
		}
	}

	@Test
	void outByIndexShouldWork() throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall("{? = call db.info()}")) {

			statement.execute();
			var msg = statement.getString(2);
			assertThat(msg).isEqualTo("neo4j");
			assertThat(msg).isEqualTo(statement.getString(2));
		}
	}

	@Test
	void outByNameShouldWork() throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall("{$name = call db.info()}")) {

			statement.execute();
			var msg = statement.getString("name");
			assertThat(msg).isEqualTo("neo4j");
			assertThat(msg).isEqualTo(statement.getString("name"));
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "{? = call sin(?)}", "RETURN sin(?)" })
	void outByIndexOnFunctionShouldWork(String sql) throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall(sql)) {

			statement.setDouble(1, Math.toRadians(150));
			statement.execute();
			var sin = statement.getDouble(1);
			assertThat(sin).isCloseTo(0.5, Percentage.withPercentage(1));
			assertThat(sin).isEqualTo(statement.getDouble(1));
		}
	}

}
