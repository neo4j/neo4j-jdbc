package org.neo4j.jdbc.bolt.data;

import java.sql.*;

/**
 * An utility to manage the external database for the Performance Tests
 */
public class PerformanceTestData {

    public static final String NEO_URL = "jdbc:neo4j:bolt://localhost:7687?user=neo4j,password=test";

    /**
     * Get a new connection to the external database
     * @return
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(NEO_URL);
    }

    /**
     * Fill, if needed, the test random data.
     * (:A)-[:X]->(:B)-[:Y]->(:C)
     * @return true if the DB was empty and the load run
     * @throws SQLException
     */
    public static boolean loadABCXYData() throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("MATCH (n) RETURN n LIMIT 1");
        boolean hasDataLoaded = rs.next();
        rs.close();
        if(hasDataLoaded){
            Statement stmtCheck = conn.createStatement();
            ResultSet checkRs = stmtCheck.executeQuery("MATCH (x:PerformanceTestData) RETURN x LIMIT 1");
            boolean hasTestDataLoaded = checkRs.next();
            if(!hasTestDataLoaded){
                throw new IllegalStateException("The database is loaded without test data. Make sure you are using the correct db at "+NEO_URL);
            }
            stmtCheck.close();
        } else{
            for (int i = 0; i < 100; i++) {
                stmt.executeQuery("CREATE (:A {prop:" + (int) (Math.random() * 100) + "})" + (Math.random() * 10 > 5 ?
                        "-[:X]->(:B {prop:'" + (int) (Math.random() * 100) + "'})" :
                        ""));
            }
            stmt.executeQuery("CREATE (:C)");
            stmt.executeQuery("MATCH (b:B), (c:C) MERGE (c)<-[:Y]-(b)");
            stmt.executeQuery("CREATE (x:PerformanceTestData {when: timestamp()})");
            stmt.close();
            conn.close();
        }

        return !hasDataLoaded;
    }
}
