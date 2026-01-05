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
package org.neo4j.jdbc.translator.impl;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterNameGeneratorTests {

	@Test
	void shouldMeaningfullSequence() {

		var generator = new ParameterNameGenerator();
		var names = new ArrayList<String>();
		names.add(generator.newIndex());
		names.add(generator.newIndex("0"));
		names.add(generator.newIndex("1"));
		names.add(generator.newIndex());
		names.add(generator.newIndex());
		names.add(generator.newIndex("foobar"));
		names.add(generator.newIndex("6"));
		names.add(generator.newIndex("7"));
		names.add(generator.newIndex("1"));
		names.add(generator.newIndex());
		assertThat(names).containsExactly("1", "0", "1", "2", "3", "foobar", "6", "7", "1", "8");
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|", nullValues = "n/a",
			textBlock = """
					$   | INSERT INTO Movie (a, b, c, d) VALUES($1, $2, $a, $3) | CREATE (movie:`Movie` {a: $1, b: $2, c: $a, d: $3})
					$   | INSERT INTO Movie (a, b, c, d) VALUES($1, $2, $3, $4) | CREATE (movie:`Movie` {a: $1, b: $2, c: $3, d: $4})
					$   | INSERT INTO Movie (a, b, c, d) VALUES($1, $2, $4, $3) | CREATE (movie:`Movie` {a: $1, b: $2, c: $4, d: $3})
					$   | INSERT INTO Movie (a, b, c, d) VALUES($1, $2, ?, ?)   | CREATE (movie:`Movie` {a: $1, b: $2, c: $3, d: $4})
					$   | INSERT INTO Movie (a, b, c, d) VALUES($2, $1, ?, ?)   | CREATE (movie:`Movie` {a: $2, b: $1, c: $3, d: $4})
					n/a | INSERT INTO Movie (a, b, c, d) VALUES(:1, :2, :a, :3) | CREATE (movie:`Movie` {a: $1, b: $2, c: $a, d: $3})
					n/a | INSERT INTO Movie (a, b, c, d) VALUES(?, ?, ?, ?)     | CREATE (movie:`Movie` {a: $1, b: $2, c: $3, d: $4})
					""")
	void usingIndexLikeNamedParameters(String prefix, String sql, String cypher) {
		var translator = SqlToCypher.with(SqlToCypherConfig.builder()
			.withParseNamedParamPrefix(prefix)
			.withPrettyPrint(false)
			.withAlwaysEscapeNames(true)
			.build());
		assertThat(translator.translate(sql)).isEqualTo(cypher);
	}

}
