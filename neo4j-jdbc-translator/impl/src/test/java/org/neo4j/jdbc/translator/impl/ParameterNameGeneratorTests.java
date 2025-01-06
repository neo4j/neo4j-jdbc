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
package org.neo4j.jdbc.translator.impl;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterNameGeneratorTests {

	@Test
	void shouldGenerateProperSequence() {

		var generator = new ParameterNameGenerator();
		var names = new ArrayList<String>();
		names.add(generator.newIndex());
		names.add(generator.newIndex("0"));
		names.add(generator.newIndex("1"));
		names.add(generator.newIndex());
		names.add(generator.newIndex());
		names.add(generator.newIndex("foobar"));
		names.add(generator.newIndex("6"));
		names.add(generator.newIndex("7"));
		names.add(generator.newIndex("1"));
		names.add(generator.newIndex());
		assertThat(names).containsExactly("1", "2", "3", "4", "5", "foobar", "7", "8", "9", "10");
	}

}
