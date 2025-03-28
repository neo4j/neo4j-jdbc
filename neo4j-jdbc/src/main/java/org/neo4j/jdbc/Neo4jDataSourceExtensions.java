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

import javax.sql.DataSource;

/**
 * Neo4j specific extensions to a {@link DataSource}.
 *
 * @author Michael J. Simons
 */
public sealed interface Neo4jDataSourceExtensions extends DataSource permits Neo4jDataSource {

	/**
	 * Returns the name of the database to use.
	 * @return the name of the database to use
	 */
	String getDatabaseName();

	/**
	 * Configures the database to use.
	 * @param databaseName a database name
	 */
	void setDatabaseName(String databaseName);

	/**
	 * Returns the password for the user that connects to the database.
	 * @return the password for the user that connects to the database
	 */
	String getPassword();

	/**
	 * Configures the password to use.
	 * @param password a password
	 */
	void setPassword(String password);

	/**
	 * Returns the port number.
	 * @return the port number
	 */
	int getPortNumber();

	/**
	 * Configures the port number to use.
	 * @param portNumber the new port number
	 */
	void setPortNumber(int portNumber);

	/**
	 * Returns the server name or IP address to connect against.
	 * @return the server name
	 */
	String getServerName();

	/**
	 * Configures the server name or IP address to connect against.
	 * @param serverName the new server name
	 */
	void setServerName(String serverName);

	/**
	 * Returns the user that should be connected to the database.
	 * @return the user
	 */
	String getUser();

	/**
	 * Configures the user that should be connected to the database.
	 * @param user the new user
	 */
	void setUser(String user);

	/**
	 * Returns the transport protocol.
	 * @return the transport protocol
	 */
	String getTransportProtocol();

	/**
	 * Configures the transport protocol, might be null or empty, {@literal s} or
	 * {@literal ssc}.
	 * @param transportProtocol the new protocol
	 */
	void setTransportProtocol(String transportProtocol);

	/**
	 * Configures the URL to connect to, overriding any other configuration via properties
	 * above.
	 * @param url the Neo4j connection URL
	 * @since 6.3.0
	 */
	void setUrl(String url);

	/**
	 * Sets a connection property on the data source.
	 * @param name the name of the connection property to be set
	 * @param value the value of the connection property to be set
	 */
	void setConnectionProperty(String name, String value);

}
