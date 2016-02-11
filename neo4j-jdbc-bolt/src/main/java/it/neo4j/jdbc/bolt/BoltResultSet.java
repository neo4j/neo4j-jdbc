/**
 * Copyright (c) 2004-2015 LARUS Business Automation Srl
 * <p>
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * <p>
 * Created on 11/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.ResultSet;
import org.neo4j.driver.v1.ResultCursor;

import java.sql.SQLException;

/**
 * @author AgileLARUS
 *
 * @since 3.0.0
 */
public class BoltResultSet extends ResultSet {

	private ResultCursor cursor;

	public BoltResultSet (ResultCursor cursor)
	{
		this.cursor = cursor;
	}

	@Override public boolean next() throws SQLException {
		return false;
	}
}

