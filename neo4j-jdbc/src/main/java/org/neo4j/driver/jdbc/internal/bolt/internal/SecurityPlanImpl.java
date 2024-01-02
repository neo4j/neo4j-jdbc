/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal;

import javax.net.ssl.SSLContext;

import org.neo4j.driver.jdbc.internal.bolt.RevocationCheckingStrategy;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlan;

public final class SecurityPlanImpl implements SecurityPlan {

	private final boolean requiresEncryption;

	private final SSLContext sslContext;

	private final boolean requiresHostnameVerification;

	private final RevocationCheckingStrategy revocationCheckingStrategy;

	public SecurityPlanImpl(boolean requiresEncryption, SSLContext sslContext, boolean requiresHostnameVerification,
			RevocationCheckingStrategy revocationCheckingStrategy) {
		this.requiresEncryption = requiresEncryption;
		this.sslContext = sslContext;
		this.requiresHostnameVerification = requiresHostnameVerification;
		this.revocationCheckingStrategy = revocationCheckingStrategy;
	}

	@Override
	public boolean requiresEncryption() {
		return this.requiresEncryption;
	}

	@Override
	public SSLContext sslContext() {
		return this.sslContext;
	}

	@Override
	public boolean requiresHostnameVerification() {
		return this.requiresHostnameVerification;
	}

	@Override
	public RevocationCheckingStrategy revocationCheckingStrategy() {
		return this.revocationCheckingStrategy;
	}

}
