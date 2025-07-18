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

import org.neo4j.jdbc.values.ArrayBasedVectors.Float32VectorImpl;
import org.neo4j.jdbc.values.ArrayBasedVectors.Float64VectorImpl;
import org.neo4j.jdbc.values.ArrayBasedVectors.Int16VectorImpl;
import org.neo4j.jdbc.values.ArrayBasedVectors.Int32VectorImpl;
import org.neo4j.jdbc.values.ArrayBasedVectors.Int64VectorImpl;
import org.neo4j.jdbc.values.ArrayBasedVectors.Int8VectorImpl;

/**
 * This type represents a Neo4j {@code Vector}. A Neo4j vector is a vector in the
 * mathematical sense composed of a uniform element-type and a fixed length. The
 * element-type of a vector can be of the following types (the types in parentheses are
 * the Java types to which the Neo4j types are mapped):
 * <ul>
 * <li>{@link ElementType#INTEGER8} ({@code byte})</li>
 * <li>{@link ElementType#INTEGER16} ({@code short})</li>
 * <li>{@link ElementType#INTEGER32} ({@code int})</li>
 * <li>{@link ElementType#INTEGER64} ({@code long})</li>
 * <li>{@link ElementType#FLOAT32} ({@code float})</li>
 * <li>{@link ElementType#FLOAT64} ({@code double})</li>
 * </ul>
 * A vector is immutable, and all {@literal toXXXArray} methods will return a copy. Hence,
 * it is advised that you keep that copy around for as long as you need it and not
 * recreate it on every use. Constructions of {@link Vector instances} must go through the
 * appropriate factory methods in this interface.
 *
 * @author Michael J. Simons
 * @since 6.8.0
 */
public sealed interface Vector {

	/**
	 * {@return element-type of this vector}
	 */
	ElementType elementType();

	/**
	 * {@return the length of this vector}
	 */
	int length();

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER8}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector int8(byte[] elements) {
		return new ArrayBasedVectors.Int8VectorImpl(ElementType.INTEGER8, elements.length, elements);
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER16}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector int16(short[] elements) {
		return new ArrayBasedVectors.Int16VectorImpl(ElementType.INTEGER16, elements.length, elements);
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER32}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector int32(int[] elements) {
		return new ArrayBasedVectors.Int32VectorImpl(ElementType.INTEGER32, elements.length, elements);
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER64}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector int64(long[] elements) {
		return new ArrayBasedVectors.Int64VectorImpl(ElementType.INTEGER64, elements.length, elements);
	}

	/**
	 * Creates a vector composed of {@link ElementType#FLOAT32}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector float32(float[] elements) {
		return new ArrayBasedVectors.Float32VectorImpl(ElementType.FLOAT32, elements.length, elements);
	}

	/**
	 * Creates a vector composed of {@link ElementType#FLOAT64}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector float64(double[] elements) {
		return new ArrayBasedVectors.Float64VectorImpl(ElementType.FLOAT64, elements.length, elements);
	}

	/**
	 * This enum describes the element-type of a {@link VectorValue} and the corresponding
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
			return this.javaType;
		}

	}

	sealed interface Int8Vector extends Vector permits Int8VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code byte} values}
		 */
		byte[] toArray();

	}

	sealed interface Int16Vector extends Vector permits Int16VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code short} values}
		 */
		short[] toArray();

	}

	sealed interface Int32Vector extends Vector permits Int32VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code int} values}
		 */
		int[] toArray();

	}

	sealed interface Int64Vector extends Vector permits Int64VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code long} values}
		 */
		long[] toArray();

	}

	sealed interface Float32Vector extends Vector permits Float32VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code float} values}
		 */
		float[] toArray();

	}

	sealed interface Float64Vector extends Vector permits Float64VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code double} values}
		 */
		double[] toArray();

	}

}
