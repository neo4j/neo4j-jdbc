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
package org.neo4j.driver.jdbc;

import java.sql.ClientInfoStatus;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class ConnectionImplTests {

	ConnectionImpl connection;

	@Test
	void getMetaData() throws SQLException {
		var boltConnection = Mockito.mock(BoltConnection.class);
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		try (var c = new ConnectionImpl(boltConnection)) {
			Assertions.assertThat(c.getMetaData()).isNotNull();
		}
		catch (UnsupportedOperationException ex) {
			// ignored
		}
	}

	@Test
	void shouldCreateStatement() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		var statement = this.connection.createStatement();

		assertThat(statement).isNotNull();
	}

	@Test
	void shouldPrepareStatement() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		var statement = this.connection.prepareStatement("sql");

		assertThat(statement).isNotNull();
	}

	@Test
	void shouldPrepareCall() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		var statement = this.connection.prepareCall("RETURN pi()");

		assertThat(statement).isNotNull();
	}

	@Test
	void shouldCallTranslator() throws SQLException {
		var translator = mock(SqlTranslator.class);
		var sql = "SQL";
		var expectedNativeSql = "nativeSQL";
		given(translator.translate(sql)).willReturn(expectedNativeSql);
		this.connection = new ConnectionImpl(mock(BoltConnection.class), () -> translator, false, false);

		var nativeSQL = this.connection.nativeSQL(sql);

		assertThat(nativeSQL).isEqualTo(expectedNativeSql);
		then(translator).should().translate(sql);
	}

	@Test
	void shouldBeAutoCommitByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getAutoCommit()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldSetAutoCommit(boolean autoCommit) throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		this.connection.setAutoCommit(autoCommit);

		assertThat(this.connection.getAutoCommit()).isEqualTo(autoCommit);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCommitTransactionOnAutoCommitUpdate(boolean autoCommit) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);
		this.connection.setAutoCommit(!autoCommit);
		var transactionType = this.connection.getAutoCommit() ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false))
			.willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.commit()).willReturn(CompletableFuture.completedStage(null));
		var transaction = this.connection.getTransaction();

		this.connection.setAutoCommit(autoCommit);

		assertThat(Neo4jTransaction.State.COMMITTED.equals(transaction.getState())).isTrue();
		then(boltConnection).should()
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false);
		then(boltConnection).should().commit();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldNotCommitTransactionOnSameAutoCommit(boolean autoCommit) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);
		this.connection.setAutoCommit(autoCommit);
		var transactionType = this.connection.getAutoCommit() ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false))
			.willReturn(CompletableFuture.completedStage(null));
		var transaction = this.connection.getTransaction();

		this.connection.setAutoCommit(autoCommit);

		assertThat(Neo4jTransaction.State.NEW.equals(transaction.getState())).isTrue();
		then(boltConnection).should()
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, transactionType, false);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldThrowOnManagingAutoCommitTransaction(boolean rollback) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.UNCONSTRAINED,
				false))
			.willReturn(CompletableFuture.completedStage(null));
		var transaction = this.connection.getTransaction();

		ConnectionMethodRunner methodRunner = rollback ? Connection::rollback : Connection::commit;

		assertThatThrownBy(() -> methodRunner.run(this.connection)).isExactlyInstanceOf(SQLException.class);

		assertThat(Neo4jTransaction.State.NEW.equals(transaction.getState())).isTrue();
		then(boltConnection).should()
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.UNCONSTRAINED, false);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldManageTransaction(boolean rollback) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);
		this.connection.setAutoCommit(false);
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.DEFAULT, false))
			.willReturn(CompletableFuture.completedStage(null));
		if (rollback) {
			given(boltConnection.rollback()).willReturn(CompletableFuture.completedStage(null));
		}
		else {
			given(boltConnection.commit()).willReturn(CompletableFuture.completedStage(null));
		}
		var transaction = this.connection.getTransaction();

		if (rollback) {
			this.connection.rollback();
		}
		else {
			this.connection.commit();
		}

		assertThat(transaction.getState()
			.equals(rollback ? Neo4jTransaction.State.ROLLEDBACK : Neo4jTransaction.State.COMMITTED)).isTrue();
		then(boltConnection).should()
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.DEFAULT, false);
		then(boltConnection).should(times(rollback ? 1 : 0)).rollback();
		then(boltConnection).should(times(rollback ? 0 : 1)).commit();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldNotBeClosedByDefault() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);

		assertThat(this.connection.isClosed()).isFalse();
	}

	@Test
	void shouldClose() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		this.connection = new ConnectionImpl(boltConnection);

		this.connection.close();

		assertThat(this.connection.isClosed()).isTrue();
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollbackOnClose() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.UNCONSTRAINED,
				false))
			.willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.rollback()).willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		this.connection = new ConnectionImpl(boltConnection);
		var transaction = this.connection.getTransaction();

		this.connection.close();

		assertThat(this.connection.isClosed()).isTrue();
		assertThat(Neo4jTransaction.State.ROLLEDBACK.equals(transaction.getState())).isTrue();
		then(boltConnection).should()
			.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.UNCONSTRAINED, false);
		then(boltConnection).should().rollback();
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldHaveNonReadOnlyByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.isReadOnly()).isFalse();
	}

	@Test
	void shouldUpdateReadOnly() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		this.connection.setReadOnly(true);

		assertThat(this.connection.isReadOnly()).isTrue();
	}

	@Test
	void shouldThrowOnUpdatingReadOnlyDuringTransaction() throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE, TransactionType.UNCONSTRAINED,
				false))
			.willReturn(CompletableFuture.completedStage(null));
		this.connection = new ConnectionImpl(boltConnection);
		var transaction = this.connection.getTransaction();

		assertThatThrownBy(() -> this.connection.setReadOnly(true)).isExactlyInstanceOf(SQLException.class);
	}

	@Test
	void shouldNotHaveCatalogByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getCatalog()).isNull();
	}

	@Test
	void shouldIgnoreUpdatingCatalog() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		this.connection.setCatalog("ignored");

		assertThat(this.connection.getCatalog()).isNull();
	}

	@Test
	void shouldHaveTransactionIsolationByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
	}

	@Test
	void shouldNotHaveWarningsByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat((Object) this.connection.getWarnings()).isNull();
	}

	@Test
	void shouldClearWarnings() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class), new SQLWarning());

		this.connection.clearWarnings();

		assertThat((Object) this.connection.getWarnings()).isNull();
	}

	@Test
	void shouldPrepareStatementWithResultSetTypeAndConcurrency() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		var statement = this.connection.prepareStatement("sql", ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);

		assertThat(statement).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(ints = { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE })
	void shouldThrowOnPreparingStatementWithUnsupportedResultSetType(int type) {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThatThrownBy(() -> this.connection.prepareStatement("sql", type, ResultSet.CONCUR_READ_ONLY))
			.isExactlyInstanceOf(SQLException.class);
	}

	@Test
	void shouldThrowOnPreparingStatementWithUnsupportedResultSetConcurrency() {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThatThrownBy(
				() -> this.connection.prepareStatement("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE))
			.isExactlyInstanceOf(SQLException.class);
	}

	@Test
	void shouldHaveDefaultHoldability() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Test
	void shouldThrowOnValidatingWithNegativeTimeout() {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThatThrownBy(() -> this.connection.isValid(-1)).isExactlyInstanceOf(SQLException.class);
	}

	@ParameterizedTest
	@MethodSource("getSendResetOnValidatingWithoutActiveTransactionArgs")
	void shouldSendResetOnValidatingWithoutActiveTransaction(boolean setupClosedTransaction, boolean expectedValid)
			throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		this.connection = new ConnectionImpl(boltConnection);
		if (setupClosedTransaction) {
			given(boltConnection.beginTransaction(Collections.emptySet(), AccessMode.WRITE,
					TransactionType.UNCONSTRAINED, false))
				.willReturn(CompletableFuture.completedStage(null));
			given(boltConnection.commit()).willReturn(CompletableFuture.completedStage(null));
			var transaction = this.connection.getTransaction();
			transaction.commit();
		}
		given(boltConnection.reset(true)).willReturn(expectedValid ? CompletableFuture.completedStage(null)
				: CompletableFuture.failedFuture(new BoltException("ignored")));

		var valid = this.connection.isValid(0);

		assertThat(valid).isEqualTo(expectedValid);
		then(boltConnection).should().reset(true);
	}

	static Stream<Arguments> getSendResetOnValidatingWithoutActiveTransactionArgs() {
		return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true),
				Arguments.of(false, false));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldAddWarningOnUnknownClientInfoProperty(boolean setupExistingWarging) throws SQLException {
		var existingWarning = setupExistingWarging ? new SQLWarning() : null;
		this.connection = new ConnectionImpl(mock(BoltConnection.class), existingWarning);
		var name = "something";

		this.connection.setClientInfo(name, null);
		var warning = this.connection.getWarnings();
		if (setupExistingWarging) {
			warning = warning.getNextWarning();
		}

		assertThat((Object) warning).isNotNull();
		assertThat((Object) warning.getCause()).isExactlyInstanceOf(SQLClientInfoException.class);
		var infoException = (SQLClientInfoException) warning.getCause();
		assertThat(infoException.getFailedProperties().size()).isEqualTo(1);
		assertThat(infoException.getFailedProperties().get(name)).isEqualTo(ClientInfoStatus.REASON_UNKNOWN_PROPERTY);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldAddWarningOnUnknownClientInfoProperties(boolean setupExistingWarging) throws SQLException {
		var existingWarning = setupExistingWarging ? new SQLWarning() : null;
		this.connection = new ConnectionImpl(mock(BoltConnection.class), existingWarning);
		var properties = new Properties();
		properties.put("property1", "value1");
		properties.put("property2", "value2");

		this.connection.setClientInfo(properties);
		var warning = this.connection.getWarnings();
		if (setupExistingWarging) {
			warning = warning.getNextWarning();
		}

		assertThat((Object) warning).isNotNull();
		assertThat((Object) warning.getCause()).isExactlyInstanceOf(SQLClientInfoException.class);
		var infoException = (SQLClientInfoException) warning.getCause();
		assertThat(infoException.getFailedProperties().size()).isEqualTo(properties.size());
		properties.keySet()
			.stream()
			.map(key -> (String) key)
			.forEach(key -> assertThat(infoException.getFailedProperties().get(key))
				.isEqualTo(ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
	}

	@Test
	void shouldReturnNullClientInfo() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getClientInfo("name")).isNull();
	}

	@Test
	void shouldReturnEmptyClientInfoProperties() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getClientInfo().isEmpty()).isTrue();
	}

	@Test
	void shouldHaveSchemaByDefault() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		assertThat(this.connection.getSchema()).isEqualTo("public");
	}

	@Test
	void shouldIgnoreUpdatingScheme() throws SQLException {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		this.connection.setSchema("name");

		assertThat(this.connection.getSchema()).isEqualTo("public");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCloseOnAbort(boolean transactionPresent) throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		this.connection = new ConnectionImpl(boltConnection);
		Neo4jTransaction transaction = null;
		if (transactionPresent) {
			given(boltConnection.beginTransaction(any(), any(), any(), anyBoolean()))
				.willReturn(CompletableFuture.completedStage(null));
			transaction = this.connection.getTransaction();
		}

		this.connection.abort(Executors.newSingleThreadExecutor());

		assertThat(this.connection.isClosed()).isTrue();
		if (transactionPresent) {
			assertThat(transaction).isNotNull();
			assertThat(transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
			then(boltConnection).should().beginTransaction(any(), any(), any(), anyBoolean());
		}
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getThrowingMethodExecutorsWhenClosed")
	void shouldThrowWhenClosed(ConnectionMethodRunner consumer, Class<? extends SQLException> exceptionType)
			throws SQLException {
		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		this.connection = new ConnectionImpl(boltConnection);
		this.connection.close();
		assertThat(this.connection.isClosed()).isTrue();
		assertThatThrownBy(() -> consumer.run(this.connection)).isExactlyInstanceOf(exceptionType);
	}

	static Stream<Arguments> getThrowingMethodExecutorsWhenClosed() {
		return Stream.of(Arguments.of((ConnectionMethodRunner) Connection::createStatement, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.nativeSQL("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setAutoCommit(false),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getAutoCommit, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::commit, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::rollback, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getMetaData, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setReadOnly(false), SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::isReadOnly, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setCatalog("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getCatalog, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getTransactionIsolation, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getWarnings, SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::clearWarnings, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY), SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getHoldability, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setClientInfo("ignored", "ignored"),
						SQLClientInfoException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setClientInfo(new Properties()),
						SQLClientInfoException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.getClientInfo("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getClientInfo, SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setSchema("ignored"),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getSchema, SQLException.class));
	}

	@ParameterizedTest
	@MethodSource("getUnsupportedMethodExecutors")
	void shouldThrowUnsupported(ConnectionMethodRunner consumer, Class<? extends SQLException> exceptionType) {
		this.connection = new ConnectionImpl(mock(BoltConnection.class));
		assertThatThrownBy(() -> consumer.run(this.connection)).isExactlyInstanceOf(exceptionType);
	}

	static Stream<Arguments> getUnsupportedMethodExecutors() {
		return Stream.of(
				Arguments.of((ConnectionMethodRunner) connection -> connection
					.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("RETURN pi()",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE), SQLException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getTypeMap, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setTypeMap(Collections.emptyMap()),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection
					.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT), SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::setSavepoint, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setSavepoint("ignored"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.rollback(mock(Savepoint.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.releaseSavepoint(mock(Savepoint.class)),
						SQLFeatureNotSupportedException.class),
				Arguments
					.of((ConnectionMethodRunner) connection -> connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
							ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT), SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT), SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT), SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						Statement.NO_GENERATED_KEYS), SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored", new int[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection.prepareStatement("ignored", new String[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createClob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createBlob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createNClob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createSQLXML, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.createArrayOf("ignored", new String[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.createStruct("ignored", new Object[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection.setNetworkTimeout(mock(Executor.class), 1),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::getNetworkTimeout,
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setShardingKeyIfValid(null, null, 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setShardingKeyIfValid(null, 0),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setShardingKey(null, null),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setShardingKey(null),
						SQLFeatureNotSupportedException.class),
				// other
				Arguments.of((ConnectionMethodRunner) connection -> connection
					.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED), SQLException.class));
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		// when & then
		if (shouldUnwrap) {
			var unwrapped = this.connection.unwrap(cls);
			assertThat(unwrapped).isInstanceOf(cls);
		}
		else {
			assertThatThrownBy(() -> this.connection.unwrap(cls)).isInstanceOf(SQLException.class);
		}
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldHandleIsWrapperFor(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		this.connection = new ConnectionImpl(mock(BoltConnection.class));

		// when
		var wrapperFor = this.connection.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(ConnectionImpl.class, true), Arguments.of(Connection.class, true),
				Arguments.of(Neo4jConnection.class, true), Arguments.of(Wrapper.class, true),
				Arguments.of(AutoCloseable.class, true), Arguments.of(Object.class, true),
				Arguments.of(Statement.class, false));
	}

	@FunctionalInterface
	private interface ConnectionMethodRunner {

		void run(Connection connection) throws SQLException;

	}

}
