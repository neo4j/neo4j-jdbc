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
package it.larusba.neo4j.jdbc.bolt.data;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class StatementData {
	public static String STATEMENT_MATCH_ALL                            = "MATCH (n) RETURN n;";
	public static String STATEMENT_MATCH_ALL_STRING                     = "MATCH (n:User) RETURN n.name;";
	public static String STATEMENT_MATCH_NODES                          = "MATCH (n:User) RETURN n;";
	public static String STATEMENT_MATCH_NODES_MORE                     = "MATCH (n:User)-[]->(s:Session) RETURN n, s;";
	public static String STATEMENT_MATCH_MISC                           = "MATCH (n:User) RETURN n, n.name;";
	public static String STATEMENT_MATCH_RELATIONS                      = "MATCH ()-[r:CONNECTED_IN]-() RETURN r;";
	public static String STATEMENT_MATCH_NODES_RELATIONS                = "MATCH (n:User)-[r:CONNECTED_IN]->(s:Session) RETURN n, r, s";
	public static String STATEMENT_CREATE                               = "CREATE (n:User {name:\"test\"});";
	public static String STATEMENT_CREATE_REV                           = "MATCH (n:User {name:\"test\"}) DELETE n;";
	public static String STATEMENT_CREATE_TWO_PROPERTIES                = "CREATE (n:User {name:\"test\", surname:\"testAgain\"});";
	public static String STATEMENT_CREATE_TWO_PROPERTIES_REV            = "MATCH (n:USer {name:\"test\", surname:\"testAgain\"}) DETACH DELETE n;";
	public static String STATEMENT_MATCH_ALL_STRING_PARAMETRIC          = "MATCH (n) WHERE n.name = ? RETURN n.surname;";
	public static String STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS      = "MATCH (n:User) CREATE (n)-[:CONNECTED_IN {date:1459248821051}]->(:Session {status:true});";
	public static String STATEMENT_CREATE_OTHER_TYPE_AND_RELATIONS_REV  = "MATCH (s:Session) DETACH DELETE s;";
	public static String STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC     = "CREATE (n:User {name:?, surname:?});";
	public static String STATEMENT_CREATE_TWO_PROPERTIES_PARAMETRIC_REV = "MATCH (n:User {name:?, surname:?}) DETACH DELETE n;";
	public static String STATEMENT_CLEAR_DB                              = "MATCH (n) DETACH DELETE n;";
	public static String STATEMENT_COUNT_NODES                          = "MATCH (n) RETURN count(n) AS total;";
}
