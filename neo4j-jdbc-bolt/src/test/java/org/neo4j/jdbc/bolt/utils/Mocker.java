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
 * Created on 24/03/16
 */
package org.neo4j.jdbc.bolt.utils;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.DatabaseNameUtil;
import org.neo4j.driver.internal.DefaultBookmarkHolder;
import org.neo4j.driver.internal.InternalSession;
import org.neo4j.driver.internal.async.NetworkSession;
import org.neo4j.driver.internal.handlers.pulln.FetchSizeUtil;
import org.neo4j.driver.internal.logging.DevNullLogging;
import org.neo4j.driver.internal.spi.ConnectionProvider;
import org.neo4j.jdbc.bolt.impl.BoltNeo4jConnectionImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * This class is a factory to create all the mocks that are used in multiple tests
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class Mocker {

	public static Driver mockDriverOpen() {
        Session session = mockSessionOpen();
		Driver driver = mock(Driver.class);
		when(driver.session(any(SessionConfig.class))).thenReturn(session);
		return driver;
	}

	public static Driver mockDriverClosed() {
        Session session = mockSessionClosed();
        Driver driver = mock(Driver.class);
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
		return driver;
	}

	public static Driver mockDriverOpenSlow() {
        Session session = mockSessionOpenSlow();
		Driver driver = mock(Driver.class);
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
		return driver;
	}

	public static Driver mockDriverException() {
        Session session = mockSessionException();
        Driver driver = mock(Driver.class);
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
		return driver;
	}

	public static Session mockSessionOpen() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		return session;
	}

	public static Session mockSessionClosed() {
        return mock(Session.class);
	}

	public static Session mockSessionOpenSlow() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		when(session.run(anyString())).thenAnswer(new Answer<ResultSet>() {
			@Override public ResultSet answer(InvocationOnMock invocation) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
				}
				return null;
			}
		});
		return session;
	}

	public static Session mockSessionException() {
		Session session = mock(Session.class);
		when(session.isOpen()).thenReturn(true);
		Transaction transaction = mock(Transaction.class);
		when(session.beginTransaction()).thenReturn(transaction);
		when(session.run(anyString())).thenThrow(new RuntimeException("RuntimeException THROWN"));
		return session;
	}

	public static BoltNeo4jConnectionImpl mockConnectionOpen() throws SQLException {
		BoltNeo4jConnectionImpl mockConnection = mock(BoltNeo4jConnectionImpl.class);
		when(mockConnection.isClosed()).thenReturn(false);
		return mockConnection;
	}

	public static BoltNeo4jConnectionImpl mockConnectionClosed() throws SQLException {
		BoltNeo4jConnectionImpl mockConnection = mock(BoltNeo4jConnectionImpl.class);
		when(mockConnection.isClosed()).thenReturn(true);
		return mockConnection;
	}

	public static BoltNeo4jConnectionImpl mockConnectionOpenWithTransactionThatReturns(Result cur) throws SQLException {
		Transaction mockTransaction = mock(Transaction.class);
		when(mockTransaction.run(anyString())).thenReturn(cur);
		when(mockTransaction.run(anyString(), any(Map.class))).thenReturn(cur);

		BoltNeo4jConnectionImpl mockConnection = mockConnectionOpen();
		when(mockConnection.getTransaction()).thenReturn(mockTransaction);
		return mockConnection;
	}

	public static Driver mockDriver() {
		Driver mockedDriver = mock(org.neo4j.driver.Driver.class);
		ConnectionProvider connectionProvider = mock(ConnectionProvider.class, RETURNS_MOCKS);
		NetworkSession networkSession = new NetworkSession(connectionProvider, null,
				DatabaseNameUtil.database(""), AccessMode.READ, new DefaultBookmarkHolder(), FetchSizeUtil.UNLIMITED_FETCH_SIZE, DevNullLogging.DEV_NULL_LOGGING);
		Mockito.when(mockedDriver.session()).thenReturn(new InternalSession(networkSession));
		return mockedDriver;
	}
}
