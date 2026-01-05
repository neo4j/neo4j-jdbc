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
package org.neo4j.jdbc;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * The GQL-status object as defined by the GQL standard, see
 * <a href="https://neo4j.com/docs/status-codes/current/">Status Codes for Errors &amp;
 * Notifications</a>.
 *
 * @author Neo4j Drivers Team
 * @since 6.4.0
 */
public interface GqlStatusObject extends Serializable {

	/**
	 * Returns the GQLSTATUS as defined by the GQL standard.
	 * @return the GQLSTATUS value
	 */
	String gqlStatus();

	/**
	 * The GQLSTATUS description.
	 * @return the GQLSTATUS description
	 */
	String statusDescription();

	/**
	 * Returns the diagnostic record.
	 * @return the diagnostic record
	 */
	Map<String, String> diagnosticRecord();

	/**
	 * Returns an optional cause for the status or–as the execution outcome for errors is
	 * represented as a list of GQL-status objects–the status object prior in the list of
	 * status objects for the execution in question.
	 * @return an optional cause for this status object
	 */
	Optional<GqlStatusObject> cause();

}
