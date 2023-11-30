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
package org.neo4j.driver.jdbc.translator.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import org.neo4j.cypherdsl.parser.CypherParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 * @author Michael Hunger
 */
class Sql2CypherTests {

	@Test
	void namedParameterPrefixForParsingShouldBeConfigurable() {
		var translator = Sql2Cypher
			.with(Sql2CypherConfig.builder().withParseNamedParamPrefix("$").withPrettyPrint(false).build());
		assertThat(translator.translate("INSERT INTO Movie (Movie.title) VALUES($1)"))
			.isEqualTo("CREATE (movie:`Movie` {title: $1})");
	}

	@Test
	void simpleUpdateShouldWork() {

		var translator = Sql2Cypher.defaultTranslator();
		assertThat(translator.translate("UPDATE Actor a SET name = 'Foo' WHERE id(a) = 4711")).isEqualTo("""
				MATCH (a:Actor)
				WHERE id(a) = 4711
				SET a.name = 'Foo'""");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|", textBlock = """
			SELECT id(n) FROM Movies n|MATCH (n:`Movies`) RETURN id(n)
			SELECT elementId(n) FROM Movies n|MATCH (n:`Movies`) RETURN elementId(n)
			SELECT foobar('const', bazbar(:1))|RETURN foobar('const', bazbar($1))
			""")
	void parserShallNotFailOnUnknownFunctions(String in, String expected) {

		var translator = Sql2Cypher.with(Sql2CypherConfig.builder().withPrettyPrint(false).build());
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
				.map(t -> DynamicTest.dynamicTest(t.name,
						() -> assertThatSqlIsTranslatedAsExpected(t.sql, t.cypher, t.tableMappings, t.prettyPrint)))
				.toList();
			return DynamicContainer.dynamicContainer(file.getName(), tests);
		});
	}

	void assertThatSqlIsTranslatedAsExpected(String sql, String expected, Map<String, String> tableMappings,
			boolean prettyPrint) {
		assertThat(Sql2Cypher.with(
				Sql2CypherConfig.builder().withPrettyPrint(prettyPrint).withTableToLabelMappings(tableMappings).build())
			.translate(sql)).isEqualTo(expected);
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
		var properties = new HashMap<String, String>();
		if (prettyPrint != null) {
			properties.put("s2c.prettyPrint", prettyPrint.toString());
		}
		if (alwaysEscapeNames != null) {
			properties.put("s2c.alwaysEscapeNames", alwaysEscapeNames.toString());
		}
		var cfg = Sql2CypherConfig.of(properties);
		var defaultCfg = Sql2CypherConfig.defaultConfig();
		assertThat(cfg.isPrettyPrint()).isEqualTo((prettyPrint != null) ? prettyPrint : defaultCfg.isPrettyPrint());
		assertThat(cfg.isAlwaysEscapeNames())
			.isEqualTo((alwaysEscapeNames != null) ? alwaysEscapeNames : !cfg.isPrettyPrint());
		var sql = "Select * from movies";
		var cypher = Sql2Cypher.with(cfg).translate(sql);
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
				Map<String, String> tableMappings = new HashMap<>();
				if (sqlBlock.getAttribute("table_mappings") != null) {
					tableMappings = Sql2CypherConfig.buildMap((String) sqlBlock.getAttribute("table_mappings"));
				}
				boolean parseCypher = Boolean.parseBoolean(((String) cypherBlock.getAttribute("parseCypher", "true")));
				boolean prettyPrint = true;
				if (parseCypher) {
					cypher = CypherParser.parse(cypher).getCypher();
					prettyPrint = false;
				}
				return new TestData(name, sql, cypher, tableMappings, prettyPrint);
			}).forEach(this.testData::add);
			return document;
		}

	}

	private record TestData(String name, String sql, String cypher, Map<String, String> tableMappings,
			boolean prettyPrint) {
	}

}
