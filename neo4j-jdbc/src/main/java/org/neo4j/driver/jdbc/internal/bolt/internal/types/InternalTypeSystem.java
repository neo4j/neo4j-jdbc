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
package org.neo4j.driver.jdbc.internal.bolt.internal.types;

import org.neo4j.driver.jdbc.internal.bolt.Value;
import org.neo4j.driver.jdbc.internal.bolt.types.Type;
import org.neo4j.driver.jdbc.internal.bolt.types.TypeSystem;

/**
 * Utility class for determining and working with the Cypher types of values.
 *
 * @author Neo4j Drivers Team
 * @since 1.0.0
 * @see Value
 * @see Type
 */
public final class InternalTypeSystem implements TypeSystem {

	public static final InternalTypeSystem TYPE_SYSTEM = new InternalTypeSystem();

	private final TypeRepresentation anyType = constructType(TypeConstructor.ANY);

	private final TypeRepresentation booleanType = constructType(TypeConstructor.BOOLEAN);

	private final TypeRepresentation bytesType = constructType(TypeConstructor.BYTES);

	private final TypeRepresentation stringType = constructType(TypeConstructor.STRING);

	private final TypeRepresentation numberType = constructType(TypeConstructor.NUMBER);

	private final TypeRepresentation integerType = constructType(TypeConstructor.INTEGER);

	private final TypeRepresentation floatType = constructType(TypeConstructor.FLOAT);

	private final TypeRepresentation listType = constructType(TypeConstructor.LIST);

	private final TypeRepresentation mapType = constructType(TypeConstructor.MAP);

	private final TypeRepresentation nodeType = constructType(TypeConstructor.NODE);

	private final TypeRepresentation relationshipType = constructType(TypeConstructor.RELATIONSHIP);

	private final TypeRepresentation pathType = constructType(TypeConstructor.PATH);

	private final TypeRepresentation pointType = constructType(TypeConstructor.POINT);

	private final TypeRepresentation dateType = constructType(TypeConstructor.DATE);

	private final TypeRepresentation timeType = constructType(TypeConstructor.TIME);

	private final TypeRepresentation localTimeType = constructType(TypeConstructor.LOCAL_TIME);

	private final TypeRepresentation localDateTimeType = constructType(TypeConstructor.LOCAL_DATE_TIME);

	private final TypeRepresentation dateTimeType = constructType(TypeConstructor.DATE_TIME);

	private final TypeRepresentation durationType = constructType(TypeConstructor.DURATION);

	private final TypeRepresentation nullType = constructType(TypeConstructor.NULL);

	private InternalTypeSystem() {
	}

	@Override
	public Type ANY() {
		return this.anyType;
	}

	@Override
	public Type BOOLEAN() {
		return this.booleanType;
	}

	@Override
	public Type BYTES() {
		return this.bytesType;
	}

	@Override
	public Type STRING() {
		return this.stringType;
	}

	@Override
	public Type NUMBER() {
		return this.numberType;
	}

	@Override
	public Type INTEGER() {
		return this.integerType;
	}

	@Override
	public Type FLOAT() {
		return this.floatType;
	}

	@Override
	public Type LIST() {
		return this.listType;
	}

	@Override
	public Type MAP() {
		return this.mapType;
	}

	@Override
	public Type NODE() {
		return this.nodeType;
	}

	@Override
	public Type RELATIONSHIP() {
		return this.relationshipType;
	}

	@Override
	public Type PATH() {
		return this.pathType;
	}

	@Override
	public Type POINT() {
		return this.pointType;
	}

	@Override
	public Type DATE() {
		return this.dateType;
	}

	@Override
	public Type TIME() {
		return this.timeType;
	}

	@Override
	public Type LOCAL_TIME() {
		return this.localTimeType;
	}

	@Override
	public Type LOCAL_DATE_TIME() {
		return this.localDateTimeType;
	}

	@Override
	public Type DATE_TIME() {
		return this.dateTimeType;
	}

	@Override
	public Type DURATION() {
		return this.durationType;
	}

	@Override
	public Type NULL() {
		return this.nullType;
	}

	private TypeRepresentation constructType(TypeConstructor tyCon) {
		return new TypeRepresentation(tyCon);
	}

}
