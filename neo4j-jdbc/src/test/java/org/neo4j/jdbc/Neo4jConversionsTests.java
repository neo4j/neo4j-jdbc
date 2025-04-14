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
package org.neo4j.jdbc;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class Neo4jConversionsTests {

	static Stream<Arguments> shouldParseNames() {
		return Stream.of(Arguments.of("Any", Type.ANY), Arguments.of("Boolean", Type.BOOLEAN),
				Arguments.of("Bytes", Type.BYTES), Arguments.of("String", Type.STRING),
				Arguments.of("Integer", Type.INTEGER), Arguments.of("Long", Type.INTEGER),
				Arguments.of("Float", Type.FLOAT), Arguments.of("Double", Type.FLOAT),
				Arguments.of("StringArray", Type.LIST), Arguments.of("DoubleArray", Type.LIST),
				Arguments.of("LongArray", Type.LIST), Arguments.of("Map", Type.MAP), Arguments.of("Point", Type.POINT),
				Arguments.of("Path", Type.PATH), Arguments.of("Relationship", Type.RELATIONSHIP),
				Arguments.of("Node", Type.NODE), Arguments.of("Date", Type.DATE),
				Arguments.of("DATE_TIME", Type.DATE_TIME), Arguments.of("Time", Type.TIME),
				Arguments.of("LocalDateTime", Type.LOCAL_DATE_TIME), Arguments.of("LocalTime", Type.LOCAL_TIME),
				Arguments.of("Duration", Type.DURATION), Arguments.of("Null", Type.NULL));
	}

	@ParameterizedTest
	@MethodSource
	void shouldParseNames(String in, Type expected) {
		var type = Neo4jConversions.valueOfV5Name(in);
		assertThat(type).isEqualTo(expected);
	}

	static Stream<Arguments> asTime() {
		return Stream.of(Arguments.of(Values.value(OffsetTime.of(23, 21, 42, 0, ZoneOffset.ofHours(5))), null),
				Arguments.of(Values.value(LocalTime.of(23, 21, 42)), null),
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))), null),
				Arguments.of(Values.NULL, null), Arguments.of(null, null),
				Arguments.of(Values.value(LocalDate.now()), SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asTime(Value value, Class<? extends SQLException> expexctedExceptionType) throws SQLException {

		if (expexctedExceptionType != null) {
			assertThatExceptionOfType(expexctedExceptionType).isThrownBy(() -> Neo4jConversions.asTime(value))
				.withMessage("DATE value cannot be mapped to java.sql.Time");
		}
		else {
			var time = Neo4jConversions.asTime(value);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(time).isNull();
			}
			else {
				assertThat(time).isEqualTo(Time.valueOf("23:21:42"));
			}
		}
	}

	static Stream<Arguments> asTimeWithCalendar() {
		return Stream.of(
				Arguments.of(Values.value(OffsetTime.of(23, 21, 42, 0, ZoneOffset.ofHours(5))), "19:21:42", null),
				Arguments.of(Values.value(LocalTime.of(23, 21, 42)), "23:21:42", null),
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 9, 21), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), "04:21:42", null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))),
						"23:21:42", null),
				Arguments.of(Values.NULL, null, null), Arguments.of(null, null, null),
				Arguments.of(Values.value(LocalDate.now()), null, SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asTimeWithCalendar(Value value, String expected, Class<? extends SQLException> expectedExceptionType)
			throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
		cal.set(Calendar.MONTH, 2);
		cal.set(Calendar.DATE, 1);

		if (expectedExceptionType != null) {
			assertThatExceptionOfType(expectedExceptionType).isThrownBy(() -> Neo4jConversions.asTime(value, cal))
				.withMessage("DATE value cannot be mapped to java.sql.Time");
		}
		else {
			var time = Neo4jConversions.asTime(value, cal);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(time).isNull();
			}
			else {
				assertThat(time).isEqualTo(Time.valueOf(expected));
			}
		}
	}

	@Test
	void asTimeMustBeSymmetric() throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
		var value = Neo4jConversions.asValue(Time.valueOf("23:21:42"), cal);
		var time = Neo4jConversions.asTime(value, cal);
		assertThat(time).isEqualTo(Time.valueOf("23:21:42"));
		assertThat(Neo4jConversions.asTime(Neo4jConversions.asValue((Time) null, null))).isNull();
	}

	static Stream<Arguments> asDate() {
		return Stream.of(Arguments.of(Values.value(LocalDate.of(2024, 3, 9)), null),
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))), null),
				Arguments.of(Values.NULL, null), Arguments.of(null, null),
				Arguments.of(Values.value(LocalTime.now()), SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asDate(Value value, Class<? extends SQLException> expexctedExceptionType) throws SQLException {

		if (expexctedExceptionType != null) {
			assertThatExceptionOfType(expexctedExceptionType).isThrownBy(() -> Neo4jConversions.asDate(value))
				.withMessage("LOCAL_TIME value cannot be mapped to java.sql.Date");
		}
		else {
			var date = Neo4jConversions.asDate(value);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(date).isNull();
			}
			else {
				assertThat(date).isEqualTo(Date.valueOf("2024-03-09"));
			}
		}
	}

	static Stream<Arguments> asDateWithCalendar() {
		return Stream.of(Arguments.of(Values.value(LocalDate.of(2024, 3, 9)), "2024-03-09", null),
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), "2024-03-10", null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))),
						"2024-03-09", null),
				Arguments.of(Values.NULL, null, null), Arguments.of(null, null, null),
				Arguments.of(Values.value(LocalTime.now()), null, SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asDateWithCalendar(Value value, String expected, Class<? extends SQLException> expectedExceptionType)
			throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));

		if (expectedExceptionType != null) {
			assertThatExceptionOfType(expectedExceptionType).isThrownBy(() -> Neo4jConversions.asDate(value, cal))
				.withMessage("LOCAL_TIME value cannot be mapped to java.sql.Date");
		}
		else {
			var date = Neo4jConversions.asDate(value, cal);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(date).isNull();
			}
			else {
				assertThat(date).isEqualTo(Date.valueOf(expected));
			}
		}
	}

	@Test
	void asDateMustBeSymmetric() throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
		var value = Neo4jConversions.asValue(Date.valueOf("2024-03-10"), cal);
		var time = Neo4jConversions.asDate(value, cal);
		assertThat(time).isEqualTo(Date.valueOf("2024-03-10"));
		assertThat(Neo4jConversions.asDate(Neo4jConversions.asValue((Date) null, null))).isNull();
	}

	static Stream<Arguments> asTimestamp() {
		return Stream.of(
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))), null),
				Arguments.of(Values.NULL, null), Arguments.of(null, null),
				Arguments.of(Values.value(LocalDate.now()), SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asTimestamp(Value value, Class<? extends SQLException> expexctedExceptionType) throws SQLException {

		if (expexctedExceptionType != null) {
			assertThatExceptionOfType(expexctedExceptionType).isThrownBy(() -> Neo4jConversions.asTimestamp(value))
				.withMessage("DATE value cannot be mapped to java.sql.Timestamp");
		}
		else {
			var time = Neo4jConversions.asTimestamp(value);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(time).isNull();
			}
			else {
				assertThat(time).isEqualTo(Timestamp.valueOf("2024-03-09 23:21:42"));
			}
		}
	}

	static Stream<Arguments> asTimestampWithCalendar() {
		return Stream.of(
				Arguments.of(Values.value(ZonedDateTime.of(LocalDate.of(2024, 8, 21), LocalTime.of(23, 21, 42),
						ZoneId.of("America/New_York"))), "2024-08-22 05:21:42", null),
				Arguments.of(Values.value(LocalDateTime.of(LocalDate.of(2024, 3, 9), LocalTime.of(23, 21, 42))),
						"2024-03-09 23:21:42", null),
				Arguments.of(Values.NULL, null, null), Arguments.of(null, null, null),
				Arguments.of(Values.value(LocalDate.now()), null, SQLException.class));
	}

	@ParameterizedTest
	@MethodSource
	void asTimestampWithCalendar(Value value, String expected, Class<? extends SQLException> expectedExceptionType)
			throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));

		if (expectedExceptionType != null) {
			assertThatExceptionOfType(expectedExceptionType).isThrownBy(() -> Neo4jConversions.asTimestamp(value, cal))
				.withMessage("DATE value cannot be mapped to java.sql.Timestamp");
		}
		else {
			var time = Neo4jConversions.asTimestamp(value, cal);
			if (value == null || Values.NULL.equals(value)) {
				assertThat(time).isNull();
			}
			else {
				assertThat(time).isEqualTo(Timestamp.valueOf(expected));
			}
		}
	}

	@Test
	void asTimestampMustBeSymmetric() throws SQLException {
		var cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
		var value = Neo4jConversions.asValue(Timestamp.valueOf("2024-03-10 23:42:03"), cal);
		var time = Neo4jConversions.asTimestamp(value, cal);
		assertThat(time).isEqualTo(Timestamp.valueOf("2024-03-10 23:42:03"));
		assertThat(Neo4jConversions.asTimestamp(Neo4jConversions.asValue((Timestamp) null, null))).isNull();
	}

	@ParameterizedTest
	@EnumSource
	void shouldCoverAllPredefinedTypes(Type type) {
		// noinspection ResultOfMethodCallIgnored
		assertThatNoException().isThrownBy(() -> Neo4jConversions.toSqlType(type));
	}

	@Test
	void shouldNotFailOnSuddenNewNeo4jTypesThatDontMapToModernOnes() {
		assertThat(Neo4jConversions.oldCypherTypesToNew("whatever")).isEqualTo("OTHER");
		assertThat(Neo4jConversions.oldCypherTypesToNew("Null")).isEqualTo("NULL");
	}

	static Stream<Arguments> assertTypeMapShouldWork() {
		return Stream.of(null, Map.of()).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource
	void assertTypeMapShouldWork(Map<String, Class<?>> map) {
		assertThatNoException().isThrownBy(() -> Neo4jConversions.assertTypeMap(map));
	}

	static Stream<Arguments> assertTypeNonEmptyMapShouldWork() {
		return Stream.of(Map.of("BIGINT", String.class, "VARCHAR", Integer.class)).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource
	void assertTypeNonEmptyMapShouldWork(Map<String, Class<?>> map) {
		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> Neo4jConversions.assertTypeMap(map))
			.matches(ex -> ex.getErrorCode() == 0 && "22N11".equals(ex.getSQLState()))
			.withMessage(
					"Invalid argument: cannot process non-empty type map BIGINT = class java.lang.String, VARCHAR = class java.lang.Integer");
	}

}
