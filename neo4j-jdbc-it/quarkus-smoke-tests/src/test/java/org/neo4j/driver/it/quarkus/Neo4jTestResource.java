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
package org.neo4j.driver.it.quarkus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import jakarta.ws.rs.core.MediaType;
import org.testcontainers.containers.Neo4jContainer;

/**
 * If we used the official Neo4j quarkus extension, we wouldn't have to do this dance.
 * However, the extension would bring in the common Neo4j Java driver, which is not what
 * we want.
 *
 * @author Michael J. Simons
 */
public final class Neo4jTestResource implements QuarkusTestResourceLifecycleManager {

	private final Neo4jContainer<?> neo4j = new Neo4jContainer<>(System.getProperty("neo4j-jdbc.default-neo4j-image"))
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withReuse(true);

	@Override
	public Map<String, String> start() {
		this.neo4j.start();

		// language=json
		var statements = """
				{
					"statements": [
						{
							"statement": "MERGE (m:Movie {title: $title}) RETURN m",
							"parameters": {
								"title": "Der frühe Vogel fängt den Wurm"
							}
						}
					]
				}
				""";

		var httpClient = HttpClient.newBuilder().build();
		try {
			httpClient.send(HttpRequest.newBuilder(URI.create(this.neo4j.getHttpUrl() + "/db/neo4j/tx/commit"))
				.POST(HttpRequest.BodyPublishers.ofString(statements))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.header("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString(("neo4j:" + this.neo4j.getAdminPassword()).getBytes(StandardCharsets.UTF_8)))
				.build(), HttpResponse.BodyHandlers.ofString());
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}

		return Map.of("quarkus.datasource.jdbc.url",
				"jdbc:neo4j://%s:%d?sql2cypher=true".formatted("localhost", this.neo4j.getMappedPort(7687)),
				"quarkus.datasource.username", "neo4j", "quarkus.datasource.password", this.neo4j.getAdminPassword());
	}

	@Override
	public void stop() {
		this.neo4j.stop();
	}

}
