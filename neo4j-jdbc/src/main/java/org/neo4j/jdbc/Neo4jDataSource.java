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
 * Created on 03/02/16
 */
package org.neo4j.jdbc;

import org.neo4j.jdbc.utils.ExceptionBuilder;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * @author AgileLARUS
 * @since 3.2.0
 */
public abstract class Neo4jDataSource implements javax.sql.DataSource {

	public static final String NEO4J_JDBC_PREFIX = "jdbc:neo4j:";
	protected           String serverName        = "localhost";
	protected String user;
	protected String password;
	protected int     portNumber = 0;
	protected boolean isSsl      = false;

	/*
	 * Ensure the driver is loaded as JDBC Driver might be invisible to Java's ServiceLoader.
	 * Usually, {@code Class.forName(...)} is not required as {@link DriverManager} detects JDBC drivers
	 * via {@code META-INF/services/java.sql.Driver} entries. However there might be cases when the driver
	 * is located at the application level classloader, thus it might be required to perform manual
	 * registration of the driver.
	 */
	static {
		try {
			Class.forName("org.neo4j.jdbc.Neo4jDriver");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(
					"Neo4jDataSource is unable to load org.neo4j.jdbc.Neo4jDriver. Please check if you have proper Neo4j JDBC Driver jar on the classpath", e);
		}
	}

	@Override public PrintWriter getLogWriter() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setLogWriter(PrintWriter out) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public void setLoginTimeout(int seconds) throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public int getLoginTimeout() throws SQLException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw ExceptionBuilder.buildUnsupportedOperationException();
	}

	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return Wrapper.unwrap(iface, this);
	}

	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return Wrapper.isWrapperFor(iface, this.getClass());
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getUrl(String protocol) {
		String url = NEO4J_JDBC_PREFIX + protocol + "://" + getServerName() + ((getPortNumber() > 0) ? ":" + getPortNumber() : "") + "?" + ((!getIsSsl()) ?
				"nossl," :
				"");
		if (Objects.nonNull(getUser())) {
			url += "user=" + getUser();
			if (Objects.nonNull(getPassword())) {
				url += ",password=" + getPassword();
			}
		}
		return url;
	}

	public boolean getIsSsl() {
		return this.isSsl;
	}

	public void setIsSsl(boolean ssl) {
		this.isSsl = ssl;
	}
}
