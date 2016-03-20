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
 * Created on 14/03/16
 */
package it.neo4j.jdbc.bolt;

import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
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
		// @formatter:off
		Options opt = new OptionsBuilder()
				.include(this.getClass().getName() + ".*")
				.mode(Mode.AverageTime)
				.timeUnit(TimeUnit.MICROSECONDS)
				//Warmup
				.warmupTime(TimeValue.seconds(1))
				.warmupIterations(10)
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

	@State(Scope.Thread) public static class Data {
		@Setup public static void initialize() throws ClassNotFoundException, SQLException, IOException {
			Class.forName("it.neo4j.jdbc.bolt.BoltDriver");
		}

		public String query = "MATCH (n) RETURN n";
	}

	@Benchmark public void testSimpleQueryJDBC(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		Connection conn = DriverManager.getConnection("jdbc:bolt://localhost:7687");
		Statement stmt = conn.createStatement();
		bh.consume(stmt.executeQuery(data.query));
		stmt.close();
		conn.close();
	}

	@Benchmark public void testSimpleQueryBoltDriver(Data data, Blackhole bh) {
		org.neo4j.driver.v1.Driver driver = GraphDatabase.driver("bolt://localhost:7687");

		Session session = driver.session();

		bh.consume(session.run(data.query));

		session.close();
	}

	@Benchmark public void testSimpleQueryWithDebugJDBC(Data data, Blackhole bh) throws SQLException {
		System.setOut(Mockito.mock(PrintStream.class));
		Connection conn = DriverManager.getConnection("jdbc:bolt://localhost:7687?debug");
		Statement stmt = conn.createStatement();
		bh.consume(stmt.executeQuery(data.query));
		stmt.close();
		conn.close();
	}
}
