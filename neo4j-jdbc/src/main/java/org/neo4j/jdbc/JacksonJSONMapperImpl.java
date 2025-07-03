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
package org.neo4j.jdbc;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.neo4j.jdbc.values.BooleanValue;
import org.neo4j.jdbc.values.BytesValue;
import org.neo4j.jdbc.values.DateTimeValue;
import org.neo4j.jdbc.values.DateValue;
import org.neo4j.jdbc.values.DurationValue;
import org.neo4j.jdbc.values.FloatValue;
import org.neo4j.jdbc.values.IntegerValue;
import org.neo4j.jdbc.values.ListValue;
import org.neo4j.jdbc.values.LocalDateTimeValue;
import org.neo4j.jdbc.values.LocalTimeValue;
import org.neo4j.jdbc.values.MapValue;
import org.neo4j.jdbc.values.Node;
import org.neo4j.jdbc.values.NodeValue;
import org.neo4j.jdbc.values.NullValue;
import org.neo4j.jdbc.values.Path;
import org.neo4j.jdbc.values.PathValue;
import org.neo4j.jdbc.values.PointValue;
import org.neo4j.jdbc.values.Relationship;
import org.neo4j.jdbc.values.RelationshipValue;
import org.neo4j.jdbc.values.StringValue;
import org.neo4j.jdbc.values.TimeValue;
import org.neo4j.jdbc.values.Value;
import org.neo4j.jdbc.values.Values;

/**
 * A {@link Value} to JSON mapper based on Jackson-Databind. The class will be loaded via
 * reflection, hence it appears as unused.
 *
 * @author Michael J. Simons
 * @since 6.7.0
 */
final class JacksonJSONMapperImpl implements JSONMapper<JsonNode> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@SuppressWarnings("squid:S3776") // Has a lot of ifs, but isn't really complex
	@Override
	public JsonNode toJson(Value value) {

		// Formats are aligned with the Query API.
		if (value instanceof BooleanValue booleanValue) {
			return BooleanNode.valueOf(booleanValue.asBoolean());
		}
		else if (value instanceof BytesValue bytesValue) {
			return TextNode.valueOf(Base64.getEncoder().encodeToString(bytesValue.asByteArray()));
		}
		else if (value instanceof DateTimeValue dateTimeValue) {
			String textValue;
			if (dateTimeValue.asZonedDateTime().getZone().normalized() instanceof ZoneOffset) {
				textValue = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTimeValue.asOffsetDateTime());
			}
			else {
				textValue = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(dateTimeValue.asZonedDateTime());
			}
			return TextNode.valueOf(textValue);
		}
		else if (value instanceof DateValue dateValue) {
			return TextNode.valueOf(DateTimeFormatter.ISO_LOCAL_DATE.format(dateValue.asObject()));
		}
		else if (value instanceof DurationValue durationValue) {
			return TextNode.valueOf(durationValue.toString().replace("DURATION '", "").replace("'", ""));
		}
		else if (value instanceof FloatValue floatValue) {
			return DoubleNode.valueOf(floatValue.asDouble());
		}
		else if (value instanceof IntegerValue integerValue) {
			return LongNode.valueOf(integerValue.asLong());
		}
		else if (value instanceof ListValue listValue) {
			return mapList(listValue);
		}
		else if (value instanceof LocalDateTimeValue localDateTimeValue) {
			return TextNode.valueOf(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTimeValue.asObject()));
		}
		else if (value instanceof LocalTimeValue localTimeValue) {
			return TextNode.valueOf(DateTimeFormatter.ISO_LOCAL_TIME.format(localTimeValue.asObject()));
		}
		else if (value instanceof MapValue mapValue) {
			return mapMap(mapValue);
		}
		else if (value instanceof NodeValue nodeValue) {
			return mapNode(nodeValue.asNode());
		}
		else if (value == null || value instanceof NullValue) {
			return NullNode.getInstance();
		}
		else if (value instanceof PathValue pathValue) {
			return mapPath(pathValue.asPath());
		}
		else if (value instanceof PointValue pointValue) {
			var point = pointValue.asPoint();
			var is3d = !Double.isNaN(point.z());
			return TextNode.valueOf("SRID=" + point.srid() + ";POINT" + (is3d ? " Z " : " ") + "(" + point.x() + " "
					+ point.y() + (is3d ? " " + point.z() + ")" : ")"));
		}
		else if (value instanceof RelationshipValue relationshipValue) {
			return mapRelationship(relationshipValue.asRelationship());
		}
		else if (value instanceof StringValue stringValue) {
			return TextNode.valueOf(stringValue.asString());
		}
		else if (value instanceof TimeValue timeValue) {
			return TextNode.valueOf(DateTimeFormatter.ISO_OFFSET_TIME.format(timeValue.asObject()));
		}

		throw new UnsupportedOperationException(
				"Cannot map %s to a %s".formatted(value, this.getBaseType().getSimpleName()));
	}

	@Override
	public Value fromJson(Object in) {
		if (in == null) {
			return Values.NULL;
		}
		if (!(in instanceof JsonNode json)) {
			throw new UnsupportedOperationException("Cannot map objects of type %s to %s"
				.formatted(in.getClass().getName(), this.getBaseType().getSimpleName()));
		}
		if (json.isNull()) {
			return Values.NULL;
		}
		else if (json instanceof BooleanNode booleanNode) {
			return Values.value(booleanNode.booleanValue());
		}
		else if (json instanceof BinaryNode binaryNode) {
			return Values.value(Base64.getEncoder().encodeToString(binaryNode.binaryValue()));
		}
		else if (json instanceof DecimalNode decimalNode) {
			return Values.value(decimalNode.decimalValue().toString());
		}
		else if (json instanceof DoubleNode doubleNode) {
			return Values.value(doubleNode.doubleValue());
		}
		else if (json instanceof FloatNode floatNode) {
			return Values.value(floatNode.floatValue());
		}
		else if (json instanceof NumericNode numericNode) {
			return Values.value(numericNode.longValue());
		}
		else if (json instanceof TextNode textNode) {
			return Values.value(textNode.textValue());
		}
		else if (json instanceof ObjectNode objectNode) {
			var result = new LinkedHashMap<String, Value>();
			objectNode.forEachEntry((k, v) -> result.put(k, fromJson(v)));
			return Values.value(result);
		}
		else if (json instanceof ArrayNode arrayNode) {
			var result = new ArrayList<Value>();
			arrayNode.forEach(v -> result.add(fromJson(v)));
			return Values.value(result);
		}

		throw new UnsupportedOperationException("Cannot map %s to a %s".formatted(json, Value.class.getSimpleName()));
	}

	@Override
	public Class<JsonNode> getBaseType() {
		return JsonNode.class;
	}

	private JsonNode mapPath(Path path) {

		var result = this.objectMapper.createArrayNode();
		result.add(mapNode(path.start()));
		path.relationships().forEach(relationship -> result.add(mapRelationship(relationship)));
		result.add(mapNode(path.end()));
		return result;
	}

	private JsonNode mapRelationship(Relationship relationship) {
		var result = this.objectMapper.createObjectNode();

		result.put("elementId", relationship.elementId());
		result.put("startNodeElementId", relationship.startNodeElementId());
		result.put("endNodeElementId", relationship.endNodeElementId());
		result.put("type", relationship.type());

		var properties = this.objectMapper.createObjectNode();
		relationship.keys().forEach(key -> properties.set(key, toJson(relationship.get(key))));
		result.set("properties", properties);
		return result;
	}

	private JsonNode mapList(ListValue listValue) {
		var result = this.objectMapper.createArrayNode();
		listValue.values().forEach(value -> result.add(toJson(value)));
		return result;
	}

	private JsonNode mapMap(MapValue mapValue) {
		var result = this.objectMapper.createObjectNode();
		mapValue.keys().forEach(key -> result.set(key, toJson(mapValue.get(key))));
		return result;
	}

	private JsonNode mapNode(Node node) {
		var result = this.objectMapper.createObjectNode();

		result.put("elementId", node.elementId());

		var labels = this.objectMapper.createArrayNode();
		node.labels().forEach(labels::add);
		result.set("labels", labels);

		var properties = this.objectMapper.createObjectNode();
		node.keys().forEach(key -> properties.set(key, toJson(node.get(key))));
		result.set("properties", properties);
		return result;
	}

}
