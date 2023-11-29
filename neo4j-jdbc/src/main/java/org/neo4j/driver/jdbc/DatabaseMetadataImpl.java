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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.neo4j.driver.jdbc.internal.bolt.AccessMode;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.TransactionType;
import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.driver.jdbc.internal.bolt.value.RecordImpl;
import org.neo4j.driver.jdbc.values.Record;
import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.Values;

/**
 * Internal implementation for providing Neo4j specific database metadata.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
final class DatabaseMetadataImpl implements DatabaseMetaData {

	private final BoltConnection boltConnection;

	DatabaseMetadataImpl(BoltConnection boltConnection) {
		this.boltConnection = boltConnection;
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		var query = "SHOW PROCEDURE EXECUTABLE YIELD name AS PROCEDURE_NAME";
		var response = doQueryForPullResponse(query, Map.of());

		List<String> executableProcedures = new ArrayList<>();
		for (Record record : response.records()) {
			executableProcedures.add(record.get(0).asString());
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
		var response = doQueryForPullResponse("SHOW CURRENT USER YIELD user", Map.of());
		return response.records().get(0).get(0).asString();
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
		var response = doQueryForPullResponse("""
				CALL dbms.components() YIELD name, versions, edition
				UNWIND versions AS version RETURN name, edition, version""", Map.of());

		var record = response.records().get(0);
		return "%s-%s-%s".formatted(record.get(0).asString(), record.get(1).asString(), record.get(2).asString());
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		var response = doQueryForPullResponse("""
				CALL dbms.components() YIELD versions
				UNWIND versions AS version RETURN version""", Map.of());

		return response.records().get(0).get(0).asString();
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
	public String getIdentifierQuoteString() {
		return "`";
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
	 * @return a {@link ResultSet} that contains the procedures that you can execute with
	 * the columns: name, description, mode and worksOnSystem.
	 * @throws SQLException if you try and call with catalog or schema
	 */
	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
			throws SQLException {

		assertCatalogIsNull(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		if (procedureNamePattern == null) {
			return doQueryForResultSet("""
					SHOW PROCEDURE YIELD name AS PROCEDURE_NAME, description AS PROCEDURE_DESCRIPTION
					RETURN "" AS PROCEDURE_CAT, "" AS PROCEDURE_SCHEM, PROCEDURE_NAME,
					"" AS reserved_1, "" AS reserved_2, "" AS reserved_3, PROCEDURE_DESCRIPTION
					""", Map.of());
		}
		else {
			return doQueryForResultSet("""
					SHOW PROCEDURE YIELD name AS PROCEDURE_NAME, description AS PROCEDURE_DESCRIPTION
					WHERE name = $name
					RETURN "" AS PROCEDURE_CAT, "" AS PROCEDURE_SCHEM, PROCEDURE_NAME,
					"" AS reserved_1, "" AS reserved_2, "" AS reserved_3, PROCEDURE_DESCRIPTION
					""", Map.of("name", procedureNamePattern));
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

		assertSchemaIsPublicOrNull(schemaPattern);
		assertCatalogIsNull(catalog);

		if (tableNamePattern != null) {
			return doQueryForResultSet("""
					CALL db.labels() YIELD label AS TABLE_NAME WHERE TABLE_NAME=$name RETURN "" as TABLE_CAT,
					"public" AS TABLE_SCHEM, TABLE_NAME, "LABEL" as TABLE_TYPE, "" as REMARKS,
					"" AS TYPE_CAT, "" AS TYPE_SCHEM, "" AS TYPE_NAME, "" AS SELF_REFERENCES_COL_NAME,
					"" AS REF_GENERATION""", Map.of("name", tableNamePattern));
		}
		else {
			return doQueryForResultSet("""
					CALL db.labels() YIELD label AS TABLE_NAME RETURN "" as TABLE_CAT,
					"public" AS TABLE_SCHEM, TABLE_NAME, "LABEL" as TABLE_TYPE, "" as REMARKS,
					"" AS TYPE_CAT, "" AS TYPE_SCHEM, "" AS TYPE_NAME, "" AS SELF_REFERENCES_COL_NAME,
					"" AS REF_GENERATION""", Map.of());
		}
	}

	@Override
	public ResultSet getSchemas() {
		var keys = new ArrayList<String>();
		keys.add("TABLE_SCHEM");
		keys.add("TABLE_CATALOG");
		// return RS with just public in it
		var pull = new PullResponse() {
			@Override
			public List<Record> records() {
				var values = new Value[] { Values.value("public"), Values.value("") };
				return Collections.singletonList(new RecordImpl(keys, values));
			}

			@Override
			public Optional<ResultSummary> resultSummary() {
				return Optional.empty();
			}
		};

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), response, pull, -1, -1, -1);
	}

	/***
	 * Returns an empty Result set as there cannot be Catalogs in neo4j.
	 * @return all catalogs
	 */
	@Override
	public ResultSet getCatalogs() {
		var keys = new ArrayList<String>();
		keys.add("TABLE_CAT");

		var pull = new PullResponse() {
			@Override
			public List<Record> records() {
				return new ArrayList<>();
			}

			@Override
			public Optional<ResultSummary> resultSummary() {
				return Optional.empty();
			}

			@Override
			public boolean hasMore() {
				return false;
			}
		};

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), response, pull, -1, -1, -1);

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
		return Integer.parseInt(getDatabaseProductVersion().split("\\.")[0]);
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return Integer.parseInt(getDatabaseProductVersion().split("\\.")[1]);
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
	 * Neo4j does not support schemas, this method does always throw an
	 * {@link UnsupportedOperationException}.
	 * @return nothing
	 * @throws UnsupportedOperationException in all cases
	 */
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) {
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

	private static RunResponse createRunResponseForStaticKeys(ArrayList<String> keys) {
		return new RunResponse() {
			@Override
			public long queryId() {
				return 0;
			}

			@Override
			public List<String> keys() {
				return keys;
			}
		};
	}

	private void assertSchemaIsPublicOrNull(String schemaPattern) throws SQLException {
		if (schemaPattern != null && !"public".equals(schemaPattern)) {
			throw new SQLException("Schema must be public or null.");
		}
	}

	private static void assertCatalogIsNull(String catalog) throws SQLException {
		if (catalog != null) {
			throw new SQLException("Catalog is not applicable to Neo4j please leave null.");
		}
	}

	private PullResponse doQueryForPullResponse(String query, Map<String, Object> args) throws SQLException {
		var response = doQuery(query, args);
		return response.pullResponse;
	}

	private ResultSet doQueryForResultSet(String query, Map<String, Object> args) throws SQLException {
		var response = doQuery(query, args);

		return new ResultSetImpl(new LocalStatementImpl(), response.runFuture.join(), response.pullResponse, -1, -1,
				-1);
	}

	private QueryAndRunResponse doQuery(String query, Map<String, Object> args) throws SQLException {
		var beginFuture = this.boltConnection
			.beginTransaction(Collections.emptySet(), AccessMode.READ, TransactionType.DEFAULT, false)
			.toCompletableFuture();
		var runFuture = this.boltConnection.run(query, args, false).toCompletableFuture();
		var pullFuture = this.boltConnection.pull(runFuture, -1).toCompletableFuture();
		var joinedFuture = CompletableFuture.allOf(beginFuture, runFuture).thenCompose(ignored -> pullFuture);

		try {
			var pullResponse = joinedFuture.get();
			this.boltConnection.reset(true);
			return new QueryAndRunResponse(pullResponse, runFuture);
		}
		catch (InterruptedException | ExecutionException ex) {
			this.boltConnection.reset(true).toCompletableFuture().join();
			var cause = ex.getCause();
			throw new SQLException("An error occurred when running the query", (cause != null) ? cause : ex);
		}
	}

	private record QueryAndRunResponse(PullResponse pullResponse, CompletableFuture<RunResponse> runFuture) {
	}

}
