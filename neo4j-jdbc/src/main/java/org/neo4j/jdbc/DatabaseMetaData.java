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
package org.neo4j.jdbc;

import org.neo4j.jdbc.impl.ListResultSet;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.*;
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

	/**
	 * Name of the driver.
	 */
	private String driverName;

	/**
	 * Version of the driver.
	 */
	private String driverVersion;

	/**
	 * Database version.
	 */
	protected String databaseVersion = "Unknown";

	/**
	 * The JDBC connection.
	 */
	private Connection connection;

	/**
	 * Do we are in debug mode ?
	 */
	protected boolean debug = false;

	/**
	 * Default constructor.
	 * Permit to load version and driver name from a property file.
	 */
	public DatabaseMetaData(Connection connection, boolean debug) {
		this.connection = connection;
		this.debug = debug;

		// Compute driver version, name, ...
		try {
			InputStream stream = DatabaseMetaData.class.getResourceAsStream("/neo4j-jdbc-driver.properties");
			Properties properties = new Properties();
			properties.load(stream);
			this.driverName = properties.getProperty("driver.name");
			this.driverVersion = properties.getProperty("driver.version");
			stream.close();
		} catch (Exception e) {
			this.driverName = "Neo4j JDBC Driver";
			this.driverVersion = "Unknown";
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extract a part of a Version
	 *
	 * @param version  The string representation of a version
	 * @param position 1 for the major, 2 for minor and 3 for revision
	 * @return The corresponding driver version part if it's possible, otherwise -1
	 */
	protected int extractVersionPart(String version, int position) {
		int result = -1;
		try {
			Matcher matcher = VERSION_REGEX.matcher(this.getDriverVersion());
			if (matcher.find()) {
				result = Integer.valueOf(matcher.group(position));
			}
		} catch (SQLException e) {
			// silent exception, but there is the default value
		}
		return result;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	public java.sql.Connection getConnection() throws SQLException {
		return this.connection;
	}

	public String getDriverName() throws SQLException {
		return this.driverName;
	}

	public String getDriverVersion() throws SQLException {
		return this.driverVersion;
	}

	public int getDriverMajorVersion() {
		return this.extractVersionPart(driverVersion, 1);
	}

	public int getDriverMinorVersion() {
		return this.extractVersionPart(driverVersion, 2);
	}

	public String getDatabaseProductName() throws SQLException {
		return "Neo4j";
	}

	public String getDatabaseProductVersion() throws SQLException {
		return this.databaseVersion;
	}

	public int getDatabaseMajorVersion() throws SQLException {
		return this.extractVersionPart(driverVersion, 1);
	}

	public int getDatabaseMinorVersion() throws SQLException {
		return this.extractVersionPart(driverVersion, 2);
	}

	public int getJDBCMajorVersion() throws SQLException {
		return 4;
	}

	public int getJDBCMinorVersion() throws SQLException {
		return 0;
	}

	public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	// Here make a list of cypher keyword ?
	public String getSQLKeywords() throws SQLException {
		return "";
	}

	public String getNumericFunctions() throws SQLException {
		return "";
	}

	public String getStringFunctions() throws SQLException {
		return "";
	}

	public String getTimeDateFunctions() throws SQLException {
		return "";
	}

	public String getExtraNameCharacters() throws SQLException {
		return "";
	}

	public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	public String getCatalogTerm() throws SQLException {
		return null;
	}

	public String getCatalogSeparator() throws SQLException {
		return "";
	}

	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return false;
	}

	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return false;
	}

	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return false;
	}

	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return false;
	}

	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return false;
	}

	public int getDefaultTransactionIsolation() throws SQLException {
		return 0;
	}

	public boolean supportsTransactions() throws SQLException {
		return true;
	}

	public ResultSet getSchemas() throws SQLException {
		return new ListResultSet(Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
	}

	public ResultSet getCatalogs() throws SQLException {
		return new ListResultSet(Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
	}

	public ResultSet getTableTypes() throws SQLException {
		List<Object> list = Collections.<Object>singletonList("TABLE");
		return new ListResultSet(Collections.singletonList(list), Collections.singletonList("TABLE_TYPE"));
	}

	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		return null;
	}

	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return null;
	}

	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	public boolean allProceduresAreCallable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean allTablesAreSelectable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	// this can be implemented
	public String getURL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	// this can be implemented
	public String getUserName() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	// it's always false with neo4j no ?
	public boolean isReadOnly() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean nullsAreSortedHigh() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean nullsAreSortedLow() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean nullsAreSortedAtStart() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean nullsAreSortedAtEnd() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean usesLocalFiles() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean usesLocalFilePerTable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public String getSystemFunctions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public String getSearchStringEscape() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsColumnAliasing() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsConvert() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsConvert(int fromType, int toType) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsExpressionsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOrderByUnrelated() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsGroupBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsGroupByUnrelated() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsLikeEscapeClause() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsMultipleTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsNonNullableColumns() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsMinimumSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsCoreSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsExtendedSQLGrammar() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsANSI92FullSQL() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsFullOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public String getSchemaTerm() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public String getProcedureTerm() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean isCatalogAtStart() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsPositionedDelete() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsPositionedUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsStoredProcedures() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsUnion() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsUnionAll() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxBinaryLiteralLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxCharLiteralLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnsInGroupBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnsInIndex() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnsInOrderBy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnsInSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxColumnsInTable() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxConnections() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxCursorNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxIndexLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxSchemaNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxProcedureNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxCatalogNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxRowSize() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxStatementLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxStatements() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxTableNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxTablesInSelect() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getMaxUserNameLength() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema,
			String foreignTable) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getTypeInfo() throws SQLException {
		return new ListResultSet(Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
	}

	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsResultSetType(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean ownDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean ownInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean othersDeletesAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean othersInsertsAreVisible(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean updatesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean deletesAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean insertsAreDetected(int type) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsBatchUpdates() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsSavepoints() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsNamedParameters() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getResultSetHoldability() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public int getSQLStateType() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsStatementPooling() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public RowIdLifetime getRowIdLifetime() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getClientInfoProperties() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public boolean generatedKeyAlwaysReturned() throws SQLException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
