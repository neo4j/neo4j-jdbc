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

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.jdbc.bolt.cache.BoltDriverCache;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jDriverImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriver extends BoltNeo4jDriverImpl {

	public static final String JDBC_BOLT_PREFIX = "bolt";
	private static final BoltDriverCache cache = new BoltDriverCache(params ->
	{
		return GraphDatabase.driver(params.getRoutingUris().get(0), params.getAuthToken(), params.getConfig());
	}
	);

	static {
		try {
			BoltDriver driver = new BoltDriver();
			DriverManager.registerDriver(driver);
		} catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public BoltDriver() throws SQLException {
		super(JDBC_BOLT_PREFIX);
	}

	@Override
	protected Driver getDriver(List<URI> routingUris, Config config, AuthToken authToken, Properties info) throws URISyntaxException {
		return cache.getDriver(routingUris, config, authToken, info);
	}

	@Override
	protected Properties getRoutingContext(String url, Properties properties) {
		return new Properties();
	}

	@Override
	protected String addRoutingPolicy(String url, Properties properties) {
		return url;
	}

    @Override
    protected List<URI> buildRoutingUris(String boltUrl, Properties properties) throws URISyntaxException {
        return Arrays.asList(new URI(boltUrl));
    }
}
