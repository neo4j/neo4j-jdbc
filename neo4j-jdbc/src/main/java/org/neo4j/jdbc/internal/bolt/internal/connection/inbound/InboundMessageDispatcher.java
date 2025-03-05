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
package org.neo4j.jdbc.internal.bolt.internal.connection.inbound;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import org.neo4j.jdbc.internal.bolt.Logging;
import org.neo4j.jdbc.internal.bolt.exception.MessageIgnoredException;
import org.neo4j.jdbc.internal.bolt.exception.Neo4jException;
import org.neo4j.jdbc.internal.bolt.internal.handler.ResponseHandler;
import org.neo4j.jdbc.internal.bolt.internal.messaging.ResponseMessageHandler;
import org.neo4j.jdbc.values.Value;

public final class InboundMessageDispatcher implements ResponseMessageHandler {

	private static final Logger boltLogger = Logging.getLog(InboundMessageHandler.class);

	private final Channel channel;

	private final Queue<ResponseHandler> handlers = new LinkedList<>();

	private boolean fatalErrorOccurred;

	private HandlerHook beforeLastHandlerHook;

	public InboundMessageDispatcher(Channel channel) {
		this.channel = Objects.requireNonNull(channel);
	}

	public void enqueue(ResponseHandler handler) {
		if (this.fatalErrorOccurred) {
			handler.onFailure(new IllegalStateException("No handlers are accepted after fatal error"));
		}
		else {
			this.handlers.add(handler);
		}
	}

	@Override
	public void handleSuccessMessage(Map<String, Value> meta) {
		boltLogger.log(Level.FINE, "S: SUCCESS {0}", meta);
		invokeBeforeLastHandlerHook(HandlerHook.MessageType.SUCCESS);
		var handler = removeHandler();
		handler.onSuccess(meta);
	}

	@Override
	public void handleRecordMessage(Value[] fields) {
		boltLogger.log(Level.FINE, "S: RECORD {0}", Arrays.toString(fields));
		var handler = this.handlers.peek();
		if (handler == null) {
			throw new IllegalStateException(
					"No handler exists to handle RECORD message with fields: " + Arrays.toString(fields));
		}
		handler.onRecord(fields);
	}

	@Override
	public void handleFailureMessage(String code, String message) {
		boltLogger.log(Level.FINE, "S: FAILURE {0} \"{1}\"", new Object[] { code, message });
		var handler = removeHandler();
		handler.onFailure(new Neo4jException(code, message));
	}

	@Override
	public void handleIgnoredMessage() {
		boltLogger.log(Level.FINE, "S: IGNORED");
		var handler = removeHandler();
		handler.onFailure(new MessageIgnoredException("The server has ignored the message"));
	}

	public void handleChannelInactive(Exception cause) {
		while (!this.handlers.isEmpty()) {
			var handler = removeHandler();
			handler.onFailure(cause);
		}
		this.channel.close();
	}

	public void handleChannelError(Exception cause) {
		this.fatalErrorOccurred = true;

		while (!this.handlers.isEmpty()) {
			var handler = removeHandler();
			handler.onFailure(cause);
		}

		this.channel.close();
	}

	private ResponseHandler removeHandler() {
		return this.handlers.remove();
	}

	public void setBeforeLastHandlerHook(HandlerHook beforeLastHandlerHook) {
		if (!this.channel.eventLoop().inEventLoop()) {
			throw new IllegalStateException("This method may only be called in the EventLoop");
		}
		this.beforeLastHandlerHook = beforeLastHandlerHook;
	}

	public boolean fatalErrorOccurred() {
		return this.fatalErrorOccurred;
	}

	private void invokeBeforeLastHandlerHook(HandlerHook.MessageType messageType) {
		if (this.handlers.size() == 1 && this.beforeLastHandlerHook != null) {
			this.beforeLastHandlerHook.run(messageType);
		}
	}

	public interface HandlerHook {

		enum MessageType {

			SUCCESS, FAILURE

		}

		void run(MessageType messageType);

	}

}
