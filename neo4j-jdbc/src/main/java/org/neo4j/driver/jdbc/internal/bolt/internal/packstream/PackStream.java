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
package org.neo4j.driver.jdbc.internal.bolt.internal.packstream;

import java.io.IOException;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class PackStream {

	public static final byte TINY_STRING = (byte) 0x80;

	public static final byte TINY_LIST = (byte) 0x90;

	public static final byte TINY_MAP = (byte) 0xA0;

	public static final byte TINY_STRUCT = (byte) 0xB0;

	public static final byte NULL = (byte) 0xC0;

	public static final byte FLOAT_64 = (byte) 0xC1;

	public static final byte FALSE = (byte) 0xC2;

	public static final byte TRUE = (byte) 0xC3;

	@SuppressWarnings("unused")
	public static final byte RESERVED_C4 = (byte) 0xC4;

	@SuppressWarnings("unused")
	public static final byte RESERVED_C5 = (byte) 0xC5;

	@SuppressWarnings("unused")
	public static final byte RESERVED_C6 = (byte) 0xC6;

	@SuppressWarnings("unused")
	public static final byte RESERVED_C7 = (byte) 0xC7;

	public static final byte INT_8 = (byte) 0xC8;

	public static final byte INT_16 = (byte) 0xC9;

	public static final byte INT_32 = (byte) 0xCA;

	public static final byte INT_64 = (byte) 0xCB;

	public static final byte BYTES_8 = (byte) 0xCC;

	public static final byte BYTES_16 = (byte) 0xCD;

	public static final byte BYTES_32 = (byte) 0xCE;

	@SuppressWarnings("unused")
	public static final byte RESERVED_CF = (byte) 0xCF;

	public static final byte STRING_8 = (byte) 0xD0;

	public static final byte STRING_16 = (byte) 0xD1;

	public static final byte STRING_32 = (byte) 0xD2;

	@SuppressWarnings("unused")
	public static final byte RESERVED_D3 = (byte) 0xD3;

	public static final byte LIST_8 = (byte) 0xD4;

	public static final byte LIST_16 = (byte) 0xD5;

	public static final byte LIST_32 = (byte) 0xD6;

	@SuppressWarnings("unused")
	public static final byte RESERVED_D7 = (byte) 0xD7;

	public static final byte MAP_8 = (byte) 0xD8;

	public static final byte MAP_16 = (byte) 0xD9;

	public static final byte MAP_32 = (byte) 0xDA;

	@SuppressWarnings("unused")
	public static final byte RESERVED_DB = (byte) 0xDB;

	public static final byte STRUCT_8 = (byte) 0xDC;

	public static final byte STRUCT_16 = (byte) 0xDD;

	@SuppressWarnings("unused")
	public static final byte RESERVED_DE = (byte) 0xDE;

	@SuppressWarnings("unused")
	public static final byte RESERVED_DF = (byte) 0xDF;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E0 = (byte) 0xE0;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E1 = (byte) 0xE1;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E2 = (byte) 0xE2;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E3 = (byte) 0xE3;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E4 = (byte) 0xE4;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E5 = (byte) 0xE5;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E6 = (byte) 0xE6;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E7 = (byte) 0xE7;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E8 = (byte) 0xE8;

	@SuppressWarnings("unused")
	public static final byte RESERVED_E9 = (byte) 0xE9;

	@SuppressWarnings("unused")
	public static final byte RESERVED_EA = (byte) 0xEA;

	@SuppressWarnings("unused")
	public static final byte RESERVED_EB = (byte) 0xEB;

	@SuppressWarnings("unused")
	public static final byte RESERVED_EC = (byte) 0xEC;

	@SuppressWarnings("unused")
	public static final byte RESERVED_ED = (byte) 0xED;

	@SuppressWarnings("unused")
	public static final byte RESERVED_EE = (byte) 0xEE;

	@SuppressWarnings("unused")
	public static final byte RESERVED_EF = (byte) 0xEF;

	private static final long PLUS_2_TO_THE_31 = 2147483648L;

	private static final long PLUS_2_TO_THE_16 = 65536L;

	private static final long PLUS_2_TO_THE_15 = 32768L;

	private static final long PLUS_2_TO_THE_7 = 128L;

	private static final long MINUS_2_TO_THE_4 = -16L;

	private static final long MINUS_2_TO_THE_7 = -128L;

	private static final long MINUS_2_TO_THE_15 = -32768L;

	private static final long MINUS_2_TO_THE_31 = -2147483648L;

	private static final String EMPTY_STRING = "";

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private static final Charset UTF_8 = StandardCharsets.UTF_8;

	private PackStream() {
	}

	public static class Packer {

		private final PackOutput out;

		public Packer(PackOutput out) {
			this.out = out;
		}

		private void packRaw(byte[] data) throws IOException {
			this.out.writeBytes(data);
		}

		public void packNull() throws IOException {
			this.out.writeByte(NULL);
		}

		public void pack(boolean value) throws IOException {
			this.out.writeByte(value ? TRUE : FALSE);
		}

		public void pack(long value) throws IOException {
			if (value >= MINUS_2_TO_THE_4 && value < PLUS_2_TO_THE_7) {
				this.out.writeByte((byte) value);
			}
			else if (value >= MINUS_2_TO_THE_7 && value < MINUS_2_TO_THE_4) {
				this.out.writeByte(INT_8).writeByte((byte) value);
			}
			else if (value >= MINUS_2_TO_THE_15 && value < PLUS_2_TO_THE_15) {
				this.out.writeByte(INT_16).writeShort((short) value);
			}
			else if (value >= MINUS_2_TO_THE_31 && value < PLUS_2_TO_THE_31) {
				this.out.writeByte(INT_32).writeInt((int) value);
			}
			else {
				this.out.writeByte(INT_64).writeLong(value);
			}
		}

		public void pack(double value) throws IOException {
			this.out.writeByte(FLOAT_64).writeDouble(value);
		}

		public void pack(byte[] values) throws IOException {
			if (values == null) {
				packNull();
			}
			else {
				packBytesHeader(values.length);
				packRaw(values);
			}
		}

		public void pack(String value) throws IOException {
			if (value == null) {
				packNull();
			}
			else {
				var utf8 = value.getBytes(UTF_8);
				packStringHeader(utf8.length);
				packRaw(utf8);
			}
		}

		private void pack(List<?> values) throws IOException {
			if (values == null) {
				packNull();
			}
			else {
				packListHeader(values.size());
				for (Object value : values) {
					pack(value);
				}
			}
		}

		private void pack(Map<?, ?> values) throws IOException {
			if (values == null) {
				packNull();
			}
			else {
				packMapHeader(values.size());
				for (Object key : values.keySet()) {
					pack(key);
					pack(values.get(key));
				}
			}
		}

		public void pack(Object value) throws IOException {
			if (value == null) {
				packNull();
			}
			else if (value instanceof Boolean) {
				pack((boolean) value);
			}
			else if (value instanceof boolean[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Byte) {
				pack((byte) value);
			}
			else if (value instanceof byte[]) {
				pack((byte[]) value);
			}
			else if (value instanceof Short) {
				pack((short) value);
			}
			else if (value instanceof short[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Integer) {
				pack((int) value);
			}
			else if (value instanceof int[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Long) {
				pack((long) value);
			}
			else if (value instanceof long[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Float) {
				pack((float) value);
			}
			else if (value instanceof float[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Double) {
				pack((double) value);
			}
			else if (value instanceof double[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof Character) {
				pack(Character.toString((char) value));
			}
			else if (value instanceof char[]) {
				pack(new String((char[]) value));
			}
			else if (value instanceof String) {
				pack((String) value);
			}
			else if (value instanceof String[]) {
				pack(Collections.singletonList(value));
			}
			else if (value instanceof List) {
				pack((List<?>) value);
			}
			else if (value instanceof Map) {
				pack((Map<?, ?>) value);
			}
			else {
				throw new UnPackable(String.format("Cannot pack object %s", value));
			}
		}

		public void packBytesHeader(int size) throws IOException {
			if (size <= Byte.MAX_VALUE) {
				this.out.writeByte(BYTES_8).writeByte((byte) size);
			}
			else if (size < PLUS_2_TO_THE_16) {
				this.out.writeByte(BYTES_16).writeShort((short) size);
			}
			else {
				this.out.writeByte(BYTES_32).writeInt(size);
			}
		}

		private void packStringHeader(int size) throws IOException {
			if (size < 0x10) {
				this.out.writeByte((byte) (TINY_STRING | size));
			}
			else if (size <= Byte.MAX_VALUE) {
				this.out.writeByte(STRING_8).writeByte((byte) size);
			}
			else if (size < PLUS_2_TO_THE_16) {
				this.out.writeByte(STRING_16).writeShort((short) size);
			}
			else {
				this.out.writeByte(STRING_32).writeInt(size);
			}
		}

		public void packListHeader(int size) throws IOException {
			if (size < 0x10) {
				this.out.writeByte((byte) (TINY_LIST | size));
			}
			else if (size <= Byte.MAX_VALUE) {
				this.out.writeByte(LIST_8).writeByte((byte) size);
			}
			else if (size < PLUS_2_TO_THE_16) {
				this.out.writeByte(LIST_16).writeShort((short) size);
			}
			else {
				this.out.writeByte(LIST_32).writeInt(size);
			}
		}

		public void packMapHeader(int size) throws IOException {
			if (size < 0x10) {
				this.out.writeByte((byte) (TINY_MAP | size));
			}
			else if (size <= Byte.MAX_VALUE) {
				this.out.writeByte(MAP_8).writeByte((byte) size);
			}
			else if (size < PLUS_2_TO_THE_16) {
				this.out.writeByte(MAP_16).writeShort((short) size);
			}
			else {
				this.out.writeByte(MAP_32).writeInt(size);
			}
		}

		public void packStructHeader(int size, byte signature) throws IOException {
			if (size < 0x10) {
				this.out.writeByte((byte) (TINY_STRUCT | size)).writeByte(signature);
			}
			else if (size <= Byte.MAX_VALUE) {
				this.out.writeByte(STRUCT_8).writeByte((byte) size).writeByte(signature);
			}
			else if (size < PLUS_2_TO_THE_16) {
				this.out.writeByte(STRUCT_16).writeShort((short) size).writeByte(signature);
			}
			else {
				throw new Overflow("Structures cannot have more than " + (PLUS_2_TO_THE_16 - 1) + " fields");
			}
		}

	}

	public static class Unpacker {

		private final PackInput in;

		public Unpacker(PackInput in) {
			this.in = in;
		}

		public long unpackStructHeader() throws IOException {
			final var markerByte = this.in.readByte();
			final var markerHighNibble = (byte) (markerByte & 0xF0);
			final var markerLowNibble = (byte) (markerByte & 0x0F);

			if (markerHighNibble == TINY_STRUCT) {
				return markerLowNibble;
			}
			return switch (markerByte) {
				case STRUCT_8 -> unpackUINT8();
				case STRUCT_16 -> unpackUINT16();
				default -> throw new Unexpected("Expected a struct, but got: " + Integer.toHexString(markerByte));
			};
		}

		public byte unpackStructSignature() throws IOException {
			return this.in.readByte();
		}

		public long unpackListHeader() throws IOException {
			final var markerByte = this.in.readByte();
			final var markerHighNibble = (byte) (markerByte & 0xF0);
			final var markerLowNibble = (byte) (markerByte & 0x0F);

			if (markerHighNibble == TINY_LIST) {
				return markerLowNibble;
			}
			return switch (markerByte) {
				case LIST_8 -> unpackUINT8();
				case LIST_16 -> unpackUINT16();
				case LIST_32 -> unpackUINT32();
				default -> throw new Unexpected("Expected a list, but got: " + Integer.toHexString(markerByte & 0xFF));
			};
		}

		public long unpackMapHeader() throws IOException {
			final var markerByte = this.in.readByte();
			final var markerHighNibble = (byte) (markerByte & 0xF0);
			final var markerLowNibble = (byte) (markerByte & 0x0F);

			if (markerHighNibble == TINY_MAP) {
				return markerLowNibble;
			}
			return switch (markerByte) {
				case MAP_8 -> unpackUINT8();
				case MAP_16 -> unpackUINT16();
				case MAP_32 -> unpackUINT32();
				default -> throw new Unexpected("Expected a map, but got: " + Integer.toHexString(markerByte));
			};
		}

		public long unpackLong() throws IOException {
			final var markerByte = this.in.readByte();
			if (markerByte >= MINUS_2_TO_THE_4) {
				return markerByte;
			}
			return switch (markerByte) {
				case INT_8 -> this.in.readByte();
				case INT_16 -> this.in.readShort();
				case INT_32 -> this.in.readInt();
				case INT_64 -> this.in.readLong();
				default -> throw new Unexpected("Expected an integer, but got: " + Integer.toHexString(markerByte));
			};
		}

		public double unpackDouble() throws IOException {
			final var markerByte = this.in.readByte();
			if (markerByte == FLOAT_64) {
				return this.in.readDouble();
			}
			throw new Unexpected("Expected a double, but got: " + Integer.toHexString(markerByte));
		}

		public byte[] unpackBytes() throws IOException {
			final var markerByte = this.in.readByte();
			switch (markerByte) {
				case BYTES_8 -> {
					return unpackRawBytes(unpackUINT8());
				}
				case BYTES_16 -> {
					return unpackRawBytes(unpackUINT16());
				}
				case BYTES_32 -> {
					var size = unpackUINT32();
					if (size <= Integer.MAX_VALUE) {
						return unpackRawBytes((int) size);
					}
					else {
						throw new Overflow("BYTES_32 too long for Java");
					}
				}
				default -> throw new Unexpected("Expected bytes, but got: 0x" + Integer.toHexString(markerByte & 0xFF));
			}
		}

		public String unpackString() throws IOException {
			final var markerByte = this.in.readByte();
			if (markerByte == TINY_STRING) { // Note no mask, so we compare to 0x80.
				return EMPTY_STRING;
			}

			return new String(unpackUtf8(markerByte), UTF_8);
		}

		/**
		 * This may seem confusing. This method exists to move forward the internal
		 * pointer when encountering a null value. The idiomatic usage would be someone
		 * using {@link #peekNextType()} to detect a null type, and then this method to
		 * "skip past it".
		 * @return null
		 * @throws IOException if the unpacked value was not null
		 */
		@SuppressWarnings("SameReturnValue")
		public Object unpackNull() throws IOException {
			final var markerByte = this.in.readByte();
			if (markerByte != NULL) {
				throw new Unexpected("Expected a null, but got: 0x" + Integer.toHexString(markerByte & 0xFF));
			}
			return null;
		}

		private byte[] unpackUtf8(byte markerByte) throws IOException {
			final var markerHighNibble = (byte) (markerByte & 0xF0);
			final var markerLowNibble = (byte) (markerByte & 0x0F);

			if (markerHighNibble == TINY_STRING) {
				return unpackRawBytes(markerLowNibble);
			}
			switch (markerByte) {
				case STRING_8 -> {
					return unpackRawBytes(unpackUINT8());
				}
				case STRING_16 -> {
					return unpackRawBytes(unpackUINT16());
				}
				case STRING_32 -> {
					var size = unpackUINT32();
					if (size <= Integer.MAX_VALUE) {
						return unpackRawBytes((int) size);
					}
					else {
						throw new Overflow("STRING_32 too long for Java");
					}
				}
				default ->
					throw new Unexpected("Expected a string, but got: 0x" + Integer.toHexString(markerByte & 0xFF));
			}
		}

		public boolean unpackBoolean() throws IOException {
			final var markerByte = this.in.readByte();
			return switch (markerByte) {
				case TRUE -> true;
				case FALSE -> false;
				default ->
					throw new Unexpected("Expected a boolean, but got: 0x" + Integer.toHexString(markerByte & 0xFF));
			};
		}

		private int unpackUINT8() throws IOException {
			return this.in.readByte() & 0xFF;
		}

		private int unpackUINT16() throws IOException {
			return this.in.readShort() & 0xFFFF;
		}

		private long unpackUINT32() throws IOException {
			return this.in.readInt() & 0xFFFFFFFFL;
		}

		private byte[] unpackRawBytes(int size) throws IOException {
			if (size == 0) {
				return EMPTY_BYTE_ARRAY;
			}
			var heapBuffer = new byte[size];
			this.in.readBytes(heapBuffer, 0, heapBuffer.length);
			return heapBuffer;
		}

		public PackType peekNextType() throws IOException {
			final var markerByte = this.in.peekByte();
			final var markerHighNibble = (byte) (markerByte & 0xF0);

			return switch (markerHighNibble) {
				case TINY_STRING -> PackType.STRING;
				case TINY_LIST -> PackType.LIST;
				case TINY_MAP -> PackType.MAP;
				case TINY_STRUCT -> PackType.STRUCT;
				default -> switch (markerByte) {
					case NULL -> PackType.NULL;
					case TRUE, FALSE -> PackType.BOOLEAN;
					case FLOAT_64 -> PackType.FLOAT;
					case BYTES_8, BYTES_16, BYTES_32 -> PackType.BYTES;
					case STRING_8, STRING_16, STRING_32 -> PackType.STRING;
					case LIST_8, LIST_16, LIST_32 -> PackType.LIST;
					case MAP_8, MAP_16, MAP_32 -> PackType.MAP;
					case STRUCT_8, STRUCT_16 -> PackType.STRUCT;
					default -> PackType.INTEGER;
				};
			};
		}

	}

	public static class PackStreamException extends IOException {

		@Serial
		private static final long serialVersionUID = -1491422133282345421L;

		protected PackStreamException(String message) {
			super(message);
		}

	}

	public static class EndOfStream extends PackStreamException {

		@Serial
		private static final long serialVersionUID = 5102836237108105603L;

		public EndOfStream(String message) {
			super(message);
		}

	}

	public static class Overflow extends PackStreamException {

		@Serial
		private static final long serialVersionUID = -923071934446993659L;

		public Overflow(String message) {
			super(message);
		}

	}

	public static class Unexpected extends PackStreamException {

		@Serial
		private static final long serialVersionUID = 5004685868740125469L;

		public Unexpected(String message) {
			super(message);
		}

	}

	public static class UnPackable extends PackStreamException {

		@Serial
		private static final long serialVersionUID = 2408740707769711365L;

		public UnPackable(String message) {
			super(message);
		}

	}

}
