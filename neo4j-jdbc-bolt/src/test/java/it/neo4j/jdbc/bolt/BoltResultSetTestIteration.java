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
import org.neo4j.driver.internal.InternalResultCursor;
import org.neo4j.driver.v1.*;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetTestIteration {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*            next              */
	/*------------------------------*/
	@Test public void nextShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.next());
	}

	@Test public void nextShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
	}

	@Test public void nextShouldReturnTrueMoreElements() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());
	}

	@Test public void nextShouldReturnFalseAfterLast() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertFalse(resultSet.next());
	}

	@Test public void nextShouldReturnFalseAfterLastMoreElements() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());

		Assert.assertFalse(resultSet.next());
	}

	// Dependency with ResultSet.getString method
	@Test public void nextShouldReturnTrueAndPointNextNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertEquals("valueA1", resultSet.getString("columnA"));
		Assert.assertEquals("valueB1", resultSet.getString("columnB"));

		Assert.assertTrue(resultSet.next());
		Assert.assertEquals("valueA2", resultSet.getString("columnA"));
		Assert.assertEquals("valueB2", resultSet.getString("columnB"));
	}

	@Test public void nextShouldThrowExceptionEmpty() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultCursor not initialized");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.next();
	}

	/*------------------------------*/
	/*           previous           */
	/*------------------------------*/
	@Ignore @Test public void previousShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.previous());
	}

	@Ignore @Test public void previousShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS,ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());

		Assert.assertTrue(resultSet.previous());
	}

	@Ignore @Test public void previousShouldReturnFalseBeforeFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertTrue(resultSet.next());

		Assert.assertTrue(resultSet.previous());

		Assert.assertFalse(resultSet.previous());
	}

	@Ignore @Test public void previousShouldReturnTrueAndPointPreviousNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.last());

		Assert.assertTrue(resultSet.previous());
		Assert.assertEquals("valueA2", resultSet.getString("columnA"));
		Assert.assertEquals("valueB2", resultSet.getString("columnB"));

		Assert.assertTrue(resultSet.previous());
		Assert.assertEquals("valueA1", resultSet.getString("columnA"));
		Assert.assertEquals("valueB1", resultSet.getString("columnB"));
	}

	@Ignore @Test public void previousShouldThrowExceptionEmpty() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultCursor not initialized");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.previous();
	}

	/*------------------------------*/
	/*             first            */
	/*------------------------------*/
	@Ignore @Test public void firstShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueOnFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueNotOnFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertTrue(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueAndPointFirstElement() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.first());
		Assert.assertEquals("valueA1", resultSet.getString("columnA"));
		Assert.assertEquals("valueB1", resultSet.getString("columnB"));
	}

	@Ignore @Test public void firstShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet already closed");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.close();
		resultSet.first();
	}

	@Ignore @Test public void firstShouldThrowExceptionForwardOnly() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		expectedEx.expectMessage("Called method first on forward-only ResultSet");

		ResultSet resultSet = new BoltResultSet(null) {
			@Override public int getType() {
				return ResultSet.TYPE_FORWARD_ONLY;
			}
		};

		resultSet.first();
	}

	/*------------------------------*/
	/*             last             */
	/*------------------------------*/
	@Ignore @Test public void lastShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY,ResultSetData.RECORD_LIST_EMPTY, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.last());
	}

	@Ignore @Test public void lastShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_ONE_ELEMENT, ResultSetData.RECORD_LIST_ONE_ELEMENT, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.last());
	}

	@Ignore @Test public void lastShouldReturnTrueAndPointLastNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.last());
		Assert.assertEquals("valueA3", resultSet.getString("columnA"));
		Assert.assertEquals("valueB3", resultSet.getString("columnB"));
	}

	@Ignore @Test public void lastShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet already closed");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.close();
		resultSet.last();
	}

	@Ignore @Test public void lastShouldThrowExceptionForwardOnly() throws SQLException {
		expectedEx.expect(SQLFeatureNotSupportedException.class);
		expectedEx.expectMessage("Called method last on forward-only ResultSet");

		ResultSet resultSet = new BoltResultSet(null) {
			@Override public int getType() {
				return ResultSet.TYPE_FORWARD_ONLY;
			}
		};

		resultSet.last();
	}
}