/**
 * Copyright (c) 2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 29/11/17
 */
package org.neo4j.jdbc.http;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.jdbc.http.driver.Neo4jResult;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class HttpNeo4jResultSetMetaDataTest {

	HttpNeo4jResultSetMetaData httpResultSetMetadata;

	@Before
	public void setUp() {
		List<Map> rows = new ArrayList<>();
		Map<String, List> map = new HashMap<>();
		List<Object> row = new ArrayList<>();

		row.add(null);
		row.add("string");
		row.add(1);
		row.add(1L);
		row.add(true);
		row.add(1.2);
		row.add(1.2F);
		row.add(new HashMap<>());
		row.add(new ArrayList());

		map.put("row", row);

		rows.add(map);

		Neo4jResult result = mock(Neo4jResult.class);
		when(result.getRows()).thenReturn(rows);
		this.httpResultSetMetadata = new HttpNeo4jResultSetMetaData(result);
	}
	@Test
	public void getColumnClassNameShouldReturnTheCorrectValue() throws SQLException {
		assertEquals(null, this.httpResultSetMetadata.getColumnClassName(1));
		assertEquals("java.lang.String", this.httpResultSetMetadata.getColumnClassName(2));
		assertEquals("java.lang.Long", this.httpResultSetMetadata.getColumnClassName(3));
		assertEquals("java.lang.Long", this.httpResultSetMetadata.getColumnClassName(4));
		assertEquals("java.lang.Boolean", this.httpResultSetMetadata.getColumnClassName(5));
		assertEquals("java.lang.Double", this.httpResultSetMetadata.getColumnClassName(6));
		assertEquals("java.lang.Double", this.httpResultSetMetadata.getColumnClassName(7));
		assertEquals("java.util.Map", this.httpResultSetMetadata.getColumnClassName(8));
		assertEquals("java.sql.Array", this.httpResultSetMetadata.getColumnClassName(9));
	}
	@Test
	public void getColumnTypeShouldReturnTheCorrectValue() throws SQLException {
		assertEquals(Types.NULL, this.httpResultSetMetadata.getColumnType(1));
		assertEquals(Types.VARCHAR, this.httpResultSetMetadata.getColumnType(2));
		assertEquals(Types.INTEGER, this.httpResultSetMetadata.getColumnType(3));
		assertEquals(Types.INTEGER, this.httpResultSetMetadata.getColumnType(4));
		assertEquals(Types.BOOLEAN, this.httpResultSetMetadata.getColumnType(5));
		assertEquals(Types.FLOAT, this.httpResultSetMetadata.getColumnType(6));
		assertEquals(Types.FLOAT, this.httpResultSetMetadata.getColumnType(7));
		assertEquals(Types.JAVA_OBJECT, this.httpResultSetMetadata.getColumnType(8));
		assertEquals(Types.ARRAY, this.httpResultSetMetadata.getColumnType(9));
	}
}
