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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooq.SQLDialect;
import org.jooq.conf.ParseNameCase;
import org.jooq.conf.ParseUnknownFunctions;
import org.jooq.conf.ParseWithMetaLookups;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.neo4j.jdbc.translator.spi.Translator;

/**
 * Configuration for the {@link SqlToCypher}, use this to configure parsing and rendering
 * settings as well as table to node mappings.
 *
 * @author Michael Hunger
 * @author Michael J. Simons
 */
public final class SqlToCypherConfig {

	/**
	 * A constant property name to make the translator always escape literal names.
	 * @deprecated no replacement
	 */
	@Deprecated(forRemoval = true, since = "6.5.0")
	public static final String PROPERTY_ALWAYS_ESCAPE_NAMES = "s2c.alwaysEscapeNames";

	/**
	 * A constant property name for enabling pretty formatted Cypher statements.
	 * @deprecated no replacement
	 */
	@Deprecated(forRemoval = true, since = "6.5.0")
	public static final String PROPERTY_PRETTY_PRINT_CYPHER = "s2c.prettyPrint";

	/**
	 * A constant property name for enabling the local translator cache.
	 * @deprecated no replacement
	 */
	@Deprecated(forRemoval = true, since = "6.5.0")
	public static final String PROPERTY_ENABLE_CACHE = "s2c.enableCache";

	private static final SqlToCypherConfig DEFAULT_CONFIG = SqlToCypherConfig.builder().build();

	// Don't want to use the deprecated properties, better repeat them here
	@SuppressWarnings("squid:S1192")
	private static final Map<String, String> DRIVER_CONFIG_TO_TRANSLATOR_CONFIG_MAPPING = Map.of("cacheSQLTranslations",
			"s2c.enableCache", "viewDefinitions", "s2c.viewDefinitions");

	/**
	 * Derives a configuration for {@code Sql2Cypher} based from the properties given.
	 * @param config will be searched for values under keys prefixed with {@code s2c}.
	 * @return a new configuration object or the default config if there are no matching
	 * properties.
	 */
	public static SqlToCypherConfig of(Map<String, ?> config) {

		if (config == null || config.isEmpty()) {
			return defaultConfig();
		}

		var localConfig = new HashMap<String, Object>();
		config.forEach((k, v) -> localConfig.put(DRIVER_CONFIG_TO_TRANSLATOR_CONFIG_MAPPING.getOrDefault(k, k), v));

		var prefix = Pattern.compile("s2c\\.(.+)");

		var relevantProperties = localConfig.keySet().stream().map(prefix::matcher).filter(Matcher::matches).toList();
		if (relevantProperties.isEmpty()) {
			return defaultConfig();
		}

		var builder = builder();
		var dashWord = Pattern.compile("-(\\w)");
		boolean customConfig = false;
		for (Matcher m : relevantProperties) {
			var v = localConfig.get(m.group());
			var k = dashWord.matcher(m.group(1)).replaceAll(mr -> mr.group(1).toUpperCase(Locale.ROOT));
			customConfig = null != switch (k) {
				case "parseNameCase" -> builder.withParseNameCase(toEnum(ParseNameCase.class, v));
				case "renderNameCase" -> builder.withRenderNameCase(toEnum(RenderNameCase.class, v));
				case "jooqDiagnosticLogging" -> builder.withJooqDiagnosticLogging(toBoolean(v));
				case "tableToLabelMappings" -> builder.withTableToLabelMappings(toMap(v));
				case "joinColumnsToTypeMappings" -> builder.withJoinColumnsToTypeMappings(toMap(v));
				case "sqlDialect" -> builder.withSqlDialect(SQLDialect.valueOf(toString(v)));
				case "prettyPrint" -> builder.withPrettyPrint(toBoolean(v));
				case "alwaysEscapeNames" -> builder.withAlwaysEscapeNames(toBoolean(v));
				case "parseNamedParamPrefix" -> builder.withParseNamedParamPrefix(toString(v));
				case "enableCache" -> builder.withCacheEnabled(toBoolean(v));
				case "precedence" -> builder.withPrecedence(toInteger(v));
				case "viewDefinitions" -> builder.withViewDefinitions(toString(v));
				default -> {
					SqlToCypher.LOGGER.log(Level.WARNING, "Unknown config option {0}", m.group());
					yield null;
				}
			};
		}

		return customConfig ? builder.build() : defaultConfig();
	}

	@SuppressWarnings("unchecked")
	static Map<String, String> toMap(Object value) {
		if (value instanceof Map<?, ?> map) {
			return (Map<String, String>) map;
		}
		else if (value instanceof String source) {
			return buildMap(source);
		}
		else if (value == null) {
			throw new IllegalArgumentException("Unsupported Map<String, String> representation representation null");
		}
		else {
			throw new IllegalArgumentException("Unsupported Map<String, String> representation " + value.getClass());
		}
	}

	static <T extends Enum<T>> T toEnum(Class<T> enumType, Object value) {
		if (enumType.isInstance(value)) {
			return enumType.cast(value);
		}
		else if (value instanceof String s) {
			return Enum.valueOf(enumType, s);
		}
		else if (value == null) {
			throw new IllegalArgumentException("Unsupported enum representation null");
		}
		else {
			throw new IllegalArgumentException(
					"Unsupported enum representation " + value.getClass() + " for " + enumType.getName());
		}
	}

	static String toString(Object val) {
		if (val instanceof String s) {
			return s;
		}
		return (val != null) ? val.toString() : "";
	}

	static boolean toBoolean(Object val) {
		if (val instanceof String s) {
			return Boolean.parseBoolean(s);
		}
		else if (val instanceof Boolean b) {
			return b;
		}
		else if (val == null) {
			throw new IllegalArgumentException("Unsupported boolean representation null");
		}
		else {
			throw new IllegalArgumentException("Unsupported boolean representation " + val.getClass());
		}
	}

	static Integer toInteger(Object val) {

		Objects.requireNonNull(val, "Unsupported Integer representation null");
		if (val instanceof Integer integer) {
			return integer;
		}
		else if (val instanceof String s) {
			try {
				return Integer.parseInt(s);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Unsupported Integer representation `%s`".formatted(s), ex);
			}
		}

		throw new IllegalArgumentException("Unsupported Integer representation " + val.getClass());
	}

	/**
	 * Builds a map from a string. String must be in {@code k1:v1;k2:v2} format.
	 * @param source the source of the map
	 * @return a new, unmodifiable map
	 */
	static Map<String, String> buildMap(String source) {

		return Arrays.stream(source.split(";"))
			.map(String::trim)
			.map(s -> s.split(":"))
			.collect(Collectors.toUnmodifiableMap(a -> a[0], a -> a[1]));
	}

	/**
	 * A builder for creating new {@link SqlToCypherConfig configuration objects}.
	 * @return a new builder for creating a new configuration from scratch.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Provides access to the default configuration.
	 * @return the default configuration ready to use.
	 */
	public static SqlToCypherConfig defaultConfig() {

		return DEFAULT_CONFIG;
	}

	private final ParseNameCase parseNameCase;

	private final RenderNameCase renderNameCase;

	private final boolean jooqDiagnosticLogging;

	private final Map<String, String> tableToLabelMappings;

	private final Map<String, String> joinColumnsToTypeMappings;

	private final SQLDialect sqlDialect;

	private final boolean prettyPrint;

	private final boolean alwaysEscapeNames;

	private final String parseNamedParamPrefix;

	private final boolean cacheEnabled;

	private final Integer precedence;

	private final String viewDefinitions;

	private SqlToCypherConfig(Builder builder) {

		this.parseNameCase = builder.parseNameCase;
		this.renderNameCase = builder.renderNameCase;
		this.jooqDiagnosticLogging = builder.jooqDiagnosticLogging;
		this.tableToLabelMappings = builder.tableToLabelMappings;
		this.joinColumnsToTypeMappings = builder.joinColumnsToTypeMappings;
		this.sqlDialect = builder.sqlDialect;
		this.prettyPrint = builder.prettyPrint;
		this.alwaysEscapeNames = builder.alwaysEscapeNames;
		this.parseNamedParamPrefix = builder.parseNamedParamPrefix;
		this.cacheEnabled = builder.enableCache;
		this.precedence = builder.precedence;
		this.viewDefinitions = builder.viewDefinitions;
	}

	/**
	 * Allows modifying this configuration.
	 * @return builder with all settings from this instance
	 */
	public Builder modify() {
		return new Builder(this);
	}

	/**
	 * Returns the case in which names are parsed.
	 * @return the case in which names are parsed
	 */
	public ParseNameCase getParseNameCase() {
		return this.parseNameCase;
	}

	/**
	 * Returns the case in which names are rendered.
	 * @return the case in which names are rendered
	 */
	public RenderNameCase getRenderNameCase() {
		return this.renderNameCase;
	}

	/**
	 * Returns whether jOOQ diagnostic logging is enabled.
	 * @return whether jOOQ diagnostic logging is enabled
	 */
	public boolean isJooqDiagnosticLogging() {
		return this.jooqDiagnosticLogging;
	}

	/**
	 * Returns the mapping from table names to labels.
	 * @return the mapping from table names to labels
	 */
	public Map<String, String> getTableToLabelMappings() {
		return this.tableToLabelMappings;
	}

	/**
	 * Returns the mapping from join columns to relationship types.
	 * @return the mapping from join columns to relationship types
	 */
	public Map<String, String> getJoinColumnsToTypeMappings() {
		return this.joinColumnsToTypeMappings;
	}

	/**
	 * Returns the configured SQL dialect for parsing.
	 * @return the configured SQL dialect for parsing
	 */
	public SQLDialect getSqlDialect() {
		return this.sqlDialect;
	}

	/**
	 * Returns whether pretty printing of Cypher statements is enabled or not.
	 * @return whether pretty printing of Cypher statements is enabled or not
	 */
	public boolean isPrettyPrint() {
		return this.prettyPrint;
	}

	/**
	 * Returns whether Cypher names are always escapd or not.
	 * @return whether Cypher names are always escapd or not
	 */
	public boolean isAlwaysEscapeNames() {
		return this.alwaysEscapeNames;
	}

	/**
	 * Returns the prefixed that is recognized when parsing named parameters.
	 * @return the prefixed that is recognized when parsing named parameters
	 */
	public String getParseNamedParamPrefix() {
		return this.parseNamedParamPrefix;
	}

	/**
	 * Returns whether the internal cache is enabled or not.
	 * @return whether the internal cache is enabled or not
	 */
	public boolean isCacheEnabled() {
		return this.cacheEnabled;
	}

	/**
	 * Returns the precedence of the translator.
	 * @return the precedence of the translator
	 */
	public Integer getPrecedence() {
		return this.precedence;
	}

	/**
	 * Optional resource pointing to a valid view definition file in JSON format.
	 * @return an optional resource containing view definitions in JSON format
	 * @since 6.5.0
	 */
	public String getViewDefinitions() {
		return this.viewDefinitions;
	}

	/**
	 * Converts this configuration into jOOQ settings.
	 * @return jOOQ Settings
	 * @deprecated No replacement, not to be used externally
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(forRemoval = true, since = "6.4.0")
	public Settings asSettings() {
		return asSettings(ParseWithMetaLookups.IGNORE_ON_FAILURE);
	}

	/**
	 * Converts this configuration into jOOQ settings.
	 * @param withMetaLookups wether to use configurable lookups or not
	 * @return jOOQ Settings
	 * @deprecated No replacement, not to be used externally
	 */
	@Deprecated(forRemoval = true, since = "6.4.0")
	public Settings asSettings(ParseWithMetaLookups withMetaLookups) {
		return new DefaultConfiguration().settings()
			.withParseNameCase(getParseNameCase())
			.withRenderNameCase(getRenderNameCase())
			.withParseWithMetaLookups(withMetaLookups)
			.withDiagnosticsLogging(isJooqDiagnosticLogging())
			.withParseUnknownFunctions(ParseUnknownFunctions.IGNORE)
			.withParseDialect(getSqlDialect());
	}

	/**
	 * A builder to create new instances of {@link SqlToCypherConfig configurations}.
	 */
	public static final class Builder {

		private ParseNameCase parseNameCase;

		private RenderNameCase renderNameCase;

		private boolean jooqDiagnosticLogging;

		private Map<String, String> tableToLabelMappings;

		private Map<String, String> joinColumnsToTypeMappings;

		private SQLDialect sqlDialect;

		private boolean prettyPrint;

		private String parseNamedParamPrefix;

		private boolean alwaysEscapeNames;

		private boolean enableCache;

		private Integer precedence;

		private String viewDefinitions;

		private Builder() {
			this(ParseNameCase.AS_IS, RenderNameCase.AS_IS, false, Map.of(), Map.of(), SQLDialect.DEFAULT, false, false,
					null, false, Translator.LOWEST_PRECEDENCE, null);
		}

		private Builder(SqlToCypherConfig config) {
			this(config.parseNameCase, config.renderNameCase, config.jooqDiagnosticLogging, config.tableToLabelMappings,
					config.joinColumnsToTypeMappings, config.sqlDialect, config.prettyPrint, config.alwaysEscapeNames,
					config.parseNamedParamPrefix, config.cacheEnabled, config.precedence, config.viewDefinitions);
		}

		private Builder(ParseNameCase parseNameCase, RenderNameCase renderNameCase, boolean jooqDiagnosticLogging,
				Map<String, String> tableToLabelMappings, Map<String, String> joinColumnsToTypeMappings,
				SQLDialect sqlDialect, boolean prettyPrint, boolean alwaysEscapeNames, String parseNamedParamPrefix,
				boolean enableCache, Integer precedence, String viewDefinitions) {
			this.parseNameCase = parseNameCase;
			this.renderNameCase = renderNameCase;
			this.jooqDiagnosticLogging = jooqDiagnosticLogging;
			this.tableToLabelMappings = tableToLabelMappings;
			this.joinColumnsToTypeMappings = joinColumnsToTypeMappings;
			this.sqlDialect = sqlDialect;
			this.prettyPrint = prettyPrint;
			this.alwaysEscapeNames = alwaysEscapeNames;
			this.parseNamedParamPrefix = parseNamedParamPrefix;
			this.enableCache = enableCache;
			this.precedence = precedence;
			this.viewDefinitions = viewDefinitions;
		}

		/**
		 * Configures how names should be parsed.
		 * @param newParseNameCase the new configuration
		 * @return this builder
		 */
		public Builder withParseNameCase(ParseNameCase newParseNameCase) {
			this.parseNameCase = Objects.requireNonNull(newParseNameCase);
			return this;
		}

		/**
		 * Configures how SQL names should be parsed.
		 * @param newRenderNameCase the new configuration
		 * @return this builder
		 */
		public Builder withRenderNameCase(RenderNameCase newRenderNameCase) {
			this.renderNameCase = Objects.requireNonNull(newRenderNameCase);
			return this;
		}

		/**
		 * Enables diagnostic logging for jOOQ.
		 * @param enabled set to {@literal true} to enable diagnostic logging on the jOOQ
		 * side of things
		 * @return this builder
		 */
		public Builder withJooqDiagnosticLogging(boolean enabled) {
			this.jooqDiagnosticLogging = enabled;
			return this;
		}

		/**
		 * Applies new table mappings.
		 * @param newTableToLabelMappings the new mappings
		 * @return this builder
		 */
		public Builder withTableToLabelMappings(Map<String, String> newTableToLabelMappings) {
			this.tableToLabelMappings = Map.copyOf(Objects.requireNonNull(newTableToLabelMappings));
			return this;
		}

		/**
		 * Applies new join column mappings.
		 * @param newJoinColumnsToTypeMappings the new mappings
		 * @return this builder
		 */
		public Builder withJoinColumnsToTypeMappings(Map<String, String> newJoinColumnsToTypeMappings) {
			this.joinColumnsToTypeMappings = Map.copyOf(Objects.requireNonNull(newJoinColumnsToTypeMappings));
			return this;
		}

		/**
		 * Applies a new {@link SQLDialect} for both parsing and optionally rendering SQL.
		 * @param newSqlDialect the new sql dialect
		 * @return this builder
		 */
		public Builder withSqlDialect(SQLDialect newSqlDialect) {
			this.sqlDialect = Objects.requireNonNull(newSqlDialect);
			return this;
		}

		/**
		 * Enables or disables pretty printing of the generated Cypher queries.
		 * @param prettyPrint set to {@literal false} to disable pretty printing
		 * @return this builder
		 */
		public Builder withPrettyPrint(boolean prettyPrint) {
			this.prettyPrint = prettyPrint;
			return this;
		}

		/**
		 * Changes the prefix used for parsing named parameters. If set to
		 * {@literal null}, the jOOQ default ({@literal :}) is used.
		 * @param parseNamedParamPrefix the new prefix for parsing named parameters
		 * @return this builder
		 */
		public Builder withParseNamedParamPrefix(String parseNamedParamPrefix) {
			this.parseNamedParamPrefix = parseNamedParamPrefix;
			return this;
		}

		/**
		 * Configure whether names should be always escaped.
		 * @param alwaysEscapeNames use {@literal true} to always escape names
		 * @return this builder
		 */
		public Builder withAlwaysEscapeNames(boolean alwaysEscapeNames) {
			this.alwaysEscapeNames = alwaysEscapeNames;
			return this;
		}

		/**
		 * Enables caching.
		 * @param cacheSetting use {@literal true} to enable caching
		 * @return this builder
		 */
		public Builder withCacheEnabled(boolean cacheSetting) {
			this.enableCache = cacheSetting;
			return this;
		}

		/**
		 * Finishes building a new configuration. The builder is safe to reuse afterward.
		 * @return a new immutable configuration
		 */
		public SqlToCypherConfig build() {
			return new SqlToCypherConfig(this);
		}

		/**
		 * Configures the precedence for the {@link SqlToCypher instance} to be
		 * configured. Lower values means higher precedence.
		 * @param newPrecedence the precedence to be configured.
		 * @return this builder
		 */
		public Builder withPrecedence(Integer newPrecedence) {
			this.precedence = newPrecedence;
			return this;
		}

		/**
		 * Configures the view definitions to use.
		 * @param viewDefinitions the view definitions to use, {@literal null} disables
		 * view resolution
		 * @return this builder
		 * @since 6.5.0
		 */
		public Builder withViewDefinitions(String viewDefinitions) {
			this.viewDefinitions = viewDefinitions;
			return this;
		}

	}

}
