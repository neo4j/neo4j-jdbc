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
package org.neo4j.jdbc.it.cp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CypherBackedViewsIT extends IntegrationTestBase {

	private final Handler handler;

	private Level oldLevel;

	CypherBackedViewsIT() {
		this.handler = new ConsoleHandler();
		this.handler.setLevel(Level.FINE);
	}

	@BeforeAll
	void setup() throws SQLException, IOException {

		var sqlLogger = Logger.getLogger("org.neo4j.jdbc.statement.SQL");
		this.oldLevel = sqlLogger.getLevel();
		sqlLogger.setLevel(Level.FINE);
		sqlLogger.addHandler(this.handler);

		super.clearBeforeEach = false;

		try (var connection = getConnection(false, false)) {
			TestUtils.createMovieGraph(connection);
		}
	}

	@AfterAll
	void cleanup() {
		var sqlLogger = Logger.getLogger("org.neo4j.jdbc.statement.SQL");
		sqlLogger.setLevel(Objects.requireNonNullElse(this.oldLevel, Level.INFO));
		sqlLogger.removeHandler(this.handler);

	}

	@Test
	void simpleMatchInSimpleSelect() throws SQLException {

		try (var connection = getConnection(true, false); var statement = connection.createStatement()) {
			var rs = statement.executeQuery("SELECT * FROM cbv1 WHERE a = 'The Matrix'");
			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("a")).isEqualTo("The Matrix");
			assertThat(rs.getInt("c1")).isEqualTo(1999);
			assertThat(rs.next()).isFalse();
		}
	}

}
