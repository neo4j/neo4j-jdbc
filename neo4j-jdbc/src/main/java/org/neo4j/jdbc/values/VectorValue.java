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

/**
 * A value containing a Neo4j {@link Vector}. Please use {@link Vector#elementType()} for
 * retrieving the concrete element-type or use the type-hierarchy of the {@link Vector
 * interface} to check the concrete type and extracting Java arrays from a vector.
 *
 * @author Michael J. Simons
 * @since 6.8.0
 */
public final class VectorValue extends AbstractObjectValue<Vector> {

	VectorValue(Vector adapted) {
		super(adapted);
	}

	@Override
	public Type type() {
		return Type.VECTOR;
	}

	@Override
	public Vector asVector() {
		return super.asObject();
	}

}
