#!/bin/bash
set -euo pipefail
repo="$(cd "$(dirname "$0")/../.." && pwd)"
source "$HOME/.local/share/petlis/mysql/backend.env"
"$repo/scripts/local/mysql.sh" start
# Local integration never runs scheduled background jobs.
export APP_SCHEDULING_ENABLED=false
export PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"
if [ -f "$HOME/.local/share/petlis/sf-express/sandbox.json" ]; then
  export APP_SF_SANDBOX_CONFIG="$HOME/.local/share/petlis/sf-express/sandbox.json"
fi
export JAVA_HOME="$HOME/.local/share/petlis/tools/Home"
export PATH="$JAVA_HOME/bin:$HOME/.local/share/petlis/tools/libexec/bin:$PATH"
cd "$repo/petlis-backend"
exec mvn -o -Dmaven.repo.local="${PETLIS_MAVEN_REPO:-$HOME/.m2/repository}" spring-boot:run -Dspring-boot.run.profiles=dev "$@"
