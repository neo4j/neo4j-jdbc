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
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
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
import org.neo4j.jdbc.values.Value;

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
	@DisabledInNativeImage // Due to Spying on things
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
