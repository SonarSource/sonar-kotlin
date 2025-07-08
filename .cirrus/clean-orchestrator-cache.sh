#!/bin/bash
set -euo pipefail

cd "$ORCHESTRATOR_HOME" || exit 1

# Find all sonarqube-enterprise-* JAR files, sort them by version, and list them
files=$(/usr/bin/find . -name 'sonarqube-enterprise-*' | /usr/bin/sort --version-sort --field-separator=- --key=3 --reverse)

echo "File that will not be deleted:"
echo "$files" | head -n 1

plugin_files=$(/usr/bin/find . -name 'sonar-kotlin-plugin-*.jar')

# Get the files to delete: all sonarqube-enterprise except the latest one, and all sonar-kotlin-plugin JAR files
files_to_delete=$(echo "$files" | tail -n +2; echo "$plugin_files")

echo ""
if [ -z "$files_to_delete" ]; then
  echo "No files will be deleted."
else
  echo "Files that will be deleted:"
  echo "$files_to_delete"

  # Delete obsolete sonarqube-enterprise files
  # shellcheck disable=SC2016
  echo "$files_to_delete" | xargs -I {} sh -c 'rm -f "{}" && rmdir "$(dirname "{}")" 2>/dev/null || true'
fi
