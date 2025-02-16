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
package org.neo4j.jdbc.values;

import java.io.Serial;

/**
 * This exception will be thrown when the coercion {@link Value} into another type is not
 * possible.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class UncoercibleException extends ValueException {

	@Serial
	private static final long serialVersionUID = -6259981390929065201L;

	/**
	 * Creates a new instance.
	 * @param sourceTypeName the source type name
	 * @param destinationTypeName the destination type name
	 */
	public UncoercibleException(String sourceTypeName, String destinationTypeName) {
		super(String.format("Cannot coerce %s to %s", sourceTypeName, destinationTypeName));
	}

}
