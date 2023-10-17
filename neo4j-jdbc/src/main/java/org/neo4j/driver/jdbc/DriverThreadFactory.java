/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc;

import io.netty.util.concurrent.DefaultThreadFactory;

class DriverThreadFactory extends DefaultThreadFactory {

	private static final String THREAD_NAME_PREFIX = "Neo4jDriverIO";

	private static final int THREAD_PRIORITY = Thread.MAX_PRIORITY;

	private static final boolean THREAD_IS_DAEMON = true;

	DriverThreadFactory() {
		super(THREAD_NAME_PREFIX, THREAD_IS_DAEMON, THREAD_PRIORITY);
	}

	@Override
	protected Thread newThread(Runnable r, String name) {
		return new DriverThread(threadGroup, r, name);
	}

}
