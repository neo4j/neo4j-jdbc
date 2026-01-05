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
package org.neo4j.jdbc.it.cp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.neo4j.jdbc.Neo4jConnection;
import org.neo4j.jdbc.Neo4jPreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;

@ParameterizedClass
@MethodSource("allProtocols")
abstract class AbstractDatabaseMetadata extends IntegrationTestBase {

	@Parameter
	String protocol = "neo4j";

	protected Connection connection;

	AbstractDatabaseMetadata(boolean enableApoc) {
		super(null, enableApoc, true);
	}

	@Override
	String getConnectionURL() {
		return getConnectionURL("neo4j");
	}

	String getConnectionURL(String database) {
		var neo4j = "neo4j".equals(this.protocol);
		return "jdbc:neo4j%s://%s:%d/%s".formatted(neo4j ? "" : ":" + this.protocol, this.neo4j.getHost(),
				this.neo4j.getMappedPort(neo4j ? 7687 : 7474), database);
	}

	@BeforeAll
	void startDriver() throws SQLException {
		this.connection = getConnection(false, false, "s2c.viewDefinitions",
				Objects.requireNonNull(this.getClass().getResource("/default-views.json")).toString());
	}

	static Stream<Arguments> indexInfo() {
		return Stream.of(
				Arguments.of("Book", true, List.of(new IndexInfo("Book", false, "book_isbn", 3, 1, "isbn", "A"))),
				Arguments.of("Book", false,
						List.of(new IndexInfo("Book", false, "book_isbn", 3, 1, "isbn", "A"),
								new IndexInfo("Book", true, "bookTitles", 3, 1, "title", "A"))),
				Arguments.of(null, true,
						List.of(new IndexInfo("Book", false, "book_isbn", 3, 1, "isbn", "A"),
								new IndexInfo("Actor", false, "actor_fullname", 3, 1, "firstname", "A"),
								new IndexInfo("Actor", false, "actor_fullname", 3, 2, "surname", "A"))),
				Arguments.of(null, false,
						List.of(new IndexInfo("Book", false, "book_isbn", 3, 1, "isbn", "A"),
								new IndexInfo("Book", true, "bookTitles", 3, 1, "title", "A"),
								new IndexInfo("Actor", false, "actor_fullname", 3, 1, "firstname", "A"),
								new IndexInfo("Actor", false, "actor_fullname", 3, 2, "surname", "A"),
								new IndexInfo("Person", true, "node_text_index_nickname", 3, 1, "nickname", "A"))));
	}

	@ParameterizedTest
	@MethodSource
	void indexInfo(String table, boolean unique, List<IndexInfo> expected) throws SQLException {
		try (var stmt = this.connection.createStatement()) {
			stmt.execute("CREATE CONSTRAINT book_isbn IF NOT EXISTS FOR (book:Book) REQUIRE book.isbn IS UNIQUE");
			stmt.execute("CREATE FULLTEXT INDEX bookTitles IF NOT EXISTS FOR (n:Book) ON EACH [n.title]");
			stmt.execute(
					"CREATE CONSTRAINT actor_fullname  IF NOT EXISTS FOR (actor:Actor) REQUIRE (actor.firstname, actor.surname) IS NODE KEY");
			stmt.execute(
					"CREATE CONSTRAINT movie_title  IF NOT EXISTS FOR (movie:Movie) REQUIRE movie.title IS :: STRING");
			stmt.execute(
					"CREATE CONSTRAINT part_of  IF NOT EXISTS FOR ()-[part:PART_OF]-() REQUIRE part.order IS :: INTEGER");
			stmt.execute(
					"CREATE CONSTRAINT wrote_year  IF NOT EXISTS FOR ()-[wrote:WROTE]-() REQUIRE wrote.year IS NOT NULL\n");
			stmt.execute(
					"CREATE FULLTEXT INDEX namesAndTeams   IF NOT EXISTS FOR (n:Employee|Manager) ON EACH [n.name, n.team]");
			stmt.execute("CREATE TEXT INDEX node_text_index_nickname IF NOT EXISTS  FOR (n:Person) ON (n.nickname)");
			stmt.execute("CREATE TEXT INDEX rel_text_index_name IF NOT EXISTS  FOR ()-[r:KNOWS]-() ON (r.interest)");
		}
		var result = new HashSet<IndexInfo>();
		try (var resultset = this.connection.getMetaData().getIndexInfo(null, null, table, unique, false)) {
			while (resultset.next()) {
				result.add(new IndexInfo(resultset));
			}
		}
		assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
	}

	@AfterEach
	void dropConstraintsAndIndexes() throws SQLException {
		dropConstraint0("SHOW CONSTRAINTS YIELD name", "DROP CONSTRAINT $constraint");
		dropConstraint0("SHOW INDEXES YIELD name", "DROP INDEX $constraint");
	}

	private void dropConstraint0(String getConstraintsStatement, String dropConstraintsStatement) throws SQLException {
		this.connection.setAutoCommit(false);
		try (var stmt = this.connection.createStatement();
				var results = stmt.executeQuery(getConstraintsStatement);
				var stmt2 = this.connection.prepareStatement(dropConstraintsStatement)
					.unwrap(Neo4jPreparedStatement.class)) {

			while (results.next()) {
				stmt2.setString("constraint", results.getString("name"));
				stmt2.addBatch();
			}
			stmt2.executeBatch();
		}
		this.connection.setAutoCommit(true);
	}

	@Test
	void getURLShouldWork() throws SQLException {
		assertThat(this.getConnection().getMetaData().getURL()).startsWith(this.getConnectionURL());
	}

	@Test
	void joinTablesWithoutPropertiesOnRel() throws SQLException {

		try (var con = this.getConnection(false, true)) {
			try (var stmt = con.createStatement()) {
				stmt.execute("CREATE (p:Start {col1: 'colSourceValue'})-[:REL]->(:End {col2: 'colTargetValue'})\n");
			}

			var meta = con.getMetaData();
			var columnsRs = meta.getColumns(null, null, "Start_REL_End", null);
			var columns = new HashSet<String>();
			while (columnsRs.next()) {
				columns.add(columnsRs.getString("COLUMN_NAME"));
			}

			assertThat(columns).containsExactlyInAnyOrder("v$start_id", "v$end_id", "col2", "v$id", "col1");
		}
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					CREATE (p:Start {col: 'colSourceValue'})-[:REL {col: 'colRelValue'}]->(:End {col: 'colTargetValue'}) | v$start_id, v$end_id, start_col, col, end_col, v$id | SELECT * FROM Start_REL_End alias WHERE alias.start_col = 'colSourceValue' @ SELECT * FROM Start_REL_End alias WHERE alias.col = 'colRelValue' AND alias.start_col = 'colSourceValue' @ SELECT * FROM Start_REL_End alias WHERE alias.end_col = 'colTargetValue'
					CREATE (p:Start)-[:REL {col: 'colRelValue'}]->(:End {col: 'colTargetValue'})                         | v$start_id, v$end_id, col, end_col, v$id            | SELECT * FROM Start_REL_End alias WHERE alias.end_col = 'colTargetValue'   @ SELECT * FROM Start_REL_End alias WHERE alias.col = 'colRelValue' AND alias.end_col = 'colTargetValue'
					CREATE (p:Start {col: 'colSourceValue'})-[:REL]->(:End {col: 'colTargetValue'})                      | v$start_id, v$end_id, start_col, end_col, v$id      | SELECT * FROM Start_REL_End alias WHERE alias.start_col = 'colSourceValue'
					CREATE (p:Start {col: 'colSourceValue'})-[:REL {col: 'colRelValue'}]->(:End)                         | v$start_id, v$end_id, start_col, col, v$id          | SELECT * FROM Start_REL_End alias WHERE alias.start_col = 'colSourceValue' @ SELECT * FROM Start_REL_End alias WHERE alias.col = 'colRelValue' AND alias.start_col = 'colSourceValue'
					""")
	void joinTablesWithDuplicateColumns(String graph, String expected, String select) throws SQLException {

		try (var con = this.getConnection(false, true)) {
			try (var stmt = con.createStatement()) {
				stmt.execute(graph);
			}

			var meta = con.getMetaData();
			var columnsRs = meta.getColumns(null, null, "Start_REL_End", null);
			var columns = new HashSet<String>();
			while (columnsRs.next()) {
				columns.add(columnsRs.getString("COLUMN_NAME"));
			}

			assertThat(columns).containsExactlyInAnyOrder(expected.split(" ?, ?"));
		}

		var queries = new ArrayList<String>();
		for (var query : select.split("@")) {
			queries.add(query.trim());
			queries.add(query.trim().replaceAll("alias\\.?", ""));
		}

		try (var con = this.getConnection(true, true); var stmt = con.createStatement()) {
			for (var query : queries) {
				var rs = stmt.executeQuery(query);
				assertThat(rs.next()).isTrue();
				assertThat(rs.next()).isFalse();
				rs.close();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "CREATE (p:Start {col: 'colValue'})-[:REL]->(:End)",
			"CREATE (p:Start)-[:REL {col: 'colValue'}]->(:End)", "CREATE (p:Start)-[:REL]->(:End {col: 'colValue'})" })
	void joinTablesWithoutPropertiesOnStartOrEnd(String graph) throws SQLException {

		try (var con = this.getConnection(false, true)) {
			try (var stmt = con.createStatement()) {
				stmt.execute(graph);
			}

			var meta = con.getMetaData();
			var columnsRs = meta.getColumns(null, null, "Start_REL_End", null);
			var columns = new HashSet<String>();
			while (columnsRs.next()) {
				columns.add(columnsRs.getString("COLUMN_NAME"));
			}
			assertThat(columns).containsExactlyInAnyOrder("v$start_id", "v$end_id", "col", "v$id");
		}
	}

	@Test
	void getAllProcedures() throws SQLException {
		try (var results = this.connection.getMetaData().getProcedures(null, null, null)) {
			var resultCount = 0;
			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isNotNull();
				assertThat(results.getString(1)).isEqualTo(((Neo4jConnection) this.connection).getDatabaseName());
				assertThat(results.getString(2)).isEqualTo("public"); // Schema
				assertThat(results.getInt("PROCEDURE_TYPE")).isEqualTo(DatabaseMetaData.procedureResultUnknown);
			}
			assertThat(resultCount).isGreaterThan(0);
		}
	}

	static Stream<Arguments> getReadOnlyShouldWork() {
		return Stream.of(Arguments.of("neo4j", false), Arguments.of("rodb", true));
	}

	@ParameterizedTest
	@MethodSource
	void getReadOnlyShouldWork(String database, boolean expected) throws SQLException {
		var info = new Properties();
		info.put("password", this.neo4j.getAdminPassword());
		try (var readOnlyConnection = DriverManager.getConnection(getConnectionURL(database), info)) {
			assertThat(readOnlyConnection.getMetaData().isReadOnly()).isEqualTo(expected);

			// some smoke test for multi db setup
			try (var statement = readOnlyConnection.createStatement();
					var rs = statement.executeQuery("MATCH (n) RETURN COUNT(*) AS cnt")) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(0);
			}
		}
	}

	@Test
	void getPseudoColumnsShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getPseudoColumns(null, null, null, null)) {
			assertThat(results.getMetaData().getColumnCount()).isEqualTo(12);
			assertThat(results.next()).isFalse();
		}
	}

	@Test
	void getColumnPrivilegesShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getColumnPrivileges(null, null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "GRANTOR", "GRANTEE",
					"PRIVILEGE", "IS_GRANTABLE" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getUDTsShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getUDTs(null, null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "CLASS_NAME", "DATA_TYPE", "REMARKS",
					"BASE_TYPE" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getSuperTypesShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getSuperTypes(null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SUPERTYPE_CAT", "SUPERTYPE_SCHEM",
					"SUPERTYPE_NAME" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getSuperTablesShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getSuperTables(null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "SUPERTABLE_NAME" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getAttributesShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getAttributes(null, null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "ATTR_NAME", "DATA_TYPE",
					"ATTR_TYPE_NAME", "ATTR_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX", "NULLABLE", "REMARKS",
					"ATTR_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB", "CHAR_OCTET_LENGTH", "ORDINAL_POSITION",
					"IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA", "SCOPE_TABLE", "SOURCE_DATA_TYPE" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getSchemasShouldMatchTheSpec() throws SQLException {
		var databaseMetadata = this.connection.getMetaData();
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

	private static void assertEmptyResultSet(int columnCount, String[] names, ResultSet results) throws SQLException {
		assertThat(columnCount).isEqualTo(names.length);
		for (int i = 1; i <= columnCount; ++i) {
			assertThat(results.getMetaData().getColumnName(i)).isEqualTo(names[i - 1]);
		}
		assertThat(results.next()).isFalse();
	}

	@Test
	void getCrossReferenceShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getCrossReference(null, null, null, null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "PKTABLE_CAT", "PKTABLE_SCHEM", "PKTABLE_NAME", "PKCOLUMN_NAME", "FKTABLE_CAT",
					"FKTABLE_SCHEM", "FKTABLE_NAME", "FKCOLUMN_NAME", "KEY_SEQ", "UPDATE_RULE", "DELETE_RULE",
					"FK_NAME", "PK_NAME", "DEFERRABILITY", };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getTablePrivilegesShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getTablePrivileges(null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "GRANTOR", "GRANTEE", "PRIVILEGE",
					"IS_GRANTABLE" };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getBestRowIdentifierShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getBestRowIdentifier(null, null, null, 0, true)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH",
					"DECIMAL_DIGITS", "PSEUDO_COLUMN", };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getVersionColumnsShouldWork() throws SQLException {
		try (var results = this.connection.getMetaData().getVersionColumns(null, null, null)) {
			var columnCount = results.getMetaData().getColumnCount();
			var names = new String[] { "SCOPE", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH",
					"DECIMAL_DIGITS", "PSEUDO_COLUMN", };
			assertEmptyResultSet(columnCount, names, results);
		}
	}

	@Test
	void getAllFunctions() throws SQLException {
		try (var results = this.connection.getMetaData().getFunctions(null, null, null)) {
			var resultCount = 0;
			while (results.next()) {
				resultCount++;
				assertThat(results.getString(3)).isNotNull();
				assertThat(results.getString(1)).isEqualTo(((Neo4jConnection) this.connection).getDatabaseName());
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
		assertThat(catalogRs.next()).isTrue();
		assertThat(catalogRs.getString("TABLE_CAT")).isEqualTo("neo4j");
	}

	@Test
	void getAllSchemasShouldReturnPublic() throws SQLException {
		var schemasRs = this.connection.getMetaData().getSchemas();

		assertThat(schemasRs.next()).isTrue();
		assertThat(schemasRs.getString("TABLE_SCHEM")).isEqualTo("public");
		assertThat(schemasRs.getString("TABLE_CATALOG")).isEqualTo("neo4j");
		assertThat(schemasRs.next()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "%", " %", "% ", "public", "Public ", "%pub%" })
	void getAllSchemasAskingForPublicShouldReturnPublic(String schemaPattern) throws SQLException {
		var schemasRs = this.connection.getMetaData().getSchemas(null, schemaPattern);

		assertThat(schemasRs.next()).isTrue();
		assertThat(schemasRs.getString("TABLE_SCHEM")).isEqualTo("public");
		assertThat(schemasRs.getString("TABLE_CATALOG")).isEqualTo("neo4j");
		assertThat(schemasRs.next()).isFalse();
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
	void getDatabaseProductNameShouldWork() throws SQLException {
		var productName = this.connection.getMetaData().getDatabaseProductName();

		assertThat(productName).startsWith("Neo4j Kernel-enterprise-");
	}

	@Test
	void getDatabaseProductVersionShouldWork() throws SQLException {
		var productVersion = this.connection.getMetaData().getDatabaseProductVersion();

		assertThat(productVersion).isNotNull();
	}

	@Test
	void getAllTablesShouldReturnAllSingleLabelsOnATable() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		for (String label : expectedLabels) {
			this.connection.createStatement().executeQuery("Create (:%s)".formatted(label)).close();
		}

		boolean found = false;
		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "%", null)) {
			while (labelsRs.next()) {
				var tableType = labelsRs.getString("TABLE_TYPE");
				if ("CBV".equals(tableType)) {
					continue;
				}
				var labelName = labelsRs.getString("TABLE_NAME");
				assertThat(expectedLabels).contains(labelName);
				found = true;
			}
		}
		assertThat(found).isTrue();
	}

	@Test
	void getAllTablesShouldReturnAllLabelsOnATable() throws SQLException {
		List<String> expectedLabels = new ArrayList<>();
		expectedLabels.add("TestLabel1");
		expectedLabels.add("TestLabel2");

		this.connection.createStatement()
			.executeQuery("Create (:%s)".formatted(String.join(":", expectedLabels)))
			.close();
		boolean found = false;
		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "%", null)) {
			while (labelsRs.next()) {
				var tableType = labelsRs.getString("TABLE_TYPE");
				if ("CBV".equals(tableType)) {
					continue;
				}
				var labelName = labelsRs.getString("TABLE_NAME");
				assertThat(expectedLabels).contains(labelName);
				found = true;
			}
		}
		assertThat(found).isTrue();
	}

	@Test
	void getProcedureColumnsShouldWorkForASingleProcedure() throws SQLException {

		int cnt = 0;
		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData()
			.getProcedureColumns(null, null, "db.index.fulltext.queryNodes", null)) {
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isEqualTo("db.index.fulltext.queryNodes");
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo("db.index.fulltext.queryNodes");
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(
						(cnt++ < 2) ? DatabaseMetaData.procedureColumnResult : DatabaseMetaData.procedureColumnIn);
				assertThat(rs.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.procedureNullableUnknown);
				assertThat(rs.getString("IS_NULLABLE")).isEmpty();
				resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(1, "node"), Map.of(2, "score"), Map.of(1, "indexName"),
				Map.of(2, "queryString"), Map.of(3, "options"));
	}

	@Test
	void getFunctionColumnsShouldWorkForASingleFunction() throws SQLException {

		List<Map<Integer, String>> resultColumns = new ArrayList<>();
		try (var rs = this.connection.getMetaData().getFunctionColumns(null, null, "atan2", null)) {
			while (rs.next()) {
				var ordinalPosition = rs.getInt("ORDINAL_POSITION");
				assertThat(rs.getString("FUNCTION_NAME")).isEqualTo("atan2");
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo("atan2");
				assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo((ordinalPosition != 0)
						? DatabaseMetaData.functionColumnIn : DatabaseMetaData.functionColumnResult);
				assertThat(rs.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.functionNullableUnknown);
				assertThat(rs.getString("IS_NULLABLE")).isEmpty();
				if (ordinalPosition >= 1) {
					resultColumns.add(Map.of(ordinalPosition, rs.getString("COLUMN_NAME")));
				}
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
				if (rs.getInt("COLUMN_TYPE") == DatabaseMetaData.functionColumnIn) {
					resultColumns.add(Map.of(rs.getInt("ORDINAL_POSITION"), rs.getString("COLUMN_NAME")));
				}
			}
		}
		assertThat(resultColumns).containsExactly(Map.of(2, "x"));
	}

	static Stream<Arguments> getProcedureColumnsShouldWork() {
		return Stream.of(Arguments.of(null, null), Arguments.of("%", "%"), Arguments.of("%", null),
				Arguments.of(null, "%"));
	}

	@ParameterizedTest
	@MethodSource
	void getProcedureColumnsShouldWork(String procedurePattern, String columnPattern) throws SQLException {

		try (var rs = this.connection.getMetaData().getProcedureColumns(null, null, procedurePattern, columnPattern)) {
			int cnt = 0;
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isNotNull();
				assertThat(rs.getString("COLUMN_NAME")).isNotNull();
				assertThat(rs.getObject("ORDINAL_POSITION")).isNotNull();
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo(rs.getString("PROCEDURE_NAME"));
				assertThat(rs.getInt("COLUMN_TYPE")).isIn(DatabaseMetaData.procedureColumnResult,
						DatabaseMetaData.procedureColumnIn);
				++cnt;
			}
			assertThat(cnt).isGreaterThan(0);
		}
	}

	@Test
	void procedureReturns() throws SQLException {
		try (var rs = this.connection.getMetaData()
			.getProcedureColumns(null, null, "dbms.routing.getRoutingTable", null)) {
			int cnt = 0;

			var names = Map.of(0, "ttl", 1, "servers", 2, "context", 3, "database");
			while (rs.next()) {
				assertThat(rs.getString("PROCEDURE_NAME")).isEqualTo("dbms.routing.getRoutingTable");
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo(rs.getString("PROCEDURE_NAME"));
				var name = names.get(cnt);
				assertThat(rs.getString("COLUMN_NAME")).isEqualTo(name);
				if (Set.of("ttl", "servers").contains(name)) {
					assertThat(rs.getInt("ORDINAL_POSITION")).isEqualTo(cnt + 1);
					assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.procedureColumnResult);
				}
				else {
					assertThat(rs.getInt("ORDINAL_POSITION")).isEqualTo(cnt + 1 - 2);
					assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.procedureColumnIn);
				}
				++cnt;
			}
			assertThat(cnt).isEqualTo(4);
		}
	}

	@Test
	void getFunctionColumnsShouldWork() throws SQLException {

		try (var rs = this.connection.getMetaData().getFunctionColumns(null, null, null, null)) {
			int cnt = 0;
			while (rs.next()) {
				assertThat(rs.getString("FUNCTION_NAME")).isNotNull();
				var ordinalPosition = rs.getInt("ORDINAL_POSITION");
				assertThat(rs.wasNull()).isFalse();
				if (ordinalPosition >= 1) {
					assertThat(rs.getString("COLUMN_NAME")).isNotNull();
					assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.functionColumnIn);
				}
				else {
					assertThat(rs.getString("COLUMN_NAME")).isNull();
					assertThat(rs.wasNull()).isTrue();
					assertThat(rs.getInt("COLUMN_TYPE")).isEqualTo(DatabaseMetaData.functionColumnResult);
				}
				assertThat(rs.getString("SPECIFIC_NAME")).isEqualTo(rs.getString("FUNCTION_NAME"));
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
		try (var rs = databaseMetadata.getColumns(null, null, null, "name%")) {
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
		try (var rs = databaseMetadata.getColumns(null, null, "Test3", "name%")) {
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
		try (var rs = databaseMetadata.getColumns(null, null, "Test", "name%")) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("name");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("INTEGER");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.BIGINT);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Test");

			assertThat(rs.next()).isFalse();
		}
	}

	@Test
	void getColumnsWithFloat() throws SQLException {
		executeQueryWithoutResult("Create (:Test {name: 3.3})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test", "name%")) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("name");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("FLOAT");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.DOUBLE);

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
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", "name%")) {
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
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", "name%")) {
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
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", "name%")) {
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
		executeQueryWithoutResult("Create (:Test4 {along: [1,2]})");
		executeQueryWithoutResult("Create (:Test4 {adouble: [1.1,2.1]})");
		executeQueryWithoutResult("Create (:Test4 {astring: ['1','2']})");

		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Test4", "a%")) {
			for (int i = 0; i < 3; i++) {
				assertThat(rs.next()).isTrue();

				var schema = rs.getString("TABLE_SCHEM");
				assertThat(schema).isEqualTo("public");

				var columnName = rs.getString("COLUMN_NAME");
				assertThat(columnName).isIn("along", "adouble", "astring");

				var columnType = rs.getString("TYPE_NAME");
				assertThat(columnType).isEqualTo("LIST");

				var sqlType = rs.getInt("DATA_TYPE");
				assertThat(sqlType).isEqualTo(Types.ARRAY);

				var tableName = rs.getString("TABLE_NAME");
				assertThat(tableName).isEqualTo("Test4");
			}
		}
	}

	@Test
	void getDatabaseVersionShouldBeOK() throws SQLException {

		assertThat(this.connection.getMetaData().getDatabaseProductVersion()).isNotNull();
		assertThat(this.connection.getMetaData().getDatabaseMajorVersion()).isGreaterThanOrEqualTo(5);
		assertThat(this.connection.getMetaData().getDatabaseMajorVersion()).isGreaterThanOrEqualTo(0);
		assertThat(this.connection.getMetaData().getUserName()).isEqualTo("neo4j");
	}

	@Test
	void getTablesWithNull() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("MATCH (n) DETACH DELETE n");
			statement.execute("CREATE (a:A {one:1, two:2})");
			statement.execute("CREATE (b:B {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, null, null);

		assertThat(labels).isNotNull();
		assertThat(labels.next()).isTrue();
		assertThat(labels.getString("TABLE_NAME")).isEqualTo("A");
		assertThat(labels.next()).isTrue();
		assertThat(labels.getString("TABLE_NAME")).isEqualTo("B");
		assertThat(labels.next()).isTrue();
		assertThat(labels.getString("TABLE_NAME")).isEqualTo("cbv1");
		assertThat(labels.next()).isTrue();
		assertThat(labels.getString("TABLE_NAME")).isEqualTo("cbv2");
		assertThat(labels.next()).isFalse();
	}

	@Test
	void getTablesWithoutSamplingShouldWork() throws SQLException {
		var info = new Properties();
		info.put("password", this.neo4j.getAdminPassword());
		try (var connectionWithLowerSampleSize = DriverManager
			.getConnection(getConnectionURL() + "?relationshipSampleSize=-1", info)) {
			var metaData = connectionWithLowerSampleSize.getMetaData();
			assertThatNoException().isThrownBy(() -> metaData.getTables(null, null, null, null));
		}
	}

	@Test
	void onlyMatchingStartAndEndLabelsMustBeConsideredForProperties() throws SQLException {
		var graph = """
					/*+ NEO4J FORCE_CYPHER */
					CREATE (:Person:A {name: 'Christoph Schlingensief', born: localdatetime({year: 1960})})
					CREATE (:Person:B {name: 'Per Berglund', born: 1939})
					CREATE (a1:Person:C {name: 'Alfred Edel', born: "1932"})
					CREATE (a2:Person:D {name: 'Karina Fallenstein', born: 1961})
					CREATE (m:Movie {title: 'Das deutsche Kettensägenmassaker', release:"1990"})
					CREATE (a1)-[:ACTED_IN {role: 'Alfred'}]->(m)
					CREATE (a2)-[:ACTED_IN {role: 'Clara'}]->(m)
					CREATE (a1)-[:ACTED_IN {role: 'Polizist im Gewahrsam'}]->(:Movie {title: 'Der demokratische Terrorist', release:1992})
				""";

		try (var con = getConnection(true, true)) {
			try (var stmt = con.createStatement()) {
				stmt.execute(graph);
			}
			var meta = con.getMetaData();
			try (var rs = meta.getColumns(null, null, "Person_ACTED_IN_Movie", null)) {
				var columnNames = new ArrayList<String>();
				while (rs.next()) {
					columnNames.add(rs.getString("COLUMN_NAME"));
				}
				assertThat(columnNames).containsExactlyInAnyOrder("v$movie_id", "v$person_id", "v$id", "born", "name",
						"release", "title", "role");
			}

			try (var stmt = con.createStatement(); var rs = stmt.executeQuery("SELECT * FROM Person_ACTED_IN_Movie")) {
				var values = new ArrayList<String>();
				while (rs.next()) {
					values.add(rs.getString("born") + "_" + rs.getString("release"));
				}
				assertThat(values).containsExactlyInAnyOrder("1932_1992", "1932_1990", "1961_1990");
			}
		}
	}

	@Test
	void getTablesWithStrictPattern() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Testa {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "Test", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactly("Test");
	}

	@Test
	void getTablesWithStrictPatternShouldFilterCBVs() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Testa {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "cbv1", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			var tableName = labels.getString("TABLE_NAME");
			tableNames.add(tableName);
			if (tableName.startsWith("cbv")) {
				assertThat(labels.getString("TABLE_TYPE")).isEqualTo("CBV");
			}
		}

		assertThat(tableNames).containsExactly("cbv1");
	}

	@Test
	void getTablesWithStrictPatternShouldFilterCBVType() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Testa {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, null, new String[] { "CBV" });

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			var tableName = labels.getString("TABLE_NAME");
			tableNames.add(tableName);
			if (tableName.startsWith("cbv")) {
				assertThat(labels.getString("TABLE_TYPE")).isEqualTo("CBV");
			}
		}

		assertThat(tableNames).containsExactly("cbv1", "cbv2");
	}

	@Test
	void getRelationshipTableShouldWork() throws SQLException {
		try (Statement statement = this.connection.createStatement()) {
			statement.execute("CREATE (:Person {name: 'A'})-[:ACTED_IN {role: 'B'}]->(:Movie {title: 'C'})");
		}

		var resultSet = this.connection.getMetaData().getTables(null, null, null, null);
		assertThat(resultSet).isNotNull();
		var tableNames = new ArrayList<String>();
		while (resultSet.next()) {
			var tableName = resultSet.getString("TABLE_NAME");
			tableNames.add(tableName);
			if ("Person_ACTED_IN_Movie".equals(tableName)) {
				assertThat(resultSet.getString("TABLE_TYPE")).isEqualTo("RELATIONSHIP");
				var info = resultSet.getString("REMARKS");
				assertThat(info).isNotEmpty();
				var labels = info.split("\n");
				assertThat(labels).containsExactly("Person", "ACTED_IN", "Movie");
			}
		}
		resultSet.close();
		assertThat(tableNames).containsExactlyInAnyOrder("Person_ACTED_IN_Movie", "Movie", "Person", "cbv1", "cbv2");

		resultSet = this.connection.getMetaData().getColumns(null, null, "Person_ACTED_IN_Movie", null);
		assertThat(resultSet).isNotNull();
		var columns = new ArrayList<String>();
		while (resultSet.next()) {
			var columnName = resultSet.getString("COLUMN_NAME");
			var scopeTable = resultSet.getString("SCOPE_TABLE");
			columns.add(columnName);
			if ("name".equals(columnName)) {
				assertThat(scopeTable).isEqualTo("Person");
			}
			else if ("title".equals(columnName)) {
				assertThat(scopeTable).isEqualTo("Movie");
			}
			else {
				assertThat(scopeTable).isNull();
			}
		}
		resultSet.close();
		var expectedColumns = new String[] { "role", "v$movie_id", "v$person_id", "v$id", "name", "title" };
		assertThat(columns).containsOnly(expectedColumns);

		try (var con = getConnection(true, true);
				var stmt = con.createStatement();
				var rs = stmt.executeQuery("SELECT * FROM Person_ACTED_IN_Movie")) {
			while (rs.next()) {
				for (var expectedColumn : expectedColumns) {
					assertThat(rs.getObject(expectedColumn)).isNotNull();
				}
			}
		}
	}

	@Test
	void getTablesWithPattern() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Testa {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "Test%", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactlyInAnyOrder("Test", "Testa");
	}

	@Test
	void getTablesWithWildcard() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Foo {one:1, two:2})");
			statement.execute("create (b:Bar {three:3, four:4})");
		}

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "%", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactlyInAnyOrder("Foo", "Bar", "cbv1", "cbv2");
	}

	@Test
	void getColumnWithNull() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:A {one:1, two:2})");
			statement.execute("create (b:B {three:3, four:4})");
		}

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, null, null);

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsOnly("v$id", "one", "two", "three", "four", "a", "b", "c1", "c2");
	}

	@Test
	void getCBVColumnsForTableWildCard() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:A {one:1, two:2})");
			statement.execute("create (b:B {three:3, four:4})");
		}

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, "cbv%", null);

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsOnly("a", "b", "c1", "c2");
	}

	@Test
	void getCBVColumnsForColumnWildCard() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:A {one:1, two:2})");
			statement.execute("create (b:B {three:3, four:4})");
		}

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, null, "c%");

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			var columnName = columns.getString("COLUMN_NAME");
			columnNames.add(columnName);

			switch (columnName) {
				case "c1":
					assertThat(columns.getString("TABLE_NAME")).isEqualTo("cbv1");
					assertThat(columns.getString("TYPE_NAME")).isEqualTo("INTEGER");
					assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BIGINT);
					break;
				case "c2":
					assertThat(columns.getString("TABLE_NAME")).isEqualTo("cbv2");
					assertThat(columns.getString("TYPE_NAME")).isEqualTo("BOOLEAN");
					assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.BOOLEAN);
					break;
				default:
					fail("Unexpected column name " + columnName);
			}
		}

		assertThat(columnNames).containsOnly("c1", "c2");
	}

	@Test
	void getColumnWithTablePattern() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Test2 {three:3, four:4})");
		}

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, "Test", null);

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsExactlyInAnyOrder("v$id", "one", "two");
	}

	@Test
	void getColumnWithColumnPattern() throws SQLException {

		try (Statement statement = this.connection.createStatement()) {
			statement.execute("create (a:Test {one:1, two:2})");
			statement.execute("create (b:Test2 {three:3, four:4})");
		}

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, "Test", "t%");

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsExactly("two");
	}

	@Test
	void classShouldWorkIfTransactionIsAlreadyOpened() throws SQLException {
		try (var localConnection = getConnection()) {
			localConnection.setAutoCommit(false);
			assertThatNoException().isThrownBy(localConnection::getMetaData);
		}
	}

	@Test
	void getSystemFunctions() throws SQLException {
		String systemFunctions = this.connection.getMetaData().getSystemFunctions();

		assertThat(systemFunctions).isNotNull();
		String[] split = systemFunctions.split(",");
		List<String> functionsList = Arrays.asList(split);

		assertThat(functionsList)
			.containsAll(List.of("date", "date.truncate", "time", "time.truncate", "duration", "duration.between"));
	}

	@Test
	void getIndexInfoWithConstraint() throws Exception {
		String constrName = "bar_uuid";

		try (var statement = this.connection.createStatement()) {
			statement
				.execute("CREATE CONSTRAINT " + constrName + " IF NOT EXISTS FOR (f:Bar) REQUIRE (f.uuid) IS UNIQUE");
		}

		try (ResultSet resultSet = this.connection.getMetaData().getIndexInfo(null, null, "Bar", true, false)) {

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString("TABLE_NAME")).isEqualTo("Bar");
			assertThat(resultSet.getBoolean("NON_UNIQUE")).isFalse();
			assertThat(resultSet.getString("INDEX_NAME")).isEqualTo(constrName);
			assertThat(resultSet.getString("INDEX_QUALIFIER")).isEqualTo(constrName);
			assertThat(resultSet.getInt("TYPE")).isEqualTo(3);
			assertThat(resultSet.getInt("ORDINAL_POSITION")).isOne();
			assertThat(resultSet.getString("COLUMN_NAME")).isEqualTo("uuid");
			assertThat(resultSet.getObject("TABLE_CAT"))
				.isEqualTo(((Neo4jConnection) this.connection).getDatabaseName());
			assertThat(resultSet.getObject("TABLE_SCHEM")).isEqualTo("public");
			assertThat(resultSet.getObject("ASC_OR_DESC")).isEqualTo("A");
			assertThat(resultSet.getObject("CARDINALITY")).isNull();
			assertThat(resultSet.getObject("PAGES")).isNull();
			assertThat(resultSet.getObject("FILTER_CONDITION")).isNull();
			assertThat(resultSet.next()).isFalse();
		}
		finally {
			try (var statement = this.connection.createStatement()) {
				statement.execute("DROP CONSTRAINT " + constrName + " IF EXISTS");
			}
		}
	}

	@Test
	void getIndexInfoWithBacktickLabels() throws Exception {
		String constrName = "barExt_uuid";
		try (var statement = this.connection.createStatement()) {
			statement.execute("CREATE CONSTRAINT " + constrName + " FOR (f:`Bar Ext`) REQUIRE (f.uuid) IS UNIQUE");
		}

		try (ResultSet resultSet = this.connection.getMetaData().getIndexInfo(null, null, "Bar Ext", true, false)) {

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString("TABLE_NAME")).isEqualTo("Bar Ext");
			assertThat(resultSet.getBoolean("NON_UNIQUE")).isFalse();
			assertThat(resultSet.getString("INDEX_NAME")).isEqualTo(constrName);
			assertThat(resultSet.getString("INDEX_QUALIFIER")).isEqualTo(constrName);
			assertThat(resultSet.getInt("TYPE")).isEqualTo(3);
			assertThat(resultSet.getInt("ORDINAL_POSITION")).isOne();
			assertThat(resultSet.getString("COLUMN_NAME")).isEqualTo("uuid");
			assertThat(resultSet.getObject("TABLE_CAT"))
				.isEqualTo(((Neo4jConnection) this.connection).getDatabaseName());
			assertThat(resultSet.getObject("TABLE_SCHEM")).isEqualTo("public");
			assertThat(resultSet.getObject("ASC_OR_DESC")).isEqualTo("A");
			assertThat(resultSet.getObject("CARDINALITY")).isNull();
			assertThat(resultSet.getObject("PAGES")).isNull();
			assertThat(resultSet.getObject("FILTER_CONDITION")).isNull();
			assertThat(resultSet.next()).isFalse();
		}
		finally {
			try (var statement = this.connection.createStatement()) {
				statement.execute("DROP CONSTRAINT " + constrName + " IF EXISTS");
			}
		}
	}

	@Test
	void getIndexInfoWithConstraintWrongLabel() throws Exception {
		try (var statement = this.connection.createStatement()) {
			statement.execute("CREATE CONSTRAINT bar_uuid IF NOT EXISTS FOR (f:Bar) REQUIRE (f.uuid) IS UNIQUE");
		}

		try (ResultSet resultSet = this.connection.getMetaData().getIndexInfo(null, null, "Foo", true, false)) {
			assertThat(resultSet.next()).isFalse();
		}
		finally {
			try (var statement = this.connection.createStatement()) {
				statement.execute("DROP CONSTRAINT bar_uuid IF EXISTS");
			}
		}
	}

	@Test
	void precisionShallBeAvailableForSomeNumericProperties() throws SQLException {
		try (var statement = this.connection.createStatement()) {
			statement.execute("CREATE (n:Wurstsalat {d: 42.23, i: 21, s: 'asd'})");
			var meta = this.connection.getMetaData().getColumns(null, null, "Wurstsalat", null);
			while (meta.next()) {
				var columnName = meta.getString("COLUMN_NAME");
				var precision = meta.getInt("COLUMN_SIZE");
				if ("d".equals(columnName)) {
					assertThat(precision).isEqualTo(15);
				}
				else if ("i".equals(columnName)) {
					assertThat(precision).isEqualTo(19);
				}
				else {
					assertThat(precision).isZero();
				}
			}
		}
	}

	@Test
	void shouldBeAbleToMapAllV5CypherTypes() throws Exception {

		var query = """
				CREATE (n:CypherTypes)
				SET
					n.aBoolean = true,
					n.aLong = 9223372036854775807, n.aDouble = 1.7976931348, n.aString = 'Hallo, Cypher',
					n.aByteArray = ?, n.aLocalDate = date('2015-07-21'),
					n.anOffsetTime  = time({ hour:12, minute:31, timezone: '+01:00' }),
					n.aLocalTime = localtime({ hour:12, minute:31, second:14 }),
					n.aZoneDateTime = datetime('2015-07-21T21:40:32-04[America/New_York]'),
					n.aLocalDateTime = localdatetime('2015202T21'), n.anIsoDuration = duration('P14DT16H12M'),
					n.aPoint = point({x:47, y:11})
				""";
		try (var stmt = this.connection.prepareStatement(query)) {
			stmt.setBytes(1, new byte[] { 6 });
			stmt.execute();

		}
		var meta = this.connection.getMetaData();
		int cnt = 0;
		var types = new HashSet<String>();
		try (var results = meta.getColumns(null, null, "CypherTypes", "a%")) {
			while (results.next()) {
				types.add(results.getString("TYPE_NAME"));
				++cnt;
			}
			assertThat(cnt).isEqualTo(12);
			assertThat(types).hasSize(cnt);
			assertThat(types).doesNotContain("OTHER");
		}
	}

	@Test
	void getIndexInfoWithIndex() throws Exception {
		String indexName = "bar_uuid";
		try (var statement = this.connection.createStatement()) {
			statement.execute("CREATE INDEX " + indexName + " IF NOT EXISTS FOR (b:Bar) ON (b.uuid)");
		}

		try (ResultSet resultSet = this.connection.getMetaData().getIndexInfo(null, null, "Bar", false, false)) {

			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString("TABLE_NAME")).isEqualTo("Bar");
			assertThat(resultSet.getBoolean("NON_UNIQUE")).isTrue();
			assertThat(resultSet.getString("INDEX_NAME")).isEqualTo(indexName);
			assertThat(resultSet.getString("INDEX_QUALIFIER")).isEqualTo(indexName);
			assertThat(resultSet.getInt("TYPE")).isEqualTo(3);
			assertThat(resultSet.getInt("ORDINAL_POSITION")).isOne();
			assertThat(resultSet.getString("COLUMN_NAME")).isEqualTo("uuid");
			assertThat(resultSet.getObject("TABLE_CAT"))
				.isEqualTo(((Neo4jConnection) this.connection).getDatabaseName());
			assertThat(resultSet.getObject("TABLE_SCHEM")).isEqualTo("public");
			assertThat(resultSet.getObject("ASC_OR_DESC")).isEqualTo("A");
			assertThat(resultSet.getObject("CARDINALITY")).isNull();
			assertThat(resultSet.getObject("PAGES")).isNull();
			assertThat(resultSet.getObject("FILTER_CONDITION")).isNull();
			assertThat(resultSet.next()).isFalse();
		}
		finally {
			try (var statement = this.connection.createStatement()) {
				statement.execute("DROP INDEX " + indexName + " IF EXISTS");
			}
		}
	}

	@Test
	void catalogEqualsToDatabaseNameIsOk() {
		assertThatNoException().isThrownBy(() -> this.connection.getMetaData().getTables("neo4j", null, null, null));
	}

	@Test
	void primaryKeysWithoutUniqueConstraints() throws SQLException, IOException {

		TestUtils.createMovieGraph(this.connection);

		var primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Movie");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Movie", "v$id", 1, "Movie_elementId");
		assertThat(primaryKeys.next()).isFalse();

		primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Person_ACTED_IN_Movie");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Person_ACTED_IN_Movie", "v$id", 1, "Person_ACTED_IN_Movie_elementId");
		assertThat(primaryKeys.next()).isFalse();
	}

	@Test
	void primaryKeysForNonExistingTable() throws SQLException {
		var primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Foobar");
		assertThat(primaryKeys.next()).isFalse();
	}

	@Test
	void primaryKeysWithUniqueConstraints() throws SQLException, IOException {

		TestUtils.createMovieGraph(this.connection);

		try (var stmt = this.connection.createStatement()) {
			stmt.execute("CREATE CONSTRAINT movie_title FOR (n:Movie) REQUIRE n.title IS UNIQUE");
			stmt.execute("CREATE CONSTRAINT movie_random_col FOR (n:Movie) REQUIRE n.whatever IS UNIQUE");
			stmt.execute("CREATE CONSTRAINT person_id FOR (n:Person) REQUIRE n.id IS UNIQUE");
			stmt.execute(
					"CREATE CONSTRAINT acted_in_id  IF NOT EXISTS FOR ()-[r:ACTED_IN]-() REQUIRE r.engagement_id IS UNIQUE");
		}

		var primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Movie");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Movie", "v$id", 1, "Movie_elementId");
		assertThat(primaryKeys.next()).isFalse();

		primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Person_ACTED_IN_Movie");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Person_ACTED_IN_Movie", "engagement_id", 1, "acted_in_id");
		assertThat(primaryKeys.next()).isFalse();
	}

	private static void assertPrimaryKey(ResultSet primaryKeys, String tableName, String columnName, int seq,
			String name) throws SQLException {
		assertThat(primaryKeys.getString("TABLE_SCHEM")).isEqualTo("public");
		assertThat(primaryKeys.getString("TABLE_CATALOG")).isEqualTo("neo4j");
		assertThat(primaryKeys.getString("TABLE_NAME")).isEqualTo(tableName);
		assertThat(primaryKeys.getString("COLUMN_NAME")).isEqualTo(columnName);
		assertThat(primaryKeys.getInt("KEY_SEQ")).isEqualTo(seq);
		assertThat(primaryKeys.getString("PK_NAME")).isEqualTo(name);
	}

	@Test
	void primaryKeysWithMoreThanOneColumn() throws SQLException, IOException {

		TestUtils.createMovieGraph(this.connection);

		try (var stmt = this.connection.createStatement()) {
			stmt.execute(
					"CREATE CONSTRAINT movie_title_per_year FOR (n:Movie) REQUIRE (n.title, n.released) IS UNIQUE");
		}

		var primaryKeys = this.connection.getMetaData().getPrimaryKeys("neo4j", null, "Movie");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Movie", "title", 1, "movie_title_per_year");
		assertThat(primaryKeys.next()).isTrue();
		assertPrimaryKey(primaryKeys, "Movie", "released", 2, "movie_title_per_year");
		assertThat(primaryKeys.next()).isFalse();
	}

	@Test
	void getCatalogsShouldWork() throws SQLException {

		var catalogs = new ArrayList<String>();
		try (var rs = this.connection.getMetaData().getCatalogs()) {
			while (rs.next()) {
				catalogs.add(rs.getString("TABLE_CAT"));
			}
		}
		assertThat(catalogs).containsExactlyInAnyOrder("neo4j", "system", "rodb");
	}

	abstract boolean apocShouldBeAvailable();

	@Test
	@DisabledInNativeImage
	void apocDetectionShouldWork() throws SQLException {

		var databaseMetadata = this.connection.getMetaData();
		var optionalMethod = ReflectionUtils.findMethod(databaseMetadata.getClass(), "isApocAvailable");
		assertThat(optionalMethod).isPresent();
		var result = (boolean) ReflectionUtils.invokeMethod(optionalMethod.get(), databaseMetadata);
		var expected = apocShouldBeAvailable();
		assertThat(result).isEqualTo(expected);
		try (var stmt = this.connection.createStatement()) {
			stmt.executeUpdate("CREATE (m:Movie)");
		}
	}

	record IndexInfo(String tableName, boolean nonUnique, String indexName, int type, int ordinalPosition,
			String columnName, String ascOrDesc) {
		IndexInfo(ResultSet resultset) throws SQLException {
			this(resultset.getString("TABLE_NAME"), resultset.getBoolean("NON_UNIQUE"),
					resultset.getString("INDEX_NAME"), resultset.getInt("TYPE"), resultset.getInt("ORDINAL_POSITION"),
					resultset.getString("COLUMN_NAME"), resultset.getString("ASC_OR_DESC"));
		}
	}

}
