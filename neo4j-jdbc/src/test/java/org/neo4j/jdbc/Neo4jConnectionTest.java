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
 * Created on 09/03/16
 */
package org.neo4j.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.neo4j.jdbc.impl.Neo4jConnectionImpl;

import java.sql.Array;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Neo4jConnectionTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		Neo4jConnection connection = mock(Neo4jConnectionImpl.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(connection.isWrapperFor(Neo4jConnection.class));
		assertTrue(connection.isWrapperFor(java.sql.Connection.class));
		assertTrue(connection.isWrapperFor(java.sql.Wrapper.class));
		assertTrue(connection.isWrapperFor(java.lang.AutoCloseable.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		Neo4jConnection connection = mock(Neo4jConnectionImpl.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(connection.isWrapperFor(Neo4jStatement.class));
		assertFalse(connection.isWrapperFor(java.sql.Driver.class));
		assertFalse(connection.isWrapperFor(Neo4jResultSet.class));
		assertFalse(connection.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		Neo4jConnection connection = mock(Neo4jConnectionImpl.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(connection.unwrap(Neo4jConnection.class));
		assertNotNull(connection.unwrap(java.sql.Connection.class));
		assertNotNull(connection.unwrap(java.sql.Wrapper.class));
		assertNotNull(connection.unwrap(java.lang.AutoCloseable.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jConnection connection = mock(Neo4jConnectionImpl.class, Mockito.CALLS_REAL_METHODS);

		connection.unwrap(Neo4jStatement.class);
	}

	@Test public void createArrayOfShouldThrowException() throws SQLException {
		Neo4jConnection connection = mock(Neo4jConnectionImpl.class, Mockito.CALLS_REAL_METHODS);

		Object[] booleans = {true, false, false, true};
		Array booleansArr = connection.createArrayOf("BOOLEAN", booleans);
		assertArrayEquals(booleans, (Object[]) booleansArr.getArray());

		Object[] strings = {"foo", "false", "bar", "1"};
		Array stringsArr = connection.createArrayOf("VARCHAR", strings);
		assertArrayEquals(strings, (Object[]) stringsArr.getArray());
		assertArrayEquals(booleans, (Object[]) booleansArr.getArray());

		Object[] integers = {1L, 100L, Long.MAX_VALUE, Long.MIN_VALUE};
		Array integersArr = connection.createArrayOf("INTEGER", integers);
		assertArrayEquals(integers, (Object[]) integersArr.getArray());

		Object[] doubles = {1D, 100D, Double.MAX_VALUE, Double.MIN_VALUE};
		Array doublesArr = connection.createArrayOf("DOUBLE", doubles);
		assertArrayEquals(doubles, (Object[]) doublesArr.getArray());

		Map<String, Object> first = new HashMap<>();
		first.put("first", 1L);
		Map<String, Object> second = new HashMap<>();
		second.put("second", 2L);
		Object[] maps = {first, second};
		Array mapsArr = connection.createArrayOf("JAVA_OBJECT", maps);
		assertArrayEquals(maps, (Object[]) mapsArr.getArray());
	}
}
