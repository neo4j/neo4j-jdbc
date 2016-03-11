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
 * Created on 23/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriver extends Driver {

	static {
		try {
			DriverManager.registerDriver(new BoltDriver());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String JDBC_PREFIX = "jdbc";
	private static final String BOLT_PREFIX = "bolt";

	public BoltDriver() {
	}

	@Override public Connection connect(String url, Properties info) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		Connection connection = null;
		if (acceptsURL(url)) {
			url = url.replace("jdbc:", "");
			try {
				Properties props = new Properties();
				parseUrlProperties(url, props);
				connection = BoltConnection.instantiate(GraphDatabase.driver(url).session(), props);

			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return connection;
	}

	@Override public boolean acceptsURL(String url) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		String[] pieces = url.split(":");
		if (pieces.length > 2) {
			if (JDBC_PREFIX.equals(pieces[0]) && BOLT_PREFIX.equals(pieces[1])) {
				return true;
			}
		}
		return false;
	}

	void parseUrlProperties(String s, Properties properties) {
		if (s.contains("?")) {
			String urlProps = s.substring(s.indexOf('?') + 1);
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
	}
}
