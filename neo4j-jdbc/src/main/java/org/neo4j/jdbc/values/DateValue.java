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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * The Cypher type {@code DATE} maps to a Java {@link LocalDate}.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class DateValue extends AbstractObjectValue<LocalDate> {

	private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().appendLiteral("DATE '")
		.append(DateTimeFormatter.ISO_DATE)
		.appendLiteral('\'')
		.toFormatter();

	DateValue(LocalDate date) {
		super(date);
	}

	@Override
	public LocalDate asLocalDate() {
		return asObject();
	}

	@Override
	public Type type() {
		return Type.DATE;
	}

	@Override
	public String toString() {
		return FORMATTER.format(super.adapted);
	}

}
