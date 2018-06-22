package org.neo4j.jdbc.http;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.jdbc.http.test.Neo4jHttpITUtil;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for the spatial objects (point)
 */
public class HttpNeo4jSpatialIT extends Neo4jHttpITUtil {

    Connection connection;

    @Before
    public void cleanDB() throws SQLException {
        connection = DriverManager.getConnection(getJDBCUrl());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("MATCH (n) DETACH DELETE n;");
        }
    }

    //RETURN point({ x:3, y:0 }) AS cartesian_2d, point({ x:0, y:4, z:1 }) AS cartesian_3d, point({ latitude: 12, longitude: 56 }) AS geo_2d, point({ latitude: 12, longitude: 56, height: 1000 }) AS geo_3d

    /**
     * ====================
     * Cartesian 2D
     * ====================
     */

    @Test
    public void executeQueryShouldReturnFieldCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ x: 3, y: -0.1 })}) RETURN g AS cartesian_2d;");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object point = geo.get("position");
        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(7203, pointMap.get("srid"));
        assertEquals("cartesian", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ x: 3, y: -0.1 }) AS cartesian_2d;");

        assertTrue(rs.next());
        Object point = rs.getObject(1);

        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(7203, pointMap.get("srid"));
        assertEquals("cartesian", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));

        rs.close();
        statement.close();
    }

    @Ignore
    @Test
    public void executeQueryShouldReturnArrayFieldCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [ point({ x: 3, y: -0.1 }) ] }) RETURN g AS cartesian_2d;");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object points = geo.get("position");
        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(7203, pointMap.get("srid"));
        assertEquals("cartesian", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [point({ x: 3, y: -0.1 })] AS cartesian_2d");

        assertTrue(rs.next());
        Object points = rs.getObject(1);

        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(7203, pointMap.get("srid"));
        assertEquals("cartesian", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));

        rs.close();
        statement.close();
    }

    /**
     * ====================
     * Cartesian 3D
     * ====================
     */
    @Test
    public void executeQueryShouldReturnFieldCartesian3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ x: 3, y: -0.1, z: 2 })}) RETURN g AS cartesian_3d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object point = geo.get("position");
        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(9157, pointMap.get("srid"));
        assertEquals("cartesian-3d", pointMap.get("crs"));
        assertEquals(((double)(2.0)), pointMap.get("z"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnCartesian3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ x: 3, y: -0.1, z: 2 }) AS cartesian_3d");

        assertTrue(rs.next());
        Object point = rs.getObject(1);

        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(9157, pointMap.get("srid"));
        assertEquals("cartesian-3d", pointMap.get("crs"));
        assertEquals(((double)(2.0)), pointMap.get("z"));

        rs.close();
        statement.close();
    }

    @Ignore
    @Test
    public void executeQueryShouldReturnArrayFieldCartesian3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ x: 3, y: -0.1, z: 2 })]}) RETURN g AS cartesian_3d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object points = geo.get("position");
        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(9157, pointMap.get("srid"));
        assertEquals("cartesian-3d", pointMap.get("crs"));
        assertEquals(((double)(2.0)), pointMap.get("z"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayCartesian3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [point({ x:3, y:-0.1, z:2 })] AS cartesian_3d");

        assertTrue(rs.next());
        Object points = rs.getObject(1);

        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(3.0)), pointMap.get("x"));
        assertEquals(((double)(-0.1)), pointMap.get("y"));
        assertEquals(9157, pointMap.get("srid"));
        assertEquals("cartesian-3d", pointMap.get("crs"));
        assertEquals(((double)(2.0)), pointMap.get("z"));

        rs.close();
        statement.close();
    }

    /**
     * ====================
     * Cartesian GEO 2D
     * ====================
     */
    @Test
    public void executeQueryShouldReturnFieldGeo2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ latitude: 12, longitude: 56 })}) RETURN g AS geo_2d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object point = geo.get("position");
        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(4326, pointMap.get("srid"));
        assertEquals("wgs-84", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));
        assertFalse(pointMap.containsKey("height"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnGeo2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ latitude: 12, longitude: 56 }) AS geo_2d");

        assertTrue(rs.next());
        Object point = rs.getObject(1);

        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(4326, pointMap.get("srid"));
        assertEquals("wgs-84", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));
        assertFalse(pointMap.containsKey("height"));

        rs.close();
        statement.close();
    }

    @Ignore
    @Test
    public void executeQueryShouldReturnArrayFieldGeo2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ latitude: 12, longitude: 56 })]}) RETURN g AS geo_2d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object points = geo.get("position");
        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(4326, pointMap.get("srid"));
        assertEquals("wgs-84", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));
        assertFalse(pointMap.containsKey("height"));
        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayGeo2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [point({ latitude: 12, longitude: 56  })] AS geo_2d");

        assertTrue(rs.next());
        Object points = rs.getObject(1);

        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(4326, pointMap.get("srid"));
        assertEquals("wgs-84", pointMap.get("crs"));
        assertFalse(pointMap.containsKey("z"));
        assertFalse(pointMap.containsKey("height"));

        rs.close();
        statement.close();
    }

    /**
     * ====================
     * Cartesian GEO 3D
     * ====================
     */
    @Test
    public void executeQueryShouldReturnFieldGeo3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ latitude: 12, longitude: 56, height: 4321 })}) RETURN g AS geo_3d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object point = geo.get("position");
        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(4321)), pointMap.get("z"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(((double)(4321)), pointMap.get("height"));
        assertEquals(4979, pointMap.get("srid"));
        assertEquals("wgs-84-3d", pointMap.get("crs"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnGeo3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ latitude: 12, longitude: 56, height: 4321 }) AS geo_3d");

        assertTrue(rs.next());
        Object point = rs.getObject(1);

        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(4321)), pointMap.get("z"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(((double)(4321)), pointMap.get("height"));
        assertEquals(4979, pointMap.get("srid"));
        assertEquals("wgs-84-3d", pointMap.get("crs"));

        rs.close();
        statement.close();
    }

    @Ignore
    @Test
    public void executeQueryShouldReturnArrayFieldGeo3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ latitude: 12, longitude: 56, height: 4321 })]}) RETURN g AS geo_3d");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object points = geo.get("position");
        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(4321)), pointMap.get("z"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(((double)(4321)), pointMap.get("height"));
        assertEquals(4979, pointMap.get("srid"));
        assertEquals("wgs-84-3d", pointMap.get("crs"));
        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayGeo3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [point({ latitude: 12, longitude: 56, height: 4321 })] AS geo_3d");

        assertTrue(rs.next());
        Object points = rs.getObject(1);

        assertTrue(points instanceof List);
        List pointList = (List) points;
        assertEquals(1, pointList.size());
        Object point = pointList.get(0);

        assertTrue(point instanceof Map);
        Map pointMap = (Map) point;
        assertEquals(((double)(56.0)), pointMap.get("x"));
        assertEquals(((double)(12)), pointMap.get("y"));
        assertEquals(((double)(4321)), pointMap.get("z"));
        assertEquals(((double)(56.0)), pointMap.get("longitude"));
        assertEquals(((double)(12)), pointMap.get("latitude"));
        assertEquals(((double)(4321)), pointMap.get("height"));
        assertEquals(4979, pointMap.get("srid"));
        assertEquals("wgs-84-3d", pointMap.get("crs"));

        rs.close();
        statement.close();
    }

}
