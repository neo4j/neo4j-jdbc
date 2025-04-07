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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.jdbc.events.ConnectionListener;
import org.neo4j.jdbc.events.Neo4jEvent;
import org.neo4j.jdbc.events.ResultSetListener;
import org.neo4j.jdbc.events.StatementListener;
import org.neo4j.jdbc.tracing.Neo4jSpan;
import org.neo4j.jdbc.tracing.Neo4jTracer;

/**
 * A tracing mechanism based on the event system of the driver. It will listen on the
 * creation of connections, so that any statement created on them is traced. It will
 * listen on statements for the beginning and ending of executions and on result-sets to
 * listen to start and ending of iteration.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
final class Tracing implements ConnectionListener, StatementListener, ResultSetListener {

	private final Neo4jTracer tracer;

	private final Map<String, Neo4jSpan> executionSpans = new ConcurrentHashMap<>();

	private final Map<String, Neo4jSpan> resultSetIterationSpans = new ConcurrentHashMap<>();

	private final Map<String, String> defaultTags;

	Tracing(Neo4jTracer tracer, Neo4jConnection connection) {
		this.tracer = tracer;

		// See
		// https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md
		var uri = URI.create(connection.getDatabaseURL().getSchemeSpecificPart());
		this.defaultTags = Map.of("server.address", uri.getHost(), "server.port", Integer.toString(uri.getPort()),
				"db.system.name", "neo4j", "db.namespace", connection.getDatabaseName());
	}

	boolean usingSameTracer(Neo4jTracer tracer) {
		return this.tracer == tracer;
	}

	@Override
	public void onStatementCreated(StatementCreatedEvent event) {
		try {
			event.statement().unwrap(Neo4jStatement.class).addListener(this);
		}
		catch (SQLException ignored) {
		}
	}

	@Override
	public void onExecutionStarted(ExecutionStartedEvent event) {
		var method = switch (event.executionMode()) {
			case UPDATE -> "executeUpdate";
			case PLAIN -> "execute";
			case QUERY -> "executeQuery";
		};

		var type = event.statementType().getSimpleName();
		var tags = new HashMap<>(this.defaultTags);
		tags.putAll(Map.of("db.operation.name", String.format("%s#%s".formatted(type, method)), "db.query.text",
				event.statement()));
		this.executionSpans.put(event.id(), this.tracer.start("neo4j.jdbc %s".formatted(method), tags));
	}

	@Override
	public void onExecutionEnded(ExecutionEndedEvent event) {
		var span = this.executionSpans.remove(event.id());
		if (span != null) {
			span.end();
		}
	}

	@Override
	public void onIterationStarted(IterationStartedEvent event) {
		var tags = new HashMap<>(this.defaultTags);
		tags.put("db.operation.name", "ResultSet#next");
		this.resultSetIterationSpans.put(event.id(), this.tracer.start("neo4j.jdbc iterate result", tags));
	}

	@Override
	public void onIterationDone(IterationDoneEvent event) {
		var span = this.resultSetIterationSpans.remove(event.id());
		if (span != null) {
			span.end();
		}
	}

	@Override
	public void on(Neo4jEvent event) {
		Class<?> source = (Class<?>) event.payload().get("source");
		String id = (String) event.payload().get("id");
		if (id == null) {
			return;
		}

		Neo4jSpan span;
		if (Statement.class.isAssignableFrom(source)) {
			span = this.executionSpans.get(id);
		}
		else if (ResultSet.class.isAssignableFrom(source)) {
			span = this.resultSetIterationSpans.get(id);
		}
		else {
			return;
		}

		span.annotate(event.type().toString());
	}

}
