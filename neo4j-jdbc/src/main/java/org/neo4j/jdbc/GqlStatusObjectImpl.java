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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.neo4j.bolt.connection.exception.BoltGqlErrorException;
import org.neo4j.jdbc.values.Values;

/**
 * Internal implementation of the {@link GqlStatusObject}, to decouple it from a
 * {@link Neo4jException}. The latter is just a carrier and enabler for streamlined
 * construction and not anything externally visible. Also, the GQL notification machinery
 * isn't exception driven, even an exceptional outcome will have statuses only.
 *
 * @author Michael J. Simons
 */
final class GqlStatusObjectImpl implements GqlStatusObject {

	@Serial
	private static final long serialVersionUID = 2325394968498244944L;

	private final String gqlStatus;

	private final String statusDescription;

	private final HashMap<String, String> diagnosticRecord;

	private final GqlStatusObject cause;

	private GqlStatusObjectImpl(String gqlStatus, String statusDescription, HashMap<String, String> diagnosticRecord,
			GqlStatusObject cause) {
		this.gqlStatus = gqlStatus;
		this.statusDescription = statusDescription;
		this.diagnosticRecord = diagnosticRecord;
		this.cause = cause;
	}

	static GqlStatusObject of(BoltGqlErrorException gqlErrorException) {
		var cause = gqlErrorException.gqlCause().map(GqlStatusObjectImpl::of).orElse(null);
		return new GqlStatusObjectImpl(gqlErrorException.gqlStatus(), gqlErrorException.statusDescription(),
				adaptDiagnosticRecord(gqlErrorException), cause);
	}

	/**
	 * This turns a bolt diagnostic record into a serializable map, hence {@link HashMap}
	 * is chosen on purpose. Callers must ensure immutability.
	 * @param gqlErrorException the original bolt gql error to extract the diagnostic
	 * record from
	 * @return a serializable map
	 */
	static HashMap<String, String> adaptDiagnosticRecord(BoltGqlErrorException gqlErrorException) {
		return gqlErrorException.diagnosticRecord()
			.entrySet()
			.stream()
			.collect(Collectors.collectingAndThen(
					Collectors.toMap(Map.Entry::getKey, e -> Values.value(e.getValue()).asObject().toString()),
					m -> (m instanceof HashMap<String, String> hm) ? hm : new HashMap<>(m)));
	}

	@Override
	public Optional<GqlStatusObject> cause() {
		return Optional.ofNullable(this.cause);
	}

	@Override
	public String gqlStatus() {
		return this.gqlStatus;
	}

	@Override
	public String statusDescription() {
		return this.statusDescription;
	}

	@Override
	public Map<String, String> diagnosticRecord() {
		return Map.copyOf(this.diagnosticRecord);
	}

}
