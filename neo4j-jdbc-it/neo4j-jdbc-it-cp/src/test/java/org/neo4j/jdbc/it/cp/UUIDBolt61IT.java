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
import java.util.UUID;

import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.neo4j.Neo4jContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * For now only runs in case a local neo4j:boltv61 is available (i.e. the nightly tagged
 * as such)
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIf(value = "neo4jGreaterThanOrEqual202606Available",
		disabledReason = "Neo4j 2026.06 or higher is not available")
class UUIDBolt61IT {

	protected final Neo4jContainer neo4j;

	protected static final String IMAGE_NAME = "neo4j:boltv61";

	// Client must not be closed, otherwise the factory will yell at you:
	// "You should never close the global DockerClient!"
	@SuppressWarnings("resource")
	static boolean neo4jGreaterThanOrEqual202606Available() {
		var dockerClient = DockerClientFactory.lazyClient();
		try {
			dockerClient.inspectImageCmd(IMAGE_NAME).exec();
		}
		catch (NotFoundException ex) {
			return false;
		}
		return true;
	}

	UUIDBolt61IT() {
		this.neo4j = TestUtils.getNeo4jContainer(IMAGE_NAME, false, false)
			.withNeo4jConfig("internal.dbms.bolt.max_protocol_version", "6.1")
			.withNeo4jConfig("db.query.default_language", "CYPHER_25");
		this.neo4j.start();
	}

	@AfterAll
	void stopNeo4j() {
		this.neo4j.stop();
	}

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(
				"jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687)), "neo4j",
				this.neo4j.getAdminPassword());
	}

	@Test
	final void shouldTransportNativeUuid() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.prepareStatement("""
				CYPHER 25
				RETURN $1 AS value""")) {

			var sent = UUID.randomUUID();
			stmt.setObject(1, sent);

			var rs = stmt.executeQuery();
			assertThat(rs.next()).isTrue();

			var received = rs.getObject(1);
			assertThat(received).isEqualTo(sent);

			received = rs.getObject(1, UUID.class);
			assertThat(received).isEqualTo(sent);

			received = UUID.fromString(rs.getString(1));
			assertThat(received).isEqualTo(sent);
		}

	}

	@Test
	final void shouldTranslateStringUuid() throws SQLException {
		try (var connection = getConnection(); var stmt = connection.prepareStatement("""
				CYPHER 25
				RETURN $1 AS value""")) {

			var sent = UUID.randomUUID();

			stmt.setString(1, sent.toString());

			var rs = stmt.executeQuery();
			assertThat(rs.next()).isTrue();

			var received = UUID.fromString(rs.getString(1));
			assertThat(received).isEqualTo(sent);

			received = rs.getObject(1, UUID.class);
			assertThat(received).isEqualTo(sent);
		}

	}

}
