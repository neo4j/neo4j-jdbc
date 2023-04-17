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
package org.neo4j.jdbc.http.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.util.Arrays;
import java.util.Scanner;

import static java.net.HttpURLConnection.HTTP_OK;

@RunWith(Parameterized.class)
public abstract class Neo4jHttpITUtil extends Neo4jHttpUnitTestUtil {

    public static Neo4jContainer<?> createNeo4jContainer() {
        return new Neo4jContainer<>(neo4jImageCoordinates()).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .withAdminPassword(null);
    }

    private static String neo4jImageCoordinates() {
        String neo4jVersion = System.getenv("NEO4J_VERSION");
        if (neo4jVersion == null) neo4jVersion = "4.4";
        String enterpriseEdition = System.getenv("NEO4J_ENTERPRISE_EDITION");
        if (enterpriseEdition == null) enterpriseEdition = "false";
        return String.format("neo4j:%s%s", neo4jVersion, Boolean.parseBoolean(enterpriseEdition) ? "-enterprise": "");
    }

    // Neo4jContainer overwrites the default password so in order to replicate a use-case
    // where we use the default password that needs to be changed we use a GenericContainer

    public static GenericContainer<?> createNeo4jContainerWithDefaultPassword() {
        return new Neo4jContainer<>(neo4jImageCoordinates())
                .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                ;
    }

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList(Boolean.FALSE);
    }

    @Parameterized.Parameter
    public Boolean secureMode;

    @ClassRule
    public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-enterprise")
            .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
            .withAdminPassword(null);

    @BeforeClass
    public static void beforeClass() {
        try (Scanner scanner = new Scanner(Thread.currentThread().getContextClassLoader().getResourceAsStream("data/movie.cyp"));
             org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            scanner.useDelimiter(";");
            while (scanner.hasNext()) {
                final String query = scanner.next();
                if (StringUtils.isNotBlank(query)) {
                    session.run(query);
                }
            }
        }
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();


    protected String getJDBCUrl() {
        if (secureMode) {
			return "jdbc:neo4j:" + neo4j.getHttpsUrl();
		}
		return "jdbc:neo4j:" + neo4j.getHttpUrl();
	}

}
