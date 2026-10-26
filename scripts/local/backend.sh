#!/bin/bash
set -euo pipefail
repo="$(cd "$(dirname "$0")/../.." && pwd)"
source "$HOME/.local/share/petlis/mysql/backend.env"
"$repo/scripts/local/mysql.sh" start
export JAVA_HOME="$HOME/.local/share/petlis/tools/Home"
export PATH="$JAVA_HOME/bin:$HOME/.local/share/petlis/tools/libexec/bin:$PATH"
cd "$repo/petlis-backend"
exec mvn -o -Dmaven.repo.local="$HOME/.local/share/petlis/tools/m2" spring-boot:run -Dspring-boot.run.profiles=dev "$@"
