package org.neo4j.jdbc.bolt.utils;

import org.neo4j.jdbc.bolt.BoltDriver;
import org.neo4j.jdbc.bolt.Neo4jBoltRule;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

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

    public static Connection getConnection(Neo4jBoltRule neo4j, String parameters) throws SQLException {
        //return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl,user=neo4j,password=neo4j");
        if(!warmedup){
            warmup();
        }
        return DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + "?nossl"+parameters,USERNAME,PASSWORD);
    }

    public static Connection getConnection(Neo4jBoltRule neo4j) throws SQLException {
        return getConnection(neo4j,"");
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
        try {
            if(connection != null &&  !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
