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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Michael J. Simons
 */
class CypherBackedViewsIT extends IntegrationTestBase {

	private final Handler handler;

	private Level oldLevel;

	CypherBackedViewsIT() {
		this.handler = new ConsoleHandler();
		this.handler.setLevel(Level.FINE);
	}

	@BeforeAll
	void setup() {

		var sqlLogger = Logger.getLogger("org.neo4j.jdbc.statement.SQL");
		this.oldLevel = sqlLogger.getLevel();
		sqlLogger.setLevel(Level.FINE);
		sqlLogger.addHandler(this.handler);
	}

	@BeforeEach
	void createMovieGraph() throws SQLException, IOException {
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

	@ParameterizedTest
	@ValueSource(strings = { "SELECT * FROM cbv1 WHERE a = 'The Matrix'",
			"SELECT * FROM cbv1 WHERE a = 'The Matrix' ORDER BY a",
			"SELECT x.a, x.c1 FROM cbv1 x WHERE a = 'The Matrix' ORDER BY a",
			"SELECT x.a AS foo, x.c1 AS bar FROM cbv1 x WHERE a = 'The Matrix' ORDER BY a",
			"SELECT a, a AS title, c1 FROM cbv1 WHERE a = 'The Matrix' ORDER BY title",
			"SELECT * FROM cbv1 x WHERE x.a = 'The Matrix'",
			"SELECT * FROM (SELECT * FROM cbv1 x WHERE x.a = 'The Matrix') f",
			"SELECT count(*) FROM cbv1 WHERE a = 'The Matrix'", "SELECT count(*), a FROM cbv1 GROUP BY a",
			"SELECT * FROM cbv1 x, cbv2 y WHERE x.a = 'The Matrix' AND x.c1 = y.b",
			"SELECT * FROM cbv1 x, cbv2 y, cbv1 z WHERE x.a = 'The Matrix' AND x.c1 = y.b AND x.a = z.a" })
	void resolvingCypherBackedViews(String query) throws SQLException {

		try (var connection = getConnection(true, false, "viewDefinitions",
				Objects.requireNonNull(this.getClass().getResource("/default-views.json")).toString());
				var statement = connection.createStatement()) {
			var rs = statement.executeQuery(query);
			assertThat(rs.next()).isTrue();
			var columnCount = rs.getMetaData().getColumnCount();
			if (columnCount == 1) {
				assertThat(rs.getInt(1)).isEqualTo(5);
				assertThat(rs.next()).isFalse();
			}
			else if (rs.getMetaData().getColumnType(1) == Types.VARCHAR) {
				if (rs.getMetaData().getColumnName(1).equals("a")) {
					assertThat(rs.getString("a")).isEqualTo("The Matrix");
					assertThat(rs.getInt("c1")).isEqualTo(1999);
				}
				else {
					assertThat(rs.getString(1)).isEqualTo("The Matrix");
					assertThat(rs.getInt(2)).isEqualTo(1999);
				}
				if (columnCount == 4) {
					assertThat(rs.getBoolean("c2")).isFalse();
				}
				int cnt = 1;
				while (rs.next()) {
					++cnt;
				}
				assertThat(cnt).isEqualTo((columnCount != 6) ? 5 : 25);
			}
			else {
				assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
				int cnt = 1;
				while (rs.next()) {
					++cnt;
				}
				assertThat(cnt).isEqualTo(38);

			}
		}
	}

	@ParameterizedTest
	@CsvSource(
			textBlock = """
					SELECT a AS name, c1 AS released FROM cbv1 p JOIN cbv2 m on (m = p.ACTED_IN) ORDER BY c1 | Cypher-backed views cannot be used with a JOIN clause
					SELECT a AS name, c1 AS released FROM cbv1 p JOIN Movie m on (m = p.ACTED_IN) ORDER BY c1 | Cypher-backed views cannot be used with a JOIN clause
					SELECT a AS name, c1 AS released FROM Movie m JOIN cbv2 p on (m = p.ACTED_IN) ORDER BY c1 | Cypher-backed views cannot be used with a JOIN clause
					INSERT INTO cbv1 VALUES('a', 1234) | Cypher-backed views cannot be inserted to
					INSERT INTO cbv1 t VALUES('a', 1234) | Cypher-backed views cannot be inserted to
					DELETE FROM cbv1 | Cypher-backed views cannot be deleted from
					DELETE FROM cbv1 t | Cypher-backed views cannot be deleted from
					TRUNCATE TABLE cbv1 | Cypher-backed views cannot be deleted from
					UPDATE cbv1 SET a = 'asd' | Cypher-backed views cannot be updated
					""",
			delimiter = '|')
	void shouldRejectSomeCBVs(String query, String expectedMessage) throws SQLException {
		try (var connection = getConnection(true, false, "viewDefinitions",
				Objects.requireNonNull(this.getClass().getResource("/default-views.json")).toString());
				var statement = connection.createStatement()) {
			assertThatExceptionOfType(SQLException.class).isThrownBy(() -> statement.executeQuery(query))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withStackTraceContaining(expectedMessage);
		}
	}

	@Test
	void complexViewShouldWork() throws SQLException {
		try (var connection = getConnection(true, false, "viewDefinitions",
				Objects.requireNonNull(this.getClass().getResource("/movie_actors.json")).toString());
				var statement = connection.createStatement();
				var rs = statement.executeQuery("SELECT * FROM v_movie_actors ORDER BY title")) {
			int cnt = 0;
			while (rs.next()) {
				assertThat(rs.getString("id")).matches("\\d+:.+:\\d+");
				assertThat(rs.getString("title")).isNotNull();
				assertThat(((Object[]) rs.getArray("actors").getArray())).isNotEmpty();
				++cnt;
			}
			assertThat(cnt).isEqualTo(38);
		}
	}

	@Test
	void propertyRefShouldWork() throws SQLException {
		try (var connection = getConnection(true, false, "viewDefinitions",
				Objects.requireNonNull(this.getClass().getResource("/movie_actors.json")).toString());
				var statement = connection.createStatement();
				var rs = statement.executeQuery("SELECT * FROM v_people WHERE name LIKE 'A%' ORDER BY name")) {
			int cnt = 0;
			var names = new ArrayList<String>();
			while (rs.next()) {
				names.add(rs.getString("name"));
				assertThat(rs.getInt("id")).isGreaterThanOrEqualTo(0);
				++cnt;
			}
			assertThat(cnt).isEqualTo(6);
			assertThat(names).containsExactly("Aaron Sorkin", "Al Pacino", "Angela Scope", "Annabella Sciorra",
					"Anthony Edwards", "Audrey Tautou");
		}
	}

	@Test
	void shouldReadHttpDefinitions() throws IOException, SQLException {
		HttpServer server = null;
		int port = 0;
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			port = serverSocket.getLocalPort();
		}

		try {
			server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
			server.createContext("/views.json", exchange -> {
				var outputStream = exchange.getResponseBody();

				// language=JSON
				var htmlResponse = """
						[{"columns":[],"name":"test","query":"test"}]
						""";

				exchange.sendResponseHeaders(200, htmlResponse.length());
				outputStream.write(htmlResponse.getBytes());
				outputStream.flush();
				outputStream.close();
			});

			server.start();
			try (var connection = getConnection(true, false, "s2c.viewDefinitions",
					"http://localhost:%d/views.json".formatted(port))) {
				var meta = connection.getMetaData();
				try (var cbvs = meta.getTables(null, null, null, new String[] { "CBV" })) {
					assertThat(cbvs.next()).isTrue();
					assertThat(cbvs.getString("TABLE_NAME")).isEqualTo("test");
					assertThat(cbvs.next()).isFalse();
				}
			}
		}
		finally {
			if (server != null) {
				server.stop(0);
			}
		}
	}

}
