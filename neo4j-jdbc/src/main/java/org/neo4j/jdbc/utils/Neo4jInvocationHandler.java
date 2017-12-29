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
 * Created on 22/12/17
 */
package org.neo4j.jdbc.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Gianmarco Laggia @ Larus B.A.
 * @since 3.2.0
 */
public class Neo4jInvocationHandler implements InvocationHandler {

	private static final Logger LOGGER = Logger.getLogger(Neo4jInvocationHandler.class.getName());

	private final Map<String, Method> methods = new HashMap<>();

	private Object  target;
	private boolean debug;

	public Neo4jInvocationHandler(Object target, boolean debug) {
		this.target = target;
		this.debug = debug;

		for (Method method : target.getClass().getMethods()) {
			String key = getUniqueKeyFromMethod(method);
			this.methods.put(key, method);
		}
	}

	@Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			Object result = methods.get(getUniqueKeyFromMethod(method)).invoke(target, args);

			if (debug) {
				LOGGER.info("[" + target.getClass().getCanonicalName() + "] " + method.getName());
			}

			return result;
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private String getUniqueKeyFromMethod(Method method) {
		String key = method.getName() + "_";
		for(Class type : method.getParameterTypes()) {
			key += type.getCanonicalName() + "_";
		}
		return key;
	}
}
