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
package org.neo4j.driver.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
public final class Neo4jDriver implements Driver {

	private static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(new DriverThreadFactory());

	private static final String URL_REGEX = "^jdbc:neo4j://(?<host>[^:/?]+):?(?<port>\\d+)?/?(?<database>[^?]+)?\\??(?<urlParams>\\S+)?$";

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
		if (url == null || info == null) {
			throw new SQLException("url and info cannot be null.");
		}

		var matcher = URL_PATTERN.matcher(url);

		if (!matcher.matches()) {
			throw new SQLException("Invalid url.");
		}

		var host = matcher.group("host");
		var port = (matcher.group("port") != null) ? Integer.parseInt(matcher.group(2)) : 7687;
		var address = new BoltServerAddress(host, port);

		var securityPlan = SecurityPlans.insecure();

		var databaseName = matcher.group("database");
		if (databaseName == null) {
			databaseName = info.getProperty("database", "neo4j");
		}

		var urlParams = splitUrlParams(matcher.group("urlParams"));
		var config = mergeConfig(urlParams, info);

		var user = config.getOrDefault("user", "neo4j");

		var password = config.getOrDefault("password", "password");
		var authToken = (password != null) ? AuthTokens.basic(user, password) : AuthTokens.none();

		var boltAgent = BoltAgentUtil.boltAgent();
		var userAgent = config.getOrDefault("agent", "neo4j-jdbc");
		var connectTimeoutMillis = Integer.parseInt(config.getOrDefault("timeout", "1000"));

		var boltConnection = this.boltConnectionProvider
			.connect(address, securityPlan, databaseName, authToken, boltAgent, userAgent, connectTimeoutMillis)
			.toCompletableFuture()
			.join();

		var automaticSqlTranslation = Boolean.parseBoolean(config.getOrDefault("sql2cypher", "false"));

		return new ConnectionImpl(boltConnection,
				getSqlTranslatorSupplier(automaticSqlTranslation, config, this::getSqlTranslatorFactory),
				automaticSqlTranslation);
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
		throw new UnsupportedOperationException();
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
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}

	private static String[] splitUrlParams(String urlParams) {
		if (urlParams != null) {
			return urlParams.split("&");
		}

		return new String[0];
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

}
