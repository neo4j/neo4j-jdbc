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
package org.neo4j.driver.jdbc.internal.bolt.internal.util;

import org.neo4j.driver.jdbc.internal.bolt.exception.BoltException;

public final class ErrorUtil {

	private ErrorUtil() {
	}

	public static BoltException newConnectionTerminatedError(String reason) {
		if (reason == null) {
			return newConnectionTerminatedError();
		}
		return new BoltException("Connection to the database terminated. " + reason);
	}

	public static BoltException newConnectionTerminatedError() {
		return new BoltException("Connection to the database terminated. "
				+ "Please ensure that your database is listening on the correct host and port and that you have compatible encryption settings both on Neo4j server and driver. "
				+ "Note that the default encryption setting has changed in Neo4j 4.0.");
	}

}
