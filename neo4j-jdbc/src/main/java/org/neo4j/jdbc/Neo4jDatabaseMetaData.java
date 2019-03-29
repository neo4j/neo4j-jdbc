/*able
 * Copyritring ght (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
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

import org.neo4j.jdbc.impl.ListNeo4jResultSet;
import org.neo4j.jdbc.metadata.Column;
import org.neo4j.jdbc.metadata.Table;
import org.neo4j.jdbc.utils.ExceptionBuilder;
import org.neo4j.jdbc.utils.Neo4jJdbcRuntimeException;

import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Neo4jDatabaseMetaData implements java.sql.DatabaseMetaData {

	public static final Logger LOGGER = Logger.getLogger(Neo4jDatabaseMetaData.class.getName());

	/**
	 * The regex to parse the version driver.
	 * NUMBER + . + NUMBER + .|- + STRING
	 */
	private static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+)\\.(\\d+)(\\.|-)?(.*)?$");

	protected static final int PROPERTY_SAMPLE_SIZE = 1000;
	
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
	 * Database labels.
	 */
	protected List<Table> databaseLabels;
	
	/**
	 * Database keys.
	 */
	protected List<Column> databaseProperties;
	
	/**
	 * The JDBC connection.
	 */
	private Neo4jConnection connection;

	/**
	 * Default constructor.
	 * Permit to load version and driver name from a property file.
	 *  @param connection the connection
	 *
	 *
	 */
	public Neo4jDatabaseMetaData(Neo4jConnection connection) {
		this.connection = connection;

		// Compute driver version, name, ...
		try (InputStream stream = Neo4jDatabaseMetaData.class.getResourceAsStream("/neo4j-jdbc-driver.properties")) {
			Properties properties = new Properties();
			properties.load(stream);
			this.driverName = properties.getProperty("driver.name");
			this.driverVersion = properties.getProperty("driver.version");
		} catch (Exception e) {
			this.driverName = "Neo4j JDBC Driver";
			this.driverVersion = "Unknown";
			throw new Neo4jJdbcRuntimeException(e);
		}
		
		this.databaseLabels = new ArrayList<>();
		this.databaseProperties = new ArrayList<>();
	}

	/**
	 * Extract a part of a Version
	 *
	 * @param position 1 for the major, 2 for minor and 3 for revision
	 * @return The corresponding driver version part if it's possible, otherwise -1
	 */
	private int extractVersionPart(int position) {
		int result = -1;
		try {
			Matcher matcher = VERSION_REGEX.matcher(this.getDriverVersion());
			if (matcher.find()) {
				result = Integer.valueOf(matcher.group(position));
			}
		} catch (SQLException e) {
			LOGGER.log(Level.FINEST, "Silent exception", e);
		}
		return result;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	/*------------------------------------*/
	/*       Default implementation       */
	/*------------------------------------*/

	@Override public java.sql.Connection getConnection() throws SQLException {
		return this.connection;
	}

	@Override public String getDriverName() throws SQLException {
		return this.driverName;
	}

	@Override public String getDriverVersion() throws SQLException {
		return this.driverVersion;
	}

	@Override public int getDriverMajorVersion() {
		return this.extractVersionPart(1);
	}

	@Override public int getDriverMinorVersion() {
		return this.extractVersionPart(2);
	}

	@Override public String getDatabaseProductName() throws SQLException {
		return "Neo4j";
	}

	@Override public String getDatabaseProductVersion() throws SQLException {
		return this.databaseVersion;
	}

	@Override public int getDatabaseMajorVersion() throws SQLException {
		return this.extractVersionPart(1);
	}

	@Override public int getDatabaseMinorVersion() throws SQLException {
		return this.extractVersionPart(2);
	}

	@Override public int getJDBCMajorVersion() throws SQLException {
		return 4;
	}

	@Override public int getJDBCMinorVersion() throws SQLException {
		return 0;
	}

	@Override public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	@Override public String getSQLKeywords() throws SQLException {
		return "UNION,ALL,OPTIONAL,MATCH,UNWIND,MERGE,ON,CREATE,SET,DELETE,DETACH,REMOVE,WITH,DISTINCT,RETURN,ORDER,BY,SKIP,LIMIT,"
				+ "ASCENDING,ASC,DESCENDING,DESC,WHERE,AND,OR,XOR,NOT,FOREACH,CALL,USING,INDEX,DROP,CONSTRAINT,ASSERT,UNIQUE,LOAD,CSV,"
				+ "FROM,HEADERS,AS,START,CASE,WHEN,THEN,ELSE,END,STARTS,ENDS,CONTAINS";
	}

	@Override public String getNumericFunctions() throws SQLException {
		return "abs,rand,round,ceil,floor,sqrt,sign,sin,cos,tan,cot,asin,acos,atan,atan2,havesin,degrees,radians,pi,log10,log,exp,e";
	}

	@Override public String getStringFunctions() throws SQLException {
		return "toString,replace,substring,left,right,trim,ltrim,rtrim,upper,lower,split,reverse,length";
	}

	@Override public String getTimeDateFunctions() throws SQLException {
		return "timestamp";
	}

	@Override public String getExtraNameCharacters() throws SQLException {
		return "";
	}

	@Override public boolean supportsMultipleResultSets() throws SQLException {
		return true;
	}

	@Override public String getCatalogTerm() throws SQLException {
		return null;
	}

	@Override public String getCatalogSeparator() throws SQLException {
		return null;
	}

	@Override public boolean supportsSchemasInDataManipulation() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return false;
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

	@Override public int getDefaultTransactionIsolation() throws SQLException {
		return Neo4jConnection.TRANSACTION_READ_COMMITTED;
	}

	@Override public boolean supportsTransactions() throws SQLException {
		return true;
	}

	@Override public ResultSet getSchemas() throws SQLException {
		return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Arrays.asList("TABLE_SCHEM","TABLE_CATALOG"));
	}

	@Override public ResultSet getCatalogs() throws SQLException {
		return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Arrays.asList("TABLE_CAT"));
	}

	@Override public ResultSet getTableTypes() throws SQLException {
		List<Object> tableTypes = new ArrayList<>();
		tableTypes.add("TABLE");
		List<List<Object>> schemas = new ArrayList<>();
		schemas.add(tableTypes);
		return ListNeo4jResultSet.newInstance(false, schemas, Arrays.asList("TABLE_TYPE"));
	}

	@Override public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
	}

	/**
	 *
	 * @param catalog works only with null, because #getCatalogs() return no rows
	 * @param schemaPattern works only with null, because #getSchemas() return no rows
	 * @param tableNamePattern works with % too
	 * @param types
	 * @return
	 * @throws SQLException
	 */
	@Override public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		if (this.databaseLabels == null || this.databaseLabels.isEmpty()) {
			return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
		}

		//TODO how to manage types? nowadays there's only 'TABLE'

		String pattern = toPattern(tableNamePattern);

		List<List<Object>> tables = new ArrayList<>();
		for (Table databaseLabel : this.databaseLabels) {
			if(databaseLabel.getTableName().matches(pattern)){
				tables.add(databaseLabel.toResultSetRow());
			}
		}
		return ListNeo4jResultSet.newInstance(false, tables, Table.getColumns());
	}

	/**
	 * Convert an input pattern (sql) to a java regex pattern
	 * @param sqlPattern
	 * @return
	 */
	private String toPattern(String sqlPattern) {
		String pattern = null;
		if(sqlPattern == null){
			pattern = ".*";//any
		}else {
			pattern = sqlPattern.replaceAll("%",".*");// % SQL stands for ANY
		}
		return pattern;
	}

	/**
	 *
	 * @param catalog works only with null, because #getCatalogs() return no rows
	 * @param schemaPattern works only with null, because #getSchemas() return no rows
	 * @param tableNamePattern works with % too
	 * @param columnNamePattern works with % too
	 * @return
	 * @throws SQLException
	 */
	@Override public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		if (this.databaseProperties == null || this.databaseProperties.isEmpty()) {
			return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
		}

		String tablePattern = toPattern(tableNamePattern);
		String columnPattern = toPattern(columnNamePattern);

		List<List<Object>> columns = new ArrayList<>();
		for (Column databaseKey : this.databaseProperties) {
			if (databaseKey.getTableName().matches(tablePattern) &&
					databaseKey.getColumnName().matches(columnPattern)) {
				columns.add(databaseKey.toResultSetRow());
			}
		}
		return ListNeo4jResultSet.newInstance(false, columns, Column.getColumns());
	}

	@Override public String getSearchStringEscape() throws SQLException {
		return "\\";
	}

	@Override public String getUserName() throws SQLException {
		return this.connection.getUserName();
	}

	@Override public boolean isReadOnly() throws SQLException {
		return this.connection.isReadOnly();
	}

	@Override public String getURL() throws SQLException {
		return this.connection.getUrl();
	}

	@Override public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	@Override public boolean storesUpperCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override public boolean storesLowerCaseIdentifiers() throws SQLException {
		return false;
	}

	@Override public boolean storesMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	@Override public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	@Override public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	@Override public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	@Override public boolean supportsSelectForUpdate() throws SQLException {
		return false;
	}

	@Override public boolean supportsStoredProcedures() throws SQLException {
		return false;
	}

	@Override public int getMaxStatements() throws SQLException {
		return 0;
	}

	@Override public int getMaxConnections() throws SQLException {
		return 0;
	}

	@Override public boolean allProceduresAreCallable() throws SQLException {
		return true;
	}
	
	@Override public boolean allTablesAreSelectable() throws SQLException {
		return true;
	}
	
	@Override public boolean nullsAreSortedHigh() throws SQLException {
		return false;
	}
	
	@Override public boolean nullsAreSortedLow() throws SQLException {
		return true;
	}
	
	@Override public boolean nullsAreSortedAtStart() throws SQLException {
		return false;
	}

	@Override public boolean nullsAreSortedAtEnd() throws SQLException {
		return true;
	}
	
	/*---------------------------------*/
	/*       Not implemented yet       */
	/*---------------------------------*/

	@Override public boolean usesLocalFiles() throws SQLException {
		return false;
	}

	@Override public boolean usesLocalFilePerTable() throws SQLException {
		return false;
	}

	@Override public String getSystemFunctions() throws SQLException {
		return "";
	}

	@Override public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return false;
	}

	@Override public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return false;
	}

	@Override public boolean supportsColumnAliasing() throws SQLException {
		return true;
	}

	@Override public boolean nullPlusNonNullIsNull() throws SQLException {
		return true;
	}

	@Override public boolean supportsConvert() throws SQLException {
		return false;
	}

	@Override public boolean supportsConvert(int fromType, int toType) throws SQLException {
		return false;
	}

	@Override public boolean supportsTableCorrelationNames() throws SQLException {
		return false;
	}

	@Override public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		return false;
	}

	@Override public boolean supportsExpressionsInOrderBy() throws SQLException {
		return true;
	}

	@Override public boolean supportsOrderByUnrelated() throws SQLException {
		return false;
	}

	@Override public boolean supportsGroupBy() throws SQLException {
		return false;
	}

	@Override public boolean supportsGroupByUnrelated() throws SQLException {
		return false;
	}

	@Override public boolean supportsGroupByBeyondSelect() throws SQLException {
		return false;
	}

	@Override public boolean supportsLikeEscapeClause() throws SQLException {
		return false;
	}

	@Override public boolean supportsMultipleTransactions() throws SQLException {
		return false;
	}

	@Override public boolean supportsNonNullableColumns() throws SQLException {
		return false;
	}

	@Override public boolean supportsMinimumSQLGrammar() throws SQLException {
		return false;
	}

	@Override public boolean supportsCoreSQLGrammar() throws SQLException {
		return false;
	}

	@Override public boolean supportsExtendedSQLGrammar() throws SQLException {
		return false;
	}

	@Override public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return false;
	}

	@Override public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return false;
	}

	@Override public boolean supportsANSI92FullSQL() throws SQLException {
		return false;
	}

	@Override public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return false;
	}

	@Override public boolean supportsOuterJoins() throws SQLException {
		return false;
	}

	@Override public boolean supportsFullOuterJoins() throws SQLException {
		return false;
	}

	@Override public boolean supportsLimitedOuterJoins() throws SQLException {
		return false;
	}

	@Override public String getSchemaTerm() throws SQLException {
		return "";
	}

	@Override public String getProcedureTerm() throws SQLException {
		return "procedure";
	}

	@Override public boolean isCatalogAtStart() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return false;
	}

	@Override public boolean supportsPositionedDelete() throws SQLException {
		return false;
	}

	@Override public boolean supportsPositionedUpdate() throws SQLException {
		return false;
	}

	@Override public boolean supportsSubqueriesInComparisons() throws SQLException {
		return false;
	}

	@Override public boolean supportsSubqueriesInExists() throws SQLException {
		return false;
	}

	@Override public boolean supportsSubqueriesInIns() throws SQLException {
		return false;
	}

	@Override public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return false;
	}

	@Override public boolean supportsCorrelatedSubqueries() throws SQLException {
		return false;
	}

	@Override public boolean supportsUnion() throws SQLException {
		return true;
	}

	@Override public boolean supportsUnionAll() throws SQLException {
		return true;
	}

	@Override public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return false;
	}

	@Override public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return false;
	}

	@Override public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return false;
	}

	@Override public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return false;
	}

	@Override public int getMaxBinaryLiteralLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxCharLiteralLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxColumnNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxColumnsInGroupBy() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public int getMaxColumnsInIndex() throws SQLException {
		return 0;
	}

	@Override public int getMaxColumnsInOrderBy() throws SQLException {
		return 0;
	}

	@Override public int getMaxColumnsInSelect() throws SQLException {
		return 0;
	}

	@Override public int getMaxColumnsInTable() throws SQLException {
		return 0;
	}

	@Override public int getMaxCursorNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxIndexLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxSchemaNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxProcedureNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxCatalogNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxRowSize() throws SQLException {
		return 0;
	}

	@Override public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return false;
	}

	@Override public int getMaxStatementLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxTableNameLength() throws SQLException {
		return 0;
	}

	@Override public int getMaxTablesInSelect() throws SQLException {
		return 0;
	}

	@Override public int getMaxUserNameLength() throws SQLException {
		return 0;
	}

	@Override public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
		return level == Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		return false;
	}

	@Override public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		return true;
	}

	@Override public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return false;
	}

	@Override public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return false;
	}

	private ResultSet emptyResultSet() {
		return ListNeo4jResultSet.newInstance(false, Collections.<List<Object>>emptyList(), Collections.<String>emptyList());
	}
	@Override public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema,
			String foreignTable) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getTypeInfo() throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		List<String> filters = new ArrayList<>();
		boolean hasTable = table != null && !table.trim().isEmpty();
		if (hasTable) {
			filters.add("label = ?");
		}
		if (unique) {
			filters.add("type = 'node_unique_property'");
		}
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("CALL db.indexes() YIELD description, label, properties, state, type\n");
		if (!filters.isEmpty()) {
			queryBuilder.append("WHERE " + String.join(" AND ", filters) + "\n");
		}
		queryBuilder.append("WITH description, label, properties, state, type\n");
		queryBuilder.append("UNWIND range(0, size(properties) - 1) AS index\n");
		queryBuilder.append("RETURN null AS TABLE_CAT,\n");
		queryBuilder.append("null AS TABLE_SCHEM,\n");
		queryBuilder.append("label AS TABLE_NAME,\n");
		queryBuilder.append("type <> 'node_unique_property' AS NON_UNIQUE,\n");
		queryBuilder.append("description AS INDEX_QUALIFIER,\n");
		queryBuilder.append("description AS INDEX_NAME,\n");
		queryBuilder.append(DatabaseMetaData.tableIndexOther + " AS TYPE,\n");
		queryBuilder.append("index + 1 AS ORDINAL_POSITION,\n");
		queryBuilder.append("properties[index] AS COLUMN_NAME,\n");
		queryBuilder.append("null AS ASC_OR_DESC,\n");
		queryBuilder.append("null AS CARDINALITY,\n");
		queryBuilder.append("null AS PAGES,\n");
		queryBuilder.append("null AS FILTER_CONDITION");
		PreparedStatement ps = this.connection.prepareStatement(queryBuilder.toString());
		if (hasTable) {
			ps.setString(1, table);
		}
		return ps.executeQuery();
	}

	@Override public boolean supportsResultSetType(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
		return false;
	}

	@Override public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override public boolean ownDeletesAreVisible(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override public boolean ownInsertsAreVisible(int type) throws SQLException {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override public boolean othersDeletesAreVisible(int type) throws SQLException {
		return false;
	}

	@Override public boolean othersInsertsAreVisible(int type) throws SQLException {
		return false;
	}

	@Override public boolean updatesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override public boolean deletesAreDetected(int type) throws SQLException {
		return false;
	}

	@Override public boolean insertsAreDetected(int type) throws SQLException {
		return false;
	}

	@Override public boolean supportsBatchUpdates() throws SQLException {
		return true;
	}

	@Override public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		return emptyResultSet();
	}

	@Override public boolean supportsSavepoints() throws SQLException {
		return false;
	}

	@Override public boolean supportsNamedParameters() throws SQLException {
		return false;
	}

	@Override public boolean supportsMultipleOpenResults() throws SQLException {
		return false;
	}

	@Override public boolean supportsGetGeneratedKeys() throws SQLException {
		return false;
	}

	@Override public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public boolean supportsResultSetHoldability(int holdability) throws SQLException {
		return false;
	}

	@Override public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override public int getSQLStateType() throws SQLException {
		return -1;
	}

	@Override public boolean locatorsUpdateCopy() throws SQLException {
		return false;
	}

	@Override public boolean supportsStatementPooling() throws SQLException {
		return false;
	}

	@Override public RowIdLifetime getRowIdLifetime() throws SQLException {
		return RowIdLifetime.ROWID_UNSUPPORTED;
	}

	@Override public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return true;
	}

	@Override public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return false;
	}

	@Override public ResultSet getClientInfoProperties() throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
			throws SQLException {
		return emptyResultSet();
	}

	@Override public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return emptyResultSet();
	}

	@Override public boolean generatedKeyAlwaysReturned() throws SQLException {
		return false;
	}

}
