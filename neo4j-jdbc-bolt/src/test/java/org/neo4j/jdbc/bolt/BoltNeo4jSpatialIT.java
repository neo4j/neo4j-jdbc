package org.neo4j.jdbc.bolt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.types.Point;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
  * Test for the spatial objects (point)
 * @since 3.4
 */
public class BoltNeo4jSpatialIT {
    @ClassRule
    public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

    static Connection connection;

    @Before
    public void cleanDB() throws SQLException {
        neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
        connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
    }

    @AfterClass
    public static void tearDown(){
        JdbcConnectionTestUtils.closeConnection(connection);
    }

    /**
     * ====================
     * Cartesian 2D
     * ====================
     */

    @Test
    public void executeQueryShouldReturnFieldCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ x:3, y:-0.1 })}) RETURN g AS cartesian_2d");

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
        ResultSet rs = statement.executeQuery("RETURN point({ x:3, y:-0.1 }) AS cartesian_2d");

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

    @Test
    public void executeQueryShouldReturnArrayFieldCartesian2D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ x:3, y:-0.1 })]}) RETURN g AS cartesian_2d");

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
        ResultSet rs = statement.executeQuery("RETURN [point({ x:3, y:-0.1 })] AS cartesian_2d");

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
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: point({ x:3, y:-0.1, z:2 })}) RETURN g AS cartesian_3d");

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
        ResultSet rs = statement.executeQuery("RETURN point({ x:3, y:-0.1, z:2 }) AS cartesian_3d");

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

    @Test
    public void executeQueryShouldReturnArrayFieldCartesian3D() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ x:3, y:-0.1, z:2 })]}) RETURN g AS cartesian_3d");

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

    /**
     * ================
     * FLATTEN
     * ================
     */
    @Test
    public void executeQueryShouldReturnArrayGeo3DFlatten() throws SQLException {

        Connection conn = JdbcConnectionTestUtils.getConnection(neo4j,",flatten=1");
        Statement statement = conn.createStatement();

        ResultSet rs = statement.executeQuery("CREATE (g:Geo {position: [point({ latitude: 12, longitude: 56, height: 4321 })]}) RETURN g AS geo_3d");

        assertTrue(rs.next());
        assertEquals(4,rs.getMetaData().getColumnCount());

        Object points = rs.getObject("geo_3d.position");
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
        conn.close();
    }

    /**
     * ================
     * GET OBJECT
     * ================
     */

    @Test
    public void executeGetObjectShouldReturnCartesian2D() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ x:3, y:-0.1 }) AS point");

        rs.next();

        Point object = rs.getObject(1, Point.class);
        assertEquals("Point{srid=7203, x=3.0, y=-0.1}", object.toString());

        Point label = rs.getObject("point", Point.class);
        assertEquals("Point{srid=7203, x=3.0, y=-0.1}", label.toString());


        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnCartesian3D() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ x:3, y:-0.1, z:2 }) AS point");

        rs.next();

        Point object = rs.getObject(1, Point.class);
        assertEquals("Point{srid=9157, x=3.0, y=-0.1, z=2.0}", object.toString());

        Point label = rs.getObject("point", Point.class);
        assertEquals("Point{srid=9157, x=3.0, y=-0.1, z=2.0}", label.toString());


        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnCartesianGeo2D() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ latitude: 12, longitude: 56 }) AS point");

        rs.next();

        Point object = rs.getObject(1, Point.class);
        assertEquals("Point{srid=4326, x=56.0, y=12.0}", object.toString());

        Point label = rs.getObject("point", Point.class);
        assertEquals("Point{srid=4326, x=56.0, y=12.0}", label.toString());


        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnCartesianGeo3D() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN point({ latitude: 12, longitude: 56, height: 4321 }) AS point");

        rs.next();

        Point object = rs.getObject(1, Point.class);
        assertEquals("Point{srid=4979, x=56.0, y=12.0, z=4321.0}", object.toString());

        Point label = rs.getObject("point", Point.class);
        assertEquals("Point{srid=4979, x=56.0, y=12.0, z=4321.0}", label.toString());

        rs.close();
        statement.close();
    }
}
