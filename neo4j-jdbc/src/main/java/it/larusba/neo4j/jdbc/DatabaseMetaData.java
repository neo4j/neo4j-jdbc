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
 * Created on 03/02/16
 */
package it.larusba.neo4j.jdbc;

import it.larusba.neo4j.jdbc.impl.ListResultSet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class DatabaseMetaData implements java.sql.DatabaseMetaData {

	/**
	 * The regex to parse the version driver.
	 * NUMBER + . + NUMBER + .|- + STRING
	 */
	private final static Pattern VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)(\\.|-)?(.*)?$");

	private String driverName;
	private String driverVersion;

	/**
	 * Default constructor.
	 * Permit to load version and driver name from a property file.
	 */
	public DatabaseMetaData() {
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream("./neo4j-jdbc-driver.properties")){
			Properties properties = new Properties();
			properties.load(stream);
			this.driverName = properties.getProperty("driver.name");
			this.driverVersion = properties.getProperty("driver.version");
		} catch (Exception e) {
			this.driverName = "Neo4j JDBC Driver";
			this.driverVersion = "Unknown";
		}
	}

	@Override public boolean allProceduresAreCallable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean allTablesAreSelectable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getURL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getUserName() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean isReadOnly() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean nullsAreSortedHigh() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean nullsAreSortedLow() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean nullsAreSortedAtStart() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean nullsAreSortedAtEnd() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getDatabaseProductName() throws SQLException {
		return "Neo4j";
	}

	@Override public String getDatabaseProductVersion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getDriverName() throws SQLException {
		return this.driverName;
	}

	@Override public String getDriverVersion() throws SQLException {
		return this.driverVersion;
	}

	@Override public int getDriverMajorVersion() {
		return this.getDriverVersionPart(1);
	}

	@Override public int getDriverMinorVersion() {
		return this.getDriverVersionPart(2);
	}

	@Override public boolean usesLocalFiles() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean usesLocalFilePerTable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesUpperCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesLowerCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	@Override public String getSQLKeywords() throws SQLException {
		return "";
	}

	@Override public String getNumericFunctions() throws SQLException {
		return "";
	}

	@Override public String getStringFunctions() throws SQLException {
		return "";
	}

	@Override public String getSystemFunctions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getTimeDateFunctions() throws SQLException {
		return "";
	}

	@Override public String getSearchStringEscape() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getExtraNameCharacters() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsAlterTableWithAddColumn() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsAlterTableWithDropColumn() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsColumnAliasing() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean nullPlusNonNullIsNull() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsConvert() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsConvert(int fromType, int toType) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsExpressionsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOrderByUnrelated() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsGroupBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsGroupByUnrelated() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsGroupByBeyondSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsLikeEscapeClause() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	@Override public boolean supportsMultipleTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsNonNullableColumns() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsMinimumSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsCoreSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsExtendedSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsANSI92IntermediateSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsANSI92FullSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsFullOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsLimitedOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getSchemaTerm() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getProcedureTerm() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getCatalogTerm() throws SQLException {
		return null;
	}

	@Override public boolean isCatalogAtStart() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public String getCatalogSeparator() throws SQLException {
		return "";
	}

	@Override public boolean supportsSchemasInDataManipulation() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInProcedureCalls() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return false;
	}

	@Override public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return false;
	}

	@Override public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsPositionedDelete() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsPositionedUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSelectForUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsStoredProcedures() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSubqueriesInComparisons() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSubqueriesInExists() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSubqueriesInIns() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsCorrelatedSubqueries() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsUnion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsUnionAll() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxBinaryLiteralLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxCharLiteralLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnsInGroupBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnsInIndex() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnsInSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxColumnsInTable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxConnections() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxCursorNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxIndexLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxSchemaNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxProcedureNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxCatalogNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxRowSize() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxStatementLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxStatements() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxTableNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxTablesInSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getMaxUserNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getDefaultTransactionIsolation() throws SQLException {
		return 0;
	}

	@Override public boolean supportsTransactions() throws SQLException {
		return true;
	}

	@Override public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getSchemas() throws SQLException {
		return new ListResultSet(Collections.emptyList(), Collections.emptyList());
	}

	@Override public ResultSet getCatalogs() throws SQLException {
		return new ListResultSet(Collections.emptyList(), Collections.emptyList());
	}

	@Override public ResultSet getTableTypes() throws SQLException {
		List<Object> list = Arrays.asList("TABLE");
		return new ListResultSet(Arrays.asList(list), Arrays.asList("TABLE_TYPE"));
	}

	@Override public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema,
			String foreignTable) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getTypeInfo() throws SQLException {
		return new ListResultSet(Collections.emptyList(), Collections.emptyList());
	}

	@Override public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsResultSetType(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean ownUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean ownDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean ownInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean othersUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean othersDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean othersInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean updatesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean deletesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean insertsAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsBatchUpdates() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public abstract java.sql.Connection getConnection() throws SQLException;

	@Override public boolean supportsSavepoints() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsNamedParameters() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsMultipleOpenResults() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsGetGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getDatabaseMajorVersion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getDatabaseMinorVersion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getJDBCMajorVersion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getJDBCMinorVersion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public int getSQLStateType() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean locatorsUpdateCopy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsStatementPooling() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public RowIdLifetime getRowIdLifetime() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getClientInfoProperties() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
			throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public boolean generatedKeyAlwaysReturned() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	/**
	 * Extract a part of the driver version.
	 *
	 * @param position 1 for the major, 2 for minor and 3 for revision
	 * @return The corresponding driver version part if it's possible, otherwise -1
	 */
	private int getDriverVersionPart(int position) {
		int version = -1;
		try {
			Matcher matcher = VERSION_REGEX.matcher(this.getDriverVersion());
			if(matcher.find()) {
				version = Integer.valueOf(matcher.group(position));
			}
		} catch (SQLException e) {
			// silent exception, but there is the default value
		}
		return version;
	}
}
