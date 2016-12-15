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
 * Created on 09/03/16
 */
package org.neo4j.jdbc;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
class Wrapper {

	@SuppressWarnings("unchecked")
	public static <T> T unwrap(Class<T> iface, Object obj) throws SQLException {
		if (!isWrapperFor(iface, obj.getClass())) {
			//Current class is not implementing the requested class
			throw new SQLException();
		}

		return (T) obj;
	}

	@SuppressWarnings("rawtypes")
	public static boolean isWrapperFor(Class<?> iface, Class cls) throws SQLException {
		if (cls.getName().equals(iface.getName())) {
			//iface is the cls class
			return true;
		}

		for (Class c : cls.getInterfaces()) {
			//Looking inside current class implementations
			if (c.getName().equals(iface.getName())) {
				//The cls class implements iface
				return true;
			} else if (isWrapperFor(iface, c)) {
				//Recursively search inside cls' interface
				return true;
			}
		}

		if (cls.getSuperclass() != null) {
			//Recursively search inside the cls' superclass
			return isWrapperFor(iface, cls.getSuperclass());
		} else {
			//iface is not directly or indirectly implemented in cls
			return false;
		}
	}
}
