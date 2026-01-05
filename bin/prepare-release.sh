#!/usr/bin/env bash
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


set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"

sed -i.bak 's/\(:latest_version:\) \(.*\)/\1 '"${1}"'/g' $DIR/../README.adoc
rm $DIR/../README.adoc.bak

sed -i.bak 's/\(.*"version":\) ".*",/\1 "'${1}'",/g' $DIR/../etc/antora/package.json
rm $DIR/../etc/antora/package.json.bak

if test -n "${2-}"; then
  DRYRUN=$2
else
  DRYRUN='false'
fi

if [ "$DRYRUN" != "true" ]; then
  git add $DIR/../README.adoc
  git commit -m "[maven-release-plugin] update README.adoc"
fi
