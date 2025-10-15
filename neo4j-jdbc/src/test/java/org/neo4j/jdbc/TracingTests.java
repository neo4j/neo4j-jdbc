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
package org.neo4j.jdbc;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neo4j.bolt.connection.BoltConnection;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.tracing.Neo4jTracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TracingTests {

	@Mock
	Neo4jTracer tracer;

	@SuppressWarnings("resource")
	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
	}

	@ParameterizedTest
	@ValueSource(strings = { "jdbc:neo4j://localhost:8888", "jdbc:neo4j:http://localhost:8888" })
	void shouldSetServerDefaultTags(String url) {
		var databaseUrl = URI.create(url);
		var connection = new ConnectionImpl(databaseUrl, Authentication::none, auth -> mock(BoltConnection.class),
				List::of, false, false, false, false, new NoopBookmarkManagerImpl(), Map.of(), 0, "neo4j", null,
				List.of());

		var tracing = new Tracing(this.tracer, connection);

		var defaultTags = tracing.defaultTags();
		assertThat(defaultTags.get("server.address")).isEqualTo("localhost");
		assertThat(defaultTags.get("server.port")).isEqualTo("8888");
	}

}
