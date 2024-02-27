/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
package org.neo4j.jdbc.it.mybatis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;

public class Neo4jHttpClient {

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private final URI uri;

	private final String password;

	public Neo4jHttpClient(ObjectMapper objectMapper, String baseUrl, String password) {
		this.objectMapper = objectMapper;
		this.uri = URI.create(baseUrl + "/db/neo4j/tx/commit");
		this.password = password;
	}

	Map<String, Object> executeQuery(String statement, Map<String, String> parameters) {
		var statements = """
				{
					"statements": [
						{
							"statement": "%s",
							"parameters": %s
						}
					]
				}
				""".formatted(statement,
				parameters.entrySet()
					.stream()
					.map((e) -> "\"%s\": \"%s\"".formatted(e.getKey(), e.getValue()))
					.collect(Collectors.joining(", ", "{", "}")));
		try {
			var response = this.httpClient
				.send(HttpRequest.newBuilder(this.uri)
					.POST(HttpRequest.BodyPublishers.ofString(statements))
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Authorization", "Basic " + Base64.getEncoder()
						.encodeToString(String.format("neo4j:%s", this.password).getBytes(StandardCharsets.UTF_8)))
					.build(), HttpResponse.BodyHandlers.ofInputStream());
			TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
			};
			return this.objectMapper.readValue(response.body(), typeRef);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
	}

}
