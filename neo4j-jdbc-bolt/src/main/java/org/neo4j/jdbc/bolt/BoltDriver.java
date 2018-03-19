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
import org.neo4j.jdbc.bolt.impl.BoltNeo4jDriverImpl;

import java.net.URISyntaxException;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriver extends BoltNeo4jDriverImpl {

	public static final String JDBC_BOLT_PREFIX = "bolt";

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

    protected Driver getDriver(String boltUrl, Config config, AuthToken authToken) throws URISyntaxException {
        return GraphDatabase.driver(boltUrl, authToken, config);
    }
}
