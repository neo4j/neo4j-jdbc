/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 19/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.Connection;
import it.neo4j.jdbc.bolt.data.StatementData;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltStatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private Session mockSession() {
		return mock(Session.class);
	}

	/*------------------------------*/
	/*          executeQuery        */
	/*------------------------------*/
	@Ignore @Test public void executeQueryShouldReturnCorrectResultSetStructure() throws SQLException {
		Connection connection = new BoltConnection(mockSession());

		Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		ResultSet rs = statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
		assertTrue(rs instanceof BoltResultSet);
		assertEquals(ResultSet.TYPE_FORWARD_ONLY, rs.getType());
		assertEquals(ResultSet.CONCUR_READ_ONLY, rs.getConcurrency());
		assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, rs.getHoldability());
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.createStatement();
		statement.close();
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.prepareStatement(null);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.prepareCall(null);
		statement.executeQuery(StatementData.STATEMENT_MATCH_ALL);
	}

	@Ignore @Test public void executeQueryShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Transaction transaction = mock(Transaction.class);

		given(transaction.run(anyString())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return null;
		});

		Session session = mock(Session.class);
		given(session.beginTransaction()).willReturn(transaction);
		given(session.isOpen()).willReturn(true);

		Statement statement = new BoltStatement(new BoltConnection(session));

		statement.setQueryTimeout(1);
		statement.executeQuery(StatementData.STATEMENT_CREATE);

		fail();
	}


	/*------------------------------*/
	/*         executeUpdate        */
	/*------------------------------*/

	@Ignore @Test public void executeUpdateShouldRun() throws SQLException {
		Connection connection = new BoltConnection(mockSession());

		Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnClosedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.createStatement();
		statement.close();
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnPreparedStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.prepareStatement(null);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnCallableStatement() throws SQLException {
		expectedEx.expect(SQLException.class);

		Connection connection = new BoltConnection(mockSession());
		Statement statement = connection.prepareCall(null);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);
	}

	@Ignore @Test public void executeUpdateShouldThrowExceptionOnTimeoutExceeded() throws SQLException {
		expectedEx.expect(SQLException.class);

		Transaction transaction = mock(Transaction.class);

		given(transaction.run(anyString())).willAnswer(invocation -> {
			Thread.sleep(1500);
			return null;
		});

		Session session = mock(Session.class);
		given(session.beginTransaction()).willReturn(transaction);
		given(session.isOpen()).willReturn(true);

		Statement statement = new BoltStatement(new BoltConnection(session));

		statement.setQueryTimeout(1);
		statement.executeUpdate(StatementData.STATEMENT_CREATE);

		fail();
	}
}
