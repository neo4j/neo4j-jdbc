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
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.neo4j.driver.jdbc.internal.bolt.AuthTokens;
import org.neo4j.driver.jdbc.internal.bolt.BoltAgentUtil;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProviders;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlans;

/**
 * The main entry point for the Neo4j JDBC driver. There is usually little need to use
 * this class directly, it registers automatically with the {@link DriverManager}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
public final class Neo4jDriver implements Driver {

	private static final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(new DriverThreadFactory());

	private final BoltConnectionProvider boltConnectionProvider = BoltConnectionProviders.netty(eventLoopGroup,
			Clock.systemUTC());

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

	public Neo4jDriver() {
		// This is not only fine, but also required on the module path so that this public
		// class
		// can be properly exported.
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		if (url == null || info == null) {
			throw new UnsupportedOperationException();
		}
		var pattern = Pattern.compile("^jdbc:neo4j:onlyfortesting://([^:]+):(\\d+)$");
		var matcher = pattern.matcher(url);
		if (matcher.matches()) {
			var host = matcher.group(1);
			var port = Integer.parseInt(matcher.group(2));
			var address = new BoltServerAddress(host, port);

			var securityPlan = SecurityPlans.insecure();

			var databaseName = info.getProperty("database", "neo4j");

			var user = info.getProperty("user", "neo4j");
			var password = info.getProperty("password");
			var authToken = (password != null) ? AuthTokens.basic(user, password) : AuthTokens.none();

			var boltAgent = BoltAgentUtil.boltAgent();
			var userAgent = info.getProperty("agent", "neo4j-jdbc");
			var connectTimeoutMillis = Integer.parseInt(info.getProperty("timeout", "1000"));

			var boltConnection = this.boltConnectionProvider
				.connect(address, securityPlan, databaseName, authToken, boltAgent, userAgent, connectTimeoutMillis)
				.toCompletableFuture()
				.join();

			return new ConnectionImpl(boltConnection);
		}
		else {
			return null;
		}
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		var pattern = Pattern.compile("^jdbc:neo4j:onlyfortesting://([^:]+):(\\d+)$");
		var matcher = pattern.matcher(url);
		if (matcher.matches()) {
			return true;
		}
		throw new UnsupportedOperationException();
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

}
