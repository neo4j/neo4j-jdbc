/*
 *
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
 * Created on 24/4/2016
 *
 */
package org.neo4j.jdbc.example.mybatis;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.jdbc.example.mybatis.bean.Actor;
import org.neo4j.jdbc.example.mybatis.util.ActorManager;
import org.neo4j.jdbc.example.mybatis.util.ConnectionFactory;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class ActorManagerBoltTestWithLocalServer extends MybatisTest {

	/*
	 * Need to start a neo4j instance and load the "Movie" example graph  
	 */
	@Test
	@Ignore
	public void testSelectActorBornIn1973() {
		ConnectionFactory.getSqlSessionFactory("bolt-database-config.xml");
		
		Actor actor = ActorManager.selectActorByBorn(1973);
		Assert.assertNotNull(actor);
		Assert.assertEquals(1973, actor.getBorn());
		Assert.assertEquals("Dave Chappelle", actor.getName());
	}}
