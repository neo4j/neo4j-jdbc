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
package org.neo4j.jdbc.it.cp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.MountableFile;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

	// used by several parameterized test classes
	@SuppressWarnings("unused")
	protected final Stream<Arguments> allProtocols() {
		var result = Stream.<Arguments>builder();
		result.add(Arguments.of("neo4j"));
		var neo4jImage = System.getProperty("neo4j-jdbc.default-neo4j-image");
		if (neo4jImage == null || neo4jImage.startsWith("neo4j:20")) {
			result.add(Arguments.of("http"));
		}
		return result.build();
	}

	IntegrationTestBase() {
		this.neo4j = TestUtils.getNeo4jContainer();
	}

	IntegrationTestBase(String neo4jContainerName) {
		this(neo4jContainerName, false, false);
	}

	IntegrationTestBase(String neo4jContainerName, boolean enableApoc, boolean forceEnterprise) {
		this.neo4j = TestUtils.getNeo4jContainer(neo4jContainerName, enableApoc, forceEnterprise);
	}

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer neo4j;

	protected boolean doClean = true;

	/**
	 * Absolute classpath resources to copy over into the
	 * <code>/var/lib/neo4j/import</code> folder inside the container.
	 */
	protected List<String> resources = new ArrayList<>();

	@BeforeAll
	void startNeo4j() throws SQLException {
		this.neo4j.start();
		for (var resource : this.resources) {
			this.neo4j.copyFileToContainer(MountableFile.forClasspathResource(resource),
					"/var/lib/neo4j/import/%s".formatted(resource.substring(resource.lastIndexOf("/") + 1)));
		}

		try (var connection = getConnection(false, false); var stmt = connection.createStatement()) {
			var resultSet = stmt.executeQuery("CALL dbms.components() YIELD edition");
			resultSet.next();
			var edition = resultSet.getString("edition");
			resultSet.close();
			if ("enterprise".equalsIgnoreCase(edition)) {
				stmt.execute("CREATE DATABASE rodb IF NOT EXISTS WAIT");
				stmt.execute("ALTER DATABASE rodb SET ACCESS READ ONLY");
			}
		}
	}

	@BeforeEach
	void clearDatabase() throws SQLException {
		if (!this.doClean) {
			return;
		}
		try (var connection = this.getConnection(); var stmt = connection.createStatement()) {
			stmt.execute("""
					MATCH (n)
					CALL {
						WITH n DETACH DELETE n
					}
					IN TRANSACTIONS OF 1000 ROWs""");
		}
	}

	final Connection getConnection() throws SQLException {
		return getConnection(false, false);
	}

	final Connection getConnection(boolean translate, boolean rewriteBatchedStatements, String... additionalProperties)
			throws SQLException {
		var url = getConnectionURL();
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		properties.put("rewriteBatchedStatements", Boolean.toString(rewriteBatchedStatements));
		if (translate) {
			properties.put("enableSQLTranslation", "true");
			properties.put("s2c.alwaysEscapeNames", "false");
			properties.put("s2c.prettyPrint", "false");
		}

		if (additionalProperties.length > 0 && additionalProperties.length % 2 == 0) {
			for (int i = 0; i < additionalProperties.length; i += 2) {
				properties.put(additionalProperties[0], additionalProperties[1]);
			}
		}
		return DriverManager.getConnection(url, properties);
	}

	String getConnectionURL() {
		return "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
	}

}
