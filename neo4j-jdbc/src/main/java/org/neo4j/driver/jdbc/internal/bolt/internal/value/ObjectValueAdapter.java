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
package org.neo4j.driver.jdbc.internal.bolt.internal.value;

import java.util.Objects;

public abstract class ObjectValueAdapter<V> extends ValueAdapter {

	private final V adapted;

	protected ObjectValueAdapter(V adapted) {
		if (adapted == null) {
			throw new IllegalArgumentException(
					String.format("Cannot construct %s from null", getClass().getSimpleName()));
		}
		this.adapted = adapted;
	}

	@Override
	public final V asObject() {
		return this.adapted;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (ObjectValueAdapter<?>) o;
		return Objects.equals(this.adapted, that.adapted);
	}

	@Override
	public int hashCode() {
		return this.adapted.hashCode();
	}

	@Override
	public String toString() {
		return this.adapted.toString();
	}

}
