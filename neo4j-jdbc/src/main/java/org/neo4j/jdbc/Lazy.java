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

import java.util.function.Supplier;

/**
 * Utility class for lazily and thread safe resolving a supplier of things.
 *
 * @param <T> the type of things to be resolved
 * @author Michael J. Simons
 * @since 6.0.0
 */
final class Lazy<T> {

	private final ThrowingSupplier<T> supplier;

	private volatile T resolved;

	static <T> Lazy<T> of(ThrowingSupplier<T> supplier) {
		return new Lazy<>(supplier);
	}

	private Lazy(ThrowingSupplier<T> supplier) {
		this.supplier = supplier;
	}

	/**
	 * Lazily resolves the value of the original {@link Supplier} and memorizes it.
	 * @return the resolved value
	 */
	T resolve() {

		T result = this.resolved;
		if (result == null) {
			synchronized (this) {
				result = this.resolved;
				if (result == null) {
					try {
						this.resolved = this.supplier.get();
					}
					catch (Exception ex) {
						if (ex instanceof RuntimeException rt) {
							throw rt;
						}
						throw new RuntimeException(ex);
					}
					result = this.resolved;
				}
			}
		}
		return result;
	}

	<E extends Throwable> T resolveThrowing(Class<E> type) throws E {
		try {
			return this.resolve();
		}
		catch (Exception ex) {
			if (type.isAssignableFrom(ex.getCause().getClass())) {
				throw type.cast(ex.getCause());
			}
			throw ex;
		}
	}

	/**
	 * This method is not synchronized and must be used in a {@code synchronized} block on
	 * this {@link Lazy}.
	 * @return true if this instance has been resolved
	 */
	boolean isResolved() {
		return this.resolved != null;
	}

	/**
	 * Forgets the resolved value. This method is not synchronized and must be used in a
	 * {@code synchronized} block on this {@link Lazy}.
	 */
	void forget() {
		this.resolved = null;
	}

}
