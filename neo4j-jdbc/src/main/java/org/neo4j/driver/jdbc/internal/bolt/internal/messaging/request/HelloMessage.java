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
package org.neo4j.driver.jdbc.internal.bolt.internal.messaging.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.neo4j.driver.jdbc.internal.bolt.BoltAgent;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.internal.InternalAuthToken;
import org.neo4j.driver.jdbc.values.Value;
import org.neo4j.driver.jdbc.values.Values;

public final class HelloMessage extends MessageWithMetadata {

	public static final byte SIGNATURE = 0x01;

	private static final String USER_AGENT_METADATA_KEY = "user_agent";

	private static final String BOLT_AGENT_METADATA_KEY = "bolt_agent";

	private static final String BOLT_AGENT_PRODUCT_KEY = "product";

	private static final String BOLT_AGENT_PLATFORM_KEY = "platform";

	private static final String BOLT_AGENT_LANGUAGE_KEY = "language";

	private static final String BOLT_AGENT_LANGUAGE_DETAIL_KEY = "language_details";

	private static final String ROUTING_CONTEXT_METADATA_KEY = "routing";

	private static final String PATCH_BOLT_METADATA_KEY = "patch_bolt";

	private static final String DATE_TIME_UTC_PATCH_VALUE = "utc";

	public HelloMessage(BoltServerAddress address, String userAgent, BoltAgent boltAgent,
			Map<String, Value> authToken) {
		super(buildMetadata(address, userAgent, boltAgent, authToken));
	}

	@Override
	public byte signature() {
		return SIGNATURE;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (HelloMessage) o;
		return Objects.equals(metadata(), that.metadata());
	}

	@Override
	public int hashCode() {
		return Objects.hash(metadata());
	}

	@Override
	public String toString() {
		Map<String, Value> metadataCopy = new HashMap<>(metadata());
		metadataCopy.replace(InternalAuthToken.CREDENTIALS_KEY, Values.value("******"));
		return "HELLO " + metadataCopy;
	}

	private static Map<String, Value> buildMetadata(BoltServerAddress address, String userAgent, BoltAgent boltAgent,
			Map<String, Value> authToken) {
		Map<String, Value> result = new HashMap<>(authToken);
		if (userAgent != null) {
			result.put(USER_AGENT_METADATA_KEY, Values.value(userAgent));
		}
		if (boltAgent != null) {
			var boltAgentMap = new HashMap<String, String>();
			boltAgentMap.put(BOLT_AGENT_PRODUCT_KEY, boltAgent.product());
			if (boltAgent.platform() != null) {
				boltAgentMap.put(BOLT_AGENT_PLATFORM_KEY, boltAgent.platform());
			}
			if (boltAgent.language() != null) {
				boltAgentMap.put(BOLT_AGENT_LANGUAGE_KEY, boltAgent.language());
			}
			if (boltAgent.languageDetails() != null) {
				boltAgentMap.put(BOLT_AGENT_LANGUAGE_DETAIL_KEY, boltAgent.languageDetails());
			}
			result.put(BOLT_AGENT_METADATA_KEY, Values.value(boltAgentMap));
		}
		result.put(ROUTING_CONTEXT_METADATA_KEY,
				Values.value(Map.of("address", "%s:%d".formatted(address.host(), address.port()))));
		return result;
	}

}
