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

import java.util.function.BiPredicate;

import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.QOM;

/**
 * Structural field equivalence matcher for jOOQ QOM fields. Determines whether two jOOQ
 * Field objects represent the same expression, ignoring alias wrappers. Used by the alias
 * registry for GROUP BY, HAVING, and ORDER BY alias resolution.
 *
 * @author Ryan Knight
 * @author Michael J. Simons
 */
final class FieldEquivalencePredicate implements BiPredicate<Field<?>, Field<?>> {

	/**
	 * Determines whether two jOOQ {@link Field} objects represent the same expression.
	 * Alias wrappers are stripped before comparison. Supports aggregate functions
	 * ({@code count}, {@code sum}, {@code min}, {@code max}, {@code avg},
	 * {@code stddev_samp}, {@code stddev_pop}), simple column references (with or without
	 * table qualifiers), and combinations thereof.
	 * @param a the first field
	 * @param b the second field
	 * @return {@code true} if the fields are structurally equivalent
	 */
	@Override
	public boolean test(Field<?> a, Field<?> b) {
		if (a == null || b == null) {
			return false;
		}

		// Case 1: Alias unwrapping — aliases are presentation, not identity
		if (a instanceof QOM.FieldAlias<?> fa) {
			if (test(fa.$field(), b)) {
				return true;
			}
			else if (fa.$field() instanceof TableField<?, ?> tfa && b instanceof TableField<?, ?> tfb
					&& (tfa.getTable() == null || tfb.getTable() == null)) {
				return a.getName().equalsIgnoreCase(b.getName());
			}
			else {
				return false;
			}
		}
		if (b instanceof QOM.FieldAlias<?> fb) {
			if (test(a, fb.$field())) {
				return true;
			}
			else if (a instanceof TableField<?, ?> tfa && fb.$field() instanceof TableField<?, ?> tfb
					&& (tfa.getTable() == null || tfb.getTable() == null)) {
				return a.getName().equalsIgnoreCase(b.getName());
			}
			else {
				return false;
			}
		}

		// Case 2: Aggregate function matching
		if (a instanceof QOM.Count ca && b instanceof QOM.Count cb) {
			if (ca.$distinct() != cb.$distinct()) {
				return false;
			}
			return test(ca.$field(), cb.$field());
		}
		if (a instanceof QOM.Sum sa && b instanceof QOM.Sum sb) {
			return test(sa.$field(), sb.$field());
		}
		if (a instanceof QOM.Min<?> ma && b instanceof QOM.Min<?> mb) {
			return test(ma.$field(), mb.$field());
		}
		if (a instanceof QOM.Max<?> ma && b instanceof QOM.Max<?> mb) {
			return test(ma.$field(), mb.$field());
		}
		if (a instanceof QOM.Avg aa && b instanceof QOM.Avg ab) {
			return test(aa.$field(), ab.$field());
		}
		if (a instanceof QOM.StddevSamp sa && b instanceof QOM.StddevSamp sb) {
			return test(sa.$field(), sb.$field());
		}
		if (a instanceof QOM.StddevPop sa && b instanceof QOM.StddevPop sb) {
			return test(sa.$field(), sb.$field());
		}

		// Case 3: Simple column references
		// Handle the count(*) asterisk case — jOOQ represents this as an SQLField
		// with toString() = "*", not as org.jooq.Asterisk
		if ("*".equals(a.toString()) && "*".equals(b.toString())) {
			return true;
		}

		// jOOQ's parser produces TableFieldImpl for all column references.
		// When a column is table-qualified (e.g. c.name), getTable() returns
		// the table/alias; when unqualified (e.g. name), getTable() is null.
		var tableA = (a instanceof TableField<?, ?> tfa) ? tfa.getTable() : null;
		var tableB = (b instanceof TableField<?, ?> tfb) ? tfb.getTable() : null;

		if (tableA != null && tableB != null) {
			// Both have table qualifiers — compare table name AND column name
			return tableA.getName().equalsIgnoreCase(tableB.getName()) && a.getName().equalsIgnoreCase(b.getName());
		}

		// At least one has no table qualifier — match on column name alone
		// (pragmatic for single-table queries or mixed qualified/unqualified)
		return a.getName().equalsIgnoreCase(b.getName());
	}

}
