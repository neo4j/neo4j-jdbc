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

/**
 * The type of {@link Value} as defined by the Cypher language.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 */
public enum Type {

	/**
	 * Any type except {@link #NULL}.
	 *
	 * @since 1.0.0
	 */
	ANY {
		@Override
		public boolean covers(Value value) {
			return !value.isNull();
		}
	},
	/**
	 * Boolean type.
	 * @since 1.0.0
	 */
	BOOLEAN,
	/**
	 * Bytes type.
	 * @since 1.0.0
	 */
	BYTES,
	/**
	 * String type.
	 * @since 1.0.0
	 */
	STRING,
	/**
	 * Number type.
	 * @since 1.0.0
	 */
	NUMBER {
		@Override
		public boolean covers(Value value) {
			var valueType = value.type();
			return valueType == this || valueType == INTEGER || valueType == FLOAT;
		}
	},
	/**
	 * Integer type.
	 * @since 1.0.0
	 */
	INTEGER,
	/**
	 * Float type.
	 * @since 1.0.0
	 */
	FLOAT,
	/**
	 * List type.
	 * @since 1.0.0
	 */
	LIST {
		@Override
		public String toString() {
			return "LIST OF ANY?";
		}
	},
	/**
	 * Map type.
	 * @since 1.0.0
	 */
	MAP {
		@Override
		public boolean covers(Value value) {
			var valueType = value.type();
			return valueType == MAP || valueType == NODE || valueType == RELATIONSHIP;
		}
	},
	/**
	 * Node type.
	 * @since 1.0.0
	 */
	NODE,
	/**
	 * Relationship type.
	 * @since 1.0.0
	 */
	RELATIONSHIP,
	/**
	 * Path type.
	 * @since 1.0.0
	 */
	PATH,
	/**
	 * Point type.
	 * @since 1.0.0
	 */
	POINT,
	/**
	 * Date type.
	 * @since 1.0.0
	 */
	DATE,
	/**
	 * Time type.
	 * @since 1.0.0
	 */
	TIME,
	/**
	 * Local time type.
	 * @since 1.0.0
	 */
	LOCAL_TIME,
	/**
	 * Local date time type.
	 * @since 1.0.0
	 */
	LOCAL_DATE_TIME,
	/**
	 * Date time type.
	 * @since 1.0.0
	 */
	DATE_TIME,
	/**
	 * Duration type.
	 * @since 1.0.0
	 */
	DURATION,
	/**
	 * Null type.
	 * @since 1.0.0
	 */
	NULL;

	/**
	 * Test if the given value has this type.
	 * @param value the value
	 * @return {@code true} if the value is a value of this type otherwise {@code false}
	 */
	public boolean isTypeOf(Value value) {
		return this.covers(value);
	}

	protected boolean covers(Value value) {
		return this == value.type();
	}

}
