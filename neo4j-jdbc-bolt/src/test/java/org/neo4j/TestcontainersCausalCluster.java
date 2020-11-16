package org.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class TestcontainersCausalCluster {
    private static final Logger logger = LoggerFactory.getLogger(TestcontainersCausalCluster.class);
    private static final int DEFAULT_BOLT_PORT = 7687;

    public enum ClusterInstanceType {CORE, READ_REPLICA}

    private static Stream<String> iterateMembers(int numOfMembers, ClusterInstanceType instanceType) {
        final IntFunction<String> generateInstanceName = i -> String.format("neo4j-%s-%d", instanceType.toString(), i);

        return IntStream.rangeClosed(1, numOfMembers).mapToObj(generateInstanceName);
    }

    public static TestcontainersCausalCluster create(int numberOfCoreMembers, int numberOfReadReplica, Duration timeout, Map<String, Object> neo4jConfig) {
        if (numberOfCoreMembers < 3) {
            throw new IllegalArgumentException("numberOfCoreMembers must be >= 3");
        }
        if (numberOfReadReplica < 0) {
            throw new IllegalArgumentException("numberOfReadReplica must be >= 0");
        }

        // Setup a naming strategy and the initial discovery members
        final String initialDiscoveryMembers = iterateMembers(numberOfCoreMembers, ClusterInstanceType.CORE)
                .map(n -> String.format("%s:5000", n))
                .collect(joining(","));

        // Prepare one shared network for those containers
        Network network = Network.newNetwork();

        // Prepare proxys as sidecars
        Map<String, GenericContainer> sidecars = createSidecars(numberOfCoreMembers, network, ClusterInstanceType.CORE);
        sidecars.putAll(createSidecars(numberOfReadReplica, network, ClusterInstanceType.READ_REPLICA));

        // Start the sidecars so that the exposed ports are available
        sidecars.values().forEach(GenericContainer::start);

        // Build the core/read_replica
        List<Neo4jContainer> members = getClusterMembers(numberOfCoreMembers, ClusterInstanceType.CORE, sidecars, network, initialDiscoveryMembers, neo4jConfig, timeout);
        members.addAll(getClusterMembers(numberOfReadReplica, ClusterInstanceType.READ_REPLICA, sidecars, network, initialDiscoveryMembers, neo4jConfig, timeout));

        // Start all of them in parallel
        final CountDownLatch latch = new CountDownLatch(numberOfCoreMembers);
        members.forEach(instance -> CompletableFuture.runAsync(() -> {
            instance.start();
            latch.countDown();
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new TestcontainersCausalCluster(members, sidecars.values().stream().collect(toList()));
    }

    private static List<Neo4jContainer> getClusterMembers(int numberOfCoreMembers,
                                                                   ClusterInstanceType instanceType,
                                                                   Map<String, GenericContainer> sidecars,
                                                                   Network network,
                                                                   String initialDiscoveryMembers,
                                                                   Map<String, Object> neo4jConfig,
                                                                   Duration timeout) {
        // Currently needed as a whole new waiting strategy due to a bug in test containers
        WaitStrategy waitForBolt = new LogMessageWaitStrategy()
                .withRegEx(String.format(".*Bolt enabled on 0\\.0\\.0\\.0:%d\\.\n", DEFAULT_BOLT_PORT))
                .withStartupTimeout(timeout);
        Function<GenericContainer, String> getProxyUrl = instance ->
                String.format("%s:%d", instance.getContainerIpAddress(), instance.getMappedPort(DEFAULT_BOLT_PORT));
        return iterateMembers(numberOfCoreMembers, instanceType)
                .map(name -> getNeo4jContainer(waitForBolt, network, initialDiscoveryMembers, sidecars, getProxyUrl, instanceType, neo4jConfig, name))
                .collect(toList());
    }

    private static Map<String, GenericContainer> createSidecars(int numOfMembers, Network network, ClusterInstanceType instanceType) {
        return iterateMembers(numOfMembers, instanceType)
                .collect(toMap(
                        Function.identity(),
                        name -> new GenericContainer("alpine/socat")
                                .withNetwork(network)
                                .withLabel("memberType", instanceType.toString())
                                // Expose the default bolt port on the sidecar
                                .withExposedPorts(DEFAULT_BOLT_PORT)
                                // And redirect that port to the corresponding Neo4j instance
                                .withCommand(String
                                        .format("tcp-listen:%d,fork,reuseaddr tcp-connect:%s:%1$d", DEFAULT_BOLT_PORT, name))
                ));
    }

    private static Neo4jContainer getNeo4jContainer(WaitStrategy waitForBolt,
                                                                      Network network,
                                                                      String initialDiscoveryMembers,
                                                                      Map<String, GenericContainer> sidecars,
                                                                      Function<GenericContainer, String> getProxyUrl,
                                                                      ClusterInstanceType instanceType,
                                                                      Map<String, Object> neo4jConfig,
                                                                      String name) {
        Neo4jContainer container = (Neo4jContainer) new Neo4jContainer("neo4j:3.5-enterprise")
                .withNeo4jConfig("dbms.mode", instanceType.toString())
                .withNeo4jConfig("dbms.connectors.default_listen_address", "0.0.0.0")
                .withNeo4jConfig("dbms.connectors.default_advertised_address", name)
                .withNeo4jConfig("dbms.connector.bolt.advertised_address", getProxyUrl.apply(sidecars.get(name)))
                .withNeo4jConfig("causal_clustering.initial_discovery_members", initialDiscoveryMembers)
                .withAdminPassword("jdbc")
                .withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes")
                .withLabel("memberType", instanceType.toString())
                .withNetwork(network)
                .withNetworkAliases(name)
//                .withLogConsumer(new Slf4jLogConsumer(logger))
                .waitingFor(waitForBolt);
        neo4jConfig.forEach((conf, value) -> container.withNeo4jConfig(conf, String.valueOf(value)));
        return container;
    }

    private final List<Neo4jContainer> clusterMembers;
    private final List<GenericContainer> sidecars;

    private Driver driver;
    private Session session;

    public TestcontainersCausalCluster(List<Neo4jContainer> clusterMembers,
                                       List<GenericContainer> sidecars) {
        this.clusterMembers = clusterMembers;
        this.sidecars = sidecars;
        this.driver = GraphDatabase.driver(getURI(), AuthTokens.basic("neo4j", "jdbc"));
        this.session = driver.session();
    }

    public Driver getDriver() {
        return driver;
    }

    public Session getSession() {
        return session;
    }

    public URI getURI() {
        return this.sidecars.stream().findAny()
                .map(instance -> String.format("bolt+routing://%s:%d", instance.getContainerIpAddress(),
                        instance.getMappedPort(DEFAULT_BOLT_PORT)))
                .map(uri -> {
                    try {
                        return new URI(uri);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No sidecar as entrypoint into the cluster available."));
    }

    public URI getURIByType(ClusterInstanceType instanceType) {
        return this.sidecars.stream()
                .filter(instance -> instance.getLabels().getOrDefault("memberType", "").equals(instanceType.toString()))
                .findAny()
                .map(instance -> String.format("bolt+routing://%s:%d", instance.getContainerIpAddress(),
                        instance.getMappedPort(DEFAULT_BOLT_PORT)))
                .map(uri -> {
                    try {
                        return new URI(uri);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new IllegalStateException("No sidecar as entrypoint into the cluster available."));
    }

    public URI getAllMembersURI() {
        try {
            return new URI("bolt+routing://" + this.sidecars.stream()
                    .map(instance -> String.format("%s:%d", instance.getContainerIpAddress(),
                            instance.getMappedPort(DEFAULT_BOLT_PORT)))
                    .collect(Collectors.joining(",")));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        getSession().close();
        getDriver().close();
        sidecars.forEach(GenericContainer::stop);
        clusterMembers.forEach(Neo4jContainer::stop);
    }
}

