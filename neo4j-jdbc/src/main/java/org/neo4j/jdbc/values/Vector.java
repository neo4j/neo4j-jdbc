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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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
 * <li>{@link ElementType#INTEGER} ({@code long})</li>
 * <li>{@link ElementType#FLOAT32} ({@code float})</li>
 * <li>{@link ElementType#FLOAT} ({@code double})</li>
 * </ul>
 * A vector is immutable, and all {@literal toXXXArray} methods will return a copy. Hence,
 * it is advised that you keep that copy around for as long as you need it and not
 * recreate it on every use. Constructions of {@link Vector instances} must go through the
 * appropriate factory methods in this interface.
 *
 * @author Michael J. Simons
 * @since 6.8.0
 */
public sealed interface Vector extends AsValue {

	/**
	 * The maximum size of a vector property supported by Neo4j as of Neo4j 2025.07.
	 */
	int MAX_VECTOR_SIZE = 4096;

	/**
	 * A flag to turn of upper bounds check. Recommended to use only as an escape hedge
	 * for in a scenario in which the Neo4j server increases its upper bounds, but it is
	 * unfeasible to update the JDBC driver.
	 */
	AtomicBoolean CHECK_UPPER_RANGE = new AtomicBoolean(true);

	/**
	 * {@return element-type of this vector}
	 */
	ElementType elementType();

	/**
	 * {@return the size of this vector}
	 */
	int size();

	/**
	 * This is an alias for {@link #size()}, aligning with the GQL compliant database
	 * function of the same name.
	 * @return the size of this vector
	 */
	default int vectorDimensionCount() {
		return size();
	}

	@Override
	default Value asValue() {
		return new VectorValue(this);
	}

	/**
	 * {@return a stream of the vectors elements}
	 */
	@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want here
	Stream<? extends Number> stream();

	private static void assertSize(int size) {

		if (size <= 0 || (size > MAX_VECTOR_SIZE && CHECK_UPPER_RANGE.get())) {
			throw new IllegalArgumentException(
					"'%d' is not a valid value. Must be a %s in the range %d to %d (GQL 42N31)".formatted(size,
							"number", 1, MAX_VECTOR_SIZE));
		}
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER8}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(byte[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Int8VectorImpl(ElementType.INTEGER8, elements.length,
				Arrays.copyOf(elements, elements.length));
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER16}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(short[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Int16VectorImpl(ElementType.INTEGER16, elements.length,
				Arrays.copyOf(elements, elements.length));
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER32}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(int[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Int32VectorImpl(ElementType.INTEGER32, elements.length,
				Arrays.copyOf(elements, elements.length));
	}

	/**
	 * Creates a vector composed of {@link ElementType#INTEGER}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(long[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Int64VectorImpl(ElementType.INTEGER, elements.length,
				Arrays.copyOf(elements, elements.length));
	}

	/**
	 * Creates a vector composed of {@link ElementType#FLOAT32}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(float[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Float32VectorImpl(ElementType.FLOAT32, elements.length,
				Arrays.copyOf(elements, elements.length));
	}

	/**
	 * Creates a vector composed of {@link ElementType#FLOAT}.
	 * @param elements the elements for the new vector
	 * @return a new {@link Vector}
	 */
	static Vector of(double[] elements) {
		assertSize(Objects.requireNonNull(elements, ArrayBasedVectors.MSG_NULL_CHECK).length);
		return new ArrayBasedVectors.Float64VectorImpl(ElementType.FLOAT, elements.length,
				Arrays.copyOf(elements, elements.length));
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
		 * Neo4j INTEGER64 type (Cypher CIP-200 normalises INTEGER64 to INTEGER, and we
		 * want the JDBC driver to be aligned here).
		 */
		INTEGER(long.class),
		/**
		 * Neo4j FLOAT32 type.
		 */
		FLOAT32(float.class),
		/**
		 * Neo4j FLOAT64 type (Cypher CIP-200 normalises FLOAT64 to FLOAT, and we want the
		 * JDBC driver to be aligned here).
		 */
		FLOAT(double.class);

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

	/**
	 * Represents a Neo4j INTEGER8 vector (A Neo4j INTEGER8 is equivalent to Javas
	 * <code>byte</code>).
	 */
	sealed interface Int8Vector extends Vector permits Int8VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code byte} values}
		 */
		byte[] toArray();

	}

	/**
	 * Represents a Neo4j INTEGER16 vector (A Neo4j INTEGER16 is equivalent to Javas
	 * <code>short</code>).
	 */
	sealed interface Int16Vector extends Vector permits Int16VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code short} values}
		 */
		short[] toArray();

	}

	/**
	 * Represents a Neo4j INTEGER32 vector (A Neo4j INTEGER32 is equivalent to Javas
	 * <code>int</code>).
	 */
	sealed interface Int32Vector extends Vector permits Int32VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code int} values}
		 */
		int[] toArray();

	}

	/**
	 * Represents a Neo4j INTEGER vector (A Neo4j Integer (or INTEGER) is equivalent to
	 * Javas <code>long</code>).
	 */
	sealed interface Int64Vector extends Vector permits Int64VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code long} values}
		 */
		long[] toArray();

	}

	/**
	 * Represents a Neo4j FLOAT32 vector (A Neo4j FLOAT32 is equivalent to Javas
	 * <code>float</code>).
	 */
	sealed interface Float32Vector extends Vector permits Float32VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code float} values}
		 */
		float[] toArray();

	}

	/**
	 * Represents a Neo4j FLOAT vector (A Neo4j FLOAT is equivalent to Javas
	 * <code>double</code>).
	 */
	sealed interface Float64Vector extends Vector permits Float64VectorImpl {

		/**
		 * {@return a copy of this vector as an array of Java {@code double} values}
		 */
		double[] toArray();

	}

}
