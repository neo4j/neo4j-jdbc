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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(Neo4jTestConfig.class)
public class ApplicationIT {

	@BeforeAll
	static void createData(@Autowired Neo4jHttpClient httpClient) {
		httpClient.executeQuery("MATCH (n) DETACH DELETE n", Map.of());
	}

	@Test
	void movieControllerShouldWork(@Autowired TestRestTemplate restTemplate) {

		var listOfMoviesType = new ParameterizedTypeReference<List<Movie>>() {
		};

		var moviesResponse = restTemplate.exchange(RequestEntity.get("/movies").build(), listOfMoviesType);
		assertThat(moviesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(moviesResponse.getBody()).isEmpty();

		var title = "Alita: Battle Angel";
		var movieCreateOrUpdatedResponse = restTemplate
			.exchange(RequestEntity.post("/movies").body(title, String.class), Movie.class);
		assertThat(movieCreateOrUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		movieCreateOrUpdatedResponse = restTemplate.exchange(RequestEntity.post("/movies").body(title, String.class),
				Movie.class);
		assertThat(movieCreateOrUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		moviesResponse = restTemplate.exchange(RequestEntity.get("/movies").build(), listOfMoviesType);
		assertThat(moviesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Assertions.assertThat(moviesResponse.getBody()).containsExactly(new Movie(title));
	}

}
