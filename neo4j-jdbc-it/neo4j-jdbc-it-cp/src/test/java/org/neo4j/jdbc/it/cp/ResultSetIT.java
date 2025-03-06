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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ResultSetIT extends IntegrationTestBase {

	ResultSet getResultSet() throws SQLException {
		return super.getConnection().createStatement().executeQuery("UNWIND RANGE (1,4) AS n RETURN n");
	}

	@Test
	void beforeFirstBeforeUse() throws SQLException {
		try (var rs = getResultSet()) {
			rs.beforeFirst();
			var ints = new ArrayList<Integer>();
			while (rs.next()) {
				ints.add(rs.getInt("n"));
			}
			assertThat(ints).containsExactly(1, 2, 3, 4);
		}
	}

	@Test
	void beforeFirstAfterUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.isBeforeFirst()).isTrue();
			assertThat(rs.next()).isTrue();
			assertThat(rs.isFirst()).isTrue();
			assertThat(rs.isBeforeFirst()).isFalse();
			assertThatExceptionOfType(SQLException.class).isThrownBy(rs::beforeFirst)
				.withMessage(
						"This result set is of type TYPE_FORWARD_ONLY (1003) and does not support beforeFirst after it has been iterated");
		}
	}

	@Test
	void afterLastBeforeUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.isAfterLast()).isFalse();
			assertThatNoException().isThrownBy(rs::afterLast);
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	@Test
	void afterLastAfterPartialUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.isAfterLast()).isFalse();
			assertThat(rs.next()).isTrue();
			assertThat(rs.isAfterLast()).isFalse();
			assertThatNoException().isThrownBy(rs::afterLast);
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	@Test
	void afterLastAfterFullUse() throws SQLException {
		try (var rs = getResultSet()) {
			do {
				assertThat(rs.isAfterLast()).isFalse();
			}
			while (rs.next());
			assertThat(rs.isAfterLast()).isTrue();
			assertThatNoException().isThrownBy(rs::afterLast);
			assertThat(rs.isAfterLast()).isTrue();
		}
	}

	@Test
	void firstBeforeUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.first()).isTrue();
			var n = rs.getInt("n");
			assertThat(n).isEqualTo(1);
		}
	}

	@Test
	void firstAfterUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.next()).isTrue();
			assertThatExceptionOfType(SQLException.class).isThrownBy(rs::first)
				.withMessage(
						"This result set is of type TYPE_FORWARD_ONLY (1003) and does not support first after it has been iterated");
		}
	}

	@Test
	void lastBeforeUse() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.last()).isTrue();
			var n = rs.getInt("n");
			assertThat(n).isEqualTo(4);
			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void lastAfterABitOfUsage() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.next()).isTrue();
			var n = rs.getInt("n");
			assertThat(n).isEqualTo(1);
			assertThat(rs.last()).isTrue();
			n = rs.getInt("n");
			assertThat(n).isEqualTo(4);
			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void lastAfterFullUse() throws SQLException {
		try (var rs = getResultSet()) {
			do {
				assertThat(rs.isAfterLast()).isFalse();
			}
			while (rs.next());
			assertThatExceptionOfType(SQLException.class).isThrownBy(rs::last)
				.withMessage(
						"This result set is of type TYPE_FORWARD_ONLY (1003) and does not support last after it has been fully iterated");
		}
	}

	@Test
	void getRow() throws SQLException {
		try (var rs = getResultSet()) {
			assertThat(rs.getRow()).isZero();
			while (rs.next()) {
				var n = rs.getInt("n");
				assertThat(rs.getRow()).isEqualTo(n);
			}
		}
	}

	static Stream<Arguments> unsupportedMovement() {
		return Stream.of(Arguments.of((Function<ResultSet, String>) resultSet -> {
			try {
				resultSet.absolute(1);
			}
			catch (SQLException ex) {
				return ex.getMessage();
			}
			return null;
		}, "This result set is of type TYPE_FORWARD_ONLY (1003) and does not support absolute scrolling"),
				Arguments.of((Function<ResultSet, String>) resultSet -> {
					try {
						resultSet.relative(1);
					}
					catch (SQLException ex) {
						return ex.getMessage();
					}
					return null;
				}, "This result set is of type TYPE_FORWARD_ONLY (1003) and does not support relative scrolling"),
				Arguments.of((Function<ResultSet, String>) resultSet -> {
					try {
						resultSet.previous();
					}
					catch (SQLException ex) {
						return ex.getMessage();
					}
					return null;
				}, "This result set is of type TYPE_FORWARD_ONLY (1003) and does not support previous scrolling"),
				Arguments.of((Function<ResultSet, String>) resultSet -> {
					try {
						resultSet.moveToCurrentRow();
					}
					catch (SQLException ex) {
						return ex.getMessage();
					}
					return null;
				}, "This result sets concurrency is of type CONCUR_READ_ONLY (1007) and does not support moving to current row"),
				Arguments.of((Function<ResultSet, String>) resultSet -> {
					try {
						resultSet.moveToInsertRow();
					}
					catch (SQLException ex) {
						return ex.getMessage();
					}
					return null;
				}, "This result sets concurrency is of type CONCUR_READ_ONLY (1007) and does not support moving to insert row"));
	}

	@ParameterizedTest
	@MethodSource
	void unsupportedMovement(Function<ResultSet, String> tester, String expected) throws SQLException {
		try (var rs = getResultSet()) {
			var msg = tester.apply(rs);
			assertThat(msg).isEqualTo(expected);
		}
	}

}
