package org.neo4j.jdbc;

import java.util.Map;
import java.util.function.Supplier;

public class TestAuthenticationSupplierFactory implements AuthenticationSupplierFactory {

	public TestAuthenticationSupplierFactory() {
		System.out.println("what?");
	}

	@Override
	public String getName() {
		System.out.println("getting da name");
		return "testsupplier";
	}

	@Override
	public Supplier<Authentication> newAuthenticationSupplier(Map<String, ?> properties) {
		return () -> Authentication.usernameAndPassword((String) properties.get("username"), "whatever");
	}

}
