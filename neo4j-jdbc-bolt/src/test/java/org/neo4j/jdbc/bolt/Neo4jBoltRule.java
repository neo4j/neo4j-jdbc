package org.neo4j.jdbc.bolt;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.harness.internal.Ports;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.BoltConnector.EncryptionLevel.DISABLED;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnector;

/**
 * provide an embedded in-memory Neo4j instance with bolt enabled
 * the port number is dynamically scanned
 *
 * @author Stefan Armbruster
 */
public class Neo4jBoltRule implements TestRule {

	private       String               hostAndPort;
	private       GraphDatabaseService graphDatabase;
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
				GraphDatabaseSettings.BoltConnector boltConnector = boltConnector("0");
				settings.put(boltConnector.type, GraphDatabaseSettings.Connector.ConnectorType.BOLT.name());
				settings.put(boltConnector.enabled, "true");
				settings.put(boltConnector.encryption_level, DISABLED.name());
				settings.put(GraphDatabaseSettings.auth_enabled, Boolean.toString(requireAuth));

				InetSocketAddress inetAddr = Ports.findFreePort("localhost", new int[] { 7687, 64 * 1024 - 1 });
				hostAndPort = String.format("%s:%d", inetAddr.getHostName(), inetAddr.getPort());
				settings.put(boltConnector.address, hostAndPort);
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
		return String.format("bolt://%s?nossl", hostAndPort);
	}

	public GraphDatabaseService getGraphDatabase() {
		return graphDatabase;
	}
}
