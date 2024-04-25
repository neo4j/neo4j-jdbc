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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.jdbc.internal.bolt.AuthToken;
import org.neo4j.jdbc.internal.bolt.AuthTokens;
import org.neo4j.jdbc.internal.bolt.BoltConnection;
import org.neo4j.jdbc.internal.bolt.BoltConnectionProvider;
import org.neo4j.jdbc.internal.bolt.BoltServerAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
	void driverMustUnescapeURL() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		driver.connect("jdbc:neo4j://host?user=user%3D&password=%26pass%3D%20word%3F", new Properties());

		var expectedAuthToken = AuthTokens.basic("user=", "&pass= word?");

		then(this.boltConnectionProvider).should()
			.connect(eq(new BoltServerAddress("host", DEFAULT_BOLT_PORT)), any(), any(), eq(expectedAuthToken), any(),
					any(), anyInt());
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

	@Test
	void testMinimalGetPropertyInfo() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "host" -> assertThat(info.value).isEqualTo("host");
				case "port" -> assertThat(info.value).isEqualTo("1234");
				case "database" -> assertThat(info.value).isEqualTo("customDb");
				case "authScheme" -> assertThat(info.value).isEqualTo("basic");
				case "user" -> assertThat(info.value).isEqualTo("neo4j");
				case "password" -> assertThat(info.value).isEqualTo("password");
				case "authRealm" -> assertThat(info.value).isEqualTo("");
				case "agent" -> assertThat(info.value).isEqualTo("neo4j-jdbc/unknown");
				case "timeout" -> assertThat(info.value).isEqualTo("1000");
				case "enableSQLTranslation", "ssl", "s2c.alwaysEscapeNames", "s2c.prettyPrint" ->
					assertThat(info.value).isEqualTo("false");
				case "rewriteBatchedStatements" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("disable");
				default -> assertThat(info.name).isIn("host", "port", "database", "authScheme", "user", "password",
						"authRealm", "agent", "timeout", "enableSQLTranslation", "ssl", "s2c.alwaysEscapeNames",
						"s2c.prettyPrint", "s2c.enableCache", "rewriteBatchedStatements", "sslMode",
						"cacheSQLTranslations");
			}
		}
	}

	@Test
	void testMinimalParseConfig() throws SQLException {
		Properties props = new Properties();

		var config = Neo4jDriver.parseConfig("jdbc:neo4j://host", props);

		assertThat(config.host()).isEqualTo("host");
		assertThat(config.port()).isEqualTo(7687);
		assertThat(config.database()).isEqualTo("neo4j");
		assertThat(config.authScheme()).isEqualTo(Neo4jDriver.AuthScheme.BASIC);
		assertThat(config.user()).isEqualTo("neo4j");
		assertThat(config.password()).isEqualTo("password");
		assertThat(config.authRealm()).isEqualTo("");
		assertThat(config.agent()).isEqualTo("neo4j-jdbc/unknown");
		assertThat(config.timeout()).isEqualTo(1000);
		assertThat(config.enableSQLTranslation()).isFalse();
		assertThat(config.enableTranslationCaching()).isFalse();
		assertThat(config.rewriteBatchedStatements()).isTrue();
		assertThat(config.rewritePlaceholders()).isTrue();
		assertThat(config.sslProperties().ssl()).isFalse();
		assertThat(config.sslProperties().sslMode()).isEqualTo(Neo4jDriver.SSLMode.DISABLE);

		// raw config, i.e., everything the user explicitly set
		var rawConfig = new HashMap<>(config.rawConfig());
		assertThat(rawConfig.remove("host")).isEqualTo(config.host());
		assertThat(rawConfig.remove("ssl")).isEqualTo(String.valueOf(config.sslProperties().ssl()));
		assertThat(rawConfig.remove("sslMode")).isEqualTo(config.sslProperties().sslMode().getName());
		assertThat(rawConfig).isEmpty();

		assertThat(config.misc()).isEqualTo(rawConfig);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldUnifyProperties(boolean value) throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234/customDb?cacheSQLTranslations=%s".formatted(value),
				props);

		var expected = String.valueOf(value);
		assertThat(infos).anyMatch(info -> "cacheSQLTranslations".equals(info.name) && expected.equals(info.value))
			.noneMatch(info -> "s2c.enableCache".equals(info.name));
	}

	@Test
	void testGetPropertyInfoPropertyOverrides() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("user", "user1");
		props.put("authScheme", "basic");
		props.put("password", "user1Password");
		props.put("database", "customDb");
		props.put("authRealm", "myRealm");
		props.put("timeout", "2000");
		props.put("enableSQLTranslation", "true");
		props.put("rewriteBatchedStatements", "false");
		props.put("cacheSQLTranslations", "true");

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234", props);

		for (var info : infos) {
			assertThat(info.description).isNotNull();

			switch (info.name) {
				case "host" -> assertThat(info.value).isEqualTo("host");
				case "port" -> assertThat(info.value).isEqualTo("1234");
				case "database" -> assertThat(info.value).isEqualTo("customDb");
				case "authScheme" -> assertThat(info.value).isEqualTo("basic");
				case "user" -> assertThat(info.value).isEqualTo("user1");
				case "password" -> assertThat(info.value).isEqualTo("user1Password");
				case "authRealm" -> assertThat(info.value).isEqualTo("myRealm");
				case "agent" -> assertThat(info.value).isEqualTo("neo4j-jdbc/unknown");
				case "timeout" -> assertThat(info.value).isEqualTo("2000");
				case "cacheSQLTranslations" -> assertThat(info.value).isEqualTo("true");
				case "ssl", "rewriteBatchedStatements", "s2c.alwaysEscapeNames", "s2c.prettyPrint" ->
					assertThat(info.value).isEqualTo("false");
				case "enableSQLTranslation" -> assertThat(info.value).isEqualTo("true");
				case "sslMode" -> assertThat(info.value).isEqualTo("disable");
				default -> assertThat(info.name).isIn("host", "port", "database", "authScheme", "user", "password",
						"agent", "authRealm", "timeout", "enableSQLTranslation", "ssl", "s2c.alwaysEscapeNames",
						"s2c.prettyPrint", "s2c.enableCache", "rewriteBatchedStatements", "sslMode");
			}
		}
	}

	@Test
	void testParseConfigOverrides() throws SQLException {
		Properties props = new Properties();
		props.put("authScheme", "basic");
		props.put("user", "user1");
		props.put("password", "user1Password");
		props.put("database", "customDb");
		props.put("authRealm", "myRealm");
		props.put("agent", "baby's-first-agent/1.2.3");
		props.put("timeout", "2000");
		props.put("enableSQLTranslation", "true");
		props.put("rewriteBatchedStatements", "false");
		props.put("cacheSQLTranslations", "true");
		props.put("s2c.alwaysEscapeNames", "true");
		props.put("s2c.prettyPrint", "true");
		props.put("s2c.enableCache", "true");
		props.put("customProperty", "foo");

		var config = Neo4jDriver.parseConfig("jdbc:neo4j://host:1234/?sslMode=require&customQuery=bar", props);

		assertThat(config.host()).isEqualTo("host");
		assertThat(config.port()).isEqualTo(1234);
		assertThat(config.database()).isEqualTo("customDb");
		assertThat(config.authScheme()).isEqualTo(Neo4jDriver.AuthScheme.BASIC);
		assertThat(config.user()).isEqualTo("user1");
		assertThat(config.password()).isEqualTo("user1Password");
		assertThat(config.authRealm()).isEqualTo("myRealm");
		assertThat(config.agent()).isEqualTo("baby's-first-agent/1.2.3");
		assertThat(config.timeout()).isEqualTo(2000);
		assertThat(config.enableSQLTranslation()).isTrue();
		assertThat(config.enableTranslationCaching()).isTrue();
		assertThat(config.rewriteBatchedStatements()).isFalse();
		assertThat(config.rewritePlaceholders()).isFalse();
		assertThat(config.sslProperties().ssl()).isTrue();
		assertThat(config.sslProperties().sslMode()).isEqualTo(Neo4jDriver.SSLMode.REQUIRE);

		// raw config, i.e., everything the user explicitly set
		var rawConfig = new HashMap<>(config.rawConfig());
		assertThat(rawConfig.remove("host")).isEqualTo(config.host());
		assertThat(rawConfig.remove("port")).isEqualTo(String.valueOf(config.port()));
		assertThat(rawConfig.remove("database")).isEqualTo(config.database());
		assertThat(rawConfig.remove("authScheme")).isEqualTo(config.authScheme().getName());
		assertThat(rawConfig.remove("user")).isEqualTo(config.user());
		assertThat(rawConfig.remove("password")).isEqualTo(config.password());
		assertThat(rawConfig.remove("authRealm")).isEqualTo(config.authRealm());
		assertThat(rawConfig.remove("agent")).isEqualTo(config.agent());
		assertThat(rawConfig.remove("timeout")).isEqualTo(String.valueOf(config.timeout()));
		assertThat(rawConfig.remove("enableSQLTranslation")).isEqualTo(String.valueOf(config.enableSQLTranslation()));
		assertThat(rawConfig.remove("cacheSQLTranslations"))
			.isEqualTo(String.valueOf(config.enableTranslationCaching()));
		assertThat(rawConfig.remove("rewriteBatchedStatements"))
			.isEqualTo(String.valueOf(config.rewriteBatchedStatements()));
		assertThat(rawConfig.remove("ssl")).isEqualTo(String.valueOf(config.sslProperties().ssl()));
		assertThat(rawConfig.remove("sslMode")).isEqualTo(config.sslProperties().sslMode().getName());
		// the rest of the raw config should have ended up in misc
		assertThat(config.misc()).isEqualTo(rawConfig);
		assertThat(rawConfig.remove("s2c.alwaysEscapeNames")).isEqualTo("true");
		assertThat(rawConfig.remove("s2c.prettyPrint")).isEqualTo("true");
		assertThat(rawConfig.remove("s2c.enableCache")).isEqualTo("true");
		assertThat(rawConfig.remove("customProperty")).isEqualTo("foo");
		assertThat(rawConfig.remove("customQuery")).isEqualTo("bar");
		assertThat(rawConfig).isEmpty();
	}

	@Test
	void testWronglyTypePropertyIsIgnored() throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		Properties props = new Properties();
		props.put("user", 1);

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234", props);
		for (var info : infos) {
			if (!info.name.equals("user")) {
				continue;
			}
			// invalid property type is being ignored - using default instead.
			assertThat(info.value).isEqualTo("neo4j");
		}
	}

	@ParameterizedTest
	@MethodSource("authSchemeProvider")
	void testAuthSchemesInfo(Properties props) throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		var infos = driver.getPropertyInfo("jdbc:neo4j://host:1234", props);

		for (var info : infos) {
			var expected = props.getProperty(info.name);
			if (expected == null) {
				continue;
			}
			assertThat(info.value).isEqualTo(expected);
		}

		var infoNames = Arrays.stream(infos).map(info -> info.name).collect(Collectors.toSet());
		assertThat(infoNames).containsAll(props.stringPropertyNames());
	}

	@ParameterizedTest
	@MethodSource("authSchemeProvider")
	void driverMustParseUrlParamsWithHostAndPortAndDatabase(Properties props, AuthToken expectedAuthToken)
			throws SQLException {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		driver.connect("jdbc:neo4j://host:1000/database", props);

		then(this.boltConnectionProvider).should()
			.connect(any(), any(), any(), eq(expectedAuthToken), any(), any(), anyInt());
	}

	@Test
	void testUnknownAuthSchemeInfo() {
		var driver = new Neo4jDriver(this.boltConnectionProvider);

		var props = new Properties();
		props.put("authScheme", "foobar");

		assertThatThrownBy(() -> driver.getPropertyInfo("jdbc:neo4j://host:1234", props))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("foobar is not a valid option for authScheme");
	}

	private static Stream<Arguments> jdbcURLProvider() {
		return Stream.of(Arguments.of("jdbc:neo4j://host", "host", DEFAULT_BOLT_PORT),
				Arguments.of("jdbc:neo4j://host/neo4j", "host", DEFAULT_BOLT_PORT),
				Arguments.of("jdbc:neo4j://host:1000", "host", 1000),
				Arguments.of("jdbc:neo4j://host:1000/neo4j", "host", 1000));
	}

	private static Stream<Arguments> authSchemeProvider() {
		var propsNone = new Properties();
		propsNone.put("authScheme", "none");
		var tokenNone = AuthTokens.none();

		var propsBasic = new Properties();
		propsBasic.put("authScheme", "basic");
		propsBasic.put("user", "user1");
		propsBasic.put("password", "user1Password");
		propsBasic.put("authRealm", "user1Realm");
		var tokenBasic = AuthTokens.basic("user1", "user1Password", "user1Realm");

		var propsKerberos = new Properties();
		propsKerberos.put("authScheme", "kerberos");
		propsKerberos.put("password", "myTicket");
		var tokenKerberos = AuthTokens.kerberos("myTicket");

		var propsBearer = new Properties();
		propsBearer.put("authScheme", "bearer");
		propsBearer.put("password", "myToken");
		var tokenBearer = AuthTokens.bearer("myToken");

		return Stream.of(Arguments.of(propsBasic, tokenBasic), Arguments.of(propsNone, tokenNone),
				Arguments.of(propsKerberos, tokenKerberos), Arguments.of(propsBearer, tokenBearer));
	}

}
