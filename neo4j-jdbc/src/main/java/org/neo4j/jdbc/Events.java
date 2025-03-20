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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Some internal utilities around events.
 *
 * @author Michael J. Simons
 * @since 6.3.0
 */
final class Events {

	private static final Logger LOGGER = Logger.getLogger("org.neo4j.jdbc.events");

	static <T> void notify(Collection<T> listeners, Consumer<T> consumer) {
		listeners.forEach(listener -> {
			try {
				consumer.accept(listener);
			}
			catch (Exception ex) {
				LOGGER.log(Level.WARNING, ex,
						() -> "Could not notify listener %s".formatted(listener.getClass().getCanonicalName()));
			}
		});
	}

	private Events() {
	}

}
