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
public class Neo4jDriverTest {

    private Neo4jDriver neo4jDriver;

    @Before
    public void setUp() throws Exception {
        neo4jDriver = new Neo4jDriver("http") {
            public Connection connect(String url, Properties info) throws SQLException {
                return null;
            }
        };
    }

    @Test
    public void acceptsURL() throws Exception {
        assertTrue(neo4jDriver.acceptsURL("jdbc:neo4j:http://localhost"));
        assertTrue(neo4jDriver.acceptsURL("jdbc:neo4j:http://localhost:7373"));
        assertTrue(neo4jDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?nossl"));
        assertTrue(neo4jDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?user=neo4j"));
        assertTrue(neo4jDriver.acceptsURL("jdbc:neo4j:http://localhost:7373?user=neo4j,password=test"));
        assertFalse(neo4jDriver.acceptsURL("jdbc:mysql://localhost:3306/sakila"));
        assertFalse(neo4jDriver.acceptsURL("jdbc:postgresql://localhost/test"));
    }

    @Test
    public void parseUrlProperties() throws Exception {
        Properties props = new Properties();
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost",null));
        props.setProperty("user","neo4j");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j",null));
        props.setProperty("password","a test/paßword");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j&password=a%20test%2Fpa%C3%9Fword",null));
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j,password=a%20test%2Fpa%C3%9Fword",null));
        props.setProperty("password","a test pa&word");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j&password=a%20test%20pa%26word",null));
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j,password=a%20test%20pa%26word",null));
        props.setProperty("password","a test pa,word");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j&password=a%20test%20pa%2cword",null));
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user=neo4j,password=a%20test%20pa%2cword",null));
        props.setProperty("password","a test pa=word");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user:neo4j&password%3da%20test%20pa%3dword",null));
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user:neo4j,password%3da%20test%20pa%3dword",null));
        props.setProperty("password","a test pa=word");
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user:neo4j&password:a%20test%20pa%3dword",null));
        assertEquals(props, neo4jDriver.parseUrlProperties("jdbc:neo4j:http://localhost?user:neo4j,password:a%20test%20pa%3dword",null));
    }

}
