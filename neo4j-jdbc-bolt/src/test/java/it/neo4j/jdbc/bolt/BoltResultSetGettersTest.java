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
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;
import java.util.HashMap;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
		ResultCursor resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertEquals(0, resultSet.findColumn("columnA"));
		Assert.assertEquals(1, resultSet.findColumn("columnB"));
	}

	@Test public void findColumnShouldThrowExceptionOnWrongLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.findColumn("columnZ");
	}

	@Test public void findColumnShouldThrowExceptionOnClosedResultSet() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.findColumn("columnA");
	}

	/*------------------------------*/
	/*           getString          */
	/*------------------------------*/

	@Test public void getStringByLabelShouldReturnString() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getString("columnString"));

		resultSet.next();
		Assert.assertEquals("value2", resultSet.getString("columnString"));
	}

	@Test public void getStringByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getString("columnZ");
	}

	@Test public void getStringByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getString("columnString");
	}

	@Test public void getStringByIndexShouldReturnString() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getString(2));

		resultSet.next();
		Assert.assertEquals("value2", resultSet.getString(2));
	}

	@Test public void getStringByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getString(99);
	}

	@Test public void getStringByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getString(2);
	}

	/*------------------------------*/
	/*            getInt            */
	/*------------------------------*/

	@Test public void getIntByLabelShouldReturnInt() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getInt("columnInt"));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getInt("columnInt"));
	}

	@Test public void getIntByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getInt("columnZ");
	}

	@Test public void getIntByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getInt("columnInt");
	}

	@Test public void getIntByIndexShouldReturnInt() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getInt(1));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getInt(1));
	}

	@Test public void getIntByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getInt(99);
	}

	@Test public void getIntByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getInt(1);
	}

	/*------------------------------*/
	/*           getFloat           */
	/*------------------------------*/

	@Test public void getFloatByLabelShouldReturnFloat() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(0.1F, resultSet.getFloat("columnFloat"), 0);

		resultSet.next();
		Assert.assertEquals(0.2F, resultSet.getFloat("columnFloat"), 0);
	}

	@Test public void getFloatByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getFloat("columnZ");
	}

	@Test public void getFloatByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getFloat("columnFloat");
	}

	@Test public void getFloatByIndexShouldReturnFloat() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(0.1F, resultSet.getFloat(3), 0);

		resultSet.next();
		Assert.assertEquals(0.2F, resultSet.getFloat(3), 0);
	}

	@Test public void getFloatByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getFloat(99);
	}

	@Test public void getFloatByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getFloat(3);
	}

	/*------------------------------*/
	/*            getShort          */
	/*------------------------------*/

	@Test public void getShortByLabelShouldReturnShort() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getShort("columnShort"));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getShort("columnShort"));
	}

	@Test public void getShortByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getShort("columnZ");
	}

	@Test public void getShortByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getShort("columnShort");
	}

	@Test public void getShortByIndexShouldReturnShort() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getShort(1));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getShort(1));
	}

	@Test public void getShortByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getShort(99);
	}

	@Test public void getShortByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getShort(3);
	}

	/*------------------------------*/
	/*           getDouble          */
	/*------------------------------*/

	@Test public void getDoubleByLabelShouldReturnDouble() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(02.29D, resultSet.getDouble("columnDouble"), 0);

		resultSet.next();
		Assert.assertEquals(20.16D, resultSet.getDouble("columnDouble"), 0);
	}

	@Test public void getDoubleByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getDouble("columnZ");
	}

	@Test public void getDoubleByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getDouble("columnDouble");
	}

	@Test public void getDoubleByIndexShouldReturnDouble() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(02.29D, resultSet.getDouble(5), 0);

		resultSet.next();
		Assert.assertEquals(20.16D, resultSet.getDouble(5), 0);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getDouble(99);
	}

	@Test public void getDoubleByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor spyCursor = spy(
				ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED));
		doNothing().when(spyCursor).close();
		ResultSet resultSet = new BoltResultSet(spyCursor);

		resultSet.close();
		resultSet.getDouble(5);
	}

	/*------------------------------*/
	/*           getObject          */
	/*------------------------------*/
	// ! Still needs tests for paths

	@Ignore @Test public void getObjectByLabelShouldReturnObject() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getObject("columnString").toString());
		Assert.assertEquals("Object", resultSet.getObject("columnString").getClass().getName());
		Assert.assertEquals(1, (int) resultSet.getObject("columnInt"));
		Assert.assertEquals("Object", resultSet.getObject("columnInt").getClass().getName());

		resultSet.next();
		Assert.assertEquals((short) 2, (short) resultSet.getObject("columnShort"));
		Assert.assertEquals("Object", resultSet.getObject("columnShort").getClass().getName());
		Assert.assertEquals(20.16D, (double) resultSet.getObject("columnDouble"), 0);
		Assert.assertEquals("Object", resultSet.getObject("columnDouble").getClass().getName());
	}

	@Ignore @Test public void getObjectByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getObject("not present");
	}

	@Ignore @Test public void getObjectByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getObject("not present");
	}

	@Ignore @Test public void getObjectByIndexShouldReturnObject() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getObject(2).toString());
		Assert.assertEquals("Object", resultSet.getObject(2).getClass().getName());
		Assert.assertEquals(1, (int) resultSet.getObject(1));
		Assert.assertEquals("Object", resultSet.getObject(1).getClass().getName());

		resultSet.next();
		Assert.assertEquals((short) 2, (short) resultSet.getObject(3));
		Assert.assertEquals("Object", resultSet.getObject(3).getClass().getName());
		Assert.assertEquals(20.16D, (double) resultSet.getObject(4), 0);
		Assert.assertEquals("Object", resultSet.getObject(4).getClass().getName());
	}

	@Ignore @Test public void getObjectByIndexShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getObject(99);
	}

	@Ignore @Test public void getObjectByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);

		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getObject(1);
	}

	@Ignore @Test public void getObjectShouldReturnCorrectNodeAsMap() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_NODES, ResultSetData.RECORD_LIST_MORE_ELEMENTS_NODES);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 1);
				this.put("_labels", new String[] { "label1", "label2" });
				this.put("property1", "value1");
				this.put("property2", 1);
			}
		}, resultSet.getObject("node"));

		resultSet.next();
		Assert.assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 2);
				this.put("_labels", new String[] { "label" });
				this.put("property", 1.6f);
			}
		}, resultSet.getObject(1));
	}

	@Ignore @Test public void getObjectShouldReturnCorrectRelationsAsMap() throws SQLException {
		ResultCursor resultCursor = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS, ResultSetData.RECORD_LIST_MORE_ELEMENTS_RELATIONS);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 1);
				this.put("_type", "type1");
				this.put("property1", "value");
				this.put("property2", 100);
			}
		}, resultSet.getObject("relation"));

		resultSet.next();
		Assert.assertEquals(new HashMap<String, Object>() {
			{
				this.put("_id", 2);
				this.put("_type", "type2");
				this.put("property", 2.6f);
			}
		}, resultSet.getObject(1));
	}
}
