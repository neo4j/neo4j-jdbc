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
 * Created on 23/02/16
 */
package it.neo4j.jdbc.bolt;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.ResultCursor;
import org.neo4j.driver.v1.Session;

import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnectionIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();  // here we're firing up neo4j with bolt enabled

	@Ignore @Test public void commitShouldWorkFine() throws SQLException, ClassNotFoundException {
		// Make sure Neo4j Driver is registered
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");

		// Connect (autoCommit = false)
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		con.setAutoCommit(false);

		// Creating a node with a transaction
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("CREATE (:CommitShouldWorkFine{result:\"ok\"})");
			ResultSet rs = stmt.executeQuery("MATCH (n:CommitShouldWorkFine) RETURN n.result");
			while (rs.next()) {
				// Should fail only if the previous create is auto-committing
				fail();
			}

			con.commit();
			rs = stmt.executeQuery("MATCH (n:CommitShouldWorkFine) RETURN n.result");
			while (rs.next()) {
				// Should find the created node, after the commit
				assertEquals("ok", rs.getString("n.result"));
			}
		}
	}

	@Ignore @Test public void setAutoCommitShouldCommitFromFalseToTrue() throws SQLException, ClassNotFoundException {
		// Make sure Neo4j Driver is registered
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");

		// Connect (autoCommit = false)
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		con.setAutoCommit(false);

		// Creating a node with a transaction
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("CREATE (:SetAutoCommitSwitch{result:\"ok\"})");
			ResultSet rs = stmt.executeQuery("MATCH (n:SetAutoCommitSwitch) RETURN n.result");
			while (rs.next()) {
				// Should fail only if the previous create is auto-committing
				fail();
			}

			con.setAutoCommit(true);
			rs = stmt.executeQuery("MATCH (n:SetAutoCommitSwitch) RETURN n.result");
			while (rs.next()) {
				// Should find the created node, after the setAutoCommit(true);
				assertEquals("ok", rs.getString("n.result"));
			}
		}
	}

	@Ignore @Test public void rollbackShouldWorkFine() throws SQLException, ClassNotFoundException {
		// Make sure Neo4j Driver is registered
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");

		// Connect (autoCommit = false)
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());
		con.setAutoCommit(false);

		// Creating a node with a transaction
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("CREATE (:RollbackShouldWorkFine{result:\"ok\"})");
			ResultSet rs = stmt.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
			while (rs.next()) {
				// Should fail only if the previous create is auto-committing
				fail();
			}

			con.rollback();
			rs = stmt.executeQuery("MATCH (n:RollbackShouldWorkFine) RETURN n.result");
			while (rs.next()) {
				// Should fail, because
				fail();
			}
		}
		assertTrue(true);
	}

	@Ignore @Test public void autoCommitShouldWorkFine() throws SQLException, ClassNotFoundException {
		// Make sure Neo4j Driver is registered
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");

		// Connect (autoCommit = true, by default)
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		// Creating a node
		try (Statement stmt = con.createStatement()) {
			stmt.executeQuery("CREATE (:AutoCommitShouldWorkFine{result:\"ok\"})");
			ResultSet rs = stmt.executeQuery("MATCH (n:AutoCommitShouldWorkFine) RETURN n.result");
			while (rs.next()) {
				// Should find the created node
				assertEquals("ok", rs.getString("n.result"));
			}
		}
	}
}
