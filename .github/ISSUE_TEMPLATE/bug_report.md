---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

### Describe the bug

A clear and concise description of what the bug is.

### Expected behaviour

A clear and concise description of what you expected to happen.

### To Reproduce

In general, if you can, please provide:

* The query
* The configuration of the driver (such as timeouts, translation settings etc.)
* The data if the issue is data dependent

#### Always

- [ ] Java version
- [ ] Neo4j JDBC Driver version
- [ ] Which bundle of the Neo4j JDBC Driver (individual dependencies via a dependency management tool, the small bundle (`neo4j-jdbc-bundle-x.y.z.jar`) or the full bundle, containing the default translator (`neo4j-jdbc-full-bundle-x.y.z.jar`)

#### Happens inside your program code

* Which Framework if any (such as Spring Boot, Quarkus, Micronaut etc.)?
* Which JDBC abstraction layer if any (such as JDBI, Spring JDBC Template, Hibernate or others)?

#### Happens inside an application

In case you are using the JDBC driver in a tool such as

* Apache Nifi
* Apache Spark
* DBeaver
* IntelliJ DataGrip
* KNime
* Tableau
* dbt

etc. please provide the exact name and version of the tool and if it is not in the list, a documentation or download link would be helpful to have.

### Any additional context

Please feel free to add any more context that you can provide.
