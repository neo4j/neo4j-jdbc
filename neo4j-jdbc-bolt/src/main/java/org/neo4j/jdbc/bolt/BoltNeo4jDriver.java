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
 * Created on 23/02/16
 */
package org.neo4j.jdbc.bolt;

import org.neo4j.driver.v1.*;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.InstanceFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.neo4j.driver.v1.Config.build;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltNeo4jDriver extends Neo4jDriver {

	public static final String JDBC_BOLT_PREFIX = "bolt";
	public static final String JDBC_BOLT_ROUTING_PREFIX = "bolt+routing";
	public static final String TRUST_STRATEGY_KEY = "trust.strategy";
	public static final String TRUSTED_CERTIFICATE_KEY = "trusted.certificate.file";

	static {
		try {
			BoltNeo4jDriver driver = new BoltNeo4jDriver();
			DriverManager.registerDriver(driver);
		} catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	/**
	 * Default constructor.
	 * @throws SQLException sqlexception
	 */
	public BoltNeo4jDriver() throws SQLException {
		super(JDBC_BOLT_PREFIX);
	}

	@Override public Connection connect(String url, Properties props) throws SQLException {
		if (url == null) {
			throw new SQLException("null is not a valid url");
		}
		Connection connection = null;
		if (acceptsURL(url)) {
			String boltUrl = url.replace(Neo4jDriver.JDBC_PREFIX, "").replaceAll("^(" + JDBC_BOLT_PREFIX + ":)([^/])", "$1//$2");
			try {
				Properties info = parseUrlProperties(boltUrl, props);
				boltUrl = removeUrlProperties(boltUrl, info);
				Config.ConfigBuilder builder = build();
				if (info.containsKey("nossl")) {
					builder = builder.withoutEncryption();
				}
				builder = setTrustStrategy(info, builder);
				Config config = builder.toConfig();
				AuthToken authToken = getAuthToken(info);
				Driver driver = GraphDatabase.driver(boltUrl, authToken, config);
				Session session = driver.session();
                BoltNeo4jConnection boltConnection = new BoltNeo4jConnection(session, info, url);
				connection = InstanceFactory.debug(BoltNeo4jConnection.class, boltConnection, BoltNeo4jConnection.hasDebug(info));
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
		return connection;
	}

	private AuthToken getAuthToken(Properties properties) {
		if (properties.isEmpty() || (!properties.containsKey("user") && !properties.containsKey("password"))) {
			return AuthTokens.none();
		}
		return AuthTokens.basic(properties.getProperty("user"), properties.getProperty("password"));
	}

	private String removeUrlProperties(String url, Properties properties) {
		String boltUrl = url;
		if (boltUrl.indexOf('?') != -1) {
			boltUrl = url.substring(0, url.indexOf('?'));
		}
		if (boltUrl.contains(JDBC_BOLT_ROUTING_PREFIX) && properties.contains("routingcontext")) {
			boltUrl += "?routingContext=" + properties.get("routingcontext");
		}
		return boltUrl;
	}

	private Config.ConfigBuilder setTrustStrategy(Properties properties, Config.ConfigBuilder builder) throws SQLException {
		Config.ConfigBuilder newBuilder = builder;
		if (properties.containsKey(TRUST_STRATEGY_KEY)) {
			Config.TrustStrategy.Strategy strategy;
			try {
				strategy = Config.TrustStrategy.Strategy.valueOf((String) properties.get(TRUST_STRATEGY_KEY));
			} catch (IllegalArgumentException e) {
				throw new SQLException("Invalid value for trust.strategy param.", e);
			}
			switch (strategy) {
				case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
					newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustSystemCertificates());
					break;
				case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
				case TRUST_ON_FIRST_USE:
				case TRUST_SIGNED_CERTIFICATES:
					newBuilder = handleTrustStrategyWithFile(properties, strategy, builder);
					break;
				case TRUST_ALL_CERTIFICATES:
				default:
					newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustAllCertificates());
					break;
			}
		}
		return newBuilder;
	}

	private Config.ConfigBuilder handleTrustStrategyWithFile(Properties properties, Config.TrustStrategy.Strategy strategy, Config.ConfigBuilder builder) throws SQLException {
		if (properties.containsKey(TRUSTED_CERTIFICATE_KEY)) {
			Object file = properties.get(TRUSTED_CERTIFICATE_KEY);
			if (file instanceof File) {
				Config.ConfigBuilder newBuilder = builder;
				switch (strategy) {
					case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
						newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustCustomCertificateSignedBy((File) file));
						break;
					case TRUST_ON_FIRST_USE:
						newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustOnFirstUse((File) file));
						break;
					case TRUST_SIGNED_CERTIFICATES:
						newBuilder = builder.withTrustStrategy(Config.TrustStrategy.trustSignedBy((File) file));
						break;
				}
				return newBuilder;
			} else {
				throw new SQLException("Invalid parameter 'trusted.certificate.file' : NOT A VALID FILE");
			}
		} else {
			throw new SQLException("Missing parameter 'trusted.certificate.file' : A FILE IS REQUIRED");
		}
	}
}
