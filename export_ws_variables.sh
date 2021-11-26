#! /usr/bin/env bash

set -euox pipefail

get_version() {
 local version_property
 version_property=$(./gradlew properties | grep --extended-regexp "^version: (.*)")
 if [[ -z "${version_property}" ]]; then
   echo "Could not find property version in project" >&2
   exit 2
 fi
 local version
 version=$(echo "${version_property}" | tr --delete "[:space:]" | cut --delimiter=":" --fields=2)
 echo "${version%-*}"
}

main() {
  echo "export PROJECT_VERSION=$(get_version)"
}

main "$@"
