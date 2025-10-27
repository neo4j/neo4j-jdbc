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

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.jdbc.Neo4jDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Neo4jDataSourceIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@Test
	void datasourceShouldProvideConnections() throws SQLException {

		var ds = new Neo4jDataSource();
		ds.setServerName(this.neo4j.getHost());
		ds.setPortNumber(this.neo4j.getMappedPort(7687));
		ds.setPassword(this.neo4j.getAdminPassword());
		ds.setConnectionProperty("enableSQLTranslation", "true");

		try (var connection = ds.getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery("SELECT 1")) {
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isOne();

		}
	}

	@Test
	void datasourceShouldProvideConnectionViaUrl() throws SQLException {

		var ds = new Neo4jDataSource();
		ds.setServerName("example.com");
		ds.setPortNumber(4711);
		ds.setPassword("falsch");
		ds.setConnectionProperty("enableSQLTranslation", "false");

		ds.setUrl("jdbc:neo4j://%s:%d?userName=%s&password=%s&enableSQLTranslation=true".formatted(this.neo4j.getHost(),
				this.neo4j.getMappedPort(7687), "neo4j", this.neo4j.getAdminPassword()));

		try (var connection = ds.getConnection();
				var stmt = connection.createStatement();
				var result = stmt.executeQuery("SELECT 1")) {
			assertThat(result.next()).isTrue();
			assertThat(result.getInt(1)).isOne();

		}
	}

	@Test
	void shouldRejectInvalidUrl() {
		var ds = new Neo4jDataSource();
		assertThatIllegalArgumentException().isThrownBy(() -> ds.setUrl("jdbc:foor"));
	}

}
