/*
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
package org.neo4j.jdbc;

import org.neo4j.jdbc.utils.ExceptionBuilder;
import org.neo4j.jdbc.utils.Neo4jJdbcRuntimeException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Neo4jDriver implements java.sql.Driver {

	/**
	 * JDBC prefix for the connection url.
	 */
	protected static final String JDBC_PREFIX = "jdbc:neo4j:";

	/**
	 * Driver prefix for the connection url.
	 */
	private String driverPrefix;

	/**
	 * Constructor for extended class.
	 *
	 * @param prefix Prefix of the driver for the connection url.
	 */
	protected Neo4jDriver(String prefix) {
		this.driverPrefix = prefix;
	}

	@Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override public int getMajorVersion() {
		return 3;
	}

	@Override public int getMinorVersion() {
		return 4;
	}

	@Override public boolean jdbcCompliant() {
		return false;
	}

	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public boolean acceptsURL(String url) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		String[] pieces = url.split(":");
		if (pieces.length > 3 && url.startsWith(JDBC_PREFIX)) {
			if (driverPrefix != null) {
				if (pieces[2].matches(driverPrefix)) {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	protected String getPrefix() {
		return this.driverPrefix;
	}

	/**
	 * Parse the url string and construct a properties object.
	 *
	 * @param url The url to parse
	 * @param params The properties
	 * @return the properties
	 */
	protected Properties parseUrlProperties(String url, Properties params) {
		Properties properties = new Properties();
		if(params != null) {
			for (Map.Entry<Object, Object> entry : params.entrySet()) {
				properties.put(entry.getKey().toString().toLowerCase(),entry.getValue());
			}
		}
		if (url.contains("?")) {
			String urlProps = url.substring(url.indexOf('?') + 1);
			urlProps = decodeUrlComponent(urlProps);
			String[] props = urlProps.split("[,&]");
			for (String prop : props) {
				int idx1 = prop.indexOf('=');
				int idx2 = prop.indexOf(':');
				int idx = (idx1 != -1 && idx2 != -1) ? Math.min(idx1, idx2) : Math.max(idx1, idx2);
                if (idx != -1) {
					String key = prop.substring(0, idx);
                    String value = prop.substring(idx + 1);
					if (properties.containsKey(key.toLowerCase())) {
					    properties.put(key.toLowerCase(), Arrays.asList(properties.getProperty(key.toLowerCase()), value));
                    } else {
                        properties.put(key.toLowerCase(), value);
                    }
				} else {
					properties.put(prop.toLowerCase(), "true");
				}
			}
		}

		return properties;
	}

	private String decodeUrlComponent(String urlProps) {
		try {
			return URLDecoder.decode(urlProps, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Neo4jJdbcRuntimeException(e);
		}
	}

}
