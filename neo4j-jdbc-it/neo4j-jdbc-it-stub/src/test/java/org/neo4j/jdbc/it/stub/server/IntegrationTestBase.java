/*
 * Copyright (c) 2023-2024 "Neo4j,"
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
package org.neo4j.jdbc.it.stub.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Consumer;

import com.github.dockerjava.api.exception.NotModifiedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({ StubScriptConfigParameterResolver.class, StubServerExecutionExceptionHandler.class })
public abstract class IntegrationTestBase {

	@SuppressWarnings("resource")
	protected IntegrationTestBase() {
		this.stubServer = new GenericContainer<>(
				new ImageFromDockerfile().withFileFromClasspath("scripts", "docker/scripts")
					.withFileFromClasspath("entrypoint.sh", "docker/entrypoint.sh")
					.withFileFromClasspath("Dockerfile", "docker/Dockerfile"))
			.withExposedPorts(7687)
			.waitingFor(Wait.forLogMessage("^Listening\\n$", 1));
	}

	private final GenericContainer<?> stubServer;

	private LogAccumulator logAccumulator;

	@BeforeEach
	void startStubServer(StubScriptConfig stubScriptConfig) {
		this.stubServer.setCommand(stubScriptConfig.path());
		this.logAccumulator = new LogAccumulator();
		this.stubServer.withLogConsumer(this.logAccumulator);
		this.stubServer.start();
	}

	protected void verifyStubServer() {
		var state = this.stubServer.getCurrentContainerInfo().getState();
		if (Boolean.TRUE.equals(state.getRunning())) {
			var dockerClient = this.stubServer.getDockerClient();
			var containerId = this.stubServer.getContainerId();
			try {
				dockerClient.stopContainerCmd(containerId).exec();
			}
			catch (NotModifiedException ignored) {
				// assume it was stopped already
			}
			state = this.stubServer.getCurrentContainerInfo().getState();
			var exitCode = state.getExitCodeLong();
			if (exitCode != null && exitCode != 0) {
				throwStubServerException();
			}
		}
		else {
			var exitCode = state.getExitCodeLong();
			if (exitCode != null && exitCode != 0) {
				throwStubServerException();
			}
		}
	}

	private void throwStubServerException() {
		var log = this.logAccumulator.getLog();
		throw new StubServerException(String.format("See the stub server output below. \n%s", log));
	}

	@AfterEach
	void stopStubServer() {
		this.stubServer.stop();
	}

	protected final Connection getConnection() throws SQLException {
		var url = "jdbc:neo4j://%s:%d".formatted(this.stubServer.getHost(), this.stubServer.getMappedPort(7687));
		var driver = DriverManager.getDriver(url);
		var properties = new Properties();
		properties.put("user", "neo4j");
		properties.put("password", "password");
		return driver.connect(url, properties);
	}

	private static final class LogAccumulator implements Consumer<OutputFrame> {

		private final StringBuilder stringBuilder = new StringBuilder();

		@Override
		public synchronized void accept(OutputFrame outputFrame) {
			this.stringBuilder.append(outputFrame.getUtf8String());
		}

		synchronized String getLog() {
			return this.stringBuilder.toString();
		}

	}

}
