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
 * Created on 25/4/2016
 *
 */

package org.neo4j.jdbc.http;

import static org.junit.Assert.assertEquals;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Map;

import org.junit.Test;
import org.neo4j.jdbc.http.test.Neo4jHttpIT;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class HttpResultSetMetaDataIT extends Neo4jHttpIT {

	@Test
	public void getColumnClassNameShouldSucceed() throws SQLException {
		Connection con = DriverManager.getConnection(getJDBCUrl());

		try (Statement stmt = con.createStatement()) {
			stmt.execute("CREATE (n:User {name:\"test\", surname:\"testAgain\"})");
		}
		
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("MATCH (n:User) return 'a',1,1.0,[1,2,3],{a:1},null,n,n.name");
			
			while (rs.next()) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
				assertEquals(String.class.getName(), rsm.getColumnClassName(1));

				assertEquals(Types.INTEGER, rsm.getColumnType(2));
				assertEquals(Long.class.getName(), rsm.getColumnClassName(2));

				assertEquals(Types.FLOAT, rsm.getColumnType(3));
				assertEquals(Double.class.getName(), rsm.getColumnClassName(3));

				assertEquals(Types.ARRAY, rsm.getColumnType(4));
				assertEquals(Array.class.getName(), rsm.getColumnClassName(4));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(5));

				assertEquals(Types.NULL, rsm.getColumnType(6));
				assertEquals(null, rsm.getColumnClassName(6));

				assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
				assertEquals(Map.class.getName(), rsm.getColumnClassName(7));

				assertEquals(Types.VARCHAR, rsm.getColumnType(8));
				assertEquals(String.class.getName(), rsm.getColumnClassName(8));
			}
		}
		finally {
			try (Statement stmt = con.createStatement()) {
			  stmt.execute("MATCH (n:User), (s:Session) DETACH DELETE n, s");
			}
			
		  con.close();
		}	
	}
}
