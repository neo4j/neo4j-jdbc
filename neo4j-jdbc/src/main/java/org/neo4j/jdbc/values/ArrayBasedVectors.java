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
package org.neo4j.jdbc.values;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.neo4j.jdbc.values.Vector.Float32Vector;
import org.neo4j.jdbc.values.Vector.Float64Vector;
import org.neo4j.jdbc.values.Vector.Int16Vector;
import org.neo4j.jdbc.values.Vector.Int32Vector;
import org.neo4j.jdbc.values.Vector.Int64Vector;
import org.neo4j.jdbc.values.Vector.Int8Vector;

final class ArrayBasedVectors {

	static final String MSG_NULL_CHECK = "Vector elements must not be literal null";

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
		var value = vector.stream().map(Number::toString).collect(Collectors.joining(", ", "[", "]"));
		return "vector(%s, %d, %s NOT NULL)".formatted(value, vector.size(), vector.elementType());
	}

	record Int8VectorImpl(ElementType elementType, int size, byte[] elements) implements Int8Vector {

		@Override
		public byte[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Byte> stream() {
			return IntStream.range(0, this.elements.length).mapToObj(i -> this.elements[i]);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Int8VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

	record Int16VectorImpl(ElementType elementType, int size, short[] elements) implements Int16Vector {

		@Override
		public short[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Short> stream() {
			return IntStream.range(0, this.elements.length).mapToObj(i -> this.elements[i]);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Int16VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

	record Int32VectorImpl(ElementType elementType, int size, int[] elements) implements Int32Vector {

		@Override
		public int[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Integer> stream() {
			return Arrays.stream(this.elements).boxed();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Int32VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

	record Int64VectorImpl(ElementType elementType, int size, long[] elements) implements Int64Vector {

		@Override
		public long[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Long> stream() {
			return Arrays.stream(this.elements).boxed();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Int64VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

	record Float32VectorImpl(ElementType elementType, int size, float[] elements) implements Float32Vector {

		@Override
		public float[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Float> stream() {
			return IntStream.range(0, this.elements.length).mapToObj(i -> this.elements[i]);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Float32VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

	record Float64VectorImpl(ElementType elementType, int size, double[] elements) implements Float64Vector {

		@Override
		public double[] toArray() {
			return Arrays.copyOf(this.elements, this.size);
		}

		@Override
		public Stream<Double> stream() {
			return Arrays.stream(this.elements).boxed();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (Float64VectorImpl) obj;
			return Objects.equals(this.elementType, that.elementType) && this.size == that.size
					&& Arrays.equals(this.elements, that.elements);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.elementType, this.size, Arrays.hashCode(this.elements));
		}

		@Override
		public String toString() {
			return ArrayBasedVectors.toString(this);
		}

	}

}
