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
import java.net.URISyntaxException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.neo4j.jdbc.events.ConnectionClosedEvent;
import org.neo4j.jdbc.events.ConnectionOpenedEvent;
import org.neo4j.jdbc.events.ExecutionEndedEvent;
import org.neo4j.jdbc.events.StatementClosedEvent;
import org.neo4j.jdbc.events.StatementCreatedEvent;
import org.neo4j.jdbc.events.TranslationCachedEvent;

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

		var cachedTranslations = "org.neo4j.jdbc.cached-translations";
		if (meterRegistry.find(cachedTranslations).gauge() == null) {
			Gauge.builder(cachedTranslations, this.cachedTranslations::get).register(meterRegistry);
		}
	}

	/**
	 * Strips parameters away from the URL.
	 * @param jdbcUrl the URL to clean
	 * @return a parameter free URL
	 */
	static URI cleanURL(URI jdbcUrl) {
		var hlp = URI.create(jdbcUrl.getSchemeSpecificPart());
		try {
			var port = hlp.getPort();
			return new URI("jdbc",
					"%s://%s:%d%s".formatted(hlp.getScheme(), hlp.getHost(), (port != -1) ? port : 7687, hlp.getPath()),
					jdbcUrl.getFragment());
		}
		catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void connectionOpened(ConnectionOpenedEvent event) {
		var counter = this.openConnections.computeIfAbsent(cleanURL(event.uri()), url -> {
			var newCounter = new AtomicInteger();
			Gauge.builder("org.neo4j.jdbc.connections", newCounter::get)
				.description("The number of currently open connections")
				.tags("url", url.toString())
				.register(this.meterRegistry);
			return newCounter;
		});
		counter.incrementAndGet();
	}

	@Override
	public void connectionClosed(ConnectionClosedEvent event) {
		var counter = this.openConnections.get(cleanURL(event.uri()));
		if (counter != null) {
			counter.decrementAndGet();
		}
	}

	@Override
	public void statementCreated(StatementCreatedEvent event) {
		var counter = this.openStatements
			.computeIfAbsent(new StatementKey(cleanURL(event.uri()), event.statementType()), key -> {
				var newCounter = new AtomicInteger();
				Gauge.builder("org.neo4j.jdbc.statements", newCounter::get)
					.description("The number of currently open statements")
					.tags("url", key.uri().toString(), "type", key.type().getSimpleName())
					.register(this.meterRegistry);
				return newCounter;
			});
		counter.incrementAndGet();
	}

	@Override
	public void statementClosed(StatementClosedEvent event) {
		var counter = this.openStatements.get(new StatementKey(cleanURL(event.uri()), event.statementType()));
		if (counter != null) {
			counter.decrementAndGet();
		}
	}

	@Override
	public void translationCached(TranslationCachedEvent event) {
		this.cachedTranslations.set(event.cacheSize());
	}

	@Override
	public void executionEnded(ExecutionEndedEvent event) {
		var queryMetrics = this.queryMetrics.computeIfAbsent(cleanURL(event.uri()), url -> {
			var queries = "org.neo4j.jdbc.queries";
			var urlString = url.toString();
			return new QueryMetrics(
					Counter.builder(queries)
						.description("The total number of successful queries run")
						.tags("url", urlString, "state", "successful")
						.register(this.meterRegistry),
					Counter.builder(queries)
						.description("The total number of queries that failed to run")
						.tags("url", urlString, "state", "failed")
						.register(this.meterRegistry),
					Timer.builder(queries)
						.description("Duration of the queries being run")
						.tags("url", urlString)
						.register(this.meterRegistry));
		});
		if (event.state() == ExecutionEndedEvent.State.SUCCESSFUL) {
			queryMetrics.successfulQueries.increment();
		}
		else if (event.state() == ExecutionEndedEvent.State.FAILED) {
			queryMetrics.failedQueries.increment();
		}
		queryMetrics.queryTimer.record(event.elapsedTime());
	}

	record QueryMetrics(Counter successfulQueries, Counter failedQueries, Timer queryTimer) {
	}

	public record StatementKey(URI uri, Class<? extends Statement> type) {
	}

}
