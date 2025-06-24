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

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.ConnectionListener.NewAuthenticationEvent;

final class DefaultAuthenticationManagerImpl implements AuthenticationManager {

	private final URI targetUrl;

	private final Supplier<Authentication> authenticationSupplier;

	private final AtomicReference<AuthenticationAndState> currentAuthentication = new AtomicReference<>();

	private final Clock clock;

	/**
	 * Refresh will take place a bit before "now", so that any clock drift, a bad timing
	 * or early revokation might be taken care off.
	 */
	private final Duration refreshOffset;

	private final List<ConnectionListener> listeners;

	DefaultAuthenticationManagerImpl(URI targetUrl, Supplier<Authentication> authenticationSupplier, Clock clock,
			Duration refreshOffset) {
		this.targetUrl = targetUrl;
		this.authenticationSupplier = authenticationSupplier;
		this.clock = clock;
		this.refreshOffset = refreshOffset;
		this.listeners = new ArrayList<>();
	}

	boolean isValid(Authentication authentication) {

		if (authentication == null) {
			return false;
		}

		if (authentication instanceof ExpiringAuthentication expiringAuthentication) {
			var expiresAt = expiringAuthentication.expiresAt();
			return expiresAt == null || expiresAt.minus(this.refreshOffset).isAfter(Instant.now(this.clock));
		}
		return true;
	}

	@Override
	public Authentication getOrRefresh() {

		var hlp = this.currentAuthentication.updateAndGet(previous -> {
			if (previous != null && this.isValid(previous.authentication)) {
				return new AuthenticationAndState(previous.authentication, State.REUSED);
			}
			var authentication = this.authenticationSupplier.get();
			return new AuthenticationAndState(authentication, (previous != null) ? State.REFRESHED : State.NEW);
		});
		var eventState = hlp.state.toEventState();
		if (eventState != null) {
			this.notifyListeners(new NewAuthenticationEvent(this.targetUrl, eventState));
		}
		return hlp.authentication();
	}

	void notifyListeners(NewAuthenticationEvent event) {
		this.listeners.forEach(listener -> listener.onNewAuthentication(event));
	}

	@Override
	public void addListener(ConnectionListener connectionListener) {
		this.listeners.add(Objects.requireNonNull(connectionListener));
	}

	enum State {

		NEW, REUSED, REFRESHED;

		NewAuthenticationEvent.State toEventState() {
			return switch (this) {
				case NEW -> NewAuthenticationEvent.State.NEW;
				case REFRESHED -> NewAuthenticationEvent.State.REFRESHED;
				default -> null;
			};
		}

	}

	record AuthenticationAndState(Authentication authentication, State state) {

	}

}
