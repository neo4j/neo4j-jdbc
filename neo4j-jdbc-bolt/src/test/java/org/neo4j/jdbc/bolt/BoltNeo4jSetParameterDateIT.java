package org.neo4j.jdbc.bolt;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.testcontainers.containers.Neo4jContainer;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.jdbc.bolt.utils.Neo4jContainerUtils.neo4jImageCoordinates;

/**
 * Test for the setParameter for Date, Time and Timestamp
 * @since 3.4
 */
public class BoltNeo4jSetParameterDateIT {

    @ClassRule
    public static final Neo4jContainer<?> neo4j = new Neo4jContainer<>(neo4jImageCoordinates()).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes").withAdminPassword(null);

    static Connection connection;

    @Before
    public void cleanDB() {
        JdbcConnectionTestUtils.clearDatabase(neo4j);
        connection = JdbcConnectionTestUtils.verifyConnection(connection, neo4j);
    }

    @AfterClass
    public static void tearDown(){
        JdbcConnectionTestUtils.closeConnection(connection);
    }

    /*
    =============================
            TIMESTAMP
    =============================
    */

    @Test
    public void shouldSetFieldTimestamp() throws SQLException {
        long epochMilli = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Timestamp now = new Timestamp(epochMilli);

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldTimestamp' }) RETURN e AS event");
        preparedStatement.setTimestamp(1,now);
        preparedStatement.execute();

        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldTimestamp' RETURN e.when as when");
            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found",next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type", whenObj instanceof LocalDateTime);

            LocalDateTime when = (LocalDateTime) whenObj;

            final LocalDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime();
            assertEquals("Wrong data", expected, when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }

    @Test
    public void shouldSetFieldTimestampAndCalendar() throws SQLException {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
        ZonedDateTime zdt = Instant.ofEpochMilli(cal.getTimeInMillis()).atZone(ZoneId.of("America/New_York"));

        Timestamp now = new Timestamp(cal.getTimeInMillis());

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldTimestampAndCalendar' }) RETURN e AS event");
        preparedStatement.setTimestamp(1,now, cal);
        preparedStatement.execute();

        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldTimestampAndCalendar' RETURN e.when as when");

            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found",next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type", whenObj instanceof ZonedDateTime);

            ZonedDateTime when = (ZonedDateTime) whenObj;

            assertEquals("Wrong data",zdt, when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }

    /*
    =============================
            DATE
    =============================
    */

    @Test
    public void shouldSetFieldDate() throws SQLException {

        LocalDateTime ldt = LocalDateTime.now();
        long epochMilli = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Date date = new Date(epochMilli);

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldDate' }) RETURN e AS event");
        preparedStatement.setDate(1,date);
        preparedStatement.execute();

        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldDate' RETURN e.when as when");
            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found",next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type",whenObj instanceof LocalDate);

            LocalDate when = (LocalDate) whenObj;

            assertEquals("Wrong data",ldt.toLocalDate(), when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }

    @Test
    public void shouldSetFieldDateAndCalendar() throws SQLException {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
        ZonedDateTime zdt = Instant.ofEpochMilli(cal.getTimeInMillis()).atZone(ZoneId.of("America/New_York"));
        long epochMilli = zdt.toInstant().toEpochMilli();

        Date date = new Date(epochMilli);

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldDateAndCalendar' }) RETURN e AS event");
        preparedStatement.setDate(1,date, cal);
        preparedStatement.execute();

        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldDateAndCalendar' RETURN e.when as when");
            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found",next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type",whenObj instanceof ZonedDateTime);

            ZonedDateTime when = (ZonedDateTime) whenObj;

            assertEquals("Wrong data",zdt, when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }


    /*
    =============================
            TIME
    =============================
    */


    @Test
    public void shouldSetFieldTime() throws SQLException {

        long epochMilli = Instant.now().toEpochMilli();

        Time time = new Time(epochMilli);

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldTime' }) RETURN e AS event");
        preparedStatement.setTime(1, time);
        preparedStatement.execute();
        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldTime' RETURN e.when as when");

            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found", next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type", whenObj instanceof LocalTime);

            LocalTime when = (LocalTime) whenObj;

            assertEquals("Wrong data", Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalTime(), when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }

    @Test
    public void shouldSetFieldTimeAndCalendar() throws SQLException {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("America/New_York")));
        ZonedDateTime zdt = Instant.ofEpochMilli(cal.getTimeInMillis()).atZone(ZoneId.of("America/New_York"));
        OffsetTime offsetTime = zdt.toOffsetDateTime().toOffsetTime();
        long epochMilli = zdt.toInstant().toEpochMilli();

        Time time = new Time(epochMilli);

        PreparedStatement preparedStatement = connection.prepareStatement("CREATE (e:Event {when: ?, test: 'shouldSetFieldTimeAndCalendar' }) RETURN e AS event");
        preparedStatement.setTime(1,time,cal);
        preparedStatement.execute();

        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            Result result = session.run("MATCH (e:Event) WHERE e.test = 'shouldSetFieldTimeAndCalendar' RETURN e.when as when");

            assertTrue("Node not found",result.hasNext());

            Map<String, Object> next = result.next().asMap();

            assertTrue("Result not found",next.containsKey("when"));

            Object whenObj = next.get("when");

            assertTrue("Wrong type", whenObj instanceof OffsetTime);

            OffsetTime when = (OffsetTime) whenObj;

            assertEquals("Wrong data",offsetTime, when);

            JdbcConnectionTestUtils.closeStatement(preparedStatement);
        }
    }
}
