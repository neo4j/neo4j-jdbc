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
package org.neo4j.jdbc.it.cp;

import java.util.Map;

import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;

public class FunkyFactory3 implements TranslatorFactory {

	static {
		// We need to of them, as we exercise the test twice in the same VM, and the
		// static
		// block will only rune once. This is a copy of FunkyFactory
		System.err.println("This could have been a Runtime.getRuntime().exec() call.");
	}

	@Override
	public Translator create(Map<String, ?> properties) {
		return (statement, optionalDatabaseMetaData) -> statement;
	}

}
