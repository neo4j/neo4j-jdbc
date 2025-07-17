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
package org.neo4j.jdbc.it.cp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.bolt.connection.exception.BoltFailureException;
import org.neo4j.bolt.connection.exception.BoltGqlErrorException;
import org.neo4j.jdbc.Neo4jDatabaseMetaData;
import org.neo4j.jdbc.values.Node;
import org.neo4j.jdbc.values.Vector;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIf("databaseSupportsVectors")
abstract class AbstractVectorIT {

	protected final Neo4jContainer<?> neo4j;

	protected final boolean databaseSupportsVectors;

	AbstractVectorIT(String defaultLanguage) {
		this.neo4j = getNeo4jContainer("neo4j:2025.07-enterprise", defaultLanguage);
		var logs = new ArrayList<String>();
		this.neo4j.withLogConsumer(frame -> logs.add(frame.getUtf8String().trim()));
		this.neo4j.start();
		this.databaseSupportsVectors = logs.stream()
			.noneMatch(l -> l
				.contains("Unrecognized setting. No declared setting with name: internal.cypher.enable_vector_type"));
	}

	@AfterAll
	void stopNeo4j() {
		this.neo4j.stop();
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(
				"jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)), "neo4j",
				this.neo4j.getAdminPassword());
	}

	@SuppressWarnings("resource")
	private static Neo4jContainer<?> getNeo4jContainer(String image, String defaultLanguage) {

		var dockerImageName = Optional.ofNullable(image)
			.orElseGet(() -> System.getProperty("neo4j-jdbc.default-neo4j-image"));
		if (!dockerImageName.contains("-enterprise")) {
			dockerImageName = dockerImageName + "-enterprise";
		}
		return new Neo4jContainer<>(dockerImageName).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
			.withNeo4jConfig("server.config.strict_validation.enabled", "false")
			.withNeo4jConfig("internal.cypher.enable_extra_semantic_features", "VectorType")
			.withNeo4jConfig("internal.dbms.bolt.max_protocol_version", "6.0")
			.withNeo4jConfig("db.query.default_language", defaultLanguage)
			.withNeo4jConfig("internal.cypher.enable_vector_type", "true");
	}

	final boolean databaseSupportsVectors() {
		return this.databaseSupportsVectors;
	}

	@Test
	@EnabledIf("databaseSupportsVectors")
	final void shouldReadVector() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement(); var rs = stmt.executeQuery("""
				CYPHER 25
				RETURN
					VECTOR([10,20,30], 3, INT8) as i8,
					VECTOR([10,20,30], 3, INT16) as i16,
					VECTOR([10,20,30], 3, INT32) as i32,
					VECTOR([10,20,30], 3, INT64) as i64,
					VECTOR([1.0,2.0,3.0], 3, FLOAT32) as f32,
					VECTOR([1.0,2.0,3.0], 3, FLOAT64) as f64""")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getObject("i8", Vector.class)).isEqualTo(Vector.of(new byte[] { 10, 20, 30 }));
			assertThat(rs.getObject("i16", Vector.class)).isEqualTo(Vector.of(new short[] { 10, 20, 30 }));
			assertThat(rs.getObject("i32", Vector.class)).isEqualTo(Vector.of(new int[] { 10, 20, 30 }));
			assertThat(rs.getObject("i64", Vector.class)).isEqualTo(Vector.of(new long[] { 10, 20, 30 }));
			assertThat(rs.getObject("f32", Vector.class)).isEqualTo(Vector.of(new float[] { 1.0f, 2.0f, 3.0f }));
			assertThat(rs.getObject("f64", Vector.class)).isEqualTo(Vector.of(new double[] { 1.0, 2.0, 3.0 }));
			var metadata = rs.getMetaData();
			for (int i = 1; i <= metadata.getColumnCount(); ++i) {
				assertThat(metadata.getColumnType(i)).isEqualTo(Types.ARRAY);
				assertThat(metadata.getColumnTypeName(i)).isEqualTo("VECTOR");
			}
		}
	}

	@Test
	@EnabledIf("databaseSupportsVectors")
	final void shouldReadVectorAsArray() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement(); var rs = stmt.executeQuery("""
				CYPHER 25
				RETURN
					VECTOR([10,20,30], 3, INT8) as i8,
					VECTOR([10,20,30], 3, INT16) as i16,
					VECTOR([10,20,30], 3, INT32) as i32,
					VECTOR([10,20,30], 3, INT64) as i64,
					VECTOR([1.0,2.0,3.0], 3, FLOAT32) as f32,
					VECTOR([1.0,2.0,3.0], 3, FLOAT64) as f64""")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getArray("i8").getArray()).isEqualTo(new byte[] { 10, 20, 30 });
			assertThat(rs.getArray("i16").getArray()).isEqualTo(new short[] { 10, 20, 30 });
			assertThat(rs.getArray("i32").getArray()).isEqualTo(new int[] { 10, 20, 30 });
			assertThat(rs.getArray("i64").getArray()).isEqualTo(new long[] { 10, 20, 30 });
			assertThat(rs.getArray("f32").getArray()).isEqualTo(new float[] { 1.0f, 2.0f, 3.0f });
			assertThat(rs.getArray("f64").getArray()).isEqualTo(new double[] { 1.0, 2.0, 3.0 });
		}
	}

	@Test
	@EnabledIf("databaseSupportsVectors")
	final void shouldExtractProperErrorMessage() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {
			assertThatException().isThrownBy(() -> stmt.executeQuery("CYPHER 25 RETURN VECTOR([1], 0, INT8)"))
				.matches(ex -> {
					if (ex.getCause() instanceof BoltFailureException bfe
							&& bfe.getCause() instanceof BoltGqlErrorException bgee) {
						return bgee.gqlStatus().equals("42N31") && bgee.statusDescription()
							.contains(
									"specified number out of range. Expected 'dimension' to be NUMBER in the range 1 to");
					}
					return false;
				});
		}
	}

	@Test
	@EnabledIf("databaseSupportsVectors")
	final void shouldWriteVector() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.prepareStatement("""
				CREATE (n:VectorTest)
				SET n.i8 = ?,
					n.i16 = ?,
					n.i32 = ?,
					n.i64 = ?,
					n.f32 = ?,
					n.f64 = ?
				RETURN n,
					valueType(n.i8) AS ti8,
					valueType(n.i16) AS ti16,
					valueType(n.i32) AS ti32,
					valueType(n.i64) AS ti64,
					valueType(n.f32) AS tf32,
					valueType(n.f64) AS tf64
				""")) {
			stmt.setObject(1, Vector.of(new byte[] { 10, 20, 30 }));
			stmt.setObject(2, Vector.of(new short[] { 10, 20, 30 }));
			stmt.setObject(3, Vector.of(new int[] { 10, 20, 30 }));
			stmt.setObject(4, Vector.of(new long[] { 10, 20, 30 }));
			stmt.setObject(5, Vector.of(new float[] { 1.0f, 2.0f, 3.0f }));
			stmt.setObject(6, Vector.of(new double[] { 1.0, 2.0, 3.0 }));
			var rs = stmt.executeQuery();

			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("ti8")).isEqualTo("VECTOR<INTEGER8 NOT NULL>(3) NOT NULL");
			assertThat(rs.getString("ti16")).isEqualTo("VECTOR<INTEGER16 NOT NULL>(3) NOT NULL");
			assertThat(rs.getString("ti32")).isEqualTo("VECTOR<INTEGER32 NOT NULL>(3) NOT NULL");
			assertThat(rs.getString("ti64")).isEqualTo("VECTOR<INTEGER NOT NULL>(3) NOT NULL");
			assertThat(rs.getString("tf32")).isEqualTo("VECTOR<FLOAT32 NOT NULL>(3) NOT NULL");
			assertThat(rs.getString("tf64")).isEqualTo("VECTOR<FLOAT NOT NULL>(3) NOT NULL");

			var node = rs.getObject("n", Node.class);
			assertThat(node.get("i8").asVector()).isEqualTo(Vector.of(new byte[] { 10, 20, 30 }));
			assertThat(node.get("i16").asVector()).isEqualTo(Vector.of(new short[] { 10, 20, 30 }));
			assertThat(node.get("i32").asVector()).isEqualTo(Vector.of(new int[] { 10, 20, 30 }));
			assertThat(node.get("i64").asVector()).isEqualTo(Vector.of(new long[] { 10, 20, 30 }));
			assertThat(node.get("f32").asVector()).isEqualTo(Vector.of(new float[] { 1.0f, 2.0f, 3.0f }));
			assertThat(node.get("f64").asVector()).isEqualTo(Vector.of(new double[] { 1.0, 2.0, 3.0 }));
		}
	}

	@Test
	@EnabledIf("databaseSupportsVectors")
	final void metadataShouldWork() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.createStatement()) {

			var expected = new HashSet<>(Set.of("i8", "i16", "i32", "i64", "f32", "f64"));
			stmt.executeUpdate("""
					CYPHER 25
					CREATE (n:VectorMetadataTest {
						i8:  VECTOR([10,20,30], 3, INT8),
						i16: VECTOR([10,20,30], 3, INT16),
						i32: VECTOR([10,20,30], 3, INT32),
						i64: VECTOR([10,20,30], 3, INT64),
						f32: VECTOR([1.0,2.0,3.0], 3, FLOAT32),
						f64: VECTOR([1.0,2.0,3.0], 3, FLOAT64)})""");

			var metadata = connection.getMetaData().unwrap(Neo4jDatabaseMetaData.class);
			metadata.flush();
			var columns = metadata.getColumns(null, null, "VectorMetadataTest", null);
			while (columns.next()) {
				if (expected.remove(columns.getString("COLUMN_NAME"))) {
					assertThat(columns.getInt("DATA_TYPE")).isEqualTo(Types.ARRAY);
					assertThat(columns.getString("TYPE_NAME")).isEqualTo("VECTOR");
				}
			}
			assertThat(expected).isEmpty();
		}
	}

}
