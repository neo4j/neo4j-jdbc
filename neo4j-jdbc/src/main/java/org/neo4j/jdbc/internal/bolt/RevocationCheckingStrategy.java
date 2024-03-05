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
package org.neo4j.jdbc.internal.bolt;

/**
 * Defines strategy for revocation checks.
 *
 * @author Neo4j Drivers Team
 * @since 6.0.0
 */
public enum RevocationCheckingStrategy {

	/**
	 * Don't do any OCSP revocation checks, regardless whether there are stapled
	 * revocation statuses or not.
	 */
	NO_CHECKS,
	/**
	 * Verify OCSP revocation checks when the revocation status is stapled to the
	 * certificate, continue if not.
	 */
	VERIFY_IF_PRESENT,
	/**
	 * Require stapled revocation status and verify OCSP revocation checks, fail if no
	 * revocation status is stapled to the certificate.
	 */
	STRICT;

	/**
	 * Returns whether a given strategy requires revocation checking.
	 * @param revocationCheckingStrategy the strategy
	 * @return whether revocation checking is required
	 */
	public static boolean requiresRevocationChecking(RevocationCheckingStrategy revocationCheckingStrategy) {
		return revocationCheckingStrategy.equals(STRICT) || revocationCheckingStrategy.equals(VERIFY_IF_PRESENT);
	}

}
