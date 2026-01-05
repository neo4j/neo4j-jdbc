/*
 * Copyright (c) 2023-2026 "Neo4j,"
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
package org.neo4j.jdbc.authn.spi;

import java.time.Instant;

/**
 * Represents an authentication to be used with the Neo4j JDBC Driver. This interface is
 * an abstract representation of a principal, which may be identified by a username and
 * password, a token or other means.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
public sealed interface Authentication
		permits DisabledAuthentication, UsernamePasswordAuthentication, CustomAuthentication {

	/**
	 * Creates a new authentication based on a username and password. Authentication
	 * itself will be done by the Neo4j server through the drivers
	 * {@code AuthenticationManager}. The token alone is not enough to deem any principal
	 * as authenticated.
	 * @param username the identity of the principal being authenticated
	 * @param password the credentials that prove the principal is correct
	 * @return an authentication token that needs to be processed by the Neo4j server
	 */
	static Authentication usernameAndPassword(String username, String password) {
		return usernameAndPassword(username, password, null);
	}

	/**
	 * Creates a new authentication based on a username and password. Authentication
	 * itself will be done by the Neo4j server through the drivers
	 * {@code AuthenticationManager}. The token alone is not enough to deem any principal
	 * as authenticated.
	 * @param username the identity of the principal being authenticated
	 * @param password the credentials that prove the principal is correct
	 * @param realm the realm to authenticate against
	 * @return an authentication token that needs to be processed by the Neo4j server
	 */
	static Authentication usernameAndPassword(String username, String password, String realm) {
		return new UsernamePasswordAuthentication(username, password, realm);
	}

	/**
	 * Creates new authentication based on a bearer token. The token is expected to be a
	 * non-null, BASE64 encoded {@link String}. The token will be either processed locally
	 * via an {@code AuthenticationManager} or through the SSO mechanism of the Neo4j
	 * server.
	 * @param token a BASE64 encoded bearer token, must not be null
	 * @return a new authentication that needs to be processed either directly in the
	 * driver or by the Neo4j server
	 */
	static Authentication bearer(String token) {
		return bearer(token, null);
	}

	/**
	 * Creates new authentication based on a bearer token. The token is expected to be a
	 * non-null, BASE64 encoded {@link String}. The token will be either processed locally
	 * via an {@code AuthenticationManager} or through the SSO mechanism of the Neo4j
	 * server. An optional instant can be passed as expiration time for this token
	 * @param token a BASE64 encoded bearer token, must not be {@literal null}
	 * @param expiresAt an optional instant from which this token might not be longer
	 * valid
	 * @return a new authentication that needs to be processed either directly in the
	 * driver or by the Neo4j server
	 */
	static Authentication bearer(String token, Instant expiresAt) {
		return new TokenAuthentication("bearer", token, expiresAt);
	}

	/**
	 * Creates new authentication based on a Kerberos token. The token is expected to be a
	 * non-null, BASE64 encoded {@link String}. The token will be either processed through
	 * the SSO mechanism of the Neo4j server.
	 * @param token a BASE64 encoded Kerberos token, must not be null
	 * @return a new authentication that needs to be processed either directly in an
	 * {@code AuthenticationManager} or by the Neo4j server
	 */
	static Authentication kerberos(String token) {
		return new TokenAuthentication("kerberos", token, null);
	}

	/**
	 * {@return a placeholder authentication bo be used with disabled authentication}
	 */
	static Authentication none() {
		return DisabledAuthentication.INSTANCE;
	}

}
