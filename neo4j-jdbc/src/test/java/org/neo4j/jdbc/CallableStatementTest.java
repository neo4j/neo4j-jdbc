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
package org.neo4j.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class CallableStatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		CallableStatement callableStatement = mock(CallableStatement.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(callableStatement.isWrapperFor(CallableStatement.class));
		assertTrue(callableStatement.isWrapperFor(java.sql.Statement.class));
		assertTrue(callableStatement.isWrapperFor(java.sql.CallableStatement.class));
		assertTrue(callableStatement.isWrapperFor(java.sql.PreparedStatement.class));
		assertTrue(callableStatement.isWrapperFor(java.sql.Wrapper.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		CallableStatement callableStatement = mock(CallableStatement.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(callableStatement.isWrapperFor(ResultSet.class));
		assertFalse(callableStatement.isWrapperFor(java.sql.Driver.class));
		assertFalse(callableStatement.isWrapperFor(ResultSet.class));
		assertFalse(callableStatement.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		CallableStatement callableStatement = mock(CallableStatement.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(callableStatement.unwrap(CallableStatement.class));
		assertNotNull(callableStatement.unwrap(java.sql.Statement.class));
		assertNotNull(callableStatement.unwrap(java.sql.CallableStatement.class));
		assertNotNull(callableStatement.unwrap(java.sql.PreparedStatement.class));
		assertNotNull(callableStatement.unwrap(java.sql.Wrapper.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		CallableStatement callableStatement = mock(CallableStatement.class, Mockito.CALLS_REAL_METHODS);

		callableStatement.unwrap(ResultSet.class);
	}
}
