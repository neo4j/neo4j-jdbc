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
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.jr.ob.JSON;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.representations.AccessTokenResponse;
import org.neo4j.jdbc.Authentication;
import org.neo4j.jdbc.AuthenticationProvider;
import org.neo4j.jdbc.Neo4jDriver;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

class WipIT {

	@Test
	void tokenRefreshShouldWork() throws Exception {
		// Reuse with custom network is not officially supported, there are some hacks,
		// none I would like to pursue
		var dockerImageName = Optional.ofNullable(System.getProperty("neo4j-jdbc.default-neo4j-image"))
			.orElse("neo4j:5.26.8");
		if (!dockerImageName.contains("-enterprise")) {
			dockerImageName = dockerImageName + "-enterprise";
		}

		// noinspection resource
		try (var network = Network.newNetwork();
				var keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2.5")
					.useTlsKeystore("/keys.jks", "verysecret")
					.withRealmImportFile("neo4j-sso-test.json")
					.withNetwork(network)
					.withNetworkAliases("keycloak")
					.withEnv("KC_HOSTNAME", "https://keycloak:8443")
					.withEnv("KC_HOSTNAME_BACKCHANNEL_DYNAMIC", "true");
				var neo4j = new Neo4jContainer<>(dockerImageName).withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
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
					.withNeo4jConfig("dbms.security.oidc.keycloak.issuer",
							"https://keycloak:8443/realms/neo4j-sso-test")
					.withNeo4jConfig("dbms.security.oidc.keycloak.client_id", "neo4j-jdbc-driver")
					.withNeo4jConfig("dbms.security.oidc.keycloak.claims.username", "preferred_username")
					.withNeo4jConfig("dbms.security.oidc.keycloak.claims.groups", "groups")
					.withNeo4jConfig("dbms.security.auth_cache_ttl", "1s")
					.waitingFor(Neo4jContainer.WAIT_FOR_BOLT)
					.withNetwork(network)) {
			keycloak.start();
			neo4j.start();

			var cfg = new Configuration("http://localhost:" + keycloak.getHttpPort(), "neo4j-sso-test",
					"neo4j-jdbc-driver", Map.of("secret", "QcWXnTg8qJpVMnIvm8Ev8gp1PqJitZu4"), HttpClients.createMinimal());

			var client = AuthzClient.create(cfg);

			var acquired = new AtomicInteger();
			var refreshed = new AtomicInteger();
			var provider = new AuthenticationProvider() {

				TokensAndExpirationTime currentToken;

				@Override
				public Authentication get() throws IOException {
					if (this.currentToken == null) {
						this.currentToken = TokensAndExpirationTime.of(client.obtainAccessToken("william.foster", "d-fens"));
						acquired.incrementAndGet();
					}
					else {
						var url = cfg.getAuthServerUrl() + "/realms/" + cfg.getRealm()
								+ "/protocol/openid-connect/token";
						var http = new Http(cfg, cfg.getClientCredentialsProvider());
						this.currentToken = TokensAndExpirationTime.of(http.<AccessTokenResponse>post(url)
							.authentication()
							.client()
							.form()
							.param("grant_type", "refresh_token")
							.param("refresh_token", this.currentToken.refreshToken())
							.param("client_id", cfg.getResource())
							.param("client_secret", (String) cfg.getCredentials().get("secret"))
							.response()
							.json(AccessTokenResponse.class)
							.execute());
						refreshed.incrementAndGet();
					}
					return this.currentToken.toAuthentication();
				}
			};

			var driver = new Neo4jDriver();
			try (var connection = driver.connect(
					"jdbc:neo4j://%s:%d".formatted(neo4j.getHost(), neo4j.getMappedPort(7687)), new Properties(),
					provider); var stmt = connection.createStatement();

			) {
				stmt.executeUpdate("CREATE (n:IWasHere) RETURN elementId(n)");

				Thread.sleep(11_000);
				stmt.executeUpdate("CREATE (n:IWasHere) RETURN elementId(n)");

				Thread.sleep(11_000);
				try (var rs = stmt.executeQuery("MATCH (n:IWasHere) RETURN count(n) AS n")) {
					assertThat(rs.next()).isTrue();
					assertThat(rs.getInt("n")).isEqualTo(2);
				}
			}

			assertThat(acquired).hasValue(1);
			assertThat(refreshed).hasValue(2);
		}
	}

	record TokensAndExpirationTime(String accessToken, String refreshToken, Instant expiresAt) {

		static final Base64.Decoder DECODER = Base64.getDecoder();

		static TokensAndExpirationTime of(AccessTokenResponse accessTokenResponse) throws IOException {
			var chunks = accessTokenResponse.getToken().split("\\.");
			var payload = JSON.std.mapFrom(DECODER.decode(chunks[1]));
			var exp = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
			return new TokensAndExpirationTime(accessTokenResponse.getToken(), accessTokenResponse.getRefreshToken(),
					exp);
		}

		Authentication toAuthentication() {
			return Authentication.bearer(this.accessToken, this.expiresAt);
		}

	}

}
