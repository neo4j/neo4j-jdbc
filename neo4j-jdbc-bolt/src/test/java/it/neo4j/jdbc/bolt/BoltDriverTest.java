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
 * Created on 23/02/16
 */
package it.neo4j.jdbc.bolt;

import it.neo4j.jdbc.Driver;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class BoltDriverTest {

	@Rule public ExpectedException expectedEx = ExpectedException.none();

	/*------------------------------*/
	/*          acceptsURL          */
	/*------------------------------*/
	@Ignore @Test public void shouldAcceptURLOK() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Driver driver = new BoltDriver();
		assertTrue(driver.acceptsURL("jdbc:bolt://localhost:7474"));
		assertTrue(driver.acceptsURL("jdbc:bolt://192.168.0.1:7474"));
		assertTrue(driver.acceptsURL("jdbc:bolt://localhost:8080"));
	}

	@Ignore @Test public void shouldAcceptURLKO() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		Driver driver = new BoltDriver();
		assertFalse(driver.acceptsURL("jdbc:http://localhost:7474"));
		assertFalse(driver.acceptsURL("jdbc:file://192.168.0.1:7474"));
		assertFalse(driver.acceptsURL("bolt://localhost:7474"));
	}

	@Ignore @Test public void shouldThrowException() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, SQLException {
		expectedEx.expect(SQLException.class);
		expectedEx.expectMessage("null is not a valid url");

		Driver driver = new BoltDriver();
		assertFalse(driver.acceptsURL(null));
	}
}
