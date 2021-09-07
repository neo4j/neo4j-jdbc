/*
 * Copyright (c) 2018 LARUS Business Automation [http://www.larus-ba.it]
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
 */
package org.neo4j.jdbc.boltrouting;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.jdbc.bolt.cache.BoltDriverCache;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jDriverImpl;
import org.neo4j.jdbc.utils.BoltNeo4jUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
public class BoltRoutingNeo4jDriver extends BoltNeo4jDriverImpl {

    public static final String JDBC_BOLT_ROUTING_PREFIX = "^neo4j(\\+s|\\+ssc)?$";

    public static final String ROUTING_CONTEXT = "routing";
    public static final String ALTERNATIVE_SERVERS = "servers";
    public static final String BOOKMARK = "bookmark";
    public static final String LIST_SEPARATOR = ";";
    public static final String CUSTOM_ROUTING_POLICY_SEPARATOR = "&";

    private static final BoltDriverCache cache = new BoltDriverCache(params -> {
        for (URI uri : params.getRoutingUris()) {
            final Driver driver = GraphDatabase.driver(uri, params.getAuthToken(), params.getConfig());
            try {
                driver.verifyConnectivity();
                return driver;
            } catch (ServiceUnavailableException e) {
                BoltNeo4jUtils.closeSafely(driver, null);
            } catch (Throwable e) {
                // for any other errors, we first close the driver and then rethrow the original error out.
                BoltNeo4jUtils.closeSafely(driver, null);
                throw e;
            }
        }
        throw new ServiceUnavailableException( "Failed to discover an available server" );
    });

    static {
        try {
            BoltRoutingNeo4jDriver driver = new BoltRoutingNeo4jDriver();
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public BoltRoutingNeo4jDriver() throws SQLException {
        super(JDBC_BOLT_ROUTING_PREFIX);
    }

    public BoltRoutingNeo4jDriver(String prefix) throws SQLException {
        super(prefix);
    }

    private boolean isSchemaMatchPrefix(String url) {
        URI uri = URI.create(url);
        return uri.getScheme().matches(this.getPrefix());
    }

    @Override
    protected Driver getDriver(List<URI> routingUris, Config config, AuthToken authToken, Properties info) throws URISyntaxException {
        return cache.getDriver(routingUris, config, authToken, info);
    }

    @Override
    protected Properties getRoutingContext(String url, Properties properties) {
        Properties props = new Properties();
        if (isSchemaMatchPrefix(url) && properties.containsKey(ROUTING_CONTEXT)) {
            List<String> routingParams;
            if (properties.get(ROUTING_CONTEXT) instanceof String) {
                routingParams = Arrays.asList(properties.getProperty(ROUTING_CONTEXT));
            } else {
                routingParams = (List) properties.get(ROUTING_CONTEXT);
            }
            for (String routingParam : routingParams) {
                if (routingParam.startsWith(ALTERNATIVE_SERVERS))
                    props.put(ALTERNATIVE_SERVERS, routingParam.substring(ALTERNATIVE_SERVERS.length() + 1));
                else
                    props.put(ROUTING_CONTEXT, routingParam);
            }
        }
        return props;
    }

    @Override
    protected String addRoutingPolicy(String url, Properties properties) {
        String boltUrl = url;
        if (isSchemaMatchPrefix(url) && properties.containsKey(ROUTING_CONTEXT)) {
            boltUrl += "?" + properties.getProperty(ROUTING_CONTEXT).replaceAll(LIST_SEPARATOR, CUSTOM_ROUTING_POLICY_SEPARATOR);
        }
        return boltUrl;
    }

    @Override
    protected List<URI> buildRoutingUris(String boltUrl, Properties properties) throws URISyntaxException {
        URI firstServer = new URI(boltUrl);
        List<URI> routingUris = new ArrayList<>();
        String[] servers = firstServer.getAuthority().split("\\,");
        for (String server : servers) {
            routingUris.add(new URI(firstServer.getScheme(), server, firstServer.getPath(), firstServer.getQuery(), firstServer.getFragment()));
        }
        if (properties.containsKey((ALTERNATIVE_SERVERS))) {
            String alternativeServers = properties.getProperty(ALTERNATIVE_SERVERS);
            String[] alternativeServerList = alternativeServers.split("\\" + LIST_SEPARATOR);
            for (String alternativeServer: alternativeServerList) {
                routingUris.add(new URI(firstServer.getScheme(), alternativeServer, firstServer.getPath(), firstServer.getQuery(), firstServer.getFragment()));
            }
        }
        return routingUris;
    }

    // visible for testing
    public static void clearCache() {
        cache.clear();
    }
}
