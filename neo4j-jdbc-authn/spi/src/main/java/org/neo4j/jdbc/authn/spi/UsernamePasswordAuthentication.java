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
package org.neo4j.jdbc.authn.spi;

import java.util.Objects;

/**
 * Representation for a username and password based authentication.
 *
 * @param username the identity of the principal being authenticated
 * @param password the credentials that prove the principal is correct
 * @param realm the realm to authenticate against
 * @author Michael J. Simons
 * @since 6.6.0
 */
public record UsernamePasswordAuthentication(String username, String password, String realm) implements Authentication {

	/**
	 * The constructor will validate the presence of both {@code username} and
	 * {@code password}.
	 * @param username the identity of the principal being authenticated
	 * @param password the credentials that prove the principal is correct
	 * @param realm the realm to authenticate against
	 */
	public UsernamePasswordAuthentication {

		Objects.requireNonNull(username, "Username can't be null");
		Objects.requireNonNull(password, "Password can't be null");
	}
}
