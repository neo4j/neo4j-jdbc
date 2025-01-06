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

import java.util.Map;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@JdbcTest(includeFilters = @ComponentScan.Filter(Repository.class))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(Neo4jTestConfig.class)
class PeopleTests {

	@BeforeAll
	static void createData(@Autowired Neo4jHttpClient httpClient) {
		httpClient.executeQuery("MATCH (n) DETACH DELETE n", Map.of());
		httpClient.executeQuery("UNWIND range(1, 9) AS i WITH i CREATE(:Person {name: 'Person ' + i })", Map.of());
	}

	@Test
	void cypherPassedToSqlToCypherTranslator(@Autowired PeopleRepository people) {
		var names = IntStream.rangeClosed(1, 9).mapToObj(i -> "Person " + i).toList();
		assertThat(people.findAll()).hasSize(9).extracting(Person::name).containsExactlyElementsOf(names);
	}

	@Test
	void sqlGeneratedByFirstTranslator(@Autowired PeopleRepository people) {
		assertThat(people.findOne("Person 7").map(Person::name)).hasValue("Person 7");
	}

	@Test
	void firstTranslatorFailed(@Autowired PeopleRepository people) {
		assertThat(people.newPerson("Whatever")).matches("\\d+:[a-z0-9\\-]+:\\d+");
	}

	@Test
	void handleWarnings(@Autowired DataSource dataSource) {
		var jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setIgnoreWarnings(false);

		assertThatExceptionOfType(SQLWarningException.class)
			.isThrownBy(() -> jdbcTemplate.queryForObject("MATCH (n:Person) RETURN count(n)", Integer.class))
			.withStackTraceContaining("I'm sorry Dave, I'm afraid I can't do that");
	}

}
