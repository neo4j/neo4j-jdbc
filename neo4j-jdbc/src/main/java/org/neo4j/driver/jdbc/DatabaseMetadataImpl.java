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
import java.util.ArrayList;
import java.util.List;

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

		var getAllExecutableProcedures = this.connection
			.prepareStatement("SHOW PROCEDURE EXECUTABLE YIELD name AS PROCEDURE_NAME");
		getAllExecutableProcedures.closeOnCompletion();

		List<String> executableProcedures = new ArrayList<>();
		try (var allProceduresExecutableResultSet = getAllExecutableProcedures.executeQuery()) {
			while (allProceduresExecutableResultSet.next()) {
				executableProcedures.add(allProceduresExecutableResultSet.getString(1));
			}
		}

		if (executableProcedures.isEmpty()) {
			return false;
		}
		try (var proceduresResultSet = getProcedures(null, null, null)) {
			while (proceduresResultSet.next()) {
				if (!executableProcedures.contains(proceduresResultSet.getString(3))) {
					return false;
				}
			}
		}

		return true;
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
		try (var stmt = this.connection.prepareStatement("SHOW CURRENT USER YIELD user");
				var usernameRs = stmt.executeQuery()) {
			usernameRs.next();
			return usernameRs.getString(1);
		}
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

		try (var getDatabaseProductVersion = this.connection.prepareStatement("""
				call dbms.components() yield name, versions,
				edition unwind versions as version return name, edition, version""");
				var productVersionRs = getDatabaseProductVersion.executeQuery()) {
			if (productVersionRs.next()) { // will only ever have one result.
				return "%s-%s-%s".formatted(productVersionRs.getString(1), productVersionRs.getString(2),
						productVersionRs.getString(3));
			}
		}

		return ProductVersion.getValue();
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		try (var getDatabaseProductVersion = this.connection.prepareStatement("""
				call dbms.components() yield versions
				unwind versions as version return version""");
				var productVersionRs = getDatabaseProductVersion.executeQuery()) {
			if (productVersionRs.next()) { // will only ever have one result.
				return productVersionRs.getString(1);
			}
		}

		throw new SQLException("Cannot retrieve product version");
	}

	@Override
	public String getDriverName() {
		return ProductVersion.getName();
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
		return "\"";
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		// Do we just list all the keywords here?
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

	/**
	 * In order to honour the three reserved columns in the return from getProcedures as
	 * outlined in the docs for jdbc we have used reserved_1 reserved_2 reserved_3 these
	 * should not be used.
	 * @param catalog should always be null as does not apply to Neo4j.
	 * @param schemaPattern should always be null as does not apply to Neo4j.
	 * @param procedureNamePattern a procedure name pattern; must match the procedure name
	 * as it is stored in the database
	 * @return resultset that contains the procedures that you can execute with the
	 * columns: name, description, mode and worksOnSystem.
	 * @throws SQLException if you try and call with catalog or schema
	 */
	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
			throws SQLException {

		if (schemaPattern != null) {
			throw new SQLException("Schema is not applicable to Neo4j please leave null.");
		}

		assertCatalogExists(catalog);

		if (procedureNamePattern == null) {
			var statement = this.connection.createStatement();
			statement.closeOnCompletion();
			return statement.executeQuery("""
					SHOW PROCEDURE YIELD name AS PROCEDURE_NAME, description AS PROCEDURE_DESCRIPTION
					RETURN "" AS PROCEDURE_CAT, "" AS PROCEDURE_SCHEM, PROCEDURE_NAME,
					"" AS reserved_1, "" AS reserved_2, "" AS reserved_3, PROCEDURE_DESCRIPTION
					""");
		}
		else {
			var proceduresFiltered = this.connection.prepareStatement("""
					SHOW PROCEDURE YIELD name AS PROCEDURE_NAME, description AS PROCEDURE_DESCRIPTION
					WHERE name = $1
					RETURN "" AS PROCEDURE_CAT, "" AS PROCEDURE_SCHEM, PROCEDURE_NAME,
					"" AS reserved_1, "" AS reserved_2, "" AS reserved_3, PROCEDURE_DESCRIPTION
					""");

			proceduresFiltered.setString(1, procedureNamePattern);
			proceduresFiltered.closeOnCompletion();
			return proceduresFiltered.executeQuery();
		}
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {

		assertSchemaIsNull(schemaPattern);

		var currentDb = getCurrentDb();
		if (catalog != null) {
			// if you try to get labels of another db we will error.
			if (!catalog.equals(getCurrentDb())) {
				throw new SQLException(String
					.format("Cannot get Tables for another catalog. You are currently connected to %s", currentDb));
			}
		}

		if (tableNamePattern != null) {
			var preparedStatement = this.connection.prepareStatement("""
					call db.labels() YIELD label AS TABLE_NAME WHERE TABLE_NAME=$1 RETURN "%s" as TABLE_CAT,
					"" AS TABLE_SCHEM, TABLE_NAME, "LABEL" as TABLE_TYPE, "" as REMARKS,
					"" AS TYPE_CAT, "" AS TYPE_SCHEM, "" AS TYPE_NAME, "" AS SELF_REFERENCES_COL_NAME,
					"" AS REF_GENERATION""".formatted(currentDb));

			preparedStatement.setString(1, tableNamePattern);
			preparedStatement.closeOnCompletion();

			return preparedStatement.executeQuery();
		}
		else {
			var statement = this.connection.createStatement();
			statement.closeOnCompletion();

			return statement.executeQuery("""
					call db.labels() YIELD label AS TABLE_NAME RETURN "%s" as TABLE_CAT,
					"" AS TABLE_SCHEM, TABLE_NAME, "LABEL" as TABLE_TYPE, "" as REMARKS,
					"" AS TYPE_CAT, "" AS TYPE_SCHEM, "" AS TYPE_NAME, "" AS SELF_REFERENCES_COL_NAME,
					"" AS REF_GENERATION""".formatted(currentDb));
		}
	}

	/***
	 * Neo4j does not support schemas.
	 * @return nothing
	 * @throws SQLException if you call this you will receive an
	 * UnsupportedOperationException
	 */
	@Override
	public ResultSet getSchemas() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Returns all the catalogs for the current neo4j instance. For Neo4j catalogs are the
	 * databases on the current Neo4j instance.
	 * @return all catalogs
	 * @throws SQLException will be thrown if cannot connect to current DB
	 */
	@Override
	public ResultSet getCatalogs() throws SQLException {
		var statement = this.connection.createStatement();
		statement.closeOnCompletion();

		return statement.executeQuery("SHOW DATABASE yield name AS TABLE_CAT");
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
		return Integer.parseInt(getDatabaseProductVersion().split(".")[0]);
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return Integer.parseInt(getDatabaseProductVersion().split(".")[1]);
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

	/***
	 * Neo4j does not support schemas.
	 * @return nothing
	 * @throws SQLException if you call this you will receive an
	 * UnsupportedOperationException
	 */
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

	private String getCurrentDb() throws SQLException {
		try (var getCurrentDb = this.connection.prepareStatement("CALL db.info() YIELD name");
				var rs = getCurrentDb.executeQuery()) {
			if (rs.next()) { // only will have one result
				return rs.getString(1);
			}
		}

		throw new SQLException("Cannot retrieve current DB");
	}

	private void assertSchemaIsNull(String schemaPattern) throws SQLException {
		if (schemaPattern != null) {
			throw new SQLException("Schema is not applicable to Neo4j please leave null.");
		}
	}

	private void assertCatalogExists(String catalog) throws SQLException {
		if (catalog != null) {
			var foundCatalog = false;
			try (var catalogsResultSet = getCatalogs()) {
				while (catalogsResultSet.next()) {
					if (catalog.equals(catalogsResultSet.getString(1))) {
						foundCatalog = true;
						break;
					}
				}
			}

			if (!foundCatalog) {
				throw new SQLException("catalog: %s is not valid for the current database.".formatted(catalog));
			}
		}
	}

}
