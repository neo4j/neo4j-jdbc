package org.neo4j.jdbc.bolt;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the date types
 * @since 3.4
 */
public class BoltNeo4jDateIT {
    @ClassRule
    public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

    Connection connection;

    @Before
    public void cleanDB() throws SQLException {
        neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
        connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
    }

    /*
    =============================
            DATETIME
    TODO: manage the timezone
    =============================
     */

    @Test
    public void executeQueryShouldReturnFieldDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: datetime('2015-06-24T12:50:35.556+0100') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Timestamp);

        java.sql.Timestamp timestamp = (Timestamp) when;

        assertEquals(1435146635556L,timestamp.getTime());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN datetime('2015-06-24T12:50:35.556+0100') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.TIMESTAMP_WITH_TIMEZONE, rs.getMetaData().getColumnType(1));

        java.sql.Timestamp timestamp = rs.getTimestamp(1);
        assertEquals(1435146635556L,timestamp.getTime());

        java.sql.Timestamp timestampByString = rs.getTimestamp("event");
        assertEquals(1435146635556L,timestampByString.getTime());

        /*
        java.sql.Timestamp timestampByIntCal = rs.getTimestamp(1, Calendar.getInstance());
        assertEquals(1435146635556L,timestampByIntCal.getTime());

        java.sql.Timestamp timestampByStringCal = rs.getTimestamp("event", Calendar.getInstance());
        assertEquals(1435146635556L,timestampByStringCal.getTime());
        */

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(1435146635556L, ((java.sql.Timestamp)when).getTime());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [datetime('2015-06-24T12:50:35.556+0100')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(1435146635556L, ((java.sql.Timestamp)when).getTime());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [datetime('2015-06-24T12:50:35.556+0100')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());
        Object when = eventList.get(0);

        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(1435146635556L, ((java.sql.Timestamp)when).getTime());

        rs.close();
        statement.close();
    }

    /*
    =============================
            LOCAL DATETIME
    =============================
    */

    @Test
    public void executeQueryShouldReturnFieldLocalDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: localdatetime('2015-12-31T19:32:24') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Timestamp);

        java.sql.Timestamp timestamp = (Timestamp) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(32, cal.get(Calendar.MINUTE));
        assertEquals(24, cal.get(Calendar.SECOND));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnLocalDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localdatetime('2015-12-31T19:32:24') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.TIMESTAMP, rs.getMetaData().getColumnType(1));

        java.sql.Timestamp timestamp = rs.getTimestamp(1);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(32, cal.get(Calendar.MINUTE));
        assertEquals(24, cal.get(Calendar.SECOND));

        java.sql.Timestamp timestampByString = rs.getTimestamp("event");
        assertEquals(timestamp,timestampByString);

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(timestamp,when);

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldLocalDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [localdatetime('2015-12-31T19:32:24')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Timestamp);

        java.sql.Timestamp timestamp = (Timestamp) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(32, cal.get(Calendar.MINUTE));
        assertEquals(24, cal.get(Calendar.SECOND));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayLocalDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [localdatetime('2015-12-31T19:32:24')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof java.sql.Timestamp);

        java.sql.Timestamp timestamp = (Timestamp) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(32, cal.get(Calendar.MINUTE));
        assertEquals(24, cal.get(Calendar.SECOND));

        rs.close();
        statement.close();
    }

    /*
    =============================
            DATE
    =============================
    */

    @Test
    public void executeQueryShouldReturnFieldDate() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: date('2015-12-31') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Date);

        Date date = (java.sql.Date) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnDate() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN date('2015-12-31') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.DATE, rs.getMetaData().getColumnType(1));

        Date date = rs.getDate(1);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        Date dateByString = rs.getDate("event");
        assertEquals(date,dateByString);

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Date);
        assertEquals(date,when);

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldDate() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [date('2015-12-31')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Date);

        java.sql.Date date = (Date) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayDate() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [date('2015-12-31')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof java.sql.Date);

        java.sql.Date date = (Date) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(2015, cal.get(Calendar.YEAR));
        assertEquals(12-1, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));

        rs.close();
        statement.close();
    }

    /*
    =============================
            TIME
    TODO: manage the offset (if possible)
    TODO: manage the (int, Calendar) method
    =============================
    */

    @Test
    public void executeQueryShouldReturnFieldTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: time('125035.556+0100') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Time);

        Time date = (java.sql.Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));
        //assertEquals(1, cal.get(Calendar.ZONE_OFFSET));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN time('125035.556+0100') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.TIME, rs.getMetaData().getColumnType(1));

        Time time = rs.getTime(1);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));
        //assertEquals(1, cal.get(Calendar.ZONE_OFFSET));

        Time timeByString = rs.getTime("event");
        assertEquals(time,timeByString);

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Time);
        assertEquals(time,when);

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [time('125035.556+0100')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time date = (Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [time('125035.556+0100')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time date = (Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));

        rs.close();
        statement.close();
    }
    /*
    =============================
            LOCAL TIME
    =============================
    */

    @Test
    public void executeQueryShouldReturnFieldLocalTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: localtime('125035.556') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Time);

        Time date = (java.sql.Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));
        //assertEquals(1, cal.get(Calendar.ZONE_OFFSET));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnLocalTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localtime('125035.556') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.TIME, rs.getMetaData().getColumnType(1));

        Time time = rs.getTime(1);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));
        //assertEquals(1, cal.get(Calendar.ZONE_OFFSET));

        Time timeByString = rs.getTime("event");
        assertEquals(time,timeByString);

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Time);
        assertEquals(time,when);

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldLocalTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [localtime('125035.556')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time date = (Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayLocalTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [localtime('125035.556')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time date = (Time) when;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());

        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(50, cal.get(Calendar.MINUTE));
        assertEquals(35, cal.get(Calendar.SECOND));
        assertEquals(556, cal.get(Calendar.MILLISECOND));

        rs.close();
        statement.close();
    }

    /*
    =============================
            DURATION
    =============================
    */
    @Test
    public void executeQueryShouldReturnFieldDuration() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof Map);

        Map<String, Object> durationMap = (Map<String, Object>) when;

        assertEquals("P-13M-1DT-3661.001000000S",durationMap.get("duration"));

        assertEquals(-13L,durationMap.get("months"));
        assertEquals(-1L,durationMap.get("days"));
        assertEquals(-3662L,durationMap.get("seconds"));
        assertEquals(999000000,durationMap.get("nanoseconds"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnDuration() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("return duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) AS duration");

        assertTrue(rs.next());

        Object durationObj = rs.getObject(1);
        assertTrue(durationObj instanceof Map);

        Map<String, Object> durationMap = (Map<String, Object>) durationObj;

        assertEquals("P-13M-1DT-3661.001000000S",durationMap.get("duration"));

        assertEquals(-13L,durationMap.get("months"));
        assertEquals(-1L,durationMap.get("days"));
        assertEquals(-3662L,durationMap.get("seconds"));
        assertEquals(999000000,durationMap.get("nanoseconds"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldDuration() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [ duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) ] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof Map);

        Map<String, Object> durationMap = (Map<String, Object>) when;

        assertEquals("P-13M-1DT-3661.001000000S",durationMap.get("duration"));

        assertEquals(-13L,durationMap.get("months"));
        assertEquals(-1L,durationMap.get("days"));
        assertEquals(-3662L,durationMap.get("seconds"));
        assertEquals(999000000,durationMap.get("nanoseconds"));

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayDuration() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [ duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) ] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof Map);

        Map<String, Object> durationMap = (Map<String, Object>) when;

        assertEquals("P-13M-1DT-3661.001000000S",durationMap.get("duration"));

        assertEquals(-13L,durationMap.get("months"));
        assertEquals(-1L,durationMap.get("days"));
        assertEquals(-3662L,durationMap.get("seconds"));
        assertEquals(999000000,durationMap.get("nanoseconds"));


        rs.close();
        statement.close();
    }

}
