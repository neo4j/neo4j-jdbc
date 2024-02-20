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
package org.neo4j.driver.jdbc.translator.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Treeprocessor;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * @author Michael J. Simons
 * @author Michael Hunger
 */
class SqlToCypherTests {

	private static final SqlTranslator NON_PRETTY_PRINTING_TRANSLATOR = SqlToCypher
		.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

	private static ResultSet makeColumns(String firstName, String... names) throws SQLException {
		var personColumns = mock(ResultSet.class);
		if (firstName == null) {
			given(personColumns.next()).willReturn(false);
			return personColumns;
		}
		Boolean[] results = new Boolean[names.length + 1];
		for (int i = 0; i < results.length; i++) {
			results[i] = i != results.length - 1;
		}
		given(personColumns.next()).willReturn(true, results);
		given(personColumns.getString("COLUMN_NAME")).willReturn(firstName, names);
		return personColumns;
	}

	@Test
	void inClauseWithConstants() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM public.Genre
				WHERE name IN ('action comedy film', 'romcom')
				""")).isEqualTo("MATCH (genre:Genre) WHERE genre.name IN ['action comedy film', 'romcom'] RETURN *");
	}

	@Test
	void inClauseWithConstantsJoin() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM Movie NATURAL JOIN HAS NATURAL JOIN Genre
				WHERE genre.name IN ('action comedy film', 'romcom')
				""")).isEqualTo(
				"MATCH (movie:Movie)-[has:HAS]->(genre:Genre) WHERE genre.name IN ['action comedy film', 'romcom'] RETURN *");
	}

	@Test
	void inClauseWithConstantsJoinWithmeta() throws SQLException {

		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		var resultSet = makeColumns("title");
		given(databaseMetaData.getColumns(null, null, "Movie", null)).willReturn(resultSet);
		resultSet = makeColumns("foobar");
		given(databaseMetaData.getColumns(null, null, "HAS", null)).willReturn(resultSet);
		resultSet = makeColumns("name");
		given(databaseMetaData.getColumns(null, null, "Genre", null)).willReturn(resultSet);

		var renderer = Renderer.getRenderer(Configuration.newConfig()
			.withPrettyPrint(false)
			.alwaysEscapeNames(false)
			.withDialect(Dialect.NEO4J_5)
			.build());

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("""
				SELECT * FROM Movie NATURAL JOIN HAS NATURAL JOIN Genre
				WHERE genre.name IN ('action comedy film', 'romcom')
				""", databaseMetaData)).isEqualTo(renderer.render(CypherParser.parse("""
				MATCH (movie:Movie)-[has:HAS]->(genre:Genre)
				WHERE genre.name IN ['action comedy film', 'romcom']
				RETURN elementId(movie) AS element_id, movie.title AS title,
					elementId(has) AS element_id1, has.foobar AS foobar,
					elementId(genre) AS element_id2, genre.name AS name
				""")));
	}

	@Test
	void plainColumnsEverywhere() throws SQLException {

		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		var resultSet1 = makeColumns("title", "released");
		var resultSet2 = makeColumns("title", "released");
		var resultSet3 = makeColumns("title", "released");
		var resultSet4 = makeColumns("title", "released");
		given(databaseMetaData.getColumns(null, null, "Movie", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3)
			.willReturn(resultSet4);
		resultSet1 = makeColumns(null);
		resultSet2 = makeColumns(null);
		resultSet3 = makeColumns(null);
		given(databaseMetaData.getColumns(null, null, "HAS", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3);
		resultSet1 = makeColumns("name");
		resultSet2 = makeColumns("name");
		resultSet3 = makeColumns("name");
		given(databaseMetaData.getColumns(null, null, "Genre", null)).willReturn(resultSet1)
			.willReturn(resultSet2)
			.willReturn(resultSet3);

		var sql = "SELECT title, released from Movie NATURAL JOIN HAS NATURAL JOIN Genre where name IN ('action film') order by title";
		var expected = "MATCH (movie:Movie)-[has:HAS]->(genre:Genre) WHERE genre.name IN ['action film'] RETURN movie.title, movie.released ORDER BY movie.title";

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(sql, databaseMetaData)).isEqualTo(expected);
	}

	@Test
	void projectingRandomColumnsFromTable() {

		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate("SELECT name, born FROM Person"))
			.isEqualTo("MATCH (person:Person) RETURN person.name, person.born");
	}

	@Test
	void joinsUsing() {

		var in = """
				SELECT p, m FROM Person p
				JOIN Movie m USING (ACTED_IN)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[acted_in:ACTED_IN]->(m:Movie) RETURN p, m");
	}

	@Test
	void naturalJoins() {

		var in = """
				SELECT p, c FROM Person p
				NATURAL JOIN ACTED_IN r
				NATURAL JOIN Movie m
				NATURAL JOIN PLAYED_IN
				NATURAL JOIN Cinema c
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie)-[played_in:PLAYED_IN]->(c:Cinema) RETURN p, c");
	}

	@Test
	void naturalAndUsingJoins() {

		var in = """
				SELECT p, r, m FROM Person p
				NATURAL JOIN ACTED_IN r
				JOIN Movie m USING (MOVIE_ID)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie) RETURN p, r, m");
	}

	@Test
	void naturalAndOnJoins() {

		var in = """
				SELECT p, r, m FROM Person p
				NATURAL JOIN ACTED_IN r
				JOIN Movie m ON (m.id = r.movie_id)
				""";
		assertThat(NON_PRETTY_PRINTING_TRANSLATOR.translate(in))
			.isEqualTo("MATCH (p:Person)-[r:ACTED_IN]->(m:Movie) RETURN p, r, m");
	}

	@Test
	void namedParameterPrefixForParsingShouldBeConfigurable() {
		var translator = SqlToCypher
			.with(SqlToCypherConfig.builder().withParseNamedParamPrefix("$").withPrettyPrint(false).build());
		assertThat(translator.translate("INSERT INTO Movie (Movie.title) VALUES($1)"))
			.isEqualTo("CREATE (movie:`Movie` {title: $1})");
	}

	@Test
	void simpleUpdateShouldWork() {

		var translator = SqlToCypher.defaultTranslator();
		assertThat(translator.translate("UPDATE Actor a SET name = 'Foo' WHERE id(a) = 4711")).isEqualTo("""
				MATCH (a:Actor)
				WHERE id(a) = 4711
				SET a.name = 'Foo'""");
	}

	@Test
	void outerSelectStarShouldBeRemoved() {

		var translator = SqlToCypher.defaultTranslator();
		assertThat(translator
			.translate("SELECT * FROM (SELECT * FROM \"Movie\") AS \"tempTable_5301953691072342668\" WHERE 1 = 0"))
			.isEqualTo("""
					MATCH (movie:Movie)
					RETURN * LIMIT 1""");
	}

	@Test
	void upsert() {

		assertThat(
				NON_PRETTY_PRINTING_TRANSLATOR.translate("INSERT INTO Movie(title) VALUES(?) ON DUPLICATE KEY IGNORE"))
			.isEqualTo("MERGE (movie:Movie {title: $0})");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|", textBlock = """
			SELECT id(n) FROM Movies n|MATCH (n:`Movies`) RETURN id(n)
			SELECT elementId(n) FROM Movies n|MATCH (n:`Movies`) RETURN elementId(n)
			SELECT foobar('const', bazbar(:1))|RETURN foobar('const', bazbar($1))
			""")
	void parserShallNotFailOnUnknownFunctions(String in, String expected) {

		var translator = SqlToCypher.with(SqlToCypherConfig.builder().withPrettyPrint(false).build());
		assertThat(translator.translate(in)).isEqualTo(expected);
	}

	static List<TestData> getTestData(Path path) {
		try (var asciidoctor = Asciidoctor.Factory.create()) {
			var collector = new TestDataExtractor();
			asciidoctor.javaExtensionRegistry().treeprocessor(collector);

			var content = Files.readString(path);
			asciidoctor.load(content, Options.builder().build());
			return collector.testData;
		}
		catch (IOException ioe) {
			throw new RuntimeException("Error reading TCK file " + path, ioe);
		}
	}

	@TestFactory
	Stream<DynamicContainer> tck() {

		var path = ClassLoader.getSystemResource("simple.adoc").getPath();
		var parentFolder = new File(path).getParentFile();
		if (!parentFolder.isDirectory()) {
			return Stream.empty();
		}

		var files = parentFolder.listFiles((f) -> f.getName().toLowerCase().endsWith(".adoc"));
		if (files == null) {
			return Stream.empty();
		}

		return Arrays.stream(files).map(file -> {
			var tests = getTestData(file.toPath()).stream()
				.map(t -> DynamicTest.dynamicTest(
						Optional.ofNullable(t.name).filter(Predicate.not(String::isBlank)).orElse(t.id),
						() -> assertThatSqlIsTranslatedAsExpected(t.sql, t.cypher, t.tableMappings,
								t.joinColumnsMappings, t.prettyPrint, t.databaseMetaData)))
				.toList();
			return DynamicContainer.dynamicContainer(file.getName(), tests);
		});
	}

	void assertThatSqlIsTranslatedAsExpected(String sql, String expected, Map<String, String> tableMappings,
			Map<String, String> join_columns_mappings, boolean prettyPrint, DatabaseMetaData databaseMetaData) {
		assertThat(SqlToCypher
			.with(SqlToCypherConfig.builder()
				.withPrettyPrint(prettyPrint)
				.withTableToLabelMappings(tableMappings)
				.withJoinColumnsToTypeMappings(join_columns_mappings)
				.build())
			.translate(sql, databaseMetaData)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			,,MATCH (movies:movies)$RETURN *
			true,,MATCH (movies:movies)$RETURN *
			false,,MATCH (movies:`movies`) RETURN *
			,true,MATCH (movies:`movies`)$RETURN *
			,false,MATCH (movies:movies)$RETURN *
			true,false,MATCH (movies:movies)$RETURN *
			true,true,MATCH (movies:`movies`)$RETURN *
			false,false,MATCH (movies:movies)$RETURN *
			false,true,MATCH (movies:`movies`)$RETURN *
			""")
	void escapingShouldWork(Boolean prettyPrint, Boolean alwaysEscapeNames, String expected) {
		var properties = new HashMap<String, Object>();
		if (prettyPrint != null) {
			properties.put("s2c.prettyPrint", prettyPrint.toString());
		}
		if (alwaysEscapeNames != null) {
			properties.put("s2c.alwaysEscapeNames", alwaysEscapeNames.toString());
		}
		var cfg = SqlToCypherConfig.of(properties);
		var defaultCfg = SqlToCypherConfig.defaultConfig();
		assertThat(cfg.isPrettyPrint()).isEqualTo((prettyPrint != null) ? prettyPrint : defaultCfg.isPrettyPrint());
		assertThat(cfg.isAlwaysEscapeNames())
			.isEqualTo((alwaysEscapeNames != null) ? alwaysEscapeNames : !cfg.isPrettyPrint());
		var sql = "Select * from movies";
		var cypher = SqlToCypher.with(cfg).translate(sql);
		assertThat(cypher).isEqualTo(expected.replace("$", cfg.isPrettyPrint() ? System.lineSeparator() : " "));
	}

	private static class TestDataExtractor extends Treeprocessor {

		private final List<TestData> testData = new ArrayList<>();

		TestDataExtractor() {
			super(new HashMap<>()); // Must be mutable
		}

		@Override
		public Document process(Document document) {

			var blocks = document.findBy(Map.of("context", ":listing", "style", "source"))
				.stream()
				.map(Block.class::cast)
				.filter((b) -> b.hasAttribute("id"))
				.collect(Collectors.toMap(ContentNode::getId, Function.identity()));

			blocks.values().stream().filter((b) -> "sql".equals(b.getAttribute("language"))).map((sqlBlock) -> {
				var name = (String) sqlBlock.getAttribute("name");
				var sql = String.join("\n", sqlBlock.getLines());
				var cypherBlock = blocks.get(sqlBlock.getId() + "_expected");
				var cypher = String.join("\n", cypherBlock.getLines());
				DatabaseMetaData databaseMetaData = null;
				Map<String, String> tableMappings = new HashMap<>();
				Map<String, String> join_columns_mappings = new HashMap<>();
				if (sqlBlock.getAttribute("table_mappings") != null) {
					tableMappings = SqlToCypherConfig.buildMap((String) sqlBlock.getAttribute("table_mappings"));
				}
				if (sqlBlock.getAttribute("join_column_mappings") != null) {
					join_columns_mappings = SqlToCypherConfig
						.buildMap((String) sqlBlock.getAttribute("join_column_mappings"));
				}
				if (sqlBlock.getAttribute("metaData") != null) {
					String metaData = (String) sqlBlock.getAttribute("metaData");

					databaseMetaData = mock(DatabaseMetaData.class);
					for (String m : metaData.split(";")) {
						var endIndex = m.indexOf(":");
						String label = m.substring(0, endIndex);
						String[] properties = m.substring(endIndex + 1).split("\\|");
						try {
							var columns = (properties.length > 1)
									? makeColumns(properties[0], Arrays.copyOfRange(properties, 1, properties.length))
									: makeColumns(properties[0]);
							given(databaseMetaData.getColumns(null, null, label, null)).willReturn(columns);
						}
						catch (SQLException ex) {
							throw new RuntimeException(ex);
						}
					}
				}
				boolean parseCypher = Boolean.parseBoolean(((String) cypherBlock.getAttribute("parseCypher", "true")));
				boolean prettyPrint = true;
				if (parseCypher) {
					var renderer = Renderer.getRenderer(Configuration.newConfig().withDialect(Dialect.NEO4J_5).build());
					cypher = renderer.render(CypherParser.parse(cypher));
					prettyPrint = false;
				}
				return new TestData(sqlBlock.getId(), name, sql, cypher, tableMappings, join_columns_mappings,
						prettyPrint, databaseMetaData);
			}).forEach(this.testData::add);
			return document;
		}

	}

	private record TestData(String id, String name, String sql, String cypher, Map<String, String> tableMappings,
			Map<String, String> joinColumnsMappings, boolean prettyPrint, DatabaseMetaData databaseMetaData) {
	}

}
