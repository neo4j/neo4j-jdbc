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
 * Created on 18/02/16
 */
package it.neo4j.jdbc.bolt.data;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.internal.InternalRecord;
import org.neo4j.driver.internal.value.FloatValue;
import org.neo4j.driver.internal.value.IntegerValue;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.*;

import java.util.*;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class ResultSetData {
	public static List<Record> RECORD_LIST_EMPTY;
	public static List<Record> RECORD_LIST_ONE_ELEMENT;
	public static List<Record> RECORD_LIST_MORE_ELEMENTS;
	public static List<Record> RECORD_LIST_MORE_ELEMENTS_MIXED;

	public static List<String> KEYS_RECORD_LIST_EMPTY;
	public static List<String> KEYS_RECORD_LIST_ONE_ELEMENT;
	public static List<String> KEYS_RECORD_LIST_MORE_ELEMENTS;
	public static List<String> KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED;

	public static ResultSummary RESULT_SUMMARY;

	@BeforeClass public static void initialize() {
		RECORD_LIST_EMPTY = new LinkedList<>();
		RECORD_LIST_ONE_ELEMENT = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS = new LinkedList<>();
		RECORD_LIST_MORE_ELEMENTS_MIXED = new LinkedList<>();

		Record recordA = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA1"), new StringValue("valueB1") });

		RECORD_LIST_ONE_ELEMENT.add(recordA);

		Record recordB = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA2"), new StringValue("valueB2") });

		Record recordC = new InternalRecord(new LinkedList<String>() {{
			add("columnA");
			add("columnB");
		}}, new HashMap<String, Integer>() {{
			put("columnA", 0);
			put("columnB", 1);
		}}, new Value[] { new StringValue("valueA3"), new StringValue("valueB3") });

		RECORD_LIST_MORE_ELEMENTS.add(recordA);
		RECORD_LIST_MORE_ELEMENTS.add(recordB);
		RECORD_LIST_MORE_ELEMENTS.add(recordC);

		KEYS_RECORD_LIST_EMPTY = getKeys(RECORD_LIST_EMPTY);
		KEYS_RECORD_LIST_ONE_ELEMENT = getKeys(RECORD_LIST_ONE_ELEMENT);
		KEYS_RECORD_LIST_MORE_ELEMENTS = getKeys(RECORD_LIST_MORE_ELEMENTS);

		RESULT_SUMMARY = new ResultSummary() {

			@Override public Statement statement() {
				return null;
			}

			@Override public UpdateStatistics updateStatistics() {
				return null;
			}

			@Override public StatementType statementType() {
				return null;
			}

			@Override public boolean hasPlan() {
				return false;
			}

			@Override public boolean hasProfile() {
				return false;
			}

			@Override public Plan plan() {
				return null;
			}

			@Override public ProfiledPlan profile() {
				return null;
			}

			@Override public List<Notification> notifications() {
				return null;
			}
		};

		Record recordAMixed = new InternalRecord(new LinkedList<String>() {{
			add("columnInt");
			add("columnString");
			add("columnDouble");
		}}, new HashMap<String, Integer>() {{
			put("columnInt", 0);
			put("columnString", 1);
			put("columnDouble", 2);
		}}, new Value[] { new IntegerValue(1L), new StringValue("value1"), new FloatValue(0.1) });

		Record recordBMixed = new InternalRecord(new LinkedList<String>() {{
			add("columnString");
			add("columnDouble");
			add("columnInt");
		}}, new HashMap<String, Integer>() {{
			put("columnString", 0);
			put("columnDouble", 1);
			put("columnInt", 2);
		}}, new Value[] { new StringValue("value2"), new FloatValue(0.2), new IntegerValue(2L) });

		RECORD_LIST_MORE_ELEMENTS_MIXED.add(recordAMixed);
		RECORD_LIST_MORE_ELEMENTS_MIXED.add(recordBMixed);

		KEYS_RECORD_LIST_MORE_ELEMENTS_MIXED = getKeys(RECORD_LIST_MORE_ELEMENTS_MIXED);
	}

	/**
	 * Calculates the total keys from a list of record with potentially different keys
	 *
	 * @param records a list of records to retrieve keys from
	 * @return a list of keys
	 */
	private static List<String> getKeys(List<Record> records) {
		Set<String> keysSet = new HashSet<>();

		for (Record record : records) {
			keysSet.addAll(record.keys());
		}

		List<String> keys = new LinkedList<>();
		keys.addAll(keysSet);

		return keys;
	}

	@Test public void testGetKeys() {
		Assert.assertEquals(2, getKeys(RECORD_LIST_MORE_ELEMENTS).size());
		Assert.assertEquals(2, getKeys(RECORD_LIST_ONE_ELEMENT).size());
		Assert.assertEquals(0, getKeys(RECORD_LIST_EMPTY).size());
		Assert.assertEquals(3, getKeys(RECORD_LIST_MORE_ELEMENTS_MIXED).size());
	}
}
