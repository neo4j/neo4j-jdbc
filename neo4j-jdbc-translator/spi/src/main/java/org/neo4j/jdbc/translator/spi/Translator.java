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
package org.neo4j.jdbc.translator.spi;

import java.sql.DatabaseMetaData;

/**
 * This is an SPI interface to be implemented by tooling that is able to translate queries
 * written in SQL or other languages and their parameters to Neo4j's own query langauge
 * Cypher.
 * <p>
 * Translation is straight forward: The translator that is provided by the
 * {@link TranslatorFactory} with the highest precedence will be invoked first. If this
 * translator is the only translator available, it either results in a successful
 * translation or not, in the latter case an {@link IllegalArgumentException} should be
 * thrown by {@link #translate(String, DatabaseMetaData)}. If there is only one
 * translator, the result of the translation will be returned or the
 * {@link IllegalArgumentException} will be caught and rethrown as a
 * {@link java.sql.SQLException}, containing the unwrapped cause of the original exception
 * if there is a cause, otherwise the exception thrown.
 * <p>
 * If there is more than one translator available the flow is as follows: In case in which
 * a preceding translator cannot translate a statement and throws an
 * {@link IllegalArgumentException}, that exception will be caught. The translation so far
 * or the original statement in the case of the failure of the first or all preceding
 * translators will then be passed to the next translator. As long as one translator is
 * able to translate the statement, that result will be passed to the Neo4j instance.
 * <p>
 *
 * Much of the algorithm and configuration about ordering {@link TranslatorFactory
 * translator factories} is taken from Spring's <a href=
 * "https://github.com/spring-projects/spring-framework/blob/v6.1.4/spring-core/src/main/java/org/springframework/core/Ordered.java">Ordered.java</a>
 * interface and related infrastructure. Thank you.
 *
 * @author Michael J. Simons
 * @since 6.0.0
 */
@FunctionalInterface
public interface Translator {

	/**
	 * Useful constant for the highest precedence value.
	 * @see java.lang.Integer#MIN_VALUE
	 */
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/**
	 * Useful constant for the lowest precedence value.
	 * @see java.lang.Integer#MAX_VALUE
	 */
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

	/**
	 * This method can be overwritten if the translator supports caching.
	 */
	default void flushCache() {
	}

	/**
	 * Translate the given statement into a Neo4j native query or an intermediate format
	 * that needs further processing by other translators.
	 * @param statement the statement. Must not be {@literal null} and must be a valid
	 * statement.
	 * @return a Neo4j native query
	 * @throws NullPointerException if {@code statement} is {@literal null}
	 * @throws IllegalArgumentException if {@code statement} cannot be translated by this
	 * translator
	 * @see #translate(String, DatabaseMetaData)
	 */
	default String translate(String statement) {
		return translate(statement, null);
	}

	/**
	 * Translate the given statement into a Neo4j native query or an intermediate format
	 * that needs further processing by other translators. This method might behave
	 * different and more optimized when {@link DatabaseMetaData database meta-data} is
	 * available.
	 * @param statement the SQL query. Must not be {@literal null} and must be a valid
	 * statement.
	 * @param optionalDatabaseMetaData optional {@link DatabaseMetaData} that might be
	 * used to further refine translations, can safely be left {@literal null}
	 * @return a Neo4j native query
	 * @throws NullPointerException if {@code statement} is {@literal null}
	 * @throws IllegalArgumentException if {@code statement} cannot be translated by this
	 * translator
	 */
	String translate(String statement, DatabaseMetaData optionalDatabaseMetaData);

	/**
	 * Get the order value of this object.
	 * <p>
	 * Higher values are interpreted as lower precedence. As a consequence, the object
	 * with the lowest value has the highest precedence.
	 * <p>
	 * Same order values will result in arbitrary sort positions for the affected objects.
	 * <p>
	 * The default implementation has the lowest precedence possible.
	 * @return the order value
	 * @see #HIGHEST_PRECEDENCE
	 * @see #LOWEST_PRECEDENCE
	 */
	default int getOrder() {
		return LOWEST_PRECEDENCE;
	}

}
