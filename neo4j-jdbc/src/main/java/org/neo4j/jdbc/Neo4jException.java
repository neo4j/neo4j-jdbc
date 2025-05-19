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
package org.neo4j.jdbc;

import java.io.Serial;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.neo4j.bolt.connection.exception.BoltGqlErrorException;

/**
 * Tooling to centralize creation of {@link SQLException SQL exceptions}, that aim to
 * match Neo4j GQL errors. The class itself is essentially a wrapper created to avoid the
 * construction of the exception inside the builder functions to avoid adding unnessary
 * stack frames.
 *
 * @author Michael J. Simons
 * @since 6.4.0
 */
final class Neo4jException extends SQLException implements GqlStatusObject {

	@Serial
	private static final long serialVersionUID = 4192957098450787651L;

	/**
	 * See
	 * <a href="https://neo4j.com/docs/status-codes/current/errors/gql-errors/">GQLStatus
	 * error codes</a>.
	 */
	@SuppressWarnings("squid:S115") // The enum values are purposefully named after the
									// error codes
	enum GQLError {

		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_08000">08000</a>.
		 */
		$08000(""),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22000">22000</a>.
		 */
		$22000(""),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22003">22003</a>.
		 */
		$22003("The numeric value %s is outside the required range"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22G03">22G03</a>.
		 */
		$22G03("Invalid value type"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N01">22N01</a>.
		 */
		$22N01("Invalid type, expected the value %s to be of type %s, but was of type %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N02">22N02</a>.
		 */
		$22N02("You specified a negative numeric value, expected %s to be a positive number but found %s instead"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N06">22N06</a>.
		 */
		$22N06("Required input missing, %s needs to be specified"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N11">22N11</a>.
		 */
		$22N11("Invalid argument, cannot process %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N37">22N37</a>.
		 */
		$22N37("Cannot coerce %s to %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_22N63">22N63</a>.
		 */
		$22N63("The property key %s does not exist"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_25N02">25N02</a>.
		 */
		$25N02("Unable to complete transaction, see debug log for details"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_25N05">25N05</a>.
		 */
		$25N05("Transaction has been closed"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_2DN01">2DN01</a>.
		 */
		$2DN01("Failed to commit transaction: %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_2DN03">2DN03</a>.
		 */
		$2DN03("Failed to terminate transaction: %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_40N01">40N01</a>.
		 */
		$40N01("Failed to rollback transaction: %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_42N51">42N51</a>.
		 */
		$42N51("Invalid parameter %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_50N00">50N00</a>.
		 */
		$50N00("An internal exception has been raised %s: %s"),
		/**
		 * See <a href=
		 * "https://neo4j.com/docs/status-codes/current/errors/gql-errors/#_50N42">50N42</a>.
		 */
		$50N42("An unexpected error has occurred, see debug log for details");

		private final String messageTemplate;

		private static final Map<String, String> PREFIXES = Map.of("22", "data exception", "25",
				"invalid transaction state", "2D", "invalid transaction termination", "40", "transaction rollback",
				"42", "syntax error or access rule violation", "50", "general processing exception");

		GQLError(String messageTemplate) {
			this.messageTemplate = messageTemplate;
		}

		ErrorAndCause causedBy(Throwable cause) {
			return new ErrorAndCause(this, cause);
		}

		Args withTemplatedMessage(Object... args) {
			return new ErrorAndCause(this, null).withTemplatedMessage(args);
		}

		Args withMessage(String msg) {
			return new ErrorAndCause(this, null).withMessage(msg);
		}

		private String getPrefix() {
			return PREFIXES.get(this.name().substring(1, 3));
		}

		/**
		 * Some errors from the DBMS are like catch-alls, with error messages that are not
		 * helpful to the user.
		 * @param ex the exception to check
		 * @return {@literal true} for error codes coming from the server that are kinda
		 * catch-all errors
		 */
		private static boolean isCatchAll(BoltGqlErrorException ex) {
			if (ex == null) {
				return false;
			}

			if (!($50N00.name().contains(ex.gqlStatus()) || $50N42.name().contains(ex.gqlStatus()))) {
				return false;
			}

			return !Objects.requireNonNullElse(ex.getMessage(), "").equals(ex.statusDescription());
		}

		static final class ErrorAndCause {

			private final GQLError error;

			private final Throwable cause;

			ErrorAndCause(GQLError error, Throwable cause) {
				this.error = error;
				this.cause = cause;
			}

			Args withTemplatedMessage(Object... args) {
				var template = this.error.getPrefix() + " - " + this.error.messageTemplate;
				return new Args(template.formatted(args), this.error.name().substring(1), this.cause);
			}

			Args withMessage(String msg) {
				var template = this.error.getPrefix() + " - %s";
				return new Args(template.formatted(msg), this.error.name().substring(1), this.cause);
			}

		}

	}

	/**
	 * Creates a new exception from a cause that potentially carries a Bolt error with GQL
	 * errors from the transactions or falls down to wrapping the cause into a SQL
	 * exception.
	 * @param cause the cause to check for "real" GQL errors
	 * @return the exception to be thrown
	 */
	static Args withCause(Throwable cause) {
		return withMessageAndCause(null, cause);
	}

	/**
	 * Creates a new exception from a cause that potentially carries a Bolt error with GQL
	 * errors from the transactions or falls down to wrapping the cause into a SQL
	 * exception.
	 * @param message can be null, only used when there's no underlying bolt issue
	 * @param cause the cause to check for "real" GQL errors
	 * @return the exception to be thrown
	 */
	static Args withMessageAndCause(String message, Throwable cause) {
		if (cause instanceof BoltGqlErrorException gqlErrorException && gqlErrorException.gqlStatus() != null) {
			var sqlState = gqlErrorException.gqlStatus();
			String reason;
			// Those are general processing exceptions with really not helpful status
			// message, so we add the message of the original cause
			if (GQLError.isCatchAll(gqlErrorException)) {
				reason = String.format("%s (%s)", gqlErrorException.statusDescription(), cause.getMessage());
			}
			else {
				reason = gqlErrorException.statusDescription();
			}

			return new Args(reason, sqlState, cause, GqlStatusObjectImpl.adaptDiagnosticRecord(gqlErrorException),
					gqlErrorException.gqlCause().map(GqlStatusObjectImpl::of).orElse(null));
		}

		return GQLError.$50N42.causedBy(cause).withMessage((message != null) ? message : cause.getMessage());
	}

	static Args withInternal(Exception ex) {
		return withInternal(ex, ex.getMessage());
	}

	static Args withInternal(Exception ex, String msg) {
		return GQLError.$50N00.causedBy(ex).withTemplatedMessage(ex.getClass().getName(), msg);
	}

	/**
	 * This exists to encapsulate the use of "raw" {@link SQLException SQL exceptions}.
	 * @param reason the reason for the new exception
	 * @return the exception to be thrown
	 */
	static Args withReason(String reason) {
		return GQLError.$50N42.withMessage(reason);
	}

	private final HashMap<String, String> diagnosticRecord;

	private final GqlStatusObject gqlCause;

	Neo4jException(Args args) {
		super(args.reason(), args.sqlState(), args.cause());

		this.diagnosticRecord = (args.diagnosticRecord() instanceof HashMap<String, String> hm) ? hm
				: new HashMap<>(args.diagnosticRecord());
		this.gqlCause = args.gqlCause();
	}

	@Override
	public String gqlStatus() {
		return super.getSQLState();
	}

	@Override
	public String statusDescription() {
		return super.getMessage();
	}

	@Override
	public Map<String, String> diagnosticRecord() {
		return Map.copyOf(this.diagnosticRecord);
	}

	@Override
	public Optional<GqlStatusObject> cause() {
		return Optional.ofNullable(this.gqlCause);
	}

	record Args(String reason, String sqlState, Throwable cause, Map<String, String> diagnosticRecord,
			GqlStatusObject gqlCause) {

		Args(String reason, String sqlState, Throwable cause) {
			this(reason, sqlState, cause, Map.of(), null);
		}
	}

}
