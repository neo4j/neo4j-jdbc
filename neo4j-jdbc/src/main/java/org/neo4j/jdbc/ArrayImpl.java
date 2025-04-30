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
package org.neo4j.jdbc;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import org.neo4j.jdbc.Neo4jException.GQLError;
import org.neo4j.jdbc.Neo4jTransaction.PullResponse;
import org.neo4j.jdbc.Neo4jTransaction.RunResponse;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

import static org.neo4j.jdbc.Neo4jException.withReason;

/**
 * Array implementation working on a list of {@link Value values}. Byte array has a
 * special section, as it is represented inside neo4j as an array already and a single
 * byte would be transformed to int.
 *
 * @author Michael J. Simons
 * @since 6.4.0
 */
class ArrayImpl implements Array {

	private static final List<String> RESULT_SET_COLUMNS = List.of("index", "value");

	private final AtomicBoolean freed = new AtomicBoolean(false);

	static Array of(Connection connection, String typeName, Object[] elements) throws SQLException {
		if (typeName == null || typeName.isBlank()) {
			throw new Neo4jException(GQLError.$22N11.withMessage("Invalid argument, typename is required"));
		}
		try {
			var type = Type.valueOf(typeName);

			ArrayImpl theArray;
			List<Value> allValues = new ArrayList<>();
			if (type == Type.BYTES) {
				byte[] bytes = new byte[elements.length];
				for (int i = 0; i < elements.length; i++) {
					var element = elements[i];
					bytes[i] = (byte) element;
				}
				allValues.add(Values.value(bytes));
			}
			else if (elements != null) {
				for (Object element : elements) {
					allValues.add(Values.value(element));
				}
			}
			theArray = (ArrayImpl) of(connection, Values.value(allValues));
			if (type != theArray.arrayType) {
				throw new Neo4jException(GQLError.$22000
					.withMessage("Cannot satisfy type %s with the elements provided".formatted(typeName)));
			}

			return theArray;
		}
		catch (IllegalArgumentException ex) {
			throw new Neo4jException(GQLError.$22000.withMessage("Invalid type name %s".formatted(typeName)));
		}
	}

	static Array of(Connection connection, Value value) throws SQLException {

		if (value == null || value.hasType(Type.NULL)) {
			return null;
		}

		if (value.hasType(Type.BYTES)) {
			return new ArrayImpl(connection, Type.BYTES, List.of(value), false);
		}

		if (!value.hasType(Type.LIST)) {
			throw new Neo4jException(GQLError.$22N01.withTemplatedMessage(value, "LIST", value.type()));
		}

		List<Value> values = value.asList(Function.identity());
		Type arrayType;
		boolean containsNulls;
		if (value.isEmpty()) {
			arrayType = Type.ANY;
			containsNulls = false;
		}
		else {
			arrayType = values.stream().map(Value::type).filter(v -> v != Type.NULL).findFirst().orElse(Type.NULL);
			if (values.stream().anyMatch(v -> !(v.hasType(arrayType) || v.hasType(Type.NULL)))) {
				throw new Neo4jException(GQLError.$22G03.withTemplatedMessage());
			}
			containsNulls = values.stream().anyMatch(v -> v.hasType(Type.NULL));
		}

		return new ArrayImpl(connection, arrayType, values, containsNulls);
	}

	private final Connection connection;

	private final Type arrayType;

	private final List<Value> values;

	private final Lazy<Object, RuntimeException> array;

	ArrayImpl(Connection connection, Type arrayType, List<Value> values, boolean containsNulls) {
		this.connection = connection;
		this.arrayType = arrayType;
		this.values = values;
		this.array = Lazy.of((Supplier<Object>) () -> {
			if (containsNulls) {
				return this.values.stream().map(Value::asObject).toArray(Object[]::new);
			}
			else if (Type.BOOLEAN == this.arrayType) {
				boolean[] result = new boolean[this.values.size()];
				for (var i = 0; i < this.values.size(); i++) {
					result[i] = this.values.get(i).asBoolean();
				}
				return result;
			}
			else if (Type.BYTES == this.arrayType) {
				return this.values.get(0).asByteArray();
			}
			else if (Type.INTEGER == this.arrayType) {
				var builder = LongStream.builder();
				this.values.forEach(v -> builder.add(v.asInt()));
				return builder.build().toArray();
			}
			else if (Type.FLOAT == this.arrayType) {
				var builder = DoubleStream.builder();
				this.values.forEach(v -> builder.add(v.asInt()));
				return builder.build().toArray();
			}

			return this.values.stream().map(Value::asObject).toArray(Object[]::new);
		});
	}

	@Override
	public String getBaseTypeName() throws SQLException {
		assertNotFreed();
		return this.arrayType.name();
	}

	@Override
	public int getBaseType() throws SQLException {
		assertNotFreed();
		return Neo4jConversions.toSqlType(this.arrayType);
	}

	@Override
	public Object getArray() throws SQLException {
		assertNotFreed();
		return this.array.resolve();
	}

	@Override
	public Object getArray(Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getArray();
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	@Override
	public Object getArray(long index, int count) throws SQLException {
		assertNotFreed();
		var fullArray = this.array.resolve();
		var length = java.lang.reflect.Array.getLength(fullArray);
		assertSlice(new Slice(index, count), length);

		Class<?> componentType = fullArray.getClass().getComponentType();
		var slice = java.lang.reflect.Array.newInstance(componentType, count);
		System.arraycopy(fullArray, (int) index - 1, slice, 0, count);
		return slice;
	}

	@Override
	public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getArray(index, count);
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		assertNotFreed();
		return new LocalStatementImpl(this.connection, defaultRunResponseKeys(), toPullResponse(null)).getResultSet();
	}

	@Override
	public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getResultSet();
	}

	@Override
	public ResultSet getResultSet(long index, int count) throws SQLException {
		assertNotFreed();
		return new LocalStatementImpl(this.connection, defaultRunResponseKeys(),
				toPullResponse(new Slice(index, count)))
			.getResultSet();
	}

	@Override
	public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
		Neo4jConversions.assertTypeMap(map);
		return getResultSet(index, count);
	}

	@Override
	public void free() throws SQLException {
		if (this.freed.compareAndSet(false, true)) {
			this.array.forget();
		}
	}

	private static void assertSlice(Slice slice, int length) throws SQLException {
		var count = slice.count;
		var index = slice.index;

		if (count < 0 || index < 1 || index - 1 + count > length) {
			throw new Neo4jException(GQLError.$22N11
				.withTemplatedMessage("getArray(%d, %d) for array with size %d".formatted(index, count, length)));
		}
	}

	private void assertNotFreed() throws SQLException {
		if (this.freed.get()) {
			throw new Neo4jException(withReason("Array has been already freed"));
		}
	}

	private static RunResponse defaultRunResponseKeys() {
		return new RunResponse() {
			@Override
			public long queryId() {
				return 0;
			}

			@Override
			public List<String> keys() {
				return RESULT_SET_COLUMNS;
			}
		};
	}

	private PullResponse toPullResponse(Slice slice) throws SQLException {

		int length;
		Function<Integer, Value> recordValueSupplier;
		// The byte array coming back from neo4j is kinda special
		if (this.arrayType == Type.BYTES) {
			byte[] byteArray = (byte[]) this.array.resolve();
			length = byteArray.length;
			recordValueSupplier = i -> Values.value(byteArray[i]);
		}
		else {
			length = this.values.size();
			recordValueSupplier = this.values::get;
		}

		int start = 0;
		if (slice != null) {
			assertSlice(slice, length);
			start = (int) (slice.index - 1);
			length = start + slice.count;
		}

		var records = new ArrayList<Record>(length);
		for (int i = start; i < length; ++i) {
			records
				.add(Record.of(RESULT_SET_COLUMNS, new Value[] { Values.value(i + 1), recordValueSupplier.apply(i) }));
		}

		return new PullResponse() {
			@Override
			public List<Record> records() {
				return records;
			}

			@Override
			public Optional<Neo4jTransaction.ResultSummary> resultSummary() {
				return Optional.empty();
			}

			@Override
			public boolean hasMore() {
				return false;
			}
		};
	}

	private record Slice(long index, int count) {
	}

}
