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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(Neo4jTestConfig.class)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = { "management.endpoints.jackson.isolated-json-mapper=false" })
public class ApplicationIT {

	@BeforeAll
	static void createData(@Autowired Neo4jHttpClient httpClient) {
		httpClient.executeQuery("MATCH (n) DETACH DELETE n", Map.of());
	}

	@Test
	void movieControllerShouldWork(@Autowired TestRestTemplate restTemplate) {

		var listOfMoviesType = new ParameterizedTypeReference<List<Movie>>() {
		};

		var stringMapType = new ParameterizedTypeReference<Map<String, String>>() {
		};

		var moviesResponse = restTemplate.exchange(RequestEntity.get("/movies").build(), listOfMoviesType);
		assertThat(moviesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(moviesResponse.getBody()).isEmpty();

		var title = "Alita: Battle Angel";
		var movieCreateOrUpdatedResponse = restTemplate
			.exchange(RequestEntity.post("/movies").body(title, String.class), Movie.class);
		assertThat(movieCreateOrUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		movieCreateOrUpdatedResponse = restTemplate.exchange(RequestEntity.post("/movies").body(title, String.class),
				Movie.class);
		assertThat(movieCreateOrUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		moviesResponse = restTemplate.exchange(RequestEntity.get("/movies").build(), listOfMoviesType);
		assertThat(moviesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(moviesResponse.getBody()).containsExactly(new Movie(title));

		var metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.connections",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(1);
		assertThat(metrics.getMeasurements().get(0).getValue()).isGreaterThanOrEqualTo(6.0);

		metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.cached-translations",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(1);
		assertThat(metrics.getMeasurements().get(0).getValue()).isGreaterThanOrEqualTo(2);

		metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.statements",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(1);
		assertThat(metrics.getMeasurements().get(0).getValue()).isZero();

		var response = restTemplate.exchange(RequestEntity.get("/movies?fail=true").build(), stringMapType);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.queries?tag=state:successful",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(3);
		assertThat(metrics.getMeasurements().get(0).getValue()).isEqualTo(5);

		metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.queries?tag=state:failed",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(3);
		assertThat(metrics.getMeasurements().get(0).getValue()).isEqualTo(1);

		metrics = restTemplate.getForObject("/actuator/metrics/org.neo4j.jdbc.queries",
				MetricsEndpoint.MetricDescriptor.class);
		assertThat(metrics.getMeasurements()).hasSize(3);
		assertThat(metrics.getMeasurements().get(0).getValue()).isEqualTo(6);
	}

	@TestConfiguration
	static class JacksonAdditionalConfig {

		@Bean
		JsonMapperBuilderCustomizer jacksonCustomizer() {
			return builder -> builder
				.changeDefaultVisibility(v -> v.withCreatorVisibility(JsonAutoDetect.Visibility.NON_PRIVATE));
		}

	}

}
