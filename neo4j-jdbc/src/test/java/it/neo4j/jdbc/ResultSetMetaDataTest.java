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
 * Created on 09/03/16
 */
package it.neo4j.jdbc;

import org.junit.Test;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ResultSetMetaDataTest {

	/*------------------------------*/
	/*         isSearchable         */
	/*------------------------------*/
	@Test public void isSearchableShouldReturnTrueIfExistingColumnIndex() throws SQLException {
		ResultSetMetaData rsmd = mock(ResultSetMetaData.class, Mockito.CALLS_REAL_METHODS);
		when(rsmd.getColumnCount()).thenReturn(2);
		assertTrue(rsmd.isSearchable(1));
		assertTrue(rsmd.isSearchable(2));
	}

	@Test public void isSearchableShouldAlwaysReturnFalseIfIndexLessThanZero() throws SQLException {
		ResultSetMetaData rsmd = mock(ResultSetMetaData.class, Mockito.CALLS_REAL_METHODS);
		when(rsmd.getColumnCount()).thenReturn(2);
		assertFalse(rsmd.isSearchable(-1));
	}

	@Test public void isSearchableShouldAlwaysReturnFalseIfIndexOutOfRange() throws SQLException {
		ResultSetMetaData rsmd = mock(ResultSetMetaData.class, Mockito.CALLS_REAL_METHODS);
		when(rsmd.getColumnCount()).thenReturn(2);
		assertFalse(rsmd.isSearchable(3));
	}
}
