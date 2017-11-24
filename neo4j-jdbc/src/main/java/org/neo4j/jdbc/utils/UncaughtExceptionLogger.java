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
 * Created on 02/08/16
 */
package org.neo4j.jdbc.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author AgileLARUS
 * @since 3.0.0
 * This class is used to intercept any exception coming from the test query used in the <code>isValid</code> method.
 */
public class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {
	private List<Throwable> exceptions = new ArrayList<>();

	@Override public void uncaughtException(Thread th, Throwable ex) {
		this.exceptions.add(ex);
	}

	/**
	 * This method returns the list of exceptions eventually thrown during the execution of the related thread.
	 *
	 * @return An <code>ArrayList&lt;Throwable&gt;</code> of exceptions
	 */
	public List<Throwable> getExceptions() {
		return this.exceptions;
	}
}
