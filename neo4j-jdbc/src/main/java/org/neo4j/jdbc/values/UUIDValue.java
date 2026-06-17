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

import java.util.UUID;

/**
 * Value type wrapping a {@link UUID}.
 *
 * @author Michael J. Simons
 * @since 6.14.0
 */
public final class UUIDValue extends AbstractObjectValue<UUID> {

	UUIDValue(UUID adapted) {
		super(adapted);
	}

	@Override
	public UUID asUUID() {
		return asObject();
	}

	@Override
	public Type type() {
		return Type.UUID;
	}

}
