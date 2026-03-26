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
package org.neo4j.jdbc.translator.impl;

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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Table;
import org.asciidoctor.extension.Treeprocessor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Parser;
import org.jooq.Select;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.jooq.impl.QOM;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Shared setup and helpers for tests that parse SQL through jOOQ and inspect the Query
 * Object Model (QOM). Provides parser initialization, field unwrapping, and alias
 * extraction utilities.
 *
 * @author Michael J. Simons
 */
final class TestUtils {

	private static final DSLContext DSL_CONTEXT;

	private static final Parser PARSER;

	static {
		Logger.getLogger("org.jooq.Constants").setLevel(Level.WARNING);
		Logger.getLogger("org.neo4j.jdbc.internal.shaded.jooq.Constants").setLevel(Level.WARNING);
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");

		DSL_CONTEXT = DSL.using(org.jooq.SQLDialect.DEFAULT);
		PARSER = DSL_CONTEXT.parser();
	}

	static Select<?> parseSelect(String sql) {
		var query = PARSER.parseQuery(sql);
		assertThat(query).isInstanceOf(Select.class);
		return (Select<?>) query;
	}

	static Field<?> unwrapAlias(SelectFieldOrAsterisk sfa) {
		if (sfa instanceof QOM.FieldAlias<?> fa) {
			return fa.$field();
		}
		return (Field<?>) sfa;
	}

	static Field<?> asField(SelectFieldOrAsterisk sfa) {
		return (Field<?>) sfa;
	}

	static String getAliasName(SelectFieldOrAsterisk sfa) {
		return ((Field<?>) sfa).getName();
	}

	static ResultSet makeColumns(String firstName, String... names) throws SQLException {
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

	private TestUtils() {
	}

	/** Extracts test data from asciidoctor files. */
	static class TestDataExtractor extends Treeprocessor {

		private final List<TestData> testData = new ArrayList<>();

		TestDataExtractor() {
			super(new HashMap<>()); // Must be mutable
		}

		String extractCode(Cell cell) {
			var intermediate = cell.getSource();
			return intermediate.replaceAll("^`|`$", "").replace("\\`", "`");
		}

		@Override
		public Document process(Document document) {

			processTables(document);
			processSourceBlocks(document);

			return document;
		}

		private void processTables(Document document) {
			document.findBy(Map.of("context", ":table", "style", "translation_table"))
				.stream()
				.map(Table.class::cast)
				.<TestData>mapMulti((table, consumer) -> {
					var id = table.getId();
					var cnt = 1;
					for (var row : table.getBody()) {
						var name = "%s_%d".formatted(id, cnt++);
						var cells = row.getCells();
						consumer.accept(new TestData(name, name, extractCode(cells.get(0)), extractCode(cells.get(1))));
					}
				})
				.forEach(this.testData::add);
		}

		private void processSourceBlocks(Document document) {
			var blocks = document.findBy(Map.of("context", ":listing", "style", "source"))
				.stream()
				.map(Block.class::cast)
				.filter(b -> b.hasAttribute("id"))
				.collect(Collectors.toMap(ContentNode::getId, Function.identity()));

			blocks.values().stream().filter(b -> "sql".equals(b.getAttribute("language"))).map(sqlBlock -> {
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
					try {
						given(databaseMetaData.getTables(any(), any(), any(), any())).willReturn(mock(ResultSet.class));
					}
					catch (SQLException ex) {
						throw new RuntimeException(ex);
					}
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
		}

	}

	public record TestData(String id, String name, String sql, String cypher, Map<String, String> tableMappings,
			Map<String, String> joinColumnsMappings, boolean prettyPrint, DatabaseMetaData databaseMetaData,
			boolean forceDefaults) {

		TestData(String id, String name, String sql, String cypher, Map<String, String> tableMappings,
				Map<String, String> joinColumnsMappings, boolean prettyPrint, DatabaseMetaData databaseMetaData) {
			this(id, name, sql, cypher, tableMappings, joinColumnsMappings, prettyPrint, databaseMetaData, false);
		}

		TestData(String id, String name, String sql, String cypher) {
			this(id, name, sql, cypher, Map.of(), Map.of(), false, null, true);
		}
	}

}
