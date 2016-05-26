/*
 *
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
 * Created on 24/4/2016
 *
 */
package org.neo4j.jdbc.http;

import org.neo4j.jdbc.http.test.Neo4jHttpIT;
import org.junit.Test;

import java.sql.*;

import static org.junit.Assert.*;

public class HttpConnectionIT extends Neo4jHttpIT {

	@Test
	public void autocommitShouldWork() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setAutoCommit(true);
		writer.createStatement().execute("CREATE (n:TestAutocommitShouldWork {value:\"AZERTYUIOP\"})");

		// Let's check that it's saved
		Connection reader = DriverManager.getConnection(getJDBCUrl());
		ResultSet rs = reader.createStatement().executeQuery("MATCH (n:TestAutocommitShouldWork) RETURN n.value");
		assertTrue(rs.next());
		assertEquals(rs.getString("n.value"), "AZERTYUIOP");
		assertFalse(rs.next());

		writer.close();;
		reader.close();
	}

	@Test
	public void commitShouldWork() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setAutoCommit(false);
		writer.createStatement().execute("CREATE (n:TestCommitShouldWork {value:\"AZERTYUIOP\"})");

		// Let's check that it's not saved for now
		Connection reader = DriverManager.getConnection(getJDBCUrl());
		ResultSet rs = reader.createStatement().executeQuery("MATCH (n:TestCommitShouldWork) RETURN n.value");
		assertFalse(rs.next());

		// let's commit and see the result
		writer.commit();
		rs = reader.createStatement().executeQuery("MATCH (n:TestCommitShouldWork) RETURN n.value");
		assertTrue(rs.next());
		assertEquals(rs.getString("n.value"), "AZERTYUIOP");
		assertFalse(rs.next());

		writer.close();
		reader.close();
	}

	@Test
	public void changeCommitModeOnOpenedTransactionShouldCommit() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setAutoCommit(false);
		writer.createStatement().execute("CREATE (n:TestChangeCommitModeOnOpendTransactionShouldFail {value:\"AZERTYUIOP\"})");
		writer.setAutoCommit(true);

		// Let's check that it's saved
		Connection reader = DriverManager.getConnection(getJDBCUrl());
		ResultSet rs = reader.createStatement().executeQuery("MATCH (n:TestChangeCommitModeOnOpendTransactionShouldFail) RETURN n.value");
		assertTrue(rs.next());
		assertEquals(rs.getString("n.value"), "AZERTYUIOP");
		assertFalse(rs.next());

		writer.close();
		reader.close();
	}

	@Test
	public void rollbackShouldWork() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setAutoCommit(false);
		writer.createStatement().execute("CREATE (n:TestRollbackShouldWork {value:\"AZERTYUIOP\"})");
		writer.rollback();

		// Let's check that it's not saved for now
		Connection reader = DriverManager.getConnection(getJDBCUrl());
		ResultSet rs = reader.createStatement().executeQuery("MATCH (n:TestRollbackShouldWork ) RETURN n.value");
		assertFalse(rs.next());

		writer.close();
		reader.close();
	}

	@Test
	public void rollbackOnAutocommitShouldFail() throws SQLException {
		expectedEx.expect(SQLException.class);

		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setAutoCommit(true);
		writer.createStatement().execute("CREATE (n:TestRollbackOnAutocommitShouldFail {value:\"AZERTYUIOP\"})");
		writer.rollback();

		writer.close();
	}

	@Test
	public void getMetaDataShouldWork() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		DatabaseMetaData meta = writer.getMetaData();
		assertNotNull(meta);

		writer.close();
	}

	@Test
	public void closeOnClosedTransactionShouldWork() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.close();
		writer.close();
	}

	@Test
	public void closeOnOpenTransactionShouldRollback() throws SQLException {
		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.createStatement().execute("CREATE (n:TestCloseOnOpenTransactionSouldRollback {value:\"AZERTYUIOP\"})");
		writer.close();

		// Let's check that it's not saved for now
		Connection reader = DriverManager.getConnection(getJDBCUrl());
		ResultSet rs = reader.createStatement().executeQuery("MATCH (n:TestRollbackShouldWork ) RETURN n.value");
		assertFalse(rs.next());

		reader.close();
	}

	@Test
	public void holdabilityShouldFail() throws SQLException {
		expectedEx.expect(UnsupportedOperationException.class);
		expectedEx.expectMessage("Not implemented yet.");

		// Write something
		Connection writer = DriverManager.getConnection(getJDBCUrl());
		writer.setHoldability(1);
		writer.close();
	}
	
}
