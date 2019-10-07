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
 * Created on 18/02/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.types.Type;
import org.neo4j.jdbc.bolt.data.ResultSetData;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetMetaDataTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*        getColumnCount        */
	/*------------------------------*/

	@Test public void getColumnsCountShouldReturnCorrectNumberEmpty() throws SQLException {
		StatementResult resultIterator = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultIterator.keys());

		assertEquals(0, resultSet.getColumnCount());
	}

	@Test public void getColumnsCountShouldReturnCorrectNumberMoreElements() throws SQLException {
		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		assertEquals(2, resultSet.getColumnCount());
	}

	/*------------------------------*/
	/*         getColumnName        */
	/*------------------------------*/

	@Test public void getColumnNameShouldReturnCorrectColumnName() throws SQLException {
		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		assertEquals("columnA", resultSet.getColumnName(1));
	}

	@Test public void getColumnNameShouldThrowExceptionWhenEmptyCursor() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY);
		ResultSetMetaData resultSetMetaData = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		resultSetMetaData.getColumnName(1);
	}

	@Test public void getColumnNameShouldThrowExceptionWhenColumnOutOfRange() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		resultSet.getColumnName(99);
	}

	@Test public void getColumnNameShouldThrowExceptionIfCursorNull() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

		resultSet.getColumnName(1);
	}

	/*------------------------------*/
	/*         getColumnLabel        */
	/*------------------------------*/

	@Test public void getColumnLabelShouldReturnCorrectColumnName() throws SQLException {
		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		assertEquals("columnA", resultSet.getColumnLabel(1));
	}

	@Test public void getColumnLabelShouldThrowExceptionWhenEmptyCursor() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		resultSet.getColumnLabel(1);
	}

	@Test public void getColumnLabelShouldThrowExceptionWhenColumnOutOfRange() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY);
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, resultCursor.keys());

		resultSet.getColumnLabel(99);
	}

	@Test public void getColumnLabelShouldThrowExceptionIfCursorNull() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

		resultSet.getColumnLabel(1);
	}

	/*------------------------------*/
	/*         getSchemaName        */
	/*------------------------------*/

	@Test public void getSchemaNameShouldReturnDefaultValue() throws SQLException {
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

		assertEquals("", resultSet.getSchemaName(1));
	}

	/*------------------------------*/
	/*        getCatalogName        */
	/*------------------------------*/

	@Test public void getCatalogNameShouldReturnEmptyString() throws SQLException {
		ResultSetMetaData resultSet = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

		assertEquals("", resultSet.getCatalogName(1));
	}

	/*------------------------------*/
	/*          flattening          */
	/*------------------------------*/

	@Test public void flatteningTestWorking() throws SQLException {
		StatementResult resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSetMetaData rsm = BoltNeo4jResultSetMetaData.newInstance(false, Collections.EMPTY_LIST,
				Arrays.asList(new String[] { "node", "node.id", "node.label", "node.property2", "node.property1" }));

		assertEquals(5, rsm.getColumnCount());
		assertEquals("node", rsm.getColumnLabel(1));
		assertEquals("node.id", rsm.getColumnLabel(2));
		assertEquals("node.label", rsm.getColumnLabel(3));
		assertEquals("node.property2", rsm.getColumnLabel(4));
		assertEquals("node.property1", rsm.getColumnLabel(5));
	}

	@Test public void getColumnClassNameTest() throws SQLException {

		List<Type> types = Arrays.asList(
				InternalTypeSystem.TYPE_SYSTEM.STRING(),
				InternalTypeSystem.TYPE_SYSTEM.INTEGER(),
				InternalTypeSystem.TYPE_SYSTEM.BOOLEAN(),
				InternalTypeSystem.TYPE_SYSTEM.FLOAT(),
				InternalTypeSystem.TYPE_SYSTEM.NODE(),
				InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP(),
				InternalTypeSystem.TYPE_SYSTEM.PATH(),
				InternalTypeSystem.TYPE_SYSTEM.MAP(),
				InternalTypeSystem.TYPE_SYSTEM.ANY(),
				InternalTypeSystem.TYPE_SYSTEM.LIST(),
				InternalTypeSystem.TYPE_SYSTEM.NUMBER(),
				InternalTypeSystem.TYPE_SYSTEM.NULL()
		);
		List<String> cols = Arrays.asList(
				"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
		);
		ResultSetMetaData rsm = BoltNeo4jResultSetMetaData.newInstance(false, types, cols);
		assertEquals("java.lang.String", rsm.getColumnClassName(1));
		assertEquals("java.lang.Long", rsm.getColumnClassName(2));
		assertEquals("java.lang.Boolean", rsm.getColumnClassName(3));
		assertEquals("java.lang.Double", rsm.getColumnClassName(4));
		assertEquals("java.lang.Object", rsm.getColumnClassName(5));
		assertEquals("java.lang.Object", rsm.getColumnClassName(6));
		assertEquals("java.lang.Object", rsm.getColumnClassName(7));
		assertEquals("java.util.Map", rsm.getColumnClassName(8));
		assertEquals("java.lang.Object", rsm.getColumnClassName(9));
		assertEquals("java.sql.Array", rsm.getColumnClassName(10));
		assertEquals("java.lang.Double", rsm.getColumnClassName(11));
		assertEquals(null, rsm.getColumnClassName(12));
	}

	@Test public void getColumnTypeTest() throws SQLException {

		List<Type> types = Arrays.asList(
				InternalTypeSystem.TYPE_SYSTEM.STRING(),
				InternalTypeSystem.TYPE_SYSTEM.INTEGER(),
				InternalTypeSystem.TYPE_SYSTEM.BOOLEAN(),
				InternalTypeSystem.TYPE_SYSTEM.FLOAT(),
				InternalTypeSystem.TYPE_SYSTEM.NODE(),
				InternalTypeSystem.TYPE_SYSTEM.RELATIONSHIP(),
				InternalTypeSystem.TYPE_SYSTEM.PATH(),
				InternalTypeSystem.TYPE_SYSTEM.MAP(),
				InternalTypeSystem.TYPE_SYSTEM.ANY(),
				InternalTypeSystem.TYPE_SYSTEM.LIST(),
				InternalTypeSystem.TYPE_SYSTEM.NUMBER(),
				InternalTypeSystem.TYPE_SYSTEM.NULL()
		);
		List<String> cols = Arrays.asList(
				"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
		);
		ResultSetMetaData rsm = BoltNeo4jResultSetMetaData.newInstance(false, types, cols);
		assertEquals(Types.VARCHAR, rsm.getColumnType(1));
		assertEquals(Types.INTEGER, rsm.getColumnType(2));
		assertEquals(Types.BOOLEAN, rsm.getColumnType(3));
		assertEquals(Types.FLOAT, rsm.getColumnType(4));
		assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(5));
		assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(6));
		assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(7));
		assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(8));
		assertEquals(Types.JAVA_OBJECT, rsm.getColumnType(9));
		assertEquals(Types.ARRAY, rsm.getColumnType(10));
		assertEquals(Types.FLOAT, rsm.getColumnType(11));
		assertEquals(Types.NULL, rsm.getColumnType(12));
	}

	@Test public void getColumnTypeNameTest() throws SQLException {
		StatementResult resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSetMetaData rsm = BoltNeo4jResultSetMetaData.newInstance(false, Arrays.asList(new Type[] { InternalTypeSystem.TYPE_SYSTEM.NODE() }),
				Arrays.asList(new String[] { "node" }));

		assertEquals(1, rsm.getColumnCount());
		assertEquals("node", rsm.getColumnLabel(1));
		System.err.println(rsm.getColumnClassName(1));
		System.err.println(rsm.getColumnName(1));
		System.err.println(rsm.getColumnType(1));
		System.err.println(rsm.getColumnTypeName(1));
	}
}
