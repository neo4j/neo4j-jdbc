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
package org.neo4j.driver.it.cp;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.jdbc.Neo4jPreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationIT extends IntegrationTestBase {

	@Test
	void shouldTranslateAsterisk() throws SQLException {

		var title = "JDBC the Sequel";

		try (var connection = getConnection(true)) {
			try (var statement = ((Neo4jPreparedStatement) connection.prepareStatement(
					"/*+ NEO4J FORCE_CYPHER */ CREATE (m:Movie {title:  $title, released: $released})"))) {
				statement.setString("title", title);
				statement.setInt("released", 2024);
				statement.execute();
			}

			try (var statement = connection.createStatement();
					var rs = statement.executeQuery("SELECT * FROM \"Movie\"")) {

				assertThat(rs.next()).isTrue();
				assertThat(rs.getString("title")).isEqualTo(title);
				assertThat(rs.getInt("released")).isEqualTo(2024);
			}
		}

	}

}
