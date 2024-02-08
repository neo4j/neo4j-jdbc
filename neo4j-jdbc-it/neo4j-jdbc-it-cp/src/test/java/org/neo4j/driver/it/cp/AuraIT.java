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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperties;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

class AuraIT {

	@Test
	@EnabledIfSystemProperties({ @EnabledIfSystemProperty(named = "neo4j-jdbc.aura-host", matches = ".+"),
			@EnabledIfSystemProperty(named = "neo4j-jdbc.aura-password", matches = ".+") })
	void shouldExecuteQuery() throws SQLException {
		var host = System.getProperty("neo4j-jdbc.aura-host");
		var url = String.format("jdbc:neo4j+s://%s:7687", host);
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		var password = System.getProperty("neo4j-jdbc.aura-password");
		properties.put("password", password);
		try (var connection = driver.connect(url, properties);
				var statement = connection.prepareStatement("RETURN $1")) {
			var message = "Hello Aura";
			statement.setString(1, message);
			var resultSet = statement.executeQuery();
			assertThat(resultSet.next()).isTrue();
			assertThat(resultSet.getString(1)).isEqualTo(message);
			assertThat(resultSet.next()).isFalse();
		}
	}

}
