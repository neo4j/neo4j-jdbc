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
 * The base type for several exception that might occur within the Neo4j value system,
 * such as a lossy coercion, that might occur when trying to downcast a Cypher
 * {@code INTEGER} (which is a 64bit integer), into a Java int (which is only 32bit).
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public sealed class ValueException extends RuntimeException
		permits LossyCoercion, NotMultiValuedException, UncoercibleException, UnsizableException {

	@Serial
	private static final long serialVersionUID = 7850167285895596482L;

	/**
	 * Constructs a new exception with the specified detail message. The cause is not
	 * initialized, and may subsequently be initialized by a call to {@link #initCause}.
	 * @param message the detail message. The detail message is saved for later retrieval
	 * by the {@link #getMessage()} method.
	 */
	public ValueException(String message) {
		super(message);
	}

}
