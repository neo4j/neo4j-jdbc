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

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMetadataKeyValidatingTests {

	static DatabaseMetadataImpl newDatabaseMetadata() {
		var boltConnection = Mockito.mock(BoltConnection.class);

		return new DatabaseMetadataImpl(boltConnection, false);
	}

	@Test
	void getExportedKeysColumnsShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
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
	void getFunctionColumnsShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getFunctionColumns(null, "public", "someNameDoesNotMatter",
				"SomeColumnNameDoesNotMatter")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(17);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("FUNCTION_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("FUNCTION_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("FUNCTION_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("COLUMN_NAME");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("COLUMN_TYPE");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("DATA_TYPE");
			assertThat(rsMetadata.getColumnName(7)).isEqualTo("TYPE_NAME");
			assertThat(rsMetadata.getColumnName(8)).isEqualTo("PRECISION");
			assertThat(rsMetadata.getColumnName(9)).isEqualTo("LENGTH");
			assertThat(rsMetadata.getColumnName(10)).isEqualTo("SCALE");
			assertThat(rsMetadata.getColumnName(11)).isEqualTo("RADIX");
			assertThat(rsMetadata.getColumnName(12)).isEqualTo("NULLABLE");
			assertThat(rsMetadata.getColumnName(13)).isEqualTo("REMARKS");
			assertThat(rsMetadata.getColumnName(14)).isEqualTo("CHAR_OCTET_LENGTH");
			assertThat(rsMetadata.getColumnName(15)).isEqualTo("ORDINAL_POSITION");
			assertThat(rsMetadata.getColumnName(16)).isEqualTo("IS_NULLABLE");
			assertThat(rsMetadata.getColumnName(17)).isEqualTo("SPECIFIC_NAME");
		}
	}

	@Test
	void getFunctionsShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getFunctions(null, "public", "FunctionName")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(5);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("FUNCTION_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("FUNCTION_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("FUNCTION_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("REMARKS");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("FUNCTION_TYPE");
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

	@Test
	void getIndexInfoShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = newDatabaseMetadata();
		try (var expectedKeysRs = databaseMetadata.getIndexInfo(null, "public", "table", false, false)) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(15);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("TABLE_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("TABLE_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("TABLE_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("PKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("NON_UNIQUE");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("INDEX_QUALIFIER");
			assertThat(rsMetadata.getColumnName(7)).isEqualTo("INDEX_NAME");
			assertThat(rsMetadata.getColumnName(8)).isEqualTo("FKCOLUMN_NAME");
			assertThat(rsMetadata.getColumnName(9)).isEqualTo("TYPE");
			assertThat(rsMetadata.getColumnName(10)).isEqualTo("ORDINAL_POSITION");
			assertThat(rsMetadata.getColumnName(11)).isEqualTo("COLUMN_NAME");
			assertThat(rsMetadata.getColumnName(12)).isEqualTo("ASC_OR_DESC");
			assertThat(rsMetadata.getColumnName(13)).isEqualTo("CARDINALITY");
			assertThat(rsMetadata.getColumnName(14)).isEqualTo("PAGES");
			assertThat(rsMetadata.getColumnName(15)).isEqualTo("FILTER_CONDITION");
		}
	}

}
