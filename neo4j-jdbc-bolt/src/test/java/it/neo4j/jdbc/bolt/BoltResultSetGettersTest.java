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
 * Created on 18/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import it.neo4j.jdbc.bolt.data.ResultSetData;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.StatementResult;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetGettersTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*          findColumn          */
	/*------------------------------*/
	@Test public void findColumnShouldReturnCorrectIndex() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		assertEquals(1, resultSet.findColumn("columnA"));
		assertEquals(2, resultSet.findColumn("columnB"));
	}

	@Test public void findColumnShouldReturnCorrectIndexOnDifferentColumns() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_DIFF, ResultSetData.RECORD_LIST_MORE_ELEMENTS_DIFF);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		assertEquals(1, resultSet.findColumn("columnA"));
		assertEquals(2, resultSet.findColumn("columnB"));
		assertEquals(3, resultSet.findColumn("columnC"));
	}

	@Test public void findColumnShouldThrowExceptionOnWrongLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.findColumn("columnZ");
	}

	@Test public void findColumnShouldThrowExceptionOnClosedResultSet() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.findColumn("columnA");
	}

	/*------------------------------*/
	/*           getString          */
	/*------------------------------*/

	@Test public void getStringByLabelShouldReturnString() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString("columnString"));

		resultSet.next();
		assertEquals("value2", resultSet.getString("columnString"));
	}

	@Test public void getStringByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getString("columnZ");
	}

	@Test public void getStringByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getString("columnString");
	}

	@Test public void getStringByIndexShouldReturnString() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getString(2));

		resultSet.next();
		assertEquals("value2", resultSet.getString(2));
	}

	@Test public void getStringByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getString(99);
	}

	@Test public void getStringByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getString(2);
	}

	/*------------------------------*/
	/*            getInt            */
	/*------------------------------*/

	@Test public void getIntByLabelShouldReturnInt() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt("columnInt"));

		resultSet.next();
		assertEquals(2, resultSet.getInt("columnInt"));
	}

	@Test public void getIntByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getInt("columnZ");
	}

	@Test public void getIntByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getInt("columnInt");
	}

	@Test public void getIntByIndexShouldReturnInt() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(1, resultSet.getInt(1));

		resultSet.next();
		assertEquals(2, resultSet.getInt(1));
	}

	@Test public void getIntByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getInt(99);
	}

	@Test public void getIntByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getInt(1);
	}

	/*------------------------------*/
	/*           getFloat           */
	/*------------------------------*/

	@Test public void getFloatByLabelShouldReturnFloat() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(0.1F, resultSet.getFloat("columnFloat"), 0);

		resultSet.next();
		assertEquals(0.2F, resultSet.getFloat("columnFloat"), 0);
	}

	@Test public void getFloatByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getFloat("columnZ");
	}

	@Test public void getFloatByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getFloat("columnFloat");
	}

	@Test public void getFloatByIndexShouldReturnFloat() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(0.1F, resultSet.getFloat(3), 0);

		resultSet.next();
		assertEquals(0.2F, resultSet.getFloat(3), 0);
	}

	@Test public void getFloatByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getFloat(99);
	}

	@Test public void getFloatByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getFloat(3);
	}

	/*------------------------------*/
	/*            getShort          */
	/*------------------------------*/

	@Test public void getShortByLabelShouldReturnShort() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(1, resultSet.getShort("columnShort"));

		resultSet.next();
		assertEquals(2, resultSet.getShort("columnShort"));
	}

	@Test public void getShortByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getShort("columnZ");
	}

	@Test public void getShortByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getShort("columnShort");
	}

	@Test public void getShortByIndexShouldReturnShort() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(1, resultSet.getShort(1));

		resultSet.next();
		assertEquals(2, resultSet.getShort(1));
	}

	@Test public void getShortByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getShort(99);
	}

	@Test public void getShortByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getShort(3);
	}

	/*------------------------------*/
	/*           getDouble          */
	/*------------------------------*/

	@Test public void getDoubleByLabelShouldReturnDouble() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(02.29D, resultSet.getDouble("columnDouble"), 0);

		resultSet.next();
		assertEquals(20.16D, resultSet.getDouble("columnDouble"), 0);
	}

	@Test public void getDoubleByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getDouble("columnZ");
	}

	@Test public void getDoubleByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getDouble("columnDouble");
	}

	@Test public void getDoubleByIndexShouldReturnDouble() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(02.29D, resultSet.getDouble(5), 0);

		resultSet.next();
		assertEquals(20.16D, resultSet.getDouble(5), 0);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getDouble(99);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		//doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getDouble(5);
	}

	/*------------------------------*/
	/*           getObject          */
	/*------------------------------*/
	// ! Still needs tests for paths

	@Test public void getObjectByLabelShouldReturnObject() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getObject("columnString").toString());
		assertEquals(1L, resultSet.getObject("columnInt"));

		resultSet.next();
		assertEquals(2L, resultSet.getObject("columnShort"));
		assertEquals(20.16D, (double) resultSet.getObject("columnDouble"), 0);
	}

	@Test public void getObjectByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getObject("not present");
	}

	@Test public void getObjectByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.close();
		resultSet.getObject("not present");
	}

	@Test public void getObjectByIndexShouldReturnObject() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals("value1", resultSet.getObject(2).toString());
		assertEquals(1L, resultSet.getObject(1));

		resultSet.next();
		assertEquals(2L, resultSet.getObject(4));
		assertEquals(20.16D, (double) resultSet.getObject(5), 0);
	}

	@Test public void getObjectByIndexShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getObject(99);
	}

	@Test public void getObjectByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.close();
		resultSet.getObject(1);
	}

	@Test public void getObjectShouldReturnCorrectNodeAsMap() throws SQLException {
		StatementResult statementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = new BoltResultSet(statementResult);

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
				this.put("property", (double) 1.6F);
			}
		}, resultSet.getObject(1));
	}

	@Test public void getObjectShouldReturnCorrectRelationsAsMap() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_RELATIONS);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 1L);
				this.put("_type", "type1");
				this.put("property1", "value");
				this.put("property2", 100L);
			}
		}, resultSet.getObject("relation"));

		resultSet.next();
		assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 2L);
				this.put("_type", "type2");
				this.put("property", (double) 2.6F);
			}
		}, resultSet.getObject(1));
	}

	/*------------------------------*/
	/*           getBoolean         */
	/*------------------------------*/

	@Test public void getBooleanByLabelShouldReturnBoolean() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertTrue(resultSet.getBoolean("columnBoolean"));

		resultSet.next();
		assertFalse(resultSet.getBoolean("columnBoolean"));
	}

	@Test public void getBooleanByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getBoolean("columnZ");
	}

	@Test public void getBooleanByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getBoolean("columnBoolean");
	}

	@Test public void getBooleanByIndexShouldReturnBoolean() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertTrue(resultSet.getBoolean(6));

		resultSet.next();
		assertFalse(resultSet.getBoolean(6));
	}

	@Test public void getBooleanByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getBoolean(99);
	}

	@Test public void getBooleanByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getBoolean(6);
	}

	/*------------------------------*/
	/*            getFloat          */
	/*------------------------------*/

	@Test public void getLongByLabelShouldReturnLong() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(2L, resultSet.getLong("columnLong"));

		resultSet.next();
		assertEquals(6L,resultSet.getLong("columnLong"));
	}

	@Test public void getLongByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getLong("columnZ");
	}

	@Test public void getLongByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getLong("columnLong");
	}

	@Test public void getLongByIndexShouldReturnLong() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		assertEquals(2L, resultSet.getLong(7));

		resultSet.next();
		assertEquals(6L, resultSet.getLong(7));
	}

	@Test public void getLongByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(StatementResult);

		resultSet.next();
		resultSet.getLong(99);
	}

	@Test public void getLongByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		StatementResult spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getLong(7);
	}
}
