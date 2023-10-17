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
package org.neo4j.driver.jdbc.internal.bolt.internal.value;

import java.util.Objects;

import org.neo4j.driver.jdbc.internal.bolt.internal.types.InternalTypeSystem;
import org.neo4j.driver.jdbc.internal.bolt.types.Type;

public abstract sealed class BooleanValue extends ValueAdapter {

	private BooleanValue() {
		// do nothing
	}

	public static final BooleanValue TRUE = new TrueValue();

	public static final BooleanValue FALSE = new FalseValue();

	public static BooleanValue fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public abstract Boolean asObject();

	@Override
	public Type type() {
		return InternalTypeSystem.TYPE_SYSTEM.BOOLEAN();
	}

	private static final class TrueValue extends BooleanValue {

		@Override
		public Boolean asObject() {
			return Boolean.TRUE;
		}

		@Override
		public boolean asBoolean() {
			return true;
		}

		@Override
		public boolean isTrue() {
			return true;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object obj) {
			return obj == TRUE;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(TRUE);
		}

		@Override
		public String toString() {
			return "TRUE";
		}

	}

	private static final class FalseValue extends BooleanValue {

		@Override
		public Boolean asObject() {
			return Boolean.FALSE;
		}

		@Override
		public boolean asBoolean() {
			return false;
		}

		@Override
		public boolean isFalse() {
			return true;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object obj) {
			return obj == FALSE;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(FALSE);
		}

		@Override
		public String toString() {
			return "FALSE";
		}

	}

}
