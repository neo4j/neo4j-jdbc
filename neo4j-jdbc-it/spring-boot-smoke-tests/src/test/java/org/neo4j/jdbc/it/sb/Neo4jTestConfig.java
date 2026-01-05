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
package org.neo4j.jdbc.it.sb;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class Neo4jTestConfig {

	@Bean
	@ServiceConnection
	Neo4jJdbcContainer<?> neo4jContainer() {
		return new Neo4jJdbcContainer<>(System.getProperty("neo4j-jdbc.default-neo4j-image")).withReuse(true);
	}

	@Bean
	Neo4jHttpClient neo4jHttpClient(ObjectMapper objectMapper, Neo4jJdbcContainer<?> neo4jJdbcContainer) {
		return new Neo4jHttpClient(objectMapper,
				"http://%s:%d".formatted(neo4jJdbcContainer.getHost(), neo4jJdbcContainer.getMappedPort(7474)),
				neo4jJdbcContainer.adminPassword);
	}

	static class Neo4jJdbcContainer<S extends Neo4jJdbcContainer<S>> extends JdbcDatabaseContainer<S> {

		private static final WaitStrategy WAIT_FOR_BOLT = (new LogMessageWaitStrategy())
			.withRegEx(".*Bolt enabled on .+:7687\\.\n");

		final String adminPassword = "verysecret";

		Neo4jJdbcContainer(String dockerImageName) {
			super(DockerImageName.parse(dockerImageName));
			this.addEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
			this.waitStrategy = WAIT_FOR_BOLT;
			this.addExposedPorts(7687, 7474, 7473);
		}

		protected void configure() {
			this.addEnv("NEO4J_AUTH", "neo4j/%s".formatted(this.adminPassword));
		}

		@Override
		public String getDriverClassName() {
			return "org.neo4j.jdbc.Neo4jDriver";
		}

		@Override
		public String getJdbcUrl() {
			return "jdbc:neo4j://%s:%d?enableSQLTranslation=true&s2c.precedence=20&cacheSQLTranslations=true"
				.formatted(this.getHost(), this.getMappedPort(7687));
		}

		@Override
		public String getUsername() {
			return "neo4j";
		}

		@Override
		public String getPassword() {
			return this.adminPassword;
		}

		@Override
		protected String getTestQueryString() {
			return "/*+ NEO4J FORCE_CYPHER */ RETURN 1";
		}

	}

}
