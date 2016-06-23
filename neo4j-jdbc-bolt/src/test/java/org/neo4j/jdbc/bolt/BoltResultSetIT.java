/*
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
 * Created on 08/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetIT {

	@ClassRule public static Neo4jBoltRule neo4j = new Neo4jBoltRule();

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@Test public void flatteningNumberWorking() throws SQLException {
		neo4j.getGraphDatabase().execute("CREATE (:User {name:\"name\"})");
		neo4j.getGraphDatabase().execute("CREATE (:user {surname:\"surname\"})");

		Connection conn = DriverManager.getConnection("jdbc:neo4j:" + neo4j.getBoltUrl() + ",flatten=1");
		Statement stmt = conn.createStatement();

		ResultSet rs = stmt.executeQuery("MATCH (u:User) RETURN u;");
		assertEquals(4, rs.getMetaData().getColumnCount());
		assertTrue(rs.next());

		try{
			assertTrue(rs.findColumn("u.name") > 1);
			assertEquals("name", rs.getString("u.name"));
		} catch (Exception e) {
			assertTrue(rs.findColumn("u.surname") > 1);
			assertEquals("surname", rs.getString("u.surname"));
		}
	}
}
