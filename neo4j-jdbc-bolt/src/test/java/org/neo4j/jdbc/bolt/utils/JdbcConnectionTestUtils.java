package org.neo4j.jdbc.bolt.utils;

import org.neo4j.jdbc.bolt.BoltDriver;
import org.neo4j.jdbc.bolt.Neo4jBoltRule;
import org.neo4j.jdbc.bolt.data.StatementData;

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

    private static boolean warmedUp = false;

    private static void warmUp(){
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
        warmedUp = driverLoaded;
    }

    public static Connection getConnection(Neo4jBoltRule neo4j, String parameters) throws SQLException {
        if (!warmedUp) {
            warmUp();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl"+parameters, USERNAME, PASSWORD);
    }

    public static Connection getConnectionFromUrlOnly(Neo4jBoltRule neo4j, String parameters) throws SQLException {
        if (!warmedUp) {
            warmUp();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl"+parameters);
    }

    public static Properties defaultInfo(){
        Properties info = new Properties();
        info.setProperty("user",USERNAME);
        info.setProperty("password",PASSWORD);
        info.setProperty("nossl","true");
        return info;
    }

    public static Connection getConnection(Neo4jBoltRule neo4j, Properties info) throws SQLException {
        if (!warmedUp) {
            warmUp();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl(),info);
    }

    public static Connection getConnection(Neo4jBoltRule neo4j) throws SQLException {
        return getConnection(neo4j, "");
    }

    public static Connection verifyConnection(Connection connection, Neo4jBoltRule neo4j, String parameters){
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

    public static Connection verifyConnection(Connection connection, Neo4jBoltRule neo4j){
        return verifyConnection(connection,neo4j,"");
    }

    public static void closeConnection(Connection connection){
        closeConnection(connection, null, null);
    }

    public static void closeConnection(Connection connection, Statement stmt){
        closeConnection(connection, stmt, null);
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

    public static void clearDatabase(Neo4jBoltRule neo4j){
        neo4j.getGraphDatabase().execute(StatementData.STATEMENT_CLEAR_DB);
    }
}
