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

import java.io.Serial;

public sealed class ValueException extends
		RuntimeException permits LossyCoercion, NotMultiValuedException, UncoercibleException, UnsizableException {

	@Serial
	private static final long serialVersionUID = 7850167285895596482L;

	public ValueException(String message) {
		super(message);
	}

	public ValueException(String message, Throwable cause) {
		super(message, cause);
	}

}
