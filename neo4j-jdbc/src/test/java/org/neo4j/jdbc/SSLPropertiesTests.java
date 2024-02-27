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
package org.neo4j.jdbc;

import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SSLPropertiesTests {

	@Test
	void testDefaults() throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("false");
				case "sslMode" -> assertThat(info.value).isEqualTo("disable");
			}
		}
	}

	@Test
	void testSSLRequireOnly() throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();
		props.put("sslMode", "require");

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("require");
			}
		}
	}

	@Test
	void testSSLTrueOnly() throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();
		props.put("ssl", "true");

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("require");
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "require", "verify-full" })
	void testInvalidCombinationsWithFalseSSL(String sslMode) {
		var driver = new Neo4jDriver();

		Properties props = new Properties();
		props.put("sslMode", sslMode);
		props.put("ssl", "false");

		assertThatExceptionOfType(SQLException.class)
			.isThrownBy(() -> driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props));
	}

	@ParameterizedTest
	@ValueSource(strings = { "require", "verify-full", "verify_full" })
	void testDefaultsWithSSLModesToTrue(String sslMode) throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();
		props.put("sslMode", sslMode);

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo(sslMode.replace("_", "-"));
			}
		}
	}

	@Test
	void invalidSSLMode() {
		var driver = new Neo4jDriver();

		Properties props = new Properties();
		props.put("sslMode", "oops");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props))
			.withMessage("oops is not a valid option for SSLMode");
	}

	@Test
	void testSUrlParams() throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();

		var infos = driver.getPropertyInfo("jdbc:neo4j+s://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("verify-full");
			}
		}
	}

	@Test
	void testSccUrlParams() throws SQLException {
		var driver = new Neo4jDriver();

		Properties props = new Properties();

		var infos = driver.getPropertyInfo("jdbc:neo4j+ssc://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "ssl" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("require");
			}
		}
	}

}
