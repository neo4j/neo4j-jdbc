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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.Neo4jConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConnectionIT extends IntegrationTestBase {

	@Test
	void shouldCheckTXStateBeforeCommit() throws SQLException {
		var handler = new CapturingHandler();
		Logger.getLogger("org.neo4j.jdbc.connection").addHandler(handler);
		try (var connection = getConnection()) {
			assertThatNoException().isThrownBy(connection::commit);
			assertThat(handler.messages).contains("There is no active transaction that can be committed, ignoring");
		}
		finally {
			Logger.getLogger("org.neo4j.jdbc.connection").removeHandler(handler);
		}
	}

	@Test
	void shouldCheckTXStateBeforeCommitWhenAutoCommitted() throws SQLException {
		var handler = new CapturingHandler();
		Logger.getLogger("org.neo4j.jdbc.connection").addHandler(handler);
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			stmt.executeUpdate("RETURN 1");
			assertThatNoException().isThrownBy(connection::commit);
			assertThat(handler.messages).contains("There is no active transaction that can be committed, ignoring");
		}
		finally {
			Logger.getLogger("org.neo4j.jdbc.connection").removeHandler(handler);
		}
	}

	@Test
	void shouldCheckTXStateBeforeRollback() throws SQLException {
		var handler = new CapturingHandler();
		Logger.getLogger("org.neo4j.jdbc.connection").addHandler(handler);
		try (var connection = getConnection()) {
			assertThatNoException().isThrownBy(connection::rollback);
			assertThat(handler.messages).contains("There is no active transaction that can be rolled back, ignoring");
		}
		finally {
			Logger.getLogger("org.neo4j.jdbc.connection").removeHandler(handler);
		}
	}

	@Test
	void shouldCheckTXStateBeforeRollbackWhenAutoRolledBack() throws SQLException {
		var handler = new CapturingHandler();
		Logger.getLogger("org.neo4j.jdbc.connection").addHandler(handler);
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			assertThatThrownBy(() -> stmt.executeQuery("UNWIND [1, 1, 1, 1, 0] AS x RETURN 1/x"))
				.isExactlyInstanceOf(SQLException.class);
			assertThatNoException().isThrownBy(connection::rollback);
			assertThat(handler.messages).contains("There is no active transaction that can be rolled back, ignoring");
		}
		finally {
			Logger.getLogger("org.neo4j.jdbc.connection").removeHandler(handler);
		}
	}

	@Test
	void shouldBeginNewTransactionAfterFailureInAutoCommit() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			// tx1 should fail
			assertThatThrownBy(() -> statement.executeQuery("UNWIND [1, 1, 1, 1, 0] AS x RETURN 1/x"))
				.isExactlyInstanceOf(SQLException.class);
			// tx2 should succeed
			var resultSet = statement.executeQuery("RETURN 1");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isEqualTo(1);
		}
	}

	@Test
	void shouldExpectRollbackBeforeStartingNewTransactionInExplicitMode() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			// tx1 should fail
			assertThatThrownBy(() -> statement.executeQuery("UNWIND [1, 1, 1, 1, 0] AS x RETURN 1/x"))
				.isExactlyInstanceOf(SQLException.class);
			// tx1 should remain failed
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1")).isExactlyInstanceOf(SQLException.class);
			// tx1 should finish
			connection.rollback();
			// tx2 should succeed
			var resultSet = statement.executeQuery("RETURN 1");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isEqualTo(1);
		}
	}

	@Test
	void shouldThrowOnMultipleOpenStreamsInAutoCommit() throws SQLException {
		var uuid = UUID.randomUUID().toString();
		try (var connection = getConnection();
				var statement1 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n");
				var statement2 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n");
				var statement3 = connection.prepareStatement("MATCH (n {test: $1}) RETURN count(n)")) {
			statement1.setFetchSize(2);
			statement2.setFetchSize(2);
			statement1.setString(1, uuid);
			statement2.setString(1, uuid);
			statement3.setString(1, uuid);

			// begin tx
			var resultSet1 = statement1.executeQuery();
			// attempt to begin another tx should fail
			assertThatThrownBy(() -> statement2.executeQuery()).isExactlyInstanceOf(SQLException.class);
			// commit tx
			resultSet1.close();

			var resultSet3 = statement3.executeQuery();
			assertThat(resultSet3.next()).isTrue();
			assertThat(resultSet3.getInt(1)).isEqualTo(5);
		}
	}

	@Test
	void shouldSupportMultipleOpenStreamsInExplicitMode() throws SQLException {
		var uuid = UUID.randomUUID().toString();
		try (var connection = getConnection();
				var statement1 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n");
				var statement2 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n");
				var statement3 = connection.prepareStatement("MATCH (n {test: $1}) RETURN count(n)")) {
			connection.setAutoCommit(false);
			statement1.setFetchSize(2);
			statement2.setFetchSize(2);
			statement1.setString(1, uuid);
			statement2.setString(1, uuid);
			statement3.setString(1, uuid);

			var resultSet1 = statement1.executeQuery();
			var resultSet2 = statement2.executeQuery();
			connection.commit();

			var resultSet3 = statement3.executeQuery();
			assertThat(resultSet3.next()).isTrue();
			assertThat(resultSet3.getInt(1)).isEqualTo(10);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCommitOnUpdatingAutoCommitMode(boolean autocommit) throws SQLException {
		var uuid = UUID.randomUUID().toString();
		try (var connection = getConnection();
				var statement1 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n");
				var statement2 = connection.prepareStatement("MATCH (n {test: $1}) RETURN count(n)")) {
			connection.setAutoCommit(autocommit);
			statement1.setFetchSize(2);
			statement1.setString(1, uuid);
			statement2.setString(1, uuid);

			var resultSet1 = statement1.executeQuery();
			connection.setAutoCommit(!autocommit);

			var resultSet2 = statement2.executeQuery();
			assertThat(resultSet2.next()).isTrue();
			assertThat(resultSet2.getInt(1)).isEqualTo(5);
		}
	}

	@Test
	void shouldValidateOutsideTransaction() throws SQLException {
		try (var connection = getConnection()) {
			assertThat(connection.isValid(0)).isTrue();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldValidateInTransaction(boolean autocommit) throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(autocommit);
			statement.setFetchSize(2);
			statement.executeQuery("UNWIND range(1, 5) AS x RETURN 1/x");

			assertThat(connection.isValid(0)).isTrue();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldAbortAndNotAcceptFurtherWork(boolean autocommit) throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			connection.setAutoCommit(autocommit);
			statement.setFetchSize(2);
			var resultSet = statement.executeQuery("UNWIND range(1, 5) AS x RETURN 1/x");

			connection.abort(Executors.newSingleThreadExecutor());

			resultSet.next();
			resultSet.next();
			assertThatThrownBy(resultSet::next).isExactlyInstanceOf(SQLException.class);
			assertThatThrownBy(() -> statement.executeQuery("RETURN 1")).isExactlyInstanceOf(SQLException.class);
			assertThatThrownBy(() -> connection.getMetaData().getUserName()).isExactlyInstanceOf(SQLException.class);
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldRollbackOnClose(boolean autocommit) throws SQLException {
		// given
		var uuid = UUID.randomUUID().toString();
		var connection = getConnection();
		try (var statement1 = connection.prepareStatement("UNWIND range(1, 5) AS x CREATE (n {test: $1}) RETURN n")) {
			connection.setAutoCommit(autocommit);
			statement1.setFetchSize(1);
			statement1.setString(1, uuid);

			// when
			var resultSet = statement1.executeQuery();
			connection.close();

			// then
			resultSet.next();
			assertThatThrownBy(resultSet::next).isExactlyInstanceOf(SQLException.class);
		}

		try (var varificationConnection = getConnection();
				var statement = varificationConnection.prepareStatement("MATCH (n {test: $1}) RETURN count(n)")) {
			statement.setString(1, uuid);
			var resultSet = statement.executeQuery();
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getInt(1)).isEqualTo(0);
		}
	}

	@Test
	void shouldRaiseErrorOnClosingResultSetWhenInAutoCommit() throws SQLException {
		try (var connection = getConnection(); var statement = connection.createStatement()) {
			statement.setFetchSize(2);
			var resultSet = statement.executeQuery("UNWIND [1, 1, 0] AS x CREATE (n {val: 1/x}) RETURN n");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.next()).isTrue();
			assertThatThrownBy(resultSet::close).isExactlyInstanceOf(SQLException.class);
		}
	}

	@Test
	void shouldRaiseErrorOnClosingStatementWhenInAutoCommit() throws SQLException {
		try (var connection = getConnection()) {
			var statement = connection.createStatement();
			statement.setFetchSize(2);
			var resultSet = statement.executeQuery("UNWIND [1, 1, 0] AS x CREATE (n {val: 1/x}) RETURN n");
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.next()).isTrue();
			assertThatThrownBy(statement::close).isExactlyInstanceOf(SQLException.class);
		}
	}

	@Test
	void shouldRetrieveDatabaseName() throws SQLException {
		try (var connection = getConnection()) {
			var neo4jConnection = connection.unwrap(Neo4jConnection.class);
			assertThat(neo4jConnection.getDatabaseName()).isNotBlank();
		}
	}

	static class CapturingHandler extends Handler {

		List<String> messages = new ArrayList<>();

		@Override
		public void publish(LogRecord record) {
			this.messages.add(record.getMessage());
		}

		@Override
		public void flush() {

		}

		@Override
		public void close() throws SecurityException {

		}

	}

}
