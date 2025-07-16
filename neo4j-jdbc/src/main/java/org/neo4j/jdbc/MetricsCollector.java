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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.DriverListener;
import org.neo4j.jdbc.events.StatementListener;

/**
 * Basic interface for a metrics collector.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
interface MetricsCollector extends DriverListener, ConnectionListener, StatementListener {

	/**
	 * A flag if the JDBC driver has tried to find the global metrics registry already.
	 */
	AtomicBoolean GLOBAL_REGISTRY_HAS_BEEN_TRIED = new AtomicBoolean(false);

	/**
	 * Tries to create a metrics collector based on the global Micrometer registry.
	 * @return a metrics collector based on the global Micrometer instance if available
	 */
	@SuppressWarnings("CastCanBeRemovedNarrowingVariableType")
	static Optional<MetricsCollector> tryGlobal() {
		Object globalRegistry;
		try {
			globalRegistry = Metrics.globalRegistry;
		}
		catch (@SuppressWarnings("squid:S1181") Throwable ex) {
			if (GLOBAL_REGISTRY_HAS_BEEN_TRIED.compareAndSet(false, true)) {
				Logger.getLogger("org.neo4j.jdbc").log(Level.FINE, "Metrics are not available");
			}
			return Optional.empty();
		}
		// Avoid touching the class that actually is dependent on MeterRegistry as long as
		// we didn't make sure it's there.
		return Optional.of(MetricsCollectorImpl.of((MeterRegistry) globalRegistry));
	}

}
