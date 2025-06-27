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
package org.neo4j.jdbc.it.sso;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authorization.client.Configuration;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.authn.kc.KCAuthenticationSupplier;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledIfSystemProperty(named = "skipReauthenticationIT", matches = ".+", disabledReason = "For performance reasons.")
class ReauthenticationIT {

	private static final Network NETWORK = Network.newNetwork();

	private static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.5")
		.withNetwork(NETWORK)
		.useTlsKeystore("/keys.jks", "verysecret")
		.withRealmImportFile("neo4j-sso-test.json")
		.withNetworkAliases("keycloak")
		.withEnv("KC_HOSTNAME", "https://keycloak:8443")
		.withEnv("KC_HOSTNAME_BACKCHANNEL_DYNAMIC", "true");

	@SuppressWarnings("resource")
	private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>(
			Optional.ofNullable(System.getProperty("neo4j-jdbc.default-neo4j-image"))
				.or(() -> Optional.of("neo4j:5.26.8"))
				.map(v -> !v.contains("-enterprise") ? v + "-enterprise" : v)
				.orElseThrow())
		.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
		.withNetwork(NETWORK)
		.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
		.withCopyFileToContainer(MountableFile.forClasspathResource("/keys.jks"), "/tmp/keys.jks")
		.withNeo4jConfig("server.jvm.additional",
				"-Djavax.net.ssl.keyStore=/tmp/keys.jks -Djavax.net.ssl.keyStorePassword=verysecret -Djavax.net.ssl.trustStore=/tmp/keys.jks -Djavax.net.ssl.trustStorePassword=verysecret")
		.withNeo4jConfig("dbms.security.authentication_providers", "oidc-keycloak,native")
		.withNeo4jConfig("dbms.security.authorization_providers", "oidc-keycloak,native")
		.withNeo4jConfig("dbms.security.oidc.keycloak.display_name", "Keycloak")
		.withNeo4jConfig("dbms.security.oidc.keycloak.auth_flow", "pkce")
		.withNeo4jConfig("dbms.security.oidc.keycloak.well_known_discovery_uri",
				"https://keycloak:8443/realms/neo4j-sso-test/.well-known/openid-configuration")
		.withNeo4jConfig("dbms.security.oidc.keycloak.params",
				"client_id=neo4j-jdbc-driver;response_type=code;scope=openid email roles")
		.withNeo4jConfig("dbms.security.oidc.keycloak.audience", "account")
		.withNeo4jConfig("dbms.security.oidc.keycloak.issuer", "https://keycloak:8443/realms/neo4j-sso-test")
		.withNeo4jConfig("dbms.security.oidc.keycloak.client_id", "neo4j-jdbc-driver")
		.withNeo4jConfig("dbms.security.oidc.keycloak.claims.username", "preferred_username")
		.withNeo4jConfig("dbms.security.oidc.keycloak.claims.groups", "groups")
		.withNeo4jConfig("dbms.security.auth_cache_ttl", "5s");

	private static final SimpleMeterRegistry METER_REGISTRY = new SimpleMeterRegistry();

	private static KCAuthenticationSupplier authenticationSupplier;

	private static String neo4jUri;

	@BeforeAll
	static void startContainersAndMetrics() {

		// Start containers
		KEYCLOAK.start();
		NEO4J.start();

		Metrics.globalRegistry.add(METER_REGISTRY);

		// Initialize authentication supplier
		var authzConfiguration = new Configuration("http://%s:%d".formatted(KEYCLOAK.getHost(), KEYCLOAK.getHttpPort()),
				"neo4j-sso-test", "neo4j-jdbc-driver", Map.of("secret", "QcWXnTg8qJpVMnIvm8Ev8gp1PqJitZu4"),
				HttpClients.createMinimal());
		authenticationSupplier = (KCAuthenticationSupplier) KCAuthenticationSupplier.of("william.foster", "d-fens",
				authzConfiguration);
		neo4jUri = "jdbc:neo4j://%s:%d".formatted(NEO4J.getHost(), NEO4J.getMappedPort(7687));
	}

	static Stream<Arguments> tokenRefreshShouldWork() throws Exception {

		var builder = Stream.<Arguments>builder();
		var properties = new Properties();
		properties.put("enableSQLTranslation", "true");

		builder.add(Arguments.argumentSet("GlobalAuth", (Supplier<Connection>) () -> {
			Neo4jDriver.registerAuthenticationSupplier(authenticationSupplier);
			try {
				return DriverManager.getConnection(neo4jUri, properties);
			}
			catch (SQLException ex) {
				throw new RuntimeException(ex);
			}
		}));

		builder.add(Arguments.argumentSet("LocalAuth", (Supplier<Connection>) () -> {
			var driver = new Neo4jDriver();
			try {
				return driver.connect(neo4jUri, properties, authenticationSupplier);
			}
			catch (SQLException ex) {
				throw new RuntimeException(ex);
			}
		}, (Callable<Boolean>) authenticationSupplier::currentTokenIsExpired));

		var tmpDir = Files.createTempDirectory("neo4j-jdbc");
		var dotEnvFile = Files.createFile(tmpDir.resolve(".env"));
		Files.write(dotEnvFile, List.of("NEO4J_URI=%s".formatted(neo4jUri)));
		builder.add(Arguments.argumentSet("LocalAuth(builder): ", (Supplier<Connection>) () -> {
			try {
				return Neo4jDriver.withAuthenticationSupplier(authenticationSupplier)
					.withSQLTranslation()
					.fromEnv(tmpDir, dotEnvFile.getFileName().toString())
					.orElseThrow();
			}
			catch (SQLException ex) {
				throw new RuntimeException(ex);
			}
		}));

		return builder.build();
	}

	@BeforeEach
	void resetGlobal() {
		Neo4jDriver.registerAuthenticationSupplier(null);
		Metrics.globalRegistry.clear();
	}

	@ParameterizedTest
	@MethodSource
	void tokenRefreshShouldWork(Supplier<Connection> connectionSupplier) throws Exception {

		var tokenLifespanPlusN = Duration.ofSeconds(11);

		try (var connection = connectionSupplier.get(); var stmt = connection.createStatement()) {

			stmt.executeUpdate("DELETE FROM IWasHere");
			stmt.executeUpdate("INSERT INTO IWasHere(test) VALUES ('Hello')");

			await().atMost(tokenLifespanPlusN).until(authenticationSupplier::currentTokenIsExpired);
			stmt.executeUpdate("INSERT INTO IWasHere(test) VALUES ('Hello')");

			await().atMost(tokenLifespanPlusN).until(authenticationSupplier::currentTokenIsExpired);
			try (var rs = stmt.executeQuery("SELECT count(*) AS n FROM IWasHere")) {
				assertThat(rs.next()).isTrue();
				assertThat(rs.getInt("n")).isEqualTo(2);
			}
		}

		assertThat(Metrics.globalRegistry.find("org.neo4j.jdbc.authentications").tags("state", "new").counter())
			.isNotNull()
			.extracting(Counter::count)
			.isEqualTo(1.0);
		assertThat(Metrics.globalRegistry.find("org.neo4j.jdbc.authentications").tags("state", "refreshed").counter())
			.extracting(Counter::count)
			.isEqualTo(2.0);
	}

	@Test
	void authenticationSupplierFactoriesShouldWork() throws SQLException {

		var properties = new Properties();
		properties.put("enableSQLTranslation", "true");

		properties.put("user", "william.foster");
		properties.put("password", "d-fens");

		properties.put("authn.supplier", "kc");
		properties.put("authn.kc.authServerUrl", "http://%s:%d".formatted(KEYCLOAK.getHost(), KEYCLOAK.getHttpPort()));
		properties.put("authn.kc.realm", "neo4j-sso-test");
		properties.put("authn.kc.clientId", "neo4j-jdbc-driver");
		properties.put("authn.kc.clientSecret", "QcWXnTg8qJpVMnIvm8Ev8gp1PqJitZu4");

		try (var connection = DriverManager.getConnection(neo4jUri, properties);
				var stmt = connection.createStatement();
				var rs = stmt.executeQuery("SELECT 1")) {
			assertThat(rs.next()).isTrue();
			assertThat(rs.getInt(1)).isOne();
		}
	}

	@AfterAll
	static void cleanUp() {
		NEO4J.stop();
		KEYCLOAK.stop();
		NETWORK.close();

		Neo4jDriver.registerAuthenticationSupplier(null);

		Metrics.globalRegistry.remove(METER_REGISTRY);
	}

}
