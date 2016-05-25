/*
 *
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
 * Created on 19/4/2016
 *
 */

package org.neo4j.jdbc;

import it.larusba.neo4j.jdbc.bolt.BoltDriver;
import it.larusba.neo4j.jdbc.http.HttpDriver;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Driver extends it.larusba.neo4j.jdbc.Driver {

	/**
	 * Prefix/class hashMap of all available Driver.
	 */
	private final Map<String, Class> DRIVERS = new HashMap() {{
		put(BoltDriver.JDBC_BOLT_PREFIX, BoltDriver.class);
		put(HttpDriver.JDBC_HTTP_PREFIX, HttpDriver.class);
	}};

	/**
	 * Default constructor.
	 */
	public Driver() throws SQLException {
		super(null);
	}

	@Override public Connection connect(String url, Properties info) throws SQLException {
		return getDriver(url).connect(url, info);
	}

	/**
	 * Retrieve the correspondig driver from the JDBC url.
	 * @param url The JDBC url
	 * @return The driver
	 * @throws SQLException
	 */
	private it.larusba.neo4j.jdbc.Driver getDriver(String url) throws SQLException {
		it.larusba.neo4j.jdbc.Driver driver = null;

		if (url == null) {
			throw new SQLException("null is not a valid url");
		}

		try {

			// We search the driver prefix from the url
			String[] pieces = url.split(":");
			if (pieces.length > 2 && JDBC_PREFIX.equals(pieces[0])) {
				String prefix = pieces[1];

				// We look into driver map is it known
				if (DRIVERS.containsKey(prefix)) {
					Constructor constructor = DRIVERS.get(prefix).getDeclaredConstructor();
					driver = (it.larusba.neo4j.jdbc.Driver) constructor.newInstance();
				}
			}
		} catch (Exception e) {
			throw new SQLException(e);
		}

		if(driver == null) {
			throw new SQLException("Cannot find a suitable driver from the url");
		}

		return driver;
	}
}
