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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * The Cypher type {@code LOCAL DATETIME} maps to a Java {@link LocalDateTime}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class LocalDateTimeValue extends AbstractObjectValue<LocalDateTime> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().appendLiteral("DATETIME '")
		.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
		.appendLiteral('\'')
		.toFormatter();

	LocalDateTimeValue(LocalDateTime localDateTime) {
		super(localDateTime);
	}

	@Override
	public LocalDateTime asLocalDateTime() {
		return asObject();
	}

	@Override
	public Type type() {
		return Type.LOCAL_DATE_TIME;
	}

	@Override
	public String toString() {
		return FORMATTER.format(super.adapted);
	}

}
