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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultAuthenticationManagerImpl implements AuthenticationManager {

	private final AuthenticationProvider authenticationProvider;

	private final AtomicReference<Authentication> currentAuthentication = new AtomicReference<>();

	private final Clock clock;

	/**
	 * Refresh will take place a bit before "now", so that any clock drift, a bad timing
	 * or early revokation might be taken care off.
	 */
	private final Duration refreshOffset;

	DefaultAuthenticationManagerImpl(AuthenticationProvider authenticationProvider, Clock clock,
			Duration refreshOffset) {
		this.authenticationProvider = authenticationProvider;
		this.clock = clock;
		this.refreshOffset = refreshOffset;
	}

	boolean isValid(Authentication authentication) {

		if (authentication == null) {
			return false;
		}

		if (authentication instanceof TokenAuthentication tokenAuthentication
				&& tokenAuthentication.expiresAt() != null) {
			var now = Instant.now(this.clock);
			return tokenAuthentication.expiresAt().minus(this.refreshOffset).isAfter(now);
		}
		return true;
	}

	@Override
	public Authentication getOrRefresh() {

		var authentication = this.currentAuthentication.get();
		if (this.isValid(authentication)) {
			return authentication;
		}

		Authentication newAuthentication;
		try {
			newAuthentication = this.authenticationProvider.get();
		}
		catch (Exception ex) {
			// TODO this is not nice, exceptions needs to be on method
			throw new RuntimeException(ex);
		}
		var witness = this.currentAuthentication.compareAndExchange(authentication, newAuthentication);
		return (witness != authentication) ? witness : newAuthentication;
	}

}
