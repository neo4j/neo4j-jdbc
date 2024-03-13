/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * A Cypher LIST&lt;ANY&gt;.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class ListValue extends AbstractValue {

	private final Value[] values;

	ListValue(Value... values) {
		if (values == null) {
			throw new IllegalArgumentException("Cannot construct ListValue from null");
		}
		this.values = values;
	}

	@Override
	public boolean isEmpty() {
		return this.values.length == 0;
	}

	@Override
	public List<Object> asObject() {
		return asList(Values.ofObject());
	}

	@Override
	public List<Object> asList() {
		return list(this.values, Values.ofObject());
	}

	@Override
	public <T> List<T> asList(Function<Value, T> mapFunction) {
		return list(this.values, mapFunction);
	}

	@Override
	public int size() {
		return this.values.length;
	}

	@Override
	public Value get(int index) {
		return (index >= 0 && index < this.values.length) ? this.values[index] : Values.NULL;
	}

	@Override
	public <T> Iterable<T> values(final Function<Value, T> mapFunction) {
		return () -> new Iterator<>() {
			private int cursor;

			@Override
			public boolean hasNext() {
				return this.cursor < ListValue.this.values.length;
			}

			@Override
			public T next() {
				if (this.cursor >= ListValue.this.values.length) {
					throw new NoSuchElementException();
				}
				return mapFunction.apply(ListValue.this.values[this.cursor++]);
			}

			@Override
			public void remove() {
			}
		};
	}

	@Override
	public Type type() {
		return Type.LIST;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		var otherValues = (ListValue) o;
		return Arrays.equals(this.values, otherValues.values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.values);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.values);
	}

	private static <T> List<T> list(Value[] data, Function<Value, T> mapFunction) {
		var size = data.length;
		switch (size) {
			case 0 -> {
				return Collections.emptyList();
			}
			case 1 -> {
				return Collections.singletonList(mapFunction.apply(data[0]));
			}
			default -> {
				return Arrays.stream(data).map(mapFunction).toList();
			}
		}
	}

}
