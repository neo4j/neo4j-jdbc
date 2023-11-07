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
package org.neo4j.driver.jdbc.values;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.Objects;

final class IsoDurationImpl implements IsoDuration {

	private static final long NANOS_PER_SECOND = 1_000_000_000;

	private static final List<TemporalUnit> SUPPORTED_UNITS = List.of(ChronoUnit.MONTHS, ChronoUnit.DAYS,
			ChronoUnit.SECONDS, ChronoUnit.NANOS);

	private final long months;

	private final long days;

	private final long seconds;

	private final int nanoseconds;

	IsoDurationImpl(Period period) {
		this(period.toTotalMonths(), period.getDays(), Duration.ZERO);
	}

	IsoDurationImpl(Duration duration) {
		this(0, 0, duration);
	}

	IsoDurationImpl(long months, long days, long seconds, int nanoseconds) {
		this(months, days, Duration.ofSeconds(seconds, nanoseconds));
	}

	IsoDurationImpl(long months, long days, Duration duration) {
		this.months = months;
		this.days = days;
		this.seconds = duration.getSeconds(); // normalized value of seconds
		this.nanoseconds = duration.getNano(); // normalized value of nanoseconds in [0,
												// 999_999_999]
	}

	@Override
	public long months() {
		return this.months;
	}

	@Override
	public long days() {
		return this.days;
	}

	@Override
	public long seconds() {
		return this.seconds;
	}

	@Override
	public int nanoseconds() {
		return this.nanoseconds;
	}

	@Override
	public long get(TemporalUnit unit) {
		if (unit == ChronoUnit.MONTHS) {
			return this.months;
		}
		else if (unit == ChronoUnit.DAYS) {
			return this.days;
		}
		else if (unit == ChronoUnit.SECONDS) {
			return this.seconds;
		}
		else if (unit == ChronoUnit.NANOS) {
			return this.nanoseconds;
		}
		else {
			throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
		}
	}

	@Override
	public List<TemporalUnit> getUnits() {
		return SUPPORTED_UNITS;
	}

	@Override
	public Temporal addTo(Temporal temporal) {
		if (this.months != 0) {
			temporal = temporal.plus(this.months, ChronoUnit.MONTHS);
		}
		if (this.days != 0) {
			temporal = temporal.plus(this.days, ChronoUnit.DAYS);
		}
		if (this.seconds != 0) {
			temporal = temporal.plus(this.seconds, ChronoUnit.SECONDS);
		}
		if (this.nanoseconds != 0) {
			temporal = temporal.plus(this.nanoseconds, ChronoUnit.NANOS);
		}
		return temporal;
	}

	@Override
	public Temporal subtractFrom(Temporal temporal) {
		if (this.months != 0) {
			temporal = temporal.minus(this.months, ChronoUnit.MONTHS);
		}
		if (this.days != 0) {
			temporal = temporal.minus(this.days, ChronoUnit.DAYS);
		}
		if (this.seconds != 0) {
			temporal = temporal.minus(this.seconds, ChronoUnit.SECONDS);
		}
		if (this.nanoseconds != 0) {
			temporal = temporal.minus(this.nanoseconds, ChronoUnit.NANOS);
		}
		return temporal;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (IsoDurationImpl) o;
		return this.months == that.months && this.days == that.days && this.seconds == that.seconds
				&& this.nanoseconds == that.nanoseconds;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.months, this.days, this.seconds, this.nanoseconds);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append('P');
		sb.append(this.months).append('M');
		sb.append(this.days).append('D');
		sb.append('T');
		if (this.seconds < 0 && this.nanoseconds > 0) {
			if (this.seconds == -1) {
				sb.append("-0");
			}
			else {
				sb.append(this.seconds + 1);
			}
		}
		else {
			sb.append(this.seconds);
		}
		if (this.nanoseconds > 0) {
			var pos = sb.length();
			// append nanoseconds as a 10-digit string with leading '1' that is later
			// replaced by a '.'
			if (this.seconds < 0) {
				sb.append(2 * NANOS_PER_SECOND - this.nanoseconds);
			}
			else {
				sb.append(NANOS_PER_SECOND + this.nanoseconds);
			}
			sb.setCharAt(pos, '.'); // replace '1' with '.'
		}
		sb.append('S');
		return sb.toString();
	}

}
