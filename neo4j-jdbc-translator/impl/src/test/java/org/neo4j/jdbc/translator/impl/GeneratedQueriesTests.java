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
package org.neo4j.jdbc.translator.impl;

import org.jooq.SQLDialect;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.translator.spi.Translator;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedQueriesTests {

	@Nested
	class ByTableau {

		private static final Translator TABLEAU_POSTGRES = SqlToCypher.with(SqlToCypherConfig.builder()
			.withSqlDialect(SQLDialect.POSTGRES)
			.withPrettyPrint(false)
			.withAlwaysEscapeNames(false)
			.build());

		private static final Translator TABLE_DEFAULT = SqlToCypher
			.with(SqlToCypherConfig.builder().withPrettyPrint(false).withAlwaysEscapeNames(false).build());

		@Test
		void minMax() {
			var q = """
					SELECT MIN("Movie"."released") AS "lower:released",
						MAX("Movie"."released") AS "upper:released"
					FROM "public"."Movie" "Movie"
					""";

			assertThat(TABLEAU_POSTGRES.translate(q)).isEqualTo(
					"MATCH (movie:Movie) RETURN min(movie.released) AS `lower:released`, max(movie.released) AS `upper:released`");
		}

		@Test
		void containsV1() {

			var q = """
					SELECT "Movie"."released" AS "released",
						"Movie"."tagline" AS "tagline",
						"Movie"."title" AS "title"
					FROM "public"."Movie" "Movie"
					WHERE (STRPOS(CAST(LOWER(CAST("Movie"."title" AS TEXT)) AS TEXT),CAST('matrix' AS TEXT)) > 0)
					LIMIT 100
					""";
			assertThat(TABLEAU_POSTGRES.translate(q)).isEqualTo(
					"MATCH (movie:Movie) WHERE apoc.text.indexOf(toString(toLower(toString(movie.title))), toString('matrix')) > 0 RETURN movie.released AS released, movie.tagline AS tagline, movie.title AS title LIMIT 100");
		}

		@Test
		void containsV2() {

			var q = """
					SELECT TOP 100 `Movie`.`released` AS `released`,
						`Movie`.`tagline` AS `tagline`,
						`Movie`.`title` AS `title`
					FROM `public`.`Movie` `Movie`
					WHERE (POSITION('matrix' IN LOWER(`Movie`.`title`)) > 0)
					""";

			assertThat(TABLE_DEFAULT.translate(q)).isEqualTo(
					"MATCH (movie:Movie) WHERE apoc.text.indexOf(toLower(movie.title), 'matrix') > 0 RETURN movie.released AS released, movie.tagline AS tagline, movie.title AS title LIMIT 100");
		}

	}

}
