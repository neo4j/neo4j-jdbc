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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class DatabaseMetaDataTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(databaseMetaData.isWrapperFor(DatabaseMetaData.class));
		assertTrue(databaseMetaData.isWrapperFor(java.sql.DatabaseMetaData.class));
		assertTrue(databaseMetaData.isWrapperFor(java.sql.Wrapper.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(databaseMetaData.isWrapperFor(ResultSet.class));
		assertFalse(databaseMetaData.isWrapperFor(java.sql.Driver.class));
		assertFalse(databaseMetaData.isWrapperFor(ResultSet.class));
		assertFalse(databaseMetaData.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(databaseMetaData.unwrap(DatabaseMetaData.class));
		assertNotNull(databaseMetaData.unwrap(java.sql.DatabaseMetaData.class));
		assertNotNull(databaseMetaData.unwrap(java.sql.Wrapper.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		databaseMetaData.unwrap(ResultSet.class);
	}

	/*------------------------------*/
	/*     Driver Metadata          */
	/*------------------------------*/

	@Test public void getDriverVersionShouldBeCorrect() throws SQLException {
		DatabaseMetaData databaseMetaData = new DatabaseMetaData(null, false){};
		assertNotNull(databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverMajorVersionShouldBeCorrect() throws SQLException {
		DatabaseMetaData databaseMetaData = new DatabaseMetaData(null, false){};
		assertNotEquals(-1, databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverMinorVersionShouldBeCorrect() throws SQLException {
		DatabaseMetaData databaseMetaData = new DatabaseMetaData(null, false){};
		assertNotEquals(-1, databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverVersionShouldReturnNegativeNumberOnBadVersion() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

		when(databaseMetaData.getDriverVersion()).thenReturn("Unknown");
		assertEquals(-1, databaseMetaData.getDriverMajorVersion());
		assertEquals(-1, databaseMetaData.getDriverMinorVersion());
	}

	@Test public void getDriverVersionShouldBeCorrectOnSomeExampleVersions() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

		when(databaseMetaData.getDriverVersion()).thenReturn("3.0");
		assertEquals(3, databaseMetaData.getDriverMajorVersion());
		assertEquals(0, databaseMetaData.getDriverMinorVersion());

		when(databaseMetaData.getDriverVersion()).thenReturn("3.1.1-SNAPSHOT");
		assertEquals(3, databaseMetaData.getDriverMajorVersion());
		assertEquals(1, databaseMetaData.getDriverMinorVersion());
	}

	@Test public void storesUpperCaseIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesUpperCaseIdentifiers());
	}

	@Test public void storesLowerCaseIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesLowerCaseIdentifiers());
	}

	@Test public void storesMixedCaseIdentifiersShouldBeReturnTrue() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.storesMixedCaseIdentifiers());
	}

	@Test public void storesUpperCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesUpperCaseQuotedIdentifiers());
	}

	@Test public void storesLowerCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesLowerCaseQuotedIdentifiers());
	}

	@Test public void storesMixedCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesMixedCaseQuotedIdentifiers());
	}

	@Test public void supportsMixedCaseIdentifiersShouldBeReturnTrue() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsMixedCaseIdentifiers());
	}

	@Test public void supportsMixedCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsMixedCaseQuotedIdentifiers());
	}
}
