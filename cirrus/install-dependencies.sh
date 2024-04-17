#!/usr/bin/env bash

set -euox pipefail

readonly REPOSITORY=https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies

declare -A HIGH_LEVEL_API_FIR_FOR_IDE
HIGH_LEVEL_API_FIR_FOR_IDE[url]="${REPOSITORY}/org/jetbrains/kotlin/high-level-api-fir-for-ide/2.0.0-RC1/high-level-api-fir-for-ide-2.0.0-RC1.jar"
HIGH_LEVEL_API_FIR_FOR_IDE[group_id]="org.jetbrains.kotlin"
HIGH_LEVEL_API_FIR_FOR_IDE[artifact_id]="high-level-api-fir-for-ide"
HIGH_LEVEL_API_FIR_FOR_IDE[version]="2.0.0-RC1"
HIGH_LEVEL_API_FIR_FOR_IDE[packaging]="jar"

download_and_install() {
  local -n dependency="${1}"
  local origin="${dependency[url]}"
  local group_id="${dependency[group_id]}"
  local artifact_id="${dependency[artifact_id]}"
  local version="${dependency[version]}"
  local packaging="${dependency[packaging]}"

  local destination=$(basename "${origin}")
  local final_destination="~/.m2/repository/$(echo ${group_id} | sed 's|\.|\/|g')/${artifact_id}/${version}/${destination}"
  echo "Will install ${group_id}:${artifact_id}:${version} to ${final_destination}"

  if [[ -f "${final_destination}" ]]; then
    return
  fi

  # Download
  if [[ ! -f "${destination}" ]]; then
    curl \
      --location \
      --silent \
      --output "${destination}" \
      "${destination}"
  fi
  # Install in local repository
  local absolute_path="$(cd "$(dirname "${destination}")" && pwd)/$(basename "${destination}")"
  mvn install:install-file \
    -Dfile="${absolute_path}" \
    -DgroupId="${group_id}" \
    -DartifactId="${artifact_id}" \
    -Dversion="${version}" \
    -Dpackaging="${packaging}"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    download_and_install HIGH_LEVEL_API_FIR_FOR_IDE
fi