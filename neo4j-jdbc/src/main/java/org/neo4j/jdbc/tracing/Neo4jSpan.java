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
package org.neo4j.jdbc.tracing;

import java.util.Map;

/**
 * This interface is meant to be a small abstraction over spans, with enough functionality
 * needed in the core of the driver. A span is meant to be started through
 * {@link Neo4jTracer#start(String, Map)}. The span must be active and in scope until
 * {@link #end()} has been called on the span. This shall remove this span from scope and
 * end it.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface Neo4jSpan {

	/**
	 * Annotates a point in time on this span.
	 * @param name the name of the event pointing to in the new annotation
	 */
	void annotate(String name);

	/**
	 * Removes the span from scope and ends it.
	 */
	void end();

}
