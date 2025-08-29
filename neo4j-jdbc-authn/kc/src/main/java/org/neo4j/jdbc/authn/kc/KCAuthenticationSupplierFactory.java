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
package org.neo4j.jdbc.authn.kc;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.http.impl.client.HttpClients;
import org.keycloak.authorization.client.Configuration;
import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.authn.spi.AuthenticationSupplierFactory;

/**
 * Creates Keycloak based authentication suppliers. It registers itself under the name
 * {@literal kc}. The following properties are required:
 * <ul>
 * <li>{@code authServerUrl}</li>
 * <li>{@code realm}</li>
 * <li>{@code clientId}</li>
 * <li>{@code clientSecret}</li>
 * </ul>
 *
 * The initial username and password is first taken from the individual parameters,
 * otherwise looked up in the properties, too. Only secret based client authorization is
 * supported as of 6.6.0.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
public final class KCAuthenticationSupplierFactory implements AuthenticationSupplierFactory {

	/**
	 * Used by the service loader mechanism, safe to use standalone.
	 */
	public KCAuthenticationSupplierFactory() {
	}

	@Override
	public String getName() {
		return "kc";
	}

	@Override
	public Supplier<Authentication> create(String user, String password, Map<String, ?> properties) {

		var authServerUrl = (String) Objects.requireNonNull(properties.get("authServerUrl"));
		var realm = (String) Objects.requireNonNull(properties.get("realm"));
		var clientId = (String) Objects.requireNonNull(properties.get("clientId"));
		var clientSecret = (String) Objects.requireNonNull(properties.get("clientSecret"));

		return new KCAuthenticationSupplier(
				Objects.requireNonNullElseGet(user, () -> (String) Objects.requireNonNull(properties.get("user"))),
				Objects.requireNonNullElseGet(password,
						() -> (String) Objects.requireNonNull(properties.get("password"))),
				new Configuration(authServerUrl, realm, clientId, Map.of("secret", clientSecret),
						HttpClients.createMinimal()));
	}

}
