package org.neo4j.jdbc.bolt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.internal.value.*;
import org.neo4j.driver.v1.types.IsoDuration;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;

import java.sql.*;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the date types
 * @since 3.4
 */
public class BoltNeo4jDateIT {
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

    /*
    =============================
            DATETIME
    =============================
     */

    @Test
    public void executeQueryShouldReturnFieldDatetime() throws SQLException, ParseException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: datetime('2015-06-24T12:50:35.556[America/New_York]') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        //to avoid problems running the test all around the world
        ZonedDateTime newyork = ZonedDateTime.parse("2015-06-24T12:50:35.556-04:00[America/New_York]");
        ZonedDateTime local = newyork.withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String format = local.format(formatter);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Timestamp);

        java.sql.Timestamp timestamp = (Timestamp) when;
        assertEquals(format, timestamp.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN datetime('2015-06-24T12:50:35.556[America/New_York]') AS event");

        assertTrue(rs.next());

        //int columnType = rs.getMetaData().getColumnType(1);
        assertEquals(Types.TIMESTAMP_WITH_TIMEZONE, rs.getMetaData().getColumnType(1));

        Calendar berlin = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

        java.sql.Timestamp timestampByIntCal = rs.getTimestamp(1, berlin);
        assertEquals("2015-06-24 18:50:35.556", timestampByIntCal.toString());

        java.sql.Timestamp timestampByStringCal = rs.getTimestamp("event", berlin);
        assertEquals("2015-06-24 18:50:35.556", timestampByStringCal.toString());

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals("2015-06-24 18:50:35.556", when.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [datetime('2015-06-24T12:50:35.556[America/New_York]')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        //to avoid problems running the test all around the world
        ZonedDateTime newyork = ZonedDateTime.parse("2015-06-24T12:50:35.556-04:00[America/New_York]");
        ZonedDateTime local = newyork.withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String format = local.format(formatter);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(format, when.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayDatetime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [datetime('2015-06-24T12:50:35.556[America/New_York]')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        //to avoid problems running the test all around the world
        ZonedDateTime newyork = ZonedDateTime.parse("2015-06-24T12:50:35.556-04:00[America/New_York]");
        ZonedDateTime local = newyork.withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String format = local.format(formatter);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());
        Object when = eventList.get(0);

        assertTrue(when instanceof java.sql.Timestamp);
        assertEquals(format, when.toString());

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

       Only with getTime(col, Calendar) you can manage the offset,
       otherwise is always a LocalTime
    =============================
    */

    @Test
    public void executeQueryShouldReturnFieldTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: time('21:40:32+01:00') }) RETURN e AS event");

        assertTrue(rs.next());

        Map<String, Object> geo = (Map)rs.getObject(1);

        Object when = geo.get("when");

        assertTrue(when instanceof java.sql.Time);

        Time time = (java.sql.Time) when;

        assertEquals("21:40:32", time.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN time('21:40:32+01:00') AS event");

        assertTrue(rs.next());

        assertEquals(Types.TIME, rs.getMetaData().getColumnType(1));

        Calendar gmt = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        Time time = rs.getTime(1, gmt);
        assertEquals("20:40:32", time.toString());


        Time timeByString = rs.getTime("event", gmt);
        assertEquals("20:40:32", timeByString.toString());

        Object when = rs.getObject(1);
        assertTrue(when instanceof java.sql.Time);
        assertEquals("21:40:32", when.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayFieldTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("CREATE (e:Event {when: [time('21:40:32+01:00')] }) RETURN e AS event");

        assertTrue(rs.next());
        Map<String, Object> geo = (Map)rs.getObject(1);

        Object whenField = geo.get("when");
        assertTrue(whenField instanceof List);
        List whenList = (List) whenField;
        assertEquals(1, whenList.size());

        Object when = whenList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time time = (Time) when;

        assertEquals("21:40:32", time.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeQueryShouldReturnArrayTime() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN [time('21:40:32+01:00')] AS event");

        assertTrue(rs.next());
        Object event = rs.getObject(1);

        assertTrue(event instanceof List);
        List eventList = (List) event;
        assertEquals(1, eventList.size());

        Object when = eventList.get(0);
        assertTrue(when instanceof java.sql.Time);

        java.sql.Time time = (Time) when;

        assertEquals("21:40:32", time.toString());

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

    /*
    ======================================
            GET OBJECT
            NEO4J VALUES
    ======================================
     */

    @Test
    public void executeGetObjectShouldReturnDateValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN date('2015-12-31') AS event");

        rs.next();

        DateValue object = rs.getObject(1, DateValue.class);
        assertEquals("2015-12-31", object.asLocalDate().toString());

        DateValue label = rs.getObject("event", DateValue.class);
        assertEquals("2015-12-31", label.asLocalDate().toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnLocalTimeValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localtime('125035.556') AS event");

        rs.next();

        LocalTimeValue object = rs.getObject(1, LocalTimeValue.class);
        assertEquals("12:50:35.556", object.asLocalTime().toString());

        LocalTimeValue label = rs.getObject("event", LocalTimeValue.class);
        assertEquals("12:50:35.556", label.asLocalTime().toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnTimeValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN time('21:40:32+01:00') AS event");

        rs.next();

        TimeValue object = rs.getObject(1, TimeValue.class);
        assertEquals("21:40:32+01:00", object.asOffsetTime().toString());

        TimeValue label = rs.getObject("event", TimeValue.class);
        assertEquals("21:40:32+01:00", label.asOffsetTime().toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldLocalDateTimeValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localdatetime('2015-12-31T19:32:24') AS event");

        rs.next();

        LocalDateTimeValue object = rs.getObject(1, LocalDateTimeValue.class);
        assertEquals("2015-12-31T19:32:24", object.asLocalDateTime().toString());

        LocalDateTimeValue label = rs.getObject("event", LocalDateTimeValue.class);
        assertEquals("2015-12-31T19:32:24", label.asLocalDateTime().toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldDateTimeValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN datetime('2015-06-24T12:50:35.556[America/New_York]') AS event");

        rs.next();

        DateTimeValue object = rs.getObject(1, DateTimeValue.class);
        assertEquals("2015-06-24T12:50:35.556-04:00[America/New_York]", object.asZonedDateTime().toString());

        DateTimeValue label = rs.getObject("event", DateTimeValue.class);
        assertEquals("2015-06-24T12:50:35.556-04:00[America/New_York]", label.asZonedDateTime().toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldDurationValue() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("return duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) AS duration");

        rs.next();

        DurationValue object = rs.getObject(1, DurationValue.class);
        assertEquals("P-13M-1DT-3661.001000000S", object.asIsoDuration().toString());

        DurationValue label = rs.getObject("duration", DurationValue.class);
        assertEquals("P-13M-1DT-3661.001000000S", label.asIsoDuration().toString());

        rs.close();
        statement.close();
    }

        /*
    ======================================
            GET OBJECT
            JAVA VALUES
    ======================================
     */

    @Test
    public void executeGetObjectShouldReturnDate() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN date('2015-12-31') AS event");

        rs.next();

        LocalDate object = rs.getObject(1, LocalDate.class);
        assertEquals("2015-12-31", object.toString());

        LocalDate label = rs.getObject("event", LocalDate.class);
        assertEquals("2015-12-31", label.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnLocalTime() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localtime('125035.556') AS event");

        rs.next();

        LocalTime object = rs.getObject(1, LocalTime.class);
        assertEquals("12:50:35.556", object.toString());

        LocalTime label = rs.getObject("event", LocalTime.class);
        assertEquals("12:50:35.556", label.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnTime() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN time('21:40:32+01:00') AS event");

        rs.next();

        OffsetTime object = rs.getObject(1, OffsetTime.class);
        assertEquals("21:40:32+01:00", object.toString());

        OffsetTime label = rs.getObject("event", OffsetTime.class);
        assertEquals("21:40:32+01:00", label.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnLocalDateTime() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN localdatetime('2015-12-31T19:32:24') AS event");

        rs.next();

        LocalDateTime object = rs.getObject(1, LocalDateTime.class);
        assertEquals("2015-12-31T19:32:24", object.toString());

        LocalDateTime label = rs.getObject("event", LocalDateTime.class);
        assertEquals("2015-12-31T19:32:24", label.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnDateTime() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("RETURN datetime('2015-06-24T12:50:35.556[America/New_York]') AS event");

        rs.next();

        ZonedDateTime object = rs.getObject(1, ZonedDateTime.class);
        assertEquals("2015-06-24T12:50:35.556-04:00[America/New_York]", object.toString());

        ZonedDateTime label = rs.getObject("event", ZonedDateTime.class);
        assertEquals("2015-06-24T12:50:35.556-04:00[America/New_York]", label.toString());

        rs.close();
        statement.close();
    }

    @Test
    public void executeGetObjectShouldReturnDuration() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("return duration.between(datetime('2015-06-24T12:50:35.556+0100'),datetime('2014-05-23T11:49:34.555+0100')) AS duration");

        rs.next();

        IsoDuration object = rs.getObject(1, IsoDuration.class);
        assertEquals("P-13M-1DT-3661.001000000S", object.toString());

        IsoDuration label = rs.getObject("duration", IsoDuration.class);
        assertEquals("P-13M-1DT-3661.001000000S", label.toString());

        rs.close();
        statement.close();
    }
}
