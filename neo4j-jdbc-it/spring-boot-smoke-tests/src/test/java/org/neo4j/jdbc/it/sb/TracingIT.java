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

import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;
import io.micrometer.tracing.test.simple.TracerAssert;
import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.Neo4jDataSource;
import org.neo4j.jdbc.tracing.micrometer.Neo4jTracingBridge;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
public class TracingIT {

	@Container
	private static final SimpleNeo4jContainer<?> neo4jContainer = new SimpleNeo4jContainer<>(
			System.getProperty("neo4j-jdbc.default-neo4j-image"))
		.withReuse(true);

	@DynamicPropertySource
	static void postgresqlProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url",
				() -> "jdbc:neo4j://%s:%d?enableSQLTranslation=true&s2c.precedence=20&cacheSQLTranslations=true"
					.formatted(neo4jContainer.getHost(), neo4jContainer.getMappedPort(7687)));
		registry.add("spring.datasource.username", () -> "neo4j");
		registry.add("spring.datasource.password", () -> neo4jContainer.adminPassword);

	}

	@Test
	void movieControllerShouldWork(@Autowired TestRestTemplate restTemplate, @Autowired Tracer tracer) {

		restTemplate.delete("/movies");
		TracerAssert.assertThat((SimpleTracer) tracer)
			.reportedSpans()
			.anyMatch(span -> "neo4j.jdbc execute".equals(span.getName())
					&& "DELETE FROM Movie".equals(span.getTags().get("db.query.text")));

		var listOfMoviesType = new ParameterizedTypeReference<List<Movie>>() {
		};

		for (int i = 0; i < 5; ++i) {
			var title = "Movie " + i;
			var movieCreateOrUpdatedResponse = restTemplate
				.exchange(RequestEntity.post("/movies").body(title, String.class), Movie.class);
			assertThat(movieCreateOrUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		}
		TracerAssert.assertThat((SimpleTracer) tracer)
			.reportedSpans()
			.filteredOn(span -> "neo4j.jdbc executeUpdate".equals(span.getName())
					&& "INSERT INTO Movie(title) VALUES(?) ON DUPLICATE KEY IGNORE"
						.equals(span.getTags().get("db.query.text")))
			.hasSize(5);

		var moviesResponse = restTemplate.exchange(RequestEntity.get("/movies").build(), listOfMoviesType);
		assertThat(moviesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(moviesResponse.getBody()).hasSize(5);

		TracerAssert.assertThat((SimpleTracer) tracer)
			.reportedSpans()
			.filteredOn(span -> "neo4j.jdbc executeQuery".equals(span.getName())
					&& "SELECT title FROM Movie m".equals(span.getTags().get("db.query.text")))
			.hasSize(1);

		TracerAssert.assertThat((SimpleTracer) tracer)
			.reportedSpans()
			.filteredOn(span -> "neo4j.jdbc iterate result".equals(span.getName())
					&& "ResultSet#next".equals(span.getTags().get("db.operation.name"))
					&& "pulledNextBatch".equals(List.copyOf(span.getEvents()).get(0).getValue()))
			.hasSize(1);
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

	static class SimpleNeo4jContainer<S extends SimpleNeo4jContainer<S>> extends GenericContainer<S> {

		private static final WaitStrategy WAIT_FOR_BOLT = (new LogMessageWaitStrategy())
			.withRegEx(String.format(".*Bolt enabled on .*:%d\\.\n", 7687));

		final String adminPassword = "verysecret";

		SimpleNeo4jContainer(String dockerImageName) {
			super(DockerImageName.parse(dockerImageName));
			this.addEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
			this.waitStrategy = WAIT_FOR_BOLT;
			this.addExposedPorts(7687);
		}

		protected void configure() {
			this.addEnv("NEO4J_AUTH", "neo4j/%s".formatted(this.adminPassword));
		}

	}

}
