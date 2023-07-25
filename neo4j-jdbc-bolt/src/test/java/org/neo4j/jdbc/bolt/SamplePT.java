/*
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
 * Created on 14/03/16
 */
package org.neo4j.jdbc.bolt;

import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.jdbc.bolt.data.PerformanceTestData;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * @author AgileLARUS
 * @since 3.0.0
 */
public class SamplePT {

	@Test public void launchBenchmark() throws Exception {

		PerformanceTestData.loadABCXYData();

		// @formatter:off
		Options opt = new OptionsBuilder()
				.include(this.getClass().getName() + ".*")
				.mode(Mode.AverageTime)
				.timeUnit(TimeUnit.MICROSECONDS)
				//Warmup
				.warmupTime(TimeValue.seconds(1))
				.warmupIterations(1)
				//Measurement
				.measurementTime(TimeValue.seconds(1))
				.measurementIterations(10)
				.threads(1)
				.forks(2)
				.shouldFailOnError(true)
				//.shouldDoGC(true)
				//.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
				//.addProfiler(WinPerfAsmProfiler.class)
				.build();

		new Runner(opt).run();
	}

	@State(Scope.Benchmark) public static class Data {
		@Setup public void initialize() throws ClassNotFoundException, SQLException, IOException {
			jdbcConnection = DriverManager.getConnection("jdbc:neo4j:bolt://localhost:7687?user=neo4j,password=test");
			driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "test"));
		}

		@TearDown public void close() throws ClassNotFoundException, SQLException, IOException {
			jdbcConnection.close();
			driver.close();
		}

		public String query = "MATCH (n) RETURN n";
		public Connection jdbcConnection;
		public org.neo4j.driver.Driver driver;

	}

	@Benchmark public void testSimpleQueryJDBC(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		Connection conn = data.jdbcConnection;
		Statement stmt = conn.createStatement();
		bh.consume(stmt.executeQuery(data.query));
		stmt.close();
	}

	@Benchmark public void testSimpleQueryBoltDriver(Data data, Blackhole bh) {
		org.neo4j.driver.Driver driver = data.driver;
		Session session = driver.session();
		bh.consume(session.run(data.query));
		session.close();
	}

}
