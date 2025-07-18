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
 * This {@link Value} represents a Neo4j {@code Vector}. A Neo4j vector is a vector in the
 * mathematical sense composed of a uniform element-type and a fixed length. The
 * element-type of a vector can be of the following types (the types in parentheses are
 * the Java types to which the Neo4j types are mapped)::
 * <ul>
 * <li>{@link ElementType#INTEGER8} ({@code byte})</li>
 * <li>{@link ElementType#INTEGER16} ({@code short})</li>
 * <li>{@link ElementType#INTEGER32} ({@code int})</li>
 * <li>{@link ElementType#INTEGER64} ({@code long})</li>
 * <li>{@link ElementType#FLOAT32} ({@code float})</li>
 * <li>{@link ElementType#FLOAT64} ({@code double})</li>
 * </ul>
 * A {@link VectorValue} is immutable, and all {@literal toXXXArray} methods will return a
 * copy. Hence, it is advised that you keep that copy around for as long as you need it
 * and not recreate it on every use. Constructions of {@link VectorValue instances} must
 * go through the appropriate factory methods in this interface.
 *
 * @since 6.8.0
 */
public sealed interface VectorValue extends Value {

	/**
	 * This enum describes the element type of a {@link VectorValue} and the corresponding
	 * Java type.
	 */
	enum ElementType {

		/**
		 * Neo4j INTEGER8 type.
		 */
		INTEGER8(byte.class),
		/**
		 * Neo4j INTEGER16 type.
		 */
		INTEGER16(short.class),
		/**
		 * Neo4j INTEGER32 type.
		 */
		INTEGER32(int.class),
		/**
		 * Neo4j INTEGER64 type.
		 */
		INTEGER64(long.class),
		/**
		 * Neo4j FLOAT32 type.
		 */
		FLOAT32(float.class),
		/**
		 * Neo4j FLOAT64 type.
		 */
		FLOAT64(double.class);

		private final Class<?> javaType;

		ElementType(Class<?> javaType) {
			this.javaType = javaType;
		}

		/**
		 * {@return the Java type to which elements of a vector are mapped}
		 */
		public Class<?> getJavaType() {
			return javaType;
		}

	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER64}.
	 * @param elements the elements for the new vector
	 * @return a new {@link VectorValue}
	 */
	static VectorValue int64(long[] elements) {
		return new Int64VectorValue(elements);
	}

	/**
	 * {@return the length of this vector}
	 */
	int length();

	/**
	 * {@return element-type of this vector}
	 */
	ElementType elementType();

	final class Int8VectorValue extends AbstractVectorValue implements VectorValue {

		private Int8VectorValue(byte[] elements) {
			super(byte.class, elements);
		}

	}

	final class Int16VectorValue extends AbstractVectorValue implements VectorValue {

		private Int16VectorValue(short[] elements) {
			super(short.class, elements);
		}

	}

	final class Int32VectorValue extends AbstractVectorValue implements VectorValue {

		private Int32VectorValue(int[] elements) {
			super(int.class, elements);
		}

	}

	/**
	 * A {@link VectorValue} consisting of {@link ElementType#INTEGER64} elements.
	 */
	final class Int64VectorValue extends AbstractVectorValue implements VectorValue {

		private Int64VectorValue(long[] elements) {
			super(long.class, elements);
		}

		/**
		 * {@return a copy of this vector as an array of Java {@code long} values.
		 */
		public long[] toLongArray() {
			return toArray();
		}

	}

	final class Float64VectorValue extends AbstractVectorValue implements VectorValue {

		private Float64VectorValue(double[] elements) {
			super(double.class, elements);
		}

	}

	final class Float32VectorValue extends AbstractVectorValue implements VectorValue {

		private Float32VectorValue(float[] elements) {
			super(float.class, elements);
		}

	}

}
