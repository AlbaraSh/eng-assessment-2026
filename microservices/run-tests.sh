#!/bin/bash

set -e

for i in rates iot reactive; do
  pushd "$i"
  ./gradlew build dockerBuild
  popd
done

docker compose down -v
docker compose up --wait
pushd end2end-tests
./gradlew test
docker compose down -v
popd
