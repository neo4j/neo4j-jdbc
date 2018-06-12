package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.data.StatementData;

import java.sql.*;

import static org.junit.Assert.*;

/**
 * Test for the spatial objects (point)
 */
public class BoltNeo4jSpatialIT {
    @ClassRule
    public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

    Connection connection;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,user=neo4j,password=neo4j");
    }

    @Before
    public void cleanDB() throws SQLException {
        neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
        connection = getConnection();
    }

    //RETURN point({ x:3, y:0 }) AS cartesian_2d, point({ x:0, y:4, z:1 }) AS cartesian_3d, point({ latitude: 12, longitude: 56 }) AS geo_2d, point({ latitude: 12, longitude: 56, height: 1000 }) AS geo_3d

    @Test
    public void executeQueryShouldReturnCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ x:3, y:0 }) AS cartesian_2d");

        assertTrue(rs.next());
        Object point = rs.getObject(1);
        System.out.println("point = " + point);
        //assertEquals("test", rs.getString(1));
        assertFalse(rs.next());

    }
}
