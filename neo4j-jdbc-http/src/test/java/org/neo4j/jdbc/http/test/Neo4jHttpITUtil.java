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
 * Created on 15/4/2016
 */
package org.neo4j.jdbc.http.test;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.harness.junit.rule.Neo4jRule;

import java.io.File;
import java.util.Arrays;

@RunWith(Parameterized.class)
public abstract class Neo4jHttpITUtil extends Neo4jHttpUnitTestUtil {

	@Parameterized.Parameters
	public static Iterable<? extends Object> data() {
//		return Arrays.asList(Boolean.FALSE, Boolean.TRUE);
		return Arrays.asList(Boolean.FALSE);
	}

	@Parameterized.Parameter
	public Boolean secureMode;

	@ClassRule public static Neo4jRule neo4j = new Neo4jRule()
//			.withConfig(SettingImpl.newBuilder("dbms.connector.http.enabled",
//					SettingValueParsers.BOOL, true).build(), true)
//			.withConfig(SettingImpl.newBuilder("dbms.connector.http.listen_address",
//					SettingValueParsers.STRING, ":0").build(), ":0")
			.withFixture(new File(Neo4jHttpUnitTestUtil.class.getClassLoader().getResource("data/movie.cyp").getFile()));

	@Rule public ExpectedException expectedEx = ExpectedException.none();


	protected String getJDBCUrl() {
		if(secureMode)
			return "jdbc:neo4j:" + neo4j.httpsURI().toString();
		else
			return "jdbc:neo4j:" + neo4j.httpURI().toString();
	}

}
