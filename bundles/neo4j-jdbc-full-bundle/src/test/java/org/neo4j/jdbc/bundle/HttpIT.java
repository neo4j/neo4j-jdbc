/*
 * Copyright (c) 2023-2026 "Neo4j,"
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfSystemProperty(named = "disableHttpTests", matches = "true")
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j = new Neo4jContainer(System.getProperty("neo4j-jdbc.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
		.withReuse(true);

	@BeforeAll
	void startNeo4j() {

		this.neo4j.start();
	}

	Connection getBoltConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:neo4j://%s:%d/neo4j?username=neo4j&password=%s"
			.formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687), this.neo4j.getAdminPassword()));
	}

	@BeforeEach
	void createMovieGraph() throws SQLException {
		try (var connection = getBoltConnection(); var stmt = connection.createStatement();) {
			stmt.executeUpdate("CREATE (n:Movie {title: 'whatever'}) RETURN n");
		}
	}

	@Test
	void smokeTest() throws SQLException {
		int cnt = 0;
		var httpUrl = "jdbc:neo4j:%s://%s:%d/neo4j?username=neo4j&password=%s".formatted("http", this.neo4j.getHost(),
				this.neo4j.getMappedPort(7474), this.neo4j.getAdminPassword());
		try (var httpConnection = DriverManager.getConnection(httpUrl)) {
			httpConnection.setAutoCommit(false);
			var stmt = httpConnection.createStatement();
			var rs = stmt.executeQuery("MATCH (n:Movie) RETURN n.title");
			while (rs.next()) {
				++cnt;
			}
			assertThat(cnt).isGreaterThan(0);
			var stmt2 = httpConnection.prepareStatement("CREATE (n:Movie {title: $title}) RETURN n")
				.unwrap(Neo4jPreparedStatement.class);
			stmt2.setString("title", "Der frühe Vogel fängt den Wurm");
			assertThat(stmt2.executeUpdate()).isGreaterThan(0);
			httpConnection.commit();
		}

		try (var connection = getBoltConnection();
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("MATCH (n:Movie) RETURN count(n)")) {
			Assertions.assertThat(rs.next()).isTrue();
			Assertions.assertThat(rs.getInt(1)).isEqualTo(cnt + 1);
		}
	}

}
