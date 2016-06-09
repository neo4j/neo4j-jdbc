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
 * Created on 30/03/16
 */
package org.neo4j.jdbc.impl;

import org.neo4j.jdbc.Array;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ListArrayTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*        getBaseTypeName       */
	/*------------------------------*/
	@Test public void getBaseTypeNameShouldReturnCorrectType() throws SQLException {
		List<Object> list = new ArrayList<>();
		Array array = new ListArray(list, Types.VARCHAR);
		assertEquals("VARCHAR", array.getBaseTypeName());
		array = new ListArray(list, Types.INTEGER);
		assertEquals("INTEGER", array.getBaseTypeName());
		array = new ListArray(list, Types.DOUBLE);
		assertEquals("DOUBLE", array.getBaseTypeName());
		array = new ListArray(list, Types.BOOLEAN);
		assertEquals("BOOLEAN", array.getBaseTypeName());
		array = new ListArray(list, Types.JAVA_OBJECT);
		assertEquals("JAVA_OBJECT", array.getBaseTypeName());
	}

	@Test public void getBaseTypeNameShouldThrowExceptionWhenNoNeo4jTypeProvided() throws SQLException {
		List<Object> list = new ArrayList<>();
		Array array;
		for (int type : Array.TYPES_UNSUPPORTED) {
			try {
				array = new ListArray(list, type);
				array.getBaseTypeName();
				fail();
			} catch (SQLException e) {
				//Ok
			} catch (Exception e) {
				fail();
			}
		}
	}

	/*------------------------------*/
	/*          getBaseType         */
	/*------------------------------*/
	@Test public void getBaseTypeShouldReturnCorrectType() throws SQLException {
		List<Object> list = new ArrayList<>();
		Array array;
		for (int type : Array.TYPES_SUPPORTED) {
			array = new ListArray(list, type);
			assertEquals(type, array.getBaseType());
		}
	}

	@Test public void getBaseTypeShouldThrowExceptionWhenNoNeo4jTypeProvided() throws SQLException {
		List<Object> list = new ArrayList<>();
		Array array;
		for (int type : Array.TYPES_UNSUPPORTED) {
			try {
				array = new ListArray(list, type);
				array.getBaseType();
				fail();
			} catch (SQLException e) {
				//OK
			} catch (Exception e) {
				fail();
			}
		}
	}

	/*------------------------------*/
	/*           getArray           */
	/*------------------------------*/
	@Test public void getArrayShouldReturnCorrectArray() throws SQLException {
		List<String> list = new ArrayList<>();
		Array array = new ListArray(list, Types.VARCHAR);
		assertTrue(array.getArray() instanceof String[]);
		list.add("test");
		array = new ListArray(list, Types.VARCHAR);
		assertEquals("test", ((String[])array.getArray())[0]);
		list.add("test2");
		array = new ListArray(list, Types.VARCHAR);
		assertEquals("test2", ((String[])array.getArray())[1]);
	}

	@Test public void getArrayShouldThrowExceptionWhenElementsAreOfDifferentType() throws SQLException {
		expectedEx.expect(SQLException.class);

		List<Object> list = new ArrayList<>();
		list.add("test");
		list.add(true);
		Array array = new ListArray(list, Types.VARCHAR);
		array.getArray();
		assertTrue(array.getArray() instanceof String[]);
	}
}
