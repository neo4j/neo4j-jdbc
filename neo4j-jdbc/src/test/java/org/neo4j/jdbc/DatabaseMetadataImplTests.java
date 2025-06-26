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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.BoltConnectionProvider;
import org.neo4j.jdbc.values.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DatabaseMetadataImplTests {

	private BoltConnectionProvider boltConnectionProvider;

	@BeforeEach
	void beforeEach() {
		this.boltConnectionProvider = mock();
		CompletionStage<BoltConnection> mockedFuture = mock();
		CompletableFuture<BoltConnection> boltConnectionCompletableFuture = mock();
		given(boltConnectionCompletableFuture.join()).willReturn(mock());
		given(mockedFuture.toCompletableFuture()).willReturn(boltConnectionCompletableFuture);
		given(this.boltConnectionProvider.connect(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any()))
			.willReturn(mockedFuture);
	}

	@Test
	void parameterResultSetShouldWork() throws SQLException {
		var map = new LinkedHashMap<String, Object>();
		map.put("1", "a");
		map.put("2", "b");
		map.put("3", "c");
		var rs = DatabaseMetadataImpl.resultSetForParameters(mock(ConnectionImpl.class), map);
		var meta = rs.getMetaData();
		assertThat(meta.getColumnCount()).isEqualTo(3);
		for (int i = 0; i < meta.getColumnCount(); ++i) {
			assertThat(meta.getColumnLabel(i + 1)).isEqualTo(Integer.toString(i + 1));
		}
	}

	@Test
	void getProcedureNamesShouldFailIfYouPassSchema() throws SQLException {
		var connection = newConnection();

		var metaData = connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> metaData.getProcedures("NotNull", "NotNull", null));

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> metaData.getProcedures(null, "NotNull", null));
	}

	@Test
	void getDriverName() {
	}

	@Test
	void getDriverVersion() {
	}

	@Test
	void getDriverMajorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(databaseMetadata::getDriverMajorVersion)
			.withMessage("Unsupported or unknown version 'dev'");
	}

	@Test
	void getDriverMinorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(databaseMetadata::getDriverMinorVersion)
			.withMessage("Unsupported or unknown version 'dev'");
	}

	@Test
	void getJDBCMajorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThat(databaseMetadata.getJDBCMajorVersion()).isEqualTo(4);
	}

	@Test
	void getJDBCMinorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThat(databaseMetadata.getJDBCMinorVersion()).isEqualTo(3);
	}

	@Test
	void getTableTypes() throws SQLException {
		var databaseMetadata = newDatabaseMetadata();
		var tableTypes = new ArrayList<String>();
		try (var rs = databaseMetadata.getTableTypes()) {
			while (rs.next()) {
				tableTypes.add(rs.getString("TABLE_TYPE"));
			}
		}
		assertThat(tableTypes).containsExactlyInAnyOrder("CBV", "TABLE", "RELATIONSHIP");
	}

	@Test
	void getPrimaryKeysShouldErrorWhenNonPublicSchemaPassed() {
		var databaseMetadata = newDatabaseMetadata();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> databaseMetadata.getPrimaryKeys(null, "notPublic", "someTableDoesNotMatter"));
	}

	@Test
	void getImportedKeysShouldReturnEmptyResultSet() throws SQLException {
		var databaseMetadata = newDatabaseMetadata();
		try (var tableTypes = databaseMetadata.getImportedKeys(null, "public", "someTableDoesNotMatter")) {
			assertThat(tableTypes.next()).isFalse();
		}
	}

	@Test
	void getImportedKeysShouldErrorWhenNonPublicSchemaPassed() {
		var databaseMetadata = newDatabaseMetadata();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> databaseMetadata.getImportedKeys(null, "notPublic", "someTableDoesNotMatter"));
	}

	@Test
	void getExportedKeysShouldReturnEmptyResultSet() throws SQLException {
		var databaseMetadata = newDatabaseMetadata();
		try (var tableTypes = databaseMetadata.getExportedKeys(null, "public", "someTableDoesNotMatter")) {
			assertThat(tableTypes.next()).isFalse();
		}
	}

	@Test
	void getExportedKeysShouldErrorWhenNonPublicSchemaPassed() {
		var databaseMetadata = newDatabaseMetadata();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> databaseMetadata.getExportedKeys(null, "notPublic", "someTableDoesNotMatter"));
	}

	@Test
	void getFunctionColumnsShouldErrorWhenNonPublicSchemaPassed() {
		var databaseMetadata = newDatabaseMetadata();
		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> databaseMetadata.getFunctionColumns(null,
				"notPublic", "someNameDoesNotMatter", "SomeColumnNameDoesNotMatter"));
	}

	@Test
	void getAllTablesShouldErrorIfYouPassNonPublicSchema() throws SQLException {
		var connection = newConnection();

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getTables(null, "NotNull", null, null));
	}

	@Test
	void getAllTablesShouldErrorIfYouPassCatalog() throws SQLException {
		var connection = newConnection();

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getTables("NotNull", null, null, null))
			.withMessage(
					"general processing exception - Catalog 'NotNull' is not available in this Neo4j instance, please leave blank or specify the current database name");

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getTables("NotNull", "public", null, null))
			.withMessage(
					"general processing exception - Catalog 'NotNull' is not available in this Neo4j instance, please leave blank or specify the current database name");
	}

	@Test
	void getCatalogTermShouldWork() throws SQLException {

		var connection = newConnection();
		assertThat(connection.getMetaData().getCatalogTerm()).isEqualTo("database");
	}

	@Test
	void getCatalogSeparatorShouldWork() throws SQLException {

		var connection = newConnection();
		assertThat(connection.getMetaData().getCatalogSeparator()).isEqualTo(".");
	}

	@Test
	void getTypeInfoShouldWork() throws SQLException {
		var connection = newConnection();
		try (var rs = connection.getMetaData().getTypeInfo()) {
			while (rs.next()) {
				var type = Type.valueOf(rs.getString("TYPE_NAME"));
				assertThat(rs.getInt("DATA_TYPE")).isEqualTo(Neo4jConversions.toSqlType(type));
				var mp = switch (type) {
					case NUMBER, INTEGER -> 19;
					case FLOAT -> 15;
					default -> 0;
				};
				assertThat(rs.getInt("PRECISION")).isEqualTo(mp);
				assertThat(rs.getString("LITERAL_PREFIX")).isNull();
				assertThat(rs.getString("LITERAL_SUFFIX")).isNull();
				assertThat(rs.getString("CREATE_PARAMS")).isNull();
				assertThat(rs.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.typeNullable);
				assertThat(rs.getBoolean("CASE_SENSITIVE")).isEqualTo(type == Type.STRING);
				var sb = switch (type) {
					case RELATIONSHIP -> DatabaseMetaData.typePredNone;
					case STRING -> DatabaseMetaData.typeSearchable;
					default -> DatabaseMetaData.typePredBasic;
				};
				assertThat(rs.getInt("SEARCHABLE")).isEqualTo(sb);
				assertThat(rs.getBoolean("UNSIGNED_ATTRIBUTE")).isEqualTo(false);
				assertThat(rs.getBoolean("FIXED_PREC_SCALE")).isEqualTo(false);
				assertThat(rs.getBoolean("AUTO_INCREMENT")).isEqualTo(false);
				assertThat(rs.getShort("MINIMUM_SCALE")).isEqualTo((short) 0);
				assertThat(rs.wasNull()).isTrue();
				assertThat(rs.getShort("MAXIMUM_SCALE")).isEqualTo((short) 0);
				assertThat(rs.wasNull()).isTrue();

				assertThat(rs.getInt("SQL_DATA_TYPE")).isZero();
				assertThat(rs.wasNull()).isTrue();
				assertThat(rs.getInt("SQL_DATETIME_SUB")).isZero();
				assertThat(rs.wasNull()).isTrue();

				assertThat(rs.getInt("NUM_PREC_RADIX")).isEqualTo(10);
			}
		}
	}

	@Test
	void getClientInfoPropertiesShouldWork() throws SQLException {
		var connection = newConnection();
		var maxLen = (int) Math.pow(2, 16);
		try (var rs = connection.getMetaData().getClientInfoProperties()) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("NAME")).isEqualTo("ApplicationName");
			assertThat(rs.getInt("MAX_LEN")).isEqualTo(maxLen);
			assertThat(rs.getString("DEFAULT_VALUE")).isNull();
			assertThat(rs.getString("DESCRIPTION")).isNotNull();
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("NAME")).isEqualTo("ClientUser");
			assertThat(rs.getInt("MAX_LEN")).isEqualTo(maxLen);
			assertThat(rs.getString("DESCRIPTION")).isNotNull();
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("NAME")).isEqualTo("ClientHostname");
			assertThat(rs.getInt("MAX_LEN")).isEqualTo(maxLen);
			assertThat(rs.getString("DESCRIPTION")).isNotNull();
			assertThat(rs.next()).isFalse();
		}

		connection.setClientInfo("ApplicationName", "a unit test");
		assertThat(connection.getClientInfo("ApplicationName")).isEqualTo("a unit test");
	}

	private Connection newConnection() throws SQLException {
		var url = "jdbc:neo4j://host";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		return driver.connect(url, props);
	}

	static DatabaseMetadataImpl newDatabaseMetadata() {
		var connection = Mockito.mock(ConnectionImpl.class);
		try {
			given(connection.getTransaction(any())).willReturn(mock(Neo4jTransaction.class));
		}
		catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
		return new DatabaseMetadataImpl(connection, false, 1000, Set.of());
	}

}
