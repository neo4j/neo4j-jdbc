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

import org.neo4j.jdbc.http.test.Neo4jHttpIT;
import org.junit.Test;

import java.sql.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/*
 * Created by bsimard on 25/04/16.
 */
public class HttpResultSetIT extends Neo4jHttpIT {

	@Test
	public void paramConversionShouldWork() throws SQLException {
		Connection connection = DriverManager.getConnection(getJDBCUrl());
		Statement statement = connection.createStatement();
		ResultSet rs = statement.executeQuery("CREATE (n:TestParamConvertionShouldWork { string:\"AZERTYUIOP\", bool:true, float:3.14, integer:7, array:[1,2,3,4]}) RETURN n, n.string, n.bool, n.float, n.integer, n.array, n.nop");

		assertTrue(rs.next());

		// Testing string
		assertEquals("AZERTYUIOP", rs.getString("n.string"));
		assertEquals("AZERTYUIOP", rs.getString(2));

		// Testing bool
		assertEquals(true, rs.getBoolean("n.bool"));
		assertEquals(true, rs.getBoolean(3));

		// Testing float / double
		assertEquals("Float conversion failed", Float.valueOf("3.14"), rs.getFloat("n.float"));
		assertEquals("Float conversion failed", Float.valueOf("3.14"), rs.getFloat(4));
		assertEquals("Double conversion failed", Double.valueOf("3.14"), rs.getDouble("n.float"));
		assertEquals("Double conversion failed", Double.valueOf("3.14"), rs.getDouble(4));

		// Testing integer, long
		assertEquals("Integer conversion failed",7, rs.getInt("n.integer"));
		assertEquals("Integer conversion failed", 7, rs.getInt(5));
		assertEquals("Long conversion failed", Long.valueOf("7").longValue(), rs.getLong("n.integer"));
		assertEquals("Long conversion failed", Long.valueOf("7").longValue(), rs.getLong(5));
		assertEquals("Short conversion failed", Short.valueOf("7").shortValue(), rs.getShort("n.integer"));
		assertEquals("Short conversion failed", Short.valueOf("7").shortValue(), rs.getShort(5));

		// Testing array
		//assertEquals(7, rs.getArray("n.array"));
		//assertEquals(7, rs.getInt(6));

		// Testing object

		// Testing null
		assertEquals("Null for string", null, rs.getString("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for boolean", false, rs.getBoolean("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for float", Float.valueOf("0.0").floatValue(), rs.getFloat("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for double", Double.valueOf("0.0").doubleValue(), rs.getDouble("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for long", 0, rs.getLong("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for integer", 0, rs.getInt("n.nop"));
		assertTrue(rs.wasNull());
		assertEquals("Null for short", 0, rs.getShort("n.nop"));
		assertTrue(rs.wasNull());

		assertFalse(rs.next());
		connection.close();
	}
}
