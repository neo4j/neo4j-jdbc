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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

/**
 * Collects various metrics per driver instance.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
final class MetricsCollectorImpl implements MetricsCollector {

	/**
	 * One instance per meter registry. All meters will be recreated if necessary per
	 * registry, so that they can safely be reset from the registry.
	 */
	private static final Map<MeterRegistry, MetricsCollector> INSTANCES = new ConcurrentHashMap<>();

	static MetricsCollector of(MeterRegistry meterRegistry) {
		return INSTANCES.computeIfAbsent(meterRegistry, MetricsCollectorImpl::new);
	}

	private final MeterRegistry meterRegistry;

	private final Map<URI, GaugeBackend> openConnections = new ConcurrentHashMap<>();

	private final Map<StatementKey, GaugeBackend> openStatements = new ConcurrentHashMap<>();

	private final GaugeBackend cachedTranslations = new GaugeBackend("org.neo4j.jdbc.cached-translations",
			"The number of cached statement translations");

	private MetricsCollectorImpl(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void onNewAuthentication(NewAuthenticationEvent event) {
		var uri = Events.cleanURL(event.uri()).toString();
		var authentications = "org.neo4j.jdbc.authentications";

		var counter = switch (event.state()) {
			case NEW -> getOrCreateCounter(authentications, List.of(Tag.of("uri", uri), Tag.of("state", "new")),
					"The total number of new authentications acquired");
			case REFRESHED ->
				getOrCreateCounter(authentications, List.of(Tag.of("uri", uri), Tag.of("state", "refreshed")),
						"The total number of authentications refreshed");
		};

		counter.increment();
	}

	@Override
	public void onConnectionOpened(ConnectionOpenedEvent event) {
		var gauge = this.openConnections.computeIfAbsent(Events.cleanURL(event.uri()),
				key -> new GaugeBackend("org.neo4j.jdbc.connections", "The number of currently open connections",
						Tag.of("uri", key.toString())));
		gauge.registerGauge(this.meterRegistry);
		gauge.increment();
	}

	@Override
	public void onConnectionClosed(ConnectionClosedEvent event) {
		var counterAndTags = this.openConnections.get(Events.cleanURL(event.uri()));
		if (counterAndTags != null) {
			counterAndTags.decrement();
		}
	}

	@Override
	public void onStatementCreated(StatementCreatedEvent event) {
		var gauge = this.openStatements.computeIfAbsent(
				new StatementKey(Events.cleanURL(event.uri()), event.statementType()),
				key -> new GaugeBackend("org.neo4j.jdbc.statements", "The number of currently open statements",
						Tag.of("uri", key.uri().toString()), Tag.of("type", key.type().getSimpleName())));
		gauge.registerGauge(this.meterRegistry);
		gauge.increment();
	}

	@Override
	public void onStatementClosed(StatementClosedEvent event) {
		var counterWithTags = this.openStatements
			.get(new StatementKey(Events.cleanURL(event.uri()), event.statementType()));
		if (counterWithTags != null) {
			counterWithTags.decrement();
		}
	}

	@Override
	public void onTranslationCached(TranslationCachedEvent event) {
		this.cachedTranslations.registerGauge(this.meterRegistry);
		this.cachedTranslations.set(event.cacheSize());
	}

	@Override
	public void onExecutionEnded(ExecutionEndedEvent event) {
		var uri = Events.cleanURL(event.uri()).toString();
		var queries = "org.neo4j.jdbc.queries";

		getOrCreateTimer(queries,
				List.of(Tag.of("uri", uri), Tag.of("state", event.state().name().toLowerCase(Locale.ROOT))),
				"Duration of the queries being run")
			.record(event.elapsedTime());
	}

	private Counter getOrCreateCounter(String name, List<Tag> tags, String description) {
		var counter = this.meterRegistry.find(name).tags(tags).counter();
		if (counter == null) {
			counter = Counter.builder(name).description(description).tags(tags).register(this.meterRegistry);
		}
		return counter;
	}

	private Timer getOrCreateTimer(String name, List<Tag> tags,
			@SuppressWarnings("SameParameterValue") String description) {
		var timer = this.meterRegistry.find(name).tags(tags).timer();
		if (timer == null) {
			timer = Timer.builder(name).description(description).tags(tags).register(this.meterRegistry);
		}
		return timer;
	}

	public record StatementKey(URI uri, Class<? extends Statement> type) {
	}

	public record GaugeBackend(String name, String description, AtomicInteger counter, List<Tag> tags) {

		GaugeBackend(String name, String description, Tag... tags) {
			this(name, description, new AtomicInteger(), List.of(tags));
		}

		void increment() {
			this.counter.incrementAndGet();
		}

		void decrement() {
			this.counter.decrementAndGet();
		}

		int get() {
			return this.counter.get();
		}

		void set(int newValue) {
			this.counter.set(newValue);
		}

		void registerGauge(MeterRegistry meterRegistry) {
			var meter = meterRegistry.find(this.name).tags(this.tags).gauge();
			if (meter != null) {
				return;
			}
			Gauge.builder(this.name, this::get).description(this.description).tags(this.tags).register(meterRegistry);
		}
	}

}
