package org.neo4j.jdbc.http;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.jdbc.http.test.Neo4jHttpITUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.junit.Assert.assertThat;

public class HttpNeo4jDatabaseMetaDataIT extends Neo4jHttpITUtil {

    @After
    public void tearDown() throws SQLException {
        try (Connection connection = DriverManager.getConnection(getJDBCUrl());
             Statement statement = connection.createStatement()) {

            statement.execute("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    public void getSystemFunctionsShouldReturnTheAvailableFunctions() throws SQLException {
        try (Connection connection = DriverManager.getConnection(getJDBCUrl())) {
            DatabaseMetaData metaData = connection.getMetaData();

            String systemFunctions = metaData.getSystemFunctions();

            assertThat(stream(systemFunctions.split(","))
                    .filter(s -> !s.isEmpty())
                    .count(), Matchers.greaterThan(0L));
        }
    }
}