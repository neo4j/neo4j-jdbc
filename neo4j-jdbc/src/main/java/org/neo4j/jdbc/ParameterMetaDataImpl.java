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

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.util.Map;

import static org.neo4j.jdbc.Neo4jException.withReason;

class ParameterMetaDataImpl implements ParameterMetaData {

	private final int parameterCount;

	private final Map<Integer, String> parameterTypes;

	ParameterMetaDataImpl(int parameterCount) {
		this.parameterCount = parameterCount;
		this.parameterTypes = Map.of();
	}

	ParameterMetaDataImpl(Map<Integer, String> parameterTypes) {
		this.parameterCount = parameterTypes.size();
		this.parameterTypes = Map.copyOf(parameterTypes);
	}

	@Override
	public int getParameterCount() {
		return this.parameterCount;
	}

	@Override
	public int isNullable(int param) {
		return ParameterMetaData.parameterNullableUnknown;
	}

	@Override
	public boolean isSigned(int param) {
		return false;
	}

	@Override
	public int getPrecision(int param) {
		return 0;
	}

	@Override
	public int getScale(int param) {
		return 0;
	}

	@Override
	public int getParameterType(int param) {
		return Neo4jConversions.toSqlTypeFromOldCypherType(this.parameterTypes.get(param));
	}

	@Override
	public String getParameterTypeName(int param) {
		return Neo4jConversions.oldCypherTypesToNew(this.parameterTypes.get(param));
	}

	@Override
	public String getParameterClassName(int param) {
		return null;
	}

	@Override
	public int getParameterMode(int param) {
		return ParameterMetaData.parameterModeIn;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new Neo4jException(withReason("This object does not implement the given interface"));
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
	}

}
