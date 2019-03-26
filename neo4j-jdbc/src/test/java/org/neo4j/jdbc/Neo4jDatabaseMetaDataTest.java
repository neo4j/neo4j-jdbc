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

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Neo4jDatabaseMetaDataTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();


	/*------------------------------*/
	/*         isWrapperFor         */
	/*------------------------------*/

	@Test public void isWrapperForShouldReturnTrue() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertTrue(databaseMetaData.isWrapperFor(Neo4jDatabaseMetaData.class));
		assertTrue(databaseMetaData.isWrapperFor(java.sql.DatabaseMetaData.class));
		assertTrue(databaseMetaData.isWrapperFor(java.sql.Wrapper.class));
	}

	@Test public void isWrapperForShouldReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertFalse(databaseMetaData.isWrapperFor(Neo4jResultSet.class));
		assertFalse(databaseMetaData.isWrapperFor(java.sql.Driver.class));
		assertFalse(databaseMetaData.isWrapperFor(Neo4jResultSet.class));
		assertFalse(databaseMetaData.isWrapperFor(java.sql.ResultSet.class));
	}

	/*------------------------------*/
	/*            unwrap            */
	/*------------------------------*/

	@Test public void unwrapShouldReturnCorrectClass() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		assertNotNull(databaseMetaData.unwrap(Neo4jDatabaseMetaData.class));
		assertNotNull(databaseMetaData.unwrap(java.sql.DatabaseMetaData.class));
		assertNotNull(databaseMetaData.unwrap(java.sql.Wrapper.class));
	}

	@Test public void unwrapShouldThrowException() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, Mockito.CALLS_REAL_METHODS);

		databaseMetaData.unwrap(Neo4jResultSet.class);
	}

	/*------------------------------*/
	/*     Driver Metadata          */
	/*------------------------------*/

	@Test public void getDriverVersionShouldBeCorrect() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = new Neo4jDatabaseMetaData(null){};
		assertNotNull(databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverMajorVersionShouldBeCorrect() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = new Neo4jDatabaseMetaData(null){};
		assertNotEquals(-1, databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverMinorVersionShouldBeCorrect() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = new Neo4jDatabaseMetaData(null){};
		assertNotEquals(-1, databaseMetaData.getDriverVersion());
	}

	@Test public void getDriverVersionShouldReturnNegativeNumberOnBadVersion() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

		when(databaseMetaData.getDriverVersion()).thenReturn("Unknown");
		assertEquals(-1, databaseMetaData.getDriverMajorVersion());
		assertEquals(-1, databaseMetaData.getDriverMinorVersion());
	}

	@Test public void getDriverVersionShouldBeCorrectOnSomeExampleVersions() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));

		when(databaseMetaData.getDriverVersion()).thenReturn("3.0");
		assertEquals(3, databaseMetaData.getDriverMajorVersion());
		assertEquals(0, databaseMetaData.getDriverMinorVersion());

		when(databaseMetaData.getDriverVersion()).thenReturn("3.1.1-SNAPSHOT");
		assertEquals(3, databaseMetaData.getDriverMajorVersion());
		assertEquals(1, databaseMetaData.getDriverMinorVersion());
	}

	@Test public void storesUpperCaseIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesUpperCaseIdentifiers());
	}

	@Test public void storesLowerCaseIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesLowerCaseIdentifiers());
	}

	@Test public void storesMixedCaseIdentifiersShouldBeReturnTrue() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.storesMixedCaseIdentifiers());
	}

	@Test public void storesUpperCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesUpperCaseQuotedIdentifiers());
	}

	@Test public void storesLowerCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.storesLowerCaseQuotedIdentifiers());
	}

	@Test public void storesMixedCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.storesMixedCaseQuotedIdentifiers());
	}

	@Test public void supportsMixedCaseIdentifiersShouldBeReturnTrue() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsMixedCaseIdentifiers());
	}

	@Test public void supportsMixedCaseQuotedIdentifiersShouldBeReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsMixedCaseQuotedIdentifiers());
	}

	@Test public void supportsResultSetType_TYPE_FORWARD_ONLY_true() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
	}

	@Test public void supportsResultSetType_FETCH_REVERSE_false() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsResultSetType(ResultSet.FETCH_REVERSE));
	}

	@Test public void supportsSavepointsReturnFalse() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSavepoints());
	}

	@Test public void shouldReturnEmptySchemas() throws SQLException {
		//https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getSchemas()
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		ResultSet schemas = databaseMetaData.getSchemas();
		assertNotNull(schemas);
		assertFalse(schemas.next());
		assertNotNull(schemas.getMetaData());
		assertEquals(2,schemas.getMetaData().getColumnCount());
		assertEquals("TABLE_SCHEM",schemas.getMetaData().getColumnName(1));
		assertEquals("TABLE_CATALOG",schemas.getMetaData().getColumnName(2));
	}

	@Test public void shouldReturnEmptyCatalog() throws SQLException {
		//https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getCatalogs()
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		ResultSet schemas = databaseMetaData.getCatalogs();
		assertNotNull(schemas);
		assertFalse(schemas.next());
		assertNotNull(schemas.getMetaData());
		assertEquals(1,schemas.getMetaData().getColumnCount());
		assertEquals("TABLE_CAT",schemas.getMetaData().getColumnName(1));
	}

	@Test
	public void usesLocalFiles() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.usesLocalFiles());
	}

	@Test
	public void usesLocalFilePerTable() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.usesLocalFilePerTable());
	}

	@Test
	public void getSystemFunctions() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertEquals("", databaseMetaData.getSystemFunctions());
	}

	@Test
	public void supportsAlterTableWithAddColumn() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsAlterTableWithAddColumn());
	}

	@Test
	public void supportsAlterTableWithDropColumn() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsAlterTableWithDropColumn());
	}

	@Test
	public void supportsColumnAliasing() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsColumnAliasing());
	}

	@Test
	public void nullPlusNonNullIsNull() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.nullPlusNonNullIsNull());
	}

	@Test
	public void supportsTableCorrelationNames() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsTableCorrelationNames());
	}

	@Test
	public void supportsDifferentTableCorrelationNames() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsDifferentTableCorrelationNames());
	}

	@Test
	public void supportsExpressionsInOrderBy() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsExpressionsInOrderBy());
	}

	@Test
	public void supportsOrderByUnrelated() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOrderByUnrelated());
	}

	@Test
	public void supportsGroupBy() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsGroupBy());
	}

	@Test
	public void supportsGroupByUnrelated() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsGroupByUnrelated());
	}

	@Test
	public void supportsGroupByBeyondSelect() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsGroupByBeyondSelect());
	}

	@Test
	public void supportsLikeEscapeClause() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsLikeEscapeClause());
	}

	@Test
	public void supportsMultipleTransactions() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsMultipleTransactions());
	}

	@Test
	public void supportsNonNullableColumns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsNonNullableColumns());
	}

	@Test
	public void supportsMinimumSQLGrammar() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsMinimumSQLGrammar());
	}

	@Test
	public void supportsCoreSQLGrammar() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsCoreSQLGrammar());
	}

	@Test
	public void supportsExtendedSQLGrammar() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsExtendedSQLGrammar());
	}

	@Test
	public void supportsANSI92EntryLevelSQL() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsANSI92EntryLevelSQL());
	}

	@Test
	public void supportsANSI92IntermediateSQL() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsANSI92IntermediateSQL());
	}

	@Test
	public void supportsANSI92FullSQL() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsANSI92FullSQL());
	}

	@Test
	public void supportsIntegrityEnhancementFacility() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsIntegrityEnhancementFacility());
	}

	@Test
	public void supportsOuterJoins() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOuterJoins());
	}

	@Test
	public void supportsFullOuterJoins() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsFullOuterJoins());
	}

	@Test
	public void supportsLimitedOuterJoins() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsLimitedOuterJoins());
	}

	@Test
	public void getProcedureTerm() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertEquals("procedure", databaseMetaData.getProcedureTerm());
	}

	@Test
	public void supportsPositionedDelete() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsPositionedDelete());
	}

	@Test
	public void supportsStoredProcedures() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsStoredProcedures());
	}

	@Test
	public void supportsPositionedUpdate() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsPositionedUpdate());
	}

	@Test
	public void supportsSubqueriesInComparisons() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSubqueriesInComparisons());
	}

	@Test
	public void supportsSubqueriesInExists() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSubqueriesInExists());
	}

	@Test
	public void supportsSubqueriesInIns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSubqueriesInIns());
	}

	@Test
	public void supportsSubqueriesInQuantifieds() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSubqueriesInQuantifieds());
	}

	@Test
	public void supportsCorrelatedSubqueries() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsCorrelatedSubqueries());
	}

	@Test
	public void supportsUnion() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsUnion());
	}

	@Test
	public void supportsUnionAll() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsUnionAll());
	}

	@Test
	public void supportsOpenCursorsAcrossCommit() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOpenCursorsAcrossCommit());
	}

	@Test
	public void supportsOpenCursorsAcrossRollback() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOpenCursorsAcrossRollback());
	}

	@Test
	public void supportsOpenStatementsAcrossCommit() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOpenStatementsAcrossCommit());
	}

	@Test
	public void supportsOpenStatementsAcrossRollback() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsOpenStatementsAcrossRollback());
	}

	@Test
	public void doesMaxRowSizeIncludeBlobs() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.doesMaxRowSizeIncludeBlobs());
	}

	@Test
	public void supportsTransactionIsolationLevel() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsTransactionIsolationLevel(0));
	}

	@Test
	public void supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsDataDefinitionAndDataManipulationTransactions());
	}

	@Test
	public void supportsDataManipulationTransactionsOnly() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsDataManipulationTransactionsOnly());
	}

	@Test
	public void dataDefinitionCausesTransactionCommit() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.dataDefinitionCausesTransactionCommit());
	}

	@Test
	public void dataDefinitionIgnoredInTransactions() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.dataDefinitionIgnoredInTransactions());
	}

	@Test
	public void supportsResultSetConcurrency() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsResultSetConcurrency(0, 0));
	}

	@Test
	public void ownUpdatesAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.ownUpdatesAreVisible(0));
	}

	@Test
	public void ownDeletesAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.ownDeletesAreVisible(0));
	}

	@Test
	public void ownInsertsAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.ownInsertsAreVisible(0));
	}

	@Test
	public void othersUpdatesAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.othersUpdatesAreVisible(0));
	}

	@Test
	public void othersDeletesAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.othersDeletesAreVisible(0));
	}

	@Test
	public void othersInsertsAreVisible() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.othersInsertsAreVisible(0));
	}

	@Test
	public void updatesAreDetected() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.updatesAreDetected(0));
	}

	@Test
	public void deletesAreDetected() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.deletesAreDetected(0));
	}

	@Test
	public void insertsAreDetected() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.insertsAreDetected(0));
	}

	@Test
	public void supportsBatchUpdates() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsBatchUpdates());
	}

	@Test
	public void supportsSavepoints() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsSavepoints());
	}

	@Test
	public void supportsNamedParameters() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsNamedParameters());
	}

	@Test
	public void supportsMultipleOpenResults() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsMultipleOpenResults());
	}

	@Test
	public void supportsGetGeneratedKeys() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsGetGeneratedKeys());
	}

	@Test
	public void supportsResultSetHoldability() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsResultSetHoldability(0));
	}

	@Test
	public void locatorsUpdateCopy() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.locatorsUpdateCopy());
	}

	@Test
	public void supportsStatementPooling() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.supportsStatementPooling());
	}

	@Test
	public void supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertTrue(databaseMetaData.supportsStoredFunctionsUsingCallSyntax());
	}

	@Test
	public void autoCommitFailureClosesAllResultSets() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.autoCommitFailureClosesAllResultSets());
	}

	@Test
	public void generatedKeyAlwaysReturned() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.generatedKeyAlwaysReturned());
	}

	@Test
	public void getProcedureColumns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getProcedureColumns("", "", "", "").next());
	}

	@Test
	public void getColumnPrivileges() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getColumnPrivileges("", "", "", "").next());
	}

	@Test
	public void getTablePrivileges() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getTablePrivileges("", "", "").next());
	}

	@Test
	public void getBestRowIdentifier() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getBestRowIdentifier("", "", "", 0, false).next());
	}

	@Test
	public void getVersionColumns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getVersionColumns("", "", "").next());
	}

	@Test
	public void getPrimaryKeys() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getPrimaryKeys("", "", "").next());
	}

	@Test
	public void getImportedKeys() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getImportedKeys("", "", "").next());
	}

	@Test
	public void getExportedKeys() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getExportedKeys("", "", "").next());
	}

	@Test
	public void getCrossReference() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getCrossReference("", "", "", "", "", "").next());
	}

	@Test
	public void getIndexInfo() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getIndexInfo("", "", "", false,false).next());
	}

	@Test
	public void getUDTs() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getUDTs("", "", "", new int[0]).next());
	}

	@Test
	public void getSuperTypes() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getSuperTypes("", "", "").next());
	}

	@Test
	public void getSuperTables() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getSuperTables("", "", "").next());
	}

	@Test
	public void getAttributes() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getAttributes("", "", "", "").next());
	}

	@Test
	public void getSchemas() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getSchemas().next());
	}

	@Test
	public void getFunctionColumns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getFunctionColumns("", "", "", "").next());
	}

	@Test
	public void getPseudoColumns() throws SQLException {
		Neo4jDatabaseMetaData databaseMetaData = mock(Neo4jDatabaseMetaData.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
		assertFalse(databaseMetaData.getPseudoColumns("", "", "", "").next());
	}
}
