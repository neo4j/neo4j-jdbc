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
package org.neo4j.jdbc.values;

/**
 * The Cypher type {@code FLOAT} maps to a Java {@link Double}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class FloatValue extends AbstractNumberValue<Double> {

	private final double val;

	FloatValue(double val) {
		this.val = val;
	}

	@Override
	public Type type() {
		return Type.FLOAT;
	}

	@Override
	public Double asNumber() {
		return this.val;
	}

	@Override
	public long asLong() {
		var longVal = (long) this.val;
		if (Double.compare(longVal, this.val) != 0) {
			throw new LossyCoercion(type().name(), "Java long");
		}

		return longVal;
	}

	@Override
	public int asInt() {
		var intVal = (int) this.val;
		if (Double.compare(intVal, this.val) != 0) {
			throw new LossyCoercion(type().name(), "Java int");
		}

		return intVal;
	}

	@Override
	public double asDouble() {
		return this.val;
	}

	@Override
	public float asFloat() {
		var floatVal = (float) this.val;
		if (Double.compare(floatVal, this.val) != 0) {
			throw new LossyCoercion(type().name(), "Java float");
		}

		return floatVal;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var values = (FloatValue) o;
		return Double.compare(values.val, this.val) == 0;
	}

	@Override
	public int hashCode() {
		var temp = Double.doubleToLongBits(this.val);
		return (int) (temp ^ (temp >>> 32));
	}

	@Override
	public String toString() {
		return Double.toString(this.val);
	}

}
