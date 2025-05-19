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
package org.neo4j.jdbc.translator.spi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A Cypher-backed view describes an entity recognizable by a {@link Translator}. The
 * translator might treat it as table and resolve it to an actual {@link View#query cypher
 * query}, embedded as {@code CALL{}} subquery.
 *
 * @param name the name of this view
 * @param query the Cypher query backing this view
 * @param columns the columns that are returned by the query defining this view inside
 * {@link java.sql.DatabaseMetaData}
 * @author Michael J. Simons
 * @since 6.5.0
 */
public record View(String name, String query, List<Column> columns) {

	/**
	 * Creates a new view definition.
	 * @param name the name of this view
	 * @param query the Cypher query backing this view
	 * @param columns the columns that are returned by the query defining this view inside
	 */
	public View {
		columns = List.copyOf(columns);
	}

	/**
	 * Creates a new column definition.
	 * @param name the name of the column
	 * @param propertyName the name of the graph property that is queried for the columns
	 * data
	 * @param type the Neo4j datatype of that column
	 * @return a new column definition
	 */
	public static Column column(String name, String propertyName, String type) {
		return new Column(name, propertyName, type);
	}

	/**
	 * Definition of a column of a view.
	 *
	 * @param name the name of the column
	 * @param propertyName the name of the graph property that is queried for the columns
	 * data
	 * @param type the Neo4j datatype of that column
	 */
	public record Column(String name, String propertyName, String type) {

		public Column {
			var msg = "Column name is required";
			if (Objects.requireNonNull(name, msg).isBlank()) {
				throw new IllegalArgumentException(msg);
			}
			propertyName = Optional.ofNullable(propertyName).filter(Predicate.not(String::isBlank)).orElse(name);
		}
	}
}
