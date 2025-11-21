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
package org.neo4j.jdbc.it.sb;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.neo4j.jdbc.Neo4jDataSource;
import org.neo4j.jdbc.tracing.micrometer.Neo4jTracingBridge;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class TracingDataSourceIT extends AbstractTracing {

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:neo4j://%s:%d?enableSQLTranslation=true&s2c.precedence=20&cacheSQLTranslations=true"
					.formatted(NEO4J_CONTAINER.getHost(), NEO4J_CONTAINER.getMappedPort(7687)));
		registry.add("spring.datasource.username", () -> "neo4j");
		registry.add("spring.datasource.password", () -> NEO4J_CONTAINER.adminPassword);
	}

	@TestConfiguration
	static class TracingTestConfiguration {

		@Bean
		Tracer tracer() {
			return new SimpleTracer();
		}

		@Bean
		DataSource neo4jDataSource(Tracer tracer, DataSourceProperties dataSourceProperties) {

			var neo4jDataSource = new Neo4jDataSource();
			neo4jDataSource.setUrl(dataSourceProperties.getUrl());
			neo4jDataSource.setPassword(dataSourceProperties.getPassword());
			neo4jDataSource.setUser(dataSourceProperties.getUsername());
			neo4jDataSource.setTracer(Neo4jTracingBridge.to(tracer));

			var cfg = new HikariConfig();
			cfg.setDataSource(neo4jDataSource);
			return new HikariDataSource(cfg);
		}

	}

}
