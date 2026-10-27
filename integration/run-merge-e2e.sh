#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS="$ROOT/integration/results"
MYSQL_HOME="${MYSQL_HOME:-/opt/homebrew/opt/mysql@8.4}"
MYSQLD="$MYSQL_HOME/bin/mysqld"
MYSQL="$MYSQL_HOME/bin/mysql"
MYSQLADMIN="$MYSQL_HOME/bin/mysqladmin"
PORT="${PETLIS_MERGE_MYSQL_PORT:-13318}"
BACKEND_PORT="${PETLIS_MERGE_BACKEND_PORT:-18082}"
ADMIN_PORT="${PETLIS_MERGE_ADMIN_PORT:-14173}"
RUNTIME="$RESULTS/mysql84-merge-runtime"
SCHEMA="petlis_merge_e2e"
SOCKET="/tmp/petlis-merge-$$.sock"
BACKEND_PID=""
ADMIN_PID=""
mkdir -p "$RESULTS"
rm -rf "$RUNTIME"
mkdir -p "$RUNTIME"
cleanup() {
  local status=$?
  if [[ -n "$ADMIN_PID" ]]; then kill "$ADMIN_PID" 2>/dev/null || true; fi
  if [[ -n "$BACKEND_PID" ]]; then kill "$BACKEND_PID" 2>/dev/null || true; fi
  if [[ -S "$SOCKET" ]]; then MYSQL_PWD='' "$MYSQLADMIN" --no-defaults --socket="$SOCKET" -uroot shutdown >/dev/null 2>&1 || true; fi
  exit "$status"
}
trap cleanup EXIT INT TERM
[[ -x "$MYSQLD" && -x "$MYSQL" && -x "$MYSQLADMIN" ]]
"$MYSQLD" --version | grep -F 'Ver 8.4.'
"$MYSQLD" --no-defaults --initialize-insecure --user="$(id -un)" --datadir="$RUNTIME/data" >"$RESULTS/mysql84-merge-initialize.log" 2>&1
"$MYSQLD" --no-defaults --datadir="$RUNTIME/data" --bind-address=127.0.0.1 --port="$PORT" --socket="$SOCKET" --mysqlx=OFF --pid-file="$RUNTIME/mysql.pid" --log-error="$RESULTS/mysql84-merge-server.log" --skip-name-resolve --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci --daemonize
for attempt in $(seq 1 90); do
  if "$MYSQLADMIN" --no-defaults --socket="$SOCKET" -uroot ping --silent >/dev/null 2>&1; then break; fi
  [[ "$attempt" == 90 ]] && { cat "$RESULTS/mysql84-merge-server.log"; exit 1; }
  sleep 1
done
"$MYSQL" --no-defaults --socket="$SOCKET" -uroot -e "CREATE DATABASE $SCHEMA CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER 'merge'@'127.0.0.1' IDENTIFIED BY 'merge-local-only'; CREATE USER 'merge'@'localhost' IDENTIFIED BY 'merge-local-only'; GRANT ALL ON $SCHEMA.* TO 'merge'@'127.0.0.1'; GRANT ALL ON $SCHEMA.* TO 'merge'@'localhost'"
"$MYSQL" --no-defaults --socket="$SOCKET" -uroot "$SCHEMA" <"$ROOT/integration/db/legacy-fixture.sql"
export MYSQL_PWD='merge-local-only'
python3 "$ROOT/integration/schema_guard.py" snapshot --port "$PORT" --database "$SCHEMA" --user merge --isolated-copy --mysql-bin "$MYSQL" --output "$RESULTS/schema-before.json"
"$MYSQL" --no-defaults --socket="$SOCKET" -umerge "$SCHEMA" <"$ROOT/petlis-backend/src/main/resources/db/platform/V1__create_platform_workflow_tables.sql"
"$MYSQL" --no-defaults --socket="$SOCKET" -umerge "$SCHEMA" <"$ROOT/petlis-backend/src/main/resources/db/platform/V1__create_platform_workflow_tables.sql"
"$MYSQL" --no-defaults --socket="$SOCKET" -umerge "$SCHEMA" <"$ROOT/integration/db/platform-seed.sql"
python3 "$ROOT/integration/schema_guard.py" verify --port "$PORT" --database "$SCHEMA" --user merge --isolated-copy --mysql-bin "$MYSQL" --baseline "$RESULTS/schema-before.json" | tee "$RESULTS/schema-after-ddl.log"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
"$ROOT/petlis-backend/mvnw" -q -f "$ROOT/petlis-backend/pom.xml" -DskipTests package
export DB_URL="jdbc:mysql://127.0.0.1:$PORT/$SCHEMA?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true"
export DB_USERNAME=merge DB_PASSWORD='merge-local-only' SPRING_PROFILES_ACTIVE=dev DB_MIGRATIONS_ENABLED=false
export JWT_SECRET='merge-e2e-local-only-secret-at-least-32-bytes'
export SERVER_PORT="$BACKEND_PORT" SERVER_DOMAIN="http://127.0.0.1:$BACKEND_PORT"
export APP_FILE_ACCESS_URL="http://127.0.0.1:$BACKEND_PORT/api/files"
export PDF_STORAGE_PATH="$RESULTS/files" APP_NOTIFICATIONS_LEGACY_BRIDGE_ENABLED=false
mkdir -p "$PDF_STORAGE_PATH"
java -jar "$ROOT/petlis-backend/target/petlis-backend-1.0.0.jar" >"$RESULTS/backend.log" 2>&1 & BACKEND_PID=$!
API_ORIGIN="http://127.0.0.1:$BACKEND_PORT" PORT="$ADMIN_PORT" node "$ROOT/admin/server.js" >"$RESULTS/admin-server.log" 2>&1 & ADMIN_PID=$!
for attempt in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:$BACKEND_PORT/api/auth/capabilities" >/dev/null 2>&1 && curl -fsS "http://127.0.0.1:$ADMIN_PORT/" >/dev/null 2>&1; then break; fi
  [[ "$attempt" == 90 ]] && { tail -100 "$RESULTS/backend.log"; exit 1; }
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then cat "$RESULTS/backend.log"; exit 1; fi
  sleep 1
done
BACKEND_ORIGIN="http://127.0.0.1:$BACKEND_PORT" ADMIN_ORIGIN="http://127.0.0.1:$ADMIN_PORT" MYSQL_SOCKET="$SOCKET" MYSQL_CLIENT="$MYSQL" MYSQL_DATABASE="$SCHEMA" node "$ROOT/integration/merge-e2e.js" | tee "$RESULTS/merge-e2e.log"
python3 "$ROOT/integration/schema_guard.py" verify --port "$PORT" --database "$SCHEMA" --user merge --isolated-copy --mysql-bin "$MYSQL" --baseline "$RESULTS/schema-before.json" | tee "$RESULTS/schema-final.log"
