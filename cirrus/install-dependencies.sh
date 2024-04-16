#!/usr/bin/env bash

set -euox pipefail

readonly REPOSITORY=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies

main() {
  local url="${REPOSITORY}/org/jetbrains/kotlin/high-level-api-fir-for-ide/2.0.0-RC1/high-level-api-fir-for-ide-2.0.0-RC1.jar"
  local destination=$(basename "${url}")
  # Download
  if [[ ! -f "${destination}" ]]; then
    curl \
      --location \
      --silent \
      --output "${destination}" \
      "${url}"
  fi
  # Install in local repository
  local absolute_path="$(cd "$(dirname "${destination}")" && pwd)/$(basename "${destination}")"
  mvn install:install-file \
    -Dfile="${absolute_path}" \
    -DgroupId=org.jetbrains.kotlin \
    -DartifactId=high-level-api-fir-for-ide \
    -Dversion=2.0.0-RC1 \
    -Dpackaging=jar
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi