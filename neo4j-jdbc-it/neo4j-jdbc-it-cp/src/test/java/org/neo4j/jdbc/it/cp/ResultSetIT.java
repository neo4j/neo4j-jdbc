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

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

	static Stream<Arguments> shouldGetAsObjectWithType() {
		return Stream.of(
				Arguments.of(java.util.Date
					.from(LocalDate.parse("2025-12-15").atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant())),
				Arguments.of(java.sql.Date
					.from(LocalDate.parse("2025-12-15").atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant())),
				Arguments.of(1.23f), Arguments.of((short) 23), Arguments.of(42), Arguments.of(666L),
				Arguments.of(BigInteger.TEN), Arguments.of(Duration.ofDays(23)), Arguments.of(ZonedDateTime.now()),
				Arguments.of(OffsetDateTime.now()), Arguments.of(OffsetTime.now()),
				Arguments.of(ZonedDateTime.of(2026, 1, 1, 21, 21, 0, 0, ZoneId.of("Antarctica/Troll"))));
	}

	@MethodSource
	@ParameterizedTest
	void shouldGetAsObjectWithType(Object in) throws SQLException {
		try (var stmt = super.getConnection().prepareStatement("RETURN $1 AS v")) {
			stmt.setObject(1, in);
			var rs = stmt.executeQuery();
			assertThat(rs.next()).isTrue();
			assertThat(rs.getObject(1, in.getClass())).isEqualTo(in);
			assertThat(rs.next()).isFalse();
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

	@SuppressWarnings("unchecked")
	@Test
	void getListAsStringShouldNotFail() throws SQLException {
		try (var con = getConnection();
				var stmt = con.createStatement();
				var rs = stmt.executeQuery("RETURN [1, 2, 3, 4, 5] AS l")) {
			assertThat(rs.next()).isTrue();
			var l = rs.getObject("l");
			assertThat(l).isInstanceOf(List.class);
			assertThat(((List<Object>) l)).containsExactly(1L, 2L, 3L, 4L, 5L);
			var a = rs.getArray("l");
			try (var rsa = a.getResultSet()) {
				int cnt = 0;
				while (rsa.next()) {
					++cnt;
					assertThat(rsa.getLong(1)).isEqualTo(cnt);
				}
				assertThat(cnt).isEqualTo(5);
			}

			assertThat(rs.getString("l")).isEqualTo("[1, 2, 3, 4, 5]");
		}
	}

	// GH-401 and Gerrit
	@SuppressWarnings("unchecked")
	@Test
	void toStringShouldNotFailOnHeterogenous() throws SQLException {
		try (var con = getConnection(); var stmt = con.createStatement(); var rs = stmt.executeQuery("""
				RETURN [1, 2, 3, 4, 'haha'] AS l,
					{a: 1, b: 2, c: "haha"} AS m,
					point({longitude: 56.7, latitude: 12.78}) AS p1,
					point({x: 2.3, y: 4.5, crs: 'WGS-84'}) AS p2,
					point({longitude: 56.7, latitude: 12.78, height: 8}) AS p3,
					point({x: 2.3, y: 4.5, z: 2}) AS p4,
				    point({x: 2.3, y: 4.5}) AS p5""")) {
			assertThat(rs.next()).isTrue();
			var l = rs.getObject("l");
			assertThat(l).isInstanceOf(List.class);
			assertThat(((List<Object>) l)).containsExactly(1L, 2L, 3L, 4L, "haha");
			assertThat(rs.getString("l")).isEqualTo("[1, 2, 3, 4, \"haha\"]");

			var m = rs.getObject("m");
			assertThat(m).isInstanceOf(Map.class);
			assertThat(((Map<String, Object>) m)).containsAllEntriesOf(Map.of("a", 1L, "b", 2L, "c", "haha"));
			assertThat(rs.getString("m")).isEqualTo("{a: 1, b: 2, c: \"haha\"}");

			assertThat(rs.getString("p1")).isEqualTo("point({srid:4326, x:56.7, y:12.78})");
			assertThat(rs.getString("p2")).isEqualTo("point({srid:4326, x:2.3, y:4.5})");
			assertThat(rs.getString("p3")).isEqualTo("point({srid:4979, x:56.7, y:12.78, z:8.0})");
			assertThat(rs.getString("p4")).isEqualTo("point({srid:9157, x:2.3, y:4.5, z:2.0})");
			assertThat(rs.getString("p5")).isEqualTo("point({srid:7203, x:2.3, y:4.5})");
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
