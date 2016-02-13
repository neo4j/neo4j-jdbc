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
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.internal.InternalRecord;
import org.neo4j.driver.internal.InternalResultCursor;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.*;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	static List<Record> RECORD_LIST_EMPTY;
	static List<Record> RECORD_LIST_ONE_ELEMENT;
	static List<Record> RECORD_LIST_MORE_ELEMENTS;

	static List<String> KEYS_RECORD_LIST_EMPTY;
	static List<String> KEYS_RECORD_LIST_ONE_ELEMENT;
	static List<String> KEYS_RECORD_LIST_MORE_ELEMENTS;

	static ResultSummary RESULT_SUMMARY;

	@BeforeClass public static void setupClass() {
		RECORD_LIST_EMPTY = new LinkedList<>();
		RECORD_LIST_ONE_ELEMENT = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS = new LinkedList<>();

		Record recordA = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA1"), new StringValue("valueB1") });

		RECORD_LIST_ONE_ELEMENT.add(recordA);

		Record recordB = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA2"), new StringValue("valueB2") });

		Record recordC = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA3"), new StringValue("valueB3") });

		RECORD_LIST_MORE_ELEMENTS.add(recordA);
		RECORD_LIST_MORE_ELEMENTS.add(recordB);
		RECORD_LIST_MORE_ELEMENTS.add(recordC);

		KEYS_RECORD_LIST_EMPTY = getKeys(RECORD_LIST_EMPTY);
		KEYS_RECORD_LIST_ONE_ELEMENT = getKeys(RECORD_LIST_ONE_ELEMENT);
		KEYS_RECORD_LIST_MORE_ELEMENTS = getKeys(RECORD_LIST_MORE_ELEMENTS);

		RESULT_SUMMARY = new ResultSummary() {

			@Override public Statement statement() {
				return null;
			}

			@Override public UpdateStatistics updateStatistics() {
				return null;
			}

			@Override public StatementType statementType() {
				return null;
			}

			@Override public boolean hasPlan() {
				return false;
			}

			@Override public boolean hasProfile() {
				return false;
			}

			@Override public Plan plan() {
				return null;
			}

			@Override public ProfiledPlan profile() {
				return null;
			}

			@Override public List<Notification> notifications() {
				return null;
			}
		};
	}

	/**
	 * Calculates the total keys from a list of record with potentially different keys
	 *
	 * @param records a list of records to retrieve keys from
	 * @return a list of keys
	 */
	private static List<String> getKeys(List<Record> records) {
		Set<String> keysSet = new HashSet<>();

		for (Record record : records) {
			keysSet.addAll(record.keys());
		}

		List<String> keys = new LinkedList<>();
		keys.addAll(keysSet);

		return keys;
	}

	@Test public void testGetKeys() {
		Assert.assertEquals(2, getKeys(RECORD_LIST_MORE_ELEMENTS).size());
		Assert.assertEquals(2, getKeys(RECORD_LIST_ONE_ELEMENT).size());
		Assert.assertEquals(0, getKeys(RECORD_LIST_EMPTY).size());
	}

	/*------------------------------*/
	/*            next              */
	/*------------------------------*/
	@Ignore @Test public void nextShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_EMPTY, RECORD_LIST_EMPTY, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.next());
	}

	@Ignore @Test public void nextShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
	}

	@Ignore @Test public void nextShouldReturnFalseAfterLast() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertFalse(resultSet.next());
	}

	@Ignore @Test public void nextShouldReturnTrueAndPointNextNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_MORE_ELEMENTS, RECORD_LIST_MORE_ELEMENTS, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());
		Assert.assertEquals("valueA1", resultSet.getString("columnA"));
		Assert.assertEquals("valueB1", resultSet.getString("columnB"));

		Assert.assertTrue(resultSet.next());
		Assert.assertEquals("valueA2", resultSet.getString("testColumnA"));
		Assert.assertEquals("valueB2", resultSet.getString("testColumnB"));
	}

	@Ignore @Test public void nextShouldThrowExceptionEmpty() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("ResultCursor not initialized");

		ResultSet resultSet = new BoltResultSet(null);

		resultSet.next();
	}

	/*------------------------------*/
	/*           previous           */
	/*------------------------------*/
	@Ignore @Test public void previousShouldReturnFalseEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_EMPTY, RECORD_LIST_EMPTY, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.previous());
	}

	@Ignore @Test public void previousShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.previous());

		Assert.assertTrue(resultSet.next());
	}

	@Ignore @Test public void previousShouldReturnFalseBeforeFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertTrue(resultSet.previous());

		Assert.assertFalse(resultSet.previous());
	}

	@Ignore @Test public void previousShouldReturnTrueAndPointPreviousNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_MORE_ELEMENTS, RECORD_LIST_MORE_ELEMENTS, RESULT_SUMMARY);
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
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_EMPTY, RECORD_LIST_EMPTY, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueOnFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueNotOnFirst() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.next());

		Assert.assertTrue(resultSet.first());
	}

	@Ignore @Test public void firstShouldReturnTrueAndPointFirstElement() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
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
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_EMPTY, RECORD_LIST_EMPTY, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertFalse(resultSet.last());
	}

	@Ignore @Test public void lastShouldReturnTrue() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_ONE_ELEMENT, RECORD_LIST_ONE_ELEMENT, RESULT_SUMMARY);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		Assert.assertTrue(resultSet.last());
	}

	@Ignore @Test public void lastShouldReturnTrueAndPointLastNode() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(KEYS_RECORD_LIST_MORE_ELEMENTS, RECORD_LIST_MORE_ELEMENTS, RESULT_SUMMARY);
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