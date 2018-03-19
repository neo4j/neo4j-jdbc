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

import org.neo4j.driver.v1.*;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jDriverImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
public class BoltRoutingNeo4jDriver extends BoltNeo4jDriverImpl {

    public static final String JDBC_BOLT_ROUTING_PREFIX = "bolt\\+routing";

    public static final String ROUTING_CONTEXT          = "routingcontext";
    public static final String BOOKMARK                 = "bookmark";

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

    protected Driver getDriver(String boltRoutingUrl, Config config, AuthToken authToken) throws URISyntaxException {
        return GraphDatabase.routingDriver(Arrays.asList(new URI(boltRoutingUrl)), authToken, config);
    }
}
