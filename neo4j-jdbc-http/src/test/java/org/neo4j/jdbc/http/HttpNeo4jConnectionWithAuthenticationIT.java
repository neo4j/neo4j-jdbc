package org.neo4j.jdbc.http;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.Neo4jContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class HttpNeo4jConnectionWithAuthenticationIT {

    @ClassRule
    public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:4.3.0-enterprise").withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");

    @Test(expected = SQLException.class)
    public void shouldThrowExceptionWithoutAuthenticationWhenItsRequired() throws SQLException {
        // given: neo4j server requires the authentication

        try { // when
            DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl());
        } catch (SQLException e) { // then
            assertEquals("Authentication required", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldConnectWithAuthWhenRequired() throws SQLException {
        // given: neo4j server requires the authentication

        // when
        Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getHttpUrl(), "neo4j", "password");

        // then
        connection.close();
    }
}
