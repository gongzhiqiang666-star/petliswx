#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MYSQLD="${MYSQLD_84:-/opt/homebrew/opt/mysql@8.4/bin/mysqld}"
MYSQL="${MYSQL_84:-/opt/homebrew/opt/mysql@8.4/bin/mysql}"
MYSQLADMIN="${MYSQLADMIN_84:-/opt/homebrew/opt/mysql@8.4/bin/mysqladmin}"
DATA="$ROOT/integration/results/mysql84-publish-runtime"
PORT="${PETLIS_MYSQL_TEST_PORT:-13308}"
SOCKET="/tmp/petlis84-publish-$$.sock"

[ -x "$MYSQLD" ] && [ -x "$MYSQL" ] && [ -x "$MYSQLADMIN" ]
"$MYSQLD" --version | grep -F 'Ver 8.4.'
rm -rf "$DATA"
mkdir -p "$DATA"
"$MYSQLD" --initialize-insecure --datadir="$DATA" --log-error="$DATA/initialize.log"
"$MYSQLD" --datadir="$DATA" --port="$PORT" --socket="$SOCKET" --bind-address=127.0.0.1 \
  --pid-file="$DATA/mysql.pid" --log-error="$DATA/error.log" --daemonize
cleanup() {
  "$MYSQLADMIN" --socket="$SOCKET" -uroot shutdown >/dev/null 2>&1 || true
  rm -rf "$DATA"
}
trap cleanup EXIT
for attempt in $(seq 1 60); do
  if "$MYSQLADMIN" --socket="$SOCKET" -uroot ping --silent >/dev/null 2>&1; then break; fi
  if [ "$attempt" = 60 ]; then cat "$DATA/error.log"; exit 1; fi
  sleep 1
done
"$MYSQL" --socket="$SOCKET" -uroot -e "CREATE DATABASE petlis_test; SELECT VERSION() AS mysql_version, @@transaction_isolation AS isolation_level"
PETLIS_URL="jdbc:mysql://127.0.0.1:${PORT}/petlis_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
/Users/apple/claudelife/petlis/backend/mvnw -q -f "$ROOT/petlis-backend/pom.xml" \
  -Dtest=MySqlPublishConcurrencyIT \
  -DPETLIS_MYSQL_TEST_URL="$PETLIS_URL" \
  -DPETLIS_MYSQL_TEST_USER=root \
  -DPETLIS_MYSQL_TEST_PASSWORD= test
