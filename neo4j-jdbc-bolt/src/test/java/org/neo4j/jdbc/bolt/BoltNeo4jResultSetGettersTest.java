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
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.jdbc.Neo4jConnection;
import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.Neo4jStatement;
import org.neo4j.jdbc.bolt.data.ResultSetData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetGettersTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*          findColumn          */
	/*------------------------------*/
	@Test public void findColumnShouldReturnCorrectIndex() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		assertEquals(1, resultSet.findColumn("columnA"));
		assertEquals(2, resultSet.findColumn("columnB"));
	}

	@Test public void findColumnShouldReturnCorrectIndexOnDifferentColumns() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_DIFF, ResultSetData.RECORD_LIST_MORE_ELEMENTS_DIFF);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		assertEquals(1, resultSet.findColumn("columnA"));
		assertEquals(2, resultSet.findColumn("columnB"));
		assertEquals(3, resultSet.findColumn("columnC"));
	}

	@Test public void findColumnShouldThrowExceptionOnWrongLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.findColumn("columnZ");
	}

	@Test public void findColumnShouldThrowExceptionOnClosedResultSet() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.findColumn("columnA");
	}

	/*------------------------------*/
	/*           getString          */
	/*------------------------------*/

	@Test public void getStringByLabelShouldReturnString() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString("columnString"));

		resultSet.next();
		assertEquals("value2", resultSet.getString("columnString"));
	}

	@Test public void getStringByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getString("columnZ");
	}

	@Test public void getStringByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getString("columnString");
	}

	@Test public void getStringByIndexShouldReturnString() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString(2));

		resultSet.next();
		assertEquals("value2", resultSet.getString(2));
	}

	@Test public void getStringByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getString(99);
	}

	@Test public void getStringByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getString(0);
	}

	@Test public void getStringByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getString(2);
	}

	@Test public void getStringByLabelShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString("node.property1"));
	}

	@Test public void getStringByIndexShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString(5));
	}

	@Test public void getStringShouldReturnStringOnNode() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("{\"_id\":1, \"_labels\":[\"label1\", \"label2\"], \"property2\":1, \"property1\":\"value1\"}", resultSet.getString("node"));

		resultSet.next();
		assertEquals("{\"_id\":2, \"_labels\":[\"label\"], \"property\":1.6}", resultSet.getString(1));
	}

	@Test public void getObjectShouldReturnStringOnNode() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("{\"_id\":1, \"_labels\":[\"label1\", \"label2\"], \"property2\":1, \"property1\":\"value1\"}", resultSet.getString("node"));

		resultSet.next();
		assertEquals("{\"_id\":2, \"_labels\":[\"label\"], \"property\":1.6}", resultSet.getString(1));
	}

	@Test public void getStringShouldReturnStringOnRelationship() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_RELATIONS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("{\"_id\":1, \"_type\":\"type1\", \"_startId\":1, \"_endId\":2, \"property2\":100, \"property1\":\"value\"}", resultSet.getString("relation"));

		resultSet.next();
		assertEquals("{\"_id\":2, \"_type\":\"type2\", \"_startId\":3, \"_endId\":4, \"property\":2.6}", resultSet.getString(1));
	}

	@Test public void getStringShouldReturnStringOnPath() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_PATHS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_PATHS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(
				"[{\"_id\":1, \"_labels\":[\"label1\"], \"property\":\"value\"}, {\"_id\":3, \"_type\":\"type\", \"_startId\":1, \"_endId\":2, \"relProperty\":\"value3\"}, {\"_id\":2, \"_labels\":[\"label1\"], \"property\":\"value2\"}]",
				resultSet.getString("path"));

		resultSet.next();
		assertEquals(
				"[{\"_id\":4, \"_labels\":[\"label1\"], \"property\":\"value\"}, {\"_id\":7, \"_type\":\"type\", \"_startId\":4, \"_endId\":5, \"relProperty\":\"value4\"}, {\"_id\":5, \"_labels\":[\"label1\"], \"property\":\"value2\"}, {\"_id\":8, \"_type\":\"type\", \"_startId\":6, \"_endId\":5, \"relProperty\":\"value5\"}, {\"_id\":6, \"_labels\":[\"label1\"], \"property\":\"value3\"}]",
				resultSet.getString(1));
	}

	/*------------------------------*/
	/*            getInt            */
	/*------------------------------*/

	@Test public void getIntByLabelShouldReturnInt() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt("columnInt"));

		resultSet.next();
		assertEquals(2, resultSet.getInt("columnInt"));
	}

	@Test public void getIntByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getInt("columnZ");
	}

	@Test public void getIntByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getInt("columnInt");
	}

	@Test public void getIntByIndexShouldReturnInt() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt(1));

		resultSet.next();
		assertEquals(2, resultSet.getInt(1));
	}

	@Test public void getIntByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getInt(99);
	}

	@Test public void getIntByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getInt(0);
	}

	@Test public void getIntByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getInt(1);
	}

	@Test public void getIntByLabelShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt("node.id"));
		assertEquals(1, resultSet.getInt("node.property2"));
	}

	@Test public void getIntByIndexShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt(2));
		assertEquals(1, resultSet.getInt(4));
	}

	@Test public void getIntShouldReturnZeroForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0, resultSet.getInt("columnA"));
		assertEquals(0, resultSet.getInt(2));
	}
	
	/*------------------------------*/
	/*           getFloat           */
	/*------------------------------*/

	@Test public void getFloatByLabelShouldReturnFloat() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0.1F, resultSet.getFloat("columnFloat"), 0);

		resultSet.next();
		assertEquals(0.2F, resultSet.getFloat("columnFloat"), 0);
	}

	@Test public void getFloatByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getFloat("columnZ");
	}

	@Test public void getFloatByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getFloat("columnFloat");
	}

	@Test public void getFloatByIndexShouldReturnFloat() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0.1F, resultSet.getFloat(3), 0);

		resultSet.next();
		assertEquals(0.2F, resultSet.getFloat(3), 0);
	}

	@Test public void getFloatByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getFloat(99);
	}

	@Test public void getFloatByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getFloat(0);
	}

	@Test public void getFloatByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getFloat(3);
	}

	@Test public void getFloatShouldReturnZeroForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0F, resultSet.getFloat("columnA"), 0);
		assertEquals(0F, resultSet.getFloat(2), 0);
	}
	
	/*------------------------------*/
	/*            getShort          */
	/*------------------------------*/

	@Test public void getShortByLabelShouldReturnShort() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getShort("columnShort"));

		resultSet.next();
		assertEquals(2, resultSet.getShort("columnShort"));
	}

	@Test public void getShortByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getShort("columnZ");
	}

	@Test public void getShortByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getShort("columnShort");
	}

	@Test public void getShortByIndexShouldReturnShort() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(1, resultSet.getShort(1));

		resultSet.next();
		assertEquals(2, resultSet.getShort(1));
	}

	@Test public void getShortByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getShort(99);
	}

	@Test public void getShortByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getShort(0);
	}

	@Test public void getShortByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getShort(3);
	}

	@Test public void getShortShouldReturnZeroForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0, resultSet.getShort("columnA"));
		assertEquals(0, resultSet.getShort(2));
	}
	
	/*------------------------------*/
	/*           getDouble          */
	/*------------------------------*/

	@Test public void getDoubleByLabelShouldReturnDouble() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(02.29D, resultSet.getDouble("columnDouble"), 0);

		resultSet.next();
		assertEquals(20.16D, resultSet.getDouble("columnDouble"), 0);
	}

	@Test public void getDoubleByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getDouble("columnZ");
	}

	@Test public void getDoubleByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getDouble("columnDouble");
	}

	@Test public void getDoubleByIndexShouldReturnDouble() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(02.29D, resultSet.getDouble(5), 0);

		resultSet.next();
		assertEquals(20.16D, resultSet.getDouble(5), 0);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);
		resultSet.next();
		resultSet.getDouble(99);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getDouble(0);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getDouble(5);
	}

	@Test public void getDoubleShouldReturnZeroForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0D, resultSet.getDouble("columnA"), 0);
		assertEquals(0D, resultSet.getDouble(2), 0);
	}
	
	/*------------------------------*/
	/*           getObject          */
	/*------------------------------*/

	@Test public void getObjectByLabelShouldReturnObject() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getObject("columnString").toString());
		assertEquals(1L, resultSet.getObject("columnInt"));
		assertNull(resultSet.getObject("columnNull"));

		resultSet.next();
		assertEquals(2L, resultSet.getObject("columnShort"));
		assertEquals(20.16D, (double) resultSet.getObject("columnDouble"), 0);
	}

	@Test public void getObjectByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getObject("not present");
	}

	@Test public void getObjectByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.close();
		resultSet.getObject("not present");
	}

	@Test public void getObjectByIndexShouldReturnObject() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getObject(2).toString());
		assertEquals(1L, resultSet.getObject(1));

		resultSet.next();
		assertEquals(2L, resultSet.getObject(4));
		assertEquals(20.16D, (double) resultSet.getObject(5), 0);
	}

	@Test public void getObjectByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getObject(99);
	}

	@Test public void getObjectByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getObject(0);
	}

	@Test public void getObjectByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.close();
		resultSet.getObject(1);
	}

	@Test public void getObjectShouldReturnCorrectNodeAsMap() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();

		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 1L);
				this.put("_labels", Arrays.asList("label1", "label2"));
				this.put("property1", "value1");
				this.put("property2", 1L);
			}
		}, resultSet.getObject("node"));

		resultSet.next();
		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 2L);
				this.put("_labels", Collections.singletonList("label"));
				this.put("property", 1.6);
			}
		}, resultSet.getObject(1));
	}

	@Test public void getObjectShouldReturnCorrectRelationsAsMap() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_RELATIONS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 1L);
				this.put("_type", "type1");
				this.put("property1", "value");
				this.put("property2", 100L);
				this.put("_startId", 1L);
				this.put("_endId", 2L);
			}
		}, resultSet.getObject("relation"));

		resultSet.next();
		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 2L);
				this.put("_type", "type2");
				this.put("property", 2.6);
				this.put("_startId", 3L);
				this.put("_endId", 4L);
			}
		}, resultSet.getObject(1));
	}

	@Test public void getObjectShouldReturnCorrectPathAsMap() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_PATHS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_PATHS);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		assertTrue(resultSet.next());
		assertEquals(new ArrayList<Object>() {
			{
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 1L);
						this.put("_labels", Collections.singletonList("label1"));
						this.put("property", "value");
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 3L);
						this.put("_type", "type");
						this.put("relProperty", "value3");
						this.put("_startId", 1L);
						this.put("_endId", 2L);
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 2L);
						this.put("_labels", Collections.singletonList("label1"));
						this.put("property", "value2");
					}
				});
			}
		}, resultSet.getObject("path"));

		assertTrue(resultSet.next());
		assertEquals(new ArrayList<Object>() {
			{
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 4L);
						this.put("_labels", Collections.singletonList("label1"));
						this.put("property", "value");
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 7L);
						this.put("_type", "type");
						this.put("relProperty", "value4");
						this.put("_startId", 4L);
						this.put("_endId", 5L);
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 5L);
						this.put("_labels", Collections.singletonList("label1"));
						this.put("property", "value2");
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 8L);
						this.put("_type", "type");
						this.put("relProperty", "value5");
						this.put("_startId", 6L);
						this.put("_endId", 5L);
					}
				});
				this.add(new HashMap<String, Object>() {
					{
						this.put("_id", 6L);
						this.put("_labels", Collections.singletonList("label1"));
						this.put("property", "value3");
					}
				});
			}
		}, resultSet.getObject(1));
	}

	@Test public void getObjectByColumnLabelAndCastToClass() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.next();
		resultSet.next();
		assertNotNull(resultSet.getObject("columnInt", Double.class));
		assertNotNull(resultSet.getObject("columnInt", String.class));
		assertNotNull(resultSet.getObject("columnInt", Long.class));
		assertNotNull(resultSet.getObject("columnInt", Float.class));
		assertNotNull(resultSet.getObject("columnInt", Short.class));
		assertNotNull(resultSet.getObject("columnInt", Object.class));

		assertNotNull(resultSet.getObject("columnMap", Map.class));

		assertNotNull(resultSet.getObject("columnBoolean", Short.class));
		assertNotNull(resultSet.getObject("columnBoolean", Integer.class));
		assertNotNull(resultSet.getObject("columnBoolean", Long.class));
		assertNotNull(resultSet.getObject("columnBoolean", Double.class));
		assertNotNull(resultSet.getObject("columnBoolean", Float.class));

		assertNotNull(resultSet.getObject("columnShort", Integer.class));
		assertNotNull(resultSet.getObject("columnShort", Double.class));
		assertNotNull(resultSet.getObject("columnShort", Float.class));
		assertNotNull(resultSet.getObject("columnShort", String.class));

		assertNotNull(resultSet.getObject("columnString", Double.class));
		assertNotNull(resultSet.getObject("columnString", Float.class));
		resultSet.next();
		assertNotNull(resultSet.getObject("columnString", Integer.class));
		assertNotNull(resultSet.getObject("columnString", Long.class));
		assertNotNull(resultSet.getObject("columnString", Short.class));
	}

	@Test public void getObjectByColumnLabelAndCastToClassShouldThrowExceptionWhenTypeNull() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject("columnA", (Class<?>) null);
	}

	@Test public void getObjectByColumnLabelAndCastToClassShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject("not present", (Class<?>) null);
	}

	@Test public void getObjectByColumnLabelAndCastToClassShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.close();
		resultSet.getObject("not present", (Class<?>) null);
	}

	@Test public void getObjectByColumnIndexAndCastToClass() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		assertNotNull(resultSet.getObject(1, Double.class));
		assertNotNull(resultSet.getObject(1, String.class));
	}

	@Test public void getObjectByColumnIndexAndCastToClassShouldThrowExceptionWhenTypeNull() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject(1, (Class<?>) null);
	}

	@Test public void getObjectByColumnIndexAndCastToClassShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject(0, (Class<?>) null);
	}

	@Test public void getObjectByColumnIndexAndCastToClassShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject(99, Double.class);
	}

	@Test public void getObjectByColumnIndexAndCastToClassShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.close();
		resultSet.getObject(1, (Class<?>) null);
	}

	@Test public void getObjectByColumnLabelAndMapShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.getObject("not present", (Map<String, Class<?>>) null);
	}

	@Test public void getObjectByColumnLabelAndMapShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.close();
		resultSet.getObject("not present", (Map<String, Class<?>>) null);
	}

	@Test public void getObjectByColumnIndexAndMapShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		Map<String, Class<?>> map = new HashMap<>();

		resultSet.next();
		resultSet.getObject(99, map);
	}

	@Test public void getObjectByColumnIndexAndMapShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		Map<String, Class<?>> map = new HashMap<>();

		resultSet.close();
		resultSet.getObject(1, map);
	}

	@Test public void getObjectByColumnLabelAndMap() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false,null, statementResult);

		resultSet.next();
		resultSet.next();
		resultSet.next();

		Map<String, Class<?>> map = new HashMap<>();
		map.put("java.lang.Long", String.class);
		map.put("java.lang.Boolean", Integer.class);
		assertNotNull(resultSet.getObject("columnInt", map));
		assertNotNull(resultSet.getObject("columnBoolean", map));
	}

	/*------------------------------*/
	/*           getBoolean         */
	/*------------------------------*/

	@Test public void getBooleanByLabelShouldReturnBoolean() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertTrue(resultSet.getBoolean("columnBoolean"));
		assertEquals("true", resultSet.getString("columnBoolean"));
		assertTrue((Boolean) resultSet.getObject("columnBoolean"));

		resultSet.next();
		assertFalse(resultSet.getBoolean("columnBoolean"));
		assertEquals("false", resultSet.getString("columnBoolean"));
		assertFalse((Boolean) resultSet.getObject("columnBoolean"));
	}

	@Test public void getBooleanByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getBoolean("columnZ");
	}

	@Test public void getBooleanByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getBoolean("columnBoolean");
	}

	@Test public void getBooleanByIndexShouldReturnBoolean() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertTrue(resultSet.getBoolean(6));

		resultSet.next();
		assertFalse(resultSet.getBoolean(6));
	}

	@Test public void getBooleanByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getBoolean(99);
	}

	@Test public void getBooleanByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getBoolean(0);
	}

	@Test public void getBooleanByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getBoolean(6);
	}

	@Test public void getBooleanShouldReturnFalseForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(false, resultSet.getBoolean("columnA"));
		assertEquals(false, resultSet.getBoolean(2));
	}
	
	/*------------------------------*/
	/*            getLong          */
	/*------------------------------*/

	@Test public void getLongByLabelShouldReturnLong() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(2L, resultSet.getLong("columnLong"));

		resultSet.next();
		assertEquals(6L, resultSet.getLong("columnLong"));
	}

	@Test public void getLongByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong("columnZ");
	}

	@Test public void getLongByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getLong("columnLong");
	}

	@Test public void getLongByIndexShouldReturnLong() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(2L, resultSet.getLong(7));

		resultSet.next();
		assertEquals(6L, resultSet.getLong(7));
	}

	@Test public void getLongByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong(99);
	}

	@Test public void getLongByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong(0);
	}

	@Test public void getLongByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getLong(7);
	}

	@Test public void getLongShouldReturnZeroForNull() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_NULL_ELEMENT, ResultSetData.RECORD_LIST_ONE_NULL_ELEMENT);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals(0, resultSet.getLong("columnA"));
		assertEquals(0, resultSet.getLong(2));
	}
	
	/*------------------------------*/
	/*         getHoldability       */
	/*------------------------------*/

	@Test public void getHoldabilityShouldThrowExceptionOnClosedRS() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getHoldability();
	}

	@Test public void getHoldabilityShouldReturnCorrectHoldability() throws SQLException {
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, null, Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertEquals(Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT, resultSet.getHoldability());
	}

	/*------------------------------*/
	/*            getType           */
	/*------------------------------*/

	@Test public void getTypeShouldThrowExceptionOnClosedRS() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getType();
	}

	@Test public void getTypeShouldReturnCorrectType() throws SQLException {
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, null, Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertEquals(Neo4jResultSet.TYPE_FORWARD_ONLY, resultSet.getType());
	}

	/*------------------------------*/
	/*        getConcurrency        */
	/*------------------------------*/

	@Test public void getConcurrencyShouldThrowExceptionOnClosedRS() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getConcurrency();
	}

	@Test public void getConcurrencyShouldReturnCorrectConcurrency() throws SQLException {
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, null, Neo4jResultSet.TYPE_FORWARD_ONLY, Neo4jResultSet.CONCUR_READ_ONLY, Neo4jResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertEquals(Neo4jResultSet.CONCUR_READ_ONLY, resultSet.getConcurrency());
	}

	/*------------------------------*/
	/*            getArray          */
	/*------------------------------*/
	@Test public void getArrayByLabelShouldReturnArray() throws SQLException {
		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("a", ((String[]) resultSet.getArray("array").getArray())[0]);
		assertEquals("b", ((String[]) resultSet.getArray("array").getArray())[1]);
		assertEquals("c", ((String[]) resultSet.getArray("array").getArray())[2]);

		resultSet.next();
		assertEquals(5L, (long) ((Long[]) resultSet.getArray("array").getArray())[0]);
		assertEquals(10L, (long) ((Long[]) resultSet.getArray("array").getArray())[1]);
		assertEquals(99L, (long) ((Long[]) resultSet.getArray("array").getArray())[2]);

		resultSet.next();
		assertEquals(true, ((Boolean[]) resultSet.getArray("array").getArray())[0]);
		assertEquals(false, ((Boolean[]) resultSet.getArray("array").getArray())[1]);
		assertEquals(false, ((Boolean[]) resultSet.getArray("array").getArray())[2]);

		resultSet.next();
		assertEquals(6.5, ((Double[]) resultSet.getArray("array").getArray())[0], 0);
		assertEquals(4.3, ((Double[]) resultSet.getArray("array").getArray())[1], 0);
		assertEquals(2.1, ((Double[]) resultSet.getArray("array").getArray())[2], 0);
	}

	@Test public void getArrayByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong("columnZ");
	}

	@Test public void getArrayByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getLong("array");
	}

	@Test public void getArrayByIndexShouldReturnArray() throws SQLException {
		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		assertEquals("a", ((String[]) resultSet.getArray(1).getArray())[0]);
		assertEquals("b", ((String[]) resultSet.getArray(1).getArray())[1]);
		assertEquals("c", ((String[]) resultSet.getArray(1).getArray())[2]);

		resultSet.next();
		assertEquals(5L, (long) ((Long[]) resultSet.getArray(1).getArray())[0]);
		assertEquals(10L, (long) ((Long[]) resultSet.getArray(1).getArray())[1]);
		assertEquals(99L, (long) ((Long[]) resultSet.getArray(1).getArray())[2]);

		resultSet.next();
		assertEquals(true, ((Boolean[]) resultSet.getArray(1).getArray())[0]);
		assertEquals(false, ((Boolean[]) resultSet.getArray(1).getArray())[1]);
		assertEquals(false, ((Boolean[]) resultSet.getArray(1).getArray())[2]);

		resultSet.next();
		assertEquals(6.5, ((Double[]) resultSet.getArray(1).getArray())[0], 0);
		assertEquals(4.3, ((Double[]) resultSet.getArray(1).getArray())[1], 0);
		assertEquals(2.1, ((Double[]) resultSet.getArray(1).getArray())[2], 0);
	}

	@Test public void getArrayByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong(99);
	}

	@Test public void getArrayByIndexShouldThrowExceptionNoIndexZero() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.next();
		resultSet.getLong(0);
	}

	@Test public void getArrayByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_WITH_ARRAY, ResultSetData.RECORD_LIST_WITH_ARRAY);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, spyCursor);

		resultSet.close();
		resultSet.getLong(7);
	}

	@Test public void getArrayByLabelShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertArrayEquals(new String[] { "label1", "label2" }, (String[]) resultSet.getArray("node.labels").getArray());
	}

	@Test public void getArrayByIndexShouldReturnCorrectVirtualColumn() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		Neo4jConnection c = Mockito.mock(Neo4jConnection.class);
		Mockito.when(c.getFlattening()).thenReturn(1);
		Neo4jStatement stmt = Mockito.mock(Neo4jStatement.class);
		Mockito.when(stmt.getConnection()).thenReturn(c);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, stmt, statementResult);

		resultSet.next();
		assertArrayEquals(new String[] { "label1", "label2" }, (String[]) resultSet.getArray(3).getArray());
	}

	/*------------------------------*/
	/*            wasNull           */
	/*------------------------------*/
	@Test public void wasNullShouldThrowExceptionOnClosedResultSet() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = BoltNeo4jResultSet.newInstance(false, null, statementResult);

		resultSet.close();
		resultSet.wasNull();
	}

	@Test public void wasNullShouldReturnFalse() throws SQLException {
		ResultSet resultSet = Mockito.mock(BoltNeo4jResultSet.class);
		Whitebox.setInternalState(resultSet, "wasNull", false);
		resultSet.wasNull();
	}

	@Test public void wasNullShouldReturnTrue() throws SQLException {
		ResultSet resultSet = Mockito.mock(BoltNeo4jResultSet.class);
		Whitebox.setInternalState(resultSet, "wasNull", true);
		resultSet.wasNull();
	}
}
