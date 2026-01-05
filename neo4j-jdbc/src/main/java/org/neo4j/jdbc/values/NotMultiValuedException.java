/*
 * Copyright (c) 2023-2026 "Neo4j,"
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

import java.io.Serial;

/**
 * Exception that is thrown when any {@link Value} object that is not composed by keys and
 * values or indexes and values is accessed as a map or a collection item.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class NotMultiValuedException extends ValueException {

	@Serial
	private static final long serialVersionUID = -7380569883011364090L;

	/**
	 * Creates a new instance.
	 * @param message the message
	 */
	public NotMultiValuedException(String message) {
		super(message);
	}

}
