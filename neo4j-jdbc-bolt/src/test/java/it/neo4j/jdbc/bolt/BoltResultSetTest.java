/**
 * Copyright (c) 2004-2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * Created on 11/02/16
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
public class BoltResultSetTest {

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

	/*------------------------------*/
	/*          findColumn          */
	/*------------------------------*/
	@Ignore @Test public void findColumnShouldReturnCorrectIndex() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertEquals(0, resultSet.findColumn("columnA"));
		Assert.assertEquals(1, resultSet.findColumn("columnB"));
	}

	@Ignore @Test public void findColumnShouldThrowExceptionOnWrongLabel() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.findColumn("columnZ");
	}

	@Ignore @Test public void findColumnShouldThrowExceptionOnClosedResultSet() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.findColumn("columnA");
	}

	/*------------------------------*/
	/*           getString          */
	/*------------------------------*/

	@Test public void getStringByLabelShouldReturnString() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getString("columnString"));

		resultSet.next();
		Assert.assertEquals("value2", resultSet.getString("columnString"));
	}

	@Test public void getStringByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getString("columnZ");
	}

	//This test depends on the close method
	@Ignore @Test public void getStringByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getString("columnString");
	}

	@Test public void getStringByIndexShouldReturnString() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals("value1", resultSet.getString(2));

		resultSet.next();
		Assert.assertEquals("value2", resultSet.getString(1));
	}

	@Test public void getStringByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getString(99);
	}

	//This test depends on the close method
	@Ignore @Test public void getStringByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getString(2);
	}

	/*------------------------------*/
	/*            getInt            */
	/*------------------------------*/

	@Ignore @Test public void getIntByLabelShouldReturnInt() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getInt("columnInt"));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getInt("columnInt"));
	}

	@Ignore @Test public void getIntByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getInt("columnZ");
	}

	@Ignore @Test public void getIntByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getInt("columnInt");
	}

	@Ignore @Test public void getIntByIndexShouldReturnInt() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(1, resultSet.getInt(1));

		resultSet.next();
		Assert.assertEquals(2, resultSet.getInt(3));
	}

	@Ignore @Test public void getIntByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getInt(99);
	}

	@Ignore @Test public void getIntByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getInt(1);
	}

	/*------------------------------*/
	/*           getFloat           */
	/*------------------------------*/

	@Ignore @Test public void getFloatByLabelShouldReturnFloat() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(0.1, resultSet.getFloat("columnFloat"), 0);

		resultSet.next();
		Assert.assertEquals(0.2, resultSet.getInt("columnFloat"), 0);
	}

	@Ignore @Test public void getFloatByLabelShouldThrowExceptionNoLabel() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getFloat("columnZ");
	}

	@Ignore @Test public void getFloatByLabelShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getFloat("columnFloat");
	}

	@Ignore @Test public void getFloatByIndexShouldReturnFloat() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		Assert.assertEquals(0.1, resultSet.getFloat(3), 0);

		resultSet.next();
		Assert.assertEquals(0.2, resultSet.getInt(1), 0);
	}

	@Ignore @Test public void getFloatByIndexShouldThrowExceptionNoIndex() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Column not present in ResultSet");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.next();
		resultSet.getFloat(99);
	}

	@Ignore @Test public void getFloatByIndexShouldThrowExceptionClosed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultSet is closed");

		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RECORD_LIST_MORE_ELEMENTS_MIXED, ResultSetData.RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		resultSet.getFloat(3);
	}
}