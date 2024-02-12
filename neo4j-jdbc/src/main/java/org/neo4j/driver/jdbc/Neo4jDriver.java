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
package org.neo4j.driver.jdbc;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.neo4j.driver.jdbc.internal.bolt.AuthTokens;
import org.neo4j.driver.jdbc.internal.bolt.BoltAgentUtil;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProviders;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlan;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlans;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory;

/**
 * The main entry point for the Neo4j JDBC driver. There is usually little need to use
 * this class directly, it registers automatically with the {@link DriverManager}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
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
	 * containing the timeout.
	 */
	public static final String PROPERTY_TIMEOUT = "timeout";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} used to
	 * enable automatic SQL to Cypher translation.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_ENABLED = "sql2cypher";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} that
	 * makes the SQL to Cypher translation always escape all names.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_ALWAYS_ESCAPE_NAMES = "s2c.alwaysEscapeNames";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property name} that
	 * makes the SQL to Cypher generate pretty printed cypher.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_PRETTY_PRINT_CYPHER = "s2c.prettyPrint";

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

	private volatile SqlTranslatorFactory sqlTranslatorFactory;

	public Neo4jDriver() {
		// Having a public default constructor is not only fine here, but also required on
		// the module path so that this public class can be properly exported.
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

		var user = driverConfig.user;
		var password = driverConfig.password;
		var authToken = (password != null) ? AuthTokens.basic(user, password) : AuthTokens.none();

		var boltAgent = BoltAgentUtil.boltAgent();
		var userAgent = driverConfig.agent;
		var connectTimeoutMillis = driverConfig.timeout;

		var boltConnection = this.boltConnectionProvider
			.connect(address, securityPlan, databaseName, authToken, boltAgent, userAgent, connectTimeoutMillis)
			.toCompletableFuture()
			.join();

		var automaticSqlTranslation = driverConfig.automaticSqlTranslation;
		var rewriteBatchedStatements = driverConfig.rewriteBatchedStatements;

		return new ConnectionImpl(boltConnection, getSqlTranslatorSupplier(automaticSqlTranslation,
				driverConfig.rawConfig(), this::getSqlTranslatorFactory), automaticSqlTranslation,
				rewriteBatchedStatements);
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
				result.put(matcher.group("name"), matcher.group("value"));
			}
		}

		result.putIfAbsent("s2c.prettyPrint", "false");
		result.putIfAbsent("s2c.alwaysEscapeNames", "false");

		return Map.copyOf(result);
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		if (url == null) {
			throw new SQLException("url cannot be null.");
		}

		var matcher = URL_PATTERN.matcher(url);

		return matcher.matches();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		DriverConfig parsedConfig = parseConfig(url, info);
		var driverPropertyInfos = new DriverPropertyInfo[13];

		var hostPropInfo = new DriverPropertyInfo(PROPERTY_HOST, parsedConfig.host);
		hostPropInfo.description = "The host name";
		hostPropInfo.required = true;
		driverPropertyInfos[0] = hostPropInfo;

		var portPropInfo = new DriverPropertyInfo(PROPERTY_PORT, String.valueOf(parsedConfig.port));
		portPropInfo.description = "The port";
		portPropInfo.required = true;
		driverPropertyInfos[1] = portPropInfo;

		var databaseNameInfo = new DriverPropertyInfo(PROPERTY_DATABASE, parsedConfig.database);
		databaseNameInfo.description = "The database name to connect to. Will default to neo4j if left blank.";
		databaseNameInfo.required = false;
		driverPropertyInfos[2] = databaseNameInfo;

		var userPropInfo = new DriverPropertyInfo(PROPERTY_USER, parsedConfig.user);
		userPropInfo.description = "The user that will be used to connect. Will be defaulted to neo4j if left blank.";
		userPropInfo.required = false;
		driverPropertyInfos[3] = userPropInfo;

		var passwordPropInfo = new DriverPropertyInfo(PROPERTY_PASSWORD, parsedConfig.password);
		passwordPropInfo.description = "The password that is used to connect. Defaults to 'password'.";
		passwordPropInfo.required = false;
		driverPropertyInfos[4] = passwordPropInfo;

		var userAgentPropInfo = new DriverPropertyInfo(PROPERTY_USER_AGENT, parsedConfig.agent);
		userAgentPropInfo.description = "user agent to send to server, can be found in logs later.";
		userAgentPropInfo.required = false;
		driverPropertyInfos[5] = userAgentPropInfo;

		var connectionTimoutPropInfo = new DriverPropertyInfo(PROPERTY_TIMEOUT, String.valueOf(parsedConfig.timeout));
		connectionTimoutPropInfo.description = "Timeout for connection interactions. Defaults to 1000.";
		connectionTimoutPropInfo.required = false;
		driverPropertyInfos[6] = connectionTimoutPropInfo;

		var sql2cypherPropInfo = new DriverPropertyInfo(PROPERTY_SQL_TRANSLATION_ENABLED,
				String.valueOf(parsedConfig.automaticSqlTranslation));
		sql2cypherPropInfo.description = "turns on or of sql to cypher translation. Defaults to false.";
		sql2cypherPropInfo.required = false;
		hostPropInfo.choices = new String[] { "true", "false" };
		driverPropertyInfos[7] = sql2cypherPropInfo;

		var rewriteBatchedStatementsPropInfo = new DriverPropertyInfo(PROPERTY_REWRITE_BATCHED_STATEMENTS,
				String.valueOf(parsedConfig.rewriteBatchedStatements));
		rewriteBatchedStatementsPropInfo.description = "turns on generation of more efficient cypher when batching statements. Defaults to true.";
		rewriteBatchedStatementsPropInfo.required = false;
		hostPropInfo.choices = new String[] { "true", "false" };
		driverPropertyInfos[8] = rewriteBatchedStatementsPropInfo;

		var sql2CypherPrttyPrintPropInfo = new DriverPropertyInfo(PROPERTY_SQL_TRANSLATION_PRETTY_PRINT_CYPHER,
				String.valueOf(parsedConfig.s2cPrettyPrint));
		sql2CypherPrttyPrintPropInfo.description = "Ensures when printing sql2cypher statements will be formatted nicely. Defaults to false.";
		sql2CypherPrttyPrintPropInfo.required = false;
		hostPropInfo.choices = new String[] { "true", "false" };
		driverPropertyInfos[9] = sql2CypherPrttyPrintPropInfo;

		var sql2CypherAlwaysEscapeNamesPropInfo = new DriverPropertyInfo(PROPERTY_SQL_TRANSLATION_ALWAYS_ESCAPE_NAMES,
				String.valueOf(parsedConfig.s2cAlwaysEscapeNames));
		sql2CypherAlwaysEscapeNamesPropInfo.description = "Ensures all names will be escaped. Defaults to false.";
		sql2CypherAlwaysEscapeNamesPropInfo.required = false;
		hostPropInfo.choices = new String[] { "true", "false" };
		driverPropertyInfos[10] = sql2CypherAlwaysEscapeNamesPropInfo;

		var sslPropInfo = new DriverPropertyInfo(SSLProperties.SSL_PROP_NAME,
				String.valueOf(parsedConfig.sslProperties.ssl));
		sslPropInfo.description = "SSL enabled";
		portPropInfo.required = false;
		hostPropInfo.choices = new String[] { "true", "false" };
		driverPropertyInfos[11] = sslPropInfo;

		var sslModePropInfo = new DriverPropertyInfo(SSLProperties.SSL_MODE_PROP_NAME,
				parsedConfig.sslProperties().sslMode.getName());
		sslModePropInfo.description = "The mode for ssl. Accepted values are: require, verify-full, disable.";
		sslModePropInfo.required = false;
		hostPropInfo.choices = Arrays.stream(SSLMode.values()).map(SSLMode::getName).toArray(String[]::new);
		driverPropertyInfos[12] = sslModePropInfo;

		return driverPropertyInfos;
	}

	private DriverConfig parseConfig(String url, Properties info) throws SQLException {
		if (url == null || info == null) {
			throw new SQLException("url and info cannot be null.");
		}

		var matcher = URL_PATTERN.matcher(url);

		if (!matcher.matches()) {
			throw new SQLException("Invalid url.");
		}

		var urlParams = splitUrlParams(matcher.group("urlParams"));
		var config = mergeConfig(urlParams, info);

		var host = matcher.group(PROPERTY_HOST);

		var port = Integer.parseInt((matcher.group(PROPERTY_PORT) != null) ? matcher.group("port") : "7687");

		var databaseName = matcher.group(PROPERTY_DATABASE);
		if (databaseName == null) {
			databaseName = config.getOrDefault(PROPERTY_DATABASE, "neo4j");
		}
		var user = String.valueOf(config.getOrDefault(PROPERTY_USER, "neo4j"));
		var password = String.valueOf(config.getOrDefault(PROPERTY_PASSWORD, "password"));
		var userAgent = String.valueOf(config.getOrDefault(PROPERTY_USER_AGENT, getDefaultUserAgent()));
		var connectionTimeoutMillis = Integer.parseInt(config.getOrDefault(PROPERTY_TIMEOUT, "1000"));
		var automaticSqlTranslation = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_ENABLED, "false"));
		var rewriteBatchedStatements = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_REWRITE_BATCHED_STATEMENTS, "true"));
		var sql2CypherPrettyPrint = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_PRETTY_PRINT_CYPHER, "false"));
		var sql2CypherAlwaysEscapeNames = Boolean
			.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_ALWAYS_ESCAPE_NAMES, "false"));

		var sslProperties = parseSSLProperties(info, matcher.group("transport"));

		return new DriverConfig(host, port, databaseName, user, password, userAgent, connectionTimeoutMillis,
				automaticSqlTranslation, sql2CypherAlwaysEscapeNames, sql2CypherPrettyPrint, rewriteBatchedStatements,
				sslProperties);
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

	/**
	 * Returns the first element of the iterator or null, when the iterator is empty.
	 * Throws an {@link IllegalArgumentException} if the iterator contains more than one
	 * element.
	 * @param source the SQL translators found via a {@link ServiceLoader} or any other
	 * machinery
	 * @return a unique SQL translator or {@literal null} if no SQL translator was found
	 * @throws IllegalArgumentException when more than one translator is found
	 * @throws NoSuchElementException when no translator is found
	 */
	static SqlTranslatorFactory uniqueOrThrow(Iterator<SqlTranslatorFactory> source) {
		if (!source.hasNext()) {
			throw new NoSuchElementException("No SQL translators available");
		}
		var result = source.next();
		if (source.hasNext()) {
			var implementations = new ArrayList<String>();
			implementations.add(result.getName());
			do {
				implementations.add(source.next().getName());
			}
			while (source.hasNext());
			throw new IllegalArgumentException("More than one SQL translator found: "
					+ implementations.stream().collect(Collectors.joining(", ", "[", "]")));
		}
		return result;
	}

	/**
	 * Tries to load a unique {@link SqlTranslator} via the {@link ServiceLoader}
	 * machinery. Throws an exception if there is more than one implementation on the
	 * class- or module-path.
	 * @return an instance of a SQL translator.
	 */
	private SqlTranslatorFactory getSqlTranslatorFactory() {

		SqlTranslatorFactory result = this.sqlTranslatorFactory;
		if (result == null) {
			synchronized (this) {
				result = this.sqlTranslatorFactory;
				if (result == null) {
					this.sqlTranslatorFactory = uniqueOrThrow(
							ServiceLoader.load(SqlTranslatorFactory.class).iterator());
					result = this.sqlTranslatorFactory;
				}
			}
		}
		return result;
	}

	static Supplier<SqlTranslator> getSqlTranslatorSupplier(boolean automaticSqlTranslation, Map<String, String> config,
			Supplier<SqlTranslatorFactory> sqlTranslatorFactorySupplier) {

		if (automaticSqlTranslation) {
			// If the driver should translate all queries into cypher, we can make sure
			// this is possible by resolving
			// the factory right now and configure the translator from the given
			// connection properties, too
			var sqlTranslator = sqlTranslatorFactorySupplier.get().create(config);
			return () -> sqlTranslator;
		}
		else {
			// we delay this until we are explicitly asked for
			// Copy the properties, so that they can't be changed until we need them
			var localConfig = Map.copyOf(config);
			return () -> sqlTranslatorFactorySupplier.get().create(localConfig);
		}
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

	/**
	 * Internal record class to handle parsing of driver config.
	 *
	 * @param host host name
	 * @param port port
	 * @param database database name
	 * @param user user
	 * @param password password for user
	 * @param agent driver bolt agent to be used by clients to distinguish between
	 * applications
	 * @param timeout timeout for network interactions
	 * @param automaticSqlTranslation turn on or off automatic cypher translation
	 * @param s2cAlwaysEscapeNames escape names when using sql2cypher
	 * @param s2cPrettyPrint pretty print when using s2c
	 * @param rewriteBatchedStatements rewrite batched statements to be more efficient
	 * @param sslProperties ssl properties
	 */
	private record DriverConfig(String host, int port, String database, String user, String password, String agent,
			int timeout, boolean automaticSqlTranslation, boolean s2cAlwaysEscapeNames, boolean s2cPrettyPrint,
			boolean rewriteBatchedStatements, SSLProperties sslProperties) {

		Map<String, String> rawConfig() {
			Map<String, String> props = new HashMap<>();
			props.put(PROPERTY_SQL_TRANSLATION_ENABLED, String.valueOf(this.automaticSqlTranslation));
			props.put(PROPERTY_SQL_TRANSLATION_ALWAYS_ESCAPE_NAMES, String.valueOf(this.s2cAlwaysEscapeNames));
			props.put(PROPERTY_SQL_TRANSLATION_PRETTY_PRINT_CYPHER, String.valueOf(this.s2cAlwaysEscapeNames));
			props.put(PROPERTY_HOST, this.host);
			props.put(PROPERTY_PORT, String.valueOf(this.port));
			props.put(PROPERTY_USER, this.user);
			props.put(PROPERTY_USER_AGENT, this.agent);
			props.put(PROPERTY_PASSWORD, this.password);
			props.put(PROPERTY_TIMEOUT, String.valueOf(this.timeout));
			props.put(PROPERTY_REWRITE_BATCHED_STATEMENTS, String.valueOf(this.rewriteBatchedStatements));
			props.put(PROPERTY_DATABASE, this.database);

			return props;
		}
	}

}
