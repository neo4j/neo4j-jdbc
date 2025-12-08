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
package org.neo4j.jdbc.it.mybatis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.AutoConfigureMybatis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(includeFilters = @ComponentScan.Filter(Repository.class))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMybatis
@Import(Neo4jTestConfig.class)
class MoviesTests {

	private static final BigDecimal EXPECTED_LENGTH = new BigDecimal("180").add(new BigDecimal("114"))
		.divide(new BigDecimal(2), RoundingMode.HALF_EVEN);

	@BeforeAll
	static void createData(@Autowired Neo4jHttpClient httpClient) {
		httpClient.executeQuery("MATCH (n) DETACH DELETE n", Map.of());
		httpClient.executeQuery("MERGE (m:Movie {title: $title, length: $length}) RETURN m",
				Map.of("title", "Barbieheimer", "length", EXPECTED_LENGTH.toString()));
	}

	@Test
	void repositoryIsConnectedAndUsable(@Autowired MovieRepository movieRepository) {
		assertThat(movieRepository.findAll()).hasSizeGreaterThanOrEqualTo(1)
			.anyMatch(movie -> movie.title().equals("Barbieheimer") && movie.length().equals(EXPECTED_LENGTH));
	}

	@Test
	void shouldCreateNewMovie(@Autowired MovieRepository movieRepository) {
		// 3 for node, label and two properties
		assertThat(movieRepository.createOrUpdate(new Movie(UUID.randomUUID().toString(), BigDecimal.TEN)))
			.isEqualTo(4);
	}

	@Test
	void shouldNotFailOnMerge(@Autowired MovieRepository movieRepository) {
		assertThat(
				movieRepository.createOrUpdate(new Movie("00 Schneider – Jagd auf Nihil Baxter", new BigDecimal("90"))))
			.isEqualTo(4);
		assertThat(
				movieRepository.createOrUpdate(new Movie("00 Schneider – Jagd auf Nihil Baxter", new BigDecimal("90"))))
			.isEqualTo(-1);
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
