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
package org.neo4j.jdbc.bundle;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CypherBackedViewsBundleIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer<?> neo4j = new Neo4jContainer<>(System.getProperty("neo4j-jdbc.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
		.withReuse(true);

	@BeforeAll
	void startNeo4j() {

		this.neo4j.start();
	}

	@Test
	void shadedJacksonJRShouldWork() throws SQLException {

		try (var connection = DriverManager.getConnection(
				"jdbc:neo4j://%s:%d?enableSQLTranslation=true&viewDefinitions=%s".formatted(this.neo4j.getHost(),
						this.neo4j.getMappedPort(7687),
						Objects.requireNonNull(this.getClass().getResource("/it-views.json")).toString()),
				"neo4j", this.neo4j.getAdminPassword()); var stmt = connection.createStatement()) {

			ResultSet labels = connection.getMetaData().getTables(null, null, "cbv1", null);
			assertThat(labels).isNotNull();
			List<String> tableNames = new ArrayList<>();

			while (labels.next()) {
				var tableName = labels.getString("TABLE_NAME");
				tableNames.add(tableName);
				if (tableName.startsWith("cbv")) {
					assertThat(labels.getString("TABLE_TYPE")).isEqualTo("CBV");
				}
			}

			assertThat(tableNames).containsExactly("cbv1");
			assertThatNoException().isThrownBy(() -> stmt.executeQuery("SELECT * FROM cbv1 WHERE a = 'The Matrix'"));
		}

	}

}
