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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.jr.ob.JSON;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.representations.AccessTokenResponse;
import org.neo4j.jdbc.Authentication;
import org.neo4j.jdbc.Neo4jDriver;
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

	private static AuthzAuthenticationSupplier authenticationSupplier;

	@BeforeAll
	static void startContainersAndMetrics() {

		// Start containers
		KEYCLOAK.start();
		NEO4J.start();

		Metrics.globalRegistry.add(METER_REGISTRY);

		// Initialize authentication supplier
		var authzConfiguration = new Configuration("http://localhost:%d".formatted(KEYCLOAK.getHttpPort()),
				"neo4j-sso-test", "neo4j-jdbc-driver", Map.of("secret", "QcWXnTg8qJpVMnIvm8Ev8gp1PqJitZu4"),
				HttpClients.createMinimal());
		authenticationSupplier = new AuthzAuthenticationSupplier(authzConfiguration);
	}

	static Stream<Arguments> tokenRefreshShouldWork() throws Exception {

		var neo4jUri = "jdbc:neo4j://%s:%d".formatted(NEO4J.getHost(), NEO4J.getMappedPort(7687));
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
		}));

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

			await().atMost(tokenLifespanPlusN)
				.until(() -> authenticationSupplier.currentToken.get().expiresAt.isBefore(Instant.now()));
			stmt.executeUpdate("INSERT INTO IWasHere(test) VALUES ('Hello')");

			await().atMost(tokenLifespanPlusN)
				.until(() -> authenticationSupplier.currentToken.get().expiresAt.isBefore(Instant.now()));
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

	@AfterAll
	static void cleanUp() {
		NEO4J.stop();
		KEYCLOAK.stop();
		NETWORK.close();

		Neo4jDriver.registerAuthenticationSupplier(null);

		Metrics.globalRegistry.remove(METER_REGISTRY);
	}

	private static final class AuthzAuthenticationSupplier implements Supplier<Authentication> {

		private final AuthzClient authzClient;

		private final Configuration cfg;

		private final Http http;

		private final String url;

		private final AtomicReference<TokensAndExpirationTime> currentToken = new AtomicReference<>();

		AuthzAuthenticationSupplier(Configuration cfg) {
			this.cfg = cfg;
			this.authzClient = AuthzClient.create(cfg);
			this.url = "%s/realms/%s/protocol/openid-connect/token".formatted(cfg.getAuthServerUrl(), cfg.getRealm());
			this.http = new Http(cfg, cfg.getClientCredentialsProvider());
		}

		@Override
		public Authentication get() {
			return this.currentToken.updateAndGet(previous -> {
				if (previous == null) {
					return get0();
				}
				return refresh0(previous.refreshToken());
			}).toAuthentication();
		}

		TokensAndExpirationTime get0() {
			try {
				return TokensAndExpirationTime.of(this.authzClient.obtainAccessToken("william.foster", "d-fens"));
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		TokensAndExpirationTime refresh0(String refreshToken) {
			try {
				return TokensAndExpirationTime.of(this.http.<AccessTokenResponse>post(this.url)
					.authentication()
					.client()
					.form()
					.param("grant_type", "refresh_token")
					.param("refresh_token", refreshToken)
					.param("client_id", this.cfg.getResource())
					.param("client_secret", (String) this.cfg.getCredentials().get("secret"))
					.response()
					.json(AccessTokenResponse.class)
					.execute());
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}

	private record TokensAndExpirationTime(String accessToken, Instant expiresAt, String refreshToken) {

		static final Base64.Decoder DECODER = Base64.getDecoder();

		static TokensAndExpirationTime of(AccessTokenResponse accessTokenResponse) throws IOException {
			var chunks = accessTokenResponse.getToken().split("\\.");
			var payload = JSON.std.mapFrom(DECODER.decode(chunks[1]));
			var exp = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
			return new TokensAndExpirationTime(accessTokenResponse.getToken(), exp,
					accessTokenResponse.getRefreshToken());
		}

		Authentication toAuthentication() {
			return Authentication.bearer(this.accessToken, this.expiresAt);
		}

	}

}
