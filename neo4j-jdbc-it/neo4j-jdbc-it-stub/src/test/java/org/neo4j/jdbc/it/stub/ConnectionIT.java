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
package org.neo4j.jdbc.it.stub;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.jdbc.Neo4jMetadataWriter;
import org.neo4j.jdbc.it.stub.server.IntegrationTestBase;
import org.neo4j.jdbc.it.stub.server.StubScript;
import org.neo4j.jdbc.values.UnsupportedType;
import org.neo4j.jdbc.values.Value;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionIT extends IntegrationTestBase {

	@Test
	@StubScript(path = "reader_tx_with_mode.script")
	void shouldSendReadMode() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setReadOnly(true);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "writer_tx_with_begin_1_delay.script")
	void shouldThrowOnBeginTimeout() throws SQLException {
		assertThrowingOnTimeout();
	}

	private void assertThrowingOnTimeout() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setNetworkTimeout(null, 100);
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1 as n")).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "writer_tx_with_run_1_delay.script")
	void shouldThrowOnRunTimeout() throws SQLException {
		assertThrowingOnTimeout();
	}

	@Test
	@StubScript(path = "writer_tx_with_pull_1_delay.script")
	void shouldThrowOnPullTimeout() throws SQLException {
		assertThrowingOnTimeout();
	}

	@Test
	@StubScript(path = "writer_tx_with_commit_1_delay.script")
	void shouldThrowOnCommitTimeout() throws SQLException {
		try (var connection = getConnection()) {
			var statement = connection.createStatement();
			connection.setNetworkTimeout(null, 100);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
			assertThatThrownBy(statement::close).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "writer_tx_with_rollback_1_delay.script")
	void shouldThrowOnRollbackTimeout() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			connection.setNetworkTimeout(null, 100);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
			assertThatThrownBy(connection::rollback).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "write_tx_with_1_delays.script")
	void shouldExecuteWithDelays() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setNetworkTimeout(null, 2000);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_begin_2_delay.script")
	void shouldThrowOnBeginTimeoutWithHint() throws SQLException {
		assertThrowingOnTimeoutWithHint();
	}

	private void assertThrowingOnTimeoutWithHint() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1 as n")).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_run_2_delay.script")
	void shouldThrowOnRunTimeoutWithHint() throws SQLException {
		assertThrowingOnTimeoutWithHint();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_pull_2_delay.script")
	void shouldThrowOnPullTimeoutWithHint() throws SQLException {
		assertThrowingOnTimeoutWithHint();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_commit_2_delay.script")
	void shouldThrowOnCommitTimeoutWithHint() throws SQLException {
		try (var connection = getConnection()) {
			var statement = connection.createStatement();
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
			assertThatThrownBy(statement::close).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_rollback_2_delay.script")
	void shouldThrowOnRollbackTimeoutWithHint() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
			assertThatThrownBy(connection::rollback).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "hint/timeout/write_tx_with_1_delays.script")
	void shouldExecuteWithDelaysWithHint() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "hint/timeout/writer_tx_with_second_begin_timeout.script")
	void shouldApplyDefaultThrowOnBeginTimeouttest() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			connection.setNetworkTimeout(null, 4000);
			var result = statement.executeQuery("RETURN 1 as n");
			while (result.next()) {
				assertThat(result.getInt(1)).isEqualTo(1);
			}
			connection.setNetworkTimeout(null, 0);
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1 as n")).isInstanceOf(SQLException.class);
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "writer_tx_with_2nd_pull_1_delay.script")
	void shouldCloseOpenAutoClosablesOnTimeout() throws SQLException {
		try (var connection = getConnection();
				var statement = connection.createStatement();
				var preparedStatement = connection.prepareStatement("RETURN 1");
				var anotherStatement = connection.prepareStatement("RETURN 2")) {
			assertThat(preparedStatement.isClosed()).isFalse();
			assertThat(anotherStatement.isClosed()).isFalse();
			connection.setNetworkTimeout(null, 100);
			statement.setFetchSize(1);
			statement.closeOnCompletion();
			var result = statement.executeQuery("RETURN 1 as n");
			result.next();
			assertThat(result.getInt(1)).isEqualTo(1);

			assertThatThrownBy(result::next).isInstanceOf(SQLException.class);
			assertThat(result.isClosed()).isTrue();
			assertThat(statement.isClosed()).isTrue();
			assertThat(preparedStatement.isClosed()).isTrue();
			assertThat(anotherStatement.isClosed()).isTrue();
			assertThat(connection.isClosed()).isTrue();
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "server_with_reset_1_delay.script")
	void shouldTimeoutOnIsValidWithNetworkTimeoutWithoutTransacton() throws SQLException {
		try (var connection = getConnection()) {
			connection.setNetworkTimeout(null, 100);
			assertThat(connection.isValid(0)).isFalse();
			assertThat(connection.isClosed()).isTrue();
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "echo_unsupported.script")
	void unsupportedTypeShouldWork() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setReadOnly(true);
			var result = statement.executeQuery("RETURN 1 as n");
			assertThat(result.next()).isTrue();

			assertThatException().isThrownBy(() -> result.getInt(1))
				.withMessage(
						"data exception - Cannot coerce UnsupportedType[name=foo, minProtocolVersion=47.11, message=Whatever] (UNSUPPORTED) to int");

			var ut = result.getObject(1, UnsupportedType.class);
			assertThat(ut.name()).isEqualTo("foo");
			assertThat(ut.minProtocolVersion()).isEqualTo("47.11");
			assertThat(ut.message()).isEqualTo("Whatever");

			assertThat(result.getMetaData().getColumnClassName(1)).isEqualTo("org.neo4j.jdbc.values.UnsupportedType");

			var v = result.getObject(1, Value.class);
			assertThat(v.asObject()).isEqualTo(new UnsupportedType("foo", "47.11", "Whatever"));
			assertThat(result.next()).isFalse();
		}

		verifyStubServer();
	}

	@Test
	@StubScript(path = "writer_tx_with_2nd_run_1_delay.script")
	void shouldTimeoutOnIsValidOnNetworkTimeoutWithTransacton() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			connection.setNetworkTimeout(null, 100);
			statement.setFetchSize(1);
			var result = statement.executeQuery("RETURN 1 as n");
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isEqualTo(1);

			assertThat(connection.isValid(0)).isFalse();

			assertThat(result.isClosed()).isTrue();
			assertThat(statement.isClosed()).isTrue();
			assertThat(connection.isClosed()).isTrue();
		}

		verifyStubServer();
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			true, true
			false, true
			true, false
			false, false
			""")
	@StubScript(path = "tx_meta.script")
	void shouldSendMetadata(boolean autoCommit, boolean onConnection) throws Exception {
		restoreSystemProperties(() -> {

			System.setProperty("java.version", "1.4");
			System.setProperty("java.vm.vendor", "ms");
			System.setProperty("java.vm.name", "fake");
			System.clearProperty("java.vm.version");
			System.setProperty("neo4j.jdbc.version", "xxx");

			try (var connection = getConnection(); var statement = connection.createStatement()) {
				connection.setClientInfo("ApplicationName", "StubServerTest");
				connection.setAutoCommit(autoCommit);

				Neo4jMetadataWriter metadataWriter = onConnection ? connection.unwrap(Neo4jMetadataWriter.class)
						: statement.unwrap(Neo4jMetadataWriter.class);
				metadataWriter.withMetadata(Map.of("akey", "aval"));

				statement.unwrap(Neo4jMetadataWriter.class).withMetadata(Map.of("akey2", "aval2"));

				var result = statement.executeQuery("RETURN 1 as n");
				while (result.next()) {
					assertThat(result.getInt(1)).isEqualTo(1);
				}
				if (!autoCommit) {
					connection.commit();
				}
			}
		});

		verifyStubServer();
	}

}
