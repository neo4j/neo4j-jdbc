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

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisabledIfSystemProperty(named = "neo4j.version", matches = "5\\.26\\.\\d+")
@DisabledInNativeImage
class DatabaseMetadataVirtualGraphIT extends AbstractDatabaseMetadata {

	/**
	 * These are static in the virtual graph instance, hence, we need to check them all
	 * the times.
	 */
	private static final Set<String> EXISTING_LABELS = Set.of("EndNP", "Movie", "MovieA", "Person",
			"Person_ACTED_IN_Movie", "Person_DIRECTED_Movie", "Person_PRODUCED_Movie", "Person_WROTE_Movie", "Simple",
			"Simple_REL3_EndNP", "StartNP", "StartNP_REL1_EndNP", "StartNP_REL2_Simple", "TestF", "TestI", "TestP",
			"cbv1", "cbv2");

	DatabaseMetadataVirtualGraphIT() {
		super(false);

		super.doClean = false;
		super.doCreateDatabases = false;

		super.neo4j.withNeo4jConfig("internal.virtual_graph.enabled", "true");
		super.neo4j.withNeo4jConfig("internal.virtual_graph.home", "/var/lib/neo4j/conf/virtual-graph");
		super.neo4j.withNeo4jConfig("internal.metrics.enable", "true");
		super.neo4j.withNeo4jConfig("internal.metrics.export.all", "true");
		super.neo4j.withNeo4jConfig("internal.server.logs.filter.slf4j_class_prefixes",
				"org.eclipse.jetty, com.zaxxer.hikari");
		super.neo4j.withNeo4jConfig("internal.virtual_graph.unique_identifier_validation_mode", "FAIL");
		super.neo4j.withNeo4jConfig("server.metrics.filter", "*virtual_graph*");

		super.resources.put("/var/lib/neo4j/lib", List.of("/virtual-graph/drivers/duckdb_jdbc.jar"));
		super.resources.put("/var/lib/neo4j/conf/virtual-graph",
				List.of("/virtual-graph/config/datasource.json", "/virtual-graph/config/schema.json",
						"/virtual-graph/config/secret.json", "/virtual-graph/config/movies.duckdb"));
	}

	@Override
	boolean apocShouldBeAvailable() {
		return false;
	}

	@Override
	boolean runningAgainstVirtualGraphs() {
		return true;
	}

	@Override
	Collection<String> existingLabels() {
		return EXISTING_LABELS;
	}

	// Adapted for the movie graph (i.e. as the original tests will create data)

	@Test
	@Override
	void getAllTablesShouldReturnPublicForSchema() throws SQLException {
		try (var labelsRs = this.connection.getMetaData().getTables(null, null, null, null)) {
			while (labelsRs.next()) {
				var schema = labelsRs.getString(2);
				assertThat(schema).isEqualTo("public");
			}
		}
	}

	@Test
	@Override
	void getAllTablesShouldReturnPublicForSchemaIfPassingPublicForSchema() throws SQLException {
		try (var labelsRs = this.connection.getMetaData().getTables(null, "public", "", null)) {
			while (labelsRs.next()) {
				var schema = labelsRs.getString(2);
				assertThat(schema).isEqualTo("public");
			}
		}
	}

	@Test
	@Override
	void getAllTablesShouldOnlyReturnSpecifiedTables() throws SQLException {
		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "Movie", null)) {
			assertThat(labelsRs.next()).isTrue();
			var catalog = labelsRs.getString(3);
			assertThat(catalog).isEqualTo("Movie");
			assertThat(labelsRs.next()).isFalse();
		}
	}

	@Test
	@Override
	void getTablesWithNull() throws SQLException {

		var labels = new ArrayList<String>();
		var resultSet = this.connection.getMetaData().getTables(null, null, null, null);
		assertThat(resultSet).isNotNull();
		while (resultSet.next()) {
			labels.add(resultSet.getString("TABLE_NAME"));
		}
		assertThat(labels).containsExactlyInAnyOrderElementsOf(EXISTING_LABELS);
	}

	@Test
	@Override
	void getTablesWithStrictPattern() throws SQLException {

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "Movie", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactly("Movie");
	}

	@Test
	@Override
	void getTablesWithPattern() throws SQLException {

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "Movie%", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactlyInAnyOrder("Movie", "MovieA");
	}

	@Test
	@Override
	void getTablesWithWildcard() throws SQLException {

		ResultSet labels = this.connection.getMetaData().getTables(null, null, "%", null);

		assertThat(labels).isNotNull();
		List<String> tableNames = new ArrayList<>();

		while (labels.next()) {
			tableNames.add(labels.getString("TABLE_NAME"));
		}

		assertThat(tableNames).containsExactlyInAnyOrderElementsOf(EXISTING_LABELS);
	}

	@Test
	@Override
	void getColumnWithNull() throws SQLException {

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, null, null);

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsOnly("v$id", "v$movie_id", "v$person_id", "release_year", "title", "tagline",
				"release_year", "name", "birth_year", "movie_release_year", "movie_title", "movie_tagline",
				"v$endnp_id", "v$startnp_id", "role", "person_name", "person_birth_year", "v$simple_id", "simple_col",
				"a", "b", "c1", "c2", "col", "nameToFind");
	}

	@Test
	@Override
	void getColumnWithTablePattern() throws SQLException {

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, "Movie", null);

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsExactlyInAnyOrder("v$id", "title", "tagline", "release_year");
	}

	@Test
	@Override
	void getColumnWithColumnPattern() throws SQLException {

		ResultSet columns = this.connection.getMetaData().getColumns(null, null, "Movie", "ti%");

		assertThat(columns).isNotNull();
		List<String> columnNames = new ArrayList<>();

		while (columns.next()) {
			columnNames.add(columns.getString("COLUMN_NAME"));
		}

		assertThat(columnNames).containsExactly("title");
	}

	@Test
	@Override
	void joinTablesWithoutPropertiesOnRel() throws SQLException {

		try (var con = this.getConnection(false, true)) {
			var meta = con.getMetaData();
			var columnsRs = meta.getColumns(null, null, "Person_DIRECTED_Movie", null);
			var columns = new HashSet<String>();
			while (columnsRs.next()) {
				columns.add(columnsRs.getString("COLUMN_NAME"));
			}

			assertThat(columns).containsExactlyInAnyOrder("v$person_id", "v$movie_id", "birth_year", "name",
					"release_year", "tagline", "title", "v$id");
		}
	}

	@Test
	@Override
	void getAllTablesShouldReturnAllSingleLabelsOnATable() throws SQLException {

		boolean found = false;
		try (var labelsRs = this.connection.getMetaData().getTables(null, null, "%", null)) {
			while (labelsRs.next()) {
				var tableType = labelsRs.getString("TABLE_TYPE");
				if ("CBV".equals(tableType)) {
					continue;
				}
				var labelName = labelsRs.getString("TABLE_NAME");
				assertThat(EXISTING_LABELS).contains(labelName);
				found = true;
			}
		}
		assertThat(found).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "StartNP_REL1_EndNP", "StartNP_REL2_Simple", "Simple_REL3_EndNP" })
	@Override
	void joinTablesWithoutPropertiesOnStartOrEnd(String tableName) throws SQLException {

		try (var con = this.getConnection(false, true)) {

			var meta = con.getMetaData();
			var columnsRs = meta.getColumns(null, null, tableName, null);
			var columns = new HashSet<String>();
			while (columnsRs.next()) {
				columns.add(columnsRs.getString("COLUMN_NAME"));
			}
			var expected = switch (tableName) {
				case "StartNP_REL1_EndNP" -> List.of("v$startnp_id", "v$endnp_id", "col", "v$id");
				case "StartNP_REL2_Simple" -> List.of("v$startnp_id", "v$simple_id", "col", "v$id");
				case "Simple_REL3_EndNP" -> List.of("v$simple_id", "v$endnp_id", "col", "v$id");
				default -> throw new RuntimeException();
			};

			assertThat(columns).containsExactlyInAnyOrderElementsOf(expected);
		}
	}

	@Test
	@Override
	void getColumnsTest() throws SQLException {
		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, null, "title%")) {
			var tables = new ArrayList<String>();
			while (rs.next()) {
				assertThat(rs.getString("TABLE_NAME")).isNotNull();
				assertThat(rs.getString("TABLE_NAME")).isNotBlank();
				tables.add(rs.getString("TABLE_NAME"));
			}

			assertThat(tables).containsExactlyInAnyOrder("Movie", "MovieA");
		}
	}

	@Test
	@Override
	void getColumnsForSingleTableTest() throws SQLException {
		var databaseMetadata = this.connection.getMetaData();
		try (var rs = databaseMetadata.getColumns(null, null, "Movie", "tag%")) {
			assertThat(rs.next()).isTrue();

			var schema = rs.getString("TABLE_SCHEM");
			assertThat(schema).isEqualTo("public");

			var columnName = rs.getString("COLUMN_NAME");
			assertThat(columnName).isEqualTo("tagline");

			var columnType = rs.getString("TYPE_NAME");
			assertThat(columnType).isEqualTo("STRING");

			var sqlType = rs.getInt("DATA_TYPE");
			assertThat(sqlType).isEqualTo(Types.VARCHAR);

			var tableName = rs.getString("TABLE_NAME");
			assertThat(tableName).isEqualTo("Movie");

			assertThat(rs.next()).isFalse();
		}
	}

	// Additional tests

	@Test
	@Override
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
	@Override
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
	@Override
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
	@Override
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

	@ParameterizedTest
	@MethodSource
	@Override
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
	void allProceduresAreCallable() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			assertThatNoException().isThrownBy(metaData::allProceduresAreCallable);
		}
	}

	@Test
	void getUserName() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			assertThatNoException().isThrownBy(metaData::getUserName);
		}
	}

	@Test
	void getMaxConnections() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			assertThatNoException().isThrownBy(metaData::getMaxConnections);
		}
	}

	@Test
	void getPrimaryKeys() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			try (var rs = metaData.getPrimaryKeys(null, null, null)) {
				assertThat(rs.next()).isFalse();
			}
			try (var rs = metaData.getPrimaryKeys("neo4j", null, "Film")) {
				assertThat(rs.next()).isFalse();
			}
		}
	}

	@Test
	void getCatalogs() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			try (var rs = metaData.getCatalogs()) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getString("TABLE_CAT")).isEqualTo("neo4j");
			}
		}
	}

	@Test
	void isReadOnly() throws SQLException {
		try (var connection = getConnection()) {
			var metaData = connection.getMetaData();
			assertThatNoException().isThrownBy(metaData::isReadOnly);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			SELECT person_name, role, movie_title
			FROM Person_ACTED_IN_Movie
			WHERE person_name LIKE '%arrie-Anne%'
			ORDER BY person_name, role
			""", """
			SELECT
				"v2_people"."name" AS "person_name",
				"person_acted_in_movie"."role" AS "role",
				"v2_movies"."title" AS "movie_title"
			FROM "Person" AS "v2_people"
			JOIN "ACTED_IN" AS "person_acted_in_movie" ON "person_acted_in_movie"."person_id" = "v2_people"."id"
			JOIN "Movie" AS "v2_movies" ON "v2_movies"."id" = "person_acted_in_movie"."movie_id"
			WHERE (contains("v2_people"."name", 'arrie-Anne'))
			ORDER BY "v2_people"."name" ASC NULLS LAST, "person_acted_in_movie"."role" ASC NULLS LAST
			""" })
	void sql2cypherAndBack(String sql) throws SQLException {
		var result = new ArrayList<String>();
		try (var connection = getConnection(true, true, "s2c.sqlDialect", "DUCKDB");
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				result
					.add(rs.getString("person_name") + "-" + rs.getString("role") + "-" + rs.getString("movie_title"));
			}

			assertThat(result).isNotEmpty();
			assertThat(result).containsExactly("Carrie-Anne Moss-Trinity-The Matrix",
					"Carrie-Anne Moss-Trinity-The Matrix Reloaded", "Carrie-Anne Moss-Trinity-The Matrix Revolutions");
		}

	}

	@Test
	void getReadOnlyShouldWork() throws SQLException {
		var info = new Properties();
		info.put("password", this.neo4j.getAdminPassword());
		try (var readOnlyConnection = DriverManager.getConnection(getConnectionURL("neo4j"), info)) {
			assertThat(readOnlyConnection.getMetaData().isReadOnly()).isTrue();
		}
	}

}
