/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
package org.neo4j.driver.jdbc.translator.spi;

import java.sql.DatabaseMetaData;

/**
 * This is an SPI interface to be implemented by tooling that is able to translate queries
 * written in SQL and their parameters to Neo4j's own query langauge Cypher.
 *
 * @author Michael J. Simons
 * @since 1.0.0
 */
@FunctionalInterface
public interface SqlTranslator {

	/**
	 * Translate the given SQL query into a Neo4j native query.
	 * @param sql the SQL query. Must not be {@literal null} and must be a valid
	 * statement.
	 * @return a Neo4j native query
	 * @throws NullPointerException if {@code sql} is {@literal null}
	 * @throws IllegalArgumentException if {@code sql} cannot be translated to a Neo4j
	 * native statement by the implementor
	 * @see #translate(String, DatabaseMetaData)
	 */
	default String translate(String sql) {
		return translate(sql, null);
	}

	/**
	 * Translate the given SQL query into a Neo4j native query. This method might behave
	 * different and more optimized when {@link DatabaseMetaData database meta-data} is
	 * available.
	 * @param sql the SQL query. Must not be {@literal null} and must be a valid
	 * statement.
	 * @param optionalDatabaseMetaData optional {@link DatabaseMetaData} that might be
	 * used to further refine translations, can safely be left {@literal null}
	 * @return a Neo4j native query
	 * @throws NullPointerException if {@code sql} is {@literal null}
	 * @throws IllegalArgumentException if {@code sql} cannot be translated to a Neo4j
	 * native statement by the implementor
	 */
	String translate(String sql, DatabaseMetaData optionalDatabaseMetaData);

}
