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

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

final class DelegatingSystemLogger implements System.Logger {

	private final Logger logger;

	DelegatingSystemLogger(Logger logger) {
		this.logger = Objects.requireNonNull(logger);
	}

	@Override
	public String getName() {
		return this.logger.getName();
	}

	@Override
	public boolean isLoggable(Level level) {
		return this.logger.isLoggable(map(level));
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if (thrown != null) {
			this.logger.log(map(level), msg, thrown);
		}
		else {
			this.logger.log(map(level), msg);
		}
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		this.logger.log(map(level), String.format(format, params));
	}

	private static java.util.logging.Level map(Level level) {
		return switch (level) {
			case ALL -> java.util.logging.Level.ALL;
			case TRACE -> java.util.logging.Level.FINER;
			case DEBUG -> java.util.logging.Level.FINE;
			case INFO -> java.util.logging.Level.INFO;
			case WARNING -> java.util.logging.Level.WARNING;
			case ERROR -> java.util.logging.Level.SEVERE;
			case OFF -> java.util.logging.Level.OFF;
		};
	}

}
