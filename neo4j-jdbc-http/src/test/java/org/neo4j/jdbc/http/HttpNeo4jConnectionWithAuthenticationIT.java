package org.neo4j.jdbc.http;

import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.http.test.Neo4jHttpITUtil;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.neo4j.jdbc.http.test.Neo4jHttpITUtil.createNeo4jContainer;
import static org.neo4j.jdbc.http.test.Neo4jHttpITUtil.createNeo4jContainerWithDefaultPassword;

public class HttpNeo4jConnectionWithAuthenticationIT {

    @ClassRule
    public static final Neo4jContainer<?> neo4j = (Neo4jContainer) createNeo4jContainerWithDefaultPassword();



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
