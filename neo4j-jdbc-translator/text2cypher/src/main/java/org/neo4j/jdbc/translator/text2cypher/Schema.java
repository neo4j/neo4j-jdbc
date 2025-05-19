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
package org.neo4j.jdbc.translator.text2cypher;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.parser.CypherParser;

/**
 * This schema is designed analogue to <a href=
 * "https://github.com/langchain-ai/langchain/blob/master/libs/community/langchain_community/graphs/neo4j_graph.py#L17">langchain_community/graphs/neo4j_graph.py</a>.
 * There is no optimal schema solution yet, however the above proofed itself to work "good
 * enough". We can switch this out for the JDBC connection metadata anytime.
 *
 * @author Michael J. Simons
 */
final class Schema {

	private final Map<String, List<String>> nodeProperties;

	private final Map<String, List<String>> relationshipProperties;

	private final List<Configuration.RelationshipDefinition> relationships;

	private Schema(Map<String, List<String>> nodeProperties, Map<String, List<String>> relationshipProperties,
			List<Configuration.RelationshipDefinition> relationships) {
		this.nodeProperties = nodeProperties;
		this.relationshipProperties = relationshipProperties;
		this.relationships = relationships;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("Node properties are the following:\n");
		this.nodeProperties.forEach((label, properties) -> sb.append(label)
			.append(" ")
			.append(properties.stream().collect(Collectors.joining(", ", "{", "}")))
			.append(","));
		sb.replace(sb.length() - 1, sb.length(), "\nRelationship properties are the following:\n");
		this.relationshipProperties.forEach((label, properties) -> sb.append(label)
			.append(" ")
			.append(properties.stream().collect(Collectors.joining(", ", "{", "}")))
			.append(","));
		sb.replace(sb.length() - 1, sb.length(), "\nThe relationships are the following:\n");
		this.relationships.forEach(r -> sb.append("(:")
			.append(r.sourceLabel())
			.append(")-[:")
			.append(r.type())
			.append("]->(:")
			.append(r.targetLabel())
			.append(")")
			.append(","));
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	static Schema from(Connection connection) throws SQLException {
		try (var statement = connection.createStatement()) {
			var nodeProperties = new HashMap<String, List<String>>();
			try (var rs = statement.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					CALL apoc.meta.data()
					YIELD label, other, elementType, type, property
					WHERE NOT type = "RELATIONSHIP" AND elementType = "node"
					RETURN label, collect(property + ": " + type) AS properties
					""")) {
				while (rs.next()) {
					var label = rs.getString("label");
					var properties = ((List<?>) rs.getObject("properties", List.class)).stream()
						.map(String.class::cast)
						.toList();
					nodeProperties.put(label, properties);
				}
			}

			var relationshipProperties = new HashMap<String, List<String>>();
			try (var rs = statement.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					CALL apoc.meta.data()
					YIELD label, other, elementType, type, property
					WHERE NOT type = "RELATIONSHIP" AND elementType = "relationship"
					RETURN label, collect(property + ": " + type) AS properties
					""")) {
				while (rs.next()) {
					var label = rs.getString("label");
					var properties = ((List<?>) rs.getObject("properties", List.class)).stream()
						.map(String.class::cast)
						.toList();
					relationshipProperties.put(label, properties);
				}
			}

			var relationships = new ArrayList<Configuration.RelationshipDefinition>();
			try (var rs = statement.executeQuery("""
					/*+ NEO4J FORCE_CYPHER */
					CALL apoc.meta.data()
					YIELD label, other, elementType, type, property
					WHERE type = "RELATIONSHIP" AND elementType = "node"
					UNWIND other AS other_node
					RETURN label AS start, property AS type, toString(other_node) AS end
					""")) {
				while (rs.next()) {
					relationships.add(new Configuration.RelationshipDefinition(rs.getString("start"),
							rs.getString("type"), rs.getString("end")));
				}
			}

			return new Schema(Map.copyOf(nodeProperties), Map.copyOf(relationshipProperties),
					List.copyOf(relationships));
		}
	}

	String enforceRelationships(String cypher) {
		var configuration = Configuration.newConfig()
			.withPrettyPrint(true)
			.alwaysEscapeNames(true)
			.withEnforceSchema(true);

		this.relationships.forEach(configuration::withRelationshipDefinition);
		return Renderer.getRenderer(configuration.build()).render(CypherParser.parseStatement(cypher));
	}

}
