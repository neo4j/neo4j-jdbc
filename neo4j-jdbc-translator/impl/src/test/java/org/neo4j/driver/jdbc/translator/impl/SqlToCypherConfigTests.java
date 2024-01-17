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
package org.neo4j.driver.jdbc.translator.impl;

import java.util.Map;

import org.jooq.SQLDialect;
import org.jooq.conf.ParseNameCase;
import org.jooq.conf.RenderNameCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlToCypherConfigTests {

	@Test
	void configFromNullPropertiesShouldUseDefault() {

		var config = SqlToCypherConfig.of(null);
		assertThat(config).isSameAs(SqlToCypherConfig.defaultConfig());
	}

	@Test
	void configFromEmptyPropertiesShouldUseDefault() {

		var config = SqlToCypherConfig.of(Map.of());
		assertThat(config).isSameAs(SqlToCypherConfig.defaultConfig());
	}

	@Test
	void configFromNonMatchingPropertiesShouldUseDefault() {

		var config = SqlToCypherConfig.of(Map.of("a", "b"));
		assertThat(config).isSameAs(SqlToCypherConfig.defaultConfig());
	}

	@Test
	void shouldIgnoreUnknowns() {
		var config = SqlToCypherConfig.of(Map.of("s2c.foobar", "whatever"));
		assertThat(config).isSameAs(SqlToCypherConfig.defaultConfig());
	}

	@Test
	void shouldParseKnowns() {

		var config = SqlToCypherConfig.of(Map.of("s2c.parse-name-case", ParseNameCase.LOWER_IF_UNQUOTED.name(),
				"s2c.renderNameCase", ParseNameCase.LOWER_IF_UNQUOTED.name(), "s2c.jooqDiagnosticLogging", "true",
				"s2c.sql-dialect", SQLDialect.FIREBIRD.name(), "s2c.prettyPrint", "false", "s2c.parseNamedParamPrefix",
				"foo", "s2c.tableToLabelMappings", "people:Person;movies:Movie;movie_actors:ACTED_IN",
				"s2c.joinColumnsToTypeMappings", "actor_id:ACTED_IN"));

		assertThat(config.getParseNameCase()).isEqualTo(ParseNameCase.LOWER_IF_UNQUOTED);
		assertThat(config.getRenderNameCase()).isEqualTo(RenderNameCase.LOWER_IF_UNQUOTED);
		assertThat(config.isJooqDiagnosticLogging()).isTrue();
		assertThat(config.getSqlDialect()).isEqualTo(SQLDialect.FIREBIRD);
		assertThat(config.isPrettyPrint()).isFalse();
		assertThat(config.getParseNamedParamPrefix()).isEqualTo("foo");
		assertThat(config.getTableToLabelMappings()).containsExactlyInAnyOrderEntriesOf(
				Map.of("people", "Person", "movies", "Movie", "movie_actors", "ACTED_IN"));
		assertThat(config.getJoinColumnsToTypeMappings())
			.containsExactlyInAnyOrderEntriesOf(Map.of("actor_id", "ACTED_IN"));
	}

}
