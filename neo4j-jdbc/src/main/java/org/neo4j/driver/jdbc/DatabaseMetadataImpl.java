/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.driver.jdbc.internal.bolt.response.ResultSummary;
import org.neo4j.driver.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.driver.jdbc.internal.bolt.value.RecordImpl;
import org.neo4j.driver.jdbc.values.Neo4jTypeToSqlTypeMapper;
import org.neo4j.driver.jdbc.values.Record;
import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.Values;

/**
 * Internal implementation for providing Neo4j specific database metadata.
 *
 * @author Michael J. Simons
 * @author Conor Watson
 * @since 1.0.0
 */
final class DatabaseMetadataImpl implements DatabaseMetaData {

	private static final List<String> NUMERIC_FUNCTIONS = List.of("abs", "ceil", "floor", "isNaN", "rand", "round",
			"sign", "e", "exp", "log", "log10", "sqrt", "acos", "asin", "atan", "atan2", "cos", "cot", "degrees",
			"haversin", "pi", "radians", "sin", "tan");

	private static final List<String> STRING_FUNCTIONS = List.of("left", "ltrim", "replace", "reverse", "right",
			"rtrim", "split", "substring", "toLower", "toString", "toStringOrNull", "toUpper", "trim");

	private static final List<String> TIME_DATE_FUNCTIONS = List.of("date", "datetime", "localdatetime", "localtime",
			"time", "duration");

	private static final Logger LOGGER = Logger.getLogger(DatabaseMetadataImpl.class.getCanonicalName());

	private final Neo4jTransactionSupplier transactionSupplier;

	private final boolean automaticSqlTranslation;

	DatabaseMetadataImpl(Neo4jTransactionSupplier transactionSupplier, boolean automaticSqlTranslation) {
		this.transactionSupplier = Objects.requireNonNull(transactionSupplier);
		this.automaticSqlTranslation = automaticSqlTranslation;
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
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
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
	public String getSystemFunctions() {
		return "";
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
	public boolean supportsMinimumSQLGrammar() throws SQLException {
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
	public String getSchemaTerm() throws SQLException {
		return "schema";
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		return "procedure";
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		return "catalog";
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
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSubqueriesInExists() {
		return true;
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
		var response = doQueryForPullResponse(
				"SHOW SETTINGS YIELD * WHERE name =~ 'server.bolt.thread_pool_max_size' RETURN toInteger(value)",
				Map.of());

		if (response.records().isEmpty()) {
			return 0;
		}
		var record = response.records().get(0);
		return record.get(0).asInt();
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
	public int getMaxStatementLength() {
		return 0;
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

		var query = """
				SHOW PROCEDURE YIELD name AS PROCEDURE_NAME, description AS REMARKS
				%s
				RETURN NULL AS PROCEDURE_CAT, "public" AS PROCEDURE_SCHEM, PROCEDURE_NAME,
				NULL AS reserved_1, NULL AS reserved_2, NULL AS reserved_3, REMARKS, "" as SPECIFIC_NAME
				""";

		Map<String, Object> procedureNameParams;

		if (procedureNamePattern == null) {
			query = query.formatted("");
			procedureNameParams = Map.of();
		}
		else {
			query = query.formatted("WHERE name = $name");
			procedureNameParams = Map.of("name", procedureNamePattern);
		}

		return doQueryForResultSet(query, procedureNameParams);

	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		var pullResponse = createEmptyPullResponse();

		var keys = new ArrayList<String>();
		keys.add("PROCEDURE_CAT");
		keys.add("PROCEDURE_SCHEM");
		keys.add("PROCEDURE_NAME");
		keys.add("COLUMN_NAME");
		keys.add("COLUMN_TYPE");
		keys.add("DATA_TYPE");
		keys.add("TYPE_NAME");
		keys.add("PRECISION");
		keys.add("LENGTH");
		keys.add("SCALE");
		keys.add("RADIX");
		keys.add("NULLABLE");
		keys.add("REMARKS");
		keys.add("COLUMN_DEF");
		keys.add("SQL_DATA_TYPE");
		keys.add("SQL_DATETIME_SUB");
		keys.add("CHAR_OCTET_LENGTH");
		keys.add("ORDINAL_POSITION");
		keys.add("IS_NULLABLE");
		keys.add("SPECIFIC_NAME");

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pullResponse, -1,
				-1, -1);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
			throws SQLException {

		assertSchemaIsPublicOrNull(schemaPattern);
		assertCatalogIsNullOrEmpty(catalog);

		if (tableNamePattern != null && !tableNamePattern.equals("%")) {
			return doQueryForResultSet("""
					CALL db.labels() YIELD label AS TABLE_NAME WHERE TABLE_NAME=$name RETURN "" as TABLE_CAT,
					"public" AS TABLE_SCHEM, TABLE_NAME, "LABEL" as TABLE_TYPE, "" as REMARKS,
					"" AS TYPE_CAT, "" AS TYPE_SCHEM, "" AS TYPE_NAME, "" AS SELF_REFERENCES_COL_NAME,
					"" AS REF_GENERATION""", Map.of("name", tableNamePattern));
		}
		else {
			return doQueryForResultSet("""
					CALL db.labels() YIELD label AS TABLE_NAME RETURN "" as TABLE_CAT,
					"public" AS TABLE_SCHEM, TABLE_NAME, "TABLE" as TABLE_TYPE, NULL as REMARKS,
					NULL AS TYPE_CAT, NULL AS TYPE_SCHEM, NULL AS TYPE_NAME, NULL AS SELF_REFERENCES_COL_NAME,
					NULL AS REF_GENERATION""", Map.of());
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
				return Optional.empty(); // might need to populate this at some point.
			}

			@Override
			public boolean hasMore() {
				return false;
			}
		};

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pull, -1, -1, -1);
	}

	/***
	 * Returns an empty Result set as there cannot be Catalogs in neo4j.
	 * @return all catalogs
	 */
	@Override
	public ResultSet getCatalogs() {
		var keys = new ArrayList<String>();
		keys.add("TABLE_CAT");

		var pull = createEmptyPullResponse();

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pull, -1, -1, -1);

	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		var keys = new ArrayList<String>();
		keys.add("TABLE_TYPE");

		var pull = new PullResponse() {
			@Override
			public List<Record> records() {
				var values = new Value[] { Values.value("TABLE") };
				return Collections.singletonList(new RecordImpl(keys, values));
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

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pull, -1, -1, -1);
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
			throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var query = "call db.schema.nodeTypeProperties() yield nodeLabels, propertyName, propertyTypes %s";

		Map<String, Object> queryParams = new HashMap<>();
		String composedWhere = "";

		if (tableNamePattern != null && !tableNamePattern.equals("%")) {
			composedWhere = "WHERE TABLE_NAME=$name";
			queryParams.put("name", tableNamePattern);
		}

		if (columnNamePattern != null && !columnNamePattern.equals("%")) {
			var columnNameWhereStatement = "COLUMN_NAME=$column_name";

			if (composedWhere.isEmpty()) {
				composedWhere = "WHERE %s".formatted(columnNameWhereStatement);
			}
			else {
				composedWhere += " AND " + columnNameWhereStatement;
				queryParams.put("column_name", columnNamePattern);
			}
		}

		query = query.formatted(composedWhere);

		var pullResponse = doQueryForPullResponse(query, queryParams);
		var records = pullResponse.records();

		var rows = new ArrayList<Value[]>();

		// now we need to flatten the table arrays and the type arrays then put it back
		// into a resultSet.

		for (Record record : records) {
			var nodeLabels = record.get(0);
			var propertyName = record.get(1);
			var propertyTypes = record.get(2);

			var propertyTypeList = propertyTypes.asList(propertyType -> propertyType);
			if (propertyTypeList.size() > 1) {
				// some kind of logging?
			}
			var propertyType = propertyTypeList.get(0);

			var nodeLabelList = nodeLabels.asList(label -> label);
			for (Value nodeLabel : nodeLabelList) {
				var values = new Value[22];
				values[0] = Values.NULL; // TABLE_CAT
				values[1] = Values.value("public"); // TABLE_SCHEM is always public
				values[2] = nodeLabel; // TABLE_NAME
				values[3] = propertyName; // COLUMN_NAME
				values[4] = Values.value(Neo4jTypeToSqlTypeMapper.toSqlType(propertyType.type())); // DATA_TYPE
				values[5] = propertyType; // TYPE_NAME
				values[6] = Values.value(-1); // COLUMN_SIZE
				values[7] = Values.NULL; // BUFFER_LENGTH
				values[8] = Values.NULL; // DECIMAL_DIGITS
				values[9] = Values.value(2); // NUM_PREC_RADIX
				values[10] = Values.value(1); // NULLABLE = true
				values[11] = Values.NULL; // REMARKS
				values[12] = Values.NULL; // COLUMN_DEF
				values[13] = Values.value(Neo4jTypeToSqlTypeMapper.toSqlType(propertyType.type())); // SQL_DATA_TYPE
				values[14] = Values.NULL; // SQL_DATETIME_SUB
				values[15] = Values.NULL; // CHAR_OCTET_LENGTH
				values[16] = Values.value(nodeLabelList.indexOf(nodeLabel)); // ORDINAL_POSITION
				values[17] = Values.value("YES"); // IS_NULLABLE
				values[18] = Values.NULL; // SCOPE_CATALOG
				values[19] = Values.NULL; // SCOPE_SCHEMA
				values[20] = Values.NULL; // SCOPE_TABLE
				values[21] = Values.NULL; // SOURCE_DATA_TYPE

				rows.add(values);
			}
		}

		var keys = getKeysForGetColumns();
		var runResponse = createRunResponseForStaticKeys(keys);

		var staticPullResponse = new PullResponse() {
			@Override
			public List<Record> records() {
				var records = new ArrayList<Record>(rows.size());

				for (Value[] values : rows) {
					records.add(new RecordImpl(keys, values));
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

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), runResponse,
				staticPullResponse, -1, -1, -1);
	}

	private static ArrayList<String> getKeysForGetColumns() {
		var keys = new ArrayList<String>();
		keys.add("TABLE_CAT");
		keys.add("TABLE_SCHEM");
		keys.add("TABLE_NAME");
		keys.add("COLUMN_NAME");
		keys.add("DATA_TYPE");
		keys.add("TYPE_NAME"); // this will be computed if possible.
		keys.add("COLUMN_SIZE");
		keys.add("BUFFER_LENGTH");
		keys.add("DECIMAL_DIGITS");
		keys.add("NUM_PREC_RADIX");
		keys.add("NULLABLE");
		keys.add("REMARKS");
		keys.add("SQL_DATA_TYPE");
		keys.add("SQL_DATETIME_SUB");
		keys.add("CHAR_OCTET_LENGTH");
		keys.add("ORDINAL_POSITION");
		keys.add("IS_NULLABLE");
		keys.add("SCOPE_CATALOG");
		keys.add("SCOPE_SCHEMA");
		keys.add("SCOPE_TABLE");
		keys.add("SOURCE_DATA_TYPE");
		return keys;
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
	public ResultSet getVersionColumns(String catalog, String schema, String table) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = new ArrayList<String>();
		keys.add("TABLE_SCHEM");
		keys.add("TABLE_CATALOG");
		keys.add("TABLE_NAME");
		keys.add("COLUMN_NAME");
		keys.add("KEY_SEQ");
		keys.add("PK_NAME");

		var emptyPullResponse = createEmptyPullResponse();
		var runResponse = createRunResponseForStaticKeys(keys);

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), runResponse,
				emptyPullResponse, -1, -1, -1);
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

	private ResultSet createKeysResultSet(ArrayList<String> keys) {
		keys.add("PKTABLE_CAT");
		keys.add("PKTABLE_SCHEM");
		keys.add("PKTABLE_NAME");
		keys.add("PKCOLUMN_NAME");
		keys.add("FKTABLE_CAT");
		keys.add("FKTABLE_SCHEM");
		keys.add("FKTABLE_NAME");
		keys.add("FKCOLUMN_NAME");
		keys.add("KEY_SEQ");
		keys.add("UPDATE_RULE");
		keys.add("DELETE_RULE");
		keys.add("FK_NAME");
		keys.add("PK_NAME");
		keys.add("DEFERRABILITY");

		var emptyPullResponse = createEmptyPullResponse();
		var runResponse = createRunResponseForStaticKeys(keys);

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), runResponse,
				emptyPullResponse, -1, -1, -1);
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
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schema);

		var keys = new ArrayList<String>();
		keys.add("TABLE_CAT");
		keys.add("TABLE_SCHEM");
		keys.add("TABLE_NAME");
		keys.add("PKCOLUMN_NAME");
		keys.add("NON_UNIQUE");
		keys.add("INDEX_QUALIFIER");
		keys.add("INDEX_NAME");
		keys.add("FKCOLUMN_NAME");
		keys.add("TYPE");
		keys.add("ORDINAL_POSITION");
		keys.add("COLUMN_NAME");
		keys.add("ASC_OR_DESC");
		keys.add("CARDINALITY");
		keys.add("PAGES");
		keys.add("FILTER_CONDITION");

		var emptyPullResponse = createEmptyPullResponse();
		var runResponse = createRunResponseForStaticKeys(keys);

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), runResponse,
				emptyPullResponse, -1, -1, -1);
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
	public boolean supportsSavepoints() {
		return false;
	}

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

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {

		assertCatalogIsNullOrEmpty(catalog);

		if (schemaPattern.equals("public")) {
			return getSchemas();
		}

		// return an empty result set if anything other than public is asked for.
		var keys = new ArrayList<String>();
		keys.add("TABLE_SCHEM");
		keys.add("TABLE_CATALOG");
		// return RS with just public in it
		PullResponse pull = createEmptyPullResponse();

		var runResponse = createRunResponseForStaticKeys(keys);

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), runResponse, pull, -1, -1,
				-1);
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
		// Do we have functions?
		var pullResponse = createEmptyPullResponse();

		var keys = new ArrayList<String>();
		keys.add("FUNCTION_CAT");
		keys.add("FUNCTION_SCHEM");
		keys.add("FUNCTION_NAME");
		keys.add("REMARKS");
		keys.add("FUNCTION_TYPE");

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pullResponse, -1,
				-1, -1);
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {
		assertCatalogIsNullOrEmpty(catalog);
		assertSchemaIsPublicOrNull(schemaPattern);

		var pullResponse = createEmptyPullResponse();

		var keys = new ArrayList<String>();
		keys.add("FUNCTION_CAT");
		keys.add("FUNCTION_SCHEM");
		keys.add("FUNCTION_NAME");
		keys.add("COLUMN_NAME");
		keys.add("COLUMN_TYPE");
		keys.add("DATA_TYPE");
		keys.add("TYPE_NAME");
		keys.add("PRECISION");
		keys.add("LENGTH");
		keys.add("SCALE");
		keys.add("RADIX");
		keys.add("NULLABLE");
		keys.add("REMARKS");
		keys.add("CHAR_OCTET_LENGTH");
		keys.add("ORDINAL_POSITION");
		keys.add("IS_NULLABLE");
		keys.add("SPECIFIC_NAME");

		var response = createRunResponseForStaticKeys(keys);
		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response, pullResponse, -1,
				-1, -1);
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		throw new UnsupportedOperationException();
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
			throw new SQLException("This object does not implement the given interface.");
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
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
		if (schemaPattern != null && !"public".equals(schemaPattern)) {
			throw new SQLException("Schema must be public or null.");
		}
	}

	private static void assertCatalogIsNullOrEmpty(String catalog) throws SQLException {
		if (catalog != null && !catalog.isEmpty()) {
			throw new SQLException("Catalog is not applicable to Neo4j please leave null.");
		}
	}

	private PullResponse doQueryForPullResponse(String query, Map<String, Object> args) throws SQLException {
		var response = doQuery(query, args);
		return response.pullResponse;
	}

	private ResultSet doQueryForResultSet(String query, Map<String, Object> args) throws SQLException {
		var response = doQuery(query, args);

		return new ResultSetImpl(new LocalStatementImpl(), new ThrowingTransactionImpl(), response.runFuture.join(),
				response.pullResponse, -1, -1, -1);
	}

	private QueryAndRunResponse doQuery(String query, Map<String, Object> args) throws SQLException {
		var transaction = this.transactionSupplier.getTransaction();
		var newTransaction = Neo4jTransaction.State.NEW.equals(transaction.getState());
		var responses = transaction.runAndPull(query, args, -1, 0);
		if (newTransaction) {
			transaction.rollback();
		}
		return new QueryAndRunResponse(responses.pullResponse(),
				CompletableFuture.completedFuture(responses.runResponse()));
	}

	private record QueryAndRunResponse(PullResponse pullResponse, CompletableFuture<RunResponse> runFuture) {
	}

}
