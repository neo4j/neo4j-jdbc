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
package org.neo4j.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.internal.bolt.response.PullResponse;
import org.neo4j.jdbc.internal.bolt.response.RunResponse;
import org.neo4j.jdbc.values.Neo4jTypeToSqlTypeMapper;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.StringValue;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ResultSetMetaDataImplTests {

	private static final int INDEX = 1;

	private static final String LABEL = "label";

	@Test
	void testResultSetMetadataColumnCount() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value(2)))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnCount()).isEqualTo(1);
		}
	}

	@Test
	void testResultSetMetadataGetSchema() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value(2)))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getSchemaName(1)).isEqualTo("public");
		}
	}

	@Test
	void testResultSetMetadataGetCatalog() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value(2)))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getCatalogName(1)).isEqualTo("");
		}
	}

	@Test
	void testResultSetMetadataGetColumnName() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value(2)))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnName(1)).isEqualTo("label");
		}
	}

	@Test
	void testResultSetMetadataGetColumnLabel() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value("String")))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnLabel(1)).isEqualTo("label");
		}
	}

	@Test
	void testResultSetMetadataGetInteger() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value(2)))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnType(1))
				.isEqualTo(Neo4jTypeToSqlTypeMapper.toSqlType(Type.INTEGER));
		}
	}

	@Test
	void testResultSetMetadataGetString() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value("String")))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnType(1))
				.isEqualTo(Neo4jTypeToSqlTypeMapper.toSqlType(Type.STRING));
		}
	}

	@Test
	void testResultSetMetadataGetNull() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.NULL))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnType(1)).isEqualTo(Neo4jTypeToSqlTypeMapper.toSqlType(Type.NULL));
		}
	}

	@Test
	void testResultSetMetadataGetStringClassName() throws SQLException {
		try (var resultSet = setupWithValues(Collections.singletonList(Values.value("String")))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnClassName(1)).isEqualTo(StringValue.class.getName());
		}
	}

	@Test
	void testResultSetMetadataReturnsNullForTypeIfTheFirstRecordIsNull() throws SQLException {
		// This is a side effect of having the type be derived from first record. If we
		// decide to change this
		// we should update this test.

		try (var resultSet = setupWithValues(List.of(Values.NULL, Values.value("String")))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnType(1)).isEqualTo(Neo4jTypeToSqlTypeMapper.toSqlType(Type.NULL));
		}
	}

	@Test
	void testResultSetMetadataReturnsTheTypeOfTheFirstRecordIfTheFirstRecordTypeIsDifferent() throws SQLException {
		// This is a side effect of having the type be derived from first record. If we
		// decide to change this
		// we should update this test.

		try (var resultSet = setupWithValues(List.of(Values.value(1), Values.value("String")))) {
			var rsMetadata = resultSet.getMetaData();
			Assertions.assertThat(rsMetadata.getColumnType(1))
				.isEqualTo(Neo4jTypeToSqlTypeMapper.toSqlType(Type.INTEGER));
		}
	}

	private ResultSet setupWithValues(List<Value> expectedValue) {
		var statement = mock(StatementImpl.class);
		var runResponse = mock(RunResponse.class);

		List<Record> boltRecords = new ArrayList<>();

		for (Value value : expectedValue) {
			var boltRecord = mock(Record.class);
			given(boltRecord.size()).willReturn(1);
			given(boltRecord.get(INDEX - 1)).willReturn(value);
			given(boltRecord.containsKey(LABEL)).willReturn(true);
			given(boltRecord.get(LABEL)).willReturn(value);
			given(boltRecord.keys()).willReturn(List.of(LABEL));
			boltRecords.add(boltRecord);
		}

		var pullResponse = mock(PullResponse.class);
		given(pullResponse.records()).willReturn(boltRecords);

		return new ResultSetImpl(statement, mock(Neo4jTransaction.class), runResponse, pullResponse, 1000, 0, 0);
	}

}
