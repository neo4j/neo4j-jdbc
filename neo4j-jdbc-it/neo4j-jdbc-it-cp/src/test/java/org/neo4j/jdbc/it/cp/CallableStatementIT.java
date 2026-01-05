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
package org.neo4j.jdbc.it.cp;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

	@ParameterizedTest
	@ValueSource(strings = { "RETURN atan2(?, ?)", "RETURN atan2($x, $y)", "{? = call atan2(?, ?)}" })
	void parameterNamesForFunctionsShouldWork(String query) throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall(query)) {
			var meta = statement.getParameterMetaData();
			assertThat(meta.getParameterCount()).isEqualTo(2);
			assertThat(meta.getParameterType(1)).isEqualTo(Types.DOUBLE);
			assertThat(meta.getParameterType(2)).isEqualTo(Types.DOUBLE);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "CALL dbms.routing.getRoutingTable(?, ?)",
			"CALL dbms.routing.getRoutingTable($context, $database)" })
	void parameterNamesForProceduresShouldWork(String query) throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall(query)) {
			var meta = statement.getParameterMetaData();
			assertThat(meta.getParameterCount()).isEqualTo(2);
			assertThat(meta.getParameterType(1)).isEqualTo(Types.STRUCT);
			assertThat(meta.getParameterType(2)).isEqualTo(Types.VARCHAR);
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
			var rs = statement.getResultSet();
			assertThat(rs.next()).isTrue();
			var msg = rs.getString(2);
			assertThat(msg).isEqualTo("neo4j");
			assertThat(msg).isEqualTo(rs.getString(2));
		}
	}

	@Test
	void outByNameShouldWork() throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall("{$name = call db.info()}")) {

			statement.execute();
			var rs = statement.getResultSet();
			assertThat(rs.next()).isTrue();
			var msg = rs.getString("name");
			assertThat(msg).isEqualTo("neo4j");
			assertThat(msg).isEqualTo(rs.getString("name"));
		}
	}

	@Test
	void outByNameAsParameterShouldWork() throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall("{$name = call db.info()}")) {

			statement.registerOutParameter("name", Types.VARCHAR);
			statement.execute();
			var rs = statement.getResultSet();
			assertThat(rs.next()).isTrue();
			var msg = rs.getString("name");
			assertThat(msg).isEqualTo("neo4j");
			assertThat(msg).isEqualTo(rs.getString("name"));
		}
	}

	@Test
	void batchingShouldNotBeSupported() throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall("{$name = call db.info()}")) {
			assertThatExceptionOfType(SQLException.class).isThrownBy(statement::clearBatch)
				.withMessage("general processing exception - This method must not be called on CallableStatement");
			assertThatExceptionOfType(SQLException.class).isThrownBy(statement::addBatch)
				.withMessage("general processing exception - This method must not be called on CallableStatement");
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> statement.addBatch("RETURN sin(?)"))
				.withMessage("general processing exception - This method must not be called on CallableStatement");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "{? = call sin(?)}", "RETURN sin(?)" })
	void outByIndexOnFunctionShouldWork(String sql) throws SQLException {
		try (var connection = getConnection(); var statement = connection.prepareCall(sql)) {

			statement.setDouble(1, Math.toRadians(150));
			statement.execute();
			var rs = statement.getResultSet();
			assertThat(rs.next()).isTrue();
			var sin = rs.getDouble(1);
			assertThat(sin).isCloseTo(0.5, Percentage.withPercentage(1));
			assertThat(sin).isEqualTo(rs.getDouble(1));
		}
	}

	Stream<Arguments> parameterTypeRetrievalShouldWork() {
		return Stream.of(Arguments.of("RETURN valueType(?)",
				List.of(new ParametertypeHelper<>(true, (stmt, v) -> stmt.setBoolean(1, v), stmt -> stmt.getBoolean(1)),
						new ParametertypeHelper<>((byte) 2, (stmt, v) -> stmt.setByte(1, v), stmt -> stmt.getByte(1))),
				new ParametertypeHelper<>((short) 2, (stmt, v) -> stmt.setShort(1, v), stmt -> stmt.getShort(1)),
				new ParametertypeHelper<>(2, (stmt, v) -> stmt.setInt(1, v), stmt -> stmt.getInt(1)),
				new ParametertypeHelper<>(2L, (stmt, v) -> stmt.setLong(1, v), stmt -> stmt.getLong(1)),
				new ParametertypeHelper<>((float) 2.0, (stmt, v) -> stmt.setFloat(1, v), stmt -> stmt.getFloat(1)),
				new ParametertypeHelper<>(2.0, (stmt, v) -> stmt.setDouble(1, v), stmt -> stmt.getDouble(1)),
				new ParametertypeHelper<>(Date.valueOf(LocalDate.now()), (stmt, v) -> stmt.setDate(1, v),
						stmt -> stmt.getDate(1)),
				new ParametertypeHelper<>(Time.valueOf(LocalTime.now()), (stmt, v) -> stmt.setTime(1, v),
						stmt -> stmt.getTime(1)),
				new ParametertypeHelper<>(Timestamp.valueOf(LocalDateTime.now()), (stmt, v) -> stmt.setTimestamp(1, v),
						stmt -> stmt.getTimestamp(1)),
				new ParametertypeHelper<>(BigDecimal.TEN, (stmt, v) -> stmt.setBigDecimal(1, v),
						stmt -> stmt.getBigDecimal(1)),
				new ParametertypeHelper<>(null, (stmt, v) -> stmt.setInt(1, v), stmt -> stmt.getInt(1))),
				Arguments.of("RETURN valueType($input)",
						List.of(new ParametertypeHelper<>(true, (stmt, v) -> stmt.setBoolean("input", v),
								stmt -> stmt.getBoolean("input")),
								new ParametertypeHelper<>((byte) 2, (stmt, v) -> stmt.setByte("input", v),
										stmt -> stmt.getByte("input"))),
						new ParametertypeHelper<>((short) 2, (stmt, v) -> stmt.setShort("input", v),
								stmt -> stmt.getShort("input")),
						new ParametertypeHelper<>(2, (stmt, v) -> stmt.setInt("input", v),
								stmt -> stmt.getInt("input")),
						new ParametertypeHelper<>(2L, (stmt, v) -> stmt.setLong("input", v),
								stmt -> stmt.getLong("input")),
						new ParametertypeHelper<>((float) 2.0, (stmt, v) -> stmt.setFloat("input", v),
								stmt -> stmt.getFloat("input")),
						new ParametertypeHelper<>(2.0, (stmt, v) -> stmt.setDouble("input", v),
								stmt -> stmt.getDouble("input")),
						new ParametertypeHelper<>(Date.valueOf(LocalDate.now()), (stmt, v) -> stmt.setDate("input", v),
								stmt -> stmt.getDate("input")),
						new ParametertypeHelper<>(Time.valueOf(LocalTime.now()), (stmt, v) -> stmt.setTime("input", v),
								stmt -> stmt.getTime("input")),
						new ParametertypeHelper<>(Timestamp.valueOf(LocalDateTime.now()),
								(stmt, v) -> stmt.setTimestamp("input", v), stmt -> stmt.getTimestamp("input")),
						new ParametertypeHelper<>(BigDecimal.TEN, (stmt, v) -> stmt.setBigDecimal("input", v),
								stmt -> stmt.getBigDecimal("input")),

						new ParametertypeHelper<>(null, (stmt, v) -> stmt.setInt("input", v),
								stmt -> stmt.getInt("input"))));
	}

	@ParameterizedTest
	@MethodSource
	void parameterTypeRetrievalShouldWork(String query, List<ParametertypeHelper<?>> types) throws SQLException {
		// types passed as list on purpose, so that we catch clearing parameters, too
		try (var connection = getConnection(); var statement = connection.prepareCall(query)) {

			for (ParametertypeHelper<?> parametertype : types) {
				assertThat(parametertype.apply(statement)).isEqualTo(parametertype.value);
				assertThat(statement.wasNull()).isEqualTo(parametertype.value == null);
				assertStatementResultSet(statement.getResultSet());
				statement.clearParameters();
			}
		}
	}

	private static void assertStatementResultSet(ResultSet rs) throws SQLException {
		assertThat(rs.next()).isTrue();
		assertThat(rs.getString(1)).isNotNull();
		assertThat(rs.next()).isFalse();
	}

	interface ThrowingSetter<T> {

		void set(CallableStatement statement, T v) throws SQLException;

	}

	interface ThrowingGetter<T> {

		T get(CallableStatement statement) throws SQLException;

	}

	static class ParametertypeHelper<T> {

		private final T value;

		private final ThrowingSetter<T> setter;

		private final ThrowingGetter<T> getter;

		ParametertypeHelper(T value, ThrowingSetter<T> setter, ThrowingGetter<T> getter) {
			this.value = value;
			this.setter = setter;
			this.getter = getter;

		}

		T apply(CallableStatement stmt) throws SQLException {
			this.setter.set(stmt, this.value);
			stmt.execute();
			return this.getter.get(stmt);
		}

	}

}
