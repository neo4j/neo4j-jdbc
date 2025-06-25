/*
 * Copyright (c) 2023-2025 "Neo4j,"
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
package org.neo4j.jdbc.authn.spi;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Contract for factories producing authentication suppliers. Factories can be implemented
 * by anyone interested to plug-in custom authentication logic into the JDBC driver that
 * will be configured via standard {@link java.util.Properties JDBC properties} and–if
 * available–also with the initial username and password combination. Any implementation
 * of this interface must also provide a public default constructor so that it can be
 * properly loaded via Javas {@link java.util.ServiceLoader}. Make sure you define a
 * provider configuration file
 *
 * <blockquote>{@code META-INF/services/org.neo4j.jdbc.authn.api.AuthenticationSupplierFactory}</blockquote>
 *
 * containing the fully qualified classname of your implementation. In case you offer
 * several factories, each name goes into a separate line. When you want to make sure that
 * your library runs well on the module-path too, add a {@code module-info.java} such as
 * the following too:
 *
 * <pre>{@code
 * module your.library {
 *   provides org.neo4j.jdbc.authn.api.AuthenticationSupplierFactory with YourImplementation;
 * }
 * }</pre>
 *
 * The names of the factories loaded into the JDBC driver must be unique over the runtime
 * of the given instance. Factories will be referenced by their name configured through
 * the JDBC property {@code authn.supplier}.
 *
 * @author Michael J. Simons
 * @since 6.6.0
 */
public interface AuthenticationSupplierFactory {

	/**
	 * {@return the name of this authentication supplier factory}
	 */
	String getName();

	/**
	 * Creates a new authentication supplier to be used with the driver. While the Neo4j
	 * JDBC driver assumes that the factory will always provide a fresh one, it is ok to
	 * return always the same instance if that makes sense in a given scenario.
	 * @param user the username passed to the initial connect call to the driver, maybe
	 * {@literal null}
	 * @param password the users password passed to the initial connect call to the
	 * driver, maybe {@literal null}
	 * @param properties all {@link String} properties from the standard
	 * {@link java.util.Properties JDBC properties} starting with
	 * {@literal authn.theNameOfThisFactory} (case-insensitive); the prefix will be
	 * stripped, meaning {@code authn.myfactory.username} becomes {@code username} for
	 * easier and better readable access
	 * @return a new {@link Supplier authentication supplier}
	 */
	Supplier<Authentication> create(String user, String password, Map<String, ?> properties);

}
