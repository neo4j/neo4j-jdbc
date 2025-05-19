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
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Collects various metrics per driver instance.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
final class MetricsCollectorImpl implements MetricsCollector {

	/**
	 * The reason for this cache is that the global registry (and possibly others) are
	 * indeed global. While you can get back the timers and counters from those, you don't
	 * get the backing objects of gauges back. While one could theoretically find the
	 * gauge, get the value and then recreate the gauge, this solution here is at that
	 * point a lot more straight forward: Caching the collector per registry.
	 */
	private static final Map<MeterRegistry, MetricsCollector> INSTANCES = new ConcurrentHashMap<>();

	static MetricsCollector of(MeterRegistry meterRegistry) {
		return INSTANCES.computeIfAbsent(meterRegistry, MetricsCollectorImpl::new);
	}

	private final MeterRegistry meterRegistry;

	private final Map<URI, AtomicInteger> openConnections = new ConcurrentHashMap<>();

	private final Map<StatementKey, AtomicInteger> openStatements = new ConcurrentHashMap<>();

	private final AtomicInteger cachedTranslations = new AtomicInteger();

	private final Map<URI, QueryMetrics> queryMetrics = new ConcurrentHashMap<>();

	private MetricsCollectorImpl(MeterRegistry meterRegistry) {

		this.meterRegistry = meterRegistry;

		var cachedTranslationsKey = "org.neo4j.jdbc.cached-translations";
		if (meterRegistry.find(cachedTranslationsKey).gauge() == null) {
			Gauge.builder(cachedTranslationsKey, this.cachedTranslations::get).register(meterRegistry);
		}
	}

	@Override
	public void onConnectionOpened(ConnectionOpenedEvent event) {
		var counter = this.openConnections.computeIfAbsent(Events.cleanURL(event.uri()), uri -> {
			var newCounter = new AtomicInteger();
			Gauge.builder("org.neo4j.jdbc.connections", newCounter::get)
				.description("The number of currently open connections")
				.tags("uri", uri.toString())
				.register(this.meterRegistry);
			return newCounter;
		});
		counter.incrementAndGet();
	}

	@Override
	public void onConnectionClosed(ConnectionClosedEvent event) {
		var counter = this.openConnections.get(Events.cleanURL(event.uri()));
		if (counter != null) {
			counter.decrementAndGet();
		}
	}

	@Override
	public void onStatementCreated(StatementCreatedEvent event) {
		var counter = this.openStatements
			.computeIfAbsent(new StatementKey(Events.cleanURL(event.uri()), event.statementType()), key -> {
				var newCounter = new AtomicInteger();
				Gauge.builder("org.neo4j.jdbc.statements", newCounter::get)
					.description("The number of currently open statements")
					.tags("uri", key.uri().toString(), "type", key.type().getSimpleName())
					.register(this.meterRegistry);
				return newCounter;
			});
		counter.incrementAndGet();
	}

	@Override
	public void onStatementClosed(StatementClosedEvent event) {
		var counter = this.openStatements.get(new StatementKey(Events.cleanURL(event.uri()), event.statementType()));
		if (counter != null) {
			counter.decrementAndGet();
		}
	}

	@Override
	public void onTranslationCached(TranslationCachedEvent event) {
		this.cachedTranslations.set(event.cacheSize());
	}

	@Override
	public void onExecutionEnded(ExecutionEndedEvent event) {
		var queryMetricsForUri = this.queryMetrics.computeIfAbsent(Events.cleanURL(event.uri()), uri -> {
			var queries = "org.neo4j.jdbc.queries";
			var uriString = uri.toString();
			return new QueryMetrics(
					Counter.builder(queries)
						.description("The total number of successful queries run")
						.tags("uri", uriString, "state", "successful")
						.register(this.meterRegistry),
					Counter.builder(queries)
						.description("The total number of queries that failed to run")
						.tags("uri", uriString, "state", "failed")
						.register(this.meterRegistry),
					Timer.builder(queries)
						.description("Duration of the queries being run")
						.tags("uri", uriString)
						.register(this.meterRegistry));
		});
		if (event.state() == ExecutionEndedEvent.State.SUCCESSFUL) {
			queryMetricsForUri.successfulQueries.increment();
		}
		else if (event.state() == ExecutionEndedEvent.State.FAILED) {
			queryMetricsForUri.failedQueries.increment();
		}
		queryMetricsForUri.queryTimer.record(event.elapsedTime());
	}

	record QueryMetrics(Counter successfulQueries, Counter failedQueries, Timer queryTimer) {
	}

	public record StatementKey(URI uri, Class<? extends Statement> type) {
	}

}
