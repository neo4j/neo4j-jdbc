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
package org.neo4j.driver.jdbc;

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

class ParameterMetaDataImpl implements ParameterMetaData {

	@Override
	public int getParameterCount() {
		return 0;
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
		return Types.NULL;
	}

	@Override
	public String getParameterTypeName(int param) {
		return null;
	}

	@Override
	public String getParameterClassName(int param) {
		return null;
	}

	@Override
	public int getParameterMode(int param) {
		return ParameterMetaData.parameterModeUnknown;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		else {
			throw new SQLException("This object does not implement the given interface.");
		}
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
	}

}
