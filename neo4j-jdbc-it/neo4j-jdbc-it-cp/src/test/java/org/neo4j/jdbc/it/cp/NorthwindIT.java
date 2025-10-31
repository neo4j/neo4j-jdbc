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
package org.neo4j.jdbc.it.cp;

import java.sql.SQLException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NorthwindIT extends IntegrationTestBase {

	NorthwindIT() {
		super.doClean = false;
		super.resources.add("/northwind/categories.csv");
		super.resources.add("/northwind/products.csv");
		super.resources.add("/northwind/suppliers.csv");
	}

	@BeforeAll
	void loadGraph() throws SQLException {

		try (var connection = getConnection(false, false); var stmt = connection.createStatement()) {

			stmt.executeUpdate("""
					LOAD CSV WITH HEADERS FROM "file:///products.csv" AS row
					CREATE (n:Product)
					SET n = row,
					n.unitPrice = toFloat(row.unitPrice),
					n.unitsInStock = toInteger(row.unitsInStock), n.unitsOnOrder = toInteger(row.unitsOnOrder),
					n.reorderLevel = toInteger(row.reorderLevel), n.discontinued = (row.discontinued <> "0")
					""");

			stmt.executeUpdate("""
					LOAD CSV WITH HEADERS FROM "file:///categories.csv" AS row
					CREATE (n:Category)
					SET n = row
					""");

			stmt.executeUpdate("""
					 LOAD CSV WITH HEADERS FROM "file:///suppliers.csv" AS row
					 CREATE (n:Supplier)
					 SET n = row
					""");
		}
	}

	@ParameterizedTest
	@CsvSource(delimiterString = "|",
			textBlock = """
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id) VALUES (?, ?)                        | MERGE (_lhs:Supplier {id: $1}) MERGE (_rhs:Product {id: $2}) CREATE (_lhs)-[:SUPPLIES]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (id, Product.id) VALUES (?, ?)                                 | CREATE (_lhs:Supplier) MERGE (_rhs:Product {id: $2}) CREATE (_lhs)-[:SUPPLIES {id: $1}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Product.id, id) VALUES (?, ?)                                 | CREATE (_lhs:Supplier) MERGE (_rhs:Product {id: $1}) CREATE (_lhs)-[:SUPPLIES {id: $2}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES (?, ?, ?)    | MERGE (_lhs:Supplier {id: $1}) MERGE (_rhs:Product {id: $2}) CREATE (_lhs)-[:SUPPLIES {amount: $3}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES ($a, $b, $c) | MERGE (_lhs:Supplier {id: $a}) MERGE (_rhs:Product {id: $b}) CREATE (_lhs)-[:SUPPLIES {amount: $c}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES ($b, $a, $c) | MERGE (_lhs:Supplier {id: $b}) MERGE (_rhs:Product {id: $a}) CREATE (_lhs)-[:SUPPLIES {amount: $c}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES ($b, $c, $a) | MERGE (_lhs:Supplier {id: $b}) MERGE (_rhs:Product {id: $c}) CREATE (_lhs)-[:SUPPLIES {amount: $a}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES ($1, $2, $3) | MERGE (_lhs:Supplier {id: $1}) MERGE (_rhs:Product {id: $2}) CREATE (_lhs)-[:SUPPLIES {amount: $3}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount) VALUES ($3, $1, $2) | MERGE (_lhs:Supplier {id: $3}) MERGE (_rhs:Product {id: $1}) CREATE (_lhs)-[:SUPPLIES {amount: $2}]->(_rhs)
					INSERT INTO Supplier_SUPPLIES_Product (Supplier.id, Product.id, SUPPLIES.amount, SUPPLIES.lol) VALUES ($3, $1, $2, ?) | MERGE (_lhs:Supplier {id: $3}) MERGE (_rhs:Product {id: $1}) CREATE (_lhs)-[:SUPPLIES {amount: $2, lol: $4}]->(_rhs)
					""")
	void insertsShouldUseStableParameters(String sql, String cypher) throws SQLException {
		try (var connection = getConnection(false, false, "s2c.parseNamedParamPrefix", "$")) {
			Assertions.assertThat(connection.nativeSQL(sql)).isEqualTo(cypher);
		}
	}

}
