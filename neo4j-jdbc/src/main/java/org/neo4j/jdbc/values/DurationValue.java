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
package org.neo4j.jdbc.values;

/**
 * The Neo4j duration is different than both Java's {@link java.time.Duration}
 * <strong>and</strong> {@link java.time.Period}. In Java, those are different concepts,
 * in which a duration is always time based and exact for the supported units while a
 * period is calendar based and therefor does not always have the same number of dates.
 * The Neo4j {@link IsoDuration} reassembles what Neo4j does, but blurs the lines between
 * a duration and a period.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class DurationValue extends AbstractObjectValue<IsoDuration> {

	DurationValue(IsoDuration duration) {
		super(duration);
	}

	@Override
	public IsoDuration asIsoDuration() {
		return asObject();
	}

	@Override
	public Type type() {
		return Type.DURATION;
	}

}
