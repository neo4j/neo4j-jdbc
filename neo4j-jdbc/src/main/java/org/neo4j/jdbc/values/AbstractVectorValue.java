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

import java.lang.reflect.Array;

/**
 * @since 6.8.0
 */
abstract class AbstractVectorValue extends AbstractValue {

	private final Class<?> elementType;

	private final Object elements;

	private final int length;

	AbstractVectorValue(Class<?> elementType, Object elements) {
		this.elementType = elementType;
		this.elements = elements;
		this.length = Array.getLength(this.elements);
	}

	@Override
	public final Type type() {
		return Type.VECTOR;
	}

	@Override
	public final boolean equals(Object obj) {
		return false;
	}

	@Override
	public final int hashCode() {
		return 0;
	}

	@Override
	public final String toString() {
		// VECTOR<FLOAT32 NOT NULL>(3) NOT NULL"
		return "";
	}

	public final int length() {
		return length;
	}

	@SuppressWarnings({ "SuspiciousSystemArraycopy", "unchecked" })
	final <T> T toArray() {
		var result = (T) Array.newInstance(elementType, length);
		System.arraycopy(elements, 0, result, 0, length);
		return result;
	}

}
