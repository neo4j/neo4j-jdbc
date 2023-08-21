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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Actual implementation of the Neo4j data source.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class DataSourceImpl implements Neo4jDataSource {

	// Properties according to table 9-1 "Standard Data Source Properties" in JDBC 4.3
	// spec

	/**
	 * The name of a particular database on a server.
	 */
	private String databaseName;

	/**
	 * A data source name; used to name an underlying XADataSource object or
	 * ConnectionPoolDataSource object when pooling of connections is done.
	 */
	private String dataSourceName;

	/**
	 * Description of this data source.
	 */
	private String description;

	/**
	 * The network protocol used to communicate with the server. Valid options are all
	 * valid sub-protocols that are supported by {@link Neo4jDriver#acceptsURL(String)}.
	 * The main protocol {@literal neo4j} does not need to be configured, it is implied.
	 */
	private String networkProtocol;

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

	@Override
	public String getDatabaseName() {
		return this.databaseName;
	}

	@Override
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public String getDataSourceName() {
		return this.dataSourceName;
	}

	@Override
	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getNetworkProtocol() {
		return this.networkProtocol;
	}

	@Override
	public void setNetworkProtocol(String networkProtocol) {
		this.networkProtocol = networkProtocol;
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

	String getUrl() {
		return "jdbc:neo4j:%s://%s:%d/%s".formatted(
				Objects.requireNonNull(this.networkProtocol,
						"The network protocol must be specified on the data source"),
				Objects.requireNonNull(this.serverName, "The server name must be specified on the data source"),
				this.portNumber, Optional.of(this.databaseName).orElse("neo4j"));
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (this.user != null && this.password != null) {
			return this.getConnection(this.user, new String(this.password));
		}
		return DriverManager.getConnection(getUrl());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return DriverManager.getConnection(getUrl(), username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
