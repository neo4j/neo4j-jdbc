/**
 * Copyright (c) 2004-2015 LARUS Business Automation Srl
 * <p>
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * <p>
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.util.logging.Logger;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class DataSource implements javax.sql.DataSource {
	/**
	 * <p>Attempts to establish a connection with the data source that
	 * this {@code DataSource} object represents.
	 *
	 * @return a connection to the data source
	 * @throws SQLException        if a database access error occurs
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value specified by the {@code setLoginTimeout} method
	 *                             has been exceeded and has at least tried to cancel the
	 *                             current database connection attempt
	 */
	@Override public Connection getConnection() throws SQLException {
		return null;
	}

	/**
	 * <p>Attempts to establish a connection with the data source that
	 * this {@code DataSource} object represents.
	 *
	 * @param username the database user on whose behalf the connection is
	 *                 being made
	 * @param password the user's password
	 * @return a connection to the data source
	 * @throws SQLException        if a database access error occurs
	 * @throws SQLTimeoutException when the driver has determined that the
	 *                             timeout value specified by the {@code setLoginTimeout} method
	 *                             has been exceeded and has at least tried to cancel the
	 *                             current database connection attempt
	 * @since 1.4
	 */
	@Override public Connection getConnection(String username, String password) throws SQLException {
		return null;
	}

	/**
	 * <p>Retrieves the log writer for this <code>DataSource</code>
	 * object.
	 * <p>
	 * <p>The log writer is a character output stream to which all logging
	 * and tracing messages for this data source will be
	 * printed.  This includes messages printed by the methods of this
	 * object, messages printed by methods of other objects manufactured
	 * by this object, and so on.  Messages printed to a data source
	 * specific log writer are not printed to the log writer associated
	 * with the <code>java.sql.DriverManager</code> class.  When a
	 * <code>DataSource</code> object is
	 * created, the log writer is initially null; in other words, the
	 * default is for logging to be disabled.
	 *
	 * @return the log writer for this data source or null if
	 * logging is disabled
	 * @throws SQLException if a database access error occurs
	 * @see #setLogWriter
	 * @since 1.4
	 */
	@Override public PrintWriter getLogWriter() throws SQLException {
		return null;
	}

	/**
	 * <p>Sets the log writer for this <code>DataSource</code>
	 * object to the given <code>java.io.PrintWriter</code> object.
	 * <p>
	 * <p>The log writer is a character output stream to which all logging
	 * and tracing messages for this data source will be
	 * printed.  This includes messages printed by the methods of this
	 * object, messages printed by methods of other objects manufactured
	 * by this object, and so on.  Messages printed to a data source-
	 * specific log writer are not printed to the log writer associated
	 * with the <code>java.sql.DriverManager</code> class. When a
	 * <code>DataSource</code> object is created the log writer is
	 * initially null; in other words, the default is for logging to be
	 * disabled.
	 *
	 * @param out the new log writer; to disable logging, set to null
	 * @throws SQLException if a database access error occurs
	 * @see #getLogWriter
	 * @since 1.4
	 */
	@Override public void setLogWriter(PrintWriter out) throws SQLException {

	}

	/**
	 * <p>Sets the maximum time in seconds that this data source will wait
	 * while attempting to connect to a database.  A value of zero
	 * specifies that the timeout is the default system timeout
	 * if there is one; otherwise, it specifies that there is no timeout.
	 * When a <code>DataSource</code> object is created, the login timeout is
	 * initially zero.
	 *
	 * @param seconds the data source login time limit
	 * @throws SQLException if a database access error occurs.
	 * @see #getLoginTimeout
	 * @since 1.4
	 */
	@Override public void setLoginTimeout(int seconds) throws SQLException {

	}

	/**
	 * Gets the maximum time in seconds that this data source can wait
	 * while attempting to connect to a database.  A value of zero
	 * means that the timeout is the default system timeout
	 * if there is one; otherwise, it means that there is no timeout.
	 * When a <code>DataSource</code> object is created, the login timeout is
	 * initially zero.
	 *
	 * @return the data source login time limit
	 * @throws SQLException if a database access error occurs.
	 * @see #setLoginTimeout
	 * @since 1.4
	 */
	@Override public int getLoginTimeout() throws SQLException {
		return 0;
	}

	/**
	 * Return the parent Logger of all the Loggers used by this data source. This
	 * should be the Logger farthest from the root Logger that is
	 * still an ancestor of all of the Loggers used by this data source. Configuring
	 * this Logger will affect all of the log messages generated by the data source.
	 * In the worst case, this may be the root Logger.
	 *
	 * @return the parent Logger for this data source
	 * @throws SQLFeatureNotSupportedException if the data source does not use
	 *                                         {@code java.util.logging}
	 * @since 1.7
	 */
	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy.
	 * <p>
	 * If the receiver implements the interface then the result is the receiver
	 * or a proxy for the receiver. If the receiver is a wrapper
	 * and the wrapped object implements the interface then the result is the
	 * wrapped object or a proxy for the wrapped object. Otherwise return the
	 * the result of calling <code>unwrap</code> recursively on the wrapped object
	 * or a proxy for that result. If the receiver is not a
	 * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
	 *
	 * @param iface A Class defining an interface that the result must implement.
	 * @return an object that implements the interface. May be a proxy for the actual implementing object.
	 * @throws SQLException If no object found that implements the interface
	 * @since 1.6
	 */
	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	/**
	 * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
	 * for an object that does. Returns false otherwise. If this implements the interface then return true,
	 * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
	 * object. If this does not implement the interface and is not a wrapper, return false.
	 * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
	 * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
	 * returns true then calling <code>unwrap</code> with the same argument should succeed.
	 *
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly wraps an object that does.
	 * @throws SQLException if an error occurs while determining whether this is a wrapper
	 *                      for an object with the given interface.
	 * @since 1.6
	 */
	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}
}
