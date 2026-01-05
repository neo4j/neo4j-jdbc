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
package org.neo4j.jdbc;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCollectorImplTests {

	@ParameterizedTest
	@CsvSource(
			textBlock = """
					jdbc:neo4j+ssc://localhost:7688?enableSQLTranslation=true&cacheSQLTranslations=true,jdbc:neo4j+ssc://localhost:7688
					jdbc:neo4j+ssc://localhost?enableSQLTranslation=true&cacheSQLTranslations=true,jdbc:neo4j+ssc://localhost:7687
					jdbc:neo4j+ssc://localhost/aDatabase?enableSQLTranslation=true&cacheSQLTranslations=true,jdbc:neo4j+ssc://localhost:7687/aDatabase
					jdbc:neo4j+ssc://localhost/aDatabase?enableSQLTranslation=true&cacheSQLTranslations=true#whatever,jdbc:neo4j+ssc://localhost:7687/aDatabase#whatever
					jdbc:neo4j+ssc://localhost/aDatabase#whatever,jdbc:neo4j+ssc://localhost:7687/aDatabase#whatever
					""")
	void urlCleanerShouldWork(URI in, URI expected) {
		assertThat(Events.cleanURL(in)).isEqualTo(expected);
	}

}
