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
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DatabaseMetadataKeyValidatingTests {

	static DatabaseMetadataImpl newDatabaseMetadata() throws SQLException {
		var connection = mock(Connection.class);
		given(connection.getCatalog()).willReturn("someCatalog");
		return new DatabaseMetadataImpl(connection, (s) -> mock(Neo4jTransaction.class), false, 1000);
	}

	@Test
	void getExportedKeysColumnsShouldMatchTheSpec() throws SQLException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getExportedKeys(null, "public", "someTableDoesNotMatter")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(14);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("PKTABLE_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("PKTABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("PKTABLE_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("PKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("FKTABLE_CAT");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("FKTABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(7)).isEqualTo("FKTABLE_NAME");
			assertThat(rsMetadata.getColumnName(8)).isEqualTo("FKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(9)).isEqualTo("KEY_SEQ");
			assertThat(rsMetadata.getColumnName(10)).isEqualTo("UPDATE_RULE");
			assertThat(rsMetadata.getColumnName(11)).isEqualTo("DELETE_RULE");
			assertThat(rsMetadata.getColumnName(12)).isEqualTo("FK_NAME");
			assertThat(rsMetadata.getColumnName(13)).isEqualTo("PK_NAME");
			assertThat(rsMetadata.getColumnName(14)).isEqualTo("DEFERRABILITY");
		}
	}

	@Test
	void getImportedKeysColumnsShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getImportedKeys(null, "public", "someTableDoesNotMatter")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(14);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("PKTABLE_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("PKTABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("PKTABLE_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("PKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("FKTABLE_CAT");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("FKTABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(7)).isEqualTo("FKTABLE_NAME");
			assertThat(rsMetadata.getColumnName(8)).isEqualTo("FKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(9)).isEqualTo("KEY_SEQ");
			assertThat(rsMetadata.getColumnName(10)).isEqualTo("UPDATE_RULE");
			assertThat(rsMetadata.getColumnName(11)).isEqualTo("DELETE_RULE");
			assertThat(rsMetadata.getColumnName(12)).isEqualTo("FK_NAME");
			assertThat(rsMetadata.getColumnName(13)).isEqualTo("PK_NAME");
			assertThat(rsMetadata.getColumnName(14)).isEqualTo("DEFERRABILITY");
		}
	}

	@Test
	void getSchemasShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getSchemas(null, "public")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(2);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("TABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("TABLE_CATALOG");

		}

		try (var expectedKeysRs = databaseMetadata.getSchemas()) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(2);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("TABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("TABLE_CATALOG");

		}
	}

}
