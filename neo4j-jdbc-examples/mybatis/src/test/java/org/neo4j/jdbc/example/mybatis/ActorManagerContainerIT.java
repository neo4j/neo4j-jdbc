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

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.jdbc.example.mybatis.bean.Actor;
import org.neo4j.jdbc.example.mybatis.mapper.ActorMapper;
import org.neo4j.jdbc.example.mybatis.util.ActorManager;
import org.neo4j.jdbc.example.mybatis.util.ConnectionFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class ActorManagerContainerIT {

	private void buildMybatisConfiguration(String uri,
											 String user,
											 String password) {
		Properties prop = new Properties();
		prop.setProperty("user", user);
		prop.setProperty("password", password);
		DataSource dataSource = new UnpooledDataSource("org.neo4j.jdbc.Driver", "jdbc:neo4j:" + uri, prop);
		TransactionFactory transactionFactory = new JdbcTransactionFactory();
		Environment environment = new Environment("development", transactionFactory, dataSource);

		Configuration configuration = new Configuration(environment);
		configuration.getMapperRegistry().addMapper(ActorMapper.class);
		configuration.addLoadedResource("org/neo4j/jdbc/example/mybatis/mapper/ActorMapper.xml");

		ConnectionFactory.getSqlSessionFactory(configuration);
	}

	public static Neo4jContainer neo4jContainer;

	@BeforeClass
	public static void setUp() throws URISyntaxException {
		try {
			neo4jContainer = (Neo4jContainer) new Neo4jContainer("neo4j:4.0.2-enterprise")
					.waitingFor(new LogMessageWaitStrategy().withRegEx(".*Bolt enabled on .*:7687\\.\n"))
					.withEnv("NEO4J_AUTH", "neo4j/password")
					.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
			neo4jContainer.start();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		Assume.assumeTrue("neo4j container should be not null", neo4jContainer != null);
		Assume.assumeTrue("neo4j container should be up and running", neo4jContainer.isRunning());
		try (final Driver driver = GraphDatabase.driver(new URI(neo4jContainer.getBoltUrl()), AuthTokens.basic("neo4j", neo4jContainer.getAdminPassword()))) {
			final String foo = "foo";
			try (final Session session = driver.session(SessionConfig.forDatabase("system"))) {
				session.writeTransaction(tx -> tx.run("create database " + foo + " if not exists"));
			}
			try (final Session session = driver.session(SessionConfig.forDatabase(foo))) {
				session.writeTransaction(tx -> tx.run("create (p:Person{name: 'Dave Chappelle', born: 1973})"));
			}
		}
	}

	@Test
	public void testMybatisViaBolt() {
		buildMybatisConfiguration(neo4jContainer.getBoltUrl() + "?database=foo&nossl", "neo4j", neo4jContainer.getAdminPassword());
		Actor actor = ActorManager.selectActorByBorn(1973);
		Assert.assertNotNull(actor);
		Assert.assertEquals(1973, actor.getBorn());
		Assert.assertEquals("Dave Chappelle", actor.getName());
	}
}
