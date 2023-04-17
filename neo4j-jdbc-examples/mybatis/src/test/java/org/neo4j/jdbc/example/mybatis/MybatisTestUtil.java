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

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.example.mybatis.mapper.ActorMapper;
import org.neo4j.jdbc.example.mybatis.util.ConnectionFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import javax.sql.DataSource;
import java.util.Properties;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class MybatisTestUtil {

	public static Neo4jContainer createNeo4jContainter() {
		return new Neo4jContainer<>("neo4j:5.6.0-enterprise")
				.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
				.withAdminPassword(null);
	}

	public static void populateGraphDB(Neo4jContainer<?> neo4j) {
		populateGraphDB(neo4j.getBoltUrl(), null, null);
	}

	public static void populateGraphDB(String url, String user, String password) {
		final AuthToken authToken = StringUtils.isBlank(user) ? AuthTokens.none() : AuthTokens.basic(user, password);
		try (org.neo4j.driver.Driver driver = GraphDatabase.driver(url, authToken);
			 Session session = driver.session()) {
			session.run("CREATE (:Person{name: 'Dave Chappelle', born: 1973})").consume();
		}
	}

	protected void buildMybatisConfiguration(String protocol, String host, int port) {
		buildMybatisConfiguration(protocol, host, port, "user", "password", false);
	}

	protected void buildMybatisConfiguration(String protocol, String host, int port, String user, String password, boolean ssl) {
		Properties prop = new Properties();
		prop.setProperty("user", user);
		prop.setProperty("password", password);
		DataSource dataSource = new UnpooledDataSource("org.neo4j.jdbc.Driver", "jdbc:neo4j:" + protocol + "://" + host + ":" + port + (ssl ? "" : "?nossl"), prop);
		TransactionFactory transactionFactory = new JdbcTransactionFactory();
		Environment environment = new Environment("development", transactionFactory, dataSource);

		Configuration configuration = new Configuration(environment);
		configuration.getMapperRegistry().addMapper(ActorMapper.class);
		configuration.addLoadedResource("org/neo4j/jdbc/example/mybatis/mapper/ActorMapper.xml");

		ConnectionFactory.getSqlSessionFactory(configuration);
	}
}
