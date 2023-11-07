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
package org.neo4j.driver.jdbc.internal.bolt;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.neo4j.driver.jdbc.internal.bolt.internal.InternalAuthToken;
import org.neo4j.driver.jdbc.internal.bolt.internal.util.Iterables;
import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.Values;

/**
 * This is a listing of the various methods of authentication supported by this driver.
 * The scheme used must be supported by the Neo4j Instance you are connecting to.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 */
public final class AuthTokens {

	private AuthTokens() {
	}

	/**
	 * The basic authentication scheme, using a username and a password.
	 * @param username this is the "principal", identifying who this token represents
	 * @param password this is the "credential", proving the identity of the user
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when either username or password is {@code null}
	 */
	public static AuthToken basic(String username, String password) {
		return basic(username, password, null);
	}

	/**
	 * The basic authentication scheme, using a username and a password.
	 * @param username this is the "principal", identifying who this token represents
	 * @param password this is the "credential", proving the identity of the user
	 * @param realm this is the "realm", specifies the authentication provider
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when either username or password is {@code null}
	 */
	public static AuthToken basic(String username, String password, String realm) {
		Objects.requireNonNull(username, "Username can't be null");
		Objects.requireNonNull(password, "Password can't be null");

		Map<String, Value> map = Iterables.newHashMapWithSize(4);
		map.put(InternalAuthToken.SCHEME_KEY, Values.value("basic"));
		map.put(InternalAuthToken.PRINCIPAL_KEY, Values.value(username));
		map.put(InternalAuthToken.CREDENTIALS_KEY, Values.value(password));
		if (realm != null) {
			map.put(InternalAuthToken.REALM_KEY, Values.value(realm));
		}
		return new InternalAuthToken(map);
	}

	/**
	 * The bearer authentication scheme, using a base64 encoded token.
	 * @param token base64 encoded token
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when token is {@code null}
	 */
	public static AuthToken bearer(String token) {
		Objects.requireNonNull(token, "Token can't be null");

		Map<String, Value> map = Iterables.newHashMapWithSize(2);
		map.put(InternalAuthToken.SCHEME_KEY, Values.value("bearer"));
		map.put(InternalAuthToken.CREDENTIALS_KEY, Values.value(token));
		return new InternalAuthToken(map);
	}

	/**
	 * The kerberos authentication scheme, using a base64 encoded ticket.
	 * @param base64EncodedTicket a base64 encoded service ticket
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when ticket is {@code null}
	 * @since 1.3
	 */
	public static AuthToken kerberos(String base64EncodedTicket) {
		Objects.requireNonNull(base64EncodedTicket, "Ticket can't be null");

		Map<String, Value> map = Iterables.newHashMapWithSize(3);
		map.put(InternalAuthToken.SCHEME_KEY, Values.value("kerberos"));
		map.put(InternalAuthToken.PRINCIPAL_KEY, Values.value("")); // This empty string
																	// is required for
																	// backwards
		// compatibility.
		map.put(InternalAuthToken.CREDENTIALS_KEY, Values.value(base64EncodedTicket));
		return new InternalAuthToken(map);
	}

	/**
	 * A custom authentication token used for doing custom authentication on the server
	 * side.
	 * @param principal this used to identify who this token represents
	 * @param credentials this is credentials authenticating the principal
	 * @param realm this is the "realm:", specifying the authentication provider.
	 * @param scheme this it the authentication scheme, specifying what kind of
	 * authentication that should be used
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when either principal, credentials or scheme is
	 * {@code null}
	 */
	public static AuthToken custom(String principal, String credentials, String realm, String scheme) {
		return custom(principal, credentials, realm, scheme, null);
	}

	/**
	 * A custom authentication token used for doing custom authentication on the server
	 * side.
	 * @param principal this used to identify who this token represents
	 * @param credentials this is credentials authenticating the principal
	 * @param realm this is the "realm:", specifying the authentication provider.
	 * @param scheme this it the authentication scheme, specifying what kind of
	 * authentication that should be used
	 * @param parameters extra parameters to be sent along the authentication provider.
	 * @return an authentication token that can be used to connect to Neo4j
	 * @throws NullPointerException when either principal, credentials or scheme is
	 * {@code null}
	 */
	public static AuthToken custom(String principal, String credentials, String realm, String scheme,
			Map<String, Object> parameters) {
		Objects.requireNonNull(principal, "Principal can't be null");
		Objects.requireNonNull(credentials, "Credentials can't be null");
		Objects.requireNonNull(scheme, "Scheme can't be null");

		Map<String, Value> map = Iterables.newHashMapWithSize(5);
		map.put(InternalAuthToken.SCHEME_KEY, Values.value(scheme));
		map.put(InternalAuthToken.PRINCIPAL_KEY, Values.value(principal));
		map.put(InternalAuthToken.CREDENTIALS_KEY, Values.value(credentials));
		if (realm != null) {
			map.put(InternalAuthToken.REALM_KEY, Values.value(realm));
		}
		if (parameters != null) {
			map.put(InternalAuthToken.PARAMETERS_KEY, Values.value(parameters));
		}
		return new InternalAuthToken(map);
	}

	/**
	 * No authentication scheme. This will only work if authentication is disabled on the
	 * Neo4j Instance we are connecting to.
	 * @return an authentication token that can be used to connect to Neo4j instances with
	 * auth disabled
	 */
	public static AuthToken none() {
		return new InternalAuthToken(Collections.singletonMap(InternalAuthToken.SCHEME_KEY, Values.value("none")));
	}

}
