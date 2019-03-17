package org.neo4j.jdbc.http;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.harness.junit.Neo4jRule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class HttpNeo4jConnectionWithAuthenticationIT {

    @Rule
    public Neo4jRule neo4jRule = new Neo4jRule().withConfig(GraphDatabaseSettings.auth_enabled, "true");

    @Test(expected = SQLException.class)
    public void shouldThrowExceptionWithoutAuthenticationWhenItsRequired() throws SQLException {
        // given: neo4j server requires the authentication

        try { // when
            DriverManager.getConnection("jdbc:neo4j:" + neo4jRule.httpURI().toString());
        } catch (SQLException e) { // then
            assertEquals("Authentication required", e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldConnectWithAuthWhenRequired() throws SQLException {
        // given: neo4j server requires the authentication

        // when
        Connection connection = DriverManager.getConnection("jdbc:neo4j:" + neo4jRule.httpURI().toString(), "neo4j", "neo4j");

        // then
        connection.close();
    }
}
