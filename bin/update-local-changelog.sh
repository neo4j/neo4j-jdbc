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


# That script will break after 100 releases (limits in the gh tooling, but until then, it's enough).

set -euo pipefail
DIR="$(dirname "$(realpath "$0")")"
cd $DIR/..

FILENAME=CHANGELOG.md
rm CHANGELOG.md

for OUTPUT in $(gh release list | awk -F "\t" '{print $1}'| sort -Vr)
do
  if [[ $OUTPUT = 6* ]]
  then
    echo -en "# $OUTPUT\n\n" >> $FILENAME
    gh release view $OUTPUT | dos2unix | gsed '1,/^--$/d' | gsed "s/# What's Changed//g" | gsed '1,2{/^$/d}' >> $FILENAME
    echo -en "\n\n\n" >> $FILENAME
  fi
done

gsed -i -e :a -e '/^\n*$/{$d;N;ba' -e '}' $FILENAME
