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
package org.neo4j.jdbc.translator.sparkcleaner;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class SparkSubqueryCleaningTranslatorTests {

	static final int RANDOM_PRECEDENCE = 4711;

	@ParameterizedTest
	@CsvSource(textBlock = """
			n/a,false
			SELECT * FROM dingens,false
			SELECT * FROM SPARK_GEN_SUBQ,true
			MATCH (n) RETURN n,false,
			,false
			SELECT * FROM (MATCH (n) RETURN n) SPARK_GEN_SUBQ_0 WHERE 1=0, true
			""", nullValues = "n/a")
	void shouldDetectPossibleSparkSubqueries(String statement, boolean expectation) {
		assertThat(SparkSubqueryCleaningTranslator.mightBeASparkQuery(statement)).isEqualTo(expectation);
	}

	static Stream<Arguments> shouldBeAbleToExtractSubquery() {
		return Stream.of(Arguments.of("""
				SELECT * FROM (
				MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title
				) SPARK_GEN_SUBQ_0 WHERE 1=0
				""", """
				MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title
				"""),
				Arguments.of(
						"""
								SELECT * FROM (
								MATCH (m:Movie)<-[:ACTED_IN]-(p:Person) RETURN m.title AS title, collect(p.name) AS actors ORDER BY m.title
								) SPARK_GEN_SUBQ_0 WHERE 1=0
								""",
						"""
								MATCH (m:Movie)<-[:ACTED_IN]-(p:Person) RETURN m.title AS title, collect(p.name) AS actors ORDER BY m.title
								"""),
				Arguments.of("""
						SELECT * FROM (
							SELECT * FROM (RETURN 1) SPARK_GEN_SUBQ_1
						) SPARK_GEN_SUBQ_0 WHERE 1=0
						""", """
						SELECT * FROM (RETURN 1) SPARK_GEN_SUBQ_1
						"""), Arguments.of("""
						SELECT * FROM (
							SELECT * FROM (RETURN 1) SPARK_GEN_SUBQ_1
						)
						""", null), Arguments.of("SELECT * FROM Movie", null));
	}

	@ParameterizedTest
	@MethodSource
	void shouldBeAbleToExtractSubquery(String statement, String expectedSubquery) {
		var extracted = SparkSubqueryCleaningTranslator.extractSubquery(statement);
		if (expectedSubquery != null) {
			assertThat(extracted).hasValue(expectedSubquery.trim());
		}
		else {
			assertThat(extracted).isEmpty();
		}
	}

	static Stream<Arguments> shouldDetectParsableCypher() {
		return Stream.of(Arguments.of(
				"MATCH (m:Movie)<-[:ACTED_IN]-(p:Person) RETURN m.title AS title, collect(p.name) AS actors ORDER BY m.title",
				true), Arguments.of("INSERT INTO Movie(title, year) VALUES (?, ?)", false),
				Arguments.of("SELECT * FROM Movie", false), Arguments.of("RETURN 1", true));
	}

	@ParameterizedTest
	@MethodSource
	void shouldDetectParsableCypher(String statement, boolean expectation) {

		var translator = new SparkSubqueryCleaningTranslator(RANDOM_PRECEDENCE);
		assertThat(translator.canParseAsCypher(statement)).isEqualTo(expectation);
	}

	@Test
	void shouldWrap() {
		var translator = new SparkSubqueryCleaningTranslator(RANDOM_PRECEDENCE);

		var query = """
				SELECT * FROM (
				MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title
				) SPARK_GEN_SUBQ_0 WHERE 1=0
				""";

		assertThat(translator.translate(query)).isEqualTo("""
				/*+ NEO4J FORCE_CYPHER */
				CALL {MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
				RETURN m.title AS title, collect(p.name) AS actors
				ORDER BY m.title} RETURN * LIMIT 1
				""".strip());
	}

}
