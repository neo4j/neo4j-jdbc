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
package org.neo4j.driver.jdbc.internal.bolt.internal.value;

import java.time.LocalTime;

import org.neo4j.driver.jdbc.internal.bolt.internal.types.InternalTypeSystem;
import org.neo4j.driver.jdbc.internal.bolt.types.Type;

public final class LocalTimeValue extends ObjectValueAdapter<LocalTime> {

	public LocalTimeValue(LocalTime time) {
		super(time);
	}

	@Override
	public LocalTime asLocalTime() {
		return asObject();
	}

	@Override
	public Type type() {
		return InternalTypeSystem.TYPE_SYSTEM.LOCAL_TIME();
	}

}
