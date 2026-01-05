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

import java.lang.reflect.InvocationTargetException;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * This value will be returned for date time properties that are stored in a time zone
 * that is available on the Neo4j host system but not on the client system in which the
 * driver is used.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public final class UnsupportedDateTimeValue extends AbstractValue {

	final DateTimeException exception;

	/**
	 * Constructs a new value based on the exception the driver caught when reading the
	 * corresponding message from the Neo4j host system.
	 * @param exception the original exception caught when trying to deserialize a date
	 * time value.
	 */
	public UnsupportedDateTimeValue(DateTimeException exception) {
		this.exception = exception;
	}

	@Override
	public OffsetDateTime asOffsetDateTime() {
		throw instantiateDateTimeException();
	}

	@Override
	public ZonedDateTime asZonedDateTime() {
		throw instantiateDateTimeException();
	}

	@Override
	public Object asObject() {
		throw instantiateDateTimeException();
	}

	@Override
	public Type type() {
		return Type.DATE_TIME;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "Unsupported datetime value.";
	}

	private DateTimeException instantiateDateTimeException() {
		DateTimeException newException;
		try {
			newException = this.exception.getClass()
				.getDeclaredConstructor(String.class, Throwable.class)
				.newInstance(this.exception.getMessage(), this.exception);
		}
		catch (NoSuchMethodException | InvocationTargetException | InstantiationException
				| IllegalAccessException ignored) {
			newException = new DateTimeException(this.exception.getMessage(), this.exception);
		}
		return newException;
	}

}
