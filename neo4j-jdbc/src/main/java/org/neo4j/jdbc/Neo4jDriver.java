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
package org.neo4j.jdbc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.cdimascio.dotenv.Dotenv;
import org.neo4j.bolt.connection.AuthToken;
import org.neo4j.bolt.connection.AuthTokens;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.bolt.connection.BoltConnectionProvider;
import org.neo4j.bolt.connection.BoltConnectionProviderFactory;
import org.neo4j.bolt.connection.BoltProtocolVersion;
import org.neo4j.bolt.connection.NotificationConfig;
import org.neo4j.bolt.connection.SecurityPlan;
import org.neo4j.bolt.connection.SecurityPlans;
import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.authn.spi.AuthenticationSupplierFactory;
import org.neo4j.jdbc.authn.spi.CustomAuthentication;
import org.neo4j.jdbc.authn.spi.DisabledAuthentication;
import org.neo4j.jdbc.authn.spi.TokenAuthentication;
import org.neo4j.jdbc.authn.spi.UsernamePasswordAuthentication;
import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.DriverListener;
import org.neo4j.jdbc.events.DriverListener.ConnectionClosedEvent;
import org.neo4j.jdbc.events.DriverListener.ConnectionOpenedEvent;
import org.neo4j.jdbc.internal.bolt.BoltAdapters;
import org.neo4j.jdbc.tracing.Neo4jTracer;
import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;

import static org.neo4j.jdbc.Neo4jException.withInternal;
import static org.neo4j.jdbc.Neo4jException.withReason;

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
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the host.
	 */
	public static final String PROPERTY_HOST = "host";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the port.
	 */
	public static final String PROPERTY_PORT = "port";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the username.
	 */
	public static final String PROPERTY_USER = "user";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the database.
	 */
	public static final String PROPERTY_DATABASE = "database";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the user-agent.
	 */
	public static final String PROPERTY_USER_AGENT = "agent";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the password.
	 */
	public static final String PROPERTY_PASSWORD = "password";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the auth scheme.
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
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the auth realm.
	 */
	public static final String PROPERTY_AUTH_REALM = "authRealm";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the authn supplier name.
	 */
	public static final String PROPERTY_AUTHN_SUPPLIER = "authn.supplier";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} containing
	 * the timeout.
	 */
	public static final String PROPERTY_TIMEOUT = "timeout";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} used to
	 * enable automatic SQL to Cypher translation.
	 */
	public static final String PROPERTY_SQL_TRANSLATION_ENABLED = "enableSQLTranslation";

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} used to
	 * enable the use of causal cluster bookmarks.
	 */
	public static final String PROPERTY_USE_BOOKMARKS = "useBookmarks";

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
	 * The name of the {@link #getPropertyInfo(String, Properties) property} used to
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

	/**
	 * The name of the {@link #getPropertyInfo(String, Properties) property} that enables
	 * automatic rewrite of batched prepared statements into batched Cypher statements.
	 */
	public static final String PROPERTY_REWRITE_BATCHED_STATEMENTS = "rewriteBatchedStatements";

	/**
	 * An optional property that is an alternative to {@literal "neo4j+s"}. It can be used
	 * for example to programmatically enable the full SSL chain. Possible values are
	 * {@literal "true"} (SSL enabled) and {@literal "false"} (SSL disabled).
	 */
	public static final String PROPERTY_SSL = "ssl";

	/**
	 * Use this to configure the sample size for determining the relationship types
	 * between labels. Defaults to {@literal 1000}.
	 */
	public static final String PROPERTY_RELATIONSHIP_SAMPLE_SIZE = "relationshipSampleSize";

	/**
	 * An optional configuration for fine-grained control over SSL configuration. Allowed
	 * values are
	 * <ul>
	 * <li>{@literal "disable"}: no SSL
	 * <li>{@literal "require"}: enforce a save connection, but don't verify the server
	 * certificate
	 * <li>{@literal "verify-full"}: full SSL with certificate validation
	 * </ul>
	 */
	public static final String PROPERTY_SSL_MODE = "sslMode";

	private static final String URL_REGEX = "^jdbc:neo4j(?:\\+(?<transport>s(?:sc)?)?)?(?::(?<protocol>https?))?://(?<host>[^:/?]+):?(?<port>\\d+)?/?(?<database>[^?]+)?\\??(?<urlParams>\\S+)?$";

	/**
	 * The URL pattern that this driver supports.
	 * @since 6.2.0
	 */
	public static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

	private static final BoltProtocolVersion MIN_BOLT_VERSION = new BoltProtocolVersion(5, 1);

	private static final Map<String, Object> BOLT_CONNECTION_OPTIONS = Map.of("eventLoopThreadNamePrefix",
			"Neo4jJDBCDriverIO", "maxVersion", new BoltProtocolVersion(5, 8));

	/*
	 * Register one default instance globally.
	 */
	static {
		try {
			DriverManager.registerDriver(new Neo4jDriver());
		}
		catch (SQLException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 * Lets you configure the driver from the environment, but always enable SQL to Cypher
	 * translation.
	 * @return a builder that lets you create a connection from the environment.
	 * @deprecated the return type will change to
	 * {@link SpecifyAdditionalPropertiesOrAuthStep} in the next major version, with all
	 * existing fluent api, plus the ability to configure authentication
	 * @see SpecifyTranslationStep
	 * @see SpecifyAuthStep
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(since = "6.6.0")
	public static SpecifyAdditionalPropertiesStep withSQLTranslation() {
		return new SpecifyAdditionalPropertiesOrAuthStepImpl(new BuilderImpl(true, Map.of(), null));
	}

	/**
	 * Lets you configure the driver from the environment, with additional properties
	 * being applied as well.
	 * @param additionalProperties additional properties to be added to the configuration
	 * @return a builder that lets you create a connection from the environment.
	 * @deprecated the return type will change to {@link SpecifyTranslationOrAuthStep} in
	 * the next major version, with all existing fluent api, plus the ability to configure
	 * authentication
	 * @see SpecifyAdditionalPropertiesStep
	 * @see SpecifyAuthStep
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated(since = "6.6.0")
	public static SpecifyTranslationStep withProperties(Map<String, Object> additionalProperties) {
		return new SpecifyTranslationOrAuthStepImpl(new BuilderImpl(false, additionalProperties, null));
	}

	/**
	 * Registers a supplier of {@link Authentication authentications} for the
	 * {@link Neo4jDriver drivers} registered with the {@link DriverManager}. This allows
	 * {@link DriverManager#getConnection(String)} and its overloads to use that
	 * authentication supplier. If the provider is not {@literal null}, all {@code user}
	 * and {@code password} keys from the JDBC properties or environment will be ignored.
	 * @param authenticationSupplier the authentication supplier that shall be used with
	 * the drivers registered on the {@link DriverManager}
	 */
	public static void registerAuthenticationSupplier(Supplier<Authentication> authenticationSupplier) {
		iterateGlobalNeo4jDrivers(driver -> driver.setAuthenticationSupplier(authenticationSupplier));
	}

	/**
	 * Registers a supplier of {@link Authentication authentications} for the
	 * {@link Neo4jDriver drivers} registered with the {@link DriverManager}. This allows
	 * {@link DriverManager#getConnection(String)} and its overloads to use that
	 * authentication supplier. If the provider is not {@literal null}, all {@code user}
	 * and {@code password} keys from the JDBC properties or environment will be ignored.
	 * @param tracer the tracer that shall be used on the drivers registered on the
	 * {@link DriverManager}
	 */
	public static void registerTracer(Neo4jTracer tracer) {
		iterateGlobalNeo4jDrivers(driver -> driver.setTracer(tracer));
	}

	private static void iterateGlobalNeo4jDrivers(Consumer<Neo4jDriver> callback) {
		var drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			if (drivers.nextElement() instanceof Neo4jDriver neo4jDriver) {
				callback.accept(neo4jDriver);
			}
		}
	}

	/**
	 * Lets you configure a new instance of the driver from the environment, starting with
	 * the authentication supplier. You can later enable sql translation or add additional
	 * properties.
	 * @param authenticationSupplier the authentication provider to use, can be
	 * {@literal null}
	 * @return a builder that lets you create a new connection from the environment or
	 * specify more features
	 * @since 6.6.0
	 * @see SpecifyTranslationStep
	 * @see SpecifyAdditionalPropertiesStep
	 */
	public static SpecifyAdditionalPropertiesOrTranslationStep withAuthenticationSupplier(
			Supplier<Authentication> authenticationSupplier) {
		return new SpecifyAdditionalPropertiesOrTranslationStepImpl(
				new BuilderImpl(false, Map.of(), authenticationSupplier));
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
		return new BuilderImpl(false, Map.of(), null).fromEnv(directory, filename);
	}

	private final List<BoltConnectionProviderFactory> boltConnectionProviderFactories;

	private final Map<String, BoltConnectionProvider> providerCache = new ConcurrentHashMap<>();

	private final Lazy<List<TranslatorFactory>> sqlTranslatorFactories = Lazy
		.of(() -> this.loadServices(TranslatorFactory.class));

	private final Lazy<Map<String, AuthenticationSupplierFactory>> authenticationSupplierFactories = Lazy
		.of(() -> this.loadServices(AuthenticationSupplierFactory.class)
			.stream()
			.collect(Collectors.collectingAndThen(
					Collectors.toMap(AuthenticationSupplierFactory::getName, Function.identity()), Map::copyOf)));

	private final Map<DriverConfig, BookmarkManager> bookmarkManagers = new ConcurrentHashMap<>();

	private final Map<String, Object> transactionMetadata = new ConcurrentHashMap<>();

	private final Set<DriverListener> listeners = new HashSet<>();

	private Neo4jTracer tracer;

	private Supplier<Authentication> authenticationSupplier;

	/**
	 * Creates a new instance of the {@link Neo4jDriver}. The instance is usable and is
	 * able to provide connections. The public constructor is provided mainly for tooling
	 * that directly accesses vendor specific classes and circumvents the service loader
	 * machinery for JDBC.
	 */
	public Neo4jDriver() {
		this(List.of());
	}

	Neo4jDriver(List<BoltConnectionProviderFactory> boltConnectionProviderFactories) {
		this.boltConnectionProviderFactories = boltConnectionProviderFactories.isEmpty()
				? this.loadServices(BoltConnectionProviderFactory.class) : List.copyOf(boltConnectionProviderFactories);
		MetricsCollector.tryGlobal().ifPresent(this::addListener);
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return connect(url, info, null);
	}

	@Override
	public Connection connect(String url, Properties info, Supplier<Authentication> authenticationSupplier)
			throws SQLException {

		var driverConfig = DriverConfig.of(url, info);

		var securityPlan = parseSSLParams(driverConfig.sslProperties);

		var databaseName = driverConfig.database;
		var userAgent = driverConfig.agent;
		var connectTimeoutMillis = driverConfig.timeout;

		var enableSqlTranslation = driverConfig.enableSQLTranslation;
		var enableTranslationCaching = driverConfig.enableTranslationCaching;
		var rewriteBatchedStatements = driverConfig.rewriteBatchedStatements;
		var rewritePlaceholders = driverConfig.rewritePlaceholders;
		var translatorFactory = driverConfig.rawConfig.get(PROPERTY_TRANSLATOR_FACTORY);
		var bookmarkManager = this.bookmarkManagers.computeIfAbsent(driverConfig,
				k -> driverConfig.useBookmarks ? new DefaultBookmarkManagerImpl() : new NoopBookmarkManagerImpl());

		Supplier<List<TranslatorFactory>> translatorFactoriesSupplier = this.sqlTranslatorFactories::resolve;
		if (translatorFactory != null && !translatorFactory.isBlank()) {
			translatorFactoriesSupplier = () -> getSqlTranslatorFactory(translatorFactory);
		}

		var finalAuthenticationSupplier = determineAuthenticationSupplier(authenticationSupplier, driverConfig);
		var targetUrl = driverConfig.toUrl();

		var connectionListeners = new ArrayList<ConnectionListener>();
		this.listeners.forEach(listener -> {
			if (listener instanceof ConnectionListener connectionListener) {
				connectionListeners.add(connectionListener);
			}
		});

		var connection = new ConnectionImpl(targetUrl, finalAuthenticationSupplier,
				authentication -> establishBoltConnection(driverConfig, userAgent, connectTimeoutMillis, securityPlan,
						toAuthToken(authentication)),
				getSqlTranslatorSupplier(enableSqlTranslation, driverConfig.rawConfig(), translatorFactoriesSupplier),
				enableSqlTranslation, enableTranslationCaching, rewriteBatchedStatements, rewritePlaceholders,
				bookmarkManager, this.transactionMetadata, driverConfig.relationshipSampleSize(), databaseName,
				aborted -> {
					var event = new ConnectionClosedEvent(targetUrl, aborted);
					Events.notify(this.listeners, listener -> listener.onConnectionClosed(event));
				}, connectionListeners);

		synchronized (this) {
			if (this.tracer != null) {
				connection.addListener(new Tracing(this.tracer, connection));
			}
		}

		Events.notify(this.listeners, listener -> listener.onConnectionOpened(new ConnectionOpenedEvent(targetUrl)));
		return connection;
	}

	Supplier<Authentication> determineAuthenticationSupplier(Supplier<Authentication> authenticationSupplier,
			DriverConfig driverConfig) {

		if (authenticationSupplier != null) {
			return authenticationSupplier;
		}

		var user = (driverConfig.user == null || driverConfig.user.isBlank()) ? "" : driverConfig.user;
		var password = (driverConfig.password == null || driverConfig.password.isBlank()) ? "" : driverConfig.password;
		if (driverConfig.rawConfig().containsKey(PROPERTY_AUTHN_SUPPLIER)) {
			var factory = this.authenticationSupplierFactories.resolve()
				.get(driverConfig.rawConfig().get(PROPERTY_AUTHN_SUPPLIER));
			if (factory != null) {
				var prefix = "authn.%s.".formatted(factory.getName().toLowerCase(Locale.ROOT));
				return factory.create(user, password,
						driverConfig.rawConfig.entrySet()
							.stream()
							.filter(e -> e.getKey().toLowerCase(Locale.ROOT).startsWith(prefix))
							.collect(Collectors.toMap(e -> e.getKey().replace(prefix, ""), Map.Entry::getValue)));
			}
		}

		synchronized (this) {
			if (this.authenticationSupplier != null) {
				return this.authenticationSupplier;
			}
		}

		var authRealm = (driverConfig.authRealm == null || driverConfig.authRealm.isBlank()) ? null
				: driverConfig.authRealm;
		var authentication = switch (driverConfig.authScheme) {
			case NONE -> Authentication.none();
			case BASIC -> Authentication.usernameAndPassword(user, password, authRealm);
			case BEARER -> Authentication.bearer(password);
			case KERBEROS -> Authentication.kerberos(password);
		};
		return () -> authentication;
	}

	static AuthToken toAuthToken(Authentication authentication) {

		var valueFactory = BoltAdapters.getValueFactory();
		if (authentication instanceof DisabledAuthentication) {
			return AuthTokens.none(valueFactory);
		}
		else if (authentication instanceof UsernamePasswordAuthentication usernameAndPassword) {
			return AuthTokens.basic(usernameAndPassword.username(), usernameAndPassword.password(),
					usernameAndPassword.realm(), valueFactory);
		}
		else if (authentication instanceof TokenAuthentication token) {
			return switch (token.scheme().toLowerCase(Locale.ROOT)) {
				case "bearer" -> AuthTokens.bearer(token.value(), valueFactory);
				case "kerberos" -> AuthTokens.kerberos(token.value(), valueFactory);
				default -> throw new IllegalArgumentException(
						"Invalid scheme `%s` for token based authentication".formatted(token.scheme()));
			};
		}
		else if (authentication instanceof CustomAuthentication customAuthentication) {
			return AuthTokens.custom(BoltAdapters.adaptMap(customAuthentication.toMap()));
		}

		throw new IllegalArgumentException("Unsupported authentication type %s".formatted(authentication));
	}

	private BoltConnection establishBoltConnection(DriverConfig driverConfig, String userAgent,
			int connectTimeoutMillis, SecurityPlan securityPlan, AuthToken authToken) {

		var targetUri = URI
			.create("%s://%s%s".formatted(driverConfig.protocol(), driverConfig.host(), driverConfig.formattedPort()));

		var connectionProvider = this.providerCache.computeIfAbsent(targetUri.getScheme(),
				scheme -> this.boltConnectionProviderFactories.stream()
					.filter(factory -> factory.supports(scheme))
					.findFirst()
					.map(factory -> factory.create(BoltAdapters.newLoggingProvider(), BoltAdapters.getValueFactory(),
							null, BOLT_CONNECTION_OPTIONS))
					.orElseThrow(() -> new RuntimeException(
							"Failed to load a connection provider supporting target %s".formatted(targetUri))));

		return connectionProvider
			.connect(targetUri, null, BoltAdapters.newAgent(ProductVersion.getValue()), userAgent, connectTimeoutMillis,
					securityPlan, authToken, MIN_BOLT_VERSION, NotificationConfig.defaultConfig())
			.toCompletableFuture()
			.join();
	}

	static String getDefaultUserAgent() {
		return "neo4j-jdbc/%s".formatted(ProductVersion.getValue());
	}

	static Map<String, String> mergeConfig(String[] urlParams, Properties jdbcProperties) {
		var result = new HashMap<String, String>();
		for (String name : jdbcProperties.stringPropertyNames()) {
			var value = jdbcProperties.getProperty(name);
			if (value == null) {
				continue;
			}
			result.put(name, value);
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
			throw new Neo4jException(GQLError.$22000.withMessage("url cannot be null"));
		}

		return URL_PATTERN.matcher(url).matches();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {

		var parsedConfig = DriverConfig.of(url, info);
		var driverPropertyInfos = new ArrayList<DriverPropertyInfo>();
		var trueFalseChoices = new String[] { "true", "false" };

		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_HOST, parsedConfig.host, "The host name", true, null));
		driverPropertyInfos
			.add(newDriverPropertyInfo(PROPERTY_PORT, String.valueOf(parsedConfig.port), "The port", true, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_DATABASE, parsedConfig.database,
				"The database name to connect to. Will default to neo4j if left blank.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_USER, parsedConfig.user,
				"The user that will be used to connect. Will be defaulted to neo4j if left blank.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_PASSWORD, parsedConfig.password,
				"The password that is used to connect. Defaults to 'password'.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_AUTH_SCHEME, parsedConfig.authScheme.getName(),
				"The authentication scheme to use. Defaults to 'basic'.", false,
				Arrays.stream(AuthScheme.values()).map(AuthScheme::getName).toArray(String[]::new)));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_AUTH_REALM, parsedConfig.authRealm,
				"The authentication realm to use. Defaults to ''.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_USER_AGENT, parsedConfig.agent,
				"User agent to send to server, can be found in logs later.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_TIMEOUT, String.valueOf(parsedConfig.timeout),
				"Timeout for connection interactions. Defaults to 1000.", false, null));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_SQL_TRANSLATION_ENABLED,
				String.valueOf(parsedConfig.enableSQLTranslation),
				"Turns on or of sql to cypher translation. Defaults to false.", false, trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_REWRITE_BATCHED_STATEMENTS,
				String.valueOf(parsedConfig.rewriteBatchedStatements),
				"Turns on generation of more efficient cypher when batching statements. Defaults to true.", false,
				trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_USE_BOOKMARKS, String.valueOf(parsedConfig.useBookmarks),
				"Enables the use of causal cluster bookmarks. Defaults to true", false, trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_REWRITE_PLACEHOLDERS,
				String.valueOf(parsedConfig.rewritePlaceholders),
				"Rewrites SQL placeholders (?) into $1, $2 .. $n. Defaults to true when SQL translation is not enabled.",
				false, trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_SQL_TRANSLATION_CACHING_ENABLED,
				String.valueOf(parsedConfig.enableTranslationCaching), "Enable caching of translations.", false,
				trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_SSL, String.valueOf(parsedConfig.sslProperties.ssl),
				"SSL enabled", false, trueFalseChoices));
		driverPropertyInfos.add(newDriverPropertyInfo(PROPERTY_SSL_MODE, parsedConfig.sslProperties().sslMode.getName(),
				"The mode for ssl. Accepted values are: require, verify-full, disable.", false,
				Arrays.stream(SSLMode.values()).map(SSLMode::getName).toArray(String[]::new)));

		parsedConfig.misc().forEach((k, v) -> driverPropertyInfos.add(newDriverPropertyInfo(k, v, "", false, null)));

		return driverPropertyInfos.toArray(DriverPropertyInfo[]::new);
	}

	private static DriverPropertyInfo newDriverPropertyInfo(String name, String value, String description,
			boolean required, String[] choices) {
		var result = new DriverPropertyInfo(name, value);
		result.description = description;
		result.required = required;
		result.choices = choices;
		return result;
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
					yield SecurityPlans.encryptedForAnyCertificate();
				}
				catch (GeneralSecurityException ex) {
					throw new Neo4jException(withInternal(ex));
				}
			}
			case VERIFY_FULL -> {
				try {
					yield SecurityPlans.encryptedForSystemCASignedCertificates();
				}
				catch (GeneralSecurityException | IOException ex) {
					throw new Neo4jException(withInternal(ex));
				}
			}
			case DISABLE -> null;
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

	@SuppressWarnings("squid:S3776") // Yep, this is complex.
	private static SSLProperties parseSSLProperties(Map<String, String> info, String transport) throws SQLException {
		var sslMode = sslMode(info.get(PROPERTY_SSL_MODE));
		Boolean ssl = null;

		// Some Parsing with validation
		var sslString = info.get(PROPERTY_SSL);
		if (sslString != null) {
			if (!sslString.equals("true") && !sslString.equals("false")) {
				throw new Neo4jException(GQLError.$22N11.withMessage("Invalid SSL option, accepts true or false"));
			}

			ssl = Boolean.parseBoolean(sslString);
		}

		if (transport != null) {
			if (transport.equals("s")) {
				if (ssl != null && !ssl) {
					throw new Neo4jException(GQLError.$22N11.withMessage(
							"Invalid transport option +s when ssl option set to false, accepted ssl option is true"));
				}

				ssl = true;

				if (sslMode == null) {
					sslMode = SSLMode.VERIFY_FULL;
				}
				else {
					if (sslMode == SSLMode.DISABLE) {
						throw new Neo4jException(GQLError.$22N11.withMessage(
								"Invalid SSLMode %s for +s transport option, accepts verify-ca, verify-full, require"));
					}
				}
			}
			else if (transport.equals("ssc")) {
				if (ssl != null && !ssl) {
					throw new Neo4jException(GQLError.$22N11.withMessage(
							"Invalid transport option +ssc when ssl option set to false, accepted ssl option is true"));
				}
				ssl = true;

				if (sslMode == null) {
					sslMode = SSLMode.REQUIRE;
				}
				else if (sslMode != SSLMode.REQUIRE) {
					throw new Neo4jException(GQLError.$22N11
						.withMessage("Invalid SSLMode %s for +scc transport option, accepts 'require' only"));
				}
			}
			else if (!transport.isEmpty()) {
				throw new Neo4jException(
						GQLError.$22N11.withMessage("Invalid Transport section of the URL, accepts +s or +scc"));
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
				throw new Neo4jException(GQLError.$22N11.withMessage(
						"Invalid sslMode %s when ssl = true, accepts verify-full and require".formatted(sslMode)));
			}
		}
		else {
			if (sslMode != SSLMode.DISABLE) {
				throw new Neo4jException(GQLError.$22N11.withMessage(
						"Invalid sslMode %s when ssl = false, accepts disable, allow and prefer".formatted(sslMode)));

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

	private <T> List<T> loadServices(Class<T> type) {
		return ServiceLoader.load(type, this.getClass().getClassLoader())
			.stream()
			.map(ServiceLoader.Provider::get)
			.toList();
	}

	static Supplier<List<Translator>> getSqlTranslatorSupplier(boolean automaticSqlTranslation, Map<String, ?> config,
			Supplier<List<TranslatorFactory>> sqlTranslatorFactoriesSupplier) throws SQLException {

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
		return new Neo4jException(withReason("No translators available"));
	}

	private static List<Translator> sortedListOfTranslators(Map<String, ?> config, List<TranslatorFactory> factories) {
		if (factories.size() == 1) {
			var t1 = factories.get(0).create(config);
			return (t1 != null) ? List.of(t1) : List.of();
		}
		return factories.stream()
			.map(factory -> factory.create(config))
			.filter(Objects::nonNull)
			.sorted(TranslatorComparator.INSTANCE)
			.toList();
	}

	@Override
	public Collection<Bookmark> getCurrentBookmarks(String url, Properties info) throws SQLException {
		var bm = this.bookmarkManagers.get(DriverConfig.of(url, info));
		if (bm == null) {
			return Set.of();
		}
		return bm.getBookmarks(Bookmark::new);
	}

	@Override
	public void addBookmarks(String url, Properties info, Collection<Bookmark> bookmarks) throws SQLException {
		var bm = this.bookmarkManagers.get(DriverConfig.of(url, info));
		if (bm != null) {
			bm.updateBookmarks(Bookmark::value, List.of(), bookmarks);
		}
	}

	@Override
	public void addListener(DriverListener driverListener) {
		this.listeners.add(Objects.requireNonNull(driverListener));
	}

	@SuppressWarnings("removal")
	@Override
	public Neo4jDriver withTracer(Neo4jTracer tracer) {
		this.setTracer(tracer);
		return this;
	}

	@Override
	public synchronized void setTracer(Neo4jTracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public synchronized void setAuthenticationSupplier(Supplier<Authentication> authenticationSupplier) {
		this.authenticationSupplier = authenticationSupplier;
	}

	@Override
	public Neo4jDriver withMetadata(Map<String, Object> metadata) {
		if (metadata != null) {
			this.transactionMetadata.putAll(metadata);
		}
		return this;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	enum SSLMode {

		/**
		 * Disables SSL completely.
		 */
		DISABLE("disable"),
		/**
		 * Require SSL but don't do a full certificate verification.
		 */
		REQUIRE("require"),
		/**
		 * Require SSL and fully verify the certificate chain.
		 */
		VERIFY_FULL("verify-full");

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
	record SSLProperties(SSLMode sslMode, boolean ssl) {

		String protocolSuffix() {
			if (!this.ssl) {
				return "";
			}
			return (this.sslMode == SSLMode.VERIFY_FULL) ? "+s" : "+ssc";
		}
	}

	enum AuthScheme {

		/**
		 * Disable authentication.
		 */
		NONE("none"),
		/**
		 * Use basic auth (username and password).
		 */
		BASIC("basic"),
		/**
		 * Use a token as authentication (the password will be treated as JWT or other SSO
		 * token).
		 */
		BEARER("bearer"),
		/**
		 * Use Kerberos authentication.
		 */
		KERBEROS("kerberos");

		private final String name;

		AuthScheme(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

	/**
	 * Internal record class to handle parsing of driver config.
	 *
	 * @param host host name
	 * @param protocol the underlying protocol in use
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
	 * @param useBookmarks enables the use of causal cluster bookmarks
	 * @param relationshipSampleSize Sample size for determining relationship types
	 * @param sslProperties ssl properties
	 * @param rawConfig Unprocessed configuration options
	 */
	record DriverConfig(String host, String protocol, Integer port, String database, AuthScheme authScheme, String user,
			String password, String authRealm, String agent, int timeout, boolean enableSQLTranslation,
			boolean enableTranslationCaching, boolean rewriteBatchedStatements, boolean rewritePlaceholders,
			boolean useBookmarks, int relationshipSampleSize, SSLProperties sslProperties,
			Map<String, String> rawConfig) {

		private static final Set<String> DRIVER_SPECIFIC_PROPERTIES = Set.of(PROPERTY_HOST, PROPERTY_PORT,
				PROPERTY_DATABASE, PROPERTY_AUTH_SCHEME, PROPERTY_USER, PROPERTY_PASSWORD, PROPERTY_AUTH_REALM,
				PROPERTY_USER_AGENT, PROPERTY_TIMEOUT, PROPERTY_SQL_TRANSLATION_ENABLED,
				PROPERTY_SQL_TRANSLATION_CACHING_ENABLED, PROPERTY_REWRITE_BATCHED_STATEMENTS,
				PROPERTY_REWRITE_PLACEHOLDERS, PROPERTY_SSL, PROPERTY_SSL_MODE);

		DriverConfig {
			rawConfig = Collections.unmodifiableMap(new TreeMap<>(rawConfig));
		}

		/**
		 * Returns a view on all properties that are not directly used for the driver.
		 * @return a view on all properties that are not directly used for the driver
		 */
		Map<String, String> misc() {

			Map<String, String> misc = new HashMap<>();
			for (var entry : this.rawConfig.entrySet()) {
				if (DRIVER_SPECIFIC_PROPERTIES.contains(entry.getKey())) {
					continue;
				}
				misc.put(entry.getKey(), entry.getValue());

			}

			return misc;
		}

		static DriverConfig of(String url, Properties info) throws SQLException {
			if (url == null) {
				throw new Neo4jException(GQLError.$22N06.withTemplatedMessage("url"));
			}
			if (info == null) {
				throw new Neo4jException(GQLError.$22N06.withTemplatedMessage("info"));
			}
			var matcher = URL_PATTERN.matcher(url);

			if (!matcher.matches()) {
				throw new Neo4jException(GQLError.$22N11.withTemplatedMessage(url));
			}

			var urlParams = splitUrlParams(matcher.group("urlParams"));

			var config = mergeConfig(urlParams, info);
			var raw = new HashMap<>(config);

			var protocol = Optional.ofNullable(matcher.group("protocol")).orElse("neo4j");

			var host = matcher.group(PROPERTY_HOST);
			raw.put(PROPERTY_HOST, host);
			var rawPort = matcher.group(PROPERTY_PORT);
			Integer port = null;
			if ("neo4j".equals(protocol)) {
				port = Integer.parseInt((rawPort != null) ? rawPort : "7687");
			}
			else if (rawPort != null) {
				port = Integer.parseInt(rawPort);
			}

			if (rawPort != null) {
				raw.put(PROPERTY_PORT, matcher.group(PROPERTY_PORT));
			}
			var databaseName = matcher.group(PROPERTY_DATABASE);
			if (databaseName == null) {
				databaseName = config.getOrDefault(PROPERTY_DATABASE, "neo4j");
			}
			else {
				raw.put(PROPERTY_DATABASE, databaseName);
			}

			var sslProperties = parseSSLProperties(config, matcher.group("transport"));
			raw.put(PROPERTY_SSL, String.valueOf(sslProperties.ssl));
			raw.put(PROPERTY_SSL_MODE, sslProperties.sslMode.getName());

			var authScheme = authScheme(config.get(PROPERTY_AUTH_SCHEME));

			var user = String.valueOf(config.getOrDefault(PROPERTY_USER, "neo4j"));
			var password = String.valueOf(config.getOrDefault(PROPERTY_PASSWORD, ""));
			var authRealm = config.getOrDefault(PROPERTY_AUTH_REALM, "");

			var userAgent = String.valueOf(config.getOrDefault(PROPERTY_USER_AGENT, getDefaultUserAgent()));
			var connectionTimeoutMillis = Integer.parseInt(config.getOrDefault(PROPERTY_TIMEOUT, "1000"));
			var automaticSqlTranslation = Boolean
				.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_ENABLED, "false"));
			var enableTranslationCaching = Boolean
				.parseBoolean(config.getOrDefault(PROPERTY_SQL_TRANSLATION_CACHING_ENABLED, "false"));
			var rewriteBatchedStatements = Boolean
				.parseBoolean(config.getOrDefault(PROPERTY_REWRITE_BATCHED_STATEMENTS, "true"));
			var rewritePlaceholders = Boolean.parseBoolean(
					config.getOrDefault(PROPERTY_REWRITE_PLACEHOLDERS, Boolean.toString(!automaticSqlTranslation)));
			var useBookmarks = Boolean.parseBoolean(config.getOrDefault(PROPERTY_USE_BOOKMARKS, "true"));
			var relationshipSampleSize = Integer
				.parseInt(config.getOrDefault(PROPERTY_RELATIONSHIP_SAMPLE_SIZE, "1000"));
			if (relationshipSampleSize < -1) {
				throw new Neo4jException(
						GQLError.$22N02.withMessage("Sample size for relationships must be greater than or equal -1"));
			}

			return new DriverConfig(host, protocol, port, databaseName, authScheme, user, password, authRealm,
					userAgent, connectionTimeoutMillis, automaticSqlTranslation, enableTranslationCaching,
					rewriteBatchedStatements, rewritePlaceholders, useBookmarks, relationshipSampleSize, sslProperties,
					raw);
		}

		private static AuthScheme authScheme(String scheme) throws IllegalArgumentException {
			if (scheme == null || scheme.isBlank()) {
				return AuthScheme.BASIC;
			}

			try {
				return AuthScheme.valueOf(scheme.toUpperCase(Locale.ROOT));
			}
			catch (IllegalArgumentException ignored) {
				throw new IllegalArgumentException("%s is not a valid option for authScheme".formatted(scheme));
			}
		}

		String formattedPort() {
			return (this.port() != null) ? (":" + this.port()) : "";
		}

		URI toUrl() {
			var sslProperties = this.sslProperties();
			var result = new StringBuilder("jdbc:neo4j%s%s://%s%s/%s?".formatted(sslProperties.protocolSuffix(),
					"neo4j".equals(this.protocol()) ? "" : ":" + this.protocol(), this.host(), this.formattedPort(),
					this.database()));
			append(result, PROPERTY_SQL_TRANSLATION_ENABLED, this.enableSQLTranslation()).append("&");
			append(result, PROPERTY_SQL_TRANSLATION_CACHING_ENABLED, this.enableTranslationCaching()).append("&");
			append(result, PROPERTY_REWRITE_BATCHED_STATEMENTS, this.rewriteBatchedStatements()).append("&");
			append(result, PROPERTY_REWRITE_PLACEHOLDERS, this.rewriteBatchedStatements()).append("&");
			append(result, PROPERTY_USE_BOOKMARKS, this.useBookmarks()).append("&");
			return URI.create(result.substring(0, result.length() - 1));
		}

		static StringBuilder append(StringBuilder result, String name, Object value) {
			result.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
				.append("=")
				.append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
			return result;
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
	 * Allows to configure additional properties or SQL translation.
	 *
	 * @since 6.6.0
	 */
	public sealed interface SpecifyAdditionalPropertiesOrTranslationStep extends SpecifyEnvStep
			permits SpecifyAdditionalPropertiesOrTranslationStepImpl {

		/**
		 * Call this to specify any additional properties. The environment has precedence.
		 * Especially username, password and host will always be taken from the
		 * environment.
		 * @param additionalProperties any additional properties.
		 * @return final step to retrieve a driver from the environment
		 */
		SpecifyTranslationStep withProperties(Map<String, Object> additionalProperties);

		/**
		 * Call to enable SQL to Cypher translation.
		 * @return final step to retrieve a driver from the environment
		 */
		SpecifyAdditionalPropertiesStep withSQLTranslation();

	}

	private static final class SpecifyAdditionalPropertiesOrTranslationStepImpl
			implements SpecifyAdditionalPropertiesOrTranslationStep {

		private final BuilderImpl delegate;

		private SpecifyAdditionalPropertiesOrTranslationStepImpl(BuilderImpl delegate) {
			this.delegate = delegate;
		}

		@Override
		public SpecifyTranslationStep withProperties(Map<String, Object> additionalProperties) {
			return (SpecifyTranslationStep) this.delegate.withProperties(additionalProperties);
		}

		@Override
		public SpecifyAdditionalPropertiesStep withSQLTranslation() {
			return (SpecifyAdditionalPropertiesStep) this.delegate.withSQLTranslation();
		}

		@Override
		public Optional<Connection> fromEnv(Path directory, String filename) throws SQLException {
			return this.delegate.fromEnv(directory, filename);
		}

	}

	/**
	 * Allows to configure additional properties or the authentication provider.
	 *
	 * @since 6.6.0
	 */
	public sealed interface SpecifyAdditionalPropertiesOrAuthStep extends SpecifyEnvStep
			permits SpecifyAdditionalPropertiesOrAuthStepImpl {

		/**
		 * Call this to specify any additional properties. The environment has precedence.
		 * Especially username, password and host will always be taken from the
		 * environment.
		 * @param additionalProperties any additional properties.
		 * @return the next step that lets you either configure an authentication supplier
		 * or retrieve a driver from the environment
		 */
		SpecifyAuthStep withProperties(Map<String, Object> additionalProperties);

		/**
		 * Call this to specify an authentication supplier.
		 * @param authenticationSupplier the authentication provider to use, can be
		 * {@literal null}
		 * @return the next step that lets you either configure additional properties or
		 * retrieve a driver from the environment
		 */
		SpecifyAdditionalPropertiesStep withAuthenticationSupplier(Supplier<Authentication> authenticationSupplier);

	}

	private static final class SpecifyAdditionalPropertiesOrAuthStepImpl
			implements SpecifyAdditionalPropertiesStep, SpecifyAdditionalPropertiesOrAuthStep {

		private final BuilderImpl delegate;

		private SpecifyAdditionalPropertiesOrAuthStepImpl(BuilderImpl delegate) {
			this.delegate = delegate;
		}

		@Override
		public SpecifyAuthStep withProperties(Map<String, Object> additionalProperties) {
			return (SpecifyAuthStep) this.delegate.withProperties(additionalProperties);
		}

		@Override
		public SpecifyAdditionalPropertiesStep withAuthenticationSupplier(
				Supplier<Authentication> authenticationSupplier) {
			return (SpecifyAdditionalPropertiesStep) this.delegate.withAuthenticationSupplier(authenticationSupplier);
		}

		@Override
		public Optional<Connection> fromEnv(Path directory, String filename) throws SQLException {
			return this.delegate.fromEnv(directory, filename);
		}

	}

	/**
	 * Allows to configure SQL translation or the authentication provider.
	 *
	 * @since 6.6.0
	 */
	public sealed interface SpecifyTranslationOrAuthStep extends SpecifyEnvStep
			permits SpecifyTranslationOrAuthStepImpl {

		/**
		 * Call to enable SQL to Cypher translation.
		 * @return the next step that lets you either configure an authentication supplier
		 * or retrieve a driver from the environment
		 */
		SpecifyAuthStep withSQLTranslation();

		/**
		 * Call this to specify an authentication supplier.
		 * @param authenticationSupplier the authentication provider to use, can be
		 * {@literal null}
		 * @return the next step that lets you either enable SQL translation or retrieve a
		 * driver from the environment
		 */
		SpecifyTranslationStep withAuthenticationSupplier(Supplier<Authentication> authenticationSupplier);

	}

	private static final class SpecifyTranslationOrAuthStepImpl
			implements SpecifyTranslationStep, SpecifyTranslationOrAuthStep {

		private final BuilderImpl delegate;

		private SpecifyTranslationOrAuthStepImpl(BuilderImpl delegate) {
			this.delegate = delegate;
		}

		@Override
		public SpecifyAuthStep withSQLTranslation() {
			return (SpecifyAuthStep) this.delegate.withSQLTranslation();
		}

		@Override
		public SpecifyTranslationStep withAuthenticationSupplier(Supplier<Authentication> authenticationSupplier) {
			return (SpecifyTranslationStep) this.delegate.withAuthenticationSupplier(authenticationSupplier);
		}

		@Override
		public Optional<Connection> fromEnv(Path directory, String filename) throws SQLException {
			return this.delegate.fromEnv(directory, filename);
		}

	}

	/**
	 * Responsible for configuring the optional SQL to Cypher translation.
	 */
	public sealed interface SpecifyTranslationStep extends SpecifyEnvStep
			permits BuilderImpl, SpecifyTranslationOrAuthStepImpl {

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
	public sealed interface SpecifyAdditionalPropertiesStep extends SpecifyEnvStep
			permits BuilderImpl, SpecifyAdditionalPropertiesOrAuthStepImpl {

		/**
		 * Call this to specify any additional properties. The environment has precedence.
		 * Especially username, password and host will always be taken from the
		 * environment.
		 * @param additionalProperties any additional properties.
		 * @return final step to retrieve a driver from the environment
		 */
		SpecifyEnvStep withProperties(Map<String, Object> additionalProperties);

	}

	/**
	 * Allows to configure the authentication provider.
	 *
	 * @since 6.6.0
	 */
	public sealed interface SpecifyAuthStep extends SpecifyEnvStep permits BuilderImpl {

		/**
		 * Call this to specify an authentication supplier.
		 * @param authenticationSupplier the authentication provider to use, can be
		 * {@literal null}
		 * @return the final step to retrieve a driver from the environment
		 */
		SpecifyEnvStep withAuthenticationSupplier(Supplier<Authentication> authenticationSupplier);

	}

	private static final class BuilderImpl
			implements SpecifyAdditionalPropertiesStep, SpecifyTranslationStep, SpecifyAuthStep {

		private boolean forceSqlTranslation;

		private Map<String, Object> additionalProperties;

		private Supplier<Authentication> authenticationSupplier;

		BuilderImpl(boolean forceSqlTranslation, Map<String, Object> additionalProperties,
				Supplier<Authentication> authenticationSupplier) {
			this.forceSqlTranslation = forceSqlTranslation;
			this.additionalProperties = additionalProperties;
			this.authenticationSupplier = authenticationSupplier;
		}

		@SuppressWarnings("squid:S3776") // Yep, this is complex.
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
			if (address == null || !Neo4jDriver.URL_PATTERN.matcher(address).matches()) {
				return Optional.empty();
			}
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
			return Optional.of(new Neo4jDriver().connect(address, properties, this.authenticationSupplier));

		}

		@Override
		public SpecifyEnvStep withProperties(Map<String, Object> additionalProperties) {
			this.additionalProperties = Objects.requireNonNullElseGet(additionalProperties, Map::of);
			return this;
		}

		@Override
		public SpecifyEnvStep withAuthenticationSupplier(Supplier<Authentication> authenticationSupplier) {
			this.authenticationSupplier = authenticationSupplier;
			return this;
		}

		@Override
		public SpecifyEnvStep withSQLTranslation() {
			this.forceSqlTranslation = true;
			return this;
		}

	}

}
