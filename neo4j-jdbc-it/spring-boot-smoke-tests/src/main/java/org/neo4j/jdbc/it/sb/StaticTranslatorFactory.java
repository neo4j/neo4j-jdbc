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
package org.neo4j.jdbc.it.sb;

import java.sql.DatabaseMetaData;
import java.util.Map;

import org.neo4j.jdbc.translator.spi.Translator;
import org.neo4j.jdbc.translator.spi.TranslatorFactory;

/**
 * Provides only the static translator, without any config option.
 *
 * @author Michael J. Simons
 */
public final class StaticTranslatorFactory implements TranslatorFactory {

	@Override
	public Translator create(Map<String, ?> properties) {

		return new Translator() {

			@Override
			public String translate(String statement, DatabaseMetaData optionalDatabaseMetaData) {
				if ("People, gather!".equals(statement)) {
					return "MATCH (n:Person) RETURN n ORDER BY n.name";
				}
				else if ("Come here, ?".equals(statement)) {
					return "SELECT n FROM Person n WHERE n.name = ?";
				}
				throw new IllegalArgumentException("I'm sorry Dave, I'm afraid I can't do that");
			}

			@Override
			public int getOrder() {
				return 10;
			}
		};
	}

}
