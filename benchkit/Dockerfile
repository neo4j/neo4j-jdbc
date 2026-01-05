#
# Copyright (c) 2023-2026 "Neo4j,"
# Neo4j Sweden AB [https://neo4j.com]
#
# This file is part of Neo4j.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM maven:3.9.11-eclipse-temurin-25 as maven
COPY . /driver
WORKDIR driver
RUN mvn -Dfast -am -pl neo4j-jdbc clean install
RUN mvn -Dfast -pl benchkit package

FROM eclipse-temurin:25-jre as builder
WORKDIR application
ARG JAR_FILE=/driver/benchkit/target/*.jar
COPY --from=maven ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:25-jre
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
RUN useradd -m hschneider
RUN chown -R hschneider:hschneider /application
USER hschneider
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

