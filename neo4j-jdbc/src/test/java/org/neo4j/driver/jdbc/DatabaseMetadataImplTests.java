/*
 * Copyright (c) 2023 "Neo4j,"
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProvider;

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
		given(this.boltConnectionProvider.connect(any(), any(), any(), any(), any(), any(), anyInt()))
			.willReturn(mockedFuture);
	}

	@Test
	void getProcedureNamesShouldFailIfYouPassSchema() throws SQLException {
		var url = "jdbc:neo4j://host";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		var connection = driver.connect(url, props);

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getProcedures("NotNull", "NotNull", null));

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getProcedures(null, "NotNull", null));
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
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(databaseMetadata::getDriverMajorVersion)
			.withMessage("Unsupported or unknown version 'unknown'");
	}

	@Test
	void getDriverMinorVersion() {
		var databaseMetadata = newDatabaseMetadata();
		Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(databaseMetadata::getDriverMinorVersion)
			.withMessage("Unsupported or unknown version 'unknown'");
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
	void getAllTablesShouldErrorIfYouPassSchema() throws SQLException {
		var url = "jdbc:neo4j://host";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		var connection = driver.connect(url, props);

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getTables("NotNull", "NotNull", null, null));

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> connection.getMetaData().getTables(null, "NotNull", null, null));
	}

	static DatabaseMetadataImpl newDatabaseMetadata() {
		var connection = Mockito.mock(Connection.class);
		return new DatabaseMetadataImpl(connection);
	}

}
