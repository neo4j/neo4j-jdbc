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
package org.neo4j.jdbc.it.cp;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.jooq.impl.ParserException;
import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.Neo4jPreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TranslationIT extends IntegrationTestBase {

	@Test
	void shouldLoadTranslatorDirectly() throws SQLException {

		var url = "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		properties.put("translatorFactory", "default");
		properties.put("enableSQLTranslation", "true");

		try (var con = driver.connect(url, properties);
				var stmt = con.createStatement();
				var rs = stmt.executeQuery("SELECT 1")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getInt(1)).isOne();
		}

	}

	@Test
	void shouldFailWhenLoadingInvalidClassDirectly() throws SQLException {

		var url = "jdbc:neo4j://%s:%d".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());
		properties.put("translatorFactory", "asd");
		properties.put("enableSQLTranslation", "true");

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect(url, properties))
			.withMessage("No translators available");

	}

	@Test
	void shouldTranslateAsterisk() throws SQLException {

		var title = "JDBC the Sequel";

		try (var connection = getConnection(true, false)) {
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

	@Test
	void shouldUnwrapCauseOfTranslationException() throws SQLException {

		try (var connection = getConnection(true, false);
				var stmt = connection.prepareStatement("CREATE (m:Movie {title:  $title, released: $released})")) {
			assertThatExceptionOfType(SQLException.class).isThrownBy(stmt::execute)
				.withCauseInstanceOf(ParserException.class)
				.withMessageStartingWith(
						"org.jooq.impl.ParserException: FUNCTION, GENERATOR, GLOBAL TEMPORARY TABLE, INDEX, OR ALTER, OR REPLACE, PROCEDURE, SCHEMA, SEQUENCE, TABLE, TEMPORARY TABLE, TRIGGER, TYPE, UNIQUE INDEX, or VIEW expected");
		}

	}

}
