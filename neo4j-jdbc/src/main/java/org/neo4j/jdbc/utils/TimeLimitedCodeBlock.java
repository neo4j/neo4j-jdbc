/**
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
 * Created on 28/11/17
 */
package org.neo4j.jdbc.utils;

import java.util.concurrent.*;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class TimeLimitedCodeBlock {

	private TimeLimitedCodeBlock () {}

	/**
	 * This method is used to run a specific <code>Runnable</code> for at most a <code>timeout</code> period of time.
	 * If a <code>timeout</code> of 0 is set then no timeout will be applied.
	 *
	 * @param runnable The runnable to run
	 * @param timeout The maximum time a run should last
	 * @param timeUnit The <code>TimeUnit</code> unit for the timeout
	 * @throws Neo4jJdbcRuntimeException Any exception thrown by the runnable wrapped.
	 */
	public static void runWithTimeout(final Runnable runnable, long timeout, TimeUnit timeUnit) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future future = executor.submit(runnable);
		executor.shutdown();
		try {
			if (timeout == 0) {
				future.get();
			} else {
				future.get(timeout, timeUnit);
			}
		}
		catch (Exception e) {
			future.cancel(true);
			throw new Neo4jJdbcRuntimeException(e);
		}
	}

}
