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

import java.time.Instant;
import java.util.Objects;

/**
 * Representation for a token based authentication.
 *
 * @param scheme the scheme to be used while authenticating
 * @param value the token used for authenticating
 * @param expiresAt an optional instant from which this token might not be longer valid
 * @author Michael J. Simons
 * @since 6.6.0
 */
record TokenAuthentication(AuthenticationScheme scheme, String value,
		Instant expiresAt) implements ExpiringAuthentication {

	TokenAuthentication {

		Objects.requireNonNull(scheme, "Scheme can't be null");
		Objects.requireNonNull(value, "Token can't be null");
	}
}
