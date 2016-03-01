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

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.ResultCursor;
import org.neo4j.driver.v1.Session;

import java.sql.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Stefan Armbruster
 */
public class SampleIT {

	@Rule public Neo4jBoltRule neo4j = new Neo4jBoltRule();  // here we're firing up neo4j with bolt enabled

	@Test public void shouldSimpleServerTestSucceed() {

		// if we want to have raw access to neo4j instance, e.g. for populating the DB upfront:
		neo4j.getGraphDatabase().execute("create ( )");

		// hitting the DB with a bolt request
		Driver driver = GraphDatabase.driver(neo4j.getBoltUrl());   // defaults to localhost:7687
		Session session = driver.session();
		ResultCursor rs = session.run("match (n) RETURN count(n)");
		session.close();
	}

	@Test public void exampleTestInMemory() throws ClassNotFoundException, SQLException {
		// Make sure Neo4j Driver is registered
		Class.forName("it.neo4j.jdbc.bolt.BoltDriver");

		neo4j.getGraphDatabase().execute("create (:User{name:\"testUser\"})");

		// Connect
		Connection con = DriverManager.getConnection("jdbc:" + neo4j.getBoltUrl());

		// Querying
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n:User) RETURN n.name");
			while (rs.next()) {
				assertEquals("testUser", rs.getString("n.name"));
			}
		}
		con.close();
	}
}
