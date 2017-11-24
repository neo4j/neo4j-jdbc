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
package org.neo4j.jdbc.example.mybatis.util;

import java.io.IOException;
import java.io.Reader;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * @author AgileLARUS
 * @since 3.0.2
 */
public class ConnectionFactory {

	private static final String DATABASE_CONFIG_XML = "database-config.xml";

	private static SqlSessionFactory factory;

	private ConnectionFactory() {}

	public static SqlSessionFactory getSqlSessionFactory() {
		return getSqlSessionFactory(DATABASE_CONFIG_XML);
	}

	public static SqlSessionFactory getSqlSessionFactory(String config) {
		if (factory == null) {
			try (Reader reader = Resources.getResourceAsReader(config)){
				factory = new SqlSessionFactoryBuilder().build(reader);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return factory;
	}

	public static SqlSessionFactory getSqlSessionFactory(Configuration configuration) {
		if (factory == null) {
			factory = new SqlSessionFactoryBuilder().build(configuration);
		}
		return factory;
	}
}
