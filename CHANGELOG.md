# 6.10.0

## 🚀 Features
- 0113b73 feat(translator): Add support for `LEFT JOIN` and improve flattening of joins. (#1170)
- 1c6b3e6 feat: Add support for retrieving generated keys.

## 🐛 Bug Fixes
- cf9003f fix(benchkit): Use correct classname for configuring the OpenAPI date format inside Springs Jackson customizer.
- d88f2b8 Fix formatting errors.

## 🔄️ Refactorings
- f5b5a11 refactor(benchkit): Use a non-root user for running the entry point of the container.
- ecd7a5c refactor: More improvements on the metadata of virtual tables for relationships. (#1169)
- 8103de6 refactor(translator): Don’t wrap parameters in an artifical list for the `IN` keyword.

## 📝 Documentation
- 3cb81a0 chore: Update dependencies for Antora-Documentation generation, bump driver version in `package.json`.

## 🧹 Housekeeping
- 3512912 Bump org.apache.commons:commons-lang3 from 3.19.0 to 3.20.0 (#1183)
- f75f188 Bump org.testcontainers:testcontainers-bom (#1187)
- a14f086 Bump org.apache.maven.plugins:maven-jar-plugin (#1186)
- 39ffa48 Bump org.graalvm.buildtools:native-maven-plugin (#1185)
- f795894 Bump org.jooq:jooq from 3.19.27 to 3.19.28 (#1184)
- 4d5ed8d Bump org.sonarsource.scanner.maven:sonar-maven-plugin (#1182)
- f921652 Bump com.puppycrawl.tools:checkstyle from 12.1.1 to 12.1.2 (#1181)
- ec179e7 Bump quarkus.platform.version from 3.29.2 to 3.29.3 (#1180)
- fbdcb37 Bump org.apache.maven.plugins:maven-release-plugin (#1179)
- 6de0152 Bump org.neo4j:cypher-v5-antlr-parser (#1178)
- 0540859 Bump org.jdbi:jdbi3-bom from 3.49.6 to 3.50.0 (#1177)
- faac034 Bump io.micrometer:micrometer-tracing-bom (#1176)
- ddea6e6 build(deps-dev): Bump org.asciidoctor:asciidoctorj from 3.0.0 to 3.0.1 (#1175)
- 04a9e07 Bump quarkus.platform.version from 3.28.4 to 3.29.2 (#1174)
- 2d2b60b Bump org.hibernate.orm:hibernate-platform (#1173)
- d821e36 Bump org.openapitools:openapi-generator-maven-plugin (#1172)
- 4f4d21e Bump io.micrometer:micrometer-bom from 1.15.5 to 1.16.0 (#1171)
- 6ed4599 Bump org.junit:junit-bom from 6.0.0 to 6.0.1 (#1163)
- 75b93da Bump io.fabric8:docker-maven-plugin from 0.46.0 to 0.47.0 (#1160)
- 474cc4e Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#1165)
- 48427cb Bump com.fasterxml.jackson:jackson-bom (#1164)
- 1a70651 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.20 to 2.3.23 (#1166)
- 61036fc Bump spring-boot.version from 3.5.6 to 3.5.7 (#1162)
- 9b7c466 Bump org.jreleaser:jreleaser-maven-plugin (#1161)
- 8cd78c0 Bump dev.langchain4j:langchain4j-bom from 1.7.1 to 1.8.0 (#1159)
- eccba7d Bump org.openapitools:jackson-databind-nullable (#1158)
- d1dbf45 Bump org.neo4j:cypher-v5-antlr-parser (#1157)

## 🛠 Build
- 651b875 build: Require JDK 25 for all pipelines, run integration tests on 17 and 25. (#1167)


# 6.9.1

Thanks a lot to @meistermeier and @venikkin for providing me with a plethora of test cases making the relationship insertion feature much better.

## 🐛 Bug Fixes
- a9ebf0e fix: Parameter name generation and handling with index like parameters.
- 80b08a8 fix: Improve relationship insertation.

## 🔄️ Refactorings
- 8fc9395 refactor: Use more common start/end notation than lhs/rhs.

## 🧹 Housekeeping
- 95bd1c5 Bump org.testcontainers:testcontainers-bom from 1.21.3 to 2.0.1 (#1144)
- 5388da7 Bump eu.michael-simons.maven:native-image-config-transformer (#1151)
- 9b528ef Bump org.neo4j.bolt:neo4j-bolt-connection-bom (#1149)
- 276e538 Bump io.micrometer:micrometer-bom from 1.15.4 to 1.15.5 (#1153)
- 7554a85 Bump com.puppycrawl.tools:checkstyle from 12.0.1 to 12.1.1 (#1154)
- f756741 Bump io.netty:netty-bom from 4.1.127.Final to 4.1.128.Final (#1152)
- e987ada Bump io.micrometer:micrometer-tracing-bom (#1148)
- 12da0b1 Bump org.codehaus.mojo:exec-maven-plugin (#1147)
- 289c581 Bump org.graalvm.buildtools:native-maven-plugin (#1146)
- 6a06192 Bump quarkus.platform.version from 3.28.3 to 3.28.4 (#1145)


# 6.9.0

This release contains a major new feature: Manipulating Neo4j-Relationships via plain SQL. Essentially, you can now create a relationship like this

```sql
INSERT INTO Person_ACTED_IN_Movie(name, role, title)
VALUES
    ('Jaret Leto', 'Ares', 'TRON Ares'),
    ('Jodie Turner-Smith', 'Athena', 'TRON Ares');
```

which will give you this graph:

![](https://neo4j.com/docs/jdbc-manual/current/_images/after_first_insert.png)

Read the full manual here [Manipulating relationships](https://neo4j.com/docs/jdbc-manual/current/sql2cypher/#s2c_manipulating_relationships)

The release also brings support for TCP fast open on the network level and makes the driver more future proof by supporting the `UNSUPPORTED` bolt type (no pun intended).

## 🚀 Features
- 40b3c01 feat: Allow manipulation of relationships via a single target table. (#1143)
- 2babc58 feat: Add support for the `UNSUPPORTED` bolt type. (#1120)
- d94cc1e feat: Add support for the TCP fast open provided by the Bolt connection api.

## 🐛 Bug Fixes
- 489bbed fix(tracing): Enable tracing to work with Query API (#1142)

## 📝 Documentation
- 4f4043a docs: Update local changelog.

## 🧹 Housekeeping
- e9c37c0 Bump com.github.siom79.japicmp:japicmp-maven-plugin (#1136)
- 51ca0a3 Bump org.jacoco:jacoco-maven-plugin from 0.8.13 to 0.8.14 (#1140)
- e1e7695 build(deps-dev): Bump com.github.dasniko:testcontainers-keycloak (#1141)
- 861dce5 Bump com.puppycrawl.tools:checkstyle from 11.1.0 to 12.0.1 (#1139)
- 24472fe Bump org.hibernate.orm:hibernate-platform (#1138)
- 23f2471 Bump quarkus.platform.version from 3.28.2 to 3.28.3 (#1137)
- c5115e7 Bump org.neo4j:cypher-v5-antlr-parser (#1135)
- a50c79c Bump org.codehaus.mojo:exec-maven-plugin (#1134)
- d0f5133 Bump org.keycloak:keycloak-authz-client (#1132)
- b681221 Bump org.jooq:jooq from 3.19.26 to 3.19.27 (#1130)
- 218841d Bump org.neo4j.bolt:neo4j-bolt-connection-bom (#1133)
- f3665af Bump org.junit:junit-bom from 5.13.4 to 6.0.0 (#1131)
- b8df800 Bump org.hibernate.orm:hibernate-platform (#1129)
- 9da839e Bump org.jdbi:jdbi3-bom from 3.49.5 to 3.49.6 (#1128)
- bbd4c41 Bump org.apache.maven.plugins:maven-enforcer-plugin (#1127)
- 62aa255 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.19 to 2.3.20 (#1126)
- 987ee03 Bump quarkus.platform.version from 3.28.1 to 3.28.2 (#1125)
- 82a0ae4 Bump dev.langchain4j:langchain4j-bom from 1.6.0 to 1.7.1 (#1124)
- 77c4728 Bump org.apache.commons:commons-lang3 from 3.18.0 to 3.19.0 (#1119)
- 5eb90d5 Bump org.neo4j:neo4j-cypher-dsl-bom (#1115)
- f77b325 Bump dev.langchain4j:langchain4j-bom from 1.5.0 to 1.6.0 (#1122)
- a2ee1e2 Bump org.assertj:assertj-core from 3.27.5 to 3.27.6 (#1121)
- a21a4da Bump com.puppycrawl.tools:checkstyle from 11.0.1 to 11.1.0 (#1118)
- 7dd9833 Bump quarkus.platform.version from 3.26.4 to 3.28.1 (#1117)
- 1aa4e4c Bump org.graalvm.buildtools:native-maven-plugin (#1116)
- 5ecb3bb Bump org.openapitools:openapi-generator-maven-plugin (#1114)
- ab98657 build: Ignore more betas and milestones in the versions plugin.
- 6a71609 build: Upgrade testing to Neo4j 2025.09. (#1123)
- 3c35616 build: Update dependencies
- 1204349 build: Update dependencies.


# 6.8.0

This release brings support for transmitting the new data types optimised for vector storage in future Neo4j versions. If you want to try them out, you need at least Neo4j 2025.8 and enable several settings on the database site. Consult the Neo4j documentation on how to do that.

The new types are provided via factory methods in `org.neo4j.jdbc.values.Vector` for creating them client-side and passing them as parameters and we support all Neo4j supported inner types:

* `INTEGER8` (Java `byte`)
* `INTEGER16` (Java `short`)
* `INTEGER32` (Java `int`)
* `INTEGER` (Java `long`)
* `FLOAT16` (Java `float`)
* `FLOAT` (Java `double`)

Our current client implementations are array based unless we can in some future use Javas Vector API.
Vectors can be used as is, or even used as SQL `ARRAY`'s

The rest of this release is consists mostly of dependency updates.

## 🚀 Features
- 87acb67 build: Add support for upgrading all dependencies via Maven.
- b02ab8e feat: Add support for the new Neo4j `Vector` type. (#1051)

## 🐛 Bug Fixes
- 6264aed build: Fix JavaDoc warnings wrt the optional JAXB dependency.

## 📝 Documentation
- 7c16e99 docs: Document the authn/kc module and the various Neo4j vector types.

## 🧹 Housekeeping
- e3fb173 Bump dev.langchain4j:langchain4j-bom from 1.3.0 to 1.4.0 (#1083)
- c5e65aa Bump org.openapitools:openapi-generator-maven-plugin (#1074)
- 7571c7b Bump org.mockito:mockito-bom from 5.18.0 to 5.19.0 (#1080)
- b190f07 Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#1079)
- b2697a2 Bump org.openapitools:jackson-databind-nullable (#1078)
- b8d3311 Bump io.netty:netty-bom from 4.1.123.Final to 4.1.124.Final (#1077)
- d72e801 Bump spring-boot.version from 3.5.4 to 3.5.5 (#1076)
- 734ef3d Bump quarkus.platform.version from 3.25.3 to 3.25.4 (#1075)
- 3c51df6 Bump org.neo4j:neo4j-cypher-dsl-bom to 2025.0.0
- 77c4fc7 Bump org.neo4j.bolt:neo4j-bolt-connection-bom from 6.0.2 to 7.0.0 (#1065)
- 58b55e6 Bump io.micrometer:micrometer-bom from 1.15.2 to 1.15.3 (#1072)
- 226df5e Bump io.micrometer:micrometer-tracing-bom (#1073)
- 2f133e2 Bump org.jooq:jooq from 3.19.24 to 3.19.25 (#1071)
- 10dbb90 Bump org.neo4j:neo4j-cypher-dsl-bom (#1057)
- 38aa96b Bump org.codehaus.mojo:flatten-maven-plugin (#1060)
- e8f03b3 Bump com.puppycrawl.tools:checkstyle from 10.26.1 to 11.0.0 (#1062)
- c679045 Bump dev.langchain4j:langchain4j-bom from 1.1.0 to 1.3.0 (#1064)
- 987f53a Bump org.hibernate.orm:hibernate-platform (#1066)
- ba1f85f Bump org.neo4j:cypher-v5-antlr-parser (#1067)
- 00a1d52 Bump org.assertj:assertj-core from 3.27.3 to 3.27.4 (#1068)
- 36e8c65 Bump quarkus.platform.version from 3.24.5 to 3.25.3 (#1069)
- 3f94ffe Bump org.apache.maven.plugins:maven-javadoc-plugin (#1070)
- 715a83a Add some more tests for the value system.

## 🛠 Build
- 8a9eb9d build: Downgrade to JRelease 1.19 again.
- 821ff30 build: Update to Neo4j 2025.8 for all tests by default.
- 83f6e13 build: Upgrade vector tests to use latest Neo4j 2025.8.
- a8a26d6 build: Upgrade dependencies.
- a11d810 build: Upgrade dependencies.
- 46f2921 build: Upgrade dependencies.
- a70b1d9 build: Add service transformer to text2cypher bundle.
- c5d1ea2 build: Downgrade to Javadoc plugin 3.11.2 to get rid of new wrong build warnings.


# 6.7.3

## 🐛 Bug Fixes
- 12e2b86 fix: Proper test multiple node types, and fix metadata doc.

## 🧹 Housekeeping
- 225cfdf Bump com.opencsv:opencsv from 5.11.2 to 5.12.0 (#1053)
- 04901e3 Bump spring-boot.version from 3.5.3 to 3.5.4 (#1056)
- c9036b2 Bump org.junit:junit-bom from 5.13.3 to 5.13.4 (#1055)
- 56c0fb9 Bump org.hibernate.orm:hibernate-platform (#1054)
- 7ff8dd9 Bump quarkus.platform.version from 3.24.4 to 3.24.5 (#1052)


# 6.7.2

## 🚀 Features
- f8078f1 feat: Allow to configure User-Agent from a dedicated resource. (#1041)

## 🐛 Bug Fixes
- 259f89a fix: Prevent a class cast exception when transmitting parameters of type `Point` or `IsoDuration` to the server. (#1042)

## 🧹 Housekeeping
- aca6fda Bump io.netty:netty-bom from 4.1.121.Final to 4.1.123.Final (#1049)
- dbfc88a Bump quarkus.platform.version from 3.24.3 to 3.24.4 (#1048)
- 392a48e Bump org.graalvm.buildtools:native-maven-plugin (#1047)
- 4843e90 Bump org.moditect:moditect-maven-plugin (#1046)
- fb9d651 Bump org.apache.maven.plugins:maven-enforcer-plugin (#1045)
- df64186 Bump com.fasterxml.jackson:jackson-bom (#1044)

## 🛠 Build
- 5c9984f build: Remove workarounds for JUnit 5.13 to work with older native image build tools. (#1050)


# 6.7.1

## 🚀 Features
- dad6a0d feat: Allow user-agent customisation via env-variables or system properties. (#1032)

## 📝 Documentation
- 864b042 docs: Update local changelog.
- 72102a5 docs: Remove quotes from prerelease attribute. (#1033)

## 🧹 Housekeeping
- 0404c49 Bump io.micrometer:micrometer-bom from 1.15.1 to 1.15.2 (#1039)
- e0b401d Bump quarkus.platform.version from 3.24.2 to 3.24.3 (#1037)
- 6e75f97 Bump org.hibernate.orm:hibernate-platform (#1038)
- 0d0ec22 Bump mybatis-spring-boot-starter.version (#1036)
- 9217bf1 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.8 to 5.26.9 (#1035)
- ac484d1 Bump io.micrometer:micrometer-tracing-bom (#1034)

## 🛠 Build
- cdc7a19 test: Add integration tests for retrieving the user-agent. (#1040)
- b7f4f90 build: Upgrade stubserver / testkit tests to use latest Python 3.10 image.


# 6.7.0

## Can your JDBC driver do this?

To highlight some of the features that have accumulated in the Neo4j JDBC driver since the first commit to the 6.x branch two years ago, we decided to create two hopefully easy to read demos before we jump into the list of features. We use plain Java in the first 3 examples, no dependency management and run fully on the module path with `Demo1.java`. The keen reader may notices that we do use some Java 24 preview features: Why bother with a class definition? And if we have Java modules, why not use them for import statements? (Be aware, that the JDBC driver does run on Java 17, too and does not require any preview feature)

### Cypher and SQL supported

First, let's download the JDBC bundle:

```bash
wget https://repo.maven.apache.org/maven2/org/neo4j/neo4j-jdbc-full-bundle/6.7.0/neo4j-jdbc-full-bundle-6.7.0.jar
```

And run the following class

```java
import module java.sql;

void main() throws SQLException {

    try (
        var connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/movies", "neo4j", "verysecret");
        var stmt = connection.prepareStatement("""
            MERGE (g:Genre {name: $1})
            MERGE (m:Movie {title: $2, released: $3}) -[:HAS]->(g)
            FINISH"""))
    {
        stmt.setString(1, "Science Fiction");
        stmt.setString(2, "Dune");
        stmt.setInt(3, 2021);
        stmt.addBatch();

        stmt.setString(1, "Science Fiction");
        stmt.setString(2, "Star Trek Generations");
        stmt.setInt(3, 1994);
        stmt.addBatch();

        stmt.setString(1, "Horror");
        stmt.setString(2, "Seven");
        stmt.setInt(3, 1995);
        stmt.addBatch();

        stmt.executeBatch();
    }

    // Not happy with Cypher? Enable SQL translations
    try (
        var connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/movies?enableSQLTranslation=true", "neo4j", "verysecret");
        var stmt = connection.createStatement();
        var rs = stmt.executeQuery("""
            SELECT m.*, collect(g.name) AS genres
            FROM Movie m NATURAL JOIN HAS r NATURAL JOIN Genre g
            WHERE m.title LIKE 'S%'
            ORDER BY m.title LIMIT 20"""))
    {
        while (rs.next()) {
            var title = rs.getString("title");
            var genres = rs.getObject("genres", List.class);
            System.out.println(title + " " + genres);
        }
    }
}
```

like this:

```bash
java --enable-preview --module-path neo4j-jdbc-full-bundle-6.7.0.jar Demo1.java
```

The output of the program will be similar to this. Notice how the Cypher has been rewritten to be proper batched:

```
Juli 09, 2025 12:52:54 PM org.neo4j.jdbc.PreparedStatementImpl executeBatch
INFORMATION: Rewrite batch statements is in effect, statement MERGE (g:Genre {name: $1})
MERGE (m:Movie {title: $2, released: $3}) -[:HAS]->(g)
FINISH has been rewritten into UNWIND $__parameters AS __parameter MERGE (g:Genre {name: __parameter['1']})
MERGE (m:Movie {title: __parameter['2'], released: __parameter['3']}) -[:HAS]->(g)
FINISH
Seven [Horror]
Star Trek Generations [Science Fiction]
Horror
Science Fiction
```

*Bonus example* As JDBC 4.3, section 13.2 specifies that only `?` are allowed as positional parameters, we do of course handle those. The above example uses _named_ Cypher parameters `$1`, `$2` to align them with the indexes that the `PreparedStatement` requires.  If we would switch languages here, using SQL for inserting and Cypher for querying, you see the difference. Also take now that you can `unwrap` the `PreparedStatement` into a `Neo4jPreparedStatement` that allows you to use named parameters proper:

```java
import module java.sql;
import module org.neo4j.jdbc;

void main() throws SQLException {

    try (
        var connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/movies?enableSQLTranslation=true", "neo4j", "verysecret");
        var stmt = connection.prepareStatement("""
            INSERT INTO Movie (title, released) VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """))
    {
        stmt.setString(1, "Dune: Part Two");
        stmt.setInt(2, 2024);
        stmt.addBatch();

        stmt.executeBatch();
    }

    try (
        var connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/movies", "neo4j", "verysecret");
        var stmt = connection.prepareStatement("""
            MATCH (n:$($label))
            WHERE n.released = $year
            RETURN DISTINCT n.title AS title""")
            .unwrap(Neo4jPreparedStatement.class))
    {
        stmt.setString("label", "Movie");
        stmt.setInt("year", 2024);

        var rs = stmt.executeQuery();
        while (rs.next()) {
            var title = rs.getString("title");
            System.out.println(title);
        }
    }
}
```

As we importing the whole Neo4j JDBC Driver module at the top of the class, we must explicitly add it to the module path. Run the program as follows, so that you don't have to define a `module-info.java` for that anonymous class:

```bash
java --enable-preview --module-path neo4j-jdbc-full-bundle-6.7.0.jar --add-modules org.neo4j.jdbc Demo5.java
```

### Cypher backed views

Your ETL tool just let you run plain selects but you do want to have some more complex Cypher? Don't worry, defined a Cypher-backed view like in demo 2 and we got you covered:

```java
import module java.sql;

void main() throws SQLException, IOException {

    // We do write this definition from the demo to a file, in reality it can be a
    // module- or classpath resource, a file or a resource on a webserver.
    var viewDefinition = """
        [
          {
            "name": "movies",
            "query": "MATCH (m:Movie)-[:HAS]->(g:Genre) RETURN m, collect(g.name) AS genres",
            "columns": [
              {
                "name": "title",
                "propertyName": "m.title",
                "type": "STRING"
              },
              {
                "name": "genres",
                "type": "ANY"
              }
            ]
          }
        ]""";
    var views = Files.createTempFile("views", ".json");
    Files.writeString(views, viewDefinition);

    var url = "jdbc:neo4j://localhost:7687/movies?enableSQLTranslation=%s&viewDefinitions=%s"
        .formatted(true, "file://" + views.toAbsolutePath());
    try (
        var connection = DriverManager.getConnection(url, "neo4j", "verysecret");
        var stmt = connection.createStatement();
        var rs = stmt.executeQuery("SELECT * FROM movies"))
    {
        while (rs.next()) {
            var title = rs.getString("title");
            var genres = rs.getObject("genres", List.class);
            System.out.println(title + " " + genres);
        }
    }
}
```

Running

```bash
java --enable-preview --module-path neo4j-jdbc-full-bundle-6.7.0.jar Demo2.java
```

gives you

```
Dune [Science Fiction]
Star Trek Generations [Science Fiction]
Seven [Horror]
```

### Support for the Neo4j HTTP Query API

Communication with Neo4j is usually done via the Bolt protocol, which is a binary format, running on a dedicated port. That can be problematic at times. Latest Neo4j server 2025.06 and higher have enabled the [Query API](https://neo4j.com/docs/query-api/current/), that allows Cypher over HTTP. The Neo4j JDBC driver can utilise that protocol, too, by just changing the URL like this:

```java
import module java.sql;

void main() throws SQLException {

    // Can't use Neo4j binary protocol, let's use http…
    var url = "jdbc:neo4j:http://localhost:7474/movies?enableSQLTranslation=true";
    try (
        var connection = DriverManager.getConnection(url, "neo4j", "verysecret");
        var stmt = connection.createStatement();
        var rs = stmt.executeQuery("SELECT * FROM Genre ORDER BY name"))
    {
        while (rs.next()) {
            var genre = rs.getString("name");
            System.out.println(genre);
        }
    }

    // This issue will be fixed in one of the next patch releases
    System.exit(0);
}
```

Everything else stays the same, the output of the query is

```
Horror
Science Fiction
```

### Retrieving Graph data as JSON objects

Last but not least, we did add some Object mapping to the driver. This is the only feature that requires an additional dependency. As we decided to go with an intermediate format, JSON, we are using [Jackson Databind](https://github.com/FasterXML/jackson-databind). With Jackson on the path, you can turn any Neo4j result into JSON by just asking the `ResultSet` for a `JsonNode`. The latter can than be converted to any Pojo or collection you desire.

You need [JBang](https://www.jbang.dev) to run the last demo. It does the dependency management for us when you run `jbang Demo4.java`

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 24
//PREVIEW
//DEPS org.neo4j:neo4j-jdbc:6.7.0
//DEPS com.fasterxml.jackson.core:jackson-databind:2.19.1

import java.sql.DriverManager;
import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

void main() throws SQLException, JsonProcessingException {

    var objectMapper = new ObjectMapper();

    record Movie(String title, int released, List<String> genres) {
    }

    try (
        var connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/movies", "neo4j", "verysecret");
        var stmt = connection.createStatement();
        var rs = stmt.executeQuery("""
            MATCH (m:Movie)-[:HAS]->(g:Genre)
            WITH m, collect(g.name) AS genres
            RETURN m{.*, genres: genres}
            """)) {
        while (rs.next()) {
            // First let's get a JSON object from the Cypher map
            var json = rs.getObject(1, JsonNode.class);
            // Turn it into domain objects
            var movie = objectMapper.treeToValue(json, Movie.class);
            System.out.println(movie);
        }
    }
}
```

The output now looks like this (with the dataset created in `Demo1.java`):

```
Movie[title=Dune, released=2021, genres=[Science Fiction]]
Movie[title=Star Trek Generations, released=1994, genres=[Science Fiction]]
Movie[title=Seven, released=1995, genres=[Horror]]
```

The Neo4j JDBC driver is a collective effort of @gjmwoods who did most of the work on the Query API server sides, @injectives and @meistermeier who implemented the client side parts of it in the Bolt connection module and @michael-simons who joined all of this together in the 6.7.0 release of the Neo4j JDBC driver.

## 🚀 Features
- 9300fd6 feat: Upgrade bolt-connection to 6.0.2 and add support for JDBC over HTTP. (#979)
- cf13056 feat(translator): Add support for `CONCAT`. (#1023)
- 8ad7285 feat: Allow the retrieval of graph data as `JsonNode` objects. (#1022)

## 🔄️ Refactorings
- e969fcb refactor: Remove superflous module declaration.
- 82eb600 refactor: Reduce log level of global metrics collector.
- 9b58975 refactor: Limit maximum bolt version to 5.8.

## 📝 Documentation
- 5d052d9 docs: Add database name to example urls.

## 🧹 Housekeeping
- 0e760bc build(deps-dev): Bump com.github.dasniko:testcontainers-keycloak (#1031)
- 7250aaf Bump org.hibernate.orm:hibernate-platform (#1030)
- 41edea0 Bump org.apache.maven.plugins:maven-enforcer-plugin (#1029)
- d692605 Bump quarkus.platform.version from 3.24.1 to 3.24.2 (#1026)
- 3695bef Bump org.jreleaser:jreleaser-maven-plugin (#1025)
- 6989d9f Bump org.keycloak:keycloak-authz-client (#1024)


# 6.6.1

## 🐛 Bug Fixes
- 99e02e5 fix: Allow coercion of map, list and points to string. (#1021)

## 🧹 Housekeeping
- 4e2a668 Bump org.openapitools:openapi-generator-maven-plugin (#1020)
- 2120b6d Bump com.puppycrawl.tools:checkstyle (#1019)
- 176f83d Bump org.testcontainers:testcontainers-bom (#1018)
- cee342a Bump org.hibernate.orm:hibernate-platform (#1017)
- 1b611d6 Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#1016)
- cca2982 Bump quarkus.platform.version from 3.23.4 to 3.24.1 (#1015)
- ecd7c97 build: Bump number of retries for state transition during release by a magnitude.


# 6.6.0

This release contains significant new features such as allowing token based authentication to be refreshed, an SPI to role token based authentication with any provider via standard JDBC properties and more. Additionally we improved the callable statement support quite a bit, although it is probably not that widely used (yet).

Last but not least, the database metadata is now accurate wrt the return columns of procedures and functions and includes them in the corresponding methods.

## 🚀 Features
- f6708c2 feat: Provide an authentication SPI and allow loading additional authentication suppliers via factories.
- e09a250 feat: Add support for refreshing expiring token based authentication.

## 🐛 Bug Fixes
- 3a620b3 fix: Align callable statement with JDBC spec wrt getting parameters back. (#1010)

## 🔄️ Refactorings
- 88ec761 refactor: Include function and procedure return columns in metadata and add support for parameter metadata in callable statements. (#1012)
- 466be62 refactor: Simplify `Lazy<>`.
- 47e4193 refactor: SPIs must be exported from the bundles.
- 94a196c refactor: Improve metrics collection.

## 📝 Documentation
- 2d6bb9b docs: Update local changelog.

## 🧹 Housekeeping
- ada99ed Bump org.neo4j:neo4j-cypher-dsl-bom to 2024.7.1
- 8940670 Bump org.testcontainers:testcontainers-bom (#1003)
- b5bdcdb Bump org.hibernate.orm:hibernate-platform (#997)
- f975800 Bump quarkus.platform.version from 3.23.3 to 3.23.4 (#1004)
- 117dbd6 Bump spring-boot.version from 3.5.0 to 3.5.3 (#1002)
- 86d120f Bump com.opencsv:opencsv from 5.11.1 to 5.11.2 (#1001)
- 6181140 Bump org.codehaus.mojo:flatten-maven-plugin (#1000)
- b73d7f8 Bump dev.langchain4j:langchain4j-bom from 1.0.1 to 1.1.0 (#999)
- e20b897 Bump com.puppycrawl.tools:checkstyle (#1005)
- 7015668 Bump spring-javaformat.version from 0.0.46 to 0.0.47 (#998)
- 003cedf Bump io.micrometer:micrometer-tracing-bom (#993)
- 2b948a5 Bump org.jooq:jooq from 3.19.23 to 3.19.24 (#987)
- 827309f Bump org.hibernate.orm:hibernate-platform (#988)
- 521c6af Bump quarkus.platform.version from 3.23.2 to 3.23.3 (#990)
- a76bb30 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.7 to 5.26.8 (#991)
- 87ce824 Bump com.fasterxml.jackson:jackson-bom (#992)
- a14d8da Bump org.jdbi:jdbi3-bom from 3.49.4 to 3.49.5 (#994)
- ac13f49 Bump io.micrometer:micrometer-bom from 1.15.0 to 1.15.1 (#995)

## 🛠 Build
- 8cb67d8 build(test): Retrieve host from the KEYCLOAK container instead of defaulting to `localhost`.
- 7fab110 build(docs): Updated antora dependencies.


# 6.5.1

## 🚀 Features
- ed9a104 feat(translator): Add support for `TOP n` limited queries.
- 52351f8 feat(translator): Add support for date extraction.

## 🔄️ Refactorings
- 22de0b3 refactor: Make sure the driver works fine with Cypher 25 types. (#985)

## 📝 Documentation
- e50ad26 docs: Include Cypher-backed views scheme fully in docs.

## 🧹 Housekeeping
- 6f5d4b2 Bump org.codehaus.mojo:build-helper-maven-plugin (#984)
- 98ae293 Bump spring-javaformat.version from 0.0.45 to 0.0.46 (#983)
- 1c34907 Bump quarkus.platform.version from 3.23.0 to 3.23.2 (#980)
- 878f24d Bump org.neo4j:cypher-v5-antlr-parser from 5.26.6 to 5.26.7 (#977)
- acb0f7e Bump com.puppycrawl.tools:checkstyle (#976)
- a1d2696 Bump com.opencsv:opencsv from 5.11 to 5.11.1 (#975)
- f6a5a17 Bump org.testcontainers:testcontainers-bom (#974)
- e29416b Bump org.codehaus.mojo:exec-maven-plugin (#973)
- f6bd72d Bump org.neo4j:neo4j-cypher-dsl-bom (#972)
- a8f5d3b Bump quarkus.platform.version from 3.22.3 to 3.23.0 (#970)
- b0f5eab Bump spring-boot.version from 3.4.5 to 3.5.0 (#968)
- 7eb8233 Bump org.mockito:mockito-bom from 5.17.0 to 5.18.0 (#967)
- 03ab15d Bump org.hibernate.orm:hibernate-platform (#966)
- 31bbb62 Bump dev.langchain4j:langchain4j-bom from 1.0.0 to 1.0.1 (#965)
- 6bbd59f Bump io.github.git-commit-id:git-commit-id-maven-plugin (#964)
- f0508ef Bump spring-javaformat.version from 0.0.44 to 0.0.45 (#963)
- fbf3d88 Bump com.puppycrawl.tools:checkstyle (#962)

## 🛠 Build
- 68e3e32 build: Propagate system properties starting with `NEOJ4_` to the test container image.
- b10c35b build: Allow inline return java docs.
- 3f600c7 build: Add more jOOQ classes to build time initialization. (#961)
- 0269ebc build: Make sure included single resources work in the AsciiDoc build.
- d96af78 build: Shade JDK specific Jackson classes proper.


# 6.5.0

This is a feature release, introducing ["Cypher-backed views" ](https://neo4j.com/docs/jdbc-manual/current/cypher_backed_views/). Cypher-backed views will help you teaching your tools all the capabilities of Cypher without those tools leaving the relational world. Please have a look at the documentation linked above.

In this release we also took some time and polish up the code base a bit, addressing some technical debt, resulting in a quadruple A-rating at the Neo4j SonarQube instance with regards to security, reliability, maintainability and security hotspots.

If you have feedbacks, issues, bugs: Please reach out on the [issue tracker](https://github.com/neo4j/neo4j-jdbc/issues).

## 🚀 Features
- 90900b1 feat: Support Cypher-backed views. (#946)
- 8f7be33 feat: Provide a dedicated logger for processed SQL.

## 🐛 Bug Fixes
- e7c91fc fix: Rewriting of parameter placeholders for prepared statements.

## 🔄️ Refactorings
- 8f69a6b refactor: Don’t default password to `password`
- ffc010f refactor: Correct typographic error.
- 9d0084e refactor: Use dedicated method for querying apoc availibility.

## 📝 Documentation
- 5b3d038 docs: Polish named parameters example. (#959)
- 23987e7 docs: Document Cypher-backed views in README.
- f644fef docs: Update local changelog.
- 0609ce4 docs: Fix copyright year in Antora config.
- e275046 docs: Fix typographic error in CONTRIBUTING. (#939)

## 🧹 Housekeeping
- 6666f0f Bump com.puppycrawl.tools:checkstyle from 10.21.4 to 10.23.1 (#928)
- 4d809ce Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#954)
- 71cc907 Bump io.micrometer:micrometer-bom from 1.14.6 to 1.15.0 (#953)
- edd4dc5 Bump com.fasterxml.jackson.jr:jackson-jr-objects (#956)
- 2ec5579 Bump quarkus.platform.version from 3.22.2 to 3.22.3 (#955)
- ebaad65 Bump org.jdbi:jdbi3-bom from 3.49.3 to 3.49.4 (#952)
- 90ca425 Bump dev.langchain4j:langchain4j-bom from 0.36.2 to 1.0.0 (#951)
- aaed5a7 Bump io.micrometer:micrometer-tracing-bom (#950)
- 10c33f7 Bump spring-javaformat.version from 0.0.43 to 0.0.44 (#949)
- 970f3f9 Bump org.hibernate.orm:hibernate-platform (#948)
- 2d5c565 Bump org.neo4j:neo4j-cypher-dsl-bom to 2024.6.1
- 9e07ea7 Bump org.neo4j:neo4j-cypher-dsl-bom (#945)
- 93c8012 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.5 to 5.26.6 (#944)
- 5027104 build(deps-dev): Bump com.tngtech.archunit:archunit from 1.4.0 to 1.4.1 (#943)
- 66528b5 Bump quarkus.platform.version from 3.22.1 to 3.22.2 (#942)
- c59da14 Update neo4j-bolt-connection to 3.0.0 (#940)

## 🛠 Build
- ccdd229 build: Incorporate Spring Boot smoke tests into test results.
- bb02ce6 build: Make sure integration tests are observed by Sonar. (#957)
- 1faf4fc build: Integrate deploy configuration for JReleaser with Maven and document all necessary deploy steps.
- 12ede47 build: Exclude the text2cypher PoC / experimental module from code coverage.


# 6.4.1

This release does neither include changes nor dependency updates, but addresses the issue of the missing SBOMs in Maven central:

They are now available and look like this (for the core module): [neo4j-jdbc-6.4.1-sbom-cyclonedx.json](https://repo.maven.apache.org/maven2/org/neo4j/neo4j-jdbc/6.4.1/neo4j-jdbc-6.4.1-sbom-cyclonedx.json).


# 6.4.0

(Note: Publishing SBOMs failed, but they can be created and will be in the next release)

## 🚀 Features
- 38dae9f feat: Provide access to GQL Status objects.
- b72778b feat: Propagate GQL error codes as SQL state. (#932)
- d631b45 feat: Generate and attach CycloneDX SBOMs for all relevant artifacts. (#931)
- bbd7f29 feat: Add support for `java.sql.Array`.

## 🐛 Bug Fixes
- 47032b6 fix: Neo4j FLOAT is a SQL Double, INTEGER is BIGINT.

## 🔄️ Refactorings
- d2f03d9 refactor: Polish some JavaDocs, deprecate methods and fields that should never have been public.

## 🧹 Housekeeping
- b0011ef Bump org.jooq:jooq from 3.19.22 to 3.19.23 (#936)
- 8735bbd Bump com.opencsv:opencsv from 5.10 to 5.11 (#938)
- 7b977c1 Bump org.jreleaser:jreleaser-maven-plugin (#937)
- bbd328f Bump quarkus.platform.version from 3.21.4 to 3.22.1 (#935)
- ba830c7 Bump org.jdbi:jdbi3-bom from 3.49.2 to 3.49.3 (#934)
- 104261a Bump org.openapitools:openapi-generator-maven-plugin (#929)
- a5e13b5 Bump org.jdbi:jdbi3-bom from 3.49.0 to 3.49.2 (#927)
- dad329b Bump org.testcontainers:testcontainers-bom (#926)
- dc44d00 Bump quarkus.platform.version from 3.21.2 to 3.21.4 (#925)
- 35d5103 Bump spring-boot.version from 3.4.4 to 3.4.5 (#924)
- 2b4d929 Bump io.micrometer:micrometer-tracing-bom (#922)
- 28d0206 Bump org.neo4j.bolt:neo4j-bolt-connection-bom (#930)


# 6.3.1

## 🐛 Bug Fixes
- 57c0045 fix: Remove superflous config in test.
- 8648243 fix: typo in docs (Dashß -> Dash0) (#911)

## 🔄️ Refactorings
- fe529de refactor: Avoid logging about the available more than once in the same class loader.

## 🧹 Housekeeping
- 5b3041a Bump org.junit:junit-bom from 5.12.1 to 5.12.2 (#919)
- 3233cf2 Bump org.mockito:mockito-bom from 5.16.1 to 5.17.0 (#916)
- 80d3199 Bump io.fabric8:docker-maven-plugin from 0.45.1 to 0.46.0 (#918)
- 3ccbf62 Bump quarkus.platform.version from 3.21.0 to 3.21.2 (#917)
- 1101e4b Bump io.micrometer:micrometer-bom from 1.14.5 to 1.14.6 (#915)

## 🛠 Build
- 4a64753 build: Use specific commits for none enterprise actions. (#920)
- c7cc502 build: Test against Java 24. (#912)


# 6.3.0

This is big feature release including support for both Metrics and Tracing.
Metrics will be enabled automatically if you use the Neo4j JDBC Driver within an application that has [Micrometer](https://micrometer.io) on the class path. 

Tracing can be enabled via the `Neo4jDataSource` and putting Micrometer Tracing on the class path and choosing a registry and reporter that fits your needs. We tested both the Zipkin Brave registry with Zipkin, and the [OpenTelemetry](https://opentelemetry.io) registry together with the Otel sender against Dash0.

Please read our docs how to configure this: [Configuring tracing for the JDBC driver](https://neo4j.com/docs/jdbc-manual/current/configuration/#_using_tracing_with_spring_boot).

## 🚀 Features
- 2025829 feat: Add support for tracing. (#904)
- 74466d8 feat: Allow the full URL to be configured on the datasource.
- e00bdae feat: Add events and metrics to the driver. (#895)

## 🧹 Housekeeping
- 6c71310 Bump org.jooq:jooq from 3.19.21 to 3.19.22 (#909)
- 07bc0c4 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.4 to 5.26.5 (#908)
- 87ab869 Bump org.jacoco:jacoco-maven-plugin from 0.8.12 to 0.8.13 (#907)
- 6c99932 Bump org.hibernate.orm:hibernate-platform (#906)
- 3a74dce Bump org.sonarsource.scanner.maven:sonar-maven-plugin (#902)
- 21ebae1 Bump org.apache.maven.plugins:maven-failsafe-plugin (#901)
- 9da5b81 Bump org.jdbi:jdbi3-bom from 3.48.0 to 3.49.0 (#900)
- 608578b Bump org.apache.maven.plugins:maven-surefire-plugin (#899)
- 2ee1c4e Bump quarkus.platform.version from 3.19.4 to 3.21.0 (#898)


# 6.2.1

## 🐛 Bug Fixes
- 99b78bf fix: SQL processing attempt must be made before transaction acquisition.

## 🔄️ Refactorings
- 86a67c5 refactor: Make sure all bolt connections are closed during abort, too.
- e20469e refactor: Restrict the output of `dbms.components()` to `'Neo4j Kernel'`.
- 26dc5e3 refactor: Clear out bolt-connection for database metadata on close.

## 🧹 Housekeeping
- 859c255 Bump org.neo4j.bolt:neo4j-bolt-connection-bom to 1.1.1
- 780b89a Bump org.neo4j:neo4j-cypher-dsl-bom (#894)
- c0e5e2f Bump quarkus.platform.version from 3.19.3 to 3.19.4 (#893)
- b9afe9e Bump org.asciidoctor:asciidoctor-maven-plugin (#892)
- 1f0c1d1 Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#891)
- e8669cf Bump org.neo4j.bolt:neo4j-bolt-connection-bom (#890)
- 39ec25d Bump spring-boot.version from 3.4.3 to 3.4.4 (#889)
- 209538f Bump com.mycila:license-maven-plugin from 4.6 to 5.0.0 (#888)
- 221654b Bump org.junit:junit-bom from 5.12.0 to 5.12.1 (#887)
- 0758b9c Bump org.mockito:mockito-bom from 5.16.0 to 5.16.1 (#886)
- b4b6398 Bump quarkus.platform.version from 3.19.2 to 3.19.3 (#885)
- 1c43629 Bump org.graalvm.buildtools:native-maven-plugin (#884)
- 9209dbf Bump org.hibernate.orm:hibernate-platform (#883)
- 3e650b7 Bump org.jooq:jooq from 3.19.20 to 3.19.21 (#882)
- 85b8318 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.3 to 5.26.4 (#881)

## 🛠 Build
- c1beca4 build: Add a `Lazy`-test.
- b5aadb5 test: Use `Neo4jContainer.getHost()` in ITs instead of `localhost` (#880)
- 629e287 build: Update license-maven-plugin configuration to include full path to `license.tpl`. (#878)
- 410513d Update exec-maven-plugin configuration to include full path (#879)


# 6.2.0

This is quite a big release, as from 6.2.0 onwards both the Neo4j JDBC Driver and the Neo4j Java driver will use the exact same network stack, so both drivers will equally benefit from enhancements there.

> [!TIP]
> This minor release bump is fully API compatible with 6.1.5, we bumped the version for the following reasons:
> 
> The SQL Translator now aliases projected column in such a way that they can be retrieved like they have been originally project:
> `SELECT title FROM Movie` will now be translated into `MATCH (movie:Movie) RETURN movie.title AS title`
> This is in line with the asterisk projection, too, but only available if there's only one relation in the FROM clause.
> 
> The `ResultSet` implementation is more lenient when being asked for `String` values and follows the JDBC spec that 
> will just use the Java representation of a non-string projection.
> 
> The `Connection` implementation is more lenient on commit and rollback, so that these operations are idempotent in case 
> there is no ongoing transaction or a transaction that is either already committed or rolled back.

## 🐛 Bug Fixes
- 27b8c76 fix(build): Reenable semver check. (#876)

## 🔄️ Refactorings
- b1db4b2 refactor: Try to convert strings to integers and floats if possible.
- c7ee637 refactor: Try to use Java `toString` representation on non-string results when using `getString()`.
- e06311f refactor(translator): Use `AS` to give results same name if there’s a single source table and no qualified names are expected.
- 0d27cd4 refactor(metadata): Use separate (bolt) connection for metadata.
- c9bb99c refactor(metadata): Improve transaction handling in metadata. (#877)
- 0306bf1 refactor: Replace custom bolt stack with shared bolt connection module. (#832)

## 📝 Documentation
- 27b6734 docs: Update local changelog.

## 🧹 Housekeeping
- 60b9604 Bump org.testcontainers:testcontainers-bom (#875)
- a1fa305 Bump quarkus.platform.version from 3.19.1 to 3.19.2 (#874)
- 9da051b Bump org.mockito:mockito-bom from 5.15.2 to 5.16.0 (#873)
- e00d760 Bump org.apache.maven.plugins:maven-install-plugin (#872)
- 5bb8953 Bump org.hibernate.orm:hibernate-platform (#871)
- 7842b2a Bump com.puppycrawl.tools:checkstyle (#870)


# 6.1.5

## 🐛 Bug Fixes
- c36ae21 fix(build): Add another way to spell MIT license to the license check.
- bc5e0de fix: Make driver more resilient agains already existing attribute keys for Netty channels. (#869)

## 🔄️ Refactorings
- 0e1882e refactor: Support row movement as much as possible in the `ResultSetImpl` and throw the appropriate exception otherwise.
- 0de6261 refactor: Trace usage of all result set interface methods.
- d3184c9 refactor: Trace usage of all callable statement interface methods.
- 9357d4e refactor: Trace usage of all prepared statement interface methods.
- f7e05d7 refactor: Trace usage of all statement interface methods.
- 0c9743a refactor: Trace usage of all connection interface methods.

## 📝 Documentation
- ffb06b2 docs: Add issue template.
- db3e3f0 docs: Document logging configuration.
- cdd1351 docs: Point GitHub pages always to the offical Neo4j manual.

## 🧹 Housekeeping
- 8aaaeb3 Bump quarkus.platform.version from 3.18.4 to 3.19.1 (#862)
- f7aca53 Bump org.codehaus.mojo:flatten-maven-plugin (#868)
- 4a1c289 Bump org.neo4j:cypher-v5-antlr-parser from 5.26.2 to 5.26.3 (#867)
- f9cfe1b Bump org.apache.maven.plugins:maven-deploy-plugin (#865)
- 013e29e Bump org.neo4j:neo4j-cypher-dsl-bom (#864)
- 050c220 Bump slf4j.version from 2.0.16 to 2.0.17 (#863)
- 16ea18e Bump org.openapitools:openapi-generator-maven-plugin (#861)
- 48e0863 Bump org.jreleaser:jreleaser-maven-plugin (#860)
- 9b4842e Bump io.netty:netty-bom from 4.1.118.Final to 4.1.119.Final (#859)


# 6.1.4

## 🐛 Bug Fixes
- e9d87c2 fix: Remove tests for unsupported features that are now supported.

## 🔄️ Refactorings
- 1daa556 refactor(metadata): More work on adding support for JDBC / SQL features.
- 2678d37 refactor: Replace several more unnessary usages of `SQLFeatureNotSupportedException`.

## 🧹 Housekeeping
- 3df0055 Bump org.jooq:jooq from 3.19.18 to 3.19.20 (#858)
- 70ff7b0 Bump spring-boot.version from 3.4.2 to 3.4.3 (#857)
- 6e89852 Bump quarkus.platform.version from 3.18.3 to 3.18.4 (#856)
- f068a19 Bump org.apache.maven.plugins:maven-compiler-plugin (#855)
- ce60c12 Bump org.hibernate.orm:hibernate-platform (#854)
- ff850fc Bump com.puppycrawl.tools:checkstyle (#853)
- d7df38d Bump io.github.cdimascio:dotenv-java from 3.1.0 to 3.2.0 (#852)
- 745c67b Bump org.junit:junit-bom from 5.11.4 to 5.12.0 (#851)
- edbca02 Bump org.testcontainers:testcontainers-bom (#850)


# 6.1.3

## 🚀 Features
- a42da59 feat(metadata): Cache the result of `getTables` per metadata instance as it is used during columns computations, too.
- 2de971b feat(metadata): Use `apoc.meta.schema` if available for computing tables and relationships.
- 404bd0f feat(metadata): Make sampling size for relationships configurable.

## 🐛 Bug Fixes
- 93abb1a fix(test): Disable reflection based test in native image. (#844)
- 3bb7ab7 fix(build): The default of `structuredMessage` is `false`.

## 🔄️ Refactorings
- de2b3d3 refactor(meta): Sanitize sampling size and ensure it works for both apoc and Cypher solutions.

## 🧹 Housekeeping
- 080ce04 Bump quarkus.platform.version from 3.18.2 to 3.18.3 (#849)
- 14ee32f Bump org.hibernate.orm:hibernate-platform (#848)
- c0ccfad Bump org.neo4j:neo4j-cypher-dsl-bom (#847)
- 08c1b3d build(deps-dev): Bump com.tngtech.archunit:archunit from 1.3.0 to 1.4.0 (#846)
- d7aefaf Bump org.jdbi:jdbi3-bom from 3.47.0 to 3.48.0 (#845)

## 🛠 Build
- 3a03ce1 build: Use default values for announcer.
- 34e7cad build: Exclude shaded dependencies from semver check.
- b8523fb build: Enable announcer on release only.


# 6.1.2

## 🚀 Features
- ce3e888 feat: Avoid throwing any `SQLFeatureNotSupportedException` from `DatabaseMetaData`.
- dacb98f feat: Add support for `isReadOnly` in database metadata.
- bbcb487 feat: Add support for `getURL` in database metadata.
- d42f893 feat: Provide proper schema and catalog names from within `ResultSetMetaData`.
- b0416d8 feat(translator): Provide a module containing a translator aiming specifically at Spark generated queries.

## 🔄️ Refactorings
- 69ee1a3 refactor: Use constants for metadata column names.
- 2fc53c4 refactor: Address some overly complex methods. (#843)
- b1e3435 refactor(translator): Optimize `LIKE` queries to not always use `=~`.
- 0a691b2 refactor: Allow translator factories to return `null` to indicate that no translator has been created.

## 🧹 Housekeeping
- b8bc487 Bump io.netty:netty-bom from 4.1.117.Final to 4.1.118.Final
- 175b19d Bump quarkus.platform.version from 3.18.1 to 3.18.2 (#842)
- 7b4189f Bump org.graalvm.buildtools:native-maven-plugin (#841)
- 7112adb Bump org.neo4j:cypher-v5-antlr-parser from 5.26.1 to 5.26.2 (#840)
- f39448b Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#839)
- 5c75faa Bump org.hibernate.orm:hibernate-platform (#838)
- 896f02f Bump quarkus.platform.version from 3.17.8 to 3.18.1 (#837)
- 32051be Bump com.puppycrawl.tools:checkstyle (#836)
- a6b6686 Bump quarkus.platform.version from 3.17.7 to 3.17.8 (#835)
- 1480297 Bump org.openapitools:openapi-generator-maven-plugin (#834)
- bf9b12b Bump spring-boot.version from 3.4.1 to 3.4.2 (#833)
- 03bc391 Bump org.hibernate.orm:hibernate-platform (#831)
- 5fc86b8 Bump quarkus.platform.version from 3.17.6 to 3.17.7 (#830)
- 1d7b0d7 Bump org.assertj:assertj-core from 3.27.2 to 3.27.3 (#828)
- ac32b32 Bump io.netty:netty-bom from 4.1.116.Final to 4.1.117.Final (#827)
- 789a95f Bump org.jooq:jooq from 3.19.17 to 3.19.18 (#829)
- 96e0910 Bump cookie and express in /etc/antora (#825)

## 🛠 Build
- ede26b1 build: Add release announcements.


# 6.1.1

## 🚀 Features
- e98cab4 feat: Indicate that all open result sets will be closed when autocommit fails. [metadata]
- 791e4f2 feat: Add `app` string to transactional metadata and unify with BoltAgent.
- f1baf87 feat: Indicate in metadata which client info properties are supported. [metadata]
- 9d179b1 feat: Add support for `getTypeInfo`. [metadata]
- d6b2183 feat: Show all catalogs. [metadata]
- 34bbb15 feat: In `LIKE` also translate `_` to `.`. [translator].
- 8b771b9 feat: Derive primary keys from unique constraints.

## 🐛 Bug Fixes
- 46ef2c5 fix: Don’t swallow exception and don’t overwrite previous valid translation when a later translator fails.
- d4ee5f1 fix: Allow matching on multiple patterns, check target nodes in relationships. [translator] (#819)
- 245cd93 fix: Transaction must not jump back from committed to ready.
- 7e02077 fix: Use correct target for elementId. [translator]

## 🔄️ Refactorings
- b3e3a73 refactor: Still create a relationship pattern despite an expected empty result [translator]
- 77e52f4 refactor: Align both overloads of `getSchemas` to support querying the schema and returning the current catalog.
- 895d416 refactor: Rework catalog support.

## 📝 Documentation
- 6e799ab docs: Add screenshot from 6.1.0 release notes.

## 🧰 Tasks
- 1c1562b chore: Extend license header to 2025.

## 🧹 Housekeeping
- 01b9528 Bump quarkus.platform.version from 3.17.5 to 3.17.6 (#824)
- 9160d22 Bump org.jooq:jooq from 3.19.16 to 3.19.17 (#823)
- d6abf3e Bump com.github.siom79.japicmp:japicmp-maven-plugin (#822)
- 805f8bf Bump com.opencsv:opencsv from 5.9 to 5.10 (#821)
- fcfb7d2 Bump org.hibernate.orm:hibernate-platform (#808)
- 89169c7 Bump quarkus.platform.version from 3.17.4 to 3.17.5 (#809)
- 2c1f2e6 Bump org.mockito:mockito-bom from 5.14.2 to 5.15.2 (#816)
- 6cb0502 Bump org.jreleaser:jreleaser-maven-plugin (#817)
- 76692f2 Bump org.assertj:assertj-core from 3.26.3 to 3.27.2 (#818)
- 8f758aa Bump spring-boot.version from 3.4.0 to 3.4.1 (#812)
- d1e2c56 Bump io.netty:netty-bom from 4.1.115.Final to 4.1.116.Final (#810)
- cd90d65 Bump org.neo4j:neo4j-cypher-dsl-bom (#813)
- 202c80e Bump com.puppycrawl.tools:checkstyle (#815)
- 0220639 Bump org.junit:junit-bom from 5.11.3 to 5.11.4 (#807)
- c8c4f1b Bump org.jooq:jooq from 3.19.15 to 3.19.16 (#806)
- fea2623 Bump com.puppycrawl.tools:checkstyle (#805)
- 0d263f0 Bump io.github.cdimascio:dotenv-java from 3.0.2 to 3.1.0 (#804)
- fc18b14 Bump quarkus.platform.version from 3.17.3 to 3.17.4 (#803)
- 41abb65 Bump neo4j-cypher-dsl.version from 2024.3.1 to 2024.3.2

## 🛠 Build
- d53805b build: Allow the swagger generated stuff to compile with JDK23.


# 6.1.0

> [!TIP]
> This minor release bump is fully API compatible with 6.0.1, we bumped the version for two reasons:
> The virtual column `element_id` has been renamed to `v$id` and consistently shows now in the database metadata as "generated column". The naming has been chosen on purpose to avoid any clash with nodes that have an actual property of that name (No, the `elementId()` of a node is a function call, just like `id()` was, it is not a property).
> The second reason is for the fix in behaviour of `executeUpdate` which now returns the estimated number of updated rows instead of the cumulative updates in a statement. This is in align with the JDBC Spec.

The most exciting new feature of this release is the introduction of virtual tables for relationships. The metadata does now some sampling of the graph (Similar to what is done in the [schema introspection PoC](https://github.com/neo4j/graph-schema-introspector) or Apoc Meta) and lists relationships between labels as `label1_TYPE_label2` in the data dictionary, with a `v$id` column for the relationship, and `v$label1_id` and `v$label2_id` so that ETL tools can actual join those with the labels.

Those virtual tables are fully queryable, so that you can do a `SELECT * FROM Person_ACTED_IN_Movie`, which will return the element ids of the relationship and the start and end nodes and all relationship properties and they do partake of course proper in joins.

Using the standard `JOIN` syntax,  the driver will translate the query to a simple pattern match, so that

```sql
SELECT p."v$person_id", name, title, roles 
FROM Person p
JOIN Person_ACTED_IN_Movie pm ON pm.v$person_id = p.v$id
JOIN Movie m ON m.v$id = pm.v$movie_id 
WHERE title = 'The Matrix'
ORDER BY title, name
```

becomes

```cypher
MATCH (p:Person) -[r:ACTED_IN]->(m:Movie)
WHERE m.title = 'The Matrix'
RETURN elementId(p), p.name AS name, m.title AS title, r.roles AS roles
ORDER BY m.title, p.name
```

When you enumerate the tables in the `FROM` clause and join inside `WHERE`, the driver will match start and end nodes separately and then join on ids as well. We might change this in a future optimisation.

![image](https://github.com/user-attachments/assets/b1acf323-c090-44c7-b6c7-7e634fc3d026)

## 🚀 Features
- 1b7d520 feat: Implement left and right joins on virtual tables.
- f8ed74d feat: Add support for parsing array expressions [translator].

## 🐛 Bug Fixes
- fef8468 fix: Make `executeUpdate` return only the number of modified rows. (#780)

## 🔄️ Refactorings
- f3bd2de refactor: Improve metadata support.

## 📝 Documentation
- 5623382 docs: Store changelog with the repository.
- 52133da docs: Add example how to turn Node into Map.

## 🧹 Housekeeping
- 860c969 build: Bump version.
- 785a93a Bump org.apache.maven.plugins:maven-javadoc-plugin (#802)
- 3f22be0 Bump org.graalvm.buildtools:native-maven-plugin (#801)
- 57ca1b5 Bump quarkus.platform.version from 3.17.2 to 3.17.3 (#800)
- a782b29 Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#798)
- 98e50da Bump com.puppycrawl.tools:checkstyle (#797)
- bfc5e23 Bump quarkus.platform.version from 3.16.4 to 3.17.2 (#796)
- 4e03283 Bump org.testcontainers:testcontainers-bom (#795)
- 2696970 Bump org.openapitools:openapi-generator-maven-plugin (#794)
- e8fd41b Bump org.hibernate.orm:hibernate-platform (#793)
- 70a6c3c Bump dev.langchain4j:langchain4j-bom from 0.36.0 to 0.36.2 (#792)
- 8f551b7 Bump spring-boot.version from 3.3.5 to 3.4.0 (#791)
- 35da2f6 Bump quarkus.platform.version from 3.16.3 to 3.16.4 (#790)
- 8b5c5f1 Bump org.hibernate.orm:hibernate-platform (#789)
- 4239005 Bump dev.langchain4j:langchain4j-bom from 0.35.0 to 0.36.0 (#788)
- 528843d Bump org.asciidoctor:asciidoctor-maven-plugin (#787)
- 238de8b Bump quarkus.platform.version from 3.16.2 to 3.16.3 (#786)
- 7a72201 Bump io.netty:netty-bom from 4.1.114.Final to 4.1.115.Final (#785)
- 75d41a8 Bump org.jooq:jooq from 3.19.14 to 3.19.15 (#781)
- deb9e9b Bump com.puppycrawl.tools:checkstyle (#784)
- 7f24cb1 Bump quarkus.platform.version from 3.16.1 to 3.16.2 (#783)
- e882122 Bump org.sonarsource.scanner.maven:sonar-maven-plugin (#782)


# 6.0.1

## 🚀 Features
- 7729ebd feat: Add support for the remaining aggregate functions.
- 3e91638 feat: Make LLM in translator configurable. (#764)

## 🐛 Bug Fixes
- b63af1d fix: upgrade @neo4j-documentation/macros from 1.0.2 to 1.0.4 (#760)

## 🔄️ Refactorings
- 726b1ba refactor: Adapt to non-semver Quarkus changes in integration tests.
- 678fd94 refactor: Simplify property name access.

## 📝 Documentation
- 3d184d3 docs: Remove author revision and EAP attribute (#749)
- 76f8c83 docs: Remove EAP warning from readme.

## 🧹 Housekeeping
- 57ebb6c Bump quarkus.platform.version from 3.15.1 to 3.16.1 (#776)
- 56053ce Bump org.jreleaser:jreleaser-maven-plugin (#779)
- 4ab577e Bump org.apache.maven.plugins:maven-surefire-plugin (#778)
- 7f4ee1c Bump com.puppycrawl.tools:checkstyle (#777)
- b57612f Bump org.apache.maven.plugins:maven-failsafe-plugin (#775)
- eedbeb8 Bump org.apache.maven.plugins:maven-javadoc-plugin (#774)
- 0f86e18 Bump org.asciidoctor:asciidoctor-maven-plugin (#773)
- c7ca644 Bump spring-boot.version from 3.3.4 to 3.3.5 (#767)
- f6babcb Bump org.junit:junit-bom from 5.11.2 to 5.11.3 (#766)
- b3acaf6 Bump org.jdbi:jdbi3-bom from 3.46.0 to 3.47.0 (#765)
- 7c52bf1 Bump org.apache.maven.plugins:maven-checkstyle-plugin (#772)
- 4ed6fcd Bump com.puppycrawl.tools:checkstyle (#771)
- 9930ca4 Bump org.jooq:jooq from 3.19.13 to 3.19.14 (#770)
- 14b9b89 Bump org.codehaus.mojo:exec-maven-plugin (#769)
- a5f5fb8 Bump org.testcontainers:testcontainers-bom (#768)
- aed51f8 Bump org.jdbi:jdbi3-bom from 3.45.4 to 3.46.0 (#762)
- 77e7f62 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.18 to 2.3.19 (#763)
- fe18bc0 Bump org.mockito:mockito-bom from 5.14.1 to 5.14.2 (#761)
- d2c5b8a Bump org.openapitools:openapi-generator-maven-plugin (#758)
- e455ac8 Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#757)
- 23a06b6 Bump org.mockito:mockito-bom from 5.14.0 to 5.14.1 (#756)
- 5d3a54a Bump org.apache.maven.plugins:maven-surefire-plugin (#755)
- d36be3e Bump org.junit:junit-bom from 5.11.1 to 5.11.2 (#754)
- 04d7e8f Bump org.testcontainers:testcontainers-bom (#753)
- 2b1a76d Bump org.apache.maven.plugins:maven-javadoc-plugin (#752)
- 082dfad Bump io.netty:netty-bom from 4.1.113.Final to 4.1.114.Final (#751)
- 09a56ec Bump org.apache.maven.plugins:maven-failsafe-plugin (#750)

## 🛠 Build
- 46b1507 build: Check upstream dependencies during build at all times.


# 6.0.0

## 🐛 Bug Fixes
- 5432114 docs: Fix another broken link on the Readme.

## 🔄️ Refactorings
- 908bc6b refactor: Discard open Bolt results before closing transaction. (#748)

## 📝 Documentation
- 7f52d28 docs: Make README work on GitHub, too.
- 7155ea4 docs: Update copyright dates.

## 🧹 Housekeeping
- e112db1 Bump neo4j-cypher-dsl.version from 2024.1.0 to 2024.2.0

## 🛠 Build
- e28b5ec build: Test against latest public available Neo4j image.


# 6.0.0-RC1

Big thanks to my colleagues @stefano-ottolenghi and @recrwplay for working with me setting up the release of the official documentation (Preview available [here](https://development.neo4j.dev/docs/jdbc-manual/6.0.0-RC1/) until the final release goes out).

## 🐛 Bug Fixes
- 30b90dd docs: Fix link to manual.
- 086a92b fix: Use correct directory for preview.
- 0c5429b fix: Add missing license headers.

## 📝 Documentation
- c6aaf8c docs: Correct small typo in README (#747)
- eaa343f docs: Remove `Introduction` section from page with no other sections (#736)
- 7b5323c docs: Full review and polishing of the whole documentation. (#720)

## 🧹 Housekeeping
- 02f7584 Bump dev.langchain4j:langchain4j-bom from 0.34.0 to 0.35.0 (#746)
- a234aca Bump com.puppycrawl.tools:checkstyle (#745)
- bc35f69 Bump quarkus.platform.version from 3.14.4 to 3.15.1 (#744)
- 72ce687 Bump org.mockito:mockito-bom from 5.13.0 to 5.14.0 (#743)
- 35282fb Bump org.junit:junit-bom from 5.11.0 to 5.11.1 (#742)
- a51e82b Bump org.jooq:jooq from 3.19.11 to 3.19.13 (#741)
- acc4a3b Bump io.fabric8:docker-maven-plugin from 0.45.0 to 0.45.1 (#740)
- 55a3b6a Bump com.mycila:license-maven-plugin from 4.5 to 4.6 (#739)
- a0d25f4 Bump spring-boot.version from 3.3.3 to 3.3.4 (#737)
- 0af6df4 Bump org.neo4j:neo4j-cypher-dsl-bom (#735)
- aa91667 build(deps-dev): Bump org.asciidoctor:asciidoctorj from 2.5.13 to 3.0.0 (#734)
- c59f2eb Bump org.graalvm.buildtools:native-maven-plugin (#733)
- 6a50341 Bump quarkus.platform.version from 3.14.2 to 3.14.4 (#732)
- 80458f4 Bump org.apache.maven.plugins:maven-javadoc-plugin (#724)
- 6fcd872 Bump org.apache.maven.plugins:maven-failsafe-plugin (#725)
- d10d860 Bump io.github.cdimascio:dotenv-java from 3.0.1 to 3.0.2 (#731)
- 7ac4a91 Bump org.jreleaser:jreleaser-maven-plugin (#730)
- 113ce53 Bump org.mockito:mockito-bom from 5.12.0 to 5.13.0 (#729)
- 515bccb Bump dev.langchain4j:langchain4j-bom from 0.33.0 to 0.34.0 (#728)
- f74d019 Bump quarkus.platform.version from 3.13.3 to 3.14.2 (#727)
- 74b8478 Bump org.apache.maven.plugins:maven-surefire-plugin (#726)
- 1f39b1c Bump org.neo4j:neo4j-cypher-dsl-bom (#723)
- 1f59fcf Bump io.netty:netty-bom from 4.1.112.Final to 4.1.113.Final (#722)
- 0c02a8a Bump com.puppycrawl.tools:checkstyle (#719)
- 37b1cbf Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#718)
- 9a0284c Bump org.apache.maven.plugins:maven-checkstyle-plugin (#717)
- 0c18390 Bump com.github.siom79.japicmp:japicmp-maven-plugin (#716)
- 6c2b29f Bump org.apache.maven.plugins:maven-deploy-plugin (#714)
- abb6509 Bump org.openapitools:openapi-generator-maven-plugin (#713)
- 79c5dd3 Bump spring-boot.version from 3.3.2 to 3.3.3 (#712)
- 2268b27 Bump org.apache.maven.plugins:maven-install-plugin (#711)
- 54e2dc6 Bump org.jdbi:jdbi3-bom from 3.45.3 to 3.45.4 (#710)
- 44ae3a0 Bump quarkus.platform.version from 3.13.2 to 3.13.3 (#709)
- e96075a Bump org.apache.maven.plugins:maven-surefire-plugin (#708)
- 2940c7d Bump org.junit:junit-bom from 5.10.3 to 5.11.0 (#707)
- 35d67ca Bump spring-javaformat.version from 0.0.42 to 0.0.43 (#706)
- 9d47c18 Bump org.codehaus.mojo:exec-maven-plugin (#705)
- ea82c45 Bump org.apache.maven.plugins:maven-failsafe-plugin (#704)
- ccb9ae8 Bump org.jooq:jooq from 3.19.10 to 3.19.11 (#703)
- e56cf1f Bump quarkus.platform.version from 3.12.3 to 3.13.2 (#699)
- 4cf3faf Bump org.jdbi:jdbi3-bom from 3.45.2 to 3.45.3 (#695)
- 7fbd137 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.17 to 2.3.18 (#696)
- ce8a774 Bump org.testcontainers:testcontainers-bom (#697)
- 0ccef4b Bump org.codehaus.mojo:exec-maven-plugin (#700)
- 8dcea72 Bump slf4j.version from 2.0.13 to 2.0.16 (#701)
- 6c92a4d Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#702)
- 54347d4 Bump io.fabric8:docker-maven-plugin from 0.44.0 to 0.45.0 (#694)
- f3f18f9 Bump dev.langchain4j:langchain4j-bom from 0.32.0 to 0.33.0 (#693)

## 🛠 Build
- 726d166 build: Update checkout and setup-java actions to latest.
- 2702a1c build: Mute snyk for tests, testing infrastructure, and docs (#738)
- 4aa0146 build: Configure Neo4j Antora build proper.


# 6.0.0-M05

## 🚀 Features
- 752d31b feat: Add support for transactional metadata. (#686)
- 01cd4a9 feat: Support `DISTINCT` clause in `SELECT` statements. (#679)
- 5894779 feat: Allow chained translators to break out of chain with standard magic comment.
- 3ed8f81 feat: Make text2cypher orderable and use the term `precedence` everywhere.

## 🐛 Bug Fixes
- 94e0f95 fix: Adapt to changes in generated Swagger code.
- cf46497 fix: Enforce cypher for schema queries.

## 🔄️ Refactorings
- 3dfbd77 refactor: Log translation to `FINE`, not `FINEST`.
- e501054 refactor: Correctly quote logging placeholders and increase the log level for text2cypher translations.

## 📝 Documentation
- 6059b6b docs: Adapt to new env name.
- 46646f7 build: Document log settings.

## 🧹 Housekeeping
- 6d399dd Bump spring-boot.version from 3.3.1 to 3.3.2 (#687)
- 9131e42 Bump io.netty:netty-bom from 4.1.111.Final to 4.1.112.Final (#688)
- c79913b Bump com.github.siom79.japicmp:japicmp-maven-plugin (#689)
- 037b562 Bump quarkus.platform.version from 3.12.2 to 3.12.3 (#690)
- 7d809d1 Bump org.testcontainers:testcontainers-bom (#691)
- fd1d2bb Bump org.apache.maven.plugins:maven-javadoc-plugin (#692)
- b7714cc Bump org.apache.maven.plugins:maven-failsafe-plugin (#685)
- bd148c1 Bump org.neo4j:neo4j-cypher-dsl-bom (#684)
- 97451b4 Bump org.assertj:assertj-core from 3.26.0 to 3.26.3 (#683)
- 7e9b01c Bump quarkus.platform.version from 3.12.1 to 3.12.2 (#682)
- 8642383 Bump org.apache.maven.plugins:maven-release-plugin (#681)
- 481f572 Bump org.apache.maven.plugins:maven-surefire-plugin (#680)
- b772593 Bump org.openapitools:openapi-generator-maven-plugin (#678)
- 85f0e86 Bump io.github.git-commit-id:git-commit-id-maven-plugin (#677)
- 432de17 Bump quarkus.platform.version from 3.12.0 to 3.12.1 (#676)
- f00efe6 Bump dev.langchain4j:langchain4j-bom from 0.31.0 to 0.32.0 (#675)
- 05de4ce Bump org.jreleaser:jreleaser-maven-plugin (#673)
- 12a77e2 Bump quarkus.platform.version from 3.11.3 to 3.12.0 (#672)
- d0e2cc5 Bump org.moditect:moditect-maven-plugin (#671)
- ec764fa Bump org.jdbi:jdbi3-bom from 3.45.1 to 3.45.2 (#670)
- 95c2c72 Bump org.junit:junit-bom from 5.10.2 to 5.10.3 (#669)
- f03b276 Bump io.github.cdimascio:dotenv-java from 3.0.0 to 3.0.1 (#668)
- 682fd73 Bump spring-boot.version from 3.3.0 to 3.3.1 (#667)
- ff930b3 Bump org.neo4j:neo4j-cypher-dsl-bom (#666)
- 6887f27 Bump quarkus.platform.version from 3.11.2 to 3.11.3 (#665)
- bbbb878 Bump org.apache.maven.plugins:maven-jar-plugin (#664)
- cd5822e Bump org.neo4j:neo4j-cypher-dsl-bom (#663)
- 1b3a91b Bump org.apache.maven.plugins:maven-release-plugin (#662)
- 69f7988 Bump org.apache.maven.plugins:maven-surefire-plugin (#661)
- 38d8547 Bump org.apache.maven.plugins:maven-failsafe-plugin (#660)
- 233dd73 Bump org.jooq:jooq from 3.19.9 to 3.19.10 (#659)
- f66a3aa Bump quarkus.platform.version from 3.11.1 to 3.11.2 (#658)
- daafd3b Bump io.netty:netty-bom from 4.1.110.Final to 4.1.111.Final (#657)
- 43eed8d Bump spring-javaformat.version from 0.0.41 to 0.0.42 (#651)
- 73e5ebc Bump org.apache.maven.plugins:maven-checkstyle-plugin (#652)
- 380d57a Bump io.github.git-commit-id:git-commit-id-maven-plugin (#653)
- e87c826 Bump quarkus.platform.version from 3.11.0 to 3.11.1 (#654)
- 8f807ec Bump org.jetbrains.kotlin:kotlin-stdlib-jdk8 (#655)
- 5ea0eb9 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.15 to 2.3.17 (#656)

## 🛠 Build
- 72a57aa build: Move all bundles to a dedicated folder. (#650)
- fec5857 build: Attach main project javadoc to the bundle.


# 6.0.0-M04

## 🚀 Features
- 081101a feat: Add a bundled version of the text2cypher translator. (#648)
- 4beeb4c feat: Add text2cypher example. (#647)
- 4610a45 feat: Provide method to retrieve current database name. (#646)
- 8b34f05 feat: Add support for causal clustering bookmarks.

## 🔄️ Refactorings

- 59e3e02 Expose full config to translator factories (#605)

## 📝 Documentation
- 071bbff docs: Document the text2cypher bundle. (#649)

## 🧹 Housekeeping
- 9d2c70e Bump quarkus.platform.version from 3.10.2 to 3.11.0 (#640)
- 3c4403d Bump org.apache.maven.plugins:maven-enforcer-plugin (#641)
- 39461c4 Bump org.apache.maven.plugins:maven-shade-plugin (#642)
- 2b20174 Bump org.sonarsource.scanner.maven:sonar-maven-plugin (#643)
- 054c8fd Bump org.jooq:jooq from 3.19.8 to 3.19.9 (#644)
- a6ebd03 Bump org.apache.maven.plugins:maven-javadoc-plugin (#645)
- 764e015 Bump org.codehaus.mojo:exec-maven-plugin (#639)
- f83f4ab Bump org.openapitools:openapi-generator-maven-plugin (#638)
- a435593 Bump quarkus.platform.version from 3.10.1 to 3.10.2 (#637)
- c7254f9 Bump org.assertj:assertj-core from 3.25.3 to 3.26.0 (#636)
- d8af901 Bump com.puppycrawl.tools:checkstyle (#635)
- abe1544 Bump io.netty:netty-bom from 4.1.109.Final to 4.1.110.Final (#634)
- 881cc15 Bump spring-boot.version from 3.2.5 to 3.3.0 (#633)
- 2542078 Bump org.codehaus.mojo:build-helper-maven-plugin (#632)
- 4dcab41 Bump quarkus.platform.version from 3.10.0 to 3.10.1 (#631)
- 9c1f4c4 Bump com.github.ekryd.sortpom:sortpom-maven-plugin (#630)
- 7c0cab6 Bump org.graalvm.buildtools:native-maven-plugin (#629)
- 5099ffa build(deps-dev): Bump org.asciidoctor:asciidoctorj from 2.5.12 to 2.5.13 (#628)
- 8b6563f Bump org.jooq:jooq from 3.19.7 to 3.19.8 (#619)
- 501592d Bump org.mockito:mockito-bom from 5.11.0 to 5.12.0 (#626)
- d4068af Bump com.mycila:license-maven-plugin from 4.3 to 4.5 (#625)
- c473fc9 Bump org.testcontainers:testcontainers-bom (#624)
- 0f69597 Bump com.github.siom79.japicmp:japicmp-maven-plugin (#623)
- c360a0c Bump org.neo4j:neo4j-cypher-dsl-bom (#622)
- 761b241 Bump org.apache.maven.plugins:maven-install-plugin (#621)
- 9e15485 Bump org.apache.maven.plugins:maven-deploy-plugin (#620)
- 5647301 Bump quarkus.platform.version from 3.9.5 to 3.10.0 (#618)
- 45980a6 Bump org.jreleaser:jreleaser-maven-plugin (#617)
- ffe52d8 Bump com.github.siom79.japicmp:japicmp-maven-plugin (#616)
- 932bd27 Bump org.apache.maven.plugins:maven-shade-plugin (#615)
- 79da69f Bump com.puppycrawl.tools:checkstyle (#614)
- e7f066c Bump quarkus.platform.version from 3.9.4 to 3.9.5 (#613)


# 6.0.0-M03

## 🚀 Features
- 197a2e4 feat: Add support for locally kept client info. (#607)
- ea615d8 feat: add more authentication schemes (#596)

## 🐛 Bug Fixes
- 3f64f28 fix: NPE when setting property with unexpected type (#597)
- 3421d34 fix(docs): document auth in URL parameters (#590)
- 809083e fix: percent decode query part of connection URL (#591)
- 7edc9e0 fix: Make tests DST independent. (#589)

## 🔄️ Refactorings
- f7357e7 refactor: Use the class loader the driver was loaded with to find services. (#606)

## 🧹 Housekeeping
- a30d6be Bump org.openapitools:openapi-generator-maven-plugin (#612)
- 7e31f83 Bump io.netty:netty-bom from 4.1.108.Final to 4.1.109.Final (#611)
- 7c8711f Bump org.apache.maven.plugins:maven-jar-plugin (#610)
- 4357c1a Bump quarkus.platform.version from 3.9.3 to 3.9.4 (#609)
- 56f712a Bump spring-boot.version from 3.2.4 to 3.2.5 (#608)
- 60d5d5f Bump slf4j.version from 2.0.12 to 2.0.13 (#604)
- 584ab55 Bump org.neo4j:neo4j-cypher-dsl-bom (#603)
- d845cc9 Bump quarkus.platform.version from 3.9.2 to 3.9.3 (#602)
- b9c58f9 build(deps-dev): Bump com.tngtech.archunit:archunit from 1.2.1 to 1.3.0 (#601)
- 2b8117e Bump org.apache.maven.plugins:maven-jar-plugin (#600)
- d4fa84f Bump org.apache.maven.plugins:maven-source-plugin (#595)
- 9ae16ae Bump org.jacoco:jacoco-maven-plugin from 0.8.11 to 0.8.12 (#594)
- 3da55c5 Bump quarkus.platform.version from 3.9.1 to 3.9.2 (#593)
- c8d0ef2 Bump org.jooq:jooq from 3.19.6 to 3.19.7 (#592)
- 42a9fe7 Bump org.moditect:moditect-maven-plugin (#586)
- b7a51e4 Bump com.puppycrawl.tools:checkstyle (#587)
- f3f85f3 Bump quarkus.platform.version from 3.8.2 to 3.9.1 (#588)
- 153dd0e Bump org.apache.maven.plugins:maven-assembly-plugin (#585)
- fc0d22a Bump spring-boot.version from 3.2.3 to 3.2.4 (#584)
- cae1fce Bump io.netty:netty-bom from 4.1.107.Final to 4.1.108.Final (#583)
- ad677c5 Bump org.apache.maven.plugins:maven-compiler-plugin (#582)
- b99c1fd Bump io.github.git-commit-id:git-commit-id-maven-plugin (#581)


# 6.0.0-M02

The second milestone includes several important updates, among them the fact that we now ship a BOM module, which makes it really easy to import all artifacts of the driver in a consistent way in any Maven or Gradle build.

Also we would like to highlight the fact that we now can

* chain SQL translators
* use `INSERT..RETURNING` statements, that will automatically be translated to Cypher
* use character and binary streams as well as big decimals for prepared statements
* propagate `readyOnly` correctly into the Bolt layer

While you can always use a modern Java 8+ time type, we aligned the classic way of using `java.sql.Date` and `java.sql.Time` and the corresponding methods on the prepared statement and results that take in an additional Calendar instance for building timezone information with the way dates, times and date times work in Neo4j.

Additionally, we tested the binaries we have now with more tooling. We have been able to successfully use the driver within:

* DBeaver
* IntelliJ DataGrip
* KNime
* Apache Nifi
* Tableau

Full list of changes since 6.0.0-M01:

## 🚀 Features
- de23fac feat: Add support for `strpos` and `position` (SqlToCyper). (#570)
- 7a09ab7 feat: Add possibility to specify translator factories directly. (#568)
- 39549b4 feat: Implement Connection Network Timeout. (#564)
- 7f25d41 feat: Implement `setCharacterStream`, `setBinaryStream` and related. (#546)
- ea68d7a feat: Add support for chaining SQL translators.
- 1be830d feat: Add support for `INSERT…RETURNING` statements.

## 🐛 Bug Fixes
- 4926da2 fix: Propagate the `readOnly` property to the Bolt transaction. (#544)
- 306d0e3 fix: Return the maximum precision for INTEGER and FLOAT properties. (#535)

## 🔄️ Refactorings
- 6932cc2 refactor: Manage flatten-plugin proper.
- 387483e refactor: Remove duplicate exposed port in Stub Server (#569)
- d85a767 refactor: Address code quality issues. (#565)
- 303b2c0 refactor: Fully test `ResultSetImpl` contract. (#563)
- cce2a30 refactor: Add a top-level BOM project for easy consumption of the produced binaries. (#562)
- a38f66a refactor: Add a test for objects that not coerceable to strings. (#561)
- 9016cb2 refactor: Remove trailing dots from exception messages. (#560)
- e93d810 refactor: Make sure result set can only be consumed once, add test for empty result set meta data (#559)
- 53f6c61 refactor: Align date, time and timestamp mapping in and out, provide BigDecimal support. (#547)
- 4a2d402 refactor: Address all Maven, Javac and JavaDoc warnings, and surpress banners where applicable. (#545)
- 6caa1e6 refactor: Ensure `nativeSQL` still does access the right chain of translators.
- 62807a6 refactor: Extract all metadata queries used into a separate resource. (#530)

## 📝 Documentation
- a9ad923 docs: Document supported SQL dialects.

## 🧰 Tasks
- 0c66c7a chore: Use correct version numbers in all `@since` tags.
- 6fc8a8b chore: Add a code of conduct.
- 19476b2 chore: Add a test that scalar properties are handled according to spec. (#533)
- ae22117 chore: Add dedicated tests that all properties are correctly deserialized. (#532)
- f49786a chore: Add dedicated test for retrieving element ids and ids. (#531)

## 🧹 Housekeeping
- ec040ea Update to Cypher-DSL 2023.9.5.
- 1126777 Bump org.sonarsource.scanner.maven:sonar-maven-plugin (#577)
- d26767b Bump com.puppycrawl.tools:checkstyle (#576)
- 14aa14e Bump org.asciidoctor:asciidoctorj-pdf from 2.3.14 to 2.3.15 (#575)
- 56293d8 Bump org.jdbi:jdbi3-bom from 3.45.0 to 3.45.1 (#574)
- 0028fbc Bump org.moditect:moditect-maven-plugin (#573)
- fb6ea1c build(deps-dev): Bump org.asciidoctor:asciidoctorj from 2.5.11 to 2.5.12 (#572)
- ba2f2b6 Bump org.asciidoctor:asciidoctorj-pdf from 2.3.13 to 2.3.14 (#554)
- b30637c Bump jakarta.xml.bind:jakarta.xml.bind-api (#551)
- 1a730bc Bump quarkus.platform.version from 3.8.1 to 3.8.2 (#550)
- d210634 Bump org.testcontainers:testcontainers-bom (#549)
- c1b6467 Bump org.openapitools:openapi-generator-maven-plugin (#557)
- 4e35220 Bump com.github.siom79.japicmp:japicmp-maven-plugin (#556)
- 5a305d3 Bump io.github.git-commit-id:git-commit-id-maven-plugin (#555)
- 572c89c Bump com.github.ekryd.sortpom:sortpom-maven-plugin (#553)
- 7488812 Bump org.jooq:jooq from 3.19.5 to 3.19.6 (#552)
- edce700 Bump org.apache.maven.plugins:maven-assembly-plugin (#558)
- 4b996b8 Bump org.mockito:mockito-core from 5.10.0 to 5.11.0 (#540)
- 3e18f30 Bump quarkus.platform.version from 3.7.4 to 3.8.1 (#539)
- 417228c Bump org.jreleaser:jreleaser-maven-plugin (#538)
- 69b89ab Bump com.puppycrawl.tools:checkstyle (#537)

## 🛠 Build
- 0afc54a build: Exclude benchkit from security checks.
- dcf5549 build: Update cache action to v4.
- 67e90e3 build: Add semantic versioning checks to verification.
- fc05cb0 build: Use Mockito bom.
- 6a824d1 build: Don’t generate JavaDoc for benchkit and doc modules.
- 580b5bf Add wait strategy for stub server (#571)


# 6.0.0-M01

This is the first milestone release of the Neo4j JDBC Driver version 6 with exciting new features:
* It does not depend on the common Neo4j Java driver anymore, thus being less resource intensive
* It provides a pluggable SQL to Cypher translation infrastructure and comes with one implementation ootb
* Better support for the JDBC Metadata APIs

The Java baseline for this version is JDK 17.

Please have a look at our documentation for information how to migrate from version 5 and 4: [Migrating from older versions of the driver](http://neo4j.github.io/neo4j-jdbc/6.0.0-M01/#_migrating_from_older_versions_or_other_jdbc_drivers_for_neo4j)
