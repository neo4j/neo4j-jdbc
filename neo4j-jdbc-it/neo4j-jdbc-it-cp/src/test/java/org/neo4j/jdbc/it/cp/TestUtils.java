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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.testcontainers.neo4j.Neo4jContainer;

final class TestUtils {

	private TestUtils() {
	}

	/**
	 * Uses the default image configured via system property
	 * {@code neo4j-jdbc.default-neo4j-image}
	 * @return a Neo4j testcontainer configured to the needs of the integration tests
	 * @see #getNeo4jContainer(String)
	 */
	static Neo4jContainer getNeo4jContainer() {
		return getNeo4jContainer(null);
	}

	/**
	 * @param image the name of the image to use
	 * {@return a Neo4j testcontainer configured
	 * to the needs of the integration tests}
	 */
	static Neo4jContainer getNeo4jContainer(String image) {
		return getNeo4jContainer(image, false, false);
	}

	/**
	 * @param enableApoc optionally enable apoc
	 * {@return a Neo4j testcontainer configured
	 * to the needs of the integration tests}
	 */
	@SuppressWarnings("resource")
	static Neo4jContainer getNeo4jContainer(String image, boolean enableApoc, boolean forceEnterprise) {
		var dockerImageName = Optional.ofNullable(image)
			.orElseGet(() -> System.getProperty("neo4j-jdbc.default-neo4j-image"));
		if (!dockerImageName.contains("-enterprise") && forceEnterprise) {
			dockerImageName = dockerImageName + "-enterprise";
		}
		var container = new Neo4jContainer(dockerImageName).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
			.waitingFor(Neo4jContainer.WAIT_FOR_BOLT) // The HTTP wait strategy used by
			// default seems not to work in
			// native image, bolt must be
			// sufficed.
			.withReuse(true);

		for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
			if (entry.getKey() instanceof String key && key.startsWith("NEO4J_")
					&& entry.getValue() instanceof String value) {
				container.withEnv(key, value);
			}
		}

		if (enableApoc) {
			container = container.withPlugins("apoc");
		}
		return container;
	}

	static Connection getConnection(Neo4jContainer neo4j) throws SQLException {
		return getConnection(neo4j, false);
	}

	static void createMovieGraph(Connection connection) throws SQLException, IOException {
		try (var stmt = connection.createStatement();
				var reader = new BufferedReader(new InputStreamReader(
						Objects.requireNonNull(TestUtils.class.getResourceAsStream("/movies.cypher"))))) {
			var sb = new StringBuilder();
			var buffer = new char[2048];
			var l = 0;
			while ((l = reader.read(buffer, 0, buffer.length)) > 0) {
				sb.append(buffer, 0, l);
			}
			var statements = sb.toString().split(";");
			for (String statement : statements) {
				stmt.execute("/*+ NEO4J FORCE_CYPHER */ " + statement);
			}
		}
	}

	static Connection getConnection(Neo4jContainer neo4j, boolean translate) throws SQLException {
		var url = "jdbc:neo4j://%s:%d".formatted(neo4j.getHost(), neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", neo4j.getAdminPassword());
		if (translate) {
			properties.put("enableSQLTranslation", "true");
			properties.put("s2c.alwaysEscapeNames", "false");
			properties.put("s2c.prettyPrint", "false");
		}
		return driver.connect(url, properties);
	}

}
