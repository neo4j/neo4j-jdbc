/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal.types;

import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.types.Type;

public final class TypeRepresentation implements Type {

	private final TypeConstructor tyCon;

	public TypeRepresentation(TypeConstructor tyCon) {
		this.tyCon = tyCon;
	}

	@Override
	public boolean isTypeOf(Value value) {
		return this.tyCon.covers(value);
	}

	@Override
	public String name() {
		if (this.tyCon == TypeConstructor.LIST) {
			return "LIST OF ANY?";
		}

		return this.tyCon.toString();
	}

	public TypeConstructor constructor() {
		return this.tyCon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var that = (TypeRepresentation) o;

		return this.tyCon == that.tyCon;
	}

	@Override
	public int hashCode() {
		return this.tyCon.hashCode();
	}

}
