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
 * Created on 04/03/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.jdbc.Neo4jResultSet;
import org.neo4j.jdbc.bolt.data.ResultSetData;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.driver.v1.StatementResult;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jResultSetTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*           isClosed           */
	/*------------------------------*/

	@Test public void isClosedReturnFalseWhenConnectionOpen() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		Neo4jResultSet resultSet = new BoltNeo4jResultSet(null, StatementResult);

		assertFalse(resultSet.isClosed());
	}

	//this method depends on the close() method
	@Test public void isClosedReturnTrueWhenConnectionClosed() throws SQLException {
		StatementResult StatementResult = ResultSetData
				.buildResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS);
		Neo4jResultSet resultSet = new BoltNeo4jResultSet(null, StatementResult);

		resultSet.close();
		assertTrue(resultSet.isClosed());
	}

	/*------------------------------*/
	/*             close            */
	/*------------------------------*/

	@Test public void closeShouldThrowExceptionIfIteratorIsNull() throws SQLException {
		expectedEx.expect(SQLException.class);
		Neo4jResultSet resultSet = new BoltNeo4jResultSet(null, null);

		resultSet.close();
	}

}
