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
import org.neo4j.driver.internal.async.pool.PoolSettings;
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
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
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

	@Test public void shouldAcceptTrustStrategyParamsCustomCertificateWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	@Test public void shouldAcceptTrustStrategyParamsTrustFirstUseWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		PowerMockito.mockStatic(Config.TrustStrategy.class);

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_ON_FIRST_USE");

		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
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

	/*------------------------------*/
	/*          Config              */
	/*------------------------------*/
	@Test public void shouldNotRequireConfiguration() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(PoolSettings.DEFAULT_CONNECTION_ACQUISITION_TIMEOUT, config.connectionAcquisitionTimeoutMillis());
			assertEquals(PoolSettings.DEFAULT_IDLE_TIME_BEFORE_CONNECTION_TEST, config.idleTimeBeforeConnectionTest());
			assertEquals((int) TimeUnit.SECONDS.toMillis( 5 ), config.connectionTimeoutMillis());
			assertEquals(true, config.encrypted());
			assertEquals(false, config.logLeakedSessions());
			assertEquals(Config.LoadBalancingStrategy.LEAST_CONNECTED, config.loadBalancingStrategy());
			assertEquals(PoolSettings.DEFAULT_MAX_CONNECTION_LIFETIME, config.maxConnectionLifetimeMillis());
			assertEquals(PoolSettings.DEFAULT_MAX_CONNECTION_POOL_SIZE, config.maxConnectionPoolSize());
			assertEquals(Config.TrustStrategy.trustAllCertificates().strategy(), config.trustStrategy().strategy());

			return mockedDriver;
		});

		Properties properties = new Properties();
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, properties);
	}

	@Test public void shouldConfigureConnectionAcquisitionTimeout() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(10, config.connectionAcquisitionTimeoutMillis());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.acquisition.timeout","10");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseConnectionAcquisitionTimeout() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.acquisition.timeout: XX is not a number");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.acquisition.timeout", "XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureIdleTimeBeforeConnectionTest() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(600000, config.idleTimeBeforeConnectionTest());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.liveness.check.timeout","10");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseIdleTimeBeforeConnectionTest() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.liveness.check.timeout: XX is not a number");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.liveness.check.timeout","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureConnectionTimeout() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(123_000, config.connectionTimeoutMillis());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.timeout","123000");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseConnectionTimeout() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.timeout: XX is not a number");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("connection.timeout","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureEncryption() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(true, config.encrypted());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("encryption","true");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseEncryption() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("encryption: XX is not a boolean");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("encryption","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureLeakedSessionsLogging() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(true, config.logLeakedSessions());;

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("leaked.sessions.logging","true");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseLeakedSessionsLogging() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("leaked.sessions.logging: XX is not a boolean");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("leaked.sessions.logging","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureLoadBalancingStrategy_LEAST_CONNECTED() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(Config.LoadBalancingStrategy.LEAST_CONNECTED, config.loadBalancingStrategy());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("load.balancing.strategy","LEAST_CONNECTED");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureLoadBalancingStrategy_ROUND_ROBIN() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			assertEquals(Config.LoadBalancingStrategy.ROUND_ROBIN, config.loadBalancingStrategy());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("load.balancing.strategy","ROUND_ROBIN");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseLoadBalancingStrategy() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("load.balancing.strategy: XX is not a load balancing strategy");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("load.balancing.strategy","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureMaxConnectionLifetime() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(123_000, config.maxConnectionLifetimeMillis());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("max.connection.lifetime","123000");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseMaxConnectionLifetime() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("max.connection.lifetime: XX is not a number");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("max.connection.lifetime","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureMaxConnectionPoolSize() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(3, config.maxConnectionPoolSize());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("max.connection.poolsize","3");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseMaxConnectionPoolSize() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("max.connection.poolsize: XX is not a number");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("max.connection.poolsize","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigure_TRUST_SYSTEM_CA_SIGNED_CERTIFICATES() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_SYSTEM_CA_SIGNED_CERTIFICATES");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigure_TRUST_CUSTOM_CA_SIGNED_CERTIFICATES() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(Config.TrustStrategy.trustCustomCertificateSignedBy(new File("empty.txt")).strategy(), config.trustStrategy().strategy());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");
		info.setProperty("trusted.certificate.file","empty.txt");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigure_TRUST_ON_FIRST_USE() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			assertEquals(Config.TrustStrategy.trustOnFirstUse(new File("empty.txt")).strategy(), config.trustStrategy().strategy());

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_ON_FIRST_USE");
		info.setProperty("trusted.certificate.file","empty.txt");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseTrustStrategy() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid value for trust.strategy param.");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("trust.strategy","XX");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldRefuseTrustStrategyCertificate() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Missing parameter 'trusted.certificate.file' : A FILE IS REQUIRED");
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);
			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test
	public void shouldConfigureMaxTransactionRetryTime() throws SQLException, URISyntaxException {
		PowerMockito.mockStatic(GraphDatabase.class);

		Mockito.when(GraphDatabase.driver(Mockito.eq(new URI(BOLT_URL)), Mockito.eq(AuthTokens.none()), any(Config.class))).thenAnswer(invocationOnMock -> {
			Config config = invocationOnMock.getArgumentAt(2, Config.class);

			// retrySettings is not visible...

			return mockedDriver;
		});

		Properties info = new Properties();
		info.setProperty("max.transaction.retry.time","1000");//milliseconds
		Driver driver = new BoltDriver();
		driver.connect(COMPLETE_VALID_URL, info);
	}

}
