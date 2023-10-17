/*
 * Copyright (c) 2023 "Neo4j,"
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
package org.neo4j.driver.jdbc.internal.bolt.internal.connection;

import javax.net.ssl.SSLEngine;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslHandler;
import org.neo4j.driver.jdbc.internal.bolt.BoltServerAddress;
import org.neo4j.driver.jdbc.internal.bolt.SecurityPlan;
import org.neo4j.driver.jdbc.internal.bolt.internal.connection.inbound.InboundMessageDispatcher;

public final class NettyChannelInitializer extends ChannelInitializer<Channel> {

	private final BoltServerAddress address;

	private final SecurityPlan securityPlan;

	private final int connectTimeoutMillis;

	public NettyChannelInitializer(BoltServerAddress address, SecurityPlan securityPlan, int connectTimeoutMillis) {
		this.address = address;
		this.securityPlan = securityPlan;
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	@Override
	protected void initChannel(Channel channel) {
		if (this.securityPlan.requiresEncryption()) {
			var sslHandler = createSslHandler();
			channel.pipeline().addFirst(sslHandler);
		}

		updateChannelAttributes(channel);
	}

	private SslHandler createSslHandler() {
		var sslEngine = createSslEngine();
		var sslHandler = new SslHandler(sslEngine);
		sslHandler.setHandshakeTimeoutMillis(this.connectTimeoutMillis);
		return sslHandler;
	}

	private SSLEngine createSslEngine() {
		var sslContext = this.securityPlan.sslContext();
		var sslEngine = sslContext.createSSLEngine(this.address.host(), this.address.port());
		sslEngine.setUseClientMode(true);
		if (this.securityPlan.requiresHostnameVerification()) {
			var sslParameters = sslEngine.getSSLParameters();
			sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
			sslEngine.setSSLParameters(sslParameters);
		}
		return sslEngine;
	}

	private void updateChannelAttributes(Channel channel) {
		ChannelAttributes.setMessageDispatcher(channel, new InboundMessageDispatcher(channel));
	}

}
