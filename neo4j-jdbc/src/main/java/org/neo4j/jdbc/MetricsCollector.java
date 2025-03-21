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

import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.jdbc.events.ConnectionClosedEvent;
import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.ConnectionOpenedEvent;
import org.neo4j.jdbc.events.DriverListener;
import org.neo4j.jdbc.events.ExecutionEndedEvent;
import org.neo4j.jdbc.events.ExecutionStartedEvent;
import org.neo4j.jdbc.events.StatementClosedEvent;
import org.neo4j.jdbc.events.StatementCreatedEvent;
import org.neo4j.jdbc.events.StatementListener;
import org.neo4j.jdbc.events.TranslationCachedEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;

/**
 * Collects various metrics per driver instance.
 */
final class MetricsCollector implements DriverListener, ConnectionListener, StatementListener {

	private final AtomicInteger openConnections = new AtomicInteger();

	private final AtomicInteger openStatements = new AtomicInteger();

	private final AtomicInteger successFullStatements = new AtomicInteger();

	private final AtomicInteger failedStatements = new AtomicInteger();

	@Override
	public void connectionOpened(ConnectionOpenedEvent event) {
		this.openConnections.incrementAndGet();
	}

	@Override
	public void connectionClosed(ConnectionClosedEvent event) {
		this.openConnections.decrementAndGet();
	}

	@Override
	public void statementCreated(StatementCreatedEvent event) {
		this.openStatements.incrementAndGet();
	}

	@Override
	public void statementClosed(StatementClosedEvent event) {
		this.openStatements.decrementAndGet();
	}

	@Override
	public void translationCached(TranslationCachedEvent event) {
	}

	@Override
	public void executionStarted(ExecutionStartedEvent event) {

	}

	@Override
	public void executionEnded(ExecutionEndedEvent event) {
		if (event.state() == ExecutionEndedEvent.State.SUCCESSFUL) {
			this.successFullStatements.incrementAndGet();
		}
		else if (event.state() == ExecutionEndedEvent.State.FAILED) {
			this.failedStatements.incrementAndGet();
		}

	}

}
