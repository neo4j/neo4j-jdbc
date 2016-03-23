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
 * Created on 01/03/2016
 */
package it.larusba.neo4j.jdbc.bolt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * DatabaseMetaData Tests class
 *
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDatabaseMetaDataTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*        getConnection         */
	/*------------------------------*/

	@Test public void getConnectionShouldGetConnection() throws SQLException, NoSuchFieldException, IllegalAccessException {
		BoltConnection connection = Mockito.mock(BoltConnection.class);
		BoltDatabaseMetaData boltDatabaseMetaData = new BoltDatabaseMetaData(null);
		Field field = BoltDatabaseMetaData.class.getDeclaredField("connection");
		field.setAccessible(true);
		field.set(boltDatabaseMetaData, connection);
		assertEquals(connection, boltDatabaseMetaData.getConnection());
	}
}
