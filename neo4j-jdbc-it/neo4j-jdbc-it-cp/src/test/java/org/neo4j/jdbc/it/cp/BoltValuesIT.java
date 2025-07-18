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
package org.neo4j.jdbc.it.cp;

import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.jdbc.Neo4jPreparedStatement;
import org.neo4j.jdbc.values.IsoDuration;
import org.neo4j.jdbc.values.Point;
import org.neo4j.jdbc.values.Values;

import static org.assertj.core.api.Assertions.assertThat;

class BoltValuesIT extends IntegrationTestBase {

	@SuppressWarnings("unchecked")
	@Test
	void boltValuesShouldWork() throws SQLException {

		var query = """
				RETURN $point AS p,
					$map AS m,
					$duration AS d
				""";
		try (var connection = this.getConnection();
				var statement = connection.prepareStatement(query).unwrap(Neo4jPreparedStatement.class)) {

			statement.setObject("point", Values.point(4326, 56.7, 12.78));
			statement.setObject("map", Values.value(Map.of("a", "b")));
			statement.setObject("duration", Values.isoDuration(1, 2, 3, 4));
			var rs = statement.executeQuery();
			assertThat(rs.next()).isTrue();
			assertThat(rs.getObject("p", Point.class))
				.matches(p -> p.srid() == 4326 && p.x() == 56.7 && p.y() == 12.78 && Double.isNaN(p.z()));
			assertThat((Map<String, String>) rs.getObject("m", Map.class)).containsExactlyEntriesOf(Map.of("a", "b"));
			assertThat(rs.getObject("d", IsoDuration.class))
				.matches(i -> i.months() == 1L && i.days() == 2L && i.seconds() == 3L && i.nanoseconds() == 4L);

		}
	}

}
