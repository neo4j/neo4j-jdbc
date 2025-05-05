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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.neo4j.jdbc.tracing.Neo4jTracer;

/**
 * A Neo4j specific extension of {@link DataSource}. It may be referred to for use with
 * {@link #unwrap(Class)} to access specific Neo4j functionality.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public final class Neo4jDataSource implements Neo4jDataSourceExtensions {

	/**
	 * The name of a particular database on a server.
	 */
	private String databaseName;

	/**
	 * The network protocol used to communicate with the server. Valid options are all
	 * valid sub-protocols that are supported by {@link Neo4jDriver#acceptsURL(String)}.
	 * The main protocol {@literal neo4j} does not need to be configured, it is implied.
	 */
	private String transportProtocol;

	/**
	 * A database password.
	 */
	private char[] password;

	/**
	 * The port number where a server is listening for requests, defaults to the standard
	 * bolt port {@literal 7687}.
	 */
	private int portNumber = 7687;

	/**
	 * The fully qualified name of the cluster or single instance or an ip address.
	 */
	private String serverName;

	/**
	 * The user to be authenticated.
	 */
	private String user;

	/**
	 * Timeout in seconds.
	 */
	private int loginTimeout = 1;

	private String url;

	/**
	 * A log writer, which we currently don't use.
	 */
	private PrintWriter logWriter = new PrintWriter(System.out);

	private final Properties connectionProperties = new Properties();

	private Neo4jTracer tracer;

	/**
	 * Creates a new {@link DataSource} which is not yet usable without further
	 * configuration. This constructor is mainly used for tooling that loads data sources
	 * via reflection.
	 */
	public Neo4jDataSource() {
	}

	@Override
	public String getDatabaseName() {
		return this.databaseName;
	}

	@Override
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public String getPassword() {
		return (this.password == null) ? null : new String(this.password);
	}

	@Override
	public void setPassword(String password) {
		this.password = (password != null) ? password.toCharArray() : null;
	}

	@Override
	public int getPortNumber() {
		return this.portNumber;
	}

	@Override
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	@Override
	public String getServerName() {
		return this.serverName;
	}

	@Override
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	@Override
	public String getUser() {
		return this.user;
	}

	@Override
	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getTransportProtocol() {
		return this.transportProtocol;
	}

	@Override
	public void setTransportProtocol(String transportProtocol) {
		this.transportProtocol = transportProtocol;
	}

	@Override
	public void setUrl(String url) {
		if (url != null && !url.isBlank() && !Neo4jDriver.URL_PATTERN.matcher(url).matches()) {
			throw new IllegalArgumentException("Invalid URL: " + url);
		}
		this.url = url;
	}

	/**
	 * Added for compatibility with some frameworks that look for method named
	 * {@code setDriverClassName}. It's a no-op function in our case.
	 * @param driverClassName the driver name, only allowed and legal value is
	 * {@literal "org.neo4j.jdbc.Neo4jDriver"}
	 */
	public void setDriverClassName(String driverClassName) {
		if (driverClassName != null && !Neo4jDriver.class.getCanonicalName().equals(driverClassName)) {
			throw new IllegalArgumentException("Unsupported driver clas name: " + driverClassName);
		}

		// Ignored anyway, we have only one driver class, which we check above
	}

	String getUrl() {
		if (this.url != null && !this.url.isBlank()) {
			return this.url.trim();
		}

		return "jdbc:neo4j%s://%s:%d/%s?timeout=%d".formatted(
				Optional.ofNullable(this.transportProtocol)
					.map(String::trim)
					.filter(Predicate.not(String::isBlank))
					.map(p -> "+" + p)
					.orElse(""),
				Objects.requireNonNull(this.serverName, "The server name must be specified on the data source"),
				this.portNumber, Optional.ofNullable(this.databaseName).orElse("neo4j"), this.loginTimeout * 1000);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(this.user, this.getPassword());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {

		var newProperties = new Properties();
		this.connectionProperties.stringPropertyNames()
			.forEach(k -> newProperties.put(k, this.connectionProperties.getProperty(k)));

		if (username != null && password != null) {
			newProperties.setProperty("user", username);
			newProperties.setProperty("password", password);
		}

		var connection = DriverManager.getConnection(getUrl(), newProperties).unwrap(Neo4jConnection.class);
		if (this.tracer != null) {
			connection.addListener(new Tracing(this.tracer, connection));
		}
		return connection;
	}

	@Override
	public void setTracer(Neo4jTracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public PrintWriter getLogWriter() {
		return this.logWriter;
	}

	@Override
	public void setLogWriter(PrintWriter out) {
		this.logWriter = Objects.requireNonNull(out, "Log writer must not be null");
	}

	@Override
	public void setLoginTimeout(int seconds) {
		this.loginTimeout = seconds;
	}

	@Override
	public int getLoginTimeout() {
		return this.loginTimeout;
	}

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(this.getClass().getPackageName());
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(Neo4jException.withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	@Override
	public void setConnectionProperty(String name, String value) {
		this.connectionProperties.setProperty(name, value);
	}

}
