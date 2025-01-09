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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.List;

import org.neo4j.jdbc.values.BooleanValue;
import org.neo4j.jdbc.values.BytesValue;
import org.neo4j.jdbc.values.DateTimeValue;
import org.neo4j.jdbc.values.DateValue;
import org.neo4j.jdbc.values.DurationValue;
import org.neo4j.jdbc.values.FloatValue;
import org.neo4j.jdbc.values.IntegerValue;
import org.neo4j.jdbc.values.ListValue;
import org.neo4j.jdbc.values.LocalDateTimeValue;
import org.neo4j.jdbc.values.LocalTimeValue;
import org.neo4j.jdbc.values.MapValue;
import org.neo4j.jdbc.values.NodeValue;
import org.neo4j.jdbc.values.NullValue;
import org.neo4j.jdbc.values.PathValue;
import org.neo4j.jdbc.values.PointValue;
import org.neo4j.jdbc.values.Record;
import org.neo4j.jdbc.values.RelationshipValue;
import org.neo4j.jdbc.values.StringValue;
import org.neo4j.jdbc.values.TimeValue;
import org.neo4j.jdbc.values.Type;
import org.neo4j.jdbc.values.Value;

final class ResultSetMetaDataImpl implements ResultSetMetaData {

	private final List<String> keys;

	private final Record firstRecord;

	ResultSetMetaDataImpl(List<String> keys, Record firstRecord) {
		this.keys = keys;
		this.firstRecord = firstRecord;
	}

	@Override
	public int getColumnCount() {
		return this.keys.size();
	}

	@Override
	public boolean isAutoIncrement(int column) {
		return false; // No columns can be auto Increment in neo
	}

	@Override
	public boolean isCaseSensitive(int column) {
		return true;
	}

	@Override
	public boolean isSearchable(int column) {
		return true;
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public int isNullable(int column) {
		return columnNullable;
	}

	@Override
	public boolean isSigned(int column) {
		return false;
	}

	@Override
	public int getColumnDisplaySize(int column) {
		return 0;
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		// Because the as command will change the label of the column and that's all we
		// know therefore these two methods are equivalent
		return getColumnName(column);
	}

	@Override
	public String getColumnName(int column) {
		int adjustedIndex = column - 1;
		return this.keys.get(adjustedIndex);
	}

	@Override
	public String getSchemaName(int column) {
		return "public"; // every schema is public.
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return DatabaseMetadataImpl.getMaxPrecision(this.getColumnType(column)).asInt(0);
	}

	@Override
	public int getScale(int column) {
		return 0;
	}

	@Override
	public String getTableName(int column) {
		return "Unknown?"; // can we get this? or does it need another query?
	}

	@Override
	public String getCatalogName(int column) {
		return ""; // No Catalog ever for neo4j.
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		if (this.firstRecord == null) {
			return Types.NULL;
		}
		int adjustedIndex = column - 1;
		var recordType = this.firstRecord.get(adjustedIndex).type();
		return Neo4jConversions.toSqlType(recordType);
	}

	@Override
	public String getColumnTypeName(int column) {
		if (this.firstRecord == null) {
			return "";
		}
		// Jdbc spec says the name of the type in the database so this is fine being named
		// differently from above
		int adjustedIndex = column - 1;
		return this.firstRecord.get(adjustedIndex).type().name();
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		return true; // you cannot write back using neo4j ResultSet.
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		return false; // you cannot write back using neo4j ResultSet.
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException {
		return false; // you cannot write back using neo4j ResultSet.
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		if (this.firstRecord == null) {
			return Object.class.getName();
		}

		Type type = this.firstRecord.get(column - 1).type();

		switch (type) {
			case ANY -> {
				return Value.class.getName();
			}
			case BOOLEAN -> {
				return BooleanValue.class.getName();
			}
			case BYTES -> {
				return BytesValue.class.getName();
			}
			case STRING -> {
				return StringValue.class.getName();
			}
			case NUMBER -> {
				return IntegerValue.class.getName(); // Might need some computation here.
			}
			case INTEGER -> {
				return IntegerValue.class.getName();
			}
			case FLOAT -> {
				return FloatValue.class.getName();
			}
			case LIST -> {
				return ListValue.class.getName();
			}
			case MAP -> {
				return MapValue.class.getName();
			}
			case NODE -> {
				return NodeValue.class.getName();
			}
			case RELATIONSHIP -> {
				return RelationshipValue.class.getName();
			}
			case PATH -> {
				return PathValue.class.getName();
			}
			case POINT -> {
				return PointValue.class.getName();
			}
			case DATE -> {
				return DateValue.class.getName();
			}
			case TIME -> {
				return TimeValue.class.getName();
			}
			case LOCAL_TIME -> {
				return LocalTimeValue.class.getName();
			}
			case LOCAL_DATE_TIME -> {
				return LocalDateTimeValue.class.getName();
			}
			case DATE_TIME -> {
				return DateTimeValue.class.getName();
			}
			case DURATION -> {
				return DurationValue.class.getName();
			}
			case NULL -> {
				return NullValue.class.getName();
			}
		}

		throw new SQLException("Unknown type");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new SQLException("This object does not implement the given interface");
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

}
