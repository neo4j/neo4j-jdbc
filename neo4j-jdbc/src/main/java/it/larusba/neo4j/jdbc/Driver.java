/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 03/02/16
 */
package it.larusba.neo4j.jdbc;

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
	 * JDBC prefix for the connection url.
	 */
	protected static final String JDBC_PREFIX = "jdbc";

	/**
	 * Driver perfix for the connection url.
	 */
	protected String DRIVER_PREFIX;

	public static String DRIVERS_BOLT = "it.larusba.neo4j.jdbc.bolt.BoltDriver";
	public static String DRIVERS_HTTP = "it.larusba.neo4j.jdbc.http.HttpDriver";
	public static String[] DRIVERS = new String[] { DRIVERS_BOLT, DRIVERS_HTTP };
	static {
		for (String driver : DRIVERS) {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
			}
		}
	}

	/**
	 * Default constructor.
	 *
	 * @param prefix Prefix of the driver for the connection url.
	 */
	protected Driver(String prefix) {
		this.DRIVER_PREFIX = prefix;
	}

	@Override public abstract Connection connect(String url, Properties info) throws SQLException;

	@Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override public int getMajorVersion() {
		return 3;
	}

	@Override public int getMinorVersion() {
		return 0;
	}

	@Override public boolean jdbcCompliant() {
		return false;
	}

	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean acceptsURL(String url) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		String[] pieces = url.split(":");
		if (pieces.length > 2) {
			if (JDBC_PREFIX.equals(pieces[0]) && DRIVER_PREFIX.equals(pieces[1])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parse the url string and construct a properties object.
	 *
	 * @param url The url to parse
	 * @param properties The properties
	 */
	protected Properties parseUrlProperties(String url, Properties properties) {
		if (url.contains("?")) {
			String urlProps = url.substring(url.indexOf('?') + 1);
			String[] props = urlProps.split(",");
			for (String prop : props) {
				int idx = prop.indexOf('=');
				if (idx != -1) {
					String key = prop.substring(0, idx);
					String value = prop.substring(idx + 1);
					properties.put(key, value);
				} else {
					properties.put(prop, "true");
				}
			}
		}
		return properties;
	}
}
