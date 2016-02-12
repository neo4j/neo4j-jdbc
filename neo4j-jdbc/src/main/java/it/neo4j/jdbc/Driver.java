/**
 * Copyright (c) 2004-2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Driver implements java.sql.Driver {
	/**
	 * Attempts to make a database connection to the given URL.
	 * The driver should return "null" if it realizes it is the wrong kind
	 * of driver to connect to the given URL.  This will be common, as when
	 * the JDBC driver manager is asked to connect to a given URL it passes
	 * the URL to each loaded driver in turn.
	 * <p>
	 * <P>The driver should throw an <code>SQLException</code> if it is the right
	 * driver to connect to the given URL but has trouble connecting to
	 * the database.
	 * <p>
	 * <P>The {@code Properties} argument can be used to pass
	 * arbitrary string tag/value pairs as connection arguments.
	 * Normally at least "user" and "password" properties should be
	 * included in the {@code Properties} object.
	 * <p>
	 * <B>Note:</B> If a property is specified as part of the {@code url} and
	 * is also specified in the {@code Properties} object, it is
	 * implementation-defined as to which value will take precedence. For
	 * maximum portability, an application should only specify a property once.
	 *
	 * @param url  the URL of the database to which to connect
	 * @param info a list of arbitrary string tag/value pairs as
	 *             connection arguments. Normally at least a "user" and
	 *             "password" property should be included.
	 * @return a <code>Connection</code> object that represents a
	 * connection to the URL
	 * @throws SQLException if a database access error occurs or the url is
	 *                      {@code null}
	 */
	@Override public Connection connect(String url, Properties info) throws SQLException {
		return null;
	}

	/**
	 * Retrieves whether the driver thinks that it can open a connection
	 * to the given URL.  Typically drivers will return <code>true</code> if they
	 * understand the sub-protocol specified in the URL and <code>false</code> if
	 * they do not.
	 *
	 * @param url the URL of the database
	 * @return <code>true</code> if this driver understands the given URL;
	 * <code>false</code> otherwise
	 * @throws SQLException if a database access error occurs or the url is
	 *                      {@code null}
	 */
	@Override public boolean acceptsURL(String url) throws SQLException {
		return false;
	}

	/**
	 * Gets information about the possible properties for this driver.
	 * <p>
	 * The <code>getPropertyInfo</code> method is intended to allow a generic
	 * GUI tool to discover what properties it should prompt
	 * a human for in order to get
	 * enough information to connect to a database.  Note that depending on
	 * the values the human has supplied so far, additional values may become
	 * necessary, so it may be necessary to iterate though several calls
	 * to the <code>getPropertyInfo</code> method.
	 *
	 * @param url  the URL of the database to which to connect
	 * @param info a proposed list of tag/value pairs that will be sent on
	 *             connect open
	 * @return an array of <code>DriverPropertyInfo</code> objects describing
	 * possible properties.  This array may be an empty array if
	 * no properties are required.
	 * @throws SQLException if a database access error occurs
	 */
	@Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	/**
	 * Retrieves the driver's major version number. Initially this should be 1.
	 *
	 * @return this driver's major version number
	 */
	@Override public int getMajorVersion() {
		return 0;
	}

	/**
	 * Gets the driver's minor version number. Initially this should be 0.
	 *
	 * @return this driver's minor version number
	 */
	@Override public int getMinorVersion() {
		return 0;
	}

	/**
	 * Reports whether this driver is a genuine JDBC
	 * Compliant&trade; driver.
	 * A driver may only report <code>true</code> here if it passes the JDBC
	 * compliance tests; otherwise it is required to return <code>false</code>.
	 * <p>
	 * JDBC compliance requires full support for the JDBC API and full support
	 * for SQL 92 Entry Level.  It is expected that JDBC compliant drivers will
	 * be available for all the major commercial databases.
	 * <p>
	 * This method is not intended to encourage the development of non-JDBC
	 * compliant drivers, but is a recognition of the fact that some vendors
	 * are interested in using the JDBC API and framework for lightweight
	 * databases that do not support full database functionality, or for
	 * special databases such as document information retrieval where a SQL
	 * implementation may not be feasible.
	 *
	 * @return <code>true</code> if this driver is JDBC Compliant; <code>false</code>
	 * otherwise
	 */
	@Override public boolean jdbcCompliant() {
		return false;
	}

	/**
	 * Return the parent Logger of all the Loggers used by this driver. This
	 * should be the Logger farthest from the root Logger that is
	 * still an ancestor of all of the Loggers used by this driver. Configuring
	 * this Logger will affect all of the log messages generated by the driver.
	 * In the worst case, this may be the root Logger.
	 *
	 * @return the parent Logger for this driver
	 * @throws SQLFeatureNotSupportedException if the driver does not use
	 *                                         {@code java.util.logging}.
	 * @since 1.7
	 */
	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
