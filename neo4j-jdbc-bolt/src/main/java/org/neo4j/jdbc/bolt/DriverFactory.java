package org.neo4j.jdbc.bolt;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;

import java.net.URI;
import java.util.List;
import java.util.Properties;

@FunctionalInterface
public interface DriverFactory {

    Driver createDriver(List<URI> routingUris, Config config, AuthToken authToken, Properties info);
}
