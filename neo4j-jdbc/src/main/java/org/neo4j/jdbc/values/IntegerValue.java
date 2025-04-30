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
package org.neo4j.jdbc.values;

/**
 * The Cypher type {@code INTEGER} maps to a Java {@link Long}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class IntegerValue extends AbstractNumberValue<Long> {

	private final long val;

	IntegerValue(long val) {
		this.val = val;
	}

	@Override
	public Type type() {
		return Type.INTEGER;
	}

	@Override
	public Long asNumber() {
		return this.val;
	}

	@Override
	public long asLong() {
		return this.val;
	}

	@Override
	public int asInt() {
		if (this.val > Integer.MAX_VALUE || this.val < Integer.MIN_VALUE) {
			throw new LossyCoercion(type().name(), "Java int");
		}
		return (int) this.val;
	}

	@Override
	public double asDouble() {
		var doubleVal = (double) this.val;
		if ((long) doubleVal != this.val) {
			throw new LossyCoercion(type().name(), "Java double");
		}

		return this.val;
	}

	@Override
	public float asFloat() {
		return this.val;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var values = (IntegerValue) o;
		return this.val == values.val;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.val);
	}

	@Override
	public String toString() {
		return Long.toString(this.val);
	}

}
