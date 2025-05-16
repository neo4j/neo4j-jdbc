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
package org.neo4j.jdbc.translator.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.Asterisk;
import org.jooq.CreateTableElementListStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Param;
import org.jooq.Parser;
import org.jooq.QualifiedAsterisk;
import org.jooq.Query;
import org.jooq.QueryPart;
import org.jooq.Row;
import org.jooq.Select;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.ParserException;
import org.jooq.impl.QOM;
import org.jooq.impl.QOM.TableAlias;
import org.neo4j.cypherdsl.core.Case;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.ExposesRelationships;
import org.neo4j.cypherdsl.core.ExposesReturning;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Finish;
import org.neo4j.cypherdsl.core.IdentifiableElement;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.NodeLabel;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.PropertyContainer;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.RelationshipChain;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.neo4j.cypherdsl.core.ResultStatement;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReading;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingWithWhere;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingWithoutWhere;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.GeneralizedRenderer;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.jdbc.translator.spi.Cache;
import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.View;

/**
 * A jOOQ/Cypher-DSL based SQL to Cypher translator and the default translator for the
 * Neo4j JDBC driver when bundled.
 *
 * @author Lukas Eder
 * @author Michael J. Simons
 * @author Michael Hunger
 */
final class SqlToCypher implements Translator {

	static final Pattern ELEMENT_ID_PATTERN = Pattern.compile("(?i)v\\$(?:(?<prefix>.+?)_)?id");
	static final String ELEMENT_ID_FUNCTION_NAME = "elementId";
	static final String ELEMENT_ID_ALIAS = "v$id";
	static final Pattern PERCENT_OR_UNDERSCORE = Pattern.compile("[%_]");

	static {
		Logger.getLogger("org.jooq.Constants").setLevel(Level.WARNING);
		Logger.getLogger("org.neo4j.jdbc.internal.shaded.jooq.Constants").setLevel(Level.WARNING);
		System.setProperty("org.jooq.no-logo", "true");
		System.setProperty("org.jooq.no-tips", "true");
	}

	private static final Map<String, String> FUNCTION_MAPPING = Map.of("strpos", "apoc.text.indexOf");

	private static final int STATEMENT_CACHE_SIZE = 64;

	static Translator defaultTranslator() {
		return new SqlToCypher(SqlToCypherConfig.defaultConfig());
	}

	static Translator with(SqlToCypherConfig config) {
		return new SqlToCypher(config);
	}

	private final SqlToCypherConfig config;

	private final Configuration rendererConfig;

	private final Cache<Query, String> cache = Cache.getInstance(STATEMENT_CACHE_SIZE);

	private final Set<View> views;

	private SqlToCypher(SqlToCypherConfig config) {

		this.config = config;
		this.rendererConfig = Configuration.newConfig()
			.withPrettyPrint(this.config.isPrettyPrint())
			.alwaysEscapeNames(this.config.isAlwaysEscapeNames())
			.withDialect(Dialect.NEO4J_5)
			.build();

		if (this.config.getViewDefinitions() == null) {
			this.views = Set.of();
		}
		else {
			try {
				this.views = Set.copyOf(ViewDefinitionReader.of(this.config.getViewDefinitions()).read());
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	@Override
	public void flushCache() {
		this.cache.flush();
	}

	@Override
	public int getOrder() {
		return Optional.ofNullable(this.config.getPrecedence()).orElseGet(Translator.super::getOrder);
	}

	@Override
	public Set<View> getViews() {

		return this.views;
	}

	@Override
	public String translate(String sql, DatabaseMetaData optionalDatabaseMetaData) {

		Query query;
		try {
			DSLContext dsl = createDSLContext();
			Parser parser = dsl.parser();
			query = parser.parseQuery(sql);
			if (query == null && sql != null && sql.trim().startsWith("//")) {
				return Renderer.getRenderer(this.rendererConfig, GeneralizedRenderer.class).render(Finish.create());
			}
		}
		catch (ParserException pe) {
			throw new IllegalArgumentException(pe);
		}

		if (this.config.isCacheEnabled()) {
			synchronized (this) {
				return this.cache.computeIfAbsent(query, key -> translate0(query, optionalDatabaseMetaData));
			}
		}
		return translate0(query, optionalDatabaseMetaData);
	}

	private String translate0(Query query, DatabaseMetaData databaseMetaData) {

		var views = this.getViews().stream().collect(Collectors.toMap(View::name, Function.identity()));
		return render(ContextAwareStatementBuilder.build(this.config, databaseMetaData, query, views));
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private DSLContext createDSLContext() {

		@SuppressWarnings("removal") // Deprecated only to inform users that this method
										// is going to be package private at some point.
		var settings = this.config.asSettings();
		Optional.ofNullable(this.config.getParseNamedParamPrefix())
			.filter(Predicate.not(String::isBlank))
			.map(String::trim)
			.ifPresent(settings::withParseNamedParamPrefix);

		var context = DSL.using(this.config.getSqlDialect(), settings);
		context.configuration().set(() -> {
			var tables = new HashMap<String, Query>();

			this.config.getJoinColumnsToTypeMappings().forEach((k, v) -> {
				var tableAndColumnName = k.split("\\.");
				var createTableStep = (CreateTableElementListStep) tables.computeIfAbsent(tableAndColumnName[0],
						DSL::createTable);
				createTableStep.column(DSL.field(tableAndColumnName[1]).comment("type=" + v));
			});

			this.config.getTableToLabelMappings().forEach((k, v) -> {
				var createTableStep = (CreateTableElementListStep) tables.computeIfAbsent(k, DSL::createTable);
				createTableStep.comment("label=" + v);
			});

			return context.meta(tables.values().toArray(Query[]::new));
		});
		return context;
	}

	private String render(Statement statement) {
		return Renderer.getRenderer(this.rendererConfig).render(statement);
	}

	record JoinDetails(QOM.Join<?> join, QOM.Eq<?> eq) {
		static JoinDetails of(QOM.JoinTable<?, ?> joinTable) {
			QOM.Join<?> join = (joinTable instanceof QOM.Join<?> $join) ? $join : null;
			QOM.Eq<?> eq = (join != null && join.$on() instanceof QOM.Eq<?> $eq) ? $eq : null;

			return new JoinDetails(join, eq);
		}
	}

	static class ContextAwareStatementBuilder {

		/**
		 * Column assignments known in this context.
		 */
		private final Map<String, Expression> columnsAndValues = new LinkedHashMap<>();

		/**
		 * Key is the column name, value is the number of times a column with the name has
		 * been returned. Used for generating unique names.
		 */
		private final Map<String, AtomicInteger> returnColumns = new HashMap<>();

		private final SqlToCypherConfig config;

		private final DatabaseMetaData databaseMetaData;

		private final ParameterNameGenerator parameterNameGenerator = new ParameterNameGenerator();

		/**
		 * Content of the {@literal FROM} clause, stored in the context to not have it
		 * passed around everywhere. Might be empty and is mutable on purpose.
		 */
		private final List<Table<?>> tables = new ArrayList<>();

		private final Map<SymbolicName, PatternElement> resolvedRelationships = new HashMap<>();

		private final AtomicBoolean useAliasForVColumn = new AtomicBoolean(true);

		private final Map<String, View> views;

		static Statement build(SqlToCypherConfig config, DatabaseMetaData databaseMetaData, Query query,
				Map<String, View> views) {
			if (query instanceof Select<?> s) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(s);
			}
			else if (query instanceof QOM.Delete<?> d) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(d);
			}
			else if (query instanceof QOM.Truncate<?> t) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(t);
			}
			else if (query instanceof QOM.Insert<?> t) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(t);
			}
			else if (query instanceof QOM.InsertReturning<?> t) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(t.$insert(),
						t.$returning());
			}
			else if (query instanceof QOM.Update<?> u) {
				return new ContextAwareStatementBuilder(config, databaseMetaData, views).statement(u);
			}
			else {
				throw unsupported(query);
			}
		}

		ContextAwareStatementBuilder(SqlToCypherConfig config, DatabaseMetaData databaseMetaData,
				Map<String, View> views) {
			this.config = config;
			this.databaseMetaData = databaseMetaData;
			this.views = views;
		}

		private boolean ownsView(CbvPointer cbvPointer) {
			return this.views.containsKey(cbvPointer.viewName());
		}

		private static IllegalArgumentException unsupported(QueryPart p) {
			var typeMsg = (p != null) ? " (Was of type " + p.getClass().getName() + ")" : "";
			return new IllegalArgumentException("Unsupported SQL expression: " + p + typeMsg);
		}

		private static Node nodeWithProperties(Node src, Map<String, Expression> properties) {
			// Due to a slightly problematic API in the Cypher-DSL ^ms
			return src.withProperties(
					properties.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray());
		}

		private static SymbolicName symbolicName(String value) {
			return Cypher.name(value.toLowerCase(Locale.ROOT));
		}

		private static String relationshipTypeName(Field<?> lhsJoinColumn) {
			return Objects.requireNonNull(lhsJoinColumn.getQualifiedName().last()).toUpperCase(Locale.ROOT);
		}

		@SuppressWarnings("squid:S1854") // This is about useless assignments, which I
											// don't think apply to m1 and m2.
		private Statement statement(QOM.Delete<?> d) {
			this.tables.clear();
			this.tables.add(d.$from());

			assertCypherBackedViewUsage("Cypher-backed views cannot be deleted from", this.tables.get(0));

			var m1 = Cypher.match(resolveTableOrJoin(this.tables.get(0)).get(0));
			var m2 = (d.$where() != null) ? m1.where(condition(d.$where())) : (OngoingReadingWithWhere) m1;
			return m2.delete(((Node) resolveTableOrJoin(this.tables.get(0)).get(0)).asExpression()).build();
		}

		private Statement statement(QOM.Truncate<?> t) {
			this.tables.clear();
			this.tables.addAll(t.$table());

			for (var table : this.tables) {
				assertCypherBackedViewUsage("Cypher-backed views cannot be deleted from", table);
			}

			@SuppressWarnings("squid:S1854")
			var node = (Node) resolveTableOrJoin(this.tables.get(0)).get(0);
			return Cypher.match(node).detachDelete(node.asExpression()).build();
		}

		private ResultStatement statement(Select<?> incoming) {
			Select<?> x;
			boolean addLimit = false;
			// Assume it's a funny, wrapper checked query
			if (incoming.$from().$first() instanceof TableAlias<?> tableAlias
					&& tableAlias.$table() instanceof QOM.DerivedTable<?> d) {
				addLimit = incoming.$where() != null;
				x = d.$arg1();
			}
			else {
				x = incoming;
			}

			// Retrieve all Cypher-backed views
			var allCbvs = loadCypherBackedViews();
			var cbvs = new ArrayList<CbvPointer>();

			this.tables.clear();
			this.tables.addAll(unnestFromClause(x.$from(), false, (table, partOfJoin) -> {
				var p = CbvPointer.of(table);
				if (allCbvs.contains(p.viewName()) && ownsView(p)) {
					if (partOfJoin) {
						throw new IllegalArgumentException("Cypher-backed views cannot be used with a JOIN clause");
					}
					cbvs.add(p);
				}
			}));

			if (!cbvs.isEmpty() && cbvs.size() < this.tables.size()) {
				throw new IllegalArgumentException("Cypher-backed views cannot be combined with regular tables");
			}

			// Done lazy as otherwise the property containers won't be resolved
			Supplier<List<Expression>> resultColumnsSupplier = () -> x.$select()
				.stream()
				.flatMap(this::expression)
				.toList();

			if (x.$from().isEmpty()) {
				return Cypher.returning(resultColumnsSupplier.get()).build();
			}

			OngoingReading m2;
			if (!cbvs.isEmpty()) {
				StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere m1 = null;
				List<IdentifiableElement> previousAliases = new ArrayList<>();
				for (var cbv : cbvs) {
					var view = this.views.get(cbv.viewName());
					var projection = Cypher.mapOf(view.columns()
						.stream()
						.flatMap(column -> Stream.of(column.name(), Cypher.raw(column.propertyName())))
						.toArray());
					if (m1 == null) {
						previousAliases.add(projection.as(cbv.alias()));
						m1 = Cypher.callRawCypher(view.query()).with(previousAliases);
					}
					else {
						previousAliases.add(projection.as(cbv.alias()));
						m1 = m1.callRawCypher(view.query()).with(previousAliases);
					}
				}
				m2 = (x.$where() != null) ? m1.where(condition(x.$where())) : m1;
			}
			else {
				OngoingReadingWithoutWhere m1 = Cypher
					.match(x.$from().stream().flatMap(t -> resolveTableOrJoin(t).stream()).toList());
				m2 = (x.$where() != null) ? m1.where(condition(x.$where())) : (OngoingReadingWithWhere) m1;
			}

			var intermediate = x.$distinct() ? m2.returningDistinct(resultColumnsSupplier.get())
					: m2.returning(resultColumnsSupplier.get());
			var returning = intermediate.orderBy(x.$orderBy().stream().map(this::expression).toList());

			StatementBuilder.BuildableStatement<ResultStatement> buildableStatement;
			if (!(x.$limit() instanceof Param<?> param)) {
				buildableStatement = addLimit ? returning.limit(1) : returning;
			}
			else {
				buildableStatement = returning.limit(expression(param));
			}

			return buildableStatement.build();
		}

		@SuppressWarnings("squid:S108") // The empty and already stated to be ignored
										// catch block
		private Set<String> loadCypherBackedViews() {
			if (this.databaseMetaData == null) {
				return Set.of();
			}
			var allCbvs = new HashSet<String>();
			try (var resultSet = this.databaseMetaData.getTables(null, null, null, new String[] { "CBV" })) {
				while (resultSet.next()) {
					allCbvs.add(resultSet.getString("TABLE_NAME"));
				}
			}
			catch (SQLException ignored) {
			}
			return allCbvs;
		}

		private Statement statement(QOM.Insert<?> insert) {
			return statement(insert, List.of());
		}

		private Statement statement(QOM.Insert<?> insert, List<? extends SelectFieldOrAsterisk> returning) {
			this.tables.clear();
			this.tables.add(insert.$into());

			assertCypherBackedViewUsage("Cypher-backed views cannot be inserted to", this.tables.get(0));

			var node = (Node) this.resolveTableOrJoin(this.tables.get(0)).get(0);
			var rows = insert.$values();

			var hasMergeProperties = !insert.$onConflict().isEmpty();
			var useMerge = insert.$onDuplicateKeyIgnore() || hasMergeProperties;
			if (rows.size() == 1) {
				return buildSingleCreateStatement(insert, returning, node, useMerge, hasMergeProperties);
			}
			else {
				return buildUnwindCreateStatement(insert, returning, node, useMerge, hasMergeProperties);
			}
		}

		private Statement buildSingleCreateStatement(QOM.Insert<?> insert,
				List<? extends SelectFieldOrAsterisk> returning, Node node, boolean useMerge,
				boolean hasMergeProperties) {
			var row = Objects.requireNonNull(insert.$values().$first());
			var columns = insert.$columns();
			Map<String, Expression> nodeProperties = new LinkedHashMap<>();
			for (int i = 0; i < columns.size(); ++i) {
				nodeProperties.put(columns.get(i).getName(), expression(row.field(i)));
			}
			if (useMerge) {
				Map<String, Expression> mergeProperties = hasMergeProperties ? new LinkedHashMap<>() : nodeProperties;
				insert.$onConflict().forEach(c -> {
					mergeProperties.put(c.getName(), nodeProperties.get(c.getName()));
					nodeProperties.remove(c.getName());
				});

				var properties = new ArrayList<Expression>();
				nodeProperties.forEach((k, v) -> properties.add(Cypher.set(node.property(k), v)));

				StatementBuilder.BuildableStatement<?> merge = Cypher.merge(nodeWithProperties(node, mergeProperties));
				if (hasMergeProperties) {
					merge = ((StatementBuilder.ExposesMergeAction) merge).onCreate().set(properties);
				}
				if (!insert.$updateSet().isEmpty()) {
					var updates = new ArrayList<Expression>();
					insert.$updateSet().forEach((c, v) -> {
						synchronized (this) {
							try {
								this.columnsAndValues.putAll(nodeProperties);
								updates
									.add(Cypher.set(node.property(((Field<?>) c).getName()), expression((Field<?>) v)));
							}
							finally {
								nodeProperties.keySet().forEach(this.columnsAndValues::remove);
							}
						}
					});
					merge = ((StatementBuilder.ExposesMergeAction) merge).onMatch().set(updates);
				}
				return addOptionalReturnAndBuild((ExposesReturning & StatementBuilder.BuildableStatement<?>) merge,
						returning);
			}
			return addOptionalReturnAndBuild(Cypher.create(nodeWithProperties(node, nodeProperties)), returning);
		}

		private Statement buildUnwindCreateStatement(QOM.Insert<?> insert,
				List<? extends SelectFieldOrAsterisk> returning, Node node, boolean useMerge,
				boolean hasMergeProperties) {
			if (useMerge && !hasMergeProperties) {
				throw new UnsupportedOperationException(
						"MERGE is not supported when inserting multiple rows without using a property to merge on");
			}

			var columns = insert.$columns();
			var props = Cypher.listOf(insert.$values().stream().map(row -> {
				var result = new HashMap<String, Object>(columns.size());
				for (int i = 0; i < columns.size(); ++i) {
					result.put(columns.get(i).getName(), expression(row.field(i)));
				}
				return Cypher.literalOf(result);
			}).toList());

			if (useMerge) {

				var symName = Cypher.name("properties");
				var mergeProperties = new LinkedHashMap<String, Expression>();

				insert.$onConflict()
					.forEach(c -> mergeProperties.put(c.getName(),
							Cypher.property(symName, Cypher.literalOf(c.getName()))));

				var properties = new ArrayList<Expression>();
				columns.stream()
					.filter(c -> !mergeProperties.containsKey(c.getName()))
					.forEach(c -> properties
						.add(Cypher.set(node.property(c.getName()), Cypher.property(symName, c.getName()))));

				var updates = new ArrayList<Expression>();
				synchronized (this) {
					try {
						columns.forEach(c -> this.columnsAndValues.put(c.getName(),
								Cypher.property(symName, Cypher.literalOf(c.getName()))));
						insert.$updateSet()
							.forEach((c, v) -> updates
								.add(Cypher.set(node.property(((Field<?>) c).getName()), expression((Field<?>) v))));
					}
					finally {
						columns.forEach(c -> this.columnsAndValues.remove(c.getName()));
					}
				}

				return addOptionalReturnAndBuild(Cypher.unwind(props)
					.as("properties")
					.merge(nodeWithProperties(node, mergeProperties))
					.onCreate()
					.set(properties)
					.onMatch()
					.set(updates), returning);
			}

			return addOptionalReturnAndBuild(
					Cypher.unwind(props).as("properties").create(node).set(node, Cypher.name("properties")), returning);
		}

		private <T extends ExposesReturning & StatementBuilder.BuildableStatement<?>> Statement addOptionalReturnAndBuild(
				T exposesReturning, List<? extends SelectFieldOrAsterisk> returning) {
			if (returning == null || returning.isEmpty()) {
				return exposesReturning.build();
			}

			return exposesReturning.returning(returning.stream().flatMap(this::expression).toList()).build();
		}

		private String uniqueColumnName(String s) {
			var cnt = this.returnColumns.computeIfAbsent(s, k -> new AtomicInteger(0))
				.getAndAccumulate(1, Integer::sum);
			return s + ((cnt > 0) ? cnt : "");
		}

		private Statement statement(QOM.Update<?> update) {
			this.tables.clear();
			this.tables.add(update.$table());

			assertCypherBackedViewUsage("Cypher-backed views cannot be updated", this.tables.get(0));

			var node = (Node) this.resolveTableOrJoin(this.tables.get(0)).get(0);
			var updates = new ArrayList<Expression>();
			update.$set().forEach((c, v) -> {
				updates.add(node.property(((Field<?>) c).getName()));
				updates.add(expression((Field<?>) v));
			});

			StatementBuilder.ExposesSet exposesSet;
			if (update.$where() != null) {
				exposesSet = Cypher.match(node).where(condition(update.$where()));
			}
			else {
				exposesSet = Cypher.match(node);
			}
			return exposesSet.set(updates).build();
		}

		private void assertCypherBackedViewUsage(String s, Table<?> table) {
			var allCbvs = loadCypherBackedViews();
			var p = CbvPointer.of(table);
			if (allCbvs.contains(p.viewName()) && ownsView(p)) {
				throw new IllegalArgumentException(s);
			}
		}

		@SuppressWarnings("squid:S3776") // Yep, this is complex.
		private Stream<Expression> expression(SelectFieldOrAsterisk t) {

			if (t instanceof SelectField<?> s) {
				var theField = (s instanceof QOM.FieldAlias<?> fa) ? fa.$aliased() : s;
				Expression col;
				if (theField instanceof TableField<?, ?> tf && tf.getTable() == null) {
					col = findTableFieldInTables(tf, true, !(s instanceof QOM.FieldAlias<?>));
				}
				else {
					col = expression(s);
				}

				if (s instanceof QOM.FieldAlias<?> fa) {
					col = col.as(fa.$alias().last());
				}

				return Stream.of(col);
			}
			else if (t instanceof Asterisk) {
				var properties = projectAllColumns();
				if (properties.isEmpty()) {
					properties.add(Cypher.asterisk());
				}
				return properties.stream();
			}
			else if (t instanceof QualifiedAsterisk q && resolveTableOrJoin(q.$table()).get(0) instanceof Node node) {

				var properties = new ArrayList<Expression>();
				for (var table : this.tables) {
					if (table instanceof TableAlias<?> tableAlias
							&& tableAlias.getName().equals(q.$table().getName())) {
						properties.addAll(projectAllColumns(List.of(tableAlias)));
						break;
					}
				}
				if (properties.isEmpty()) {
					var symbolicName = node.getSymbolicName().orElseGet(() -> Cypher.name(q.$table().getName()));
					properties.add(symbolicName.project(Cypher.asterisk()).as(symbolicName));
				}
				return properties.stream();
			}
			else {
				throw unsupported(t);
			}
		}

		private List<Expression> projectAllColumns() {
			return projectAllColumns(this.tables);
		}

		private List<Expression> projectAllColumns(List<Table<?>> from) {
			List<Expression> properties = new ArrayList<>();
			if (this.databaseMetaData == null) {
				return properties;
			}
			for (Table<?> table : from) {
				var pc = (PropertyContainer) resolveTableOrJoin(table).get(0);
				var tableName = labelOrType(table);
				if (!(pc instanceof Relationship rel)) {
					properties.addAll(findProperties(tableName, pc));
				}
				else {
					properties.add(makeFk(rel.getLeft()));
					properties.addAll(findProperties(tableName, pc));
					properties.add(makeFk(rel.getRight()));
				}
			}
			return properties;
		}

		private Expression makeFk(Node node) {
			var nodeName = node.getLabels().get(0).getValue();
			return Cypher.call(ELEMENT_ID_FUNCTION_NAME)
				.withArgs(node.asExpression())
				.asFunction()
				.as(uniqueColumnName("v$" + nodeName.toLowerCase(Locale.ROOT) + "_id"));
		}

		private List<Expression> findProperties(String tableName, PropertyContainer pc) {
			List<Expression> properties = new ArrayList<>();
			try (var columns = this.databaseMetaData.getColumns(null, null, tableName, null)) {
				if (!this.views.containsKey(tableName)) {
					properties.add(makeId(pc, null));
				}
				while (columns.next()) {
					var columnName = columns.getString("COLUMN_NAME");
					if ("YES".equalsIgnoreCase(columns.getString("IS_GENERATEDCOLUMN"))) {
						continue;
					}
					properties.add(pc.property(columnName).as(uniqueColumnName(columnName)));
				}
			}
			catch (SQLException ex) {
				throw new RuntimeException(ex);
			}
			return properties;
		}

		private Expression makeId(PropertyContainer pc, String alias) {
			var function = Cypher.call(ELEMENT_ID_FUNCTION_NAME).withArgs(pc.asExpression()).asFunction();
			return this.useAliasForVColumn.get()
					? function.as(uniqueColumnName((alias != null) ? alias : ELEMENT_ID_ALIAS)) : function;
		}

		private static List<? extends Table<?>> unnestFromClause(List<? extends Table<?>> tables, boolean partOfJoin,
				BiConsumer<Table<?>, Boolean> listener) {
			List<Table<?>> result = new ArrayList<>();
			for (Table<?> table : tables) {
				if (table instanceof QOM.JoinTable<?, ? extends Table<?>> join) {
					result.addAll(unnestFromClause(List.of(join.$table1()), true, listener));
					result.addAll(unnestFromClause(List.of(join.$table2()), true, listener));
				}
				else {
					result.add(table);
					listener.accept(table, partOfJoin);
				}
			}
			return result;
		}

		private Expression expression(SelectField<?> s) {
			if (s instanceof QOM.FieldAlias<?> fa) {
				return expression(fa.$aliased()).as(fa.$alias().last());
			}
			else if (s instanceof Field<?> f) {
				return expression(f);
			}
			else {
				throw unsupported(s);
			}
		}

		private SortItem expression(SortField<?> s) {
			try {
				this.useAliasForVColumn.set(false);

				var direction = s.$sortOrder().name().toUpperCase(Locale.ROOT);

				Field<?> theField = s.$field();
				Expression col = null;
				try {
					col = expression(theField);
				}
				catch (IllegalArgumentException ex) {
					if (theField instanceof TableField<?, ?> tf && tf.getTable() == null) {
						col = findTableFieldInTables(tf, false, false);
					}
					if (s instanceof QOM.FieldAlias<?> fa && col != null) {
						col = col.as(fa.$alias().last());
					}
				}
				return Cypher.sort(col, "DEFAULT".equals(direction) ? SortItem.Direction.UNDEFINED
						: SortItem.Direction.valueOf(direction));
			}
			finally {
				this.useAliasForVColumn.set(true);
			}
		}

		/**
		 * Looks for the table field {@code tf} in the list of {@code tables} for all
		 * those cases in which the table field does not have an associated table with it.
		 * @param tf the table field to reify
		 * @param fallbackToFieldName set to {@literal true} to fall back to the plain
		 * field name
		 * @param doAlias if we can use the field name as alias
		 * @return the Cypher column that was determined
		 */
		@SuppressWarnings("squid:S3776") // Yep, this is complex.
		private Expression findTableFieldInTables(TableField<?, ?> tf, boolean fallbackToFieldName, boolean doAlias) {
			Expression col = null;
			if (this.tables.size() == 1) {
				var propertyContainer = (PropertyContainer) resolveTableOrJoin(this.tables.get(0)).get(0);
				if (isElementId(tf)) {
					return makeId(propertyContainer, tf.getName());
				}
				if (isFkId(tf) != null && propertyContainer instanceof Relationship rel) {
					if (anyLabelMatches(rel.getLeft(), tf.getName())) {
						return makeId(rel.getLeft(), tf.getName());
					}
					else if (anyLabelMatches(rel.getRight(), tf.getName())) {
						return makeId(rel.getRight(), tf.getName());
					}
				}
				col = propertyContainer.getSymbolicName().filter(f -> !f.getValue().equals(tf.getName())).map(__ -> {
					var property = propertyContainer.property(tf.getName());
					if (doAlias) {
						return property.as(tf.getName());
					}
					return property;
				}).orElse(null);
			}
			else if (this.databaseMetaData != null) {
				var isId = isElementId(tf);
				var prefix = isFkId(tf);
				for (Table<?> table : this.tables) {
					var tableName = labelOrType(table);

					// Figure out virtual columns
					if (isId) {
						var pc = (PropertyContainer) resolveTableOrJoin(table).get(0);
						return makeId(pc, tf.getName());
					}
					if (tableName.equalsIgnoreCase(prefix)) {
						var pc = (PropertyContainer) resolveTableOrJoin(table).get(0);
						return makeId(pc, tf.getName());
					}

					try (var columns = this.databaseMetaData.getColumns(null, null, tableName, null)) {
						while (columns.next()) {
							var columnName = columns.getString("COLUMN_NAME");
							if (columnName.equals(tf.getName())) {
								var pc = (PropertyContainer) resolveTableOrJoin(table).get(0);
								col = pc.property(tf.getName());
							}
						}
					}
					catch (SQLException ex) {
						throw new RuntimeException(ex);
					}
				}
			}

			if (col == null && fallbackToFieldName) {
				col = Cypher.name(tf.getName());
			}
			return col;
		}

		private Expression expression(Field<?> f) {
			return expression(f, false);
		}

		@SuppressWarnings({ "NestedIfDepth", "squid:S3776" })
		private Expression expression(Field<?> f, boolean turnUnknownIntoNames) {

			if (f instanceof Param<?> p) {
				if (p.$inline()) {
					return Cypher.literalOf(p.getValue());
				}
				else {
					String parameterName;
					if (p.getParamType() == ParamType.INDEXED || p.getParamType() == ParamType.FORCE_INDEXED) {
						parameterName = this.parameterNameGenerator.newIndex();
					}
					else {
						parameterName = this.parameterNameGenerator.newIndex(p.getParamName());
					}

					return (parameterName != null) ? Cypher.parameter(parameterName, p.getValue())
							: Cypher.anonParameter(p.getValue());
				}
			}
			else if (f instanceof TableField<?, ?> tf) {
				if (tf.getTable() == null) {
					var tableField = findTableFieldInTables(tf, turnUnknownIntoNames, false);
					if (tableField == null) {
						throw unsupported(tf);
					}
					return tableField;
				}

				var pe = resolveTableOrJoin(tf.getTable()).get(0);
				if (pe instanceof PropertyContainer pc) {
					var m = ELEMENT_ID_PATTERN.matcher(tf.getName());
					if (m.matches()) {
						var src = pc;
						var prefix = m.group("prefix");
						if (pc instanceof Relationship rel && !"element".equalsIgnoreCase(prefix)) {
							if (rel.getLeft().getLabels().get(0).getValue().equalsIgnoreCase(prefix)) {
								src = rel.getLeft();
							}
							else if (rel.getRight().getLabels().get(0).getValue().equalsIgnoreCase(prefix)) {
								src = rel.getRight();
							}
						}
						// Not using makeId here as we don't want this aliased in a
						// condition
						return makeId(src, tf.getName());
					}

					return pc.property(tf.getName());
				}
				throw unsupported(tf);
			}
			else if (f instanceof QOM.Add<?> e) {
				return expression(e.$arg1()).add(expression(e.$arg2()));
			}
			else if (f instanceof QOM.Sub<?> e) {
				return expression(e.$arg1()).subtract(expression(e.$arg2()));
			}
			else if (f instanceof QOM.Mul<?> e) {
				return expression(e.$arg1()).multiply(expression(e.$arg2()));
			}
			else if (f instanceof QOM.Square<?> e) {
				return expression(e.$arg1()).multiply(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Div<?> e) {
				return expression(e.$arg1()).divide(expression(e.$arg2()));
			}
			else if (f instanceof QOM.Neg<?> e) {
				throw unsupported(e);
			}

			// https://neo4j.com/docs/cypher-manual/current/functions/mathematical-numeric/
			else if (f instanceof QOM.Abs<?> e) {
				return Cypher.abs(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Ceil<?> e) {
				return Cypher.ceil(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Floor<?> e) {
				return Cypher.floor(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Round<?> e) {
				if (e.$arg2() == null) {
					return Cypher.round(expression(e.$arg1()));
				}
				else {
					return Cypher.round(expression(e.$arg1()), expression(e.$arg2()));
				}
			}
			else if (f instanceof QOM.Sign e) {
				return Cypher.sign(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Rand) {
				return Cypher.rand();
			}

			// https://neo4j.com/docs/cypher-manual/current/functions/mathematical-logarithmic/
			else if (f instanceof QOM.Euler) {
				return Cypher.e();
			}
			else if (f instanceof QOM.Exp e) {
				return Cypher.exp(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Ln e) {
				return Cypher.log(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Log e) {
				return Cypher.log(expression(e.$arg1())).divide(Cypher.log(expression(e.$arg2())));
			}
			else if (f instanceof QOM.Log10 e) {
				return Cypher.log10(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Sqrt e) {
				return Cypher.sqrt(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Acos e) {
				return Cypher.acos(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Asin e) {
				return Cypher.asin(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Atan e) {
				return Cypher.atan(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Atan2 e) {
				return Cypher.atan2(expression(e.$arg1()), expression(e.$arg2()));
			}
			else if (f instanceof QOM.Cos e) {
				return Cypher.cos(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Cot e) {
				return Cypher.cot(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Degrees e) {
				return Cypher.degrees(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Pi) {
				return Cypher.pi();
			}
			else if (f instanceof QOM.Radians e) {
				return Cypher.radians(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Sin e) {
				return Cypher.sin(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Tan e) {
				return Cypher.tan(expression(e.$arg1()));
			}

			// https://neo4j.com/docs/cypher-manual/current/functions/string/
			else if (f instanceof QOM.CharLength e) {
				return Cypher.size(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Left e) {
				return Cypher.left(expression(e.$arg1()), expression(e.$arg2()));
			}
			else if (f instanceof QOM.Lower e) {
				return Cypher.toLower(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Ltrim e) {
				return Cypher.ltrim(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Replace e) {
				return Cypher.replace(expression(e.$arg1()), expression(e.$arg2()), expression(e.$arg3()));
			}
			else if (f instanceof QOM.Reverse e) {
				return Cypher.reverse(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Right e) {
				return Cypher.right(expression(e.$arg1()), expression(e.$arg2()));
			}
			else if (f instanceof QOM.Rtrim e) {
				return Cypher.rtrim(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Substring e) {
				var length = expression(e.$arg3());
				if (length != Cypher.literalNull()) {
					return Cypher.substring(expression(e.$arg1()), expression(e.$arg2()), length);
				}
				else {
					return Cypher.substring(expression(e.$arg1()), expression(e.$arg2()), null);
				}
			}
			else if (f instanceof QOM.Trim e) {
				if (e.$arg2() != null) {
					throw unsupported(e);
				}
				else {
					return Cypher.trim(expression(e.$arg1()));
				}
			}
			else if (f instanceof QOM.Upper e) {
				return Cypher.toUpper(expression(e.$arg1()));
			}

			// https://neo4j.com/docs/cypher-manual/current/functions/scalar/
			else if (f instanceof QOM.Coalesce<?> e) {
				return Cypher.coalesce(e.$arg1().stream().map(this::expression).toArray(Expression[]::new));
			}
			else if (f instanceof QOM.Nvl<?> e) {
				return Cypher.coalesce(expression(e.$arg1()), expression(e.$arg2()));
			}

			// https://neo4j.com/docs/cypher-manual/current/syntax/expressions/
			else if (f instanceof QOM.Nullif<?> e) {
				return Cypher.caseExpression()
					.when(expression(e.$arg1()).eq(expression(e.$arg2())))
					.then(Cypher.literalNull())
					.elseDefault(expression(e.$arg1()));
			}
			else if (f instanceof QOM.Nvl2<?> e) {
				return Cypher.caseExpression()
					.when(expression(e.$arg1()).isNotNull())
					.then(expression(e.$arg2()))
					.elseDefault(expression(e.$arg3()));
			}
			else if (f instanceof QOM.CaseSimple<?, ?> e) {
				Case c = Cypher.caseExpression(expression(e.$value()));

				for (var w : e.$when()) {
					c = c.when(expression(w.$1())).then(expression(w.$2()));
				}

				if (e.$else() != null) {
					c = ((Case.CaseEnding) c).elseDefault(expression(e.$else()));
				}

				return c;
			}
			else if (f instanceof QOM.CaseSearched<?> e) {
				Case c = Cypher.caseExpression();

				for (var w : e.$when()) {
					c = c.when(condition(w.$1())).then(expression(w.$2()));
				}

				if (e.$else() != null) {
					c = ((Case.CaseEnding) c).elseDefault(expression(e.$else()));
				}

				return c;
			}

			// Others
			else if (f instanceof QOM.Cast<?> e) {
				if (e.$dataType().isString()) {
					return Cypher.toString(expression(e.$field()));
				}
				else if (e.$dataType().isBoolean()) {
					return Cypher.toBoolean(expression(e.$field()));
				}
				else if (e.$dataType().isFloat()) {
					return Cypher.toFloat(expression(e.$field()));
				}
				else if (e.$dataType().isInteger()) {
					return Cypher.toInteger(expression(e.$field()));
				}
				else {
					throw unsupported(f);
				}
			}
			else if (f instanceof org.jooq.True) {
				return Cypher.literalTrue();
			}
			else if (f instanceof org.jooq.False) {
				return Cypher.literalFalse();
			}
			else if (f instanceof QOM.Null || f == null || f instanceof org.jooq.Null) {
				return Cypher.literalNull();
			}
			else if (f instanceof QOM.Function<?> func) {
				return buildFunction(func);
			}
			else if (f instanceof QOM.Excluded<?> excluded
					&& this.columnsAndValues.containsKey(excluded.$field().getName())) {
				return this.columnsAndValues.get(excluded.$field().getName());
			}
			else if (f instanceof QOM.Count c) {
				var field = c.$field();
				Expression exp;
				// See https://github.com/jOOQ/jOOQ/issues/16344
				if (field instanceof Asterisk || "*".equals(field.toString())) {
					exp = Cypher.asterisk();
				}
				else {
					exp = expression(field);
				}
				return c.$distinct() ? Cypher.countDistinct(exp) : Cypher.count(exp);
			}
			else if (f instanceof Asterisk) {
				return Cypher.asterisk();
			}
			else if (f instanceof QOM.Min<?> m) {
				return Cypher.min(expression(m.$field()));
			}
			else if (f instanceof QOM.Max<?> m) {
				return Cypher.max(expression(m.$field()));
			}
			else if (f instanceof QOM.Sum s) {
				return Cypher.sum(expression(s.$field()));
			}
			else if (f instanceof QOM.Avg s) {
				return Cypher.avg(expression(s.$field()));
			}
			else if (f instanceof QOM.StddevSamp s) {
				return Cypher.stDev(expression(s.$field()));
			}
			else if (f instanceof QOM.StddevPop s) {
				return Cypher.stDevP(expression(s.$field()));
			}
			else if (f instanceof QOM.Position p) {
				return Cypher.call(FUNCTION_MAPPING.get("strpos"))
					.withArgs(expression(p.$in()), expression(p.$arg2()))
					.asFunction();
			}
			else if (f instanceof QOM.ArrayGet<?> g && g.$arg1() instanceof TableField<?, ?> tf) {
				return Cypher.valueAt(expression(tf), expression(g.$arg2()));
			}
			else if (f instanceof QOM.CurrentTimestamp<?>) {
				return Cypher.localdatetime();
			}
			else {
				throw unsupported(f);
			}
		}

		private Expression buildFunction(QOM.Function<?> func) {
			Function<Field<?>, Expression> asExpression = v -> expression(v, true);
			var args = func.$args().stream().map(asExpression).toArray(Expression[]::new);
			return Cypher.call(FUNCTION_MAPPING.getOrDefault(func.getName().toLowerCase(Locale.ROOT), func.getName()))
				.withArgs(args)
				.asFunction();
		}

		@SuppressWarnings("squid:S3776") // Yep, this is complex.
		private <T> Condition condition(org.jooq.Condition c) {
			try {
				this.useAliasForVColumn.set(false);
				if (c instanceof QOM.And a) {
					return condition(a.$arg1()).and(condition(a.$arg2()));
				}
				else if (c instanceof QOM.Or o) {
					return condition(o.$arg1()).or(condition(o.$arg2()));
				}
				else if (c instanceof QOM.Xor o) {
					return condition(o.$arg1()).xor(condition(o.$arg2()));
				}
				else if (c instanceof QOM.Not o) {
					return condition(o.$arg1()).not();
				}
				else if (c instanceof QOM.Eq<?> e) {
					return expression(e.$arg1()).eq(expression(e.$arg2()));
				}
				else if (c instanceof QOM.Gt<?> e) {
					return expression(e.$arg1()).gt(expression(e.$arg2()));
				}
				else if (c instanceof QOM.Ge<?> e) {
					return expression(e.$arg1()).gte(expression(e.$arg2()));
				}
				else if (c instanceof QOM.Lt<?> e) {
					return expression(e.$arg1()).lt(expression(e.$arg2()));
				}
				else if (c instanceof QOM.Le<?> e) {
					return expression(e.$arg1()).lte(expression(e.$arg2()));
				}
				else if (c instanceof QOM.Between<?> e) {
					if (e.$symmetric()) {
						@SuppressWarnings("unchecked")
						QOM.Between<T> t = (QOM.Between<T>) e;
						return condition(t.$symmetric(false))
							.or(condition(t.$symmetric(false).$arg2(t.$arg3()).$arg3(t.$arg2())));
					}
					else {
						return expression(e.$arg2()).lte(expression(e.$arg1()))
							.and(expression(e.$arg1()).lte(expression(e.$arg3())));
					}
				}
				else if (c instanceof QOM.Ne<?> e) {
					return expression(e.$arg1()).ne(expression(e.$arg2()));
				}
				else if (c instanceof QOM.IsNull e) {
					return expression(e.$arg1()).isNull();
				}
				else if (c instanceof QOM.IsNotNull e) {
					return expression(e.$arg1()).isNotNull();
				}
				else if (c instanceof QOM.RowEq<?> e) {
					Condition result = null;

					for (int i = 0; i < e.$arg1().size(); i++) {
						Condition r = expression(e.$arg1().field(i)).eq(expression(e.$arg2().field(i)));
						result = (result != null) ? result.and(r) : r;
					}

					return result;
				}
				else if (c instanceof QOM.RowNe<?> e) {
					Condition result = null;

					for (int i = 0; i < e.$arg1().size(); i++) {
						Condition r = expression(e.$arg1().field(i)).ne(expression(e.$arg2().field(i)));
						result = (result != null) ? result.and(r) : r;
					}

					return result;
				}
				else if (c instanceof QOM.RowGt<?> e) {
					return rowCondition(e.$arg1(), e.$arg2(), Expression::gt, Expression::gt);
				}
				else if (c instanceof QOM.RowGe<?> e) {
					return rowCondition(e.$arg1(), e.$arg2(), Expression::gt, Expression::gte);
				}
				else if (c instanceof QOM.RowLt<?> e) {
					return rowCondition(e.$arg1(), e.$arg2(), Expression::lt, Expression::lt);
				}
				else if (c instanceof QOM.RowLe<?> e) {
					return rowCondition(e.$arg1(), e.$arg2(), Expression::lt, Expression::lte);
				}
				else if (c instanceof QOM.RowIsNull e) {
					return e.$arg1()
						.$fields()
						.stream()
						.map(f -> expression(f).isNull())
						.reduce(Condition::and)
						.orElseThrow();
				}
				else if (c instanceof QOM.RowIsNotNull e) {
					return e.$arg1()
						.$fields()
						.stream()
						.map(f -> expression(f).isNotNull())
						.reduce(Condition::and)
						.orElseThrow();
				}
				else if (c instanceof QOM.Like like) {
					return like(like);
				}
				else if (c instanceof QOM.FieldCondition fc && fc.$field() instanceof Param<Boolean> param) {
					return (Boolean.TRUE.equals(param.getValue()) ? Cypher.literalTrue() : Cypher.literalFalse())
						.asCondition();
				}
				else if (c instanceof QOM.InList<?> il) {
					return expression(il.$field())
						.in(Cypher.listOf(il.$list().stream().map(this::expression).toList()));
				}
				else {
					throw unsupported(c);
				}
			}
			finally {
				this.useAliasForVColumn.set(true);
			}
		}

		private Condition like(QOM.Like like) {
			Expression rhs;
			Expression lhs = expression(like.$arg1());
			if (like.$arg2() instanceof Param<?> p && p.$inline() && p.getValue() instanceof String s) {
				var sw = s.startsWith("%");
				var ew = s.endsWith("%");
				var length = s.length();
				var cnt = new LongSupplier() {
					Long value;

					@Override
					public long getAsLong() {
						if (this.value == null) {
							this.value = PERCENT_OR_UNDERSCORE.matcher(s).results().count();
						}
						return this.value;
					}
				};
				if (sw && ew && length > 2 && cnt.getAsLong() == 2) {
					return lhs.contains(Cypher.literalOf(s.substring(1, length - 1)));
				}
				else if (sw && length > 1 && cnt.getAsLong() == 1) {
					return lhs.endsWith(Cypher.literalOf(s.substring(1)));
				}
				else if (ew && length > 1 && cnt.getAsLong() == 1) {
					return lhs.startsWith(Cypher.literalOf(s.substring(0, length - 1)));
				}
				rhs = Cypher.literalOf(s.replaceAll("%+", ".*").replace("_", "."));
			}
			else {
				rhs = expression(like.$arg2());
			}
			return lhs.matches(rhs);
		}

		private Condition rowCondition(Row r1, Row r2,
				BiFunction<? super Expression, ? super Expression, ? extends Condition> comp,
				BiFunction<? super Expression, ? super Expression, ? extends Condition> last) {
			Condition result = last.apply(expression(r1.field(r1.size() - 1)), expression(r2.field(r1.size() - 1)));

			for (int i = r1.size() - 2; i >= 0; i--) {
				Expression e1 = expression(r1.field(i));
				Expression e2 = expression(r2.field(i));
				result = comp.apply(e1, e2).or(e1.eq(e2).and(result));
			}

			return result;
		}

		private List<PatternElement> resolveTableOrJoin(Table<?> t) {
			var relationship = this.resolvedRelationships.get(Cypher.name(t.getName()));
			if (relationship != null) {
				return List.of(relationship);
			}

			if (t instanceof QOM.JoinTable<?, ? extends Table<?>> joinTable) {
				return resolveJoin(joinTable);
			}

			if (t instanceof TableAlias<?> ta) {
				var patternElements = resolveTableOrJoin(ta.$aliased());
				var resolved = (patternElements.size() == 1) ? patternElements.get(0) : null;
				if ((resolved instanceof Node || resolved instanceof Relationship) && !ta.$alias().empty()) {
					return List.of(nodeOrPattern(ta.$aliased(), ta.$alias().last()));
				}
				else {
					throw unsupported(ta);
				}
			}
			else {
				return List.of(nodeOrPattern(t, t.getName()));
			}
		}

		private PatternElement nodeOrPattern(Table<?> t, String name) {
			var primaryLabel = labelOrType(t);
			var symbolicName = symbolicName(Objects.requireNonNull(name));
			if (primaryLabel.contains("_") && this.databaseMetaData != null) {
				var relationship = this.resolvedRelationships.get(symbolicName);
				if (relationship != null) {
					return relationship;
				}
				try {
					var resultSet = this.databaseMetaData.getTables(null, null, primaryLabel,
							new String[] { "RELATIONSHIP" });
					if (resultSet != null && resultSet.next()) {
						var definition = resultSet.getString("REMARKS").split("\n");
						relationship = Cypher.node(definition[0])
							.named("_lhs")
							.relationshipTo(Cypher.node(definition[2]).named("_rhs"), definition[1])
							.named(symbolicName);
						this.resolvedRelationships.put(symbolicName, relationship);
						return relationship;
					}
				}
				catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
			}
			return Cypher.node(primaryLabel).named(symbolicName);
		}

		@SuppressWarnings("squid:S3776") // Yep, this is complex.
		private List<PatternElement> resolveJoin(QOM.JoinTable<?, ? extends Table<?>> joinTable) {
			var join = JoinDetails.of(joinTable);

			String relType = null;
			SymbolicName relSymbolicName = null;

			PatternElement lhs;

			Table<?> t1 = joinTable.$table1();
			if (t1 instanceof QOM.JoinTable<?, ? extends Table<?>> lhsJoin) {
				lhs = resolveTableOrJoin(lhsJoin.$table1()).get(0);
				var eqJoin2 = JoinDetails.of(lhsJoin);
				var relationship = tryToIntegrateNodeAndVirtualTable(lhs, resolveTableOrJoin(lhsJoin.$table2()).get(0),
						eqJoin2.eq);
				if (relationship != null) {
					lhs = relationship;
				}
				else {
					relType = labelOrType(lhsJoin.$table2());
					if (lhsJoin.$table2() instanceof TableAlias<?> tableAlias) {
						relSymbolicName = symbolicName(tableAlias.getName());
					}
				}
			}
			else if (join.eq != null) {
				lhs = resolveTableOrJoin(t1).get(0);
				relType = type(t1, join.eq.$arg2());
			}
			else if (join.join != null && join.join.$using().isEmpty()) {
				throw unsupported(joinTable);
			}
			else {
				lhs = resolveTableOrJoin(t1).get(0);
				relType = (join.join != null) ? type(t1, join.join.$using().get(0)) : null;
			}

			if (relSymbolicName == null && relType != null) {
				relSymbolicName = symbolicName(relType);
			}

			PatternElement rhs = resolveTableOrJoin(joinTable.$table2()).get(0);

			if (lhs instanceof ExposesRelationships<?> from && rhs instanceof Node to) {
				var relationship = tryToIntegrateNodeAndVirtualTable(lhs, rhs, join.eq);
				if (relationship != null) {
					return List.of(relationship);
				}

				List<PatternElement> resolved = new ArrayList<>();

				// Figure out the left most driving table of the join and check if it's in
				// the relationship
				Table<?> leftMost = joinTable.$table1();
				while (leftMost instanceof QOM.JoinTable<?, ?> tab) {
					leftMost = tab.$table1();
				}
				var hlp = resolveTableOrJoin(leftMost).get(0);
				// We have one single previous relationship, the left most node matching
				// the leftmost table and the previous table is a join table.
				// Future safety check might actually be comparing the equals operators,
				// too.
				if (from instanceof Relationship r && hlp instanceof Node leftMostNode
						&& r.getLeft().getRequiredSymbolicName().equals(leftMostNode.getRequiredSymbolicName())
						&& joinTable.$table1() instanceof QOM.JoinTable<?, ?> previousJoinTable
						&& nodeOrPattern(previousJoinTable.$table2(),
								"ignored") instanceof Relationship targetRelationship) {
					resolved.add(lhs);
					from = leftMostNode;
					relType = targetRelationship.getDetails().getTypes().get(0);
				}

				var direction = Relationship.Direction.LTR;
				if (join.eq != null && joinTable.$table2() instanceof TableAlias<?> ta && !ta.$alias().empty()
						&& Objects.equals(ta.$alias().last(), join.eq.$arg2().getQualifiedName().first())) {
					direction = Relationship.Direction.RTL;
				}

				relationship = from.relationshipWith(to, direction, relType);
				if (relSymbolicName != null) {
					if (relationship instanceof Relationship r) {
						relationship = r.named(relSymbolicName);
					}
					else if (relationship instanceof RelationshipChain r) {
						relationship = r.named(relSymbolicName);
					}
					this.resolvedRelationships.put(relSymbolicName, relationship);
				}
				resolved.add(relationship);
				return resolved;
			}
			else {
				var relationship = tryToIntegrateNodeAndVirtualTable(lhs, rhs, join.eq);
				if (relationship != null) {
					return List.of(relationship);
				}
			}

			throw unsupported(joinTable);
		}

		@SuppressWarnings("squid:S3776") // Yep, this is complex.
		private RelationshipPattern tryToIntegrateNodeAndVirtualTable(PatternElement lhs, PatternElement rhs,
				QOM.Eq<?> eq) {

			Node node = (lhs instanceof Node hlp) ? hlp : (rhs instanceof Node hlp) ? hlp : null;
			Relationship rel = (lhs instanceof Relationship hlp) ? hlp : (rhs instanceof Relationship hlp) ? hlp : null;

			if (eq == null || node == null || rel == null) {
				return null;
			}

			if ((isElementId(eq.$arg1()) && anyLabelMatches(rel.getLeft(), eq.$arg2().$name().last()))
					|| (isElementId(eq.$arg2()) && anyLabelMatches(rel.getLeft(), eq.$arg1().$name().last()))) {
				var relationship = node
					.relationshipTo(rel.getRight(), rel.getDetails().getTypes().toArray(String[]::new))
					.named(rel.getRequiredSymbolicName());
				this.resolvedRelationships.put(relationship.getRequiredSymbolicName(), relationship);
				return relationship;
			}
			else if ((isElementId(eq.$arg1()) && anyLabelMatches(rel.getRight(), eq.$arg2().$name().last()))
					|| (isElementId(eq.$arg2()) && anyLabelMatches(rel.getRight(), eq.$arg1().$name().last()))) {
				var relationship = rel.getLeft()
					.relationshipTo(node, rel.getDetails().getTypes().toArray(String[]::new))
					.named(rel.getRequiredSymbolicName());
				this.resolvedRelationships.put(relationship.getRequiredSymbolicName(), relationship);
				return relationship;
			}
			else if (fitsLeftOrRight(node, rel)) {
				try {
					this.useAliasForVColumn.set(false);
					Expression e1 = null;
					Expression e2 = null;
					Relationship relationship = null;
					if (isElementIdFor(eq.$arg1(), rel.getLeft())) {
						relationship = node
							.relationshipTo(rel.getRight(), rel.getDetails().getTypes().toArray(String[]::new))
							.named(rel.getRequiredSymbolicName());
						e1 = makeId(relationship.getLeft(), null);
						e2 = makeId(relationship, null);
					}
					else if (isElementIdFor(eq.$arg1(), rel.getRight())) {
						relationship = rel.getLeft()
							.relationshipTo(node, rel.getDetails().getTypes().toArray(String[]::new))
							.named(rel.getRequiredSymbolicName());
						e1 = makeId(relationship.getRight(), null);
						e2 = makeId(relationship, null);
					}
					if (relationship != null) {
						relationship = (Relationship) relationship.where(Cypher.isEqualTo(e1, e2));
						this.resolvedRelationships.put(relationship.getRequiredSymbolicName(), relationship);
						return relationship;
					}
				}
				finally {
					this.useAliasForVColumn.set(true);
				}
			}

			return null;
		}

		static boolean isElementId(Field<?> field) {
			var matcher = ELEMENT_ID_PATTERN.matcher(Objects.requireNonNull(field.$name().last()));
			if (!matcher.matches()) {
				return false;
			}
			var prefix = matcher.group("prefix");
			return prefix == null || prefix.isBlank() || "element".equalsIgnoreCase(prefix);
		}

		static boolean isElementIdFor(Field<?> field, Node node) {
			if (!isElementId(field)) {
				return false;
			}

			return node.getLabels().stream().anyMatch(l -> l.getValue().equals(field.$name().first()));
		}

		static String isFkId(Field<?> field) {
			var matcher = ELEMENT_ID_PATTERN.matcher(Objects.requireNonNull(field.$name().last()));
			if (!matcher.matches()) {
				return null;
			}
			var prefix = matcher.group("prefix");
			return (prefix != null && !prefix.isBlank()) ? prefix : null;
		}

		private static boolean fitsLeftOrRight(Node node, Relationship relationship) {
			boolean result = node.getLabels()
				.stream()
				.map(NodeLabel::getValue)
				.anyMatch(l -> relationship.getLeft()
					.getLabels()
					.stream()
					.map(NodeLabel::getValue)
					.anyMatch(r -> r.equals(l)));
			if (result) {
				return result;
			}
			return node.getLabels()
				.stream()
				.map(NodeLabel::getValue)
				.anyMatch(l -> relationship.getRight()
					.getLabels()
					.stream()
					.map(NodeLabel::getValue)
					.anyMatch(r -> r.equals(l)));
		}

		private static boolean anyLabelMatches(Node node, String needle) {
			if (needle == null) {
				return false;
			}

			return node.getLabels()
				.stream()
				.map(NodeLabel::getValue)
				.map("v$%s_id"::formatted)
				.anyMatch(needle::equalsIgnoreCase);
		}

		private String labelOrType(Table<?> tableOrAlias) {

			var t = (tableOrAlias instanceof TableAlias<?> ta) ? ta.$aliased() : tableOrAlias;
			var comment = t.getComment();
			if (!comment.isBlank()) {
				var config = Arrays.stream(comment.split(","))
					.map(s -> s.split("="))
					.collect(Collectors.toMap(a -> a[0], a -> a[1]));
				return config.getOrDefault("label", t.getName());
			}

			return this.config.getTableToLabelMappings()
				.entrySet()
				.stream()
				.filter(e -> e.getKey().equalsIgnoreCase(t.getName()))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElseGet(t::getName);
		}

		private String type(Table<?> tableOrAlias, Field<?> field) {
			var t = (tableOrAlias instanceof TableAlias<?> ta) ? ta.$aliased() : tableOrAlias;
			var key = t.getName() + "." + field.getName();

			return this.config.getJoinColumnsToTypeMappings()
				.entrySet()
				.stream()
				.filter(e -> e.getKey().equalsIgnoreCase(key))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElseGet(() -> relationshipTypeName(field));
		}

	}

	record CbvPointer(Table<?> table, String viewName, String alias) {
		static CbvPointer of(Table<?> table) {
			var name = table.$name().first();
			var alias = name;
			if (table instanceof TableAlias<?> tableAlias) {
				name = tableAlias.$aliased().$name().first();
			}
			return new CbvPointer(table, name, alias);
		}
	}

}
