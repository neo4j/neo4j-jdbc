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
package org.neo4j.jdbc.internal.bolt.internal;

import java.util.Objects;

public final class BoltProtocolVersion implements Comparable<BoltProtocolVersion> {

	private final int majorVersion;

	private final int minorVersion;

	public BoltProtocolVersion(int majorVersion, int minorVersion) {
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}

	public static BoltProtocolVersion fromRawBytes(int rawVersion) {
		var major = rawVersion & 0x000000FF;
		var minor = (rawVersion >> 8) & 0x000000FF;

		return new BoltProtocolVersion(major, minor);
	}

	public long getMinorVersion() {
		return this.minorVersion;
	}

	public long getMajorVersion() {
		return this.majorVersion;
	}

	public int toInt() {
		var shiftedMinor = this.minorVersion << 8;
		return shiftedMinor | this.majorVersion;
	}

	public int toIntRange(BoltProtocolVersion minVersion) {
		if (this.majorVersion != minVersion.majorVersion) {
			throw new IllegalArgumentException("Versions should be from the same major version");
		}
		else if (this.minorVersion < minVersion.minorVersion) {
			throw new IllegalArgumentException("Max version should be newer than min version");
		}
		var range = this.minorVersion - minVersion.minorVersion;
		var shiftedRange = range << 16;
		return shiftedRange | toInt();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else if (!(o instanceof BoltProtocolVersion other)) {
			return false;
		}
		else {
			return this.getMajorVersion() == other.getMajorVersion()
					&& this.getMinorVersion() == other.getMinorVersion();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.minorVersion, this.majorVersion);
	}

	/**
	 * Returns the version in format X.Y where X is the major version and Y is the minor.
	 * @return the version in format X.Y where X is the major version and Y is the minor
	 * version
	 */
	@Override
	public String toString() {
		return String.format("%d.%d", this.majorVersion, this.minorVersion);
	}

	@Override
	public int compareTo(BoltProtocolVersion other) {
		var result = Integer.compare(this.majorVersion, other.majorVersion);

		if (result == 0) {
			return Integer.compare(this.minorVersion, other.minorVersion);
		}

		return result;
	}

	public static boolean isHttp(BoltProtocolVersion protocolVersion) {
		// server would respond with `HTTP..` We read 4 bytes to figure out the version.
		// The first two are not used
		// and therefore parse the `P` (80) for major and `T` (84) for minor.
		return protocolVersion.getMajorVersion() == 80 && protocolVersion.getMinorVersion() == 84;
	}

}
