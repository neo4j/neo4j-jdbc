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

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.bolt.utils.Mocker;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
@RunWith(PowerMockRunner.class) @PrepareForTest({GraphDatabase.class, Config.TrustStrategy.class}) public class BoltNeo4jDriverTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private static final String COMPLETE_VALID_URL = "jdbc:neo4j:bolt://test";
	private static final String BOLT_URL = "bolt://test";
	private static org.neo4j.driver.v1.Driver mockedDriver;

	@BeforeClass public static void initialize() {
		mockedDriver = Mocker.mockDriver();
	}

	/*------------------------------*/
	/*           connect            */
	/*------------------------------*/
	//WARNING!! NOT COMPLETE TEST!! Needs tests for parameters

	@Test public void shouldConnectCreateConnection() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Neo4jDriver driver = new BoltDriver();
		Connection connection = driver.connect(COMPLETE_VALID_URL, null);
		assertNotNull(connection);
	}

	@Test public void shouldConnectCreateConnectionWithNoAuthTokenWithPropertiesObjectWithoutUserAndPassword() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Properties properties = new Properties();
		properties.put("test", "TEST_VALUE");

		Neo4jDriver driver = new BoltDriver();
		Connection connection = driver.connect(COMPLETE_VALID_URL, properties);
		assertNotNull(connection);
	}

	@Test public void shouldConnectReturnNullIfUrlNotValid() throws SQLException {
		Neo4jDriver driver = new BoltDriver();
		assertNull(driver.connect("jdbc:neo4j:http://localhost:7474", null));
		assertNull(driver.connect("bolt://localhost:7474", null));
		assertNull(driver.connect("jdbcbolt://localhost:7474", null));
		assertNull(driver.connect("jdbc:mysql://localhost:3306/sakila", null));
	}

	@Test public void shouldConnectThrowExceptionOnNullURL() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltDriver();
		driver.connect(null, null);
	}

	@Test public void shouldConnectThrowExceptionOnConnectionFailed() throws SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltDriver();
		driver.connect("jdbc:neo4j:bolt://somehost:9999", null);
	}

	@Test public void shouldAcceptTrustStrategyParamsSystemCertificates() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_SYSTEM_CA_SIGNED_CERTIFICATES");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);

		verifyStatic(Config.TrustStrategy.class, atLeastOnce());
		Config.TrustStrategy.trustSystemCertificates();
	}

	@Test public void shouldAcceptTrustStrategyParamsCustomCertificateWithFileOk() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");
		File file = mock(File.class);
		properties.put("trusted.certificate.file", file);

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);

		verifyStatic(Config.TrustStrategy.class, atLeastOnce());
		Config.TrustStrategy.trustCustomCertificateSignedBy(file);
	}

	@Test public void shouldAcceptTrustStrategyParamsCustomCertificateWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	@Test public void shouldAcceptTrustStrategyParamsTrustFirstUseFileOk() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_ON_FIRST_USE");
		File file = mock(File.class);
		properties.put("trusted.certificate.file", file);

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);

		verifyStatic(Config.TrustStrategy.class, atLeastOnce());
		Config.TrustStrategy.trustOnFirstUse(file);
	}

	@Test public void shouldAcceptTrustStrategyParamsTrustFirstUseWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_ON_FIRST_USE");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	@Test public void shouldAcceptTrustStrategyParamsTrustSignedFileOk() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);
		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenReturn(mockedDriver);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_SIGNED_CERTIFICATES");
		File file = mock(File.class);
		properties.put("trusted.certificate.file", file);

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);

		verifyStatic(Config.TrustStrategy.class, atLeastOnce());
		Config.TrustStrategy.trustSignedBy(file);
	}

	@Test public void shouldAcceptTrustStrategyParamsTrustSignedWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_SIGNED_CERTIFICATES");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	@Test public void shouldThrowExceptionIfWrongValueTrustStrategyParam() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid value for trust.strategy param");

		Properties properties = new Properties();
		properties.put("trust.strategy", "INVALID_VALUE");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	/*------------------------------*/
	/*          acceptsURL          */
	/*------------------------------*/
	@Test public void shouldAcceptURLOK() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltDriver();
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://localhost:7474"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://192.168.0.1:7474"));
		assertTrue(driver.acceptsURL("jdbc:neo4j:bolt://localhost:8080,localhost:8081"));
	}

	@Test public void shouldAcceptURLKO() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Neo4jDriver driver = new BoltDriver();
		assertFalse(driver.acceptsURL("jdbc:neo4j:http://localhost:7474"));
		assertFalse(driver.acceptsURL("jdbc:file://192.168.0.1:7474"));
		assertFalse(driver.acceptsURL("bolt://localhost:7474"));
	}

	@Test public void shouldThrowException() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		expectedEx.expect(SQLException.class);

		Neo4jDriver driver = new BoltDriver();
		assertFalse(driver.acceptsURL(null));
	}
}
