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
 * Created on 19/02/16
 */
package it.neo4j.jdbc.bolt.data;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class StatementData {
	public static String STATEMENT_MATCH_ALL = "MATCH (n) RETURN n;";
	public static String STATEMENT_MATCH_ALL_STRING = "MATCH (n:User) RETURN n.name";
	public static String STATEMENT_CREATE = "CREATE (n:User {name:\"test\"});";
	public static String STATEMENT_CREATE_REV = "MATCH (n:User {name:\"test\"}) DELETE n;";
	public static String STATEMENT_COUNT = "MATCH (n) RETURN COUNT(n);";
}
