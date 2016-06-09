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
public class StatementTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		Statement statement = mock(Statement.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(statement.isWrapperFor(Statement.class));
		assertTrue(statement.isWrapperFor(java.sql.Statement.class));
		assertTrue(statement.isWrapperFor(java.sql.Wrapper.class));
		assertTrue(statement.isWrapperFor(AutoCloseable.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		Statement statement = mock(Statement.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(statement.isWrapperFor(ParameterMetaData.class));
		assertFalse(statement.isWrapperFor(java.sql.Driver.class));
		assertFalse(statement.isWrapperFor(javax.sql.DataSource.class));
		assertFalse(statement.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		Statement statement = mock(Statement.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(statement.unwrap(Statement.class));
		assertNotNull(statement.unwrap(java.sql.Statement.class));
		assertNotNull(statement.unwrap(java.sql.Wrapper.class));
		assertNotNull(statement.unwrap(AutoCloseable.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		Statement statement = mock(Statement.class, Mockito.CALLS_REAL_METHODS);

		statement.unwrap(ResultSet.class);
	}
}
