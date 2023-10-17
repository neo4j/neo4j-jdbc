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
package org.neo4j.driver.jdbc.internal.bolt.types;

import org.neo4j.driver.jdbc.internal.bolt.Value;

/**
 * The type of {@link Value} as defined by the Cypher language.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 */
public interface Type {

	/**
	 * Returns the name of the Cypher type.
	 * @return the name of the Cypher type (as defined by Cypher)
	 */
	String name();

	/**
	 * Test if the given value has this type.
	 * @param value the value
	 * @return {@code true} if the value is a value of this type otherwise {@code false}
	 */
	boolean isTypeOf(Value value);

}
