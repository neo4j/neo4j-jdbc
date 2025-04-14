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
package org.neo4j.jdbc;

import java.io.Serial;
import java.sql.SQLException;

/**
 * Modeled to go with
 * <a href="https://neo4j.com/docs/status-codes/current/errors/gql-errors/">the Neo4j GQL
 * Error codes</a>.
 *
 * @author Michael J. Simons
 * @since 6.4.0
 */
final class GQLException extends SQLException {

	enum ErrorCode {

		GQL_22G03, GQL_22N01, GQL_22N11;

		@Override
		public String toString() {
			return name().substring(4);
		}

	}

	@Serial
	private static final long serialVersionUID = 8197762196989208131L;

	GQLException(ErrorCode errorCode, String reason) {
		super(reason, errorCode.toString());
	}

}
