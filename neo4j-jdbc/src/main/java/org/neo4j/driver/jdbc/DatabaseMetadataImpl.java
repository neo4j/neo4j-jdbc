/*
 * Copyright (c) 2023 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * Internal implementation for providing Neo4j specific database metadata.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class DatabaseMetadataImpl implements DatabaseMetaData {

	private final Connection connection;

	DatabaseMetadataImpl(Connection connection) {
		this.connection = connection;
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getURL() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getUserName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDriverName() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDriverVersion() {
		return ProductVersion.getValue();
	}

	@Override
	public int getDriverMajorVersion() {
		return ProductVersion.getMajorVersion();
	}

	@Override
	public int getDriverMinorVersion() {
		return ProductVersion.getMinorVersion();
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getStringFunctions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxConnections() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxStatements() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
			String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
			String attributeNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getJDBCMajorVersion() {
		return 4;
	}

	@Override
	public int getJDBCMinorVersion() {
		return 3;
	}

	@Override
	public int getSQLStateType() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

}
