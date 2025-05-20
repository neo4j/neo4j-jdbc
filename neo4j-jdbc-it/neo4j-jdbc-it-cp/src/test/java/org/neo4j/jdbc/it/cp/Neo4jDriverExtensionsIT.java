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

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.neo4j.jdbc.Neo4jDriver;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisabledInNativeImage
class Neo4jDriverExtensionsIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer<?> neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@Test
	void fromEnvShouldWork() throws Exception {

		SystemLambda
			.withEnvironmentVariable("NEO4J_URI",
					"jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)))
			.and("NEO4J_USERNAME", "neo4j")
			.and("NEO4J_PASSWORD", this.neo4j.getAdminPassword())
			.and("NEO4J_SQL_TRANSLATION_ENABLED", "true")
			.execute(() -> {
				try (var connection = Neo4jDriver.withSQLTranslation()
					.withProperties(Map.of("rewritePlaceholders", "true"))
					.fromEnv()
					.orElseThrow()) {

					assertConnection(connection);
				}
			});

	}

	@Test
	void shouldLoadFromExplicitFile() throws Exception {

		var envFile = Files.createTempFile("neo4j-jdbc", ".txt");
		Files.write(envFile, List.of("NEO4J_USERNAME=neo4j",
				"NEO4J_URI=jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)),
				"NEO4J_PASSWORD=%s".formatted(this.neo4j.getAdminPassword()), "NEO4J_SQL_TRANSLATION_ENABLED=true"));

		try (var connection = Neo4jDriver.withSQLTranslation()
			.withProperties(Map.of("rewritePlaceholders", "true"))
			.fromEnv(envFile.getParent(), envFile.getFileName().toString())
			.orElseThrow()) {

			assertConnection(connection);
		}
	}

	private static void assertConnection(Connection connection) throws SQLException {
		var statement = connection.createStatement();
		assertThatNoException().isThrownBy(() -> statement.executeQuery("SELECT 1"));
		assertThatNoException().isThrownBy(statement::close);

		var ps = connection.prepareStatement("/*+ NEO4J FORCE_CYPHER */ RETURN ?");
		ps.setInt(1, 23);
		var rs = ps.executeQuery();
		assertThat(rs.next()).isTrue();
		assertThat(rs.getInt(1)).isEqualTo(23);
	}

}
