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
 * Created on 04/03/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import it.neo4j.jdbc.bolt.data.ResultSetData;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
	/*           isClosed           */
	/*------------------------------*/

	@Test public void isClosedReturnFalseWhenConnectionOpen() throws SQLException {
		ResultCursor resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		assertFalse(resultSet.isClosed());
	}

	//this method depends on the close() method
	@Test public void isClosedReturnTrueWhenConnectionClosed() throws SQLException {
		ResultCursor resultCursor = ResultSetData.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		ResultSet resultSet = new BoltResultSet(resultCursor);

		resultSet.close();
		assertTrue(resultSet.isClosed());
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/

	@Test public void closeShouldCallTheCloseMethodOfTheCursor() throws SQLException {
		ResultCursor mockedCursor = mock(ResultCursor.class);
		ResultSet resultSet = new BoltResultSet(mockedCursor);

		resultSet.close();

		verify(mockedCursor, times(1)).close();
	}

	@Test public void closeCalledMoreThanOneTimeTheTimesAfterIsNOOP() throws SQLException {
		ResultCursor mockedCursor = mock(ResultCursor.class);
		ResultSet resultSet = new BoltResultSet(mockedCursor);

		resultSet.close();
		resultSet.close();
		resultSet.close();

		verify(mockedCursor, times(1)).close();
	}

	@Test public void closeShouldThrowExceptionIfCursorNull() throws SQLException {
		expectedEx.expect(SQLException.class);
		ResultSet resultSet = new BoltResultSet(null);

		resultSet.close();
	}

}
