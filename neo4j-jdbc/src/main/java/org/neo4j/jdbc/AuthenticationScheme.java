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
package org.neo4j.jdbc;

/**
 * Authentication schemes supported by the Neo4j server. Those do not match directly to
 * {@link Authentication} objects, as for example the {@link TokenAuthentication} can
 * support multiple schemes.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
enum AuthenticationScheme {

	/**
	 * Disable authentication.
	 */
	NONE("none"),
	/**
	 * Use basic auth (username and password).
	 */
	BASIC("basic"),
	/**
	 * Use a token as authentication (the password will be treated as JWT or other SSO
	 * token).
	 */
	BEARER("bearer"),
	/**
	 * Use Kerberos authentication.
	 */
	KERBEROS("kerberos");

	private final String name;

	AuthenticationScheme(String name) {
		this.name = name;
	}

	/**
	 * {@return the name of this scheme}
	 */
	String getName() {
		return this.name;
	}

}
