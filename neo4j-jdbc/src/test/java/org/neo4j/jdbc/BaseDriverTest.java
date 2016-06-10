package org.neo4j.jdbc;

import org.junit.Before;
import org.junit.Test;

import java.sql.*;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author mh
 * @since 10.06.16
 */
public class BaseDriverTest {

    private BaseDriver baseDriver;

    @Before
    public void setUp() throws Exception {
        baseDriver = new BaseDriver("http") {
            public Connection connect(String url, Properties info) throws SQLException {
                return null;
            }
        };
    }

    @Test
    public void acceptsURL() throws Exception {
        assertTrue(baseDriver.acceptsURL("jdbc:neo4j:http://localhost"));
        assertTrue(baseDriver.acceptsURL("jdbc:neo4j:http://localhost:7373"));
        assertTrue(baseDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?noSSL"));
        assertTrue(baseDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?user=neo4j"));
        assertTrue(baseDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?user=neo4j,password=test"));
    }

    @Test
    public void parseUrlProperties() throws Exception {
        Properties props = new Properties();
        assertEquals(props,baseDriver.parseUrlProperties("jdbc:neo4j:http://localhost",null));
        props.setProperty("user","neo4j");
        assertEquals(props,baseDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j",null));
        props.setProperty("password","a test/paßword");
        assertEquals(props,baseDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j&password=a%20test%2Fpa%C3%9Fword",null));
        assertEquals(props,baseDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j,password=a%20test%2Fpa%C3%9Fword",null));
    }

}
