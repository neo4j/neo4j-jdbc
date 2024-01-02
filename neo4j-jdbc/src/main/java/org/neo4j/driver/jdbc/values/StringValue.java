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
package org.neo4j.driver.jdbc.values;

import java.util.Objects;

public final class StringValue extends AbstractValue {

	private final String val;

	StringValue(String val) {
		if (val == null) {
			throw new IllegalArgumentException("Cannot construct StringValue from null");
		}
		this.val = val;
	}

	@Override
	public boolean isEmpty() {
		return this.val.isEmpty();
	}

	@Override
	public int size() {
		return this.val.length();
	}

	@Override
	public String asObject() {
		return asString();
	}

	@Override
	public String asString() {
		return this.val;
	}

	@Override
	public Type type() {
		return Type.STRING;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (StringValue) o;
		return Objects.equals(this.val, that.val);
	}

	@Override
	public int hashCode() {
		return this.val.hashCode();
	}

	@Override
	public String toString() {
		return String.format("\"%s\"", this.val.replace("\"", "\\\""));
	}

}
