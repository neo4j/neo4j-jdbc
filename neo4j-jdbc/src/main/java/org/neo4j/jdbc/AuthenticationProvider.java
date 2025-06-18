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

import java.io.IOException;

/**
 * A provider of {@link Authentication authentications}. A provider can be as stateless as
 * the underlying mechanism allows, the underlying Neo4j-JDBC driver will ensure that
 * {@link #get()} is called exactly once per connection for both permanent authentications
 * and expiring authentications. {@link #refresh(String)} on the other hand will be called
 * when the driver comes across an expired token. In that case, the driver will try to
 * determine any state such as the {@code refreshToken} from the authentication it
 * currently holds and pass it on to the {@link #refresh(String)} method.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
@FunctionalInterface
public interface AuthenticationProvider {

	/**
	 * This method should provide the initial authentication to use. If the authentication
	 * can expire, the provider <strong>must</strong> implement {@link #refresh(String)},
	 * too.
	 * @return the authentication to use
	 * @throws IOException in case the retrieval fails
	 */
	Authentication get() throws IOException;

	/**
	 * The default implementation will throw an {@link UnsupportedOperationException},
	 * hence, any provider providing expiring tokens must overwrite this method and either
	 * use its internal state or if present, the given {@code refreshToken} to fetch a new
	 * token from upstream.
	 * @param refreshToken an optional refresh token gathered from the previous
	 * authentication
	 * @return the refreshed authentication to use
	 * @throws IOException in case the retrieval fails
	 */
	default Authentication refresh(String refreshToken) throws IOException {
		throw new UnsupportedOperationException();
	}

}
