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
package org.neo4j.jdbc.tracing.micrometer;

import io.micrometer.tracing.Tracer;
import org.neo4j.jdbc.tracing.Neo4jTracer;

/**
 * This is a factory class for bridging Neo4j tracing into Micrometer tracing.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public final class Neo4jTracingBridge {

	/**
	 * Creates a new {@link Neo4jTracer tracer} delegating to a Micrometer tracer.
	 * @param tracer the Micrometer tracer to delegate to
	 * @return a new Neo4j tracer
	 */
	public static Neo4jTracer to(Tracer tracer) {
		return new Neo4jTracerImpl(tracer);
	}

	private Neo4jTracingBridge() {
	}

}
