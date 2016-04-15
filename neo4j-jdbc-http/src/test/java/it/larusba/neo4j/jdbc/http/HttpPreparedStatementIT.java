package it.larusba.neo4j.jdbc.http;

import it.larusba.neo4j.jdbc.http.test.Neo4jHttpITTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;

import static org.junit.Assert.*;

public class HttpPreparedStatementIT extends Neo4jHttpITTest {

	@BeforeClass
	public static void initialize() throws ClassNotFoundException, SQLException {
		Class.forName("it.larusba.neo4j.jdbc.http.HttpDriver");
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/

	@Test public void executeQueryShouldExecuteAndReturnCorrectData() throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:" + neo4j.httpURI().toString());
		PreparedStatement statement = connection.prepareStatement("MATCH (m:Movie) WHERE m.title= ? RETURN m.title");
		statement.setString(1, "The Matrix");
		ResultSet rs = statement.executeQuery();

		assertTrue(rs.next());
		assertEquals("The Matrix", rs.getString(1));
		assertFalse(rs.next());
		connection.close();
	}
}

