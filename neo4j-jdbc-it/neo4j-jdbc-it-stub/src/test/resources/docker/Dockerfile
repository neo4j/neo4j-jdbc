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

FROM debian:bookworm AS build

WORKDIR /root/build/boltstub

RUN apt-get update && apt-get install -y \
      apt-transport-https \
      curl \
      gcc \
      python3 \
      python3-dev \
      git \
    && rm -rf /var/lib/apt/lists/*

RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal \
    && export PATH="/root/.cargo/bin:$PATH"
ENV PATH="/root/.cargo/bin:$PATH"

RUN git clone https://github.com/neo4j-drivers/testkit.git /testkit \
    && cd /testkit \
    && git checkout 14ebed82fb27a0280361a7c7c29f1e9f4306da6f

RUN echo $PATH && echo hi

RUN cd /testkit/boltstub_rs \
    && cargo build --release \
    && cp target/release/boltstub /root/build/boltstub-bin


FROM debian:bookworm

RUN apt-get update && apt-get install -y \
      python3 \
      python3-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /root/build/boltstub-bin /usr/local/bin/boltstub
COPY scripts /scripts
COPY /entrypoint.sh /

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
