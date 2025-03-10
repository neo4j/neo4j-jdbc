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
package org.neo4j.jdbc.internal.bolt;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.neo4j.bolt.connection.LoggingProvider;

/**
 * Adapts JUL logging to System.Logger, which is what is required by the Bolt-Connection
 * module.
 *
 * @author Neo4j Drivers Team
 */
final class LoggingProviderImpl implements LoggingProvider {

	private static final String PREFIX = "org.neo4j.jdbc.network";

	@Override
	public System.Logger getLog(Class<?> cls) {
		return new JULBridge(getLogger(cls.getSimpleName()));
	}

	@Override
	public System.Logger getLog(String name) {
		return new JULBridge(getLogger(name));
	}

	private static Logger getLogger(String name) {
		return Logger.getLogger("%s.%s".formatted(PREFIX, name));
	}

	static final class JULBridge implements System.Logger {

		private final Logger logger;

		JULBridge(Logger logger) {
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
			log0(level, bundle, msg, thrown);
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String format, Object... params) {
			log0(level, bundle, format, null, params);
		}

		private void log0(Level level, ResourceBundle bundle, String format, Throwable thrown, Object... params) {
			this.logger.log(map(level), thrown, () -> {
				var formatOrMsg = getString(bundle, format);
				return MessageFormat.format(formatOrMsg, params).formatted(params);
			});
		}

		private static String getString(ResourceBundle bundle, String key) {
			if (bundle == null || key == null) {
				return key;
			}
			try {
				return bundle.getString(key);
			}
			catch (MissingResourceException ex) {
				return key;
			}
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

}
