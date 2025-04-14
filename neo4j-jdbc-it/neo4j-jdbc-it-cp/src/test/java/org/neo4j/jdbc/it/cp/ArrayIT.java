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

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.values.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
class ArrayIT extends IntegrationTestBase {

	@ValueSource(booleans = { true, false })
	@ParameterizedTest
	void unsupportedArray(boolean byLabel) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN 1 AS thing")) {
			rs.next();
			ThrowableAssert.ThrowingCallable callable;
			if (byLabel) {
				callable = () -> rs.getArray("thing");
			}
			else {
				callable = () -> rs.getArray(1);
			}
			assertThatExceptionOfType(SQLException.class).isThrownBy(callable)
				.matches(ex -> ex.getErrorCode() == 0 && "22N01".equals(ex.getSQLState()))
				.withMessage("Expected the value 1 to be of type LIST, but was of type INTEGER");
		}
	}

	@ValueSource(booleans = { true, false })
	@ParameterizedTest
	void unsupportedArrayContentType(boolean byLabel) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1, 2, 'a chameleon', 'a bar', false] AS thing")) {
			rs.next();
			ThrowableAssert.ThrowingCallable callable;
			if (byLabel) {
				callable = () -> rs.getArray("thing");
			}
			else {
				callable = () -> rs.getArray(1);
			}
			assertThatExceptionOfType(SQLException.class).isThrownBy(callable)
				.matches(ex -> ex.getErrorCode() == 0 && "22G03".equals(ex.getSQLState()))
				.withMessage("invalid value type");
		}
	}

	@Test
	void shouldUseAnyTypeForEmptyArray() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [] AS thing")) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getBaseTypeName()).isEqualTo("ANY");
			assertThat(array.getBaseType()).isEqualTo(Types.OTHER);
		}
	}

	@Test
	void shouldNotFailOnNull() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN NULL AS thing")) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array).isNull();
			assertThat(rs.wasNull()).isTrue();
		}
	}

	static Stream<Arguments> nullInArrayShouldWork() {
		return Stream.of(Arguments.of("RETURN [null, null] AS thing", Types.NULL),
				Arguments.of("RETURN [null, 'a'] AS thing", Types.VARCHAR),
				Arguments.of("RETURN [1, null] AS thing", Types.BIGINT));
	}

	@ParameterizedTest
	@MethodSource
	void nullInArrayShouldWork(String query, int expectedType) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery(query)) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getBaseType()).isEqualTo(expectedType);
		}
	}

	static Stream<Arguments> shouldUsePrimitiveArraysWhenPossible() {
		return Stream.of(Optional.empty(), Optional.of(Map.of()))
			.flatMap(tm -> Stream.of(Arguments.of("RETURN [1,2,3] AS thing", Types.BIGINT, new long[] { 1, 2, 3 }, tm),
					Arguments.of("RETURN [1.0,2.0,3.0] AS thing", Types.DOUBLE, new double[] { 1.0, 2.0, 3.0 }, tm),
					Arguments.of("RETURN [true, false] AS thing", Types.BOOLEAN, new boolean[] { true, false }, tm)));
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	@ParameterizedTest
	@MethodSource
	void shouldUsePrimitiveArraysWhenPossible(String query, int expectedType, Object expectedArray,
			Optional<Map<String, Class<?>>> tm) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery(query)) {
			rs.next();
			var array = rs.getArray(1);
			var content = tm.isEmpty() ? array.getArray() : array.getArray(tm.get());
			assertThat(content).isEqualTo(expectedArray);
			assertThat(array.getBaseType()).isEqualTo(expectedType);
		}
	}

	static Stream<Arguments> shouldFallbackToBoxed() {
		return Stream.of(Arguments.of("RETURN [1, 2, null, 3] AS thing", Types.BIGINT, new Long[] { 1L, 2L, null, 3L }),
				Arguments.of("RETURN [1.0, null, 2.0,3.0] AS thing", Types.DOUBLE,
						new Double[] { 1.0, null, 2.0, 3.0 }),
				Arguments.of("RETURN [true, null, false] AS thing", Types.BOOLEAN,
						new Boolean[] { true, null, false }));
	}

	@ParameterizedTest
	@MethodSource
	void shouldFallbackToBoxed(String query, int expectedType, Object expectedArray) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery(query)) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getArray()).isEqualTo(expectedArray);
			assertThat(array.getBaseType()).isEqualTo(expectedType);
		}
	}

	@Test
	void byteArraysShouldWork() throws SQLException {
		byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);
		try (var connection = super.getConnection(); var stmt = connection.prepareStatement("RETURN ? AS thing")) {
			stmt.setBytes(1, bytes);
			var rs = stmt.executeQuery();
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getArray()).isEqualTo(bytes);
			assertThat(array.getBaseType()).isEqualTo(Types.BLOB);
		}
	}

	@Test
	void randomOtherThingsShouldWork() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [date('2025-4-6')]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getArray()).isEqualTo(new LocalDate[] { LocalDate.of(2025, 4, 6) });
			assertThat(array.getBaseType()).isEqualTo(Types.DATE);
		}
	}

	static Stream<Arguments> getSliceShouldWork() {
		return Stream.of(Optional.empty(), Optional.of(Map.of()))
			.flatMap(tm -> Stream.of(Arguments.of(1, 1, new long[] { 1 }, tm), Arguments.of(1, 0, new long[0], tm),
					Arguments.of(2, 3, new long[] { 2, 3, 4 }, tm), Arguments.of(5, 1, new long[] { 5 }, tm),
					Arguments.of(5, 0, new long[0], tm), Arguments.of(1, 5, new long[] { 1, 2, 3, 4, 5 }, tm)));
	}

	@ParameterizedTest
	@MethodSource
	void getSliceShouldWork(long index, int count, long[] expected, Optional<Map<String, Class<?>>> tm)
			throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getBaseType()).isEqualTo(Types.BIGINT);
			var slice = tm.isEmpty() ? array.getArray(index, count) : array.getArray(index, count, tm.get());
			assertThat(slice).isEqualTo(expected);
		}
	}

	static Stream<Arguments> sliceWithInvalidBoundsShouldFail() {
		return Stream.of(Arguments.of(1, -1, "Invalid argument: cannot process getArray(1, -1) for array with size 5"),
				Arguments.of(-1, 1, "Invalid argument: cannot process getArray(-1, 1) for array with size 5"),
				Arguments.of(0, 1, "Invalid argument: cannot process getArray(0, 1) for array with size 5"),
				Arguments.of(6, 1, "Invalid argument: cannot process getArray(6, 1) for array with size 5"),
				Arguments.of(1, 6, "Invalid argument: cannot process getArray(1, 6) for array with size 5"),
				Arguments.of(5, 2, "Invalid argument: cannot process getArray(5, 2) for array with size 5"));
	}

	@ParameterizedTest
	@MethodSource
	void sliceWithInvalidBoundsShouldFail(long index, int count, String msg) throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> array.getArray(index, count))
				.matches(ex -> ex.getErrorCode() == 0 && "22N11".equals(ex.getSQLState()))
				.withMessage(msg);
		}
	}

	@Test
	void getArrayWithUnsupportedTypemap() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThatExceptionOfType(SQLException.class)
				.isThrownBy(() -> array.getArray(Map.of("BIGINT", String.class)))
				.matches(ex -> ex.getErrorCode() == 0 && "22N11".equals(ex.getSQLState()));
		}
	}

	@Test
	void getArraySliceWithUnsupportedTypemap() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThatExceptionOfType(SQLException.class)
				.isThrownBy(() -> array.getArray(1, 1, Map.of("BIGINT", String.class)))
				.matches(ex -> ex.getErrorCode() == 0 && "22N11".equals(ex.getSQLState()));
		}
	}

	@Test
	void getResultSetShouldWork() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN ['a', null, 'b', 'c', 'd']")) {
			rs.next();
			var array = rs.getArray(1);
			try (var inner = array.getResultSet()) {
				var meta = inner.getMetaData();
				assertThat(meta.getColumnCount()).isEqualTo(2);
				assertThat(meta.getColumnName(1)).isEqualTo("index");
				assertThat(meta.getColumnName(2)).isEqualTo("value");

				int cnt = 1;
				var expected = "0a_bcd";
				while (inner.next()) {
					var index = inner.getInt(1);
					assertThat(index).isEqualTo(cnt);
					var value = inner.getString(2);
					if (cnt == 2) {
						assertThat(value).isNull();
						assertThat(inner.wasNull()).isTrue();
					}
					else {
						assertThat(value).isEqualTo("" + expected.charAt(cnt));
					}
					++cnt;
				}
			}
		}
	}

	@Test
	void getResultForBytesShouldWork() throws SQLException {
		byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);
		try (var connection = super.getConnection(); var stmt = connection.prepareStatement("RETURN ? AS thing")) {
			stmt.setBytes(1, bytes);
			var rs = stmt.executeQuery();
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getBaseType()).isEqualTo(Types.BLOB);

			try (var inner = array.getResultSet()) {
				int cnt = 0;
				while (inner.next()) {
					var index = inner.getInt("index");
					assertThat(index).isEqualTo(cnt + 1);
					var value = inner.getByte("value");
					assertThat(value).isEqualTo(bytes[cnt]);
					++cnt;
				}
				assertThat(cnt).isEqualTo(bytes.length);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("getSliceShouldWork")
	void getResultSetSliceShouldWork(long index, int count, long[] expected, Optional<Map<String, Class<?>>> tm)
			throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			assertThat(array.getBaseType()).isEqualTo(Types.BIGINT);
			var inner = tm.isEmpty() ? array.getResultSet(index, count) : array.getResultSet(index, count, tm.get());
			var actualCount = 0;
			while (inner.next()) {
				++actualCount;
			}
			inner.close();
			assertThat(actualCount).isEqualTo(expected.length);
		}
	}

	@Test
	void freedShouldWork() throws SQLException {
		try (var connection = super.getConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("RETURN [1,2,3,4,5]")) {
			rs.next();
			var array = rs.getArray(1);
			array.free();
			assertThatExceptionOfType(SQLException.class).isThrownBy(array::getArray)
				.withMessage("Array has been already freed");
		}
	}

	@Test
	void writingByteArraysShouldWork() throws SQLException {
		byte[] bytes = "Hello".getBytes(StandardCharsets.UTF_8);

		var elements = new Object[bytes.length];
		for (var i = 0; i < bytes.length; i++) {
			elements[i] = bytes[i];
		}

		try (var connection = super.getConnection(); var stmt = connection.prepareStatement("RETURN ? AS thing")) {
			stmt.setArray(1, connection.createArrayOf(Type.BYTES.name(), elements));
			var rs = stmt.executeQuery();
			rs.next();
			assertThat(new String(rs.getBytes(1))).isEqualTo("Hello");
		}
	}

	@Test
	void writingArraysShouldWork() throws SQLException {

		try (var connection = super.getConnection(); var stmt = connection.prepareStatement("RETURN ? AS thing")) {
			stmt.setArray(1, connection.createArrayOf(Type.STRING.name(), new Object[] { "A", "B", "C" }));
			var rs = stmt.executeQuery();
			rs.next();
			assertThat(rs.getObject(1)).isEqualTo(List.of("A", "B", "C"));
		}
	}

}
