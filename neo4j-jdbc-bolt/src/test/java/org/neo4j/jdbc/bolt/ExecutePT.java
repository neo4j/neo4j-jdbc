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
import org.neo4j.jdbc.bolt.data.PerformanceTestData;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.TimeUnit;

/**
 * @author AgileLARUS
 * @since 3.3
 * Created by the issue #141
 */
public class ExecutePT {

	@Test public void launchBenchmark() throws Exception {
		PerformanceTestData.loadABCXYData();

		// @formatter:off
		Options opt = new OptionsBuilder()
				.include(this.getClass().getName() + ".*")
				.mode(Mode.AverageTime)
				.timeUnit(TimeUnit.MICROSECONDS)
				//Warmup
				//.warmupTime(TimeValue.seconds(1))
				.warmupIterations(1)
				//Measurement
				.measurementTime(TimeValue.seconds(1))
				.measurementIterations(10)
				.threads(1)
				.forks(1)
				.shouldFailOnError(true)
				//.shouldDoGC(true)
				//.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
				//.addProfiler(WinPerfAsmProfiler.class)
				.build();

		new Runner(opt).run();
	}

	@State(Scope.Thread) public static class Data {

		@Setup public void initialize() throws ClassNotFoundException, SQLException, IOException {
			connection = PerformanceTestData.getConnection();
			stmt = connection.prepareStatement(query);
			stmt.setLong(1, (long)(Math.random() * 100));
		}

		@TearDown public void close() throws ClassNotFoundException, SQLException, IOException {
			stmt.close();
			connection.close();
		}


		public String query = "MATCH (n:A {prop: ?}) RETURN n as NODO, id(n) as ID, n.prop as PROP LIMIT 2;";
		public Connection connection;
		public PreparedStatement stmt;
	}

	@Benchmark public void testExecuteNoMetadata(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		PreparedStatement stmt = data.stmt;

		stmt.execute();
		ResultSet resultSet = stmt.getResultSet();

		while(resultSet.next()){
			resultSet.getObject(1);
		}

		resultSet.close();
	}

	@Benchmark public void testExecuteQueryNoMetadata(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		PreparedStatement stmt = data.stmt;

		ResultSet resultSet = stmt.executeQuery();

		while(resultSet.next()){
			resultSet.getObject(1);
		}

		resultSet.close();
	}

	@Benchmark public void testExecuteQueryWithMetadata(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		PreparedStatement stmt = data.stmt;

		ResultSet resultSet = stmt.executeQuery();
		ResultSetMetaData metaData = resultSet.getMetaData();

		int columnCount = metaData.getColumnCount();
		for(int c=1; c<=columnCount;c++){
			metaData.getColumnName(c);
			metaData.getColumnType(c);
			metaData.getColumnLabel(c);
			metaData.getColumnClassName(c);
		}

		while(resultSet.next()){
			resultSet.getObject("NODO");
			resultSet.getObject("PROP");
			resultSet.getObject("ID");
		}

		resultSet.close();
	}

	@Benchmark public void testExecuteWithMetadata(Data data, Blackhole bh) throws ClassNotFoundException, SQLException {
		PreparedStatement stmt = data.stmt;

		stmt.execute();
		ResultSet resultSet = stmt.getResultSet();
		ResultSetMetaData metaData = resultSet.getMetaData();

		int columnCount = metaData.getColumnCount();
		for(int c=1; c<=columnCount;c++){
			metaData.getColumnName(c);
			metaData.getColumnType(c);
			metaData.getColumnLabel(c);
			metaData.getColumnClassName(c);
		}

		while(resultSet.next()){
			resultSet.getObject("NODO");
			resultSet.getObject("PROP");
			resultSet.getObject("ID");
		}

		resultSet.close();
	}

}
