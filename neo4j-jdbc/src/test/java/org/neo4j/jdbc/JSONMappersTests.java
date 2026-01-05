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
package org.neo4j.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JSONMappersTests {

	static boolean withDependencies() {
		return !withoutRequiredDependencies();
	}

	static boolean withoutRequiredDependencies() {
		return Boolean.getBoolean("withoutRequiredJSONDependencies");
	}

	@EnabledIf(value = "withDependencies",
			disabledReason = "Dependencies are not on the classpath, so we cannot test their presence")
	@ParameterizedTest
	@ValueSource(
			strings = { "com.fasterxml.jackson.databind.JsonNode", "com.fasterxml.jackson.databind.node.ObjectNode" })
	void shouldLoadJacksonTreeNodeMapper(String typeName) {
		var mapper = JSONMappers.INSTANCE.getMapper(typeName);
		assertThat(mapper).isNotEmpty();
	}

	// @DisabledIf("withJackson") just didn't call the method, so ¯\_(ツ)_/¯
	@EnabledIf(value = "withoutRequiredDependencies",
			disabledReason = "Dependencies are on the classpath, so we cannot test their absence")
	@Test
	void shouldGracefullyFail() {
		var handler = new CapturingHandler();
		var logger = Logger.getLogger("org.neo4j.jdbc.JSONMappers");
		logger.addHandler(handler);

		try {
			var mapper = JSONMappers.INSTANCE.getMapper("com.fasterxml.jackson.databind.JsonNode");
			assertThat(mapper).isEmpty();
			Assertions.assertThat(handler.messages)
				.contains("Could not load a mapper for com.fasterxml.jackson.databind.JsonNode");
		}
		finally {
			logger.removeHandler(handler);
		}
	}

	static class CapturingHandler extends Handler {

		List<String> messages = new ArrayList<>();

		@Override
		public void publish(LogRecord record) {
			this.messages.add(record.getMessage());
		}

		@Override
		public void flush() {

		}

		@Override
		public void close() throws SecurityException {

		}

	}

}
