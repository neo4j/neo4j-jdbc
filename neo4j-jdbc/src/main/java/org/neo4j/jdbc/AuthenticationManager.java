/*
 * Copyright (c) 2023-2026 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.jdbc;

import org.neo4j.jdbc.authn.spi.Authentication;
import org.neo4j.jdbc.events.ConnectionListener;

/**
 * Internal auth manager.
 *
 * @author Michael J. Simons
 */
interface AuthenticationManager {

	/**
	 * The method figures out if there's currently an active {@link Authentication}
	 * spawned from this manager and if so, determines freely if it is still valid. In
	 * case of being valid, it will be returned, otherwise any underlying authentication
	 * supplier shall be asked for a fresh authentication, which will then be stored with
	 * the manager and returned.
	 * @return the current or a refreshed authentication
	 */
	Authentication getOrRefresh();

	/**
	 * The listener will only be notified on authentication events.
	 * @param connectionListener a listener that will only be notified on authentication
	 * events
	 */
	void addListener(ConnectionListener connectionListener);

}
