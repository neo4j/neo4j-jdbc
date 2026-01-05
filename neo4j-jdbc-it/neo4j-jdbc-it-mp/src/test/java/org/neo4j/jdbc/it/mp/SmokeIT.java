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
package org.neo4j.jdbc.it.mp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.neo4j.jdbc.Neo4jDriver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Just making sure the driver can be loaded on the module path.
 * <p>
 * Not using Testcontainers here as it is quite some pain doing it on the module path.
 */
@EnabledIf("boltPortIsReachable")
public class SmokeIT {

	@Test
	void driverShouldBeLoaded() {

		var url = "jdbc:neo4j://%s:%d".formatted(getHost(), getPort());
		Assertions.assertThatNoException().isThrownBy(() -> DriverManager.getDriver(url));
	}

	@SuppressWarnings("SqlNoDataSourceInspection")
	@Test
	void shouldConfigureConnectionToUseSqlTranslator() throws SQLException {

		try (var connection = DriverManager.getConnection(getNeo4jUrl())) {
			assertThat(connection).isNotNull();
			assertThat(connection.nativeSQL("SELECT * FROM FooBar"))
				.isEqualTo("MATCH (foobar:FooBar) RETURN elementId(foobar) AS `v$id`");
			assertThat(connection.nativeSQL("""
					SELECT * FROM (
					MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
					RETURN m.title AS title, collect(p.name) AS actors
					ORDER BY m.title
					) SPARK_GEN_SUBQ_0 WHERE 1=0
					""")).isEqualTo("""
					/*+ NEO4J FORCE_CYPHER */
					CALL {MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
					RETURN m.title AS title, collect(p.name) AS actors
					ORDER BY m.title} RETURN * LIMIT 1
					""".trim());
		}
	}

	private static String getNeo4jUrl() {
		return "jdbc:neo4j://%s:%d?user=%s&password=%s".formatted(getHost(), getPort(), "neo4j", getPassword());
	}

	@Test
	void defaultUA() throws SQLException {

		var driver = new Neo4jDriver();
		try (var connection = DriverManager.getConnection(getNeo4jUrl())) {

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

	static boolean boltPortIsReachable() {

		return new BoltHandshaker(getHost(), getPort()).isBoltPortReachable(Duration.ofSeconds(10));
	}

	private static int getPort() {
		return Integer.parseInt(System.getProperty("it-database-port", "7687"));
	}

	private static String getHost() {
		return System.getProperty("it-database-host", "localhost");
	}

	private static String getPassword() {
		return System.getProperty("it-database-password", "verysecret");
	}

	private static List<String> getUserAgents(Connection connection) throws SQLException {
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
