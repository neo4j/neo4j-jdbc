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
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http;

import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.InstanceFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC Driver class for the HTTP connector.
 */
public class HttpDriver extends Neo4jDriver {

	public static final String JDBC_HTTP_PREFIX = "http[s]?";

	static {
		try {
			HttpDriver driver = new HttpDriver();
			DriverManager.registerDriver(driver);
        } catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Default constructor.
	 * 
	 * @throws SQLException sqlexception
	 */
	public HttpDriver() throws SQLException {
		super(JDBC_HTTP_PREFIX);
	}

	@Override public Connection connect(String url, Properties params) throws SQLException {
		Connection connection = null;
		try {
			if (acceptsURL(url)) {
				URL neo4jUrl = new URL(url.replace(Neo4jDriver.JDBC_PREFIX, "").replaceAll("^(" + JDBC_HTTP_PREFIX + ":)([^/])", "$1//$2"));
				Properties props = parseUrlProperties(url, params);
				String host = neo4jUrl.getHost();
				Boolean secure = Boolean.FALSE;
				// default port for http
				int port = 7474;
				// default port for https
				if ("https".equals(neo4jUrl.getProtocol())) {
					port = 7473;
					secure = Boolean.TRUE;
				}
				// if a port a specified, we take it
				if (neo4jUrl.getPort() > 0) {
					port = neo4jUrl.getPort();
				}
				connection = InstanceFactory.debug(new HttpNeo4jConnection(host, port, secure, props, url));
			}
		} catch (MalformedURLException e) {
			throw new SQLException(e);
		}

		return connection;
	}

}
