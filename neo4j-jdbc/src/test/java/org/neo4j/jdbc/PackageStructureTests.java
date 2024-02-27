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
package org.neo4j.jdbc;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.jdbc.values.Value;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PackageStructureTests {

	private final DescribedPredicate<JavaClass> jdbcModuleClasses = resideInAPackage("..jdbc");

	private JavaClasses allClasses;

	@BeforeAll
	void importAllClasses() {
		this.allClasses = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("org.neo4j.jdbc..");
	}

	@Test
	void boltInternalsShouldOnlyBeUsedFromBoltApi() {
		var rule = classes().that()
			.resideInAPackage("..jdbc.internal.bolt.internal..")
			.should()
			.onlyBeAccessed()
			.byAnyPackage("..jdbc.internal.bolt..");
		rule.check(this.allClasses);
	}

	@Test
	void jdbcModuleClassesMustNotBeUsedFromBoltInternals() {

		var rule = noClasses().that()
			.resideInAPackage("..jdbc.internal..")
			.should()
			.dependOnClassesThat(this.jdbcModuleClasses);
		rule.check(this.allClasses);
	}

	@Test
	void typeSystemShouldBeFreeOfBoltDependencies() {

		var packageUnderTest = Value.class.getPackageName();
		var rule = noClasses().that()
			.resideInAPackage(packageUnderTest)
			.should()
			.dependOnClassesThat(resideOutsideOfPackages("java..", packageUnderTest)
				.and(describe("are not primitives", not(arePrimitives()))));
		rule.check(this.allClasses);
	}

	private static DescribedPredicate<JavaClass> arePrimitives() {
		return new DescribedPredicate<>("Should be a primitive") {
			@Override
			public boolean test(JavaClass input) {
				return input.isPrimitive() || (input.isArray() && input.getBaseComponentType().isPrimitive());
			}
		};
	}

}
