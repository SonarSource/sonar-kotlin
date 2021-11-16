#! /usr/bin/env bash

readonly SCRIPT_DIRECTORY=$(dirname "$(readlink -f "$0")")
readonly GRADLE_WRAPPER="${SCRIPT_DIRECTORY}/gradlew"

set -euox pipefail

get_gradle_property() {
  local property="${1}"
  local version_property
  version_property=$(${GRADLE_WRAPPER} properties | grep --extended-regexp "^${property}: (.*)")
  if [[ -z "${version_property}" ]]; then
    echo "Could not find property ${property} in project" >&2
    exit 2
  fi
  local property
  property=$(echo "${version_property}" | tr --delete "[:space:]" | cut --delimiter=":" --fields=2)
  echo "${property%-*}"
}

get_version() {
  local raw_version
  raw_version=$(get_gradle_property "version")
  echo "${raw_version%-*}"
}

main() {
  if [[ "${#}" -ne 1 ]]; then
    echo "Usage: ${0} project-name"
    exit 0
  fi
  local project_name="${1}"
  local group_id
  group_id=$(get_gradle_property "group")
  artifact_id="${project_name}"
  local coordinates="${group_id}:${artifact_id}"
  PROJECT_VERSION=$(get_version)
  echo "export PROJECT_VERSION='${PROJECT_VERSION}'"
  echo "export WS_PRODUCTNAME='${coordinates}'"
  echo "export WS_PROJECTNAME='${coordinates} ${PROJECT_VERSION}'"
}

main "$@"
