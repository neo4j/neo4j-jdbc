package org.neo4j.jdbc.bolt;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.kernel.configuration.BoltConnector;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.BoltConnector.EncryptionLevel.DISABLED;

/**
 * provide an embedded in-memory Neo4j instance with bolt enabled
 * the port number is dynamically scanned
 *
 * @author Stefan Armbruster
 */
public class Neo4jBoltRule implements TestRule {

	private       String               hostAndPort;
	private       GraphDatabaseService graphDatabase;
	private String host;
	private int port;
	private final boolean              requireAuth;

	public Neo4jBoltRule() {
		this(false);
	}

	public Neo4jBoltRule(boolean requireAuth) {
		this.requireAuth = requireAuth;
	}

	@Override public Statement apply(final Statement statement, Description description) {
		return new Statement() {

			@Override public void evaluate() throws Throwable {
				Map<Setting<?>, String> settings = new HashMap<>();
				BoltConnector boltConnector = new BoltConnector();
				settings.put(boltConnector.enabled, "true");
				settings.put(boltConnector.type, GraphDatabaseSettings.Connector.ConnectorType.BOLT.name());
				settings.put(boltConnector.encryption_level, DISABLED.name());
				settings.put(GraphDatabaseSettings.auth_enabled, Boolean.toString(requireAuth));
				InetSocketAddress inetAddr = new InetSocketAddress("localhost", getFreePort());
				host = inetAddr.getHostName();
				port = inetAddr.getPort();
				hostAndPort = String.format("%s:%d", inetAddr.getHostName(), inetAddr.getPort());
				settings.put(boltConnector.listen_address, hostAndPort);
				graphDatabase = new TestGraphDatabaseFactory().newImpermanentDatabase(settings);
				try {
					statement.evaluate();
				} finally {
					graphDatabase.shutdown();
				}
			}
		};
	}

	public String getBoltUrl() {
		return String.format("bolt://%s", hostAndPort);
	}

	public GraphDatabaseService getGraphDatabase() {
		return graphDatabase;
	}

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	private int getFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		}
	}
}
