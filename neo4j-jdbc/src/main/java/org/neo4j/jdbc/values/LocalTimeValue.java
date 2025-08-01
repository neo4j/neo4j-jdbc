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
package org.neo4j.jdbc.values;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * The Cypher type {@code LOCAL TIME} maps to a Java {@link LocalTime}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class LocalTimeValue extends AbstractObjectValue<LocalTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().appendLiteral("TIME '")
		.append(DateTimeFormatter.ISO_LOCAL_TIME)
		.appendLiteral('\'')
		.toFormatter();

	LocalTimeValue(LocalTime time) {
		super(time);
	}

	@Override
	public LocalTime asLocalTime() {
		return asObject();
	}

	@Override
	public Type type() {
		return Type.LOCAL_TIME;
	}

	@Override
	public String toString() {
		return FORMATTER.format(super.adapted);
	}

}
