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
package org.neo4j.jdbc.translator.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;
import org.jooq.impl.QOM;

/**
 * Registry that maps jOOQ field expressions to their WITH clause aliases. Supports two
 * lookup modes: structural matching (for aggregate expressions) and name-based matching
 * (for alias references that jOOQ does not resolve).
 *
 * @author Ryan Knight
 * @author Michael J. Simons
 */
final class AliasRegistry {

	private final List<AliasEntry> entries = new ArrayList<>();

	private final Map<String, String> aliasByName = new HashMap<>();

	/**
	 * Registers a field expression with its alias. The field is unwrapped from any
	 * {@link QOM.FieldAlias} wrapper before storage. First registration wins if the same
	 * expression is registered twice.
	 * @param field the field expression
	 * @param alias the alias string
	 */
	void register(Field<?> field, String alias) {
		var unwrapped = unwrap(field);
		this.entries.add(new AliasEntry(unwrapped, alias));
		this.aliasByName.putIfAbsent(alias.toUpperCase(), alias);
	}

	/**
	 * Looks up the alias for a given field. Tries structural matching first (for
	 * aggregates), then falls back to name-based matching (for unresolved alias
	 * references like {@code HAVING cnt > 5} or {@code ORDER BY cnt}). Returns
	 * {@code null} if no match is found.
	 * @param field the field to look up
	 * @return the alias, or {@code null} if no match is found
	 */
	String resolve(Field<?> field) {
		var unwrapped = unwrap(field);

		// 1. Structural matching against all registered entries
		var equivalence = new FieldEquivalencePredicate();
		for (var entry : this.entries) {
			if (equivalence.test(entry.field(), unwrapped)) {
				return entry.alias();
			}
		}

		// 2. Name-based matching (field.getName() against registered alias strings)
		var nameKey = unwrapped.getName().toUpperCase();
		return this.aliasByName.get(nameKey);
	}

	private static Field<?> unwrap(Field<?> field) {
		if (field instanceof QOM.FieldAlias<?> fa) {
			return fa.$field();
		}
		return field;
	}

	private record AliasEntry(Field<?> field, String alias) {
	}

}
