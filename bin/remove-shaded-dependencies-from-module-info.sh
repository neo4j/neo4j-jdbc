#!/usr/bin/env bash
#
# Copyright (c) 2023-2025 "Neo4j,"
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


set -euo pipefail

# Expects to arguments
# 1. Absolute path of the existing module descriptor
# 2. Absolute path of the target module descriptor
# 3. Is optional and if set to an arbitrary value creates the module in such a way that the service provider implementations are bundled

IN="$1"
OUT="$2"

mkdir -p "$(dirname "${OUT}")"

if [ "$#" -ne 3 ]; then
  sed -e '/\/\/ start::shaded-dependencies/,/\/\/ end::shaded-dependencies/d' \
      -e 's/\/\/ requires \(.*\);/requires \1;/' "$IN" | cat -s > "$OUT"
else
  sed -e '/\/\/ start::shaded-dependencies/,/\/\/ end::shaded-dependencies/d' \
      -e 's/\/\/ requires \(.*\);/requires \1;/' \
      -e 's/\/\/ exports \(.*\);/exports \1;/' \
      -e 's/\/\/ provides \(.*\);/provides \1;/' "$IN" | cat -s > "$OUT"
fi;
