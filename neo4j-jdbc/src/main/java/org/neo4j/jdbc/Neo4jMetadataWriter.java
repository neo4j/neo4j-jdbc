package org.neo4j.jdbc;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public interface Neo4jMetadataWriter<T extends Neo4jMetadataWriter<T>> {

	/**
	 * Set the transaction metadata. Specified metadata will be attached to the executing transaction and visible in the output of
	 * {@code dbms.listQueries} and {@code dbms.listTransactions} procedures. It will also get logged to the {@code query.log}.
	 * <p>
	 * This functionality makes it easier to tag transactions and is equivalent to {@code dbms.setTXMetaData} procedure.
	 * <p>
	 * Provided value should not be {@code null}.
	 *
	 * @param metadata the metadata.
	 * @return this builder.
	 */
	T withMetadata(Map<String, Object> metadata);
	/*
	{
		requireNonNull(metadata, "Transaction metadata should not be null");
		metadata.values()
			.forEach(Extract::assertParameter); // Just assert valid parameters but don't create a value map yet
		this.metadata = new HashMap<>(metadata); // Create a defensive copy
		return this;
	}*/
}
