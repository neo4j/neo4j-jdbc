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
package org.neo4j.driver.it.sb;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(includeFilters = @ComponentScan.Filter(Repository.class))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(Neo4jTestConfig.class)
class MoviesTests {

	@BeforeAll
	static void createData(@Autowired Neo4jHttpClient httpClient) {
		httpClient.executeQuery("MATCH (n) DETACH DELETE n", Map.of());
		httpClient.executeQuery("MERGE (m:Movie {title: $title}) RETURN m", Map.of("title", "Barbieheimer"));
	}

	@Test
	void repositoryIsConnectedAndUsable(@Autowired MovieRepository movieRepository) {
		assertThat(movieRepository.findAll()).hasSizeGreaterThanOrEqualTo(1)
			.anyMatch(movie -> movie.title().equals("Barbieheimer"));
	}

	@Test
	void shouldCreateNewMovie(@Autowired MovieRepository movieRepository) {
		// 3 for node, label and property
		assertThat(movieRepository.createOrUpdate(new Movie(UUID.randomUUID().toString()))).isEqualTo(3);
	}

	@Test
	void shouldNotFailOnMerge(@Autowired MovieRepository movieRepository) {
		assertThat(movieRepository.createOrUpdate(new Movie("00 Schneider – Jagd auf Nihil Baxter"))).isEqualTo(3);
		assertThat(movieRepository.createOrUpdate(new Movie("00 Schneider – Jagd auf Nihil Baxter"))).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	@AfterAll
	static void makeSureEverythingHasBeenRolledBack(@Autowired Neo4jHttpClient httpClient) {
		var result = httpClient.executeQuery("MATCH (n:Movie) RETURN n", Map.of());

		var results = (List<Map<String, Object>>) result.get("results");
		assertThat(results).hasSize(1);
		var data = (List<Map<String, Object>>) results.get(0).get("data");
		assertThat(data).hasSize(1);
	}

}
