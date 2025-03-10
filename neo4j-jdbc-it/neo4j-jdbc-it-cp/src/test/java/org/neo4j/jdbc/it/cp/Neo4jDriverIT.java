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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.values.IsoDuration;
import org.neo4j.jdbc.values.PointValue;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Neo4jDriverIT {

	@SuppressWarnings("resource") // On purpose to reuse this
	protected final Neo4jContainer<?> neo4j = TestUtils.getNeo4jContainer();

	@BeforeAll
	void startNeo4j() {
		this.neo4j.start();
	}

	@Test
	void driverMajorVersionMustWork() {

		var driver = new Neo4jDriver();
		assertThat(driver.getMajorVersion()).isGreaterThanOrEqualTo(6);
	}

	@Test
	void driverMinorVersionMustWork() {

		var driver = new Neo4jDriver();
		assertThat(driver.getMinorVersion()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void shouldConnect() throws SQLException {
		var driver = new Neo4jDriver();

		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", this.neo4j.getAdminPassword());

		var url = "jdbc:neo4j://%s:%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687));

		var connection = driver.connect(url, properties);
		assertThat(connection).isNotNull();
		assertThat(validateConnection(connection)).isTrue();
	}

	@Test
	void shouldConfigureConnectionToUseSqlTranslator() throws SQLException {

		var url = computeUrl();

		var connection = DriverManager.getConnection(url);
		assertThat(connection).isNotNull();
		assertThat(validateConnection(connection)).isTrue();
		assertThat(connection.nativeSQL("SELECT * FROM FooBar"))
			.isEqualTo("MATCH (foobar:FooBar) RETURN elementId(foobar) AS `v$id`");

		try (var stmt = connection.createStatement();
				var rs = stmt.executeQuery(connection.nativeSQL("SELECT count(*) FROM Whatever"))) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getInt(1)).isZero();
		}
	}

	private String computeUrl() {
		return "jdbc:neo4j://%s:%s?user=%s&password=%s".formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687),
				"neo4j", this.neo4j.getAdminPassword());
	}

	@Test
	void additionalURLParametersShouldBePreserved() throws SQLException {

		var url = "jdbc:neo4j://%s:%s?user=%s&password=%s&s2c.tableToLabelMappings=genres:Genre"
			.formatted(this.neo4j.getHost(), this.neo4j.getMappedPort(7687), "neo4j", this.neo4j.getAdminPassword());

		var connection = DriverManager.getConnection(url);
		assertThat(connection).isNotNull();
		assertThat(validateConnection(connection)).isTrue();
		assertThat(connection.nativeSQL("SELECT * FROM genres"))
			.isEqualTo("MATCH (genres:Genre) RETURN elementId(genres) AS `v$id`");

		var driver = new Neo4jDriver();
		var propertyInfo = driver.getPropertyInfo(url, new Properties());
		assertThat(propertyInfo)
			.anyMatch(pi -> "s2c.tableToLabelMappings".equals(pi.name) && "genres:Genre".equals(pi.value));
	}

	@Test
	void additionalPropertiesParametersShouldBePreserved() throws SQLException {

		var url = computeUrl();

		var properties = new Properties();
		properties.put("s2c.tableToLabelMappings", "genres:Genre");

		var connection = DriverManager.getConnection(url, properties);
		assertThat(connection).isNotNull();
		assertThat(validateConnection(connection)).isTrue();
		assertThat(connection.nativeSQL("SELECT * FROM genres"))
			.isEqualTo("MATCH (genres:Genre) RETURN elementId(genres) AS `v$id`");

		var driver = new Neo4jDriver();
		var propertyInfo = driver.getPropertyInfo(url, properties);
		assertThat(propertyInfo)
			.anyMatch(pi -> "s2c.tableToLabelMappings".equals(pi.name) && "genres:Genre".equals(pi.value));
	}

	@Test
	void shouldUseBookmarksByDefault() throws SQLException {

		var url = computeUrl();
		var driver = DriverManager.getDriver(url);
		try (var connection = driver.connect(url, new Properties()); var stmt = connection.createStatement()) {
			stmt.executeQuery("RETURN 1").close();
		}
		assertThat(((Neo4jDriver) driver).getCurrentBookmarks(url)).isNotEmpty();
	}

	@Test
	void typesThatHadSpecialHandling() throws SQLException {

		var url = computeUrl();
		var driver = DriverManager.getDriver(url);
		try (var connection = driver.connect(url, new Properties());
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery(
						"RETURN point({latitude: toFloat('13.43'), longitude: toFloat('56.21')}) AS p1, duration('P14DT16H12M') AS theDuration")) {
			assertThat(rs.next()).isTrue();
			var point = rs.getObject("p1", PointValue.class).asPoint();
			assertThat(point.x()).isEqualTo(56.21);
			assertThat(point.y()).isEqualTo(13.43);
			assertThat(point.srid()).isEqualTo(4326);

			var duration = rs.getObject("theDuration", IsoDuration.class);
			assertThat(duration.days()).isEqualTo(14);
			assertThat(duration.seconds()).isEqualTo(16 * 60 * 60 + 12 * 60);
		}
		assertThat(((Neo4jDriver) driver).getCurrentBookmarks(url)).isNotEmpty();
	}

	private boolean validateConnection(Connection connection) throws SQLException {
		try (var resultSet = connection.createStatement().executeQuery("UNWIND 10 as x return x")) {
			return resultSet.next();
		}
	}

}
