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
package org.neo4j.jdbc.benchkit;

import java.io.Serial;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.StdDateFormat;

final class RFC3339DateFormat extends DateFormat {

	private static final TimeZone TIMEZONE_Z = TimeZone.getTimeZone("UTC");

	@Serial
	private static final long serialVersionUID = -6281017024295175044L;

	private final StdDateFormat fmt = new StdDateFormat().withTimeZone(TIMEZONE_Z).withColonInTimeZone(true);

	RFC3339DateFormat() {
		this.calendar = new GregorianCalendar();
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		return this.fmt.parse(source, pos);
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		return this.fmt.format(date, toAppendTo, fieldPosition);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RFC3339DateFormat that)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return Objects.equals(this.fmt, that.fmt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.fmt);
	}

}
