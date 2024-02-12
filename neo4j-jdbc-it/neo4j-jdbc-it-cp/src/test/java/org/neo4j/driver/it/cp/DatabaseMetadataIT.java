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
package org.neo4j.driver.it.cp;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseMetadataIT extends IntegrationTestBase {

	private Connection connection;

	DatabaseMetadataIT() {
		super("neo4j:5.13.0-enterprise");
	}

	@BeforeAll
	void startDriver() throws SQLException {
		this.connection = getConnection();
	}

	@Test
	void getAllProcedures() throws SQLException {
		try (var results = this.connection.getMetaData().getProcedures(null, null, null)) {
			var resultCount = 0;
			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isNotNull();
				assertThat(results.getString(1)).isNull(); // Catalog
				assertThat(results.getString(2)).isEqualTo("public"); // Schema
				assertThat(results.getInt("PROCEDURE_TYPE")).isEqualTo(DatabaseMetaData.procedureResultUnknown);
			}
			assertThat(resultCount).isGreaterThan(0);
		}
	}

	@Test
	void getAllFunctions() throws SQLException {
		try (var results = this.connection.getMetaData().getFunctions(null, null, null)) {
			var resultCount = 0;
			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isNotNull();
				assertThat(results.getString(1)).isNull(); // Catalog
				assertThat(results.getString(2)).isEqualTo("public"); // Schema
				assertThat(results.getInt("FUNCTION_TYPE")).isEqualTo(DatabaseMetaData.functionResultUnknown);
			}
			assertThat(resultCount).isGreaterThan(0);
		}
	}

	@Test
	void getMetaDataProcedure() throws SQLException {
		var resultCount = 0;
		try (var results = this.connection.getMetaData().getProcedures(null, null, "tx.getMetaData")) {

			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isEqualTo("tx.getMetaData");
			}

			assertThat(resultCount).isEqualTo(1);
		}

		resultCount = 0;
		try (var results = this.connection.getMetaData().getProcedures(null, null, "tx.setMetaData")) {

			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isEqualTo("tx.setMetaData");
			}

			assertThat(resultCount).isEqualTo(1);
		}
	}

	@Test
	void passingAnyCatalogMustError() throws SQLException {
		var metaData = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> metaData.getProcedures("somethingRandom", null, null));
	}

	@Test
	void proceduresShouldBeExecutable() throws SQLException {
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();
	}

	@Test
	void afterDenyingExecutionAllProceduresAreCallableShouldFail() throws SQLException {
		executeQueryWithoutResult("DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS TO admin");
		var executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isFalse();

		executeQueryWithoutResult("REVOKE DENY EXECUTE PROCEDURE tx.getMetaData ON DBMS FROM admin");
		executable = this.connection.getMetaData().allProceduresAreCallable();

		assertThat(executable).isTrue();

	}

	void executeQueryWithoutResult(String query) throws SQLException {
		try (var stmt = this.connection.createStatement(); var result = stmt.executeQuery(query)) {
			result.next();
		}
	}

	@Test
	void getAllCatalogsShouldReturnAnEmptyResultSet() throws SQLException {
		var catalogRs = this.connection.getMetaData().getCatalogs();
		assertThat(catalogRs.next()).isFalse();
	}

	@Test
	void getAllSchemasShouldReturnPublic() throws SQLException {
		var schemasRs = this.connection.getMetaData().getSchemas();

		if (schemasRs.next()) {
			assertThat(schemasRs.getString(1)).isEqualTo("public");
		}
	}

	@Test
	void getAllSchemasAskingForPublicShouldReturnPublic() throws SQLException {
		var schemasRs = this.connection.getMetaData().getSchemas(null, "public");

		if (schemasRs.next()) {
			assertThat(schemasRs.getString(1)).isEqualTo("public");
		}
	}

	@Test
	void getAllSchemasAskingForPublicShouldReturnAnEmptyRs() throws SQLException {
		var schemasRs = this.connection.getMetaData().getSchemas(null, "notPublic");

		assertThat(schemasRs.next()).isFalse();
	}

	@Test
	void testGetUser() throws SQLException {
		var username = this.connection.getMetaData().getUserName();

		assertThat(username).isEqualTo("neo4j");
	}

	@Test
	void testGetDatabaseProductNameShouldReturnNeo4j() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductName();

		assertThat(productName).isEqualTo("Neo4j Kernel-enterprise-5.13.0");
	}

	@Test
	void getDatabaseProductVersionShouldReturnTestContainerVersion() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductVersion();

		assertThat(productName).isEqualTo("5.13.0");
	}

	@Test
	void getAllTablesShouldReturnAllLabelsOnATable() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			this.connection.createStatement().executeQuery("Create (:%s)".formatted(label)).close();
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "", null)) {
			while (labelsRs.next()) {
				var labelName = labelsRs.getString(3);
				assertThat(expectedLabels).contains(labelName);
			}
		}
	}

	@Test
	void getProcedureColumnsShouldWorkForASingleProcedure() throws SQLException {

		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData()
			.getProcedureColumns(null, null, "db.index.fulltext.queryNodes", null)) {
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isEqualTo("db.index.fulltext.queryNodes");
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo("db.index.fulltext.queryNodes");
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.procedureColumnIn);
				assertThat(rs.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.procedureNullableUnknown);
				assertThat(rs.getString("IS_NULLABLE")).isEmpty();
				resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(1, "indexName"), Map.of(2, "queryString"),
				Map.of(3, "options"));
	}

	@Test
	void getFunctionColumnsShouldWorkForASingleFunction() throws SQLException {

		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData().getFunctionColumns(null, null, "atan2", null)) {
			while (rs.next()) {
				assertThat(rs.getString("FUNCTION_NAME")).isEqualTo("atan2");
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo("atan2");
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.procedureColumnIn);
				assertThat(rs.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.procedureNullableUnknown);
				assertThat(rs.getString("IS_NULLABLE")).isEmpty();
				resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(1, "y"), Map.of(2, "x"));
	}

	@Test
	void getProcedureColumnsShouldWorkForASingleProcedureAndASingleCol() throws SQLException {

		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData()
			.getProcedureColumns(null, null, "db.index.fulltext.queryNodes", "options")) {
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isEqualTo("db.index.fulltext.queryNodes");
				resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(3, "options"));
	}

	@Test
	void getFunctionColumnsShouldWorkForASingleProcedureAndASingleCol() throws SQLException {

		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData().getFunctionColumns(null, null, "atan2", "x")) {
			while (rs.next()) {
				assertThat(rs.getString("FUNCTION_NAME")).isEqualTo("atan2");
				resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(2, "x"));
	}

	@Test
	void getProcedureColumnsShouldWork() throws SQLException {

		try (var rs = this.connection.getMetaData().getProcedureColumns(null, null, null, null)) {
			int cnt = 0;
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isNotNull();
				assertThat(rs.getString("COLUMN_NAME")).isNotNull();
				assertThat(rs.getObject("ORDINAL_POSITION")).isNotNull();
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo(rs.getString("PROCEDURE_NAME"));
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.procedureColumnIn);
				++cnt;
			}
			assertThat(cnt).isGreaterThan(0);
		}
	}

	@Test
	void getFunctionColumnsShouldWork() throws SQLException {

		try (var rs = this.connection.getMetaData().getFunctionColumns(null, null, null, null)) {
			int cnt = 0;
			while (rs.next()) {
				assertThat(rs.getString("FUNCTION_NAME")).isNotNull();
				assertThat(rs.getString("COLUMN_NAME")).isNotNull();
				assertThat(rs.getObject("ORDINAL_POSITION")).isNotNull();
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo(rs.getString("FUNCTION_NAME"));
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.functionColumnIn);
				++cnt;
			}
			assertThat(cnt).isGreaterThan(0);
		}
	}

	@Test
	void getAllTablesShouldReturnEmptyForCatalogAndSchema() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			executeQueryWithoutResult("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "", null)) {
			while (labelsRs.next()) {
				var catalog = labelsRs.getString(1);
				assertThat(catalog).isEqualTo("");
			}
		}
	}

	@Test
	void getAllTablesShouldReturnPublicForSchema() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			executeQueryWithoutResult("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, null, null)) {
			while (labelsRs.next()) {
				var schema = labelsRs.getString(2);
				assertThat(schema).isEqualTo("public");
			}
		}
	}

	@Test
	void getAllTablesShouldReturnPublicForSchemaIfPassingPublicForSchema() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			executeQueryWithoutResult("Create (:%s)".formatted(label));
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, "public", "", null)) {
			while (labelsRs.next()) {
				var schema = labelsRs.getString(2);
				assertThat(schema).isEqualTo("public");
			}
		}
	}

	@Test
	void getAllTablesShouldOnlyReturnSpecifiedTables() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			this.connection.createStatement().executeQuery("Create (:%s)".formatted(label)).close();
		}

		try (var labelsRs = this.connection.getMetaData().getTables(null, null, expectedLabels.get(0), null)) {
			assertThat(labelsRs.next()).isTrue();
			var catalog = labelsRs.getString(3);
			assertThat(catalog).isEqualTo(expectedLabels.get(0));
			assertThat(labelsRs.next()).isFalse();
		}
	}

	@Test
	void getAllTablesShouldErrorIfYouPassAnythingToCatalog() throws SQLException {
		var getMetadata = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> getMetadata.getTables("someRandomGarbage", null, "", new String[0]));
	}

	@Test
	void getAllTablesShouldErrorIfYouPassAnythingButPublicToSchema() throws SQLException {
		var getMetadata = this.connection.getMetaData();
		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> getMetadata.getTables(null, "notPublic", "", new String[0]));
	}

	@Test
	void maxConnectionsShouldWork() throws SQLException {
		var metadata = this.connection.getMetaData();
		assertThat(metadata.getMaxConnections()).isGreaterThan(0);
	}

	@Test
	void getProceduresShouldMatchTheSpec() throws SQLException {
		var databaseMetadata = this.connection.getMetaData();
		try (var expectedKeysRs = databaseMetadata.getProcedures(null, "public", "someProc")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(9);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("PROCEDURE_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("PROCEDURE_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("PROCEDURE_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("reserved_1");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("reserved_2");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("reserved_3");
			assertThat(rsMetadata.getColumnName(7)).isEqualTo("REMARKS");
			assertThat(rsMetadata.getColumnName(8)).isEqualTo("PROCEDURE_TYPE");
			assertThat(rsMetadata.getColumnName(9)).isEqualTo("SPECIFIC_NAME");
			assertThat(expectedKeysRs.next()).isFalse();
		}
	}

	@Test
	void getFunctionsShouldMatchTheSpec() throws SQLException {
		var databaseMetadata = this.connection.getMetaData();
		try (var expectedKeysRs = databaseMetadata.getFunctions(null, "public", "foo")) {
			var rsMetadata = expectedKeysRs.getMetaData();
			assertThat(rsMetadata.getColumnCount()).isEqualTo(6);
			assertThat(rsMetadata.getColumnName(1)).isEqualTo("FUNCTION_CAT");
			assertThat(rsMetadata.getColumnName(2)).isEqualTo("FUNCTION_SCHEM");
			assertThat(rsMetadata.getColumnName(3)).isEqualTo("FUNCTION_NAME");
			assertThat(rsMetadata.getColumnName(4)).isEqualTo("REMARKS");
			assertThat(rsMetadata.getColumnName(5)).isEqualTo("FUNCTION_TYPE");
			assertThat(rsMetadata.getColumnName(6)).isEqualTo("SPECIFIC_NAME");
			assertThat(expectedKeysRs.next()).isFalse();
		}
	}

	@Test
	void getFunctionColumnsShouldMatchTheSpec() throws SQLException, ExecutionException, InterruptedException {
		var databaseMetadata = this.connection.getMetaData();
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

	// GetColumns
	@Test
	void getColumnsTest() throws SQLException {
		executeQueryWithoutResult("Create (:Test1 {name: 'column1'})");
		executeQueryWithoutResult("Create (:Test2 {nameTwo: 'column2'})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, null, null)) {
			var seenTable1 = false;
			var seenTable2 = false;
			while (rs.next()) {
				assertThat(rs.getString("TABLE_NAME")).isNotNull();
				assertThat(rs.getString("TABLE_NAME")).isNotBlank();

				if (rs.getString("TABLE_NAME").equals("Test2")) {
					var schema = rs.getString("TABLE_SCHEM");
					assertThat(schema).isEqualTo("public");

					var columnName = rs.getString("COLUMN_NAME");
					assertThat(columnName).isEqualTo("nameTwo");

					var columnType = rs.getString("TYPE_NAME");
					assertThat(columnType).isEqualTo("STRING");
					seenTable2 = true;
				}
				else if (rs.getString("TABLE_NAME").equals("Test1")) {
					var schema2 = rs.getString("TABLE_SCHEM");
					assertThat(schema2).isEqualTo("public");

					var columnName2 = rs.getString("COLUMN_NAME");
					assertThat(columnName2).isEqualTo("name");

					var sqlType = rs.getInt("DATA_TYPE");
					assertThat(sqlType).isEqualTo(Types.VARCHAR);

					var columnType2 = rs.getString("TYPE_NAME");
					assertThat(columnType2).isEqualTo("STRING");
					seenTable1 = true;
				}
			}

			assertThat(seenTable2).isTrue();
			assertThat(seenTable1).isTrue();
		}
	}

	@Test
	void getColumnsForSingleTableTest() throws SQLException {
		executeQueryWithoutResult("Create (:Test3 {nameToFind: 'test3'})");
		executeQueryWithoutResult("Create (:Test4 {nameTwo: 'column2'})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test3", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("nameToFind");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("STRING");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.VARCHAR);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test3");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithInteger() throws SQLException {
		executeQueryWithoutResult("Create (:Test {name: 3})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("name");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("INTEGER");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.INTEGER);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithFloat() throws SQLException {
		executeQueryWithoutResult("Create (:Test {name: 3.3})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("name");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("FLOAT");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.FLOAT);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithMultipleTypesButOneIsAStringShouldReturnString() throws SQLException {
		executeQueryWithoutResult("Create (:Test4 {nameToFind: 'test4'})");
		executeQueryWithoutResult("Create (:Test4 {nameToFind: 3})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("nameToFind");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("STRING");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.VARCHAR);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test4");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithMultipleTypesShouldAny() throws SQLException {
		executeQueryWithoutResult("Create (:Test4 {nameToFind: date(\"2019-06-01\")})");
		executeQueryWithoutResult("Create (:Test4 {nameToFind: 3})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("nameToFind");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("ANY");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.OTHER);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test4");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithPoint() throws SQLException {
		executeQueryWithoutResult("Create (:Test4 {nameToFind: point({srid:7203, x: 3.0, y: 0.0})})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", null)) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("nameToFind");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("POINT");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.STRUCT);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test4");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithLists() throws SQLException {
		executeQueryWithoutResult("Create (:Test4 {long: [1,2]})");
		executeQueryWithoutResult("Create (:Test4 {double: [1.1,2.1]})");
		executeQueryWithoutResult("Create (:Test4 {string: ['1','2']})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", null)) {
			for (int i = 0; i < 3; i++) {
				assertThat(rs.next()).isTrue();

				var schema = rs.getString("TABLE_SCHEM");
				assertThat(schema).isEqualTo("public");

				var columnName = rs.getString("COLUMN_NAME");
				assertThat(columnName).isIn("long", "double", "string");

				var columnType = rs.getString("TYPE_NAME");
				assertThat(columnType).isEqualTo("LIST");

				var sqlType = rs.getInt("DATA_TYPE");
				assertThat(sqlType).isEqualTo(Types.ARRAY);

				var tableName = rs.getString("TABLE_NAME");
				assertThat(tableName).isEqualTo("Test4");
			}
		}
	}

}
