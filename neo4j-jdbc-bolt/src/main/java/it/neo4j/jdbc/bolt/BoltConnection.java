/**
 * Copyright (c) 2004-2016 LARUS Business Automation [http://www.larus-ba.it]
 * <p>
 * This file is part of the "LARUS Integration Framework for Neo4j".
 * <p>
 * The "LARUS Integration Framework for Neo4j" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * Created on 17/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.Connection;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltConnection extends Connection {

	private boolean closed   = false;
	private boolean readOnly = false;

	@Override public void close() throws SQLException {
		this.closed = true;
	}

	@Override public boolean isClosed() throws SQLException {
		return this.closed;
	}

	@Override public void setReadOnly(boolean readOnly) throws SQLException {
		if (!isClosed()) {
			this.readOnly = readOnly;
		} else {
			throw new SQLException("Connection already closed");
		}
	}

	@Override public boolean isReadOnly() throws SQLException {
		if (!isClosed()) {
			return this.readOnly;
		} else {
			throw new SQLException("Connection already closed");
		}
	}
}
