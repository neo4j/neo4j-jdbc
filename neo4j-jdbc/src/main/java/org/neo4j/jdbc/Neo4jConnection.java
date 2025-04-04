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

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.tracing.Neo4jTracer;

/**
 * A Neo4j specific extension of {@link Connection}. It may be referred to for use with
 * {@link #unwrap(Class)} to access specific Neo4j functionality.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
public sealed interface Neo4jConnection extends Connection, Neo4jMetadataWriter permits ConnectionImpl {

	/**
	 * Sets the timeout for which a connection may remain idle following a request.
	 * <p>
	 * The timeout value applies to individual server responses, not the JDBC API calls.
	 * Any response from the server, including the {@literal NOOP} chunk, interrupts the
	 * idle period.
	 * <p>
	 * When initializing a new Bolt connection, the Neo4j server may supply a
	 * {@literal connection.recv_timeout_seconds} connection hint that defines the amount
	 * of time for which the connection may remain idle (see the linked documentation for
	 * more details). When the network timeout value is set to {@literal 0} and the hint
	 * value is available, the latter is used as a default.
	 * <p>
	 * For method full description, see the
	 * {@link Connection#setNetworkTimeout(Executor, int)} documentation.
	 * @param executor this parameter is not used by this implementation and may be
	 * {@literal null}.
	 * @param milliseconds the timeout value in milliseconds. If the value is
	 * {@literal 0}, the connection hint value is used providing it was supplied by the
	 * server or the timeout is turned off.
	 * @throws SQLException if a database access error occurs, this method is called on a
	 * closed connection or the value specified for seconds is less than 0.
	 * @see <a href=
	 * "https://neo4j.com/docs/bolt/current/appendix/connection-hints/#hint-recv-timeout-seconds">connection.recv_timeout_seconds
	 * connection hint</a>
	 */
	@Override
	void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException;

	/**
	 * Returns the network timeout value.
	 * <p>
	 * For method full description, see the {@link Connection#getNetworkTimeout()}
	 * documentation.
	 * @return the timeout value. The value {@literal 0} means the server supplied timeout
	 * is used when available or the timeout is off.
	 * @throws SQLException if a database access error occurs or this method is called on
	 * a closed connection.
	 * @see <a href=
	 * "https://neo4j.com/docs/bolt/current/appendix/connection-hints/#hint-recv-timeout-seconds">connection.recv_timeout_seconds
	 * connection hint</a>
	 */
	@Override
	int getNetworkTimeout() throws SQLException;

	/**
	 * Flushes the SQL to Cypher translation cache.
	 */
	void flushTranslationCache();

	/**
	 * Returns the name of the database for this connection.
	 * @return the name of the database for this connection
	 */
	String getDatabaseName();

	/**
	 * Adds a listener to this connection that gets notified when statements are created
	 * and closed.
	 * @param connectionListener the listener to add, must not be {@literal null}
	 * @since 6.3.0
	 */
	void addListener(ConnectionListener connectionListener);

	/**
	 * The database URL this connection is connected to.
	 * @return the database URL this connection is connected to
	 * @since 6.3.0
	 */
	URI getDatabaseURL();

	/**
	 * A call with a {@code tracer} that is not {@literal null} will enable tracing for
	 * this connection.
	 * @param tracer the tracer to use, {@literal null} safe
	 * @return this connection
	 * @since 6.3.0
	 */
	Neo4jConnection withTracer(Neo4jTracer tracer);

}
