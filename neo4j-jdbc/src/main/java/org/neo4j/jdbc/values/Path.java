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
package org.neo4j.jdbc.values;

/**
 * A <strong>Path</strong> is a directed sequence of relationships between two nodes. This
 * generally represents a <em>traversal</em> or <em>walk</em> through a graph and
 * maintains a direction separate from that of any relationships traversed.
 * <p>
 * It is allowed to be of size 0, meaning there are no relationships in it. In this case,
 * it contains only a single node which is both the start and the end of the path.
 *
 * <pre>
 *     Path routeToStockholm = ..;
 *
 *     // Work with each segment of the path
 *     for( Segment segment : routeToStockholm )
 *     {
 *
 *     }
 * </pre>
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public interface Path extends AsValue, Iterable<Path.Segment> {

	/**
	 * Returns the start node of this path.
	 * @return the start node of this path
	 */
	Node start();

	/**
	 * Returns the end node of this path.
	 * @return the end node of this path
	 */
	Node end();

	/**
	 * Returns the number of segments in this path, which will be the same as the number.
	 * @return the number of segments in this path, which will be the same as the number
	 * of relationships
	 */
	int length();

	/**
	 * Returns true if the specified node is contained in this path.
	 * @param node the node to check for
	 * @return true if the specified node is contained in this path
	 */
	boolean contains(Node node);

	/**
	 * Returns true if the specified relationship is contained in this path.
	 * @param relationship the relationship to check for
	 * @return true if the specified relationship is contained in this path
	 */
	boolean contains(Relationship relationship);

	/**
	 * Create an iterable over the nodes in this path, nodes will appear in the same order
	 * as they appear in the path.
	 * @return an {@link java.lang.Iterable} of all nodes in this path
	 */
	Iterable<Node> nodes();

	/**
	 * Create an iterable over the relationships in this path. The relationships will
	 * appear in the same order as they appear in the path.
	 * @return an {@link java.lang.Iterable} of all relationships in this path
	 */
	Iterable<Relationship> relationships();

	/**
	 * A segment combines a relationship in a path with a start and end node that describe
	 * the traversal direction for that relationship. This exists because the relationship
	 * has a direction between the two nodes that is separate and potentially different
	 * from the direction of the path. {@code
	 * Path: (n1)-[r1]->(n2)<-[r2]-(n3)
	 * Segment 1: (n1)-[r1]->(n2)
	 * Segment 2: (n2)<-[r2]-(n3)
	 * }
	 */
	interface Segment {

		/**
		 * Returns the relationship underlying this path segment.
		 * @return the relationship underlying this path segment
		 */
		Relationship relationship();

		/**
		 * The node that this segment starts at.
		 * @return the start node
		 */
		Node start();

		/**
		 * The node that this segment ends at.
		 * @return the end node
		 */
		Node end();

	}

}
