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
package org.neo4j.jdbc;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.neo4j.bolt.connection.BoltProtocolVersion;
import org.neo4j.bolt.connection.observation.BoltExchangeObservation;
import org.neo4j.bolt.connection.observation.HttpExchangeObservation;
import org.neo4j.bolt.connection.observation.ImmutableObservation;
import org.neo4j.bolt.connection.observation.ObservationProvider;

/**
 * Utility class to hide away noop-observations for the bolt-connection api.
 *
 * @author Michael J. Simons
 */
final class BoltConnectionObservations {

	private BoltConnectionObservations() {
	}

	enum NoopBoltExchangeObservation implements BoltExchangeObservation {

		INSTANCE;

		@Override
		public BoltExchangeObservation onWrite(String messageName) {
			return this;
		}

		@Override
		public BoltExchangeObservation onRecord() {
			return this;
		}

		@Override
		public BoltExchangeObservation onSummary(String messageName) {
			return this;
		}

		@Override
		public BoltExchangeObservation error(Throwable error) {
			return this;
		}

		@Override
		public void stop() {
		}

	}

	enum NoopHttpExchangeObservation implements HttpExchangeObservation {

		INSTANCE;

		@Override
		public HttpExchangeObservation onHeaders(Map<String, List<String>> headers) {
			return this;
		}

		@Override
		public HttpExchangeObservation onResponse(Response response) {
			return this;
		}

		@Override
		public HttpExchangeObservation error(Throwable error) {
			return this;
		}

		@Override
		public void stop() {

		}

	}

	enum NoopObservation implements ImmutableObservation {

		INSTANCE

	}

	enum NoopBoltObservationProvider implements ObservationProvider {

		INSTANCE;

		@Override
		public BoltExchangeObservation boltExchange(ImmutableObservation observationParent, String host, int port,
				BoltProtocolVersion boltVersion, BiConsumer<String, String> setter) {
			return NoopBoltExchangeObservation.INSTANCE;
		}

		@Override
		public HttpExchangeObservation httpExchange(ImmutableObservation observationParent, URI uri, String method,
				String uriTemplate, BiConsumer<String, String> setter) {
			return NoopHttpExchangeObservation.INSTANCE;
		}

		@Override
		public ImmutableObservation scopedObservation() {
			return NoopObservation.INSTANCE;
		}

		@Override
		public <T> T supplyInScope(ImmutableObservation observation, Supplier<T> supplier) {
			return supplier.get();
		}

	}

}
