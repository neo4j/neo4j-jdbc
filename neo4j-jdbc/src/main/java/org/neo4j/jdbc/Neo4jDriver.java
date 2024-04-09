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
package org.neo4j.jdbc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.github.cdimascio.dotenv.Dotenv;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.neo4j.jdbc.internal.bolt.AuthTokens;
import org.neo4j.jdbc.internal.bolt.BoltAgentUtil;
import org.neo4j.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.jdbc.internal.bolt.BoltConnectionProviders;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.jdbc.internal.bolt.SecurityPlan;
import org.neo4j.jdbc.internal.bolt.SecurityPlans;
import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;

/**
 * The main entry point for the Neo4j JDBC driver. There is usually little need to use
 * this class directly, it registers automatically with the {@link DriverManager}.
 *
 * @author Michael J. Simons
 * @author Rouven Bauer
 * @since 6.0.0
 */
public final class Neo4jDriver implements Neo4jDriverExtensions {

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the host.
	 */
	public static final String PROPERTY_HOST = "host";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the port.
	 */
	public static final String PROPERTY_PORT = "port";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the username.
	 */
	public static final String PROPERTY_USER = "user";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the database.
	 */
	public static final String PROPERTY_DATABASE = "database";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the user-agent.
	 */
	public static final String PROPERTY_USER_AGENT = "agent";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the password.
	 */
	public static final String PROPERTY_PASSWORD = "password";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the auth scheme.
	 * <p>
	 * Currently supported values are:
	 * <ul>
	 * <li>{@literal "none"} for no authentication. The properties {@link #PROPERTY_USER},
	 * {@link #PROPERTY_PASSWORD} and {@link #PROPERTY_AUTH_REALM} have no effect.
	 * <li>{@literal "basic"} (default) for basic authentication.
	 * <li>{@literal "bearer"} for bearer authentication (SSO). {@link #PROPERTY_PASSWORD}
	 * should be set to the bearer token; {@link #PROPERTY_USER} and
	 * {@link #PROPERTY_AUTH_REALM} have no effect.
	 * <li>{@literal "kerberos"} for kerberos authentication. Requires
	 * {@link #PROPERTY_PASSWORD} to be set to the kerberos ticket; {@link #PROPERTY_USER}
	 * and {@link #PROPERTY_AUTH_REALM} have no effect.
	 * </ul>
	 */
	public static final String PROPERTY_AUTH_SCHEME = "authScheme";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the auth realm.
	 */
	public static final String PROPERTY_AUTH_REALM = "authRealm";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name}
	 * containing the timeout.
	 */
	public static final String PROPERTY_TIMEOUT = "timeout";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} used to
	 * enable automatic SQL to Cypher translation.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_ENABLED = "enableSQLTranslation";

	/**
	 * This property can be used when you want to use Cypher with quotation marks
	 * ({@literal ?}) as placeholders. This can happen when you actually want to pass
	 * Cypher to the driver but the Cypher gets preprocessed by another tooling down the
	 * line. If this property is set to {@literal true}, replacement will happen after all
	 * translators have been applied. However, it usually does not make sense to enable it
	 * in that case.
	 */
	public static final String PROPERTY_REWRITE_PLACEHOLDERS = "rewritePlaceholders";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} used to
	 * enable automatic translation caching.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_CACHING_ENABLED = "cacheSQLTranslations";

	/**
	 * This is an alternative to the automatic configuration of translator factories and
	 * can be applied to load a single translator. This is helpful in scenarios in which
	 * an isolated class-loader is used, that prohibits access to the ServiceLoader
	 * machinery.
	 */
	public static final String PROPERTY_TRANSLATOR_FACTORY = "translatorFactory";

	private static final String PROPERTY_S2C_ALWAYS_ESCAPE_NAMES = "s2c.alwaysEscapeNames";

	private static final String PROPERTY_S2C_PRETTY_PRINT_CYPHER = "s2c.prettyPrint";

	private static final String PROPERTY_S2C_ENABLE_CACHE = "s2c.enableCache";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} that
	 * enables automatic rewrite of batched prepared statements into batched Cypher
	 * statements.
	 */
	public static final String PROPERTY_REWRITE_BATCHED_STATEMENTS = "rewriteBatchedStatements";

	private static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(new DriverThreadFactory());

	private static final String URL_REGEX = "^jdbc:neo4j(?:\\+(?<transport>s(?:sc)?)?)?://(?<host>[^:/?]+):?(?<port>\\d+)?/?(?<database>[^?]+)?\\??(?<urlParams>\\S+)?$";

	private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

	/*
	 * This is the recommended - and AFAIK - required way to register a new driver the
	 * default way. The driver manager will use the service loader mechanism to load all
	 * drivers and that in turn will trigger the static initializer block. We would also
	 * have the chance here to register a <code>DriverAction</code> that will be notified
	 * on deregistration.
	 */
	static {
		try {
			DriverManager.registerDriver(new Neo4jDriver());
		}
		catch (SQLException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	private final BoltConnectionProvider boltConnectionProvider;

	private volatile List<TranslatorFactory> sqlTranslatorFactories;

	/**
	 * Lets you configure the driver from the environment, but always enable SQL to Cypher
	 * translation.
	 * @return a builder that lets you create a driver from the environment.
	 * @see SpecifyTranslationStep
	 */
	public static SpecifyAdditionalPropertiesStep withSQLTranslation() {
		return new BuilderImpl(true, Map.of());
	}

	/**
	 * Lets you configure the driver from the environment, with additional properties
	 * being applied as well.
	 * @param additionalProperties additional properties to be added to the configuration
	 * @return a builder that lets you create a driver from the environment.
	 * @see SpecifyAdditionalPropertiesStep
	 */
	public static SpecifyTranslationStep withProperties(Map<String, Object> additionalProperties) {
		return new BuilderImpl(false, additionalProperties);
	}

	/**
	 * Configures the driver from the environment.
	 * @return a connection when the environment contains at least a supported URL under
	 * the key {@literal NEO4J_URI}.
	 * @throws SQLException any error that might happen
	 * @see SpecifyEnvStep#fromEnv()
	 */
	public static Optional<Connection> fromEnv() throws SQLException {
		return fromEnv(null, null);
	}

	/**
	 * Configures the driver from the environment.
	 * @param directory an optional directory to look for .env files
	 * @return a connection when the environment contains at least a supported URL under
	 * the key {@literal NEO4J_URI}.
	 * @throws SQLException any error that might happen
	 * @see SpecifyEnvStep#fromEnv(Path)
	 */
	public static Optional<Connection> fromEnv(Path directory) throws SQLException {
		return fromEnv(directory, null);
	}

	/**
	 * Configures the driver from the environment.
	 * @param filename an alternative filename for the .env file
	 * @return a connection when the environment contains at least a supported URL under
	 * the key {@literal NEO4J_URI}.
	 * @throws SQLException any error that might happen
	 * @see SpecifyEnvStep#fromEnv(String)
	 */
	public static Optional<Connection> fromEnv(String filename) throws SQLException {
		return fromEnv(null, filename);
	}

	/**
	 * Configures the driver from the environment.
	 * @param directory an optional directory to look for .env files
	 * @param filename an alternative filename for the .env file
	 * @return a connection when the environment contains at least a supported URL under
	 * the key {@literal NEO4J_URI}.
	 * @throws SQLException any error that might happen
	 * @see SpecifyEnvStep#fromEnv(Path, String)
	 */
	public static Optional<Connection> fromEnv(Path directory, String filename) throws SQLException {
		return new BuilderImpl(false, Map.of()).fromEnv(directory, filename);
	}

	/**
	 * Creates a new instance of the {@link Neo4jDriver}. The instance is usable and is
	 * able to provide connections. The public constructor is provided mainly for tooling
	 * that directly accesses vendor specific classes and circumvents the service loader
	 * machinery for JDBC.
	 */
	public Neo4jDriver() {
		this(BoltConnectionProviders.netty(eventLoopGroup, Clock.systemUTC()));
	}

	Neo4jDriver(BoltConnectionProvider boltConnectionProvider) {
		this.boltConnectionProvider = boltConnectionProvider;
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		var driverConfig = parseConfig(url, info);

		var address = new BoltServerAddress(driverConfig.host, driverConfig.port);

		var securityPlan = parseSSLParams(driverConfig.sslProperties);

		var databaseName = driverConfig.database;

		var user = (driverConfig.user == null || driverConfig.user.isBlank()) ? "" : driverConfig.user;
		var password = (driverConfig.password == null || driverConfig.password.isBlank()) ? "" : driverConfig.password;
		var authRealm = (driverConfig.authRealm == null || driverConfig.authRealm.isBlank()) ? null
				: driverConfig.authRealm;
		var authToken = switch (driverConfig.authScheme) {
			case NONE -> AuthTokens.none();
			case BASIC -> AuthTokens.basic(user, password, authRealm);
			case BEARER -> AuthTokens.bearer(password);
			case KERBEROS -> AuthTokens.kerberos(password);
		};

		var boltAgent = BoltAgentUtil.boltAgent();
		var userAgent = driverConfig.agent;
		var connectTimeoutMillis = driverConfig.timeout;

		var boltConnection = this.boltConnectionProvider
			.connect(address, securityPlan, databaseName, authToken, boltAgent, userAgent, connectTimeoutMillis)
			.toCompletableFuture()
			.join();

		var enableSqlTranslation = driverConfig.enableSQLTranslation;
		var enableTranslationCaching = driverConfig.enableTranslationCaching;
		var rewriteBatchedStatements = driverConfig.rewriteBatchedStatements;
		var rewritePlaceholders = driverConfig.rewritePlaceholders;
		var translatorFactory = driverConfig.misc.get(PROPERTY_TRANSLATOR_FACTORY);

		Supplier<List<TranslatorFactory>> translatorFactoriesSupplier = this::getSqlTranslatorFactories;
		if (translatorFactory != null && !translatorFactory.isBlank()) {
			translatorFactoriesSupplier = () -> getSqlTranslatorFactory(translatorFactory);
		}
		return new ConnectionImpl(boltConnection,
				getSqlTranslatorSupplier(enableSqlTranslation, driverConfig.rawConfig(), translatorFactoriesSupplier),
				enableSqlTranslation, enableTranslationCaching, rewriteBatchedStatements, rewritePlaceholders);
	}

	static String getDefaultUserAgent() {
		return "neo4j-jdbc/%s".formatted(ProductVersion.getValue());
	}

	static Map<String, String> mergeConfig(String[] urlParams, Properties jdbcProperties) {
		var result = new HashMap<String, String>();
		for (Object key : jdbcProperties.keySet()) {
			if (key instanceof String name) {
				result.put(name, jdbcProperties.getProperty(name));
			}
		}
		var regex = "^(?<name>\\S+)=(?<value>\\S+)$";
		var pattern = Pattern.compile(regex);
		for (String param : urlParams) {
			var matcher = pattern.matcher(param);
			if (matcher.matches()) {
				var name = URLDecoder.decode(matcher.group("name"), StandardCharsets.UTF_8);
				var value = URLDecoder.decode(matcher.group("value"), StandardCharsets.UTF_8);
				result.put(name, value);
			}
		}

		return Map.copyOf(result);
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		if (url == null) {
			throw new SQLException("url cannot be null");
		}

		return URL_PATTERN.matcher(url).matches();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {

		var parsedConfig = parseConfig(url, info);
		var driverPropertyInfos = new ArrayList<DriverPropertyInfo>();
		var trueFalseChoices = new String[] { "true", "false" };

		var hostPropInfo = new DriverPropertyInfo(PROPERTY_HOST, parsedConfig.host);
		hostPropInfo.description = "The host name";
		hostPropInfo.required = true;
		driverPropertyInfos.add(hostPropInfo);

		var portPropInfo = new DriverPropertyInfo(PROPERTY_PORT, String.valueOf(parsedConfig.port));
		portPropInfo.description = "The port";
		portPropInfo.required = true;
		driverPropertyInfos.add(portPropInfo);

		var databaseNameInfo = new DriverPropertyInfo(PROPERTY_DATABASE, parsedConfig.database);
		databaseNameInfo.description = "The database name to connect to. Will default to neo4j if left blank.";
		databaseNameInfo.required = false;
		driverPropertyInfos.add(databaseNameInfo);

		var userPropInfo = new DriverPropertyInfo(PROPERTY_USER, parsedConfig.user);
		userPropInfo.description = "The user that will be used to connect. Will be defaulted to neo4j if left blank.";
		userPropInfo.required = false;
		driverPropertyInfos.add(userPropInfo);

		var passwordPropInfo = new DriverPropertyInfo(PROPERTY_PASSWORD, parsedConfig.password);
		passwordPropInfo.description = "The password that is used to connect. Defaults to 'password'.";
		passwordPropInfo.required = false;
		driverPropertyInfos.add(passwordPropInfo);

		var authSchemePropInfo = new DriverPropertyInfo(PROPERTY_AUTH_SCHEME, parsedConfig.authScheme.getName());
		authSchemePropInfo.description = "The authentication scheme to use. Defaults to 'basic'.";
		authSchemePropInfo.required = false;
		authSchemePropInfo.choices = Arrays.stream(AuthScheme.values()).map(AuthScheme::getName).toArray(String[]::new);
		driverPropertyInfos.add(authSchemePropInfo);

		var authRealmPropInfo = new DriverPropertyInfo(PROPERTY_AUTH_REALM, parsedConfig.authRealm);
		authRealmPropInfo.description = "The authentication realm to use. Defaults to ''.";
		authRealmPropInfo.required = false;
		driverPropertyInfos.add(authRealmPropInfo);

		var userAgentPropInfo = new DriverPropertyInfo(PROPERTY_USER_AGENT, parsedConfig.agent);
		userAgentPropInfo.description = "user agent to send to server, can be found in logs later.";
		userAgentPropInfo.required = false;
		driverPropertyInfos.add(userAgentPropInfo);

		var connectionTimoutPropInfo = new DriverPropertyInfo(PROPERTY_TIMEOUT, String.valueOf(parsedConfig.timeout));
		connectionTimoutPropInfo.description = "Timeout for connection interactions. Defaults to 1000.";
		connectionTimoutPropInfo.required = false;
		driverPropertyInfos.add(connectionTimoutPropInfo);

		var sql2cypherPropInfo = new DriverPropertyInfo(PROPERTY_SQL_TRANSLATION_ENABLED,
				String.valueOf(parsedConfig.enableSQLTranslation));
		sql2cypherPropInfo.description = "turns on or of sql to cypher translation. Defaults to false.";
		sql2cypherPropInfo.required = false;
		sql2cypherPropInfo.choices = trueFalseChoices;
		driverPropertyInfos.add(sql2cypherPropInfo);

		var rewriteBatchedStatementsPropInfo = new DriverPropertyInfo(PROPERTY_REWRITE_BATCHED_STATEMENTS,
				String.valueOf(parsedConfig.rewriteBatchedStatements));
		rewriteBatchedStatementsPropInfo.description = "turns on generation of more efficient cypher when batching statements. Defaults to true.";
		rewriteBatchedStatementsPropInfo.required = false;
		rewriteBatchedStatementsPropInfo.choices = trueFalseChoices;
		driverPropertyInfos.add(rewriteBatchedStatementsPropInfo);

		var rewritePlaceholdersPropInfo = new DriverPropertyInfo(PROPERTY_REWRITE_PLACEHOLDERS,
				String.valueOf(parsedConfig.rewritePlaceholders));
		rewritePlaceholdersPropInfo.description = "Rewrites SQL placeholders (?) into $1, $2 .. $n. Defaults to true when SQL translation is not enabled.";
		rewritePlaceholdersPropInfo.required = false;
		rewritePlaceholdersPropInfo.choices = trueFalseChoices;
		driverPropertyInfos.add(rewriteBatchedStatementsPropInfo);

		var sql2CypherCachingsPropInfo = new DriverPropertyInfo(PROPERTY_SQL_TRANSLATION_CACHING_ENABLED,
				String.valueOf(parsedConfig.enableTranslationCaching));
		sql2CypherCachingsPropInfo.description = "Enable caching of translations.";
		sql2CypherCachingsPropInfo.required = false;
		sql2CypherCachingsPropInfo.choices = trueFalseChoices;
		driverPropertyInfos.add(sql2CypherCachingsPropInfo);

		var sslPropInfo = new DriverPropertyInfo(SSLProperties.SSL_PROP_NAME,
				String.valueOf(parsedConfig.sslProperties.ssl));
		sslPropInfo.description = "SSL enabled";
		sslPropInfo.required = false;
		sslPropInfo.choices = trueFalseChoices;
		driverPropertyInfos.add(sslPropInfo);

		var sslModePropInfo = new DriverPropertyInfo(SSLProperties.SSL_MODE_PROP_NAME,
				parsedConfig.sslProperties().sslMode.getName());
		sslModePropInfo.description = "The mode for ssl. Accepted values are: require, verify-full, disable.";
		sslModePropInfo.required = false;
		sslModePropInfo.choices = Arrays.stream(SSLMode.values()).map(SSLMode::getName).toArray(String[]::new);
		driverPropertyInfos.add(sslModePropInfo);

		parsedConfig.misc().forEach((k, v) -> {
			if (SSLProperties.SSL_MODE_PROP_NAME.equals(k) || PROPERTY_S2C_ENABLE_CACHE.equals(k)) {
				return;
			}

			var driverPropertyInfo = new DriverPropertyInfo(k, v);
			driverPropertyInfo.required = false;
			driverPropertyInfo.description = "";
			driverPropertyInfos.add(driverPropertyInfo);
		});

		return driverPropertyInfos.toArray(DriverPropertyInfo[]::new);
	}

	private static DriverConfig parseConfig(String url, Properties info) throws SQLException {
		if (url == null || info == null) {
			throw new SQLException("url and info cannot be null");
		}

		var matcher = URL_PATTERN.matcher(url);

		if (!matcher.matches()) {
			throw new SQLException("Invalid url");
		}

		var urlParams = splitUrlParams(matcher.group("urlParams"));

		var config = mergeConfig(urlParams, info);

		var host = matcher.group(PROPERTY_HOST);
		var port = Integer.parseInt((matcher.group(PROPERTY_PORT) != null) ? matcher.group("port") : "7687");
		var databaseName = matcher.group(PROPERTY_DATABASE);
		if (databaseName == null) {
			databaseName = config.getOrDefault(PROPERTY_DATABASE, "neo4j");
		}

		var sslProperties = parseSSLProperties(info, matcher.group("transport"));
		var misc = new HashMap<>(config);

		var authScheme = authScheme(config.get(PROPERTY_AUTH_SCHEME));
		misc.remove(PROPERTY_AUTH_SCHEME);

		var user = String.valueOf(config.getOrDefault(PROPERTY_USER, "neo4j"));
		misc.remove(PROPERTY_USER);
		var password = String.valueOf(config.getOrDefault(PROPERTY_PASSWORD, "password"));
		misc.remove(PROPERTY_PASSWORD);
		var authRealm = config.getOrDefault(PROPERTY_AUTH_REALM, "");
		misc.remove(PROPERTY_AUTH_REALM);

		var userAgent = String.valueOf(config.getOrDefault(PROPERTY_USER_AGENT, getDefaultUserAgent()));
		misc.remove(PROPERTY_USER_AGENT);
		var connectionTimeoutMillis = Integer.parseInt(config.getOrDefault(PROPERTY_TIMEOUT, "1000"));
		misc.remove(PROPERTY_TIMEOUT);
		var automaticSqlTranslation = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_ENABLED, "false"));
		misc.remove(PROPERTY_SQL_TRANSLATION_ENABLED);
		var enableTranslationCaching = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_CACHING_ENABLED, "false"));
		misc.remove(PROPERTY_SQL_TRANSLATION_CACHING_ENABLED);
		var rewriteBatchedStatements = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_REWRITE_BATCHED_STATEMENTS, "true"));
		misc.remove(PROPERTY_REWRITE_BATCHED_STATEMENTS);
		var rewritePlaceholders = Boolean.parseBoolean(
				config.getOrDefault(PROPERTY_REWRITE_PLACEHOLDERS, Boolean.toString(!automaticSqlTranslation)));
		misc.remove(PROPERTY_REWRITE_PLACEHOLDERS);

		misc.putIfAbsent(PROPERTY_S2C_PRETTY_PRINT_CYPHER, "false");
		misc.putIfAbsent(PROPERTY_S2C_ALWAYS_ESCAPE_NAMES, "false");
		misc.putIfAbsent(PROPERTY_S2C_ENABLE_CACHE, String.valueOf(enableTranslationCaching));

		return new DriverConfig(host, port, databaseName, authScheme, user, password, authRealm, userAgent,
				connectionTimeoutMillis, automaticSqlTranslation, enableTranslationCaching, rewriteBatchedStatements,
				rewritePlaceholders, sslProperties, Map.copyOf(misc));
	}

	@Override
	public int getMajorVersion() {
		return ProductVersion.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return ProductVersion.getMinorVersion();
	}

	/**
	 * A driver may only report {@code true} here if it passes the JDBC compliance tests;
	 * otherwise it is required to return {@code false}.
	 * <p>
	 * JDBC compliance requires full support for the JDBC API and full support for SQL 92
	 * Entry Level. It is expected that JDBC compliant drivers will be available for all
	 * the major commercial databases.
	 * @return {@literal false}
	 */
	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(this.getClass().getPackageName());
	}

	private static String[] splitUrlParams(String urlParams) {
		if (urlParams != null) {
			return urlParams.split("&");
		}

		return new String[0];
	}

	/**
	 * Returns the security plan taking into account the transport setting and the two ssl
	 * config options.
	 * @param sslProperties the config that has been passed to the driver
	 * @return a {@link SecurityPlan} that fulfills the requirements of the config
	 * @throws IllegalArgumentException when the transport option and the config options
	 * contradict
	 */
	private static SecurityPlan parseSSLParams(SSLProperties sslProperties) throws SQLException {
		return switch (sslProperties.sslMode) {
			case REQUIRE -> {
				try {
					yield SecurityPlans.forAllCertificates();
				}
				catch (GeneralSecurityException ex) {
					throw new SQLException(ex);
				}
			}
			case VERIFY_FULL -> {
				try {
					yield SecurityPlans.forSystemCASignedCertificates();
				}
				catch (GeneralSecurityException | IOException ex) {
					throw new SQLException(ex);
				}
			}
			case DISABLE -> SecurityPlans.insecure();
		};
	}

	private static SSLMode sslMode(String text) throws IllegalArgumentException {
		if (text == null) {
			return null;
		}

		try {
			return SSLMode.valueOf(text.toUpperCase(Locale.ROOT).replace("-", "_"));
		}
		catch (IllegalArgumentException ignored) {
			throw new IllegalArgumentException(String.format("%s is not a valid option for SSLMode", text));
		}
	}

	private static SSLProperties parseSSLProperties(Properties info, String transport) throws SQLException {
		var sslMode = sslMode(info.getProperty("sslMode"));
		Boolean ssl = null;

		// Some Parsing with validation
		var sslString = info.getProperty("ssl");
		if (sslString != null) {
			if (!sslString.equals("true") && !sslString.equals("false")) {
				throw new SQLException("Invalid SSL option, accepts true or false");
			}

			ssl = Boolean.parseBoolean(sslString);
		}

		if (transport != null) {
			if (transport.equals("s")) {
				if (ssl != null && !ssl) {
					throw new SQLException(
							"Invalid transport option +s when ssl option set to false, accepted ssl option is true");
				}

				ssl = true;

				if (sslMode == null) {
					sslMode = SSLMode.VERIFY_FULL;
				}
				else {
					if (sslMode == SSLMode.DISABLE) {
						throw new SQLException(
								"Invalid SSLMode %s for +s transport option, accepts verify-ca, verify-full, require");
					}
				}
			}
			else if (transport.equals("ssc")) {
				if (ssl != null && !ssl) {
					throw new SQLException(
							"Invalid transport option +ssc when ssl option set to false, accepted ssl option is true");
				}
				ssl = true;

				if (sslMode == null) {
					sslMode = SSLMode.REQUIRE;
				}
				else if (sslMode != SSLMode.REQUIRE) {
					throw new SQLException("Invalid SSLMode %s for +scc transport option, accepts 'require' only");
				}
			}
			else if (!transport.isEmpty()) {
				throw new SQLException("Invalid Transport section of the URL, accepts +s or +scc");
			}
		}

		// implicit defaults
		if (ssl == null && (sslMode == SSLMode.VERIFY_FULL || sslMode == SSLMode.REQUIRE)) {
			ssl = true;
		}
		else if (ssl != null && sslMode == null && ssl) {
			sslMode = SSLMode.REQUIRE;
		}

		if (sslMode == null) {
			sslMode = SSLMode.DISABLE;
		}
		if (ssl == null) {
			ssl = false;
		}

		// Validation
		if (ssl) {
			if (sslMode != SSLMode.VERIFY_FULL && sslMode != SSLMode.REQUIRE) {
				throw new SQLException(
						String.format("Invalid sslMode %s when ssl = true, accepts verify-full and require", sslMode));
			}
		}
		else {
			if (sslMode != SSLMode.DISABLE) {
				throw new SQLException(String
					.format("Invalid sslMode %s when ssl = false, accepts disable, allow and prefer", sslMode));

			}
		}

		return new SSLProperties(sslMode, ssl);
	}

	private List<TranslatorFactory> getSqlTranslatorFactory(String translatorFactory) {

		var fqn = "DEFAULT".equalsIgnoreCase(translatorFactory)
				? "org.neo4j.jdbc.translator.impl.SqlToCypherTranslatorFactory" : translatorFactory;
		try {
			@SuppressWarnings("unchecked")
			Class<TranslatorFactory> cls = (Class<TranslatorFactory>) Class.forName(fqn);
			return List.of(cls.getDeclaredConstructor().newInstance());
		}
		catch (ClassNotFoundException ex) {
			getParentLogger().log(Level.WARNING, "Translator factory {0} not found", new Object[] { fqn });
		}
		catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
			getParentLogger().log(Level.WARNING, ex, () -> "Could not load translator factory");
		}
		return List.of();
	}

	/**
	 * Tries to load all available {@link TranslatorFactory SqlTranslator factories} via
	 * the {@link ServiceLoader} machinery. Throws an exception if there is no
	 * implementation on the class- or module-path.
	 * @return a collection containing at least one {@link TranslatorFactory}
	 */
	private List<TranslatorFactory> getSqlTranslatorFactories() {

		List<TranslatorFactory> result = this.sqlTranslatorFactories;
		if (result == null) {
			synchronized (this) {
				result = this.sqlTranslatorFactories;
				if (result == null) {
					this.sqlTranslatorFactories = ServiceLoader.load(TranslatorFactory.class)
						.stream()
						.map(ServiceLoader.Provider::get)
						.toList();
					result = this.sqlTranslatorFactories;
				}
			}
		}
		return result;
	}

	static Supplier<List<Translator>> getSqlTranslatorSupplier(boolean automaticSqlTranslation,
			Map<String, Object> config, Supplier<List<TranslatorFactory>> sqlTranslatorFactoriesSupplier)
			throws SQLException {

		if (automaticSqlTranslation) {
			// If the driver should translate all queries into cypher, we can make sure
			// this is possible by resolving
			// the factory right now and configure the translator from the given
			// connection properties, too
			var factories = sqlTranslatorFactoriesSupplier.get();
			if (factories.isEmpty()) {
				throw noTranslatorsAvailableException();
			}
			return () -> sortedListOfTranslators(config, factories);
		}
		else {
			// we delay this until we are explicitly asked for
			// Copy the properties, so that they can't be changed until we need them
			var localConfig = Map.copyOf(config);
			return () -> sortedListOfTranslators(localConfig, sqlTranslatorFactoriesSupplier.get());
		}
	}

	static SQLException noTranslatorsAvailableException() {
		return new SQLException("No translators available");
	}

	private static List<Translator> sortedListOfTranslators(Map<String, Object> config,
			List<TranslatorFactory> factories) {
		if (factories.size() == 1) {
			return List.of(factories.get(0).create(config));
		}
		return factories.stream().map(factory -> factory.create(config)).sorted(TranslatorComparator.INSTANCE).toList();
	}

	enum SSLMode {

		DISABLE("disable"), REQUIRE("require"), VERIFY_FULL("verify-full");

		private final String name;

		SSLMode(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	/**
	 * Internal record class to handle parsing SSLProperties.
	 *
	 * @param sslMode the current ssl mode
	 * @param ssl boolean dictating if ssl is enabled
	 */
	private record SSLProperties(SSLMode sslMode, boolean ssl) {

		static final String SSL_MODE_PROP_NAME = "sslMode";
		static final String SSL_PROP_NAME = "ssl";

	}

	private enum AuthScheme {

		NONE("none"), BASIC("basic"), BEARER("bearer"), KERBEROS("kerberos");

		private final String name;

		AuthScheme(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	private static AuthScheme authScheme(String scheme) throws IllegalArgumentException {
		if (scheme == null || scheme.isBlank()) {
			return AuthScheme.BASIC;
		}

		try {
			return AuthScheme.valueOf(scheme.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ignored) {
			throw new IllegalArgumentException(String.format("%s is not a valid option for authScheme", scheme));
		}
	}

	/**
	 * Internal record class to handle parsing of driver config.
	 *
	 * @param host host name
	 * @param port port
	 * @param database database name
	 * @param authScheme auth scheme
	 * @param user user
	 * @param password password for user
	 * @param authRealm auth realm
	 * @param agent driver bolt agent to be used by clients to distinguish between
	 * applications
	 * @param timeout timeout for network interactions
	 * @param enableSQLTranslation turn on or off automatic cypher translation
	 * @param enableTranslationCaching enable caching for translations
	 * @param rewriteBatchedStatements rewrite batched statements to be more efficient
	 * @param rewritePlaceholders rewrite ? to $0 .. $n
	 * @param sslProperties ssl properties
	 * @param misc Unparsed properties
	 */

	private record DriverConfig(String host, int port, String database, AuthScheme authScheme, String user,
			String password, String authRealm, String agent, int timeout, boolean enableSQLTranslation,
			boolean enableTranslationCaching, boolean rewriteBatchedStatements, boolean rewritePlaceholders,
			SSLProperties sslProperties, Map<String, String> misc) {

		Map<String, Object> rawConfig() {
			Map<String, Object> props = new HashMap<>();
			props.put(PROPERTY_SQL_TRANSLATION_ENABLED, String.valueOf(this.enableSQLTranslation));

			props.put(PROPERTY_HOST, this.host);
			props.put(PROPERTY_PORT, String.valueOf(this.port));
			props.put(PROPERTY_USER, this.user);
			props.put(PROPERTY_USER_AGENT, this.agent);
			props.put(PROPERTY_PASSWORD, this.password);
			props.put(PROPERTY_AUTH_SCHEME, this.authScheme.getName());
			props.put(PROPERTY_AUTH_REALM, this.authRealm);
			props.put(PROPERTY_TIMEOUT, String.valueOf(this.timeout));
			props.put(PROPERTY_REWRITE_BATCHED_STATEMENTS, String.valueOf(this.rewriteBatchedStatements));
			props.put(PROPERTY_REWRITE_PLACEHOLDERS, String.valueOf(this.rewritePlaceholders));
			props.put(PROPERTY_DATABASE, this.database);

			props.putAll(this.misc);

			return props;
		}
	}

	/**
	 * Configuration step for creating new {@link Neo4jDriver driver instances} in a
	 * fluent way.
	 */
	public interface SpecifyEnvStep {

		/**
		 * Creates a new {@link Connection} from the environment. The driver will first
		 * evaluate the system environment followed by an optional {@literal .env} file in
		 * the current working directory. If you want to use a different directory, use
		 * {@link #fromEnv(Path)}, if you want to use a .env file with a different name,
		 * use {@link #fromEnv(String)}. If you want to customize both, use
		 * {@link #fromEnv(Path, String)}. The following environment variables are
		 * supported:
		 * <ul>
		 * <li><code>NEO4J_URI</code> The address or URI of the instance to connect
		 * to</li>
		 * <li><code>NEO4J_USERNAME</code> Optional username</li>
		 * <li><code>NEO4J_PASSWORD</code> Optional password</li>
		 * <li><code>NEO4J_SQL_TRANSLATION_ENABLED</code> Optional flag to enable full SQL
		 * to Cypher translation, defaults to {@literal false}</li>
		 * </ul>
		 * @return a connection when the environment contains at least a supported URL
		 * under the key {@literal NEO4J_URI}.
		 * @throws SQLException any error that might happen
		 */
		default Optional<Connection> fromEnv() throws SQLException {
			return fromEnv(null, null);
		}

		/**
		 * Creates a new {@link Connection} from the environment, changing the search path
		 * for .env files to the given directory. System environment variables have
		 * precedence.
		 * @param directory an optional directory to look for .env files
		 * @return a connection when the environment contains at least a supported URL
		 * under the key {@literal NEO4J_URI}.
		 * @throws SQLException any error that might happen
		 * @see #fromEnv() for supported environment variables
		 */
		default Optional<Connection> fromEnv(Path directory) throws SQLException {
			return fromEnv(directory, null);
		}

		/**
		 * Creates a new {@link Connection} from the environment, changing the filename of
		 * the .env files to the given name. System environment variables have precedence.
		 * @param filename an alternative filename for the .env file
		 * @return a connection when the environment contains at least a supported URL
		 * under the key {@literal NEO4J_URI}.
		 * @throws SQLException any error that might happen
		 * @see #fromEnv() for supported environment variables
		 */
		default Optional<Connection> fromEnv(String filename) throws SQLException {

			return fromEnv(null, filename);
		}

		/**
		 * Creates a new {@link Connection} from the environment, changing the search path
		 * for .env files to the given directory and the filename of the .env files to the
		 * given name. System environment variables have precedence.
		 * @param directory an optional directory to look for .env files
		 * @param filename an alternative filename for the .env file
		 * @return a connection when the environment contains at least a supported URL
		 * under the key {@literal NEO4J_URI}.
		 * @throws SQLException any error that might happen
		 * @see #fromEnv() for supported environment variables
		 */
		Optional<Connection> fromEnv(Path directory, String filename) throws SQLException;

	}

	/**
	 * Responsible for configuring the optional SQL to Cypher translation.
	 */
	public interface SpecifyTranslationStep extends SpecifyEnvStep {

		/**
		 * Call to enable SQL to Cypher translation.
		 * @return final step to retrieve a driver from the environment
		 */
		SpecifyEnvStep withSQLTranslation();

	}

	/**
	 * Responsible for adding additional properties that should be used in addition to the
	 * environment when creating the driver. They won't override any username, password or
	 * host settings from the driver, but merely used in addition for everything not taken
	 * from the environment.
	 */
	public interface SpecifyAdditionalPropertiesStep extends SpecifyEnvStep {

		/**
		 * Call this to specify any additional properties. The environment has precedence.
		 * Especially username, password and host will always be taken from the
		 * environment.
		 * @param additionalProperties any additional properties.
		 * @return final step to retrieve a driver from the environment
		 */
		SpecifyEnvStep withProperties(Map<String, Object> additionalProperties);

	}

	private static final class BuilderImpl implements SpecifyAdditionalPropertiesStep, SpecifyTranslationStep {

		private boolean forceSqlTranslation;

		private Map<String, Object> additionalProperties;

		BuilderImpl(boolean forceSqlTranslation, Map<String, Object> additionalProperties) {
			this.forceSqlTranslation = forceSqlTranslation;
			this.additionalProperties = additionalProperties;
		}

		@Override
		public Optional<Connection> fromEnv(Path directory, String filename) throws SQLException {

			var builder = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed();

			if (directory != null) {
				builder = builder.directory(directory.toAbsolutePath().toString());
			}
			if (filename != null && !filename.isBlank()) {
				builder = builder.filename(filename);
			}

			var env = builder.load();

			var address = env.get("NEO4J_URI");
			if (address != null && !address.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
				address = "jdbc:" + address;
			}
			if (address != null && Neo4jDriver.URL_PATTERN.matcher(address).matches()) {
				var properties = new Properties();
				properties.putAll(this.additionalProperties);

				var username = env.get("NEO4J_USERNAME");
				if (username != null) {
					properties.put(Neo4jDriver.PROPERTY_USER, username);
				}
				var password = env.get("NEO4J_PASSWORD");
				if (password != null) {
					properties.put(Neo4jDriver.PROPERTY_PASSWORD, password);
				}
				var authScheme = env.get("NEO4J_AUTH_SCHEME");
				if (authScheme != null) {
					properties.put(Neo4jDriver.PROPERTY_AUTH_SCHEME, authScheme);
				}
				var authRealm = env.get("NEO4J_AUTH_REALM");
				if (authRealm != null) {
					properties.put(Neo4jDriver.PROPERTY_AUTH_REALM, authRealm);
				}
				var sql2cypher = env.get("NEO4J_SQL_TRANSLATION_ENABLED");
				if (this.forceSqlTranslation || Boolean.parseBoolean(sql2cypher)) {
					properties.put(Neo4jDriver.PROPERTY_SQL_TRANSLATION_ENABLED, "true");
				}
				return Optional.of(new Neo4jDriver().connect(address, properties));
			}

			return Optional.empty();
		}

		@Override
		public SpecifyEnvStep withProperties(Map<String, Object> additionalProperties) {
			this.additionalProperties = Objects.requireNonNullElseGet(additionalProperties, Map::of);
			return this;
		}

		@Override
		public SpecifyEnvStep withSQLTranslation() {
			this.forceSqlTranslation = true;
			return this;
		}

	}

}
