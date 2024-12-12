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
