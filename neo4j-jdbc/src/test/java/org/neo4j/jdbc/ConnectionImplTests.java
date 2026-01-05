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
package org.neo4j.jdbc;

import java.net.URI;
import java.sql.ClientInfoStatus;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.neo4j.bolt.connection.AccessMode;
import org.neo4j.bolt.connection.AuthInfo;
import org.neo4j.bolt.connection.AuthToken;
import org.neo4j.bolt.connection.AuthTokens;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.ResponseHandler;
import org.neo4j.bolt.connection.TransactionType;
import org.neo4j.bolt.connection.exception.BoltException;
import org.neo4j.bolt.connection.message.BeginMessage;
import org.neo4j.bolt.connection.message.CommitMessage;
import org.neo4j.bolt.connection.message.Message;
import org.neo4j.bolt.connection.message.ResetMessage;
import org.neo4j.bolt.connection.message.RollbackMessage;
import org.neo4j.bolt.connection.summary.CommitSummary;
import org.neo4j.bolt.connection.summary.ResetSummary;
import org.neo4j.bolt.connection.summary.RollbackSummary;
import org.neo4j.jdbc.ConnectionImpl.TranslatorChain;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;
import org.neo4j.jdbc.translator.spi.Translator;

import static com.github.stefanbirkner.systemlambda.SystemLambda.restoreSystemProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@SuppressWarnings("resource")
class ConnectionImplTests {

	@Test
	void noChainingShouldBeAttemptedWithoutAnyTranslators1() {

		var connection = makeConnection(mock(BoltConnection.class));
		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> connection.nativeSQL("M"))
			.withMessage("general processing exception - No translators available");
	}

	@Test
	void getMetaData() throws SQLException {
		var boltConnection = Mockito.mock(BoltConnection.class);
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		try (var c = makeConnection(boltConnection)) {
			Assertions.assertThat(c.getMetaData()).isNotNull();
		}
		catch (UnsupportedOperationException ex) {
			// ignored
		}
	}

	@Test
	void shouldCreateStatement() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		var statement = connection.createStatement();

		assertThat(statement).isNotNull();
	}

	@Test
	void shouldPrepareStatement() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		var statement = connection.prepareStatement("sql");

		assertThat(statement).isNotNull();
	}

	@Test
	void shouldCallTranslator() throws SQLException {
		var translator = mock(Translator.class);
		var sql = "SQL";
		var expectedNativeSql = "nativeSQL";
		given(translator.translate(eq(sql), any(DatabaseMetaData.class))).willReturn(expectedNativeSql);
		var connection = new ConnectionImpl(URI.create("jdbc:neo4j://localhost"), Authentication::none,
				auth -> mock(BoltConnection.class), () -> List.of(translator), false, true, false, false,
				new NoopBookmarkManagerImpl(), Map.of(), 23, "aBeautifulDatabase", null, List.of());

		var nativeSQL = connection.nativeSQL(sql);

		assertThat(nativeSQL).isEqualTo(expectedNativeSql);
		then(translator).should(times(1)).translate(eq(sql), any(DatabaseMetaData.class));
	}

	@Test
	void shouldBeAutoCommitByDefault() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getAutoCommit()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldSetAutoCommit(boolean autoCommit) throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		connection.setAutoCommit(autoCommit);

		assertThat(connection.getAutoCommit()).isEqualTo(autoCommit);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCommitTransactionOnAutoCommitUpdate(boolean autoCommit) throws SQLException {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);
		connection.setAutoCommit(!autoCommit);
		var transactionType = connection.getAutoCommit() ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;
		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.writeAndFlush(any(), anyList(), any()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});
		var transaction = connection.getTransaction(Map.of());

		connection.setAutoCommit(autoCommit);

		assertThat(Neo4jTransaction.State.COMMITTED.equals(transaction.getState())).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> finishMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).should().writeAndFlush(any(), finishMessagesCaptor.capture(), any());
		var finishMessages = finishMessagesCaptor.getValue();
		assertThat(finishMessages.get(0)).isInstanceOf(CommitMessage.class);
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldNotCommitTransactionOnSameAutoCommit(boolean autoCommit) throws SQLException {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);
		connection.setAutoCommit(autoCommit);
		var transactionType = connection.getAutoCommit() ? TransactionType.UNCONSTRAINED : TransactionType.DEFAULT;

		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		var transaction = connection.getTransaction(Map.of());

		connection.setAutoCommit(autoCommit);

		assertThat(Neo4jTransaction.State.NEW.equals(transaction.getState())).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(transactionType);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldThrowOnManagingAutoCommitTransaction(boolean rollback) throws SQLException {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);

		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		var transaction = connection.getTransaction(Map.of());

		ConnectionMethodRunner methodRunner = rollback ? Connection::rollback : Connection::commit;

		assertThatThrownBy(() -> methodRunner.run(connection)).isExactlyInstanceOf(Neo4jException.class);

		assertThat(Neo4jTransaction.State.NEW.equals(transaction.getState())).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldManageTransaction(boolean rollback) throws SQLException {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);
		connection.setAutoCommit(false);
		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.writeAndFlush(any(), anyList(), any()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				if (rollback) {
					invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
				}
				else {
					invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
				}
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});
		var transaction = connection.getTransaction(Map.of());

		if (rollback) {
			connection.rollback();
		}
		else {
			connection.commit();
		}

		assertThat(transaction.getState()
			.equals(rollback ? Neo4jTransaction.State.ROLLEDBACK : Neo4jTransaction.State.COMMITTED)).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.DEFAULT);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> finishMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), finishMessagesCaptor.capture(), any());
		var finishMessages = finishMessagesCaptor.getValue();
		if (rollback) {
			assertThat(finishMessages.get(0)).isInstanceOf(RollbackMessage.class);
		}
		else {
			assertThat(finishMessages.get(0)).isInstanceOf(CommitMessage.class);
		}
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldNotBeClosedByDefault() {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);

		assertThat(connection.isClosed()).isFalse();
	}

	@Test
	void shouldClose() throws SQLException {
		var boltConnection = mockBoltConnection();
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);

		connection.close();

		assertThat(connection.isClosed()).isTrue();
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldRollbackOnClose() throws SQLException {
		var boltConnection = mockBoltConnection();
		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		given(boltConnection.writeAndFlush(any(), anyList(), any()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				invocation.<ResponseHandler>getArgument(0).onRollbackSummary(mock(RollbackSummary.class));
				invocation.<ResponseHandler>getArgument(0).onComplete();
				return CompletableFuture.completedFuture(null);
			});
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);
		var transaction = connection.getTransaction(Map.of());

		connection.close();

		assertThat(connection.isClosed()).isTrue();
		assertThat(Neo4jTransaction.State.ROLLEDBACK.equals(transaction.getState())).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().authInfo();
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> finishMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().writeAndFlush(any(), finishMessagesCaptor.capture(), any());
		var finishMessages = finishMessagesCaptor.getValue();
		assertThat(finishMessages.get(0)).isInstanceOf(RollbackMessage.class);
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@Test
	void shouldHaveNonReadOnlyByDefault() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.isReadOnly()).isFalse();
	}

	@Test
	void shouldUpdateReadOnly() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		connection.setReadOnly(true);

		assertThat(connection.isReadOnly()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldPassAccessModeToBoltConnection(boolean readOnly) throws SQLException {
		// given
		var boltConnection = mockBoltConnection();
		var accessMode = readOnly ? AccessMode.READ : AccessMode.WRITE;
		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);
		connection.setReadOnly(readOnly);

		// when
		var transaction = connection.getTransaction(Map.of());

		// then
		assertThat(transaction).isNotNull();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(accessMode);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
	}

	@Test
	void shouldThrowOnUpdatingReadOnlyDuringTransaction() throws SQLException {
		var boltConnection = mockBoltConnection();
		given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);
		@SuppressWarnings("unused")
		var transaction = connection.getTransaction(Map.of());

		assertThatThrownBy(() -> connection.setReadOnly(true)).isExactlyInstanceOf(Neo4jException.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
		then(boltConnection).should().write(beginMessagesCaptor.capture());
		var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
		assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
		assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
		assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
		assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
	}

	@Test
	void catalogShouldBeEqualToDatabasename() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getCatalog()).isEqualTo("aBeautifulDatabase");
	}

	@Test
	void changingCatalogIsNotSupported() {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> connection.setCatalog("someCatalog"))
			.withMessage("Changing the catalog is not implemented");
	}

	@Test
	void shouldHaveTransactionIsolationByDefault() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
	}

	@Test
	void shouldNotHaveWarningsByDefault() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat((Object) connection.getWarnings()).isNull();
	}

	@Test
	void shouldClearWarnings() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));
		connection.setClientInfo("", "b");
		assertThat((Object) connection.getWarnings()).isNotNull();

		connection.clearWarnings();

		assertThat((Object) connection.getWarnings()).isNull();
	}

	@Test
	void shouldPrepareStatementWithResultSetTypeAndConcurrency() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		var statement = connection.prepareStatement("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

		assertThat(statement).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(ints = { ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_SCROLL_SENSITIVE })
	void shouldThrowOnPreparingStatementWithUnsupportedResultSetType(int type) {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThatThrownBy(() -> connection.prepareStatement("sql", type, ResultSet.CONCUR_READ_ONLY))
			.isExactlyInstanceOf(SQLFeatureNotSupportedException.class);
	}

	@Test
	void shouldThrowOnPreparingStatementWithUnsupportedResultSetConcurrency() {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThatThrownBy(
				() -> connection.prepareStatement("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE))
			.isExactlyInstanceOf(SQLFeatureNotSupportedException.class);
	}

	@Test
	void shouldHaveDefaultHoldability() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Test
	void shouldThrowOnValidatingWithNegativeTimeout() {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThatThrownBy(() -> connection.isValid(-1)).isExactlyInstanceOf(Neo4jException.class);
	}

	@ParameterizedTest
	@MethodSource("getSendResetOnValidatingWithoutActiveTransactionArgs")
	void shouldSendResetOnValidatingWithoutActiveTransaction(boolean setupClosedTransaction, boolean expectedValid)
			throws SQLException {
		var boltConnection = mockBoltConnection();
		var connection = makeConnection(boltConnection);
		if (setupClosedTransaction) {
			given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
			given(boltConnection.writeAndFlush(any(), anyList(), any()))
				.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
					invocation.<ResponseHandler>getArgument(0).onCommitSummary(mock(CommitSummary.class));
					invocation.<ResponseHandler>getArgument(0).onComplete();
					return CompletableFuture.completedFuture(null);
				});
			var transaction = connection.getTransaction(Map.of());
			transaction.commit();
		}
		given(boltConnection.writeAndFlush(any(), any(ResetMessage.class), any()))
			.willAnswer((Answer<CompletableFuture<Void>>) invocation -> {
				if (expectedValid) {
					invocation.<ResponseHandler>getArgument(0).onResetSummary(mock(ResetSummary.class));
					invocation.<ResponseHandler>getArgument(0).onComplete();
					return CompletableFuture.completedFuture(null);
				}
				else {
					return CompletableFuture.failedFuture(new BoltException("ignored"));
				}
			});

		var valid = connection.isValid(0);

		if (setupClosedTransaction) {
			@SuppressWarnings("unchecked")
			ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
			then(boltConnection).should().write(beginMessagesCaptor.capture());
			var beginMessage = (BeginMessage) beginMessagesCaptor.getValue().get(0);
			assertThat(beginMessage.databaseName().orElse(null)).isEqualTo("aBeautifulDatabase");
			assertThat(beginMessage.accessMode()).isEqualTo(AccessMode.WRITE);
			assertThat(beginMessage.transactionType()).isEqualTo(TransactionType.UNCONSTRAINED);
			assertThat(beginMessage.bookmarks().isEmpty()).isTrue();
		}
		assertThat(valid).isEqualTo(expectedValid);
		then(boltConnection).should().writeAndFlush(any(), any(ResetMessage.class), any());
	}

	private static BoltConnection mockBoltConnection() {

		var boltConnection = mock(BoltConnection.class);
		given(boltConnection.authInfo()).willReturn(CompletableFuture.completedFuture(new AuthInfo() {

			@Override
			public AuthToken authToken() {
				return AuthTokens.none(BoltAdapters.getValueFactory());
			}

			@Override
			public long authAckMillis() {
				return 0;
			}
		}));

		return boltConnection;
	}

	static Stream<Arguments> getSendResetOnValidatingWithoutActiveTransactionArgs() {
		return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true),
				Arguments.of(false, false));
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldAddWarningOnUnknownClientInfoProperty(boolean setupExistingWarning) throws Exception {
		var connection = makeConnection(mock(BoltConnection.class));
		if (setupExistingWarning) {
			var field = ConnectionImpl.class.getDeclaredField("warnings");
			field.setAccessible(true);
			((Consumer<SQLWarning>) field.get(connection)).accept(new SQLWarning());
		}
		var name = "something";

		connection.setClientInfo(name, null);
		var warning = connection.getWarnings();
		if (setupExistingWarning) {
			warning = warning.getNextWarning();
		}

		assertThat((Object) warning).isNotNull();
		assertThat((Object) warning.getCause()).isExactlyInstanceOf(SQLClientInfoException.class);
		var infoException = (SQLClientInfoException) warning.getCause();
		assertThat(infoException.getFailedProperties().size()).isEqualTo(1);
		assertThat(infoException.getFailedProperties().get(name)).isEqualTo(ClientInfoStatus.REASON_UNKNOWN_PROPERTY);
	}

	@SuppressWarnings("unchecked")
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldAddWarningOnUnknownClientInfoProperties(boolean setupExistingWarning) throws Exception {
		var connection = makeConnection(mock(BoltConnection.class));
		if (setupExistingWarning) {
			var field = ConnectionImpl.class.getDeclaredField("warnings");
			field.setAccessible(true);
			((Consumer<SQLWarning>) field.get(connection)).accept(new SQLWarning());
		}
		var properties = new Properties();
		properties.put("property1", "value1");
		properties.put("property2", "value2");

		connection.setClientInfo(properties);
		var warning = connection.getWarnings();
		if (setupExistingWarning) {
			warning = warning.getNextWarning();
		}

		assertThat((Object) warning).isNotNull();
		assertThat((Object) warning.getCause()).isExactlyInstanceOf(SQLClientInfoException.class);
		var infoException = (SQLClientInfoException) warning.getCause();
		assertThat(infoException.getFailedProperties().size()).isEqualTo(properties.size());
		properties.keySet()
			.stream()
			.map(String.class::cast)
			.forEach(key -> assertThat(infoException.getFailedProperties().get(key))
				.isEqualTo(ClientInfoStatus.REASON_UNKNOWN_PROPERTY));
	}

	@Test
	void shouldReturnNullClientInfo() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getClientInfo("name")).isNull();
	}

	@Test
	void shouldReturnEmptyClientInfoProperties() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getClientInfo().isEmpty()).isTrue();
	}

	@Test
	void shouldHaveSchemaByDefault() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		assertThat(connection.getSchema()).isEqualTo("public");
	}

	@Test
	void shouldIgnoreUpdatingScheme() throws SQLException {
		var connection = makeConnection(mock(BoltConnection.class));

		connection.setSchema("name");

		assertThat(connection.getSchema()).isEqualTo("public");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldCloseOnAbort(boolean transactionPresent) throws SQLException {
		var boltConnection = mockBoltConnection();
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);
		Neo4jTransaction transaction = null;
		if (transactionPresent) {
			given(boltConnection.write(anyList())).willReturn(CompletableFuture.completedStage(null));
			transaction = connection.getTransaction(Map.of());
		}

		connection.abort(Executors.newSingleThreadExecutor());

		assertThat(connection.isClosed()).isTrue();
		if (transactionPresent) {
			assertThat(transaction).isNotNull();
			assertThat(transaction.getState()).isEqualTo(Neo4jTransaction.State.FAILED);
			@SuppressWarnings("unchecked")
			ArgumentCaptor<List<Message>> beginMessagesCaptor = ArgumentCaptor.forClass(List.class);
			then(boltConnection).should().write(beginMessagesCaptor.capture());
			assertThat(beginMessagesCaptor.getValue().get(0)).isInstanceOf(BeginMessage.class);
		}
		if (transactionPresent) {
			then(boltConnection).should().authInfo();
		}
		then(boltConnection).should().close();
		then(boltConnection).shouldHaveNoMoreInteractions();
	}

	@ParameterizedTest
	@MethodSource("getThrowingMethodExecutorsWhenClosed")
	void shouldThrowWhenClosed(ConnectionMethodRunner consumer, Class<? extends SQLException> exceptionType)
			throws SQLException {
		var boltConnection = mockBoltConnection();
		given(boltConnection.close()).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);
		connection.close();
		assertThat(connection.isClosed()).isTrue();
		assertThatThrownBy(() -> consumer.run(connection)).isInstanceOf(exceptionType);
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

	@Test
	void getTypeMapShouldAlwaysBeEmpty() {
		var connection = makeConnection(mock(BoltConnection.class));
		assertThat(connection.getTypeMap()).isEqualTo(Map.of());
	}

	@ParameterizedTest
	@MethodSource("getUnsupportedMethodExecutors")
	void shouldThrowUnsupported(ConnectionMethodRunner consumer, Class<? extends SQLException> exceptionType) {
		var connection = makeConnection(mock(BoltConnection.class));
		assertThatThrownBy(() -> consumer.run(connection)).isInstanceOf(exceptionType);
	}

	static Stream<Arguments> getUnsupportedMethodExecutors() {
		return Stream.of(
				Arguments.of((ConnectionMethodRunner) connection -> connection
					.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection.prepareCall("RETURN pi()",
								ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setTypeMap(Map.of("a", Integer.class)),
						SQLException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection
					.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT), SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::setSavepoint, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.setSavepoint("ignored"),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.rollback(mock(Savepoint.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.releaseSavepoint(mock(Savepoint.class)),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT), SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT), SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareCall("ignored",
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.prepareStatement("ignored", new int[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection.prepareStatement("ignored", new String[0]),
						SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createClob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createBlob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createNClob, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) Connection::createSQLXML, SQLFeatureNotSupportedException.class),
				Arguments.of((ConnectionMethodRunner) connection -> connection.createStruct("ignored", new Object[0]),
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
				Arguments.of(
						(ConnectionMethodRunner) connection -> connection
							.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED),
						SQLFeatureNotSupportedException.class));
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldUnwrap(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		var connection = makeConnection(mock(BoltConnection.class));

		// when & then
		if (shouldUnwrap) {
			var unwrapped = connection.unwrap(cls);
			assertThat(unwrapped).isInstanceOf(cls);
		}
		else {
			assertThatThrownBy(() -> connection.unwrap(cls)).isInstanceOf(SQLException.class);
		}
	}

	@ParameterizedTest
	@MethodSource("getUnwrapArgs")
	void shouldHandleIsWrapperFor(Class<?> cls, boolean shouldUnwrap) throws SQLException {
		// given
		var connection = makeConnection(mock(BoltConnection.class));

		// when
		var wrapperFor = connection.isWrapperFor(cls);

		// then
		assertThat(wrapperFor).isEqualTo(shouldUnwrap);
	}

	ConnectionImpl makeConnection(BoltConnection boltConnection) {
		return new ConnectionImpl(URI.create("jdbc:neo4j://localhost"), Authentication::none, auth -> boltConnection,
				List::of, false, false, true, false, new NoopBookmarkManagerImpl(), Map.of(), 23, "aBeautifulDatabase",
				null, List.of());

	}

	private static Stream<Arguments> getUnwrapArgs() {
		return Stream.of(Arguments.of(ConnectionImpl.class, true), Arguments.of(Connection.class, true),
				Arguments.of(Neo4jConnection.class, true), Arguments.of(Wrapper.class, true),
				Arguments.of(AutoCloseable.class, true), Arguments.of(Object.class, true),
				Arguments.of(Statement.class, false));
	}

	@Test
	void shouldHaveZeroNetworkTimeout() {
		// given
		var connection = makeConnection(mock(BoltConnection.class));

		// when & then
		assertThat(connection.getNetworkTimeout()).isEqualTo(0);
	}

	@Test
	void shouldSetNetworkTimeout() throws SQLException {
		// given
		var boltConnection = mockBoltConnection();
		given(boltConnection.setReadTimeout(any())).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);

		// when
		var timeout = 1000;
		connection.setNetworkTimeout(mock(Executor.class), timeout);

		// then
		assertThat(connection.getNetworkTimeout()).isEqualTo(timeout);
	}

	@Test
	// this is not required by JDBC
	void shouldSetNetworkTimeoutWithNullExecutor() throws SQLException {
		// given
		var boltConnection = mockBoltConnection();
		given(boltConnection.setReadTimeout(any())).willReturn(CompletableFuture.completedStage(null));
		var connection = makeConnection(boltConnection);

		// when
		var timeout = 1000;
		connection.setNetworkTimeout(null, timeout);

		// then
		assertThat(connection.getNetworkTimeout()).isEqualTo(timeout);
	}

	@Test
	void shouldNotAcceptNegativeNetworkTimeout() {
		// given
		var connection = makeConnection(mock(BoltConnection.class));

		// when & then
		assertThatThrownBy(() -> connection.setNetworkTimeout(mock(Executor.class), -1))
			.isExactlyInstanceOf(Neo4jException.class);
		assertThat(connection.getNetworkTimeout()).isEqualTo(0);
	}

	@ParameterizedTest
	@ValueSource(strings = { "foo", "n/a" })
	void getAppShouldWork(String applicationName) throws Exception {

		restoreSystemProperties(() -> {
			var connection = makeConnection(mock(BoltConnection.class));

			var expected = "";
			if (!"n/a".equals(applicationName)) {
				connection.setClientInfo("ApplicationName", applicationName);
				expected = applicationName + " ";
			}
			expected += "Java/1.4 (ms fake -) neo4j-jdbc/" + ProductVersion.getValue();

			System.setProperty("java.version", "1.4");
			System.setProperty("java.vm.vendor", "ms");
			System.setProperty("java.vm.name", "fake");
			System.clearProperty("java.vm.version");

			assertThat(connection.getApp()).isEqualTo(expected);
		});
	}

	static Stream<Arguments> shouldEnforceCypher() {
		var mlQuery = """
				/*+ NEO4J FORCE_CYPHER */
				MATCH (:Station { name: 'Denmark Hill' })<-[:CALLS_AT]-(d:Stop)
					((:Stop)-[:NEXT]->(:Stop)){1,3}
					(a:Stop)-[:CALLS_AT]->(:Station { name: 'Clapham Junction' })
				RETURN d.departs AS departureTime, a.arrives AS arrivalTime
				""";

		return Stream.of(Arguments.of("/*+ NEO4J FORCE_CYPHER */ MATCH (n) RETURN n", true),
				Arguments.of("MATCH /*+ NEO4J FORCE_CYPHER */ (n) RETURN n", true),
				Arguments.of("MATCH /*+ NEO4J FORCE_CYPHER */ (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */ MATCH (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n)\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n:'Movie')\nRETURN n", true),
				Arguments.of("/*+ NEO4J FORCE_CYPHER */\nMATCH (n:`Movie`)\nRETURN n", true),
				Arguments.of("MATCH (n:`/*+ NEO4J FORCE_CYPHER */`) RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = '/*+ NEO4J FORCE_CYPHER */' RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = '   /*+ NEO4J FORCE_CYPHER */    ' RETURN n", false),
				Arguments.of("MATCH (n) SET n.f = \"/*+ NEO4J FORCE_CYPHER */\" RETURN n", false),
				Arguments.of(mlQuery, true));
	}

	@ParameterizedTest
	@MethodSource
	void shouldEnforceCypher(String sql, boolean shouldEnforceCypher) {
		assertThat(ConnectionImpl.forceCypher(sql)).isEqualTo(shouldEnforceCypher);
	}

	@FunctionalInterface
	private interface ConnectionMethodRunner {

		void run(Connection connection) throws SQLException;

	}

	static class StaticTranslator implements Translator {

		private final String expected;

		StaticTranslator(String expected) {
			this.expected = expected;
		}

		@Override
		public String translate(String statement, DatabaseMetaData optionalDatabaseMetaData) {
			if (this.expected.equals(statement)) {
				return statement + "_translated";
			}
			throw new IllegalArgumentException(statement + " != " + this.expected);
		}

	}

	@Nested
	class TranslatorChains {

		@Test
		void singleTranslatorShouldBeCalled() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1")), null, warnings::add);
			assertThat(translatorChain.apply("s1")).isEqualTo("s1_translated");
			assertThat(warnings).isEmpty();
		}

		@Test
		void singleTranslatorExceptionShouldBeRethrown() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1")), null, warnings::add);
			assertThatIllegalArgumentException().isThrownBy(() -> translatorChain.apply("x")).withMessage("x != s1");
			assertThat(warnings).isEmpty();
		}

		@Test
		void firstFailsNextDoesNot() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(
					List.of(new StaticTranslator("whatever"), new StaticTranslator("s2")), null, warnings::add);
			assertThat(translatorChain.apply("s2")).isEqualTo("s2_translated");
			assertThat(warnings).hasSize(1)
				.extracting(SQLWarning::getMessage)
				.first()
				.isEqualTo("Translator org.neo4j.jdbc.ConnectionImplTests$StaticTranslator failed to translate `s2`");
		}

		@Test
		void resultsMustBePassedOnFailureInMiddle() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1"),
					new StaticTranslator("whatever"), new StaticTranslator("s1_translated")), null, warnings::add);
			assertThat(translatorChain.apply("s1")).isEqualTo("s1_translated_translated");
			assertThat(warnings).hasSize(1)
				.extracting(SQLWarning::getMessage)
				.first()
				.isEqualTo(
						"Translator org.neo4j.jdbc.ConnectionImplTests$StaticTranslator failed to translate `s1_translated`");
		}

		@Test
		void resultsMustBePassedOn() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1"),
					new StaticTranslator("s1_translated"), new StaticTranslator("s1_translated_translated")), null,
					warnings::add);
			assertThat(translatorChain.apply("s1")).isEqualTo("s1_translated_translated_translated");
			assertThat(warnings).isEmpty();
		}

		@Test
		void allTranslatorsFail() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(
					List.of(new StaticTranslator("a"), new StaticTranslator("b"), new StaticTranslator("c")), null,
					warnings::add);
			assertThatIllegalStateException().isThrownBy(() -> translatorChain.apply("x"))
				.withMessage("No suitable translator for input `x`");
			assertThat(warnings).hasSize(3);
		}

		@Test
		void lastTranslatorFails() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1"),
					new StaticTranslator("s1_translated"), new StaticTranslator("c")), null, warnings::add);
			var translated = translatorChain.apply("s1");
			assertThat(translated).isEqualTo("s1_translated_translated");
			assertThat(warnings).hasSize(1)
				.extracting(SQLWarning::getMessage)
				.first()
				.isEqualTo(
						"Translator org.neo4j.jdbc.ConnectionImplTests$StaticTranslator failed to translate `s1_translated_translated`");
		}

		@Test
		void lastTranslatorDoesNotFinish() {

			var warnings = new ArrayList<SQLWarning>();
			var translatorChain = new TranslatorChain(List.of(new StaticTranslator("s1"),
					new StaticTranslator("s1_translated"), new StaticTranslator("s1_translated_translated")), null,
					warnings::add);
			var translated = translatorChain.apply("s1");
			assertThat(translated).isEqualTo("s1_translated_translated_translated");
			assertThat(warnings).isEmpty();
		}

	}

}
