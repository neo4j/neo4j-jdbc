package org.neo4j.jdbc.events;

import java.net.URI;

public interface AuthenticationListener {

	default void onNewAuthentication(NewAuthenticationEvent event) {
	}

	record NewAuthenticationEvent(URI uri, boolean refreshed) {
	}

}
