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

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.neo4j.TestcontainersCausalCluster;
import org.neo4j.jdbc.bolt.BoltDriver;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assume.assumeNotNull;

/**
 * @author AgileLARUS
 * @since 3.3.1
 */
@Ignore
public class BoltRoutingIT {
    private static final String NEO4J_USER = "neo4j";
    private static final String NEO4J_PASSWORD = "jdbc";

//    private String connectionUrl = "jdbc:neo4j:bolt+routing://localhost:17681?noSsl&debug=true&routing:policy=EU&routing:servers=localhost:17682;localhost:17683;localhost:17684;localhost:17685;localhost:17686;localhost:17687";
//    private String connectionUrl2 = "jdbc:neo4j:bolt+routing://localhost:17681,localhost:17682,localhost:17683,localhost:17684,localhost:17685,localhost:17686,localhost:17687?noSsl&debug=true&routing:policy=EU";

    private static TestcontainersCausalCluster cluster;

    @BeforeClass
    public static void beforeClass() {
        try {
            cluster = TestcontainersCausalCluster.create(3, 1, Duration.ofMinutes(4), Collections.emptyMap());
            assumeNotNull(cluster);
        } catch (Exception ignored) {}
    }

    @AfterClass
    public static void afterClass() {
        if (cluster != null) {
            cluster.close();
        }
        BoltRoutingNeo4jDriver.clearCache();
        BoltDriver.clearCache();
    }

    @Rule public ExpectedException expectedEx = ExpectedException.none();

    @After public void tearDown() throws Exception {
        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("match (t) detach delete t");
            }
            connection.commit();
        }
    }

    //@Ignore
    @Test public void shouldAccessReadReplicaNodes() throws SQLException {

        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("match (t:BoltRoutingTest) return count(t) as tot")) {
                    while (resultSet.next()) {
                        System.err.println(resultSet.getLong("tot"));
                    }
                }
            }
        }
    }

    @Test public void shouldAccessReadReplicaNodes2() throws SQLException {

        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getAllMembersURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("match (t:BoltRoutingTest) return count(t) as tot")) {
                    while (resultSet.next()) {
                        System.err.println(resultSet.getLong("tot"));
                    }
                }
            }
        }
    }

    //@Ignore
    @Test public void shouldFailWritingOnReadReplicaNodes() throws SQLException {

        expectedEx.expect(SQLException.class);

        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {
            connection.setReadOnly(true);

            try (Statement statement = connection.createStatement()) {
                statement.execute("create (:BoltRoutingTest { protocol: 'BOLT+ROUTING' })");
            }

            connection.commit();
        }
    }

    //@Ignore
    @Test public void shouldUseBookmarkToReadYourOwnWrites() throws SQLException {

        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {

            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute("create (:BoltRoutingTest { protocol: 'BOLT+ROUTING' })");
            }
            connection.commit();

            String bookmark = connection.getClientInfo(BoltRoutingNeo4jDriver.BOOKMARK);
            Assert.assertNotNull(bookmark);

            connection.setReadOnly(true);

            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("match (t:BoltRoutingTest) return count(t) as tot")) {
                    if (resultSet.next()) {
                        Long tot = resultSet.getLong("tot");
                        Assert.assertEquals(new Long(1), tot);
                    }
                }
            }
            connection.commit();
        }
    }

    //@Ignore
    @Test public void shouldPassTheBookMark() throws SQLException {
        List<String> bookmarks = new ArrayList<>();
        bookmarks.add(insertAndGetBookmark());
        bookmarks.add(insertAndGetBookmark());

        String bookmarksAsString = String.join(",", bookmarks);
        try  (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {

            connection.setClientInfo(BoltRoutingNeo4jDriver.BOOKMARK, bookmarksAsString);
            connection.setReadOnly(true);

            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("match (t:BoltRoutingTest) return count(t) as tot")) {
                    if (resultSet.next()) {
                        long tot = resultSet.getLong("tot");
                        Assert.assertEquals(2L, tot);
                    }
                }
            }
        }
    }

    private String insertAndGetBookmark() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:neo4j:" + cluster.getURI().toString(), NEO4J_USER, NEO4J_PASSWORD)) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute("create (:BoltRoutingTest { protocol: 'BOLT+ROUTING' })");
            }
            connection.commit();

            final String bookmark = connection.getClientInfo(BoltRoutingNeo4jDriver.BOOKMARK);
            Assert.assertNotNull(bookmark);
            return bookmark;
        }
    }
}
