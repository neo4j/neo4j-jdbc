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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.jdbc.example.mybatis.mapper.ActorMapper;
import org.neo4j.jdbc.example.mybatis.util.ConnectionFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class MybatisTestUtil {

	public enum TestLabels implements Label {
		Person
	}
	
	protected static void populateGraphDB(GraphDatabaseService graphDatabaseService) {
		try (Transaction tx = graphDatabaseService.beginTx()) { 
			Node node = graphDatabaseService.createNode(TestLabels.Person);
			node.setProperty("name", "Dave Chappelle");
			node.setProperty("born", 1973);
			tx.success();
		}
	}

	protected void buildMybatisConfiguration(String protocol, String host, int port) {
		Properties prop = new Properties();
		prop.setProperty("user","user");
		prop.setProperty("password","password");
		DataSource dataSource = new UnpooledDataSource("org.neo4j.jdbc.Driver", "jdbc:neo4j:" + protocol + "://" + host + ":" + port + "?nossl", prop);
		TransactionFactory transactionFactory = new JdbcTransactionFactory();
		Environment environment = new Environment("development", transactionFactory, dataSource);

		Configuration configuration = new Configuration(environment);
		configuration.getMapperRegistry().addMapper(ActorMapper.class);
		configuration.addLoadedResource("org/neo4j/jdbc/example/mybatis/mapper/ActorMapper.xml");

		ConnectionFactory.getSqlSessionFactory(configuration);
	}
}
