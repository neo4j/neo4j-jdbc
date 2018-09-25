/*
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
 * Created on 18/02/16
 */
package org.neo4j.jdbc.bolt.data;

import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.driver.internal.*;
import org.neo4j.driver.internal.spi.Connection;
import org.neo4j.driver.internal.value.FloatValue;
import org.neo4j.driver.internal.value.IntegerValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Entity;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;

import java.util.*;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class ResultSetData {
	public static List<Object[]> RECORD_LIST_EMPTY = Collections.emptyList();
	public static List<Object[]> RECORD_LIST_ONE_ELEMENT;
	public static List<Object[]> RECORD_LIST_ONE_NULL_ELEMENT;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_DIFF;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_MIXED;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_NODES;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_PATHS;
	public static List<Object[]> RECORD_LIST_MORE_ELEMENTS_RELATIONS;
	public static List<Object[]> RECORD_LIST_WITH_ARRAY;

	public static String[] KEYS_RECORD_LIST_EMPTY                   = new String[] {};
	public static String[] KEYS_RECORD_LIST_ONE_ELEMENT             = new String[] { "columnA", "columnB" };
	public static String[] KEYS_RECORD_LIST_ONE_NULL_ELEMENT        = KEYS_RECORD_LIST_ONE_ELEMENT;
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS           = KEYS_RECORD_LIST_ONE_ELEMENT;
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_DIFF      = new String[] { "columnA", "columnB", "columnC" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED     = new String[] { "columnInt", "columnString", "columnFloat", "columnShort", "columnDouble",
			"columnBoolean", "columnLong", "columnNull", "columnMap" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_NODES     = new String[] { "node" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_PATHS     = new String[] { "path" };
	public static String[] KEYS_RECORD_LIST_MORE_ELEMENTS_RELATIONS = new String[] { "relation" };
	public static String[] KEYS_RECORD_LIST_WITH_ARRAY              = new String[] { "array" };

	//private static Method runResponseCollectorMethod;
	//private static Method pullAllResponseCollectorMethod;

	private static Path path1;
	private static Path path2;

	@BeforeClass public static void initialize() {
		RECORD_LIST_ONE_ELEMENT = new LinkedList<>();
		RECORD_LIST_ONE_ELEMENT.add(new Object[] { "valueA1", "valueB1" });

		RECORD_LIST_ONE_NULL_ELEMENT = new LinkedList<>();
		RECORD_LIST_ONE_NULL_ELEMENT.add(new Object[] { null, null });
		
		RECORD_LIST_MORE_ELEMENTS = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA1", "valueB1" });
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA2", "valueB2" });
		RECORD_LIST_MORE_ELEMENTS.add(new Object[] { "valueA3", "valueB3" });

		RECORD_LIST_MORE_ELEMENTS_MIXED = new LinkedList<>();

		Map<String, Object> map = new HashMap<>();
		map.put("key", null);

		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 1, "value1", 0.1f, (short) 1, 02.29D, true, 2L, null, map });
		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 2, "value2", 0.2f, (short) 2, 20.16D, false, 6L, null, map });
		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 3, "1.23", 0.3f, (short) 3, 12.34D, true, 4L, null, map });
		RECORD_LIST_MORE_ELEMENTS_MIXED.add(new Object[] { 4, "22", 0.4f, (short) 4, 12.34D, false, 4L, null, map });

		RECORD_LIST_MORE_ELEMENTS_NODES = new LinkedList<>();

		RECORD_LIST_MORE_ELEMENTS_NODES.add(new Object[] { new InternalNode(1, new LinkedList<String>() {
			{
				this.add("label1");
				this.add("label2");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property1", new StringValue("value1"));
				this.put("property2", new IntegerValue(1));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_NODES.add(new Object[] { new InternalNode(2, new LinkedList<String>() {
			{
				this.add("label");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new FloatValue(1.6));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_RELATIONS = new LinkedList<>();

		RECORD_LIST_MORE_ELEMENTS_RELATIONS.add(new Object[] { new InternalRelationship(1, 1, 2, "type1", new HashMap<String, Value>() {
			{
				this.put("property1", new StringValue("value"));
				this.put("property2", new IntegerValue(100));
			}
		}) });

		RECORD_LIST_MORE_ELEMENTS_RELATIONS.add(new Object[] { new InternalRelationship(2, 3, 4, "type2", new HashMap<String, Value>() {
			{
				this.put("property", new FloatValue(2.6));
			}
		}) });

		setUpPaths();

		RECORD_LIST_MORE_ELEMENTS_PATHS = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS_PATHS.add(new Object[] { path1 });
		RECORD_LIST_MORE_ELEMENTS_PATHS.add(new Object[] { path2 });

		RECORD_LIST_MORE_ELEMENTS_DIFF = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS_DIFF.add(new Object[] { "valueA", "valueB" });
		RECORD_LIST_MORE_ELEMENTS_DIFF.add(new Object[] { "valueA", "valueB", "valueC" });

		RECORD_LIST_WITH_ARRAY = new ArrayList<>();
		RECORD_LIST_WITH_ARRAY.add(new Object[] { new String[] { "a", "b", "c" } });
		RECORD_LIST_WITH_ARRAY.add(new Object[] { new Integer[] { 5, 10, 99 } });
		RECORD_LIST_WITH_ARRAY.add(new Object[] { new Boolean[] { true, false, false } });
		RECORD_LIST_WITH_ARRAY.add(new Object[] { new Double[] { 6.5, 4.3, 2.1 } });

		fixPublicForInternalResultCursor();
	}

	private static void setUpPaths() {

		Node node1 = new InternalNode(1, new LinkedList<String>() {
			{
				this.add("label1");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new StringValue("value"));
			}
		});

		Node node2 = new InternalNode(2, new LinkedList<String>() {
			{
				this.add("label1");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new StringValue("value2"));
			}
		});

		org.neo4j.driver.v1.types.Relationship rel1 = new InternalRelationship(3, 1, 2, "type", new HashMap<String, Value>() {
			{
				this.put("relProperty", new StringValue("value3"));
			}
		});

		List<Entity> entities1 = new ArrayList<>();
		entities1.add(node1);
		entities1.add(rel1);
		entities1.add(node2);

		path1 = new InternalPath(entities1);

		Node node3 = new InternalNode(4, new LinkedList<String>() {
			{
				this.add("label1");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new StringValue("value"));
			}
		});

		Node node4 = new InternalNode(5, new LinkedList<String>() {
			{
				this.add("label1");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new StringValue("value2"));
			}
		});

		Node node5 = new InternalNode(6, new LinkedList<String>() {
			{
				this.add("label1");
			}
		}, new HashMap<String, Value>() {
			{
				this.put("property", new StringValue("value3"));
			}
		});

		org.neo4j.driver.v1.types.Relationship rel2 = new InternalRelationship(7, 4, 5, "type", new HashMap<String, Value>() {
			{
				this.put("relProperty", new StringValue("value4"));
			}
		});

		org.neo4j.driver.v1.types.Relationship rel3 = new InternalRelationship(8, 6, 5, "type", new HashMap<String, Value>() {
			{
				this.put("relProperty", new StringValue("value5"));
			}
		});

		List<Entity> entities2 = new ArrayList<>();
		entities2.add(node3);
		entities2.add(rel2);
		entities2.add(node4);
		entities2.add(rel3);
		entities2.add(node5);

		path2 = new InternalPath(entities2);
	}

	/**
	 * open up some package scope method for public usage
	 */
	private static void fixPublicForInternalResultCursor() {
/*
		try {
			runResponseCollectorMethod = InternalStatementResult.class.getDeclaredMethod("runResponseCollector");
			runResponseCollectorMethod.setAccessible(true);
			pullAllResponseCollectorMethod = InternalStatementResult.class.getDeclaredMethod("pullAllResponseCollector");
			pullAllResponseCollectorMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
*/
	}

	/**
	 * hackish way to get a {@link InternalStatementResult}
	 *
	 * @param keys
	 * @param data
	 * @return
	 */
	public static StatementResult buildResultCursor(String[] keys, final List<Object[]> data) {

		try {
			Connection connection = mock(Connection.class);

			StatementResult cursor = mock(StatementResult.class);
			final List<String> columns = asList(keys);
			when(cursor.keys()).thenReturn(columns);

			final Iterator<Object[]> it = data.iterator();
			when(cursor.hasNext()).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
					return it.hasNext();
				}
			});
			when (cursor.next()).thenAnswer(new Answer<Record>() {
				@Override
				public Record answer(InvocationOnMock invocationOnMock) throws Throwable {
					return new InternalRecord(columns, Values.values(it.next()));
				}
			});
			when (cursor.peek()).thenAnswer(new Answer<Record>() {
				@Override
				public Record answer(InvocationOnMock invocationOnMock) throws Throwable {
					return new InternalRecord(columns, Values.values(data.get(0)));
				}
			});
/*
			InternalStatementResult cursor = new InternalStatementResult(connection, null);
			StreamCollector responseCollector = (StreamCollector) runResponseCollectorMethod.invoke(cursor);
			responseCollector.keys(keys);
			responseCollector.done();

			StreamCollector pullAllResponseCollector = (StreamCollector) pullAllResponseCollectorMethod.invoke(cursor);

			for(Object[] values : data){
				pullAllResponseCollector.record(values(values));
			}
			pullAllResponseCollector.done();
			connection.run("<unknown>", ParameterSupport.NO_PARAMETERS, responseCollector);
			connection.pullAll(pullAllResponseCollector);
			//connection.sendAll();
*/
			return cursor;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
