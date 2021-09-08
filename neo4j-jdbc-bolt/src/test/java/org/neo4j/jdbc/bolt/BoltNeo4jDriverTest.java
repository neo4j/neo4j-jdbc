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
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.async.pool.PoolSettings;
import org.neo4j.jdbc.Neo4jDriver;
import org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils;
import org.neo4j.jdbc.bolt.utils.Mocker;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.neo4j.driver.Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES;
import static org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils.PASSWORD;
import static org.neo4j.jdbc.bolt.utils.JdbcConnectionTestUtils.USERNAME;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */

public class BoltNeo4jDriverTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	private static final String COMPLETE_VALID_URL = "jdbc:neo4j:bolt://test";
	private static final String BOLT_URL = "bolt://test";
	private static org.neo4j.driver.Driver mockedDriver;

	@BeforeClass public static void initialize() {
		mockedDriver = Mocker.mockDriver();
	}

	/*------------------------------*/
	/*           connect            */
	/*------------------------------*/
	//WARNING!! NOT COMPLETE TEST!! Needs tests for parameters

	@Test public void shouldConnectCreateConnection() throws SQLException {
		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		Connection connection = driver.connect(COMPLETE_VALID_URL, null);
		assertNotNull(connection);
	}

	@Test public void shouldConnectCreateConnectionWithNoAuthTokenWithPropertiesObjectWithoutUserAndPassword() throws SQLException, URISyntaxException {
		Properties properties = new Properties();
		properties.put("test", "TEST_VALUE");

		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		Connection connection = driver.connect(COMPLETE_VALID_URL, properties);
		assertNotNull(connection);
	}

	@Test public void shouldConnectReturnNullIfUrlNotValid() throws SQLException {
		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		assertNull(driver.connect("jdbc:neo4j:http://localhost:7474", null));
		assertNull(driver.connect("bolt://localhost:7474", null));
		assertNull(driver.connect("jdbcbolt://localhost:7474", null));
		assertNull(driver.connect("jdbc:mysql://localhost:3306/sakila", null));
	}

	@Test public void shouldConnectThrowExceptionOnNullURL() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("null is not a valid url");

		Neo4jDriver driver = new BoltDriver((routingUris, config, authToken, info) -> mockedDriver);
		driver.connect(null, null);
	}

	@Test public void shouldConnectThrowExceptionOnConnectionFailed() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Unable to connect to somehost:9999");

		Neo4jDriver driver = new BoltDriver();
		driver.connect("jdbc:neo4j:bolt://somehost:9999", null);
	}

	@Test public void shouldAcceptTrustStrategyParamsSystemCertificates() throws SQLException, URISyntaxException {
		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_SYSTEM_CA_SIGNED_CERTIFICATES");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, info) -> {
			assertEquals(TRUST_SYSTEM_CA_SIGNED_CERTIFICATES, config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, properties);

		assertTrue(called.get());
	}

	@Test public void shouldNotAcceptTrustStrategyParamsCustomCertificateWithoutFile() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Missing parameter 'trusted.certificate.file'");

		Properties properties = new Properties();
		properties.put("trust.strategy", "TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");
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
	@Test public void shouldNotRequireConfiguration() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(PoolSettings.DEFAULT_CONNECTION_ACQUISITION_TIMEOUT, config.connectionAcquisitionTimeoutMillis());
			assertEquals(PoolSettings.DEFAULT_IDLE_TIME_BEFORE_CONNECTION_TEST, config.idleTimeBeforeConnectionTest());
			assertEquals((int) TimeUnit.SECONDS.toMillis( 30 ), config.connectionTimeoutMillis());
			assertFalse(config.encrypted());
			assertFalse(config.logLeakedSessions());
			assertEquals(PoolSettings.DEFAULT_MAX_CONNECTION_LIFETIME, config.maxConnectionLifetimeMillis());
			assertEquals(PoolSettings.DEFAULT_MAX_CONNECTION_POOL_SIZE, config.maxConnectionPoolSize());
			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, new Properties());

		assertTrue(called.get());
	}

	@Test public void shouldOverrideConfiguration_byUrl_empty_properties() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(10, config.connectionAcquisitionTimeoutMillis());
			assertEquals(600000, config.idleTimeBeforeConnectionTest());
			assertEquals(123_000, config.connectionTimeoutMillis());
			assertTrue(config.encrypted());
			assertTrue(config.logLeakedSessions());
			assertEquals(123_000, config.maxConnectionLifetimeMillis());
			assertEquals(3, config.maxConnectionPoolSize());
			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10" +
				"&connection.liveness.check.timeout=10" +
				"&connection.timeout=123000" +
				"&encryption=true" +
				"&leaked.sessions.logging=true" +
				"&load.balancing.strategy=ROUND_ROBIN" +
				"&max.connection.lifetime=123000" +
				"&max.connection.poolsize=3" +
				"&trust.strategy=TRUST_SYSTEM_CA_SIGNED_CERTIFICATES" +
				"&max.transaction.retry.time=1000", new Properties());

		assertTrue(called.get());
	}

	@Test public void shouldOverrideConfiguration_byUrl_no_properties() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(10, config.connectionAcquisitionTimeoutMillis());
			assertEquals(600000, config.idleTimeBeforeConnectionTest());
			assertEquals(123_000, config.connectionTimeoutMillis());
			assertTrue(config.encrypted());
			assertTrue(config.logLeakedSessions());
			assertEquals(123_000, config.maxConnectionLifetimeMillis());
			assertEquals(3, config.maxConnectionPoolSize());
			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10" +
				"&connection.liveness.check.timeout=10" +
				"&connection.timeout=123000" +
				"&encryption=true" +
				"&leaked.sessions.logging=true" +
				"&load.balancing.strategy=ROUND_ROBIN" +
				"&max.connection.lifetime=123000" +
				"&max.connection.poolsize=3" +
				"&trust.strategy=TRUST_SYSTEM_CA_SIGNED_CERTIFICATES" +
				"&max.transaction.retry.time=1000", null);

		assertTrue(called.get());
	}

	@Test public void shouldOverrideConfiguration_byUrl_mix_properties() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Properties prop = new Properties();
		prop.setProperty("connection.acquisition.timeout","20");
		prop.setProperty("connection.liveness.check.timeout","20");
		prop.setProperty("connection.timeout","321000");

		Driver driver = new BoltDriver((routingUris, config, authToken, info) -> {
			assertEquals(10, config.connectionAcquisitionTimeoutMillis());
			assertEquals(600000, config.idleTimeBeforeConnectionTest());
			assertEquals(123_000, config.connectionTimeoutMillis());
			assertTrue(config.encrypted());
			assertTrue(config.logLeakedSessions());
			assertEquals(123_000, config.maxConnectionLifetimeMillis());
			assertEquals(3, config.maxConnectionPoolSize());
			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10" +
				"&connection.liveness.check.timeout=10" +
				"&connection.timeout=123000" +
				"&encryption=true" +
				"&leaked.sessions.logging=true" +
				"&load.balancing.strategy=ROUND_ROBIN" +
				"&max.connection.lifetime=123000" +
				"&max.connection.poolsize=3" +
				"&trust.strategy=TRUST_SYSTEM_CA_SIGNED_CERTIFICATES" +
				"&max.transaction.retry.time=1000", prop);

		assertTrue(called.get());
	}

	@Test public void shouldOverrideAuthentication_all_default() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(AuthTokens.none(), authToken);
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10", new Properties());

		assertTrue(called.get());
	}

	@Test public void shouldOverrideAuthentication_prop() throws SQLException {
		AtomicBoolean called = new AtomicBoolean(false);
		Properties info = JdbcConnectionTestUtils.defaultInfo();
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(AuthTokens.basic(USERNAME, PASSWORD), authToken);
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10", info);

		assertTrue(called.get());
	}

	@Test public void shouldOverrideAuthentication_url() throws SQLException, URISyntaxException {
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(AuthTokens.basic(USERNAME, PASSWORD), authToken);
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(
				String.format("%s?connection.acquisition.timeout=10&user=%s&password=%s", COMPLETE_VALID_URL, USERNAME, JdbcConnectionTestUtils.PASSWORD),
				new Properties()
		);

		assertTrue(called.get());
	}

	@Test public void shouldOverrideAuthentication_prop_url() throws SQLException, URISyntaxException {
		Properties prop = new Properties();
		prop.setProperty("user","neo4j");
		prop.setProperty("password","admin");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(AuthTokens.basic(USERNAME, PASSWORD), authToken);
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL+"?connection.acquisition.timeout=10&user="+ USERNAME+"&password="+JdbcConnectionTestUtils.PASSWORD, prop);

		assertTrue(called.get());
	}

	@Test public void shouldConfigureConnectionAcquisitionTimeout() throws SQLException {
		Properties info = new Properties();
		info.setProperty("connection.acquisition.timeout","10");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(10, config.connectionAcquisitionTimeoutMillis());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseConnectionAcquisitionTimeout() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.acquisition.timeout: XX is not a number");
		Properties info = new Properties();
		info.setProperty("connection.acquisition.timeout", "XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);

		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureIdleTimeBeforeConnectionTest() throws SQLException, URISyntaxException {
		AtomicBoolean called = new AtomicBoolean(false);
		Properties info = new Properties();
		info.setProperty("connection.liveness.check.timeout","10");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(600000, config.idleTimeBeforeConnectionTest());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);
		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseIdleTimeBeforeConnectionTest() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.liveness.check.timeout: XX is not a number");

		Properties info = new Properties();
		info.setProperty("connection.liveness.check.timeout","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureConnectionTimeout() throws SQLException, URISyntaxException {
		Properties info = new Properties();
		info.setProperty("connection.timeout","123000");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(123_000, config.connectionTimeoutMillis());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseConnectionTimeout() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("connection.timeout: XX is not a number");

		Properties info = new Properties();
		info.setProperty("connection.timeout","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureEncryption() throws SQLException {
		Properties info = new Properties();
		info.setProperty("encryption","true");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertTrue(config.encrypted());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseEncryption() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("encryption: XX is not a boolean");

		Properties info = new Properties();
		info.setProperty("encryption","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);
		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureLeakedSessionsLogging() throws SQLException, URISyntaxException {
		Properties info = new Properties();
		info.setProperty("leaked.sessions.logging","true");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertTrue(config.logLeakedSessions());;
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseLeakedSessionsLogging() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("leaked.sessions.logging: XX is not a boolean");
		Properties info = new Properties();
		info.setProperty("leaked.sessions.logging","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);

		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureMaxConnectionLifetime() throws SQLException, URISyntaxException {
		Properties info = new Properties();
		info.setProperty("max.connection.lifetime","123000");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(123_000, config.maxConnectionLifetimeMillis());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseMaxConnectionLifetime() throws SQLException, URISyntaxException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("max.connection.lifetime: XX is not a number");
		Properties info = new Properties();
		info.setProperty("max.connection.lifetime","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);

		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigureMaxConnectionPoolSize() throws SQLException, URISyntaxException {
		Properties info = new Properties();
		info.setProperty("max.connection.poolsize","3");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(3, config.maxConnectionPoolSize());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseMaxConnectionPoolSize() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("max.connection.poolsize: XX is not a number");
		Properties info = new Properties();
		info.setProperty("max.connection.poolsize","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);

		driver.connect(COMPLETE_VALID_URL, info);
	}

	@Test public void shouldConfigure_TRUST_SYSTEM_CA_SIGNED_CERTIFICATES() throws SQLException {
		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_SYSTEM_CA_SIGNED_CERTIFICATES");

		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertEquals(Config.TrustStrategy.trustSystemCertificates().strategy(), config.trustStrategy().strategy());
			assertTrue(called.compareAndSet(false, true));
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test public void shouldConfigure_TRUST_CUSTOM_CA_SIGNED_CERTIFICATES() throws SQLException, URISyntaxException {
		Properties info = new Properties();
		info.setProperty("trust.strategy","TRUST_CUSTOM_CA_SIGNED_CERTIFICATES");
		info.setProperty("trusted.certificate.file","empty.txt");
		AtomicBoolean called = new AtomicBoolean(false);
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> {
			assertTrue(called.compareAndSet(false, true));
			assertEquals(Config.TrustStrategy.trustCustomCertificateSignedBy(new File("empty.txt")).strategy(), config.trustStrategy().strategy());
			return mockedDriver;
		});

		driver.connect(COMPLETE_VALID_URL, info);

		assertTrue(called.get());
	}

	@Test
	public void shouldRefuseTrustStrategy() throws SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("Invalid value for trust.strategy param.");

		Properties info = new Properties();
		info.setProperty("trust.strategy","XX");
		Driver driver = new BoltDriver((routingUris, config, authToken, props) -> mockedDriver);
		driver.connect(COMPLETE_VALID_URL, info);
	}
}
