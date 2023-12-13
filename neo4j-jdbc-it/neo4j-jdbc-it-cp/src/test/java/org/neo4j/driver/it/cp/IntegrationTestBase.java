/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.it.cp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer<?> neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {

		this.neo4j.start();
	}

	@BeforeEach
	void clearDatabase() throws SQLException {
		try (var stmt = this.getConnection().createStatement()) {
			stmt.execute("""
					MATCH (n)
					CALL {
						WITH n DETACH DELETE n
					}
					IN TRANSACTIONS OF 1000 ROWs""");
		}
	}

	final Connection getConnection() throws SQLException {
		return getConnection(false);
	}

	final Connection getConnection(boolean translate) throws SQLException {
		var url = "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		if (translate) {
			properties.put("sql2cypher", "true");
			properties.put("s2c.alwaysEscapeNames", "false");
			properties.put("s2c.prettyPrint", "false");
		}
		return driver.connect(url, properties);
	}

}
