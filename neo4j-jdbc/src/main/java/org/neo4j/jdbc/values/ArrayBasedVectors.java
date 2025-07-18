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

import java.util.Objects;
import java.util.function.Function;

import org.neo4j.jdbc.values.Vector.Float32Vector;
import org.neo4j.jdbc.values.Vector.Float64Vector;
import org.neo4j.jdbc.values.Vector.Int16Vector;
import org.neo4j.jdbc.values.Vector.Int32Vector;
import org.neo4j.jdbc.values.Vector.Int64Vector;
import org.neo4j.jdbc.values.Vector.Int8Vector;

final class ArrayBasedVectors {

	private ArrayBasedVectors() {
	}

	@SuppressWarnings("unused")
	static final Function<Vector, Object> ELEMENT_ACCESSOR = vector -> {
		// The field is reflected upon (hence the suppression).
		Objects.requireNonNull(vector);

		if (vector instanceof Int8VectorImpl int8Vector) {
			return int8Vector.elements();
		}
		else if (vector instanceof Int16VectorImpl v) {
			return v.elements();
		}
		else if (vector instanceof Int32VectorImpl v) {
			return v.elements();
		}
		else if (vector instanceof Int64VectorImpl v) {
			return v.elements();
		}
		else if (vector instanceof Float32VectorImpl v) {
			return v.elements();
		}
		else if (vector instanceof Float64VectorImpl v) {
			return v.elements();
		}

		throw new IllegalArgumentException("Unsupported vector implementation: " + vector.getClass().getName());
	};

	static String toString(Vector vector) {
		return "VECTOR<%s>(%d)".formatted(vector.elementType(), vector.length());
	}

	record Int8VectorImpl(ElementType elementType, int length, byte[] elements) implements Int8Vector {
		@Override
		public byte[] toArray() {
			var result = new byte[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}

	record Int16VectorImpl(ElementType elementType, int length, short[] elements) implements Int16Vector {
		@Override
		public short[] toArray() {
			var result = new short[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}

	record Int32VectorImpl(ElementType elementType, int length, int[] elements) implements Int32Vector {
		@Override
		public int[] toArray() {
			var result = new int[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}

	record Int64VectorImpl(ElementType elementType, int length, long[] elements) implements Int64Vector {
		@Override
		public long[] toArray() {
			var result = new long[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}

	record Float32VectorImpl(ElementType elementType, int length, float[] elements) implements Float32Vector {
		@Override
		public float[] toArray() {
			var result = new float[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}

	record Float64VectorImpl(ElementType elementType, int length, double[] elements) implements Float64Vector {
		@Override
		public double[] toArray() {
			var result = new double[this.length()];
			System.arraycopy(this.elements, 0, result, 0, this.length());
			return result;
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}
	}
}
