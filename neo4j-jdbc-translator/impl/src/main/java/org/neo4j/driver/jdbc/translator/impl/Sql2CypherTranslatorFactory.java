/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.translator.impl;

import java.util.Map;

import org.neo4j.driver.jdbc.translator.spi.SqlTranslator;
import org.neo4j.driver.jdbc.translator.spi.SqlTranslatorFactory;

/**
 * Factory implementation for the jOOQ X Cypher-DSL based implementation of a
 * {@link SqlTranslator}.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
public final class Sql2CypherTranslatorFactory implements SqlTranslatorFactory {

	@Override
	public SqlTranslator create(Map<String, String> config) {
		return Sql2Cypher.with(Sql2CypherConfig.of(config));
	}

	@Override
	public String getName() {
		return "Default SQL to Cypher translator";
	}

}
