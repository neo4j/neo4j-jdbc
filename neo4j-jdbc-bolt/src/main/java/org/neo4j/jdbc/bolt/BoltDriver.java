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
 * Created on 23/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.*;
import org.neo4j.jdbc.BaseDriver;
import org.neo4j.jdbc.InstanceFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.neo4j.driver.v1.Config.build;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriver extends BaseDriver {

	public final static String JDBC_BOLT_PREFIX = "bolt";
	public final static String JDBC_BOLT_ROUTING_PREFIX = "bolt+routing";
	private final Neo4jDriverSupplier neo4jDriverSupplier;

	/**
	 * Default constructor.
	 * @throws SQLException sqlexception
	 */
	public BoltDriver() throws SQLException {
		this(new Neo4jDriverSupplier());
	}

	// visible for testing
	BoltDriver(Neo4jDriverSupplier neo4jDriverSupplier) throws SQLException {
		super(JDBC_BOLT_PREFIX);
		this.neo4jDriverSupplier = neo4jDriverSupplier;
	}

	@Override public Connection connect(String url, Properties props) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		Connection connection = null;
		if (acceptsURL(url)) {
			String boltUrl = url.replace(BaseDriver.JDBC_PREFIX, "").replaceAll("^(" + JDBC_BOLT_PREFIX + ":)([^/])", "$1//$2");
			try {
				Properties info = parseUrlProperties(boltUrl, props);
				boltUrl = removeUrlProperties(boltUrl, info);
				Config.ConfigBuilder builder = build();
				if (info.containsKey("nossl")) {
					builder = builder.withoutEncryption();
				}
				Config config = builder.toConfig();
				AuthToken authToken = getAuthToken(info);
				Driver driver = neo4jDriverSupplier.supply(boltUrl, authToken, config);
				Session session = driver.session();
				try (Transaction ignored = session.beginTransaction()) {

                }
                BoltConnection boltConnection = new BoltConnection(session, info, url);
				connection = InstanceFactory.debug(BoltConnection.class, boltConnection, BoltConnection.hasDebug(info));
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return connection;
	}

	private AuthToken getAuthToken(Properties properties) {
	    if(properties.isEmpty()) return AuthTokens.none();
        //if (properties.containsKey("user") && properties.containsKey("password")) {
			return AuthTokens.basic(properties.getProperty("user"), properties.getProperty("password"));
		//}
		//return null;
	}

	private String removeUrlProperties(String url, Properties properties) {
		String boltUrl = url;
		if (boltUrl.indexOf("?") != -1) {
			boltUrl = url.substring(0, url.indexOf("?"));
		}
		if (boltUrl.contains(JDBC_BOLT_ROUTING_PREFIX) && properties.contains("routingcontext")) {
			boltUrl += "?routingContext=" + properties.get("routingcontext");
		}
		return boltUrl;
	}
}

class Neo4jDriverSupplier {

	public Driver supply(String boltUrl, AuthToken authToken, Config config) {
		return GraphDatabase.driver(boltUrl, authToken, config);
	}
}
