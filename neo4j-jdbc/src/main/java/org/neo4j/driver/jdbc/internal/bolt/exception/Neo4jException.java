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
package org.neo4j.driver.jdbc.internal.bolt.exception;

import java.io.Serial;

public class Neo4jException extends BoltException {

	@Serial
	private static final long serialVersionUID = -80579062276712566L;

	/**
	 * The code value.
	 */
	private final String code;

	/**
	 * Creates a new instance.
	 * @param code the code
	 * @param message the message
	 */
	public Neo4jException(String code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * Access the status code for this exception. The Neo4j manual can provide further
	 * details on the available codes and their meanings.
	 * @return textual code, such as "Neo.ClientError.Procedure.ProcedureNotFound"
	 */
	public String code() {
		return this.code;
	}

}
