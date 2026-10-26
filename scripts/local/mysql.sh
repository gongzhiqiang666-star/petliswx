#!/bin/bash
set -euo pipefail
base="$HOME/.local/share/petlis/mysql"
mysql_bin="$base/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin"
case "${1:-status}" in
 start)
  test -f "$base/my.cnf" || { echo 'Petlis MySQL has not been initialized'; exit 1; }
  if "$mysql_bin/mysqladmin" --defaults-extra-file="$base/root.cnf" ping >/dev/null 2>&1; then echo 'Petlis MySQL is running at 127.0.0.1:13306'; exit 0; fi
  launchctl kickstart "gui/$(id -u)/com.petlis.mysql-local"
  for i in {1..60}; do
   if "$mysql_bin/mysqladmin" --defaults-extra-file="$base/root.cnf" ping >/dev/null 2>&1; then echo 'Petlis MySQL started at 127.0.0.1:13306'; exit 0; fi
   sleep 1
  done
  echo "Startup failed; inspect $base/mysql-error.log"; exit 1;;
 stop) "$mysql_bin/mysqladmin" --defaults-extra-file="$base/root.cnf" shutdown;;
 status) "$mysql_bin/mysqladmin" --defaults-extra-file="$base/root.cnf" ping;;
 client) exec "$mysql_bin/mysql" --defaults-extra-file="$base/app.cnf" laboklin;;
 *) echo 'Usage: mysql.sh start|stop|status|client'; exit 2;;
esac
