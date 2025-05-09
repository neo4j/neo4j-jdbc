/*
 * Copyright (c) 2023-2025 "Neo4j,"
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
package org.neo4j.jdbc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.jdbc.Neo4jTransaction.PullResponse;
import org.neo4j.jdbc.Neo4jTransaction.ResultSummary;
import org.neo4j.jdbc.Neo4jTransaction.RunResponse;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.neo4j.jdbc.Neo4jException.withReason;

/**
 * Internal implementation for providing Neo4j specific database metadata.
 * <p>
 * PostgreSQL and SQL Standard way of defining Catalogs and Schema:
 *
 * <ul>
 * <li>A computer may have one cluster or multiple.</li>
 * <li>A database server is a cluster.</li>
 * <li>A cluster has catalogs. ( Catalog = Database )</li>
 * <li>Catalogs have schemas. (Schema = namespace of tables, and security boundary)</li>
 * <li>Schemas have tables</li>
 * <li>Tables have rows</li>
 * <li>Rows have values, defined by columns</li>
 * </ul>
 * From this excellent <a href="https://stackoverflow.com/a/17943883/1547989">answer</a>.
 *
 * @author Michael J. Simons
 * @author Conor Watson
 * @since 6.0.0
 */
final class DatabaseMetadataImpl implements Neo4jDatabaseMetaData {

	private static final Properties QUERIES;

	private static final String COL_TABLE_NAME = "TABLE_NAME";

	private static final String COL_TABLE_CAT = "TABLE_CAT";

	private static final String COL_TABLE_SCHEM = "TABLE_SCHEM";

	private static final String COL_COLUMN_NAME = "COLUMN_NAME";

	private static final String COL_GRANTOR = "GRANTOR";

	private static final String COL_GRANTEE = "GRANTEE";

	private static final String COL_PRIVILEGE = "PRIVILEGE";

	private static final String COL_IS_GRANTABLE = "IS_GRANTABLE";

	private static final String COL_SCOPE = "SCOPE";

	private static final String COL_DATA_TYPE = "DATA_TYPE";

	private static final String COL_TYPE_NAME = "TYPE_NAME";

	private static final String COL_COLUMN_SIZE = "COLUMN_SIZE";

	private static final String COL_BUFFER_LENGTH = "BUFFER_LENGTH";

	private static final String COL_DECIMAL_DIGITS = "DECIMAL_DIGITS";

	private static final String COL_PSEUDO_COLUMN = "PSEUDO_COLUMN";

	private static final String COL_TABLE_CATALOG = "TABLE_CATALOG";

	private static final String COL_KEY_SEQ = "KEY_SEQ";

	private static final String COL_PK_NAME = "PK_NAME";

	private static final String COL_PKTABLE_CAT = "PKTABLE_CAT";

	private static final String COL_PKTABLE_SCHEM = "PKTABLE_SCHEM";

	private static final String COL_PKTABLE_NAME = "PKTABLE_NAME";

	private static final String COL_PKCOLUMN_NAME = "PKCOLUMN_NAME";

	private static final String COL_FKTABLE_CAT = "FKTABLE_CAT";

	private static final String COL_FKTABLE_SCHEM = "FKTABLE_SCHEM";

	private static final String COL_FKTABLE_NAME = "FKTABLE_NAME";

	private static final String COL_FKCOLUMN_NAME = "FKCOLUMN_NAME";

	private static final String COL_UPDATE_RULE = "UPDATE_RULE";

	private static final String COL_DELETE_RULE = "DELETE_RULE";

	private static final String COL_FK_NAME = "FK_NAME";

	private static final String COL_DEFERRABILITY = "DEFERRABILITY";

	private static final String COL_PRECISION = "PRECISION";

	private static final String COL_LITERAL_PREFIX = "LITERAL_PREFIX";

	private static final String COL_LITERAL_SUFFIX = "LITERAL_SUFFIX";

	private static final String COL_CREATE_PARAMS = "CREATE_PARAMS";

	private static final String COL_AUTO_INCREMENT = "AUTO_INCREMENT";

	private static final String COL_SQL_DATETIME_SUB = "SQL_DATETIME_SUB";

	private static final String COL_FIXED_PREC_SCALE = "FIXED_PREC_SCALE";

	private static final String COL_UNSIGNED_ATTRIBUTE = "UNSIGNED_ATTRIBUTE";

	private static final String COL_MAXIMUM_SCALE = "MAXIMUM_SCALE";

	private static final String COL_SQL_DATA_TYPE = "SQL_DATA_TYPE";

	private static final String COL_NUM_PREC_RADIX = "NUM_PREC_RADIX";

	private static final String COL_LOCAL_TYPE_NAME = "LOCAL_TYPE_NAME";

	private static final String COL_NULLABLE = "NULLABLE";

	private static final String COL_CASE_SENSITIVE = "CASE_SENSITIVE";

	private static final String COL_MINIMUM_SCALE = "MINIMUM_SCALE";

	private static final String COL_SEARCHABLE = "SEARCHABLE";

	private static final String COL_TYPE_SCHEM = "TYPE_SCHEM";

	private static final String COL_TYPE_CAT = "TYPE_CAT";

	private static final String COL_SUPERTYPE_CAT = "SUPERTYPE_CAT";

	private static final String COL_SUPERTYPE_SCHEM = "SUPERTYPE_SCHEM";

	private static final String COL_SUPERTYPE_NAME = "SUPERTYPE_NAME";

	private static final String COL_SUPERTABLE_NAME = "SUPERTABLE_NAME";

	private static final String COL_ATTR_SIZE = "ATTR_SIZE";

	private static final String COL_SCOPE_SCHEMA = "SCOPE_SCHEMA";

	private static final String COL_SCOPE_TABLE = "SCOPE_TABLE";

	private static final String COL_CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH";

	private static final String COL_SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE";

	private static final String COL_ORDINAL_POSITION = "ORDINAL_POSITION";

	private static final String COL_IS_NULLABLE = "IS_NULLABLE";

	private static final String COL_REMARKS = "REMARKS";

	private static final String COL_ATTR_NAME = "ATTR_NAME";

	private static final String COL_ATTR_TYPE_NAME = "ATTR_TYPE_NAME";

	private static final String COL_SCOPE_CATALOG = "SCOPE_CATALOG";

	private static final String COL_ATTR_DEF = "ATTR_DEF";

	private static final String COL_COLUMN_USAGE = "COLUMN_USAGE";

	static {
		QUERIES = new Properties();
		try {
			QUERIES.load(Objects.requireNonNull(
					DatabaseMetadataImpl.class.getResourceAsStream("/queries/DatabaseMetadata.properties")));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static final List<String> NUMERIC_FUNCTIONS = List.of("abs", "ceil", "floor", "isNaN", "rand", "round",
			"sign", "e", "exp", "log", "log10", "sqrt", "acos", "asin", "atan", "atan2", "cos", "cot", "degrees",
			"haversin", "pi", "radians", "sin", "tan");

	private static final List<String> STRING_FUNCTIONS = List.of("left", "ltrim", "replace", "reverse", "right",
			"rtrim", "split", "substring", "toLower", "toString", "toStringOrNull", "toUpper", "trim");

	private static final List<String> TIME_DATE_FUNCTIONS = List.of("date", "datetime", "localdatetime", "localtime",
			"time", "duration");

	private static final List<ClientInfoProperty> SUPPORTED_CLIENT_INFO_PROPERTIES = List.of(
			new ClientInfoProperty("ApplicationName", "The name of the application currently utilizing the connection"),
			new ClientInfoProperty("ClientUser",
					"The name of the user that the application using the connection is performing work for"),
			new ClientInfoProperty("ClientHostname",
					"The hostname of the computer the application using the connection is running on"));

	private static final Logger LOGGER = Logger.getLogger(DatabaseMetadataImpl.class.getCanonicalName());

	private final Connection connection;

	private final boolean automaticSqlTranslation;

	private final int relationshipSampleSize;

	private final Lazy<Boolean, RuntimeException> apocAvailable;

	private final Lazy<String, SQLException> userName;

	private final Lazy<Boolean, SQLException> readOnly;

	private final Map<GetTablesCacheKey, GetTablesCacheValue> tablesCache = new ConcurrentHashMap<>();

	DatabaseMetadataImpl(Connection connection, boolean automaticSqlTranslation, int relationshipSampleSize) {
		this.connection = connection;
		this.automaticSqlTranslation = automaticSqlTranslation;
		this.relationshipSampleSize = relationshipSampleSize;

		this.apocAvailable = Lazy.<Boolean, RuntimeException>of(this::isApocAvailable0);
		// Those queries use administrative commands that do not compose with normal
		// queries, so we cache it here and hope for the best they don't interfer with
		// other queries.
		this.userName = Lazy.of((ThrowingSupplier<String, SQLException>) () -> {
			var response = doQueryForPullResponse(getRequest("getUserName"));
			return response.records().get(0).get(0).asString();
		});
		this.readOnly = Lazy.of((ThrowingSupplier<Boolean, SQLException>) () -> {
			var response = doQueryForPullResponse(getRequest("isReadOnly", "name", this.getSingleCatalog()));
			return response.records().get(0).get(0).asBoolean();
		});
	}

	boolean isApocAvailable() {
		return this.apocAvailable.resolve();
	}

	private boolean isApocAvailable0() {
		try {
			var response = doQueryForPullResponse(new Request(
					"SHOW FUNCTIONS YIELD name WHERE name = 'apoc.version' RETURN count(*) >= 1 AS available",
					Map.of()));
			var records = response.records();
			return records.size() == 1 && records.get(0).get("available").asBoolean();
		}
		catch (SQLException ex) {
			return false;
		}
	}

	static Request getRequest(String queryName, Object... keyAndValues) {
		Map<String, Object> args = new HashMap<>();

		// Yolo in all ways possible, internally called only anyway
		for (int i = 0; i < keyAndValues.length; i += 2) {
			args.put((String) keyAndValues[i], keyAndValues[i + 1]);
		}
		return new Request(QUERIES.getProperty(queryName), args);
	}

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		var response = doQueryForPullResponse(getRequest("allProceduresAreCallable"));

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
	public boolean allTablesAreSelectable() {
		return true;
	}

	@Override
	public String getURL() throws SQLException {
		return this.connection.unwrap(Neo4jConnection.class).getDatabaseURL().toString();
	}

	@Override
	public String getUserName() throws SQLException {
		return this.userName.resolve();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return this.readOnly.resolve();
	}

	// Wrt ordering see
	// https://neo4j.com/docs/cypher-manual/current/clauses/order-by/#order-null
	@Override
	public boolean nullsAreSortedHigh() {
		return true;
	}

	@Override
	public boolean nullsAreSortedLow() {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtStart() {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() {
		return true;
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		var response = doQueryForPullResponse(getRequest("getDatabaseProductName"));

		var record = response.records().get(0);
		return "%s-%s-%s".formatted(record.get(0).asString(), record.get(1).asString(), record.get(2).asString());
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		var response = doQueryForPullResponse(getRequest("getDatabaseProductVersion"));

		return response.records().get(0).get(0).asString();
	}

	@Override
	public String getDriverName() {
		return "Neo4j JDBC Driver";
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
	public boolean usesLocalFiles() {
		return false;
	}

	@Override
	public boolean usesLocalFilePerTable() {
		return false;
	}

	// Identifiers are actually all case-sensitive in neo4j (apart from build in
	// procedures), i.e.
	// WITH 1 AS foobar WITH FooBar AS bazbar RETURN bazbar
	// will fail with Variable `FooBar` not defined, which also applies to result.
	// From
	// WITH 1 AS FooBar RETURN FooBar AS foobar, FooBar
	// you can request both foobar and FooBar
	@Override
	public boolean supportsMixedCaseIdentifiers() {
		return true;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() {
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() {
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() {
		return true;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() {
		return true;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() {
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() {
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() {
		return true;
	}

	@Override
	public String getIdentifierQuoteString() {
		return "`";
	}

	@Override
	public String getSQLKeywords() {
		return "";
	}

	@Override
	public String getNumericFunctions() {
		return String.join(",", NUMERIC_FUNCTIONS);
	}

	@Override
	public String getStringFunctions() {
		return String.join(",", STRING_FUNCTIONS);
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		var functions = new ArrayList<String>();
		try (var rs = getFunctions(null, null, null)) {
			while (rs.next()) {
				functions.add(rs.getString("FUNCTION_NAME"));
			}
		}
		return String.join(",", functions);
	}

	@Override
	public String getTimeDateFunctions() {
		return String.join(",", TIME_DATE_FUNCTIONS);
	}

	@Override
	public String getSearchStringEscape() {
		return "'";
	}

	@Override
	public String getExtraNameCharacters() {
		return "";
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() {
		return false;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() {
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() {
		return true;
	}

	@Override
	public boolean nullPlusNonNullIsNull() {
		return true;
	}

	@Override
	public boolean supportsConvert() {
		LOGGER.log(Level.FINE, "supportsConvert returns false for now, that might change in the future.");
		return false;
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) {
		LOGGER.log(Level.FINE, "supportsConvert returns false for now, that might change in the future.");
		return false;
	}

	@Override
	public boolean supportsTableCorrelationNames() {
		return this.automaticSqlTranslation;
	}

	// This method is supposed to return `false` when table correlations names are
	// restricted to being different
	// from the referenced table.
	@Override
	public boolean supportsDifferentTableCorrelationNames() {
		return false;
	}

	@Override
	public boolean supportsExpressionsInOrderBy() {
		return true;
	}

	@Override
	public boolean supportsOrderByUnrelated() {
		return true;
	}

	@Override
	public boolean supportsGroupBy() {
		return true;
	}

	@Override
	public boolean supportsGroupByUnrelated() {
		return true;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() {
		return true;
	}

	@Override
	public boolean supportsLikeEscapeClause() {
		return false;
	}

	@Override
	public boolean supportsMultipleResultSets() {
		return false;
	}

	@Override
	public boolean supportsMultipleTransactions() {
		return true;
	}

	@Override
	public boolean supportsNonNullableColumns() {
		return true;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() {
		return this.automaticSqlTranslation;
	}

	@Override
	public boolean supportsCoreSQLGrammar() {
		return this.automaticSqlTranslation;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() {
		if (this.automaticSqlTranslation) {
			LOGGER.log(Level.FINE,
					"supportsExtendedSQLGrammar returns false for now despite automatic sql translation being on, that might change in the future.");
		}
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() {
		return this.automaticSqlTranslation;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() {
		if (this.automaticSqlTranslation) {
			LOGGER.log(Level.FINE,
					"supportsANSI92IntermediateSQL returns false for now despite automatic sql translation being on, that might change in the future.");
		}
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() {
		if (this.automaticSqlTranslation) {
			LOGGER.log(Level.FINE,
					"supportsANSI92FullSQL returns false for now despite automatic sql translation being on, that might change in the future.");
		}
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() {
		return false;
	}

	@Override
	public boolean supportsOuterJoins() {
		return this.automaticSqlTranslation;
	}

	@Override
	public boolean supportsFullOuterJoins() {
		return this.automaticSqlTranslation;
	}

	@Override
	public boolean supportsLimitedOuterJoins() {
		return false;
	}

	@Override
	public String getSchemaTerm() {
		return "schema";
	}

	@Override
	public String getProcedureTerm() {
		return "procedure";
	}

	@Override
	public String getCatalogTerm() {
		return "database";
	}

	@Override
	public boolean isCatalogAtStart() {
		return true;
	}

	@Override
	public String getCatalogSeparator() {
		return ".";
	}

	@Override
	public boolean supportsSchemasInDataManipulation() {
		return false;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() {
		return false;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() {
		return false;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() {
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() {
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() {
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() {
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() {
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInExists() {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInIns() {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() {
		return false;
	}

	@Override
	public boolean supportsCorrelatedSubqueries() {
		return false;
	}

	@Override
	public boolean supportsUnion() {
		return true;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() {
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() {
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() {
		return false;
	}

	@Override
	public int getMaxBinaryLiteralLength() {
		return 0; // No limit or unknown
	}

	@Override
	public int getMaxCharLiteralLength() {
		return 0;
	}

	@Override
	public int getMaxColumnNameLength() {
		return 0;
	}

	@Override
	public int getMaxColumnsInGroupBy() {
		return 0;
	}

	@Override
	public int getMaxColumnsInIndex() {
		return 0;
	}

	@Override
	public int getMaxColumnsInOrderBy() {
		return 0;
	}

	@Override
	public int getMaxColumnsInSelect() {
		return 0;
	}

	@Override
	public int getMaxColumnsInTable() {
		return 0;
	}

	@Override
	public int getMaxConnections() throws SQLException {
		var response = doQueryForPullResponse(getRequest("getMaxConnections"));

		if (response.records().isEmpty()) {
			return 0;
		}
		var record = response.records().get(0);
		return record.get(0).asInt();
	}

	@Override
	public int getMaxCursorNameLength() {
		return 0;
	}

	@Override
	public int getMaxIndexLength() {
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() {
		return 0;
	}

	@Override
	public int getMaxProcedureNameLength() {
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() {
		return 63;
	}

	@Override
	public int getMaxRowSize() {
		return 0;
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() {
		return true;
	}

	@Override
	public int getMaxStatementLength() {
		return 0;
	}

	@Override
	public int getMaxStatements() {
		return 0;
	}

	@Override
	public int getMaxTableNameLength() {
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() {
		return 0;
	}

	@Override
	public int getMaxUserNameLength() {
		// At least that's the max length for other names
		// https://neo4j.com/docs/cypher-manual/current/syntax/naming
		return 65535;
	}

	@Override
	public int getDefaultTransactionIsolation() {
		return Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public boolean supportsTransactions() {
		return true;
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level) {
		return level == Connection.TRANSACTION_NONE || level == Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() {
		return true;
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() {
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() {
		return false;
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() {
		return false;
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

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var request = getRequest("getProcedures", "name", procedureNamePattern, "procedureType",
				DatabaseMetaData.procedureResultUnknown, "catalogAsParameterWorkaround", getSingleCatalog());
		return doQueryForResultSet(request);
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var intermediateResults = getArgumentDescriptions("PROCEDURES", procedureNamePattern);

		var request = getRequest("getProcedureColumns", "results", intermediateResults, "columnNamePattern",
				columnNamePattern, "columnType", DatabaseMetaData.procedureColumnIn, "nullable",
				DatabaseMetaData.procedureNullableUnknown, "catalogAsParameterWorkaround", getSingleCatalog());
		return doQueryForResultSet(request);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {

		assertSchemaIsPublicOrNull(schemaPattern);
		assertCatalogIsNullOrEmpty(catalog);

		try {
			var key = new GetTablesCacheKey(catalog, schemaPattern, tableNamePattern, types);
			var result = this.tablesCache.computeIfAbsent(key, this::getTables0);
			// We cannot cache the result set, as any proper usage would close it for
			// good, and it's much harder to dig down into the implementation and prevent
			// closing it on a case base case basis than just recreating it
			return new LocalStatementImpl(this.connection, result.runResponse, result.pullResponse).getResultSet();
		}
		catch (UncheckedSQLException ex) {
			throw ex.getCause();
		}
	}

	private GetTablesCacheValue getTables0(GetTablesCacheKey key) {

		var tableNamePattern = key.tableNamePattern();
		var types = key.types();

		var request = getRequest(isApocAvailable() ? "getTablesApoc" : "getTablesFallback", "name",
				(tableNamePattern != null) ? tableNamePattern.replace("%", ".*") : null, "sampleSize",
				this.relationshipSampleSize, "types", types);

		try (var resultSet = doQueryForResultSet(request)) {
			return GetTablesCacheValue.of(resultSet);
		}
		catch (SQLException ex) {
			throw new UncheckedSQLException(ex);
		}
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {

		return doQueryForResultSet(getRequest("getCatalogs"));
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		var keys = new ArrayList<String>();
		keys.add("TABLE_TYPE");

		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys,
				List.of(new Value[] { Values.value("TABLE") }, new Value[] { Values.value("RELATIONSHIP") }));
		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	static Value getMaxPrecision(int type) {
		if (type == Types.BIGINT) {
			// 64bit;
			return Values.value(19);
		}
		if (type == Types.INTEGER) {
			return Values.value(10);
		}
		if (type == Types.DOUBLE) {
			// 64bit double,
			// https://stackoverflow.com/questions/322749/retain-precision-with-double-in-java
			// Neo4j has no fixed point arithmetic, so it's kinda guess work.
			return Values.value(15);
		}
		return Values.NULL;
	}

	@SuppressWarnings("squid:S3776") // Yep, this is complex.
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		columnNamePattern = (columnNamePattern != null) ? columnNamePattern.replace("%", ".*") : columnNamePattern;
		var request = getRequest("getColumns", "name",
				(tableNamePattern != null) ? tableNamePattern.replace("%", ".*") : tableNamePattern, "column_name",
				columnNamePattern, "sampleSize", this.relationshipSampleSize);
		var innerColumnsResponse = doQueryForPullResponse(request);
		var records = innerColumnsResponse.records();

		var rows = new LinkedList<Value[]>();

		var columnPerLabel = new HashMap<Value, Set<Value>>();

		for (Record record : records) {

			var propertyName = record.get(1);
			var propertyTypes = record.get(2);

			if (propertyName.isNull() || propertyTypes.isNull()) {
				continue;
			}

			var nodeLabels = record.get(0);
			Value labelsOrTypes = nodeLabels;
			if ("RELATIONSHIP".equals(record.get("TABLE_TYPE").asString())) {
				// This column contains the flat rel type, so that we don't have to strip
				// away start / end node again.
				labelsOrTypes = Values.value(List.of(record.get("relationshipType").asString()));
			}
			var propertyTypeList = propertyTypes.asList(propertyType -> propertyType);
			var propertyType = getTypeFromList(propertyTypeList, propertyName.asString());

			var NULLABLE = DatabaseMetaData.columnNullable;
			var IS_NULLABLE = "YES";
			var innerRequest = getRequest("getColumns.nullability", "nodeLabels", labelsOrTypes, "propertyName",
					propertyName);
			try (var result = doQueryForResultSet(innerRequest)) {
				result.next();
				if (result.getBoolean(1)) {
					NULLABLE = DatabaseMetaData.columnNoNulls;
					IS_NULLABLE = "NO";
				}
			}

			var nodeLabelList = nodeLabels.asList(Function.identity());
			for (Value nodeLabel : nodeLabelList) {
				// Avoid duplicates while unrolling
				var properties = columnPerLabel.computeIfAbsent(nodeLabel, i -> new HashSet<>());
				if (properties.contains(propertyName)) {
					continue;
				}
				properties.add(propertyName);

				var values = addColumn(nodeLabel, propertyName, propertyType, NULLABLE, IS_NULLABLE,
						nodeLabelList.indexOf(nodeLabel) + 1, false);
				rows.add(values.toArray(Value[]::new));
			}
		}

		if (columnPerLabel.isEmpty()) {
			var tables = getTables(catalog, schemaPattern, tableNamePattern, null);
			while (tables.next()) {
				columnPerLabel.put(tables.getObject(COL_TABLE_NAME, Value.class), new HashSet<>());
			}
		}

		// Add artificial element ids
		for (Value v : columnPerLabel.keySet()) {
			boolean isRelationship = v.asString().contains("_");
			var additionalIds = new ArrayList<>(List.of("v$id"));
			if (isRelationship) {
				var result = getTables(null, null, v.asString(), new String[] { "RELATIONSHIP" });
				if (result.next()) {
					var definition = result.getString(COL_REMARKS).split("\n");
					additionalIds.add("v$" + definition[0].toLowerCase(Locale.ROOT) + "_id");
					additionalIds.add("v$" + definition[2].toLowerCase(Locale.ROOT) + "_id");
				}
			}
			for (var additionalId : additionalIds) {
				if (columnNamePattern != null && !additionalId.matches(columnNamePattern.replace("%", ".*"))) {
					continue;
				}

				var values = addColumn(v, Values.value(additionalId), Values.value("String"),
						DatabaseMetaData.columnNoNulls, "NO", 0, true);
				rows.add(0, values.toArray(Value[]::new));
			}
		}

		var keys = getKeysForGetColumns();
		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys, rows);

		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	private ArrayList<Value> addColumn(Value nodeLabel, Value propertyName, Value propertyType, int NULLABLE,
			String IS_NULLABLE, Integer ordinalPosition, boolean generated) throws SQLException {
		var values = new ArrayList<Value>();
		values.add(Values.value(getSingleCatalog())); // TABLE_CAT
		values.add(Values.value("public")); // TABLE_SCHEM is always public
		values.add(nodeLabel); // TABLE_NAME
		values.add(propertyName); // COLUMN_NAME
		var columnType = Neo4jConversions.toSqlTypeFromOldCypherType(propertyType.asString());
		values.add(Values.value(columnType)); // DATA_TYPE
		values.add(Values.value(Neo4jConversions.oldCypherTypesToNew(propertyType.asString()))); // TYPE_NAME
		values.add(getMaxPrecision(columnType)); // COLUMN_SIZE
		values.add(Values.NULL); // BUFFER_LENGTH
		values.add(Values.NULL); // DECIMAL_DIGITS
		values.add(Values.value(2)); // NUM_PREC_RADIX
		values.add(Values.value(NULLABLE));
		values.add(Values.NULL); // REMARKS
		values.add(Values.NULL); // COLUMN_DEF
		values.add(Values.NULL); // SQL_DATA_TYPE - unused
		values.add(Values.NULL); // SQL_DATETIME_SUB
		values.add(Values.NULL); // CHAR_OCTET_LENGTH
		values.add(Values.value(ordinalPosition)); // ORDINAL_POSITION
		values.add(Values.value(IS_NULLABLE));
		values.add(Values.NULL); // SCOPE_CATALOG
		values.add(Values.NULL); // SCOPE_SCHEMA
		values.add(Values.NULL); // SCOPE_TABLE
		values.add(Values.NULL); // SOURCE_DATA_TYPE
		values.add(Values.value("NO")); // IS_AUTOINCREMENT
		values.add(Values.value(generated ? "YES" : "NO")); // IS_GENERATEDCOLUMN
		return values;
	}

	static PullResponse staticPullResponseFor(List<String> keys, List<Value[]> rows) {
		return new PullResponse() {
			@Override
			public List<Record> records() {
				var records = new ArrayList<Record>(rows.size());

				for (Value[] values : rows) {
					records.add(Record.of(keys, values));
				}

				return records;
			}

			@Override
			public Optional<ResultSummary> resultSummary() {
				return Optional.empty(); // might need to populate this at some point.
			}

			@Override
			public boolean hasMore() {
				return false;
			}
		};
	}

	private static Value getTypeFromList(List<Value> types, String propertyName) {
		if (types.size() > 1) {
			LOGGER.log(Level.FINE,
					"More than one property type found for property %s, api will still return first one found.",
					propertyName);

			for (var propertyType : types) {
				if (propertyType.asString().equals("String")) {
					return propertyType;
				}
			}

			return Values.value("Any");
		}

		return types.get(0);
	}

	private static List<String> getKeysForGetColumns() {
		var keys = new ArrayList<String>();
		keys.add(COL_TABLE_CAT);
		keys.add(COL_TABLE_SCHEM);
		keys.add(COL_TABLE_NAME);
		keys.add(COL_COLUMN_NAME);
		keys.add(COL_DATA_TYPE);
		keys.add(COL_TYPE_NAME); // this will be computed if possible.
		keys.add(COL_COLUMN_SIZE);
		keys.add(COL_BUFFER_LENGTH);
		keys.add(COL_DECIMAL_DIGITS);
		keys.add(COL_NUM_PREC_RADIX);
		keys.add(COL_NULLABLE);
		keys.add(COL_REMARKS);
		keys.add("COLUMN_DEF");
		keys.add(COL_SQL_DATA_TYPE);
		keys.add(COL_SQL_DATETIME_SUB);
		keys.add(COL_CHAR_OCTET_LENGTH);
		keys.add(COL_ORDINAL_POSITION);
		keys.add(COL_IS_NULLABLE);
		keys.add(COL_SCOPE_CATALOG);
		keys.add(COL_SCOPE_SCHEMA);
		keys.add(COL_SCOPE_TABLE);
		keys.add(COL_SOURCE_DATA_TYPE);
		keys.add("IS_AUTOINCREMENT");
		keys.add("IS_GENERATEDCOLUMN");
		return keys;
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = List.of(COL_TABLE_CAT, COL_TABLE_SCHEM, COL_TABLE_NAME, COL_COLUMN_NAME, COL_GRANTOR, COL_GRANTEE,
				COL_PRIVILEGE, COL_IS_GRANTABLE);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var keys = List.of(COL_TABLE_CAT, COL_TABLE_SCHEM, COL_TABLE_NAME, COL_GRANTOR, COL_GRANTEE, COL_PRIVILEGE,
				COL_IS_GRANTABLE);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = List.of(COL_SCOPE, COL_COLUMN_NAME, COL_DATA_TYPE, COL_TYPE_NAME, COL_COLUMN_SIZE, COL_BUFFER_LENGTH,
				COL_DECIMAL_DIGITS, COL_PSEUDO_COLUMN);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = List.of(COL_SCOPE, COL_COLUMN_NAME, COL_DATA_TYPE, COL_TYPE_NAME, COL_COLUMN_SIZE, COL_BUFFER_LENGTH,
				COL_DECIMAL_DIGITS, COL_PSEUDO_COLUMN);
		return emptyResultSet(keys);
	}

	@SuppressWarnings("squid:S3776") // Yep, this is complex.
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = new ArrayList<String>();
		keys.add(COL_TABLE_CATALOG);
		keys.add(COL_TABLE_SCHEM);
		keys.add(COL_TABLE_NAME);
		keys.add(COL_COLUMN_NAME);
		keys.add(COL_KEY_SEQ);
		keys.add(COL_PK_NAME);
		List<Value[]> resultRows = List.of();

		if (table != null) {
			var finalTable = table;
			boolean relationshipChecked = false;
			// Check if it is a virtual table and extract the relationship name
			if (table.matches(".+?_.+?_.+?")) {
				var relationships = getTables(catalog, schema, table, new String[] { "RELATIONSHIP" });
				if (relationships.next()) {
					relationshipChecked = true;
					finalTable = relationships.getString(COL_REMARKS).split("\n")[1].trim();
				}
				relationships.close();
			}

			var request = getRequest("getPrimaryKeys", "name", finalTable);
			var pullResponse = doQueryForPullResponse(request);
			var records = pullResponse.records();

			var uniqueConstraints = new ArrayList<UniqueConstraint>();
			for (var record : records) {
				uniqueConstraints.add(new UniqueConstraint(record.get("name").asString(),
						record.get("labelsOrTypes").asList(Value::asString),
						record.get("properties").asList(Value::asString)));
			}

			// Exactly one unique constraint is fine
			if (uniqueConstraints.size() == 1) {
				resultRows = makeUniqueKeyValues(getSingleCatalog(), "public", table, uniqueConstraints);
			}
			// Otherwise we go with element ids if the "table" exists
			else {
				var exists = relationshipChecked;
				if (!exists) {
					var tables = getTables(catalog, schema, table, new String[] { "TABLE" });
					exists = tables.next();
					tables.close();
				}

				resultRows = exists
						? makeUniqueKeyValues(getSingleCatalog(), "public", table,
								List.of(new UniqueConstraint(table + "_elementId", List.of(table), List.of("v$id"))))
						: List.of();

			}
		}

		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys, resultRows);

		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	private List<Value[]> makeUniqueKeyValues(String catalog, String schema, String table,
			List<UniqueConstraint> uniqueConstraints) {
		List<Value[]> results = new ArrayList<>();
		for (var uniqueConstraint : uniqueConstraints) {
			for (var i = 0; i < uniqueConstraint.properties.size(); i++) {
				results.add(new Value[] { Values.value(catalog), Values.value(schema), Values.value(table),
						Values.value(uniqueConstraint.properties.get(i)), Values.value(i + 1),
						Values.value(uniqueConstraint.name) });
			}
		}
		return results;
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = new ArrayList<String>();
		return createKeysResultSet(keys);
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = new ArrayList<String>();
		return createKeysResultSet(keys);
	}

	private ResultSet createKeysResultSet(ArrayList<String> keys) throws SQLException {
		keys.add(COL_PKTABLE_CAT);
		keys.add(COL_PKTABLE_SCHEM);
		keys.add(COL_PKTABLE_NAME);
		keys.add(COL_PKCOLUMN_NAME);
		keys.add(COL_FKTABLE_CAT);
		keys.add(COL_FKTABLE_SCHEM);
		keys.add(COL_FKTABLE_NAME);
		keys.add(COL_FKCOLUMN_NAME);
		keys.add(COL_KEY_SEQ);
		keys.add(COL_UPDATE_RULE);
		keys.add(COL_DELETE_RULE);
		keys.add(COL_FK_NAME);
		keys.add(COL_PK_NAME);
		keys.add(COL_DEFERRABILITY);

		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = createEmptyPullResponse();

		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable,
			String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {

		assertCatalogIsNullOrEmpty(parentCatalog);
		assertSchemaIsPublicOrNull(parentSchema);
		assertCatalogIsNullOrEmpty(foreignCatalog);
		assertSchemaIsPublicOrNull(foreignSchema);

		// TODO the remarks on element ids in relationships from getColumns should be
		// duplicated here in a meaningful way
		var keys = List.of(COL_PKTABLE_CAT, COL_PKTABLE_SCHEM, COL_PKTABLE_NAME, COL_PKCOLUMN_NAME, COL_FKTABLE_CAT,
				COL_FKTABLE_SCHEM, COL_FKTABLE_NAME, COL_FKCOLUMN_NAME, COL_KEY_SEQ, COL_UPDATE_RULE, COL_DELETE_RULE,
				COL_FK_NAME, COL_PK_NAME, COL_DEFERRABILITY);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {

		var keys = List.of(COL_TYPE_NAME, COL_DATA_TYPE, COL_PRECISION, COL_LITERAL_PREFIX, COL_LITERAL_SUFFIX,
				COL_CREATE_PARAMS, COL_NULLABLE, COL_CASE_SENSITIVE, COL_SEARCHABLE, COL_UNSIGNED_ATTRIBUTE,
				COL_FIXED_PREC_SCALE, COL_AUTO_INCREMENT, COL_LOCAL_TYPE_NAME, COL_MINIMUM_SCALE, COL_MAXIMUM_SCALE,
				COL_SQL_DATA_TYPE, COL_SQL_DATETIME_SUB, COL_NUM_PREC_RADIX);

		var values = new ArrayList<Value[]>();
		for (var type : Type.values()) {
			var sqlType = Neo4jConversions.toSqlType(type);
			var row = new Value[] { Values.value(type.name()), Values.value(sqlType), getMaxPrecision(sqlType),
					Values.NULL, // Prefix and suffix are actually determined for some, we
									// should use this at some point
					Values.NULL, Values.NULL, Values.value(DatabaseMetaData.typeNullable),
					Values.value(type == Type.STRING),
					Values.value((type == Type.STRING) ? DatabaseMetaData.typeSearchable
							: (type != Type.RELATIONSHIP) ? DatabaseMetaData.typePredBasic
									: DatabaseMetaData.typePredNone),
					Values.value(false), Values.value(false), Values.NULL, Values.NULL, Values.NULL, Values.NULL,
					Values.NULL, Values.NULL, Values.value(10), };
			values.add(row);
		}

		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys, values);
		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();

	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
			throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var intermediateResults = new ArrayList<>();
		var request = getRequest("getIndexInfo", "name", table, "unique", unique);
		try (var rs = doQueryForResultSet(request)) {
			while (rs.next()) {
				intermediateResults.add(Map.of("name", rs.getString("name"), "tableName",
						rs.getObject("labelsOrTypes", Value.class).asList().get(0), "properties",
						rs.getObject("properties"), "owningConstraint", rs.getObject("owningConstraint", Value.class)));
			}
		}
		return doQueryForResultSet(getRequest("getIndexInfo.flattening", "results", intermediateResults, "type",
				DatabaseMetaData.tableIndexOther));
	}

	@Override
	public boolean supportsResultSetType(int type) {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) {
		return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) {
		return true;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) {
		return true;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) {
		return true;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) {
		return true;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) {
		return true;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) {
		return true;
	}

	@Override
	public boolean updatesAreDetected(int type) {
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) {
		return false;
	}

	@Override
	public boolean insertsAreDetected(int type) {
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() {
		return true;
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var keys = List.of(COL_TYPE_CAT, COL_TYPE_SCHEM, COL_TYPE_NAME, "CLASS_NAME", COL_DATA_TYPE, COL_REMARKS,
				"BASE_TYPE");
		return emptyResultSet(keys);
	}

	@Override
	public Connection getConnection() {
		return this.connection;
	}

	@Override
	public boolean supportsSavepoints() {
		return false;
	}

	/**
	 * The named parameter syntax in our {@link java.sql.CallableStatement} implementation
	 * {@link Neo4jCallableStatement} supports both {@code $name} and {@code :name}
	 * syntax. Named ordinalParameters cannot be mixed with parameter placeholders
	 * ({@literal ?}).
	 * @return always {@literal true}
	 */
	@Override
	public boolean supportsNamedParameters() {
		return true;
	}

	@Override
	public boolean supportsMultipleOpenResults() {
		return false;
	}

	@Override
	public boolean supportsGetGeneratedKeys() {
		return false;
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var keys = List.of(COL_TYPE_CAT, COL_TYPE_SCHEM, COL_TYPE_NAME, COL_SUPERTYPE_CAT, COL_SUPERTYPE_SCHEM,
				COL_SUPERTYPE_NAME);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var keys = List.of(COL_TABLE_CAT, COL_TABLE_SCHEM, COL_TABLE_NAME, COL_SUPERTABLE_NAME);
		return emptyResultSet(keys);
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern,
			String attributeNamePattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var keys = List.of(COL_TYPE_CAT, COL_TYPE_SCHEM, COL_TYPE_NAME, COL_ATTR_NAME, COL_DATA_TYPE,
				COL_ATTR_TYPE_NAME, COL_ATTR_SIZE, COL_DECIMAL_DIGITS, COL_NUM_PREC_RADIX, COL_NULLABLE, COL_REMARKS,
				COL_ATTR_DEF, COL_SQL_DATA_TYPE, COL_SQL_DATETIME_SUB, COL_CHAR_OCTET_LENGTH, COL_ORDINAL_POSITION,
				COL_IS_NULLABLE, COL_SCOPE_CATALOG, COL_SCOPE_SCHEMA, COL_SCOPE_TABLE, COL_SOURCE_DATA_TYPE);
		return emptyResultSet(keys);
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability) {
		return holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public int getResultSetHoldability() {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return Integer.parseInt(getDatabaseProductVersion().split("\\.")[0]);
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		var val = getDatabaseProductVersion().split("\\.")[1];
		var dash = val.indexOf("-");
		return Integer.parseInt(val.substring(0, (dash < 0) ? val.length() : dash));
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
	public int getSQLStateType() {
		return DatabaseMetaData.sqlStateSQL;
	}

	@Override
	public boolean locatorsUpdateCopy() {
		return true;
	}

	@Override
	public boolean supportsStatementPooling() {
		return false;
	}

	@Override
	public RowIdLifetime getRowIdLifetime() {
		return RowIdLifetime.ROWID_UNSUPPORTED;
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		var keys = new ArrayList<String>();
		keys.add(COL_TABLE_SCHEM);
		keys.add(COL_TABLE_CATALOG);

		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys,
				Collections.singletonList(new Value[] { Values.value("public"), Values.value(getSingleCatalog()) }));
		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);

		var thePattern = Objects.requireNonNullElse(schemaPattern, "public").trim().replace("%", ".*");
		if (thePattern.isEmpty() || "public".matches("(?i)" + thePattern)) {
			return getSchemas();
		}

		// return an empty result set if anything other than public is asked for.
		var keys = new ArrayList<String>();
		keys.add(COL_TABLE_SCHEM);
		keys.add(COL_TABLE_CATALOG);
		// return RS with just public in it
		PullResponse pullResponse = createEmptyPullResponse();

		var runResponse = createRunResponseForStaticKeys(keys);

		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() {
		return true;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() {
		return true;
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {

		var keys = List.of("NAME", "MAX_LEN", "DEFAULT_VALUE", "DESCRIPTION");
		var values = new ArrayList<Value[]>();
		for (var property : SUPPORTED_CLIENT_INFO_PROPERTIES) {
			values.add(new Value[] { Values.value(property.name()), Values.value(65536), Values.NULL,
					Values.value(property.description()) });
		}
		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys, values);

		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	static boolean isSupportedClientInfoProperty(String name) {
		return SUPPORTED_CLIENT_INFO_PROPERTIES.stream().anyMatch(p -> p.name().equals(name));
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
			throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		return doQueryForResultSet(getRequest("getFunctions", "name", functionNamePattern, "functionType",
				DatabaseMetaData.functionResultUnknown, "catalogAsParameterWorkaround", getSingleCatalog()));
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var intermediateResults = getArgumentDescriptions("FUNCTIONS", functionNamePattern);
		return doQueryForResultSet(getRequest("getFunctionColumns", "results", intermediateResults, "columnNamePattern",
				columnNamePattern, "columnType", DatabaseMetaData.functionColumnIn, "nullable",
				DatabaseMetaData.functionNullableUnknown, "catalogAsParameterWorkaround", getSingleCatalog()));
	}

	private List<Map<String, Object>> getArgumentDescriptions(String category, String namePattern) throws SQLException {

		// Tja.
		// SHOW procedures and SHOW functions are massively not composable, and so they
		// don't fly with UNWIND and WITH
		// The second query is just more maintainable than having yet another dance for
		// creating correct, fake pull responses

		List<Map<String, Object>> intermediateResults = new ArrayList<>();
		var request = getRequest("getArgumentDescriptions", "name", namePattern);
		request = new Request(request.query.formatted(category), request.args);
		try (var rs = doQueryForResultSet(request)) {
			while (rs.next()) {
				intermediateResults.add(Map.of("name", rs.getString("name"), "description", rs.getString("description"),
						"argumentDescriptions", rs.getObject("argumentDescription")));
			}
		}
		return intermediateResults;
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {

		assertSchemaIsPublicOrNull(schemaPattern);
		assertCatalogIsNullOrEmpty(catalog);

		// TODO the generated v$id columns should really move her
		var keys = List.of(COL_TABLE_CAT, COL_TABLE_SCHEM, COL_TABLE_NAME, COL_COLUMN_NAME, COL_DATA_TYPE,
				COL_COLUMN_SIZE, COL_DECIMAL_DIGITS, COL_NUM_PREC_RADIX, COL_COLUMN_USAGE, COL_REMARKS,
				COL_CHAR_OCTET_LENGTH, COL_IS_NULLABLE);
		return emptyResultSet(keys);
	}

	private ResultSet emptyResultSet(List<String> keys) throws SQLException {
		var runResponse = createRunResponseForStaticKeys(keys);
		var pullResponse = staticPullResponseFor(keys, List.of());
		return new LocalStatementImpl(this.connection, runResponse, pullResponse).getResultSet();
	}

	@Override
	public boolean generatedKeyAlwaysReturned() {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

	private static RunResponse createRunResponseForStaticKeys(List<String> keys) {
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

	private static PullResponse createEmptyPullResponse() {
		return new PullResponse() {
			@Override
			public List<Record> records() {
				return Collections.emptyList();
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
	}

	private static void assertSchemaIsPublicOrNull(String schemaPattern) throws SQLException {
		if (schemaPattern != null && !"public".equalsIgnoreCase(schemaPattern)) {
			throw new Neo4jException(withReason("Schema must be public or null (was '%s')".formatted(schemaPattern)));
		}
	}

	private void assertCatalogIsNullOrEmpty(String catalog) throws SQLException {
		if (catalog != null && !(catalog.isBlank() || catalog.trim().equalsIgnoreCase(getSingleCatalog()))) {
			throw new Neo4jException(withReason(
					"Catalog '%s' is not available in this Neo4j instance, please leave blank or specify the current database name"
						.formatted(catalog)));
		}
	}

	/**
	 * To have a central point to get the single supported catalog.
	 * @return the single supported catalog for this connection
	 */
	private String getSingleCatalog() throws SQLException {
		return this.connection.unwrap(Neo4jConnection.class).getDatabaseName();
	}

	private PullResponse doQueryForPullResponse(Request request) throws SQLException {
		var response = doQuery(request);
		return response.pullResponse;
	}

	private ResultSet doQueryForResultSet(Request request) throws SQLException {
		var response = doQuery(request);

		return new LocalStatementImpl(this.connection, response.runFuture.join(), response.pullResponse).getResultSet();
	}

	private QueryAndRunResponse doQuery(Request request) throws SQLException {
		var transaction = this.connection.unwrap(ConnectionImpl.class).newMetadataTransaction(Map.of());
		var responses = transaction.runAndPull(request.query, request.args, -1, 0);
		transaction.commit();
		return new QueryAndRunResponse(responses.pullResponse(),
				CompletableFuture.completedFuture(responses.runResponse()));
	}

	@Override
	public DatabaseMetaData flush() {
		this.tablesCache.clear();
		return this;
	}

	private record Request(String query, Map<String, Object> args) {

	}

	private record QueryAndRunResponse(PullResponse pullResponse, CompletableFuture<RunResponse> runFuture) {
	}

	private record UniqueConstraint(String name, List<String> labelsOrTypes, List<String> properties) {
	}

	private record ClientInfoProperty(String name, String description) {
	}

	private record GetTablesCacheKey(String catalog, String schemaPattern, String tableNamePattern, String[] types) {
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof GetTablesCacheKey that)) {
				return false;
			}
			return Objects.equals(this.catalog, that.catalog) && Objects.deepEquals(this.types, that.types)
					&& Objects.equals(this.schemaPattern, that.schemaPattern)
					&& Objects.equals(this.tableNamePattern, that.tableNamePattern);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.catalog, this.schemaPattern, this.tableNamePattern, Arrays.hashCode(this.types));
		}
	}

	private record GetTablesCacheValue(RunResponse runResponse, PullResponse pullResponse) {

		static GetTablesCacheValue of(ResultSet resultSet) throws SQLException {
			var keys = new ArrayList<String>();
			var metaData = resultSet.getMetaData();
			var columnCount = metaData.getColumnCount();
			for (int i = 1; i <= columnCount; ++i) {
				keys.add(metaData.getColumnName(i));
			}
			var values = new ArrayList<Value[]>();
			while (resultSet.next()) {
				var row = new Value[columnCount];
				for (int i = 1; i <= columnCount; ++i) {
					row[i - 1] = resultSet.getObject(i, Value.class);
				}
				values.add(row);
			}
			var response = createRunResponseForStaticKeys(keys);
			var pull = staticPullResponseFor(keys, values);
			return new GetTablesCacheValue(response, pull);
		}
	}

}
