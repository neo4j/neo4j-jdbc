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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
				resultSet.close();
			}
		}
	}

	@Test
	void autocommitAndPreparedStatementShouldWork() throws SQLException {
		var testId = UUID.randomUUID().toString();
		try (var connection = getConnection(true, true)) {
			for (int i = 0; i < 10; ++i) {
				try (var statement = connection.prepareStatement("INSERT INTO TestWithAutoCommit(testId) VALUES(?)")) {
					statement.setString(1, testId);
					statement.execute();
				}
			}
		}

		try (var connection = getConnection()) {
			try (var statement = connection
				.prepareStatement("MATCH (n:TestWithAutoCommit {testId: $1}) RETURN count(n)")) {
				statement.setString(1, testId);
				var resultSet = statement.executeQuery();
				resultSet.next();
				assertThat(resultSet.getInt(1)).isEqualTo(10);
				resultSet.close();
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

	@Nested
	class SymmetricTypeConversionInPreparedStatementAndResultSet {

		static Stream<Arguments> dateMappingShouldWork() {
			var cal = Calendar.getInstance();
			cal.clear();
			cal.set(2024, Calendar.FEBRUARY, 29);
			var expectedDate = cal.getTime();
			return Stream.of(
					Arguments.of("DATE", expectedDate, null, "RETURN date({year: 2024, month: 2, day: 29}) AS v"),
					Arguments.of("ZONED TIME (not supported)", expectedDate, SQLException.class,
							"RETURN time() AS v, 'TIME value cannot be mapped to java.sql.Date' AS m"),
					Arguments.of("LOCAL TIME (not supported)", expectedDate, SQLException.class,
							"RETURN localtime() AS v, 'LOCAL_TIME value cannot be mapped to java.sql.Date' AS m"),
					Arguments.of("ZONED DATETIME", expectedDate, null,
							"RETURN datetime({year: 2024, month: 2, day: 29}) AS v"),
					Arguments.of("ZONED DATETIME", expectedDate, null,
							"RETURN datetime({year: 2024, month: 2, day: 29, hour: 22, timezone: 'America/New_York'}) AS v"),
					Arguments.of("LOCAL DATETIME", expectedDate, null,
							"RETURN localdatetime({year: 2024, month: 2, day: 29}) AS v"));
		}

		@ParameterizedTest(name = "Mapping of {0} to java.sql.Date")
		@MethodSource
		void dateMappingShouldWork(@SuppressWarnings("unused") String description, java.util.Date expected,
				Class<SQLException> exceptionClass, String query) throws SQLException {
			try (var connection = getConnection();
					var stmt = connection.createStatement();
					var rs = stmt.executeQuery(query)) {
				assertThat(rs.next()).isTrue();

				if (exceptionClass != null) {
					var msg = rs.getString("m");
					assertThatExceptionOfType(exceptionClass).isThrownBy(() -> rs.getDate("v")).withMessage(msg);
				}
				else {
					var date = rs.getDate("v");
					assertThat(date).isEqualTo(expected);

					var refZone = TimeZone.getTimeZone("Atlantic/Canary").toZoneId();
					var refCal = GregorianCalendar
						.from(ZonedDateTime.of(LocalDate.of(2023, 9, 21), LocalTime.of(21, 21, 21), refZone));
					refCal.set(Calendar.MILLISECOND, 42);
					var hlp = rs.getObject("v", Value.class);
					if (Type.DATE_TIME.isTypeOf(hlp)) {
						var cal = Calendar.getInstance();
						cal.clear();
						var ld = hlp.asZonedDateTime().withZoneSameInstant(refZone).toLocalDate();
						// noinspection MagicConstant
						cal.set(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
						expected = cal.getTime();
					}
					date = rs.getDate("v", refCal);
					assertThat(date).isEqualTo(expected);
				}
			}
		}

		@SuppressWarnings("deprecation")
		static Stream<Arguments> timeMappingShouldWork() {
			var expectedTime = new Time(23, 59, 59);
			return Stream.of(Arguments.of("DATE (not supported)", expectedTime, SQLException.class,
					"RETURN date({year: 2024, month: 2, day: 29}) AS v, 'DATE value cannot be mapped to java.sql.Time' AS m"),
					Arguments.of("ZONED TIME", expectedTime, null,
							"RETURN time({hour: 23, minute: 59, second: 59}) AS v"),
					Arguments.of("ZONED TIME", expectedTime, null,
							"RETURN time({hour: 23, minute: 59, second: 59, timezone: 'America/New_York'}) AS v"),
					Arguments.of("LOCAL TIME", expectedTime, null,
							"RETURN localtime({hour: 23, minute: 59, second: 59}) AS v"),
					Arguments.of("ZONED DATETIME", expectedTime, null,
							"RETURN datetime({year: 2024, month: 1, day: 29, hour: 23, minute: 59, second: 59}) AS v"),
					Arguments.of("ZONED DATETIME", expectedTime, null,
							"RETURN datetime({year: 2024, month: 1, day: 29, hour: 23, minute: 59, second: 59, timezone: 'America/New_York'}) AS v"),
					Arguments.of("LOCAL DATETIME", expectedTime, null,
							"RETURN localdatetime({year: 2024, month: 2, day: 29, hour: 23, minute: 59, second: 59}) AS v"));
		}

		@ParameterizedTest(name = "Mapping of {0} to java.sql.Time")
		@MethodSource
		void timeMappingShouldWork(@SuppressWarnings("unused") String description, Time expected,
				Class<SQLException> exceptionClass, String query) throws SQLException {
			try (var connection = getConnection();
					var stmt = connection.createStatement();
					var rs = stmt.executeQuery(query)) {
				assertThat(rs.next()).isTrue();

				if (exceptionClass != null) {
					var msg = rs.getString("m");
					assertThatExceptionOfType(exceptionClass).isThrownBy(() -> rs.getTime("v")).withMessage(msg);
				}
				else {
					var date = rs.getTime("v");
					assertThat(date).isEqualTo(expected);

					var refZone = TimeZone.getTimeZone("Atlantic/Canary").toZoneId();
					var referenceCalendar = GregorianCalendar
						.from(ZonedDateTime.of(LocalDate.of(2023, 9, 21), LocalTime.of(21, 21, 21), refZone));
					var hlp = rs.getObject("v", Value.class);
					if (Type.DATE_TIME.isTypeOf(hlp)) {
						var lt = hlp.asZonedDateTime()
							.toOffsetDateTime()
							.toOffsetTime()
							.withOffsetSameInstant(refZone.getRules().getOffset(referenceCalendar.toInstant()));
						expected = Time.valueOf(lt.toLocalTime());
					}
					else if (Type.TIME.isTypeOf(hlp)) {
						var lt = hlp.asOffsetTime()
							.withOffsetSameInstant(refZone.getRules().getOffset(referenceCalendar.toInstant()));
						expected = Time.valueOf(lt.toLocalTime());
					}
					date = rs.getTime("v", referenceCalendar);
					assertThat(date).isEqualTo(expected);
				}
			}
		}

		static Stream<Arguments> timestampMappingShouldWork() {
			var defaultExpectation = LocalDateTime.of(2024, 2, 29, 23, 59, 59);
			var expectedTime = Timestamp.valueOf(defaultExpectation);
			return Stream.of(Arguments.of("DATE (not supported)", null, SQLException.class,
					"RETURN date({year: 2024, month: 2, day: 29}) AS v, 'DATE value cannot be mapped to java.sql.Timestamp' AS m"),
					Arguments.of("ZONED TIME (not supported)", expectedTime, SQLException.class,
							"RETURN time({hour: 23, minute: 59, second: 59}) AS v, 'TIME value cannot be mapped to java.sql.Timestamp' AS m"),
					Arguments.of("LOCAL TIME  (not supported)", expectedTime, SQLException.class,
							"RETURN localtime({hour: 23, minute: 59, second: 59}) AS v, 'LOCAL_TIME value cannot be mapped to java.sql.Timestamp' AS m"),
					Arguments.of("ZONED DATETIME", expectedTime, null,
							"RETURN datetime({year: 2024, month: 2, day: 29, hour: 23, minute: 59, second: 59}) AS v"),
					Arguments.of("ZONED DATETIME", expectedTime, null,
							"RETURN datetime({year: 2024, month: 2, day: 29, hour: 23, minute: 59, second: 59, timezone: 'America/New_York'}) AS v"),
					Arguments.of("LOCAL DATETIME", expectedTime, null,
							"RETURN localdatetime({year: 2024, month: 2, day: 29, hour: 23, minute: 59, second: 59}) AS v"));
		}

		@ParameterizedTest(name = "Mapping of {0} to java.sql.Timestamp")
		@MethodSource
		void timestampMappingShouldWork(@SuppressWarnings("unused") String description, Timestamp expected,
				Class<SQLException> exceptionClass, String query) throws SQLException {
			try (var connection = getConnection();
					var stmt = connection.createStatement();
					var rs = stmt.executeQuery(query)) {
				assertThat(rs.next()).isTrue();

				if (exceptionClass != null) {
					var msg = rs.getString("m");
					assertThatExceptionOfType(exceptionClass).isThrownBy(() -> rs.getTimestamp("v")).withMessage(msg);
				}
				else {
					Timestamp date = rs.getTimestamp("v");
					assertThat(date).isEqualTo(expected);

					var refZone = TimeZone.getTimeZone("Atlantic/Canary").toZoneId();
					var referenceCalendar = GregorianCalendar
						.from(ZonedDateTime.of(LocalDate.of(2023, 9, 21), LocalTime.of(21, 21, 21), refZone));
					var hlp = rs.getObject("v", Value.class);
					if (Type.DATE_TIME.isTypeOf(hlp)) {
						var ldt = hlp.asZonedDateTime().withZoneSameInstant(refZone).toLocalDateTime();
						expected = Timestamp.valueOf(ldt);
					}
					date = rs.getTimestamp("v", referenceCalendar);
					assertThat(date).isEqualTo(expected);
				}
			}
		}

		// GH-412
		@SuppressWarnings("unchecked")
		@Test
		void mappingOfTimestampsMustBeConsistent() throws SQLException {
			var query = """
					WITH datetime('2015-06-24T12:50:35.556+0100') AS zonedDateTime
					RETURN
						zonedDateTime,
						[zonedDateTime] AS zonedDateTimeList,
						{zonedDateTime: zonedDateTime} AS zonedDateTimeDictionary""";
			try (var connection = getConnection(); var results = connection.createStatement().executeQuery(query)) {

				while (results.next()) {
					var theDateTime = results.getObject("zonedDateTime");
					assertThat(theDateTime).isInstanceOf(ZonedDateTime.class);
					theDateTime = ((Iterable<Object>) results.getObject("zonedDateTimeList")).iterator().next();
					assertThat(theDateTime).isInstanceOf(ZonedDateTime.class);
					theDateTime = ((Map<String, Object>) results.getObject("zonedDateTimeDictionary"))
						.get("zonedDateTime");
					assertThat(theDateTime).isInstanceOf(ZonedDateTime.class);
				}

			}
		}

		@SuppressWarnings("deprecation")
		@Test
		void symmetrie() throws SQLException {

			var berlin = TimeZone.getTimeZone("Europe/Berlin");
			var berlinZoneId = berlin.toZoneId();
			var berlinOffset = berlinZoneId.getRules().getOffset(LocalDateTime.now());
			var newYork = TimeZone.getTimeZone("America/New_York");

			var cal = Calendar.getInstance(berlin);
			var ld = LocalDate.of(2024, 3, 7);
			var lt = LocalTime.of(1, 23, 42);
			var date = Date.valueOf(ld);
			var time = Time.valueOf(lt);
			var timestamp = Timestamp.valueOf(LocalDateTime.of(ld, lt));
			var zdt = LocalDateTime.of(ld, lt).atZone(berlinZoneId);
			var bd = BigDecimal.ONE.divide(new BigDecimal("2"), 2, RoundingMode.HALF_EVEN);

			try (var connection = getConnection(false, false); var statement = connection.prepareStatement("""
					RETURN $1 AS d1, $2 AS t1, $3 AS ts1,
						$4 AS d2, $5 AS t2, $6 AS ts2,
						$7 AS x,
						$8 AS bd, "2.124" AS bd2, 1.0 AS bd3
					""")) {

				statement.setDate(1, date, cal);
				statement.setTime(2, time, cal);
				statement.setTimestamp(3, timestamp, cal);

				statement.setDate(4, date);
				statement.setTime(5, time);
				statement.setTimestamp(6, timestamp);

				statement.setObject(7, Values.value(zdt));
				statement.setBigDecimal(8, bd);

				var result = statement.executeQuery();
				assertThat(result.next()).isTrue();

				var cal3 = Calendar.getInstance(newYork);
				cal3.set(2024, 2, 10, 11, 0, 0);
				var cal4 = Calendar.getInstance(newYork);
				cal4.set(2024, 2, 10, 13, 0, 0);
				for (var refCal : new Calendar[] { cal, Calendar.getInstance(newYork), cal3, cal4 }) {
					var refZone = refCal.getTimeZone().toZoneId();
					var refOffset = refZone.getRules().getOffset(refCal.toInstant());

					// Those had an offset when writing and have been converted into
					// target
					assertThat(Type.DATE_TIME.isTypeOf(result.getObject(1, Value.class))).isTrue();
					assertThat(result.getDate(1)).isEqualTo(date);
					assertThat(result.getDate(1, refCal)).isEqualTo(Date.valueOf(
							date.toLocalDate().atStartOfDay(berlinZoneId).withZoneSameInstant(refZone).toLocalDate()));

					assertThat(Type.TIME.isTypeOf(result.getObject(2, Value.class))).isTrue();
					assertThat(result.getTime(2)).isEqualTo(time);
					assertThat(result.getTime(2, refCal)).isEqualTo(Time.valueOf(
							time.toLocalTime().atOffset(berlinOffset).withOffsetSameInstant(refOffset).toLocalTime()));

					assertThat(Type.DATE_TIME.isTypeOf(result.getObject(3, Value.class))).isTrue();
					assertThat(result.getTimestamp(3)).isEqualTo(timestamp);
					assertThat(result.getTimestamp(3, refCal)).isEqualTo(Timestamp.valueOf(timestamp.toLocalDateTime()
						.atZone(berlinZoneId)
						.withZoneSameInstant(refZone)
						.toLocalDateTime()));

					// Those did not have an offset and are stored without
					assertThat(Type.DATE.isTypeOf(result.getObject(4, Value.class))).isTrue();
					assertThat(result.getDate(4)).isEqualTo(date);
					assertThat(result.getDate(4, refCal)).isEqualTo(date);

					assertThat(Type.LOCAL_TIME.isTypeOf(result.getObject(5, Value.class))).isTrue();
					assertThat(result.getTime(5)).isEqualTo(time);
					assertThat(result.getTime(5, refCal)).isEqualTo(time);

					assertThat(Type.LOCAL_DATE_TIME.isTypeOf(result.getObject(6, Value.class))).isTrue();
					assertThat(result.getTimestamp(6)).isEqualTo(timestamp);
					assertThat(result.getTimestamp(6, refCal)).isEqualTo(timestamp);

					assertThat(result.getDate(7)).isEqualTo(Date.valueOf("2024-03-07"));
					assertThat(result.getDate(7, refCal))
						.isEqualTo(Date.valueOf(zdt.withZoneSameInstant(refZone).toLocalDate()));
					assertThat(result.getTime(7)).isEqualTo(Time.valueOf("01:23:42"));
					assertThat(result.getTimestamp(7)).isEqualTo(Timestamp.valueOf("2024-03-07 01:23:42"));
					assertThat(result.getTime(7, refCal))
						.isEqualTo(Time.valueOf(zdt.toOffsetDateTime().withOffsetSameInstant(refOffset).toLocalTime()));
					assertThat(result.getTimestamp(7, refCal))
						.isEqualTo(Timestamp.valueOf(zdt.withZoneSameInstant(refZone).toLocalDateTime()));
					assertThat(result.getObject(7, Value.class).asZonedDateTime()).isEqualTo(zdt);
				}
				assertThat(result.getBigDecimal(8)).isEqualTo(bd);
				assertThat(result.getBigDecimal(8, 2)).isEqualTo(bd.setScale(2, RoundingMode.HALF_EVEN));

				assertThat(result.getBigDecimal(9)).isEqualTo(new BigDecimal("2.124"));
				assertThatExceptionOfType(SQLException.class).isThrownBy(() -> result.getBigDecimal(9, 1))
					.withCauseInstanceOf(ArithmeticException.class);
				assertThat(result.getBigDecimal(10)).isEqualTo(new BigDecimal("1.0"));

				result.close();
			}
		}

	}

	@Nested
	@DisabledInNativeImage // Due to spying on things
	class Streams {

		static String readAsString(URL resource) throws IOException {
			StringWriter target = new StringWriter();
			try (var in = new BufferedReader(new InputStreamReader(resource.openStream()))) {
				in.transferTo(target);
			}
			return target.toString();
		}

		static Stream<Arguments> characterStream() {
			return Stream.of(Arguments.of(false, null, null), Arguments.of(true, null, null),
					Arguments.of(false, -1, null), Arguments.of(false, 0, null), Arguments.of(false, 23, null),
					Arguments.of(false, 3888, null), Arguments.of(false, 5000, null), Arguments.of(false, null, -1L),
					Arguments.of(false, null, 0L), Arguments.of(false, null, 23L), Arguments.of(false, null, 3888L),
					Arguments.of(false, null, 5000L));
		}

		@ParameterizedTest
		@MethodSource
		void characterStream(boolean named, Integer lengthI, Long lengthL) throws SQLException, IOException {
			var type = "setCharacterStream";
			var lengthUsed = Optional.ofNullable(lengthL)
				.map(Long::intValue)
				.or(() -> Optional.ofNullable(lengthI))
				.orElse(null);

			var resource = Objects.requireNonNull(PreparedStatementIT.class.getResource("/cc/docker-compose.yml"));

			if (lengthUsed != null && lengthUsed < 0) {
				try (var connection = getConnection();
						var ps = connection.prepareStatement("RETURN $1");
						var r = new StringReader("")) {
					ThrowableAssert.ThrowingCallable callable;
					if (lengthI != null) {
						callable = () -> ps.setCharacterStream(1, r, lengthI);
					}
					else {
						callable = () -> ps.setCharacterStream(1, r, lengthL);
					}

					assertThatExceptionOfType(SQLException.class).isThrownBy(callable)
						.withMessage("Invalid length -1 for character stream at index 1");

				}
				return;
			}

			var originalContent = readAsString(resource);

			try (var connection = getConnection()) {
				try (var in = Mockito.spy(new InputStreamReader(resource.openStream()));
						var ps = connection.prepareStatement("CREATE (m:CSTest {type: $1, content: $2})")) {
					ps.setString(1, type);
					if (lengthI != null) {
						ps.setCharacterStream(2, in, lengthI);
					}
					else if (lengthL != null) {
						ps.setCharacterStream(2, in, lengthL);
					}
					else if (named) {
						ps.unwrap(Neo4jPreparedStatement.class).setCharacterStream("2", in);
					}
					else {
						ps.setCharacterStream(2, in);
					}
					var cnt = ps.executeUpdate();
					assertThat(cnt).isEqualTo(4);
					Mockito.verify(in).close();
				}

				try (var ps = connection.prepareStatement("MATCH (m:CSTest {type: $1}) RETURN m.content")) {
					ps.setString(1, type);
					try (var result = ps.executeQuery()) {
						assertThat(result.next()).isTrue();
						var actual = result.getString(1);
						if (lengthUsed != null) {
							assertThat(actual).isEqualTo(
									originalContent.substring(0, Math.min(lengthUsed, originalContent.length())));
						}
						else {
							assertThat(actual).isEqualTo(originalContent);
						}
					}
				}
			}
		}

		static Stream<Arguments> asciiStream() {
			return Stream.of(Arguments.of(false, null, null), Arguments.of(true, null, null),
					Arguments.of(false, -1, null), Arguments.of(false, 0, null), Arguments.of(false, 23, null),
					Arguments.of(false, 3888, null), Arguments.of(false, 5000, null), Arguments.of(false, null, -1L),
					Arguments.of(false, null, 0L), Arguments.of(false, null, 23L), Arguments.of(false, null, 3888L),
					Arguments.of(false, null, 5000L));
		}

		@ParameterizedTest
		@MethodSource
		void asciiStream(boolean named, Integer lengthI, Long lengthL) throws SQLException, IOException {
			var type = "setAsciiStream";
			var lengthUsed = Optional.ofNullable(lengthL)
				.map(Long::intValue)
				.or(() -> Optional.ofNullable(lengthI))
				.orElse(null);

			var resource = Objects.requireNonNull(PreparedStatementIT.class.getResource("/cc/docker-compose.yml"));

			if (lengthUsed != null && lengthUsed < 0) {
				try (var connection = getConnection();
						var ps = connection.prepareStatement("RETURN $1");
						var s = new ByteArrayInputStream(new byte[0])) {
					ThrowableAssert.ThrowingCallable callable;
					if (lengthI != null) {
						callable = () -> ps.setAsciiStream(1, s, lengthI);
					}
					else {
						callable = () -> ps.setAsciiStream(1, s, lengthL);
					}

					assertThatExceptionOfType(SQLException.class).isThrownBy(callable)
						.withMessage("Invalid length -1 for character stream at index 1");

				}
				return;
			}

			var originalContent = readAsString(resource);

			try (var connection = getConnection()) {
				try (var in = Mockito.spy(resource.openStream());
						var ps = connection.prepareStatement("CREATE (m:CSTest {type: $1, content: $2})")) {
					ps.setString(1, type);
					if (lengthI != null) {
						ps.setAsciiStream(2, in, lengthI);
					}
					else if (lengthL != null) {
						ps.setAsciiStream(2, in, lengthL);
					}
					else if (named) {
						ps.unwrap(Neo4jPreparedStatement.class).setAsciiStream("2", in);
					}
					else {
						ps.setAsciiStream(2, in);
					}
					var cnt = ps.executeUpdate();
					assertThat(cnt).isEqualTo(4);
					Mockito.verify(in).close();
				}

				try (var ps = connection.prepareStatement("MATCH (m:CSTest {type: $1}) RETURN m.content")) {
					ps.setString(1, type);
					try (var result = ps.executeQuery()) {
						assertThat(result.next()).isTrue();
						var actual = result.getString(1);
						if (lengthUsed != null) {
							assertThat(actual).isEqualTo(
									originalContent.substring(0, Math.min(lengthUsed, originalContent.length())));
						}
						else {
							assertThat(actual).isEqualTo(originalContent);
						}
					}
				}
			}
		}

		@ParameterizedTest
		@NullSource
		@ValueSource(longs = { -1, 0, 23, 3888, 5000 })
		void nCharacterStream(Long lengthUsed) throws SQLException, IOException {
			var type = "setNCharacterStream";
			var resource = Objects.requireNonNull(PreparedStatementIT.class.getResource("/cc/docker-compose.yml"));

			if (lengthUsed != null && lengthUsed < 0) {
				try (var connection = getConnection();
						var ps = connection.prepareStatement("RETURN $1");
						var r = new StringReader("")) {

					assertThatExceptionOfType(SQLException.class)
						.isThrownBy(() -> ps.setNCharacterStream(1, r, lengthUsed))
						.withMessage("Invalid length -1 for character stream at index 1");

				}
				return;
			}

			var originalContent = readAsString(resource);

			try (var connection = getConnection()) {

				try (var in = Mockito.spy(new InputStreamReader(resource.openStream()));
						var ps = connection.prepareStatement("CREATE (m:CSTest {type: $1, content: $2})")) {
					ps.setString(1, type);
					if (lengthUsed != null) {
						ps.setNCharacterStream(2, in, lengthUsed);
					}
					else {
						ps.setNCharacterStream(2, in);
					}
					var cnt = ps.executeUpdate();
					assertThat(cnt).isEqualTo(4);
					Mockito.verify(in).close();
				}

				try (var ps = connection.prepareStatement("MATCH (m:CSTest {type: $1}) RETURN m.content")) {
					ps.setString(1, type);
					try (var result = ps.executeQuery()) {
						assertThat(result.next()).isTrue();
						var actual = result.getString(1);
						if (lengthUsed != null) {
							assertThat(actual).isEqualTo(originalContent.substring(0,
									Math.toIntExact(Math.min(lengthUsed, originalContent.length()))));
						}
						else {
							assertThat(actual).isEqualTo(originalContent);
						}
					}
				}
			}
		}

		@SuppressWarnings("deprecation")
		@ParameterizedTest
		@ValueSource(ints = { -1, 0, 23, 3888, 5000 })
		void setUnicodeStream(int lengthUsed) throws SQLException, IOException {
			var type = "setUnicodeStream";
			var resource = Objects.requireNonNull(PreparedStatementIT.class.getResource("/cc/docker-compose.yml"));

			if (lengthUsed < 0) {
				try (var connection = getConnection();
						var ps = connection.prepareStatement("RETURN $1");
						var r = new ByteArrayInputStream(new byte[0])) {

					assertThatExceptionOfType(SQLException.class)
						.isThrownBy(() -> ps.setUnicodeStream(1, r, lengthUsed))
						.withMessage("Invalid length -1 for character stream at index 1");

				}
				return;
			}

			var originalContent = readAsString(resource);

			try (var connection = getConnection()) {

				try (var in = Mockito.spy(resource.openStream());
						var ps = connection.prepareStatement("CREATE (m:CSTest {type: $1, content: $2})")) {
					ps.setString(1, type);
					ps.setUnicodeStream(2, in, lengthUsed);
					var cnt = ps.executeUpdate();
					assertThat(cnt).isEqualTo(4);
					Mockito.verify(in).close();
				}

				try (var ps = connection.prepareStatement("MATCH (m:CSTest {type: $1}) RETURN m.content")) {
					ps.setString(1, type);
					try (var result = ps.executeQuery()) {
						assertThat(result.next()).isTrue();
						var actual = result.getString(1);
						assertThat(actual).isEqualTo(originalContent.substring(0,
								Math.toIntExact(Math.min(lengthUsed, originalContent.length()))));
					}
				}
			}
		}

		static Stream<Arguments> binaryStream() {
			return Stream.of(Arguments.of(false, null, null), Arguments.of(true, null, null),
					Arguments.of(false, -1, null), Arguments.of(false, 0, null), Arguments.of(false, 23, null),
					Arguments.of(false, 31408, null), Arguments.of(false, 32768, null), Arguments.of(false, null, -1L),
					Arguments.of(false, null, 0L), Arguments.of(false, null, 23L), Arguments.of(false, null, 31408L),
					Arguments.of(false, null, 32768L));
		}

		@ParameterizedTest
		@MethodSource
		void binaryStream(boolean named, Integer lengthI, Long lengthL)
				throws SQLException, IOException, NoSuchAlgorithmException {

			var type = "setBinaryStream";
			var lengthUsed = Optional.ofNullable(lengthL)
				.map(Long::intValue)
				.or(() -> Optional.ofNullable(lengthI))
				.orElse(null);

			var oss = Objects.requireNonNull(PreparedStatementIT.class.getResource("/opensourcerer.png"));

			if (lengthUsed != null && lengthUsed < 0) {
				try (var connection = getConnection();
						var ps = connection.prepareStatement("RETURN $1");
						var s = new ByteArrayInputStream(new byte[0])) {
					ThrowableAssert.ThrowingCallable callable;
					if (lengthI != null) {
						callable = () -> ps.setBinaryStream(1, s, lengthI);
					}
					else {
						callable = () -> ps.setBinaryStream(1, s, lengthL);
					}

					assertThatExceptionOfType(SQLException.class).isThrownBy(callable)
						.withMessage("Invalid length -1 for binary stream at index 1");

				}
				return;
			}

			var md5 = MessageDigest.getInstance("MD5");
			try (var connection = getConnection()) {
				try (var in = Mockito.spy(oss.openStream());
						var ps = connection.prepareStatement("CREATE (m:CSTest {type: $1, content: $2})")) {
					ps.setString(1, type);
					if (lengthI != null) {
						ps.setBinaryStream(2, in, lengthI);
					}
					else if (lengthL != null) {
						ps.setBinaryStream(2, in, lengthL);
					}
					else if (named) {
						ps.unwrap(Neo4jPreparedStatement.class).setBinaryStream("2", in);
					}
					else {
						ps.setBinaryStream(2, in);
					}
					var cnt = ps.executeUpdate();
					assertThat(cnt).isEqualTo(4);
					Mockito.verify(in).close();
				}

				try (var ps = connection.prepareStatement("MATCH (m:CSTest {type: $1}) RETURN m.content")) {
					ps.setString(1, type);
					try (var result = ps.executeQuery()) {
						assertThat(result.next()).isTrue();
						var actual = result.getObject(1, Value.class).asByteArray();
						if (lengthUsed != null) {
							if (lengthUsed == 0) {
								assertThat(actual).isEmpty();
							}
							else {
								var expectedHash = switch (lengthUsed) {
									case 23 -> "32E29FC162072EB1265CED9226AE74A0";
									case 31408, 32768 -> "DA4AF72F62E96D5A00CF20FEA8766D1C";
									default -> throw new RuntimeException();
								};
								assertThat(md5.digest(actual)).asHexString().isEqualTo(expectedHash);
							}

						}
						else {
							assertThat(md5.digest(actual)).asHexString().isEqualTo("DA4AF72F62E96D5A00CF20FEA8766D1C");
						}
					}
				}
			}
		}

	}

}
