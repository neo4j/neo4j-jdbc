/**
 * Copyright (c) 2004-2015 LARUS Business Automation Srl
 * <p>
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * <p>
 * Created on 03/02/16
 */
package it.neo4j.jdbc;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public abstract class Wrapper implements java.sql.Wrapper {
	/**
	 * Returns an object that implements the given interface to allow access to
	 * non-standard methods, or standard methods not exposed by the proxy.
	 * <p>
	 * If the receiver implements the interface then the result is the receiver
	 * or a proxy for the receiver. If the receiver is a wrapper
	 * and the wrapped object implements the interface then the result is the
	 * wrapped object or a proxy for the wrapped object. Otherwise return the
	 * the result of calling <code>unwrap</code> recursively on the wrapped object
	 * or a proxy for that result. If the receiver is not a
	 * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
	 *
	 * @param iface A Class defining an interface that the result must implement.
	 * @return an object that implements the interface. May be a proxy for the actual implementing object.
	 * @throws SQLException If no object found that implements the interface
	 * @since 1.6
	 */
	@Override public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	/**
	 * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
	 * for an object that does. Returns false otherwise. If this implements the interface then return true,
	 * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
	 * object. If this does not implement the interface and is not a wrapper, return false.
	 * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
	 * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
	 * returns true then calling <code>unwrap</code> with the same argument should succeed.
	 *
	 * @param iface a Class defining an interface.
	 * @return true if this implements the interface or directly or indirectly wraps an object that does.
	 * @throws SQLException if an error occurs while determining whether this is a wrapper
	 *                      for an object with the given interface.
	 * @since 1.6
	 */
	@Override public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}
}
