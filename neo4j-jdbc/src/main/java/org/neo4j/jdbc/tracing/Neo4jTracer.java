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
package org.neo4j.jdbc.tracing;

import java.util.Map;

/**
 * This interface is used by the Neo4j JDBC tracing to turn events provided by the driver
 * into traces and spans that can be worked on. The default implementation is shipped from
 * the neo4j-jdbc-tracing jar.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
public interface Neo4jTracer {

	/**
	 * Starts a new span in the current thread. If there's already an ongoing span in the
	 * same thread, it shall start the new span as a child of the ongoing one. The new
	 * span is expected to be started and put into context.
	 * @param name the name of the span to start
	 * @param tags optional list of tags, should be {@literal null} safe
	 * @return a new span
	 */
	Neo4jSpan start(String name, Map<String, String> tags);

}
