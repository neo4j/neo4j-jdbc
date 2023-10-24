/*
 * Copyright (c) 2023 "Neo4j,"
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.jdbc.Neo4jDriver;
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
		assertThat(driver.getMajorVersion()).isOne();
	}

	@Test
	void driverMinorVersionMustWork() {

		var driver = new Neo4jDriver();
		assertThat(driver.getMinorVersion()).isZero();
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

	private boolean validateConnection(Connection connection) throws SQLException {
		var resultSet = connection.createStatement().executeQuery("UNWIND 10 as x return x");
		return resultSet.next();
	}

}
