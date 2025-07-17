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
 * @since 6.8.0
 */
public sealed interface VectorValue extends Value {

	static VectorValue longVector(long[] elements) {
		return new LongVectorValue(elements);
	}

	int length();

	final class ByteVectorValue extends AbstractVectorValue implements VectorValue {

		private ByteVectorValue(byte[] elements) {
			super(byte.class, elements);
		}

	}

	final class DoubleVectorValue extends AbstractVectorValue implements VectorValue {

		private DoubleVectorValue(double[] elements) {
			super(double.class, elements);
		}

	}

	final class FloatVectorValue extends AbstractVectorValue implements VectorValue {

		private FloatVectorValue(float[] elements) {
			super(float.class, elements);
		}

	}

	final class IntVectorValue extends AbstractVectorValue implements VectorValue {

		private IntVectorValue(int[] elements) {
			super(int.class, elements);
		}

	}

	final class LongVectorValue extends AbstractVectorValue implements VectorValue {

		private LongVectorValue(long[] elements) {
			super(long.class, elements);
		}

		public long[] toLongArray() {
			return toArray();
		}

	}

	final class ShortVectorValue extends AbstractVectorValue implements VectorValue {

		private ShortVectorValue(short[] elements) {
			super(short.class, elements);
		}

	}

}
