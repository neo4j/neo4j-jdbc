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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.neo4j.jdbc.Neo4jDriver;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

@DisabledInNativeImage
class UserAgentIT extends IntegrationTestBase {

	@Test
	void defaultUA() throws SQLException {

		var driver = new Neo4jDriver();
		try (var connection = super.getConnection()) {

			String version;
			try {
				version = "neo4j-jdbc/%d.%d.".formatted(driver.getMajorVersion(), driver.getMinorVersion());
			}
			catch (IllegalArgumentException ex) {
				version = "neo4j-jdbc/dev";
			}

			var userAgents = getUserAgents(connection);
			assertThat(userAgents).hasSize(1);
			assertThat(userAgents.get(0)).startsWith(version);
		}
	}

	@Test
	void fromEnv() throws Exception {
		var expected = "agent-wurstsalat";
		withEnvironmentVariable("NEO4J_URI",
				"jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)))
			.and("NEO4J_USERNAME", "neo4j")
			.and("NEO4J_PASSWORD", this.neo4j.getAdminPassword())
			.and("NEO4J_JDBC_USER_AGENT", expected)
			.execute(() -> {
				try (var connection = Neo4jDriver.fromEnv().orElseThrow()) {
					var userAgents = getUserAgents(connection);
					assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
				}
			});
	}

	@Test
	void fromSys() throws Exception {

		SystemLambda.restoreSystemProperties(() -> {
			var expected = "00-schneider";
			System.setProperty("NEO4J_JDBC_USER_AGENT", expected);

			try (var connection = super.getConnection()) {
				var userAgents = getUserAgents(connection);
				assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
			}

			withEnvironmentVariable("NEO4J_URI",
					"jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)))
				.and("NEO4J_USERNAME", "neo4j")
				.and("NEO4J_PASSWORD", this.neo4j.getAdminPassword())
				.and("NEO4J_JDBC_USER_AGENT", "agent-wurstsalat")
				.execute(() -> {
					try (var connection = Neo4jDriver.fromEnv().orElseThrow()) {
						var userAgents = getUserAgents(connection);
						assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
					}
				});
		});
	}

	@Test
	void fromURI() throws Exception {
		var expected = "johnny_english";
		try (var connection = super.getConnection(false, false, "agent", expected)) {
			var userAgents = getUserAgents(connection);
			assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
		}

		SystemLambda.restoreSystemProperties(() -> {
			System.setProperty("NEO4J_JDBC_USER_AGENT", "Columbo");

			try (var connection = super.getConnection(false, false, "agent", expected)) {
				var userAgents = getUserAgents(connection);
				assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
			}

			withEnvironmentVariable("NEO4J_URI",
					"jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)))
				.and("NEO4J_USERNAME", "neo4j")
				.and("NEO4J_PASSWORD", this.neo4j.getAdminPassword())
				.and("NEO4J_JDBC_USER_AGENT", "agent-wurstsalat")
				.execute(() -> {
					try (var connection = super.getConnection(false, false, "agent", expected)) {
						var userAgents = getUserAgents(connection);
						assertThat(userAgents).hasSize(1).first().isEqualTo(expected);
					}
				});
		});

	}

	List<String> getUserAgents(Connection connection) throws SQLException {
		var userAgents = new ArrayList<String>();
		try (var stmt = connection.createStatement();
				var result = stmt.executeQuery(
						"/*+ NEO4J FORCE_CYPHER */ CALL dbms.listConnections() YIELD userAgent RETURN DISTINCT userAgent")) {
			while (result.next()) {
				var userAgent = result.getString(1);
				if (!(result.wasNull() || userAgent.startsWith("neo4j-query-api"))) {
					userAgents.add(userAgent);
				}
			}
		}
		return userAgents;
	}

}
