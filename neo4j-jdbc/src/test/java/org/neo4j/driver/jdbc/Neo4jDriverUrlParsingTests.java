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
package org.neo4j.driver.jdbc;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.jdbc.internal.bolt.AuthTokens;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnection;
import org.neo4j.driver.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class Neo4jDriverUrlParsingTests {

	private BoltConnectionProvider boltConnectionProvider;

	private static final int DEFAULT_BOLT_PORT = 7687;

	@BeforeEach
	void beforeEach() {
		this.boltConnectionProvider = mock();
		CompletionStage<BoltConnection> mockedFuture = mock();
		CompletableFuture<BoltConnection> boltConnectionCompletableFuture = mock();
		given(boltConnectionCompletableFuture.join()).willReturn(mock());
		given(mockedFuture.toCompletableFuture()).willReturn(boltConnectionCompletableFuture);
		given(this.boltConnectionProvider.connect(any(), any(), any(), any(), any(), any(), anyInt()))
			.willReturn(mockedFuture);
	}

	@ParameterizedTest
	@ValueSource(strings = { "jdbc:neo4j://host", "jdbc:neo4j://host:1000", "jdbc:neo4j://host:1000/database",
			"jdbc:neo4j://host/database", "jdbc:neo4j+s://host/database", "jdbc:neo4j+ssc://host/database" })
	void driverMustAcceptValidUrl(String url) throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);
		assertThat(driver.acceptsURL(url)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("jdbcURLProvider")
	void driverMustConnectWithValidUrl(String url, String host, int port) throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		driver.connect(url, props);
		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress(host, port)), any(), any(), any(), any(), any(), anyInt());
	}

	@Test
	void driverMustPullDatabaseOutOfUrl() throws SQLException {
		var url = "jdbc:neo4j://host/database";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		driver.connect(url, props);
		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("database"), any(), any(), any(),
					anyInt());
	}

	@Test
	void driverMustPullDatabaseOutOfUrlEvenIfSpecifiedInProperties() throws SQLException {
		var url = "jdbc:neo4j://host/database";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");
		props.put("database", "ThisShouldBeOverriden");

		driver.connect(url, props);
		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("database"), any(), any(), any(),
					anyInt());
	}

	@Test
	void driverMustUseDatabaseInPropertiesIfUrlDatabaseIsBlank() throws SQLException {
		var url = "jdbc:neo4j://host";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");
		props.put("database", "database");

		driver.connect(url, props);
		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("database"), any(), any(), any(),
					anyInt());
	}

	@Test
	void driverMustUseNeo4jIfDatabaseIsUnspecifiedInPropertiesAndUrl() throws SQLException {
		var url = "jdbc:neo4j://host";

		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		driver.connect(url, props);
		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("neo4j"), any(), any(), any(),
					anyInt());
	}

	@ParameterizedTest
	@ValueSource(strings = { "jdbc:neo4j:ThisIsWrong://host", "jdbc:neo4j+all-turns-to-crap://host" })
	void driverMustThrowIfInvalidUrlPassed(String url) {
		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect(url, props));
	}

	@Test
	void driverMustThrowIfNoUrlPassed() {
		var driver = new Neo4jDriver(this.boltConnectionProvider);
		var props = new Properties();
		props.put("username", "test");
		props.put("password", "password");

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect(null, props));
	}

	@Test
	void driverMustThrowIfNoUrlAndInfoPassed() {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect(null, null));
	}

	@Test
	void driverMustThrowIfNoInfoPassed() {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		assertThatExceptionOfType(SQLException.class).isThrownBy(() -> driver.connect("jdbc:neo4j://host", null));
	}

	@Test
	void driverMustParseUrlParamsWithJustHost() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("username", "incorrectUser");
		props.put("password", "incorrectPassword");

		driver.connect("jdbc:neo4j://host?user=correctUser&password=correctPassword", props);

		var expectedAuthToken = AuthTokens.basic("correctUser", "correctPassword");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), any(), eq(expectedAuthToken), any(),
					any(), anyInt());
	}

	@Test
	void driverMustParseUrlParamsWithHostAndPort() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("username", "incorrectUser");
		props.put("password", "incorrectPassword");

		driver.connect("jdbc:neo4j://host:1000?user=correctUser&password=correctPassword", props);

		var expectedAuthToken = AuthTokens.basic("correctUser", "correctPassword");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", 1000)), any(), any(), eq(expectedAuthToken), any(), any(),
					anyInt());
	}

	@Test
	void driverMustParseUrlParamsWithHostAndPortAndDatabase() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("username", "incorrectUser");
		props.put("password", "incorrectPassword");

		driver.connect("jdbc:neo4j://host:1000/database?user=correctUser&password=correctPassword", props);

		var expectedAuthToken = AuthTokens.basic("correctUser", "correctPassword");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", 1000)), any(), eq("database"), eq(expectedAuthToken), any(),
					any(), anyInt());
	}

	@Test
	void driverMustParseUrlParamsWithHostAndDatabase() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("username", "incorrectUser");
		props.put("password", "incorrectPassword");

		driver.connect("jdbc:neo4j://host/database?user=correctUser&password=correctPassword", props);

		var expectedAuthToken = AuthTokens.basic("correctUser", "correctPassword");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("database"), eq(expectedAuthToken),
					any(), any(), anyInt());
	}

	@Test
	void driverMustUsePropsIfUrlParamsEmpty() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("user", "correctUser");
		props.put("password", "correctPassword");

		driver.connect("jdbc:neo4j://host/database", props);

		var expectedAuthToken = AuthTokens.basic("correctUser", "correctPassword");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), eq("database"), eq(expectedAuthToken),
					any(), any(), anyInt());
	}

	private static Stream<Arguments> jdbcURLProvider() {
		return Stream.of(Arguments.of("jdbc:neo4j://host", "host", DEFAULT_BOLT_PORT),
				Arguments.of("jdbc:neo4j://host/neo4j", "host", DEFAULT_BOLT_PORT),
				Arguments.of("jdbc:neo4j://host:1000", "host", 1000),
				Arguments.of("jdbc:neo4j://host:1000/neo4j", "host", 1000));
	}

}
