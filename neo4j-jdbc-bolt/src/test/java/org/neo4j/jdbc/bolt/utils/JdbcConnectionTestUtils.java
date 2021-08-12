package org.neo4j.jdbc.bolt.utils;

import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.jdbc.bolt.BoltDriver;
import org.neo4j.jdbc.bolt.data.StatementData;
import org.testcontainers.containers.Neo4jContainer;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Help to build the connection for the IT test
 */
public class JdbcConnectionTestUtils {

    public static final String USERNAME = "user";
    public static final String PASSWORD = "password";
    public static final boolean SSL_ENABLED = false;

    public static boolean warmedup = false;

    private static boolean warmup(){
        // WARM UP
        long t0 = System.currentTimeMillis();
        boolean driverLoaded = false;
        while (!driverLoaded && System.currentTimeMillis() - t0 < 10_000){
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()){
                if(BoltDriver.class.equals(drivers.nextElement().getClass())){
                    driverLoaded = true;
                }
            }
        }

        warmedup = driverLoaded;

        return warmedup;
    }

    public static Connection getConnection(Neo4jContainer<?> neo4j, String parameters) throws SQLException {
        //return DriverManager.getConnection("jdbc:neo4j:" + neo4j.boltURI() + "?nossl,user=neo4j,password=neo4j");
        if(!warmedup){
            warmup();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl"+parameters,USERNAME,PASSWORD);
    }

    public static Properties defaultInfo(){
        Properties info = new Properties();
        info.setProperty("user",USERNAME);
        info.setProperty("password",PASSWORD);
        info.setProperty("nossl","true");
        return info;
    }

    public static Connection getConnection(Neo4jContainer<?> neo4j, Properties info) throws SQLException {
        if(!warmedup){
            warmup();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl(),info);
    }

    public static Connection getConnection(Neo4jContainer<?> neo4j) throws SQLException {
        return getConnection(neo4j, "");
    }

    public static Connection verifyConnection(Connection connection, Neo4jContainer<?> neo4j, String parameters){
        Connection res = connection;

        try {
            if(connection == null || connection.isClosed()){
                res =  JdbcConnectionTestUtils.getConnection(neo4j,parameters);
            }else{
                res.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(),e);
        }

        return res;
    }

    public static Connection verifyConnection(Connection connection, Neo4jContainer<?> neo4j){
        return verifyConnection(connection,neo4j,"");
    }

    public static void closeConnection(Connection connection){
        closeConnection(connection, null, null);
    }

    public static void closeConnection(Connection connection, Statement stmt){
        closeConnection(connection, stmt, null);
    }

    public static void closeStatement(Statement stmt, ResultSet rs){
        closeConnection(null, stmt, rs);
    }

    public static void closeStatement(Statement stmt){
        closeConnection(null, stmt, null);
    }

    public static void closeResultSet(ResultSet rs){
        closeConnection(null, null, rs);
    }

    public static void closeConnection(Connection connection, Statement stmt, ResultSet rs){
        try {
            if(rs != null &&  !rs.isClosed()){
                rs.close();
            }
            if(stmt != null &&  !stmt.isClosed()){
                stmt.close();
            }
            if(connection != null &&  !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void clearDatabase(Neo4jContainer<?> neo4j) {
        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session(SessionConfig.forDatabase("system"))) {
            session.run("DROP DATABASE neo4j").consume();
            session.run("CREATE DATABASE neo4j").consume();
            int counter = 0;
            while (++counter < 5 && session.run("show databases yield name where name = 'neo4j' return count(*) AS count")
                    .single()
                    .get("count")
                    .asLong() <= 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public static void executeTransactionally(Neo4jContainer<?> neo4j, String statement) {
        try (org.neo4j.driver.Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());
             Session session = driver.session()) {
            session.run(statement).consume();
        }
    }
}
