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

import it.neo4j.jdbc.ResultSetMetaData;
import it.neo4j.jdbc.bolt.data.ResultSetData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.driver.internal.InternalResultCursor;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltResultSetMetaDataTest {

	@BeforeClass public static void initialize() {
		ResultSetData.initialize();
	}

	/*------------------------------*/
	/*        getColumnCount        */
	/*------------------------------*/

	@Test public void getColumnsCountShouldReturnCorrectNumberEmpty() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_EMPTY, ResultSetData.RECORD_LIST_EMPTY,
				ResultSetData.RESULT_SUMMARY);
		ResultSetMetaData resultSet = new BoltResultSetMetaData(resultCursor);

		Assert.assertEquals(0, resultSet.getColumnCount());
	}

	@Test public void getColumnsCountShouldReturnCorrectNumberMoreElements() throws SQLException {
		ResultCursor resultCursor = new InternalResultCursor(ResultSetData.KEYS_RECORD_LIST_MORE_ELEMENTS, ResultSetData.RECORD_LIST_MORE_ELEMENTS,
				ResultSetData.RESULT_SUMMARY);
		ResultSetMetaData resultSet = new BoltResultSetMetaData(resultCursor);

		Assert.assertEquals(2, resultSet.getColumnCount());
	}
}
