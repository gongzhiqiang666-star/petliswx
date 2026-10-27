#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

PLATFORM_TABLES = {
    "lin_user_role", "lin_shipment", "lin_lab_task", "lin_test_result",
    "lin_report_workflow", "lin_report_template", "lin_order_clinical",
    "lin_order_idempotency", "lin_contract", "lin_contract_item_price",
    "lin_monthly_bill", "lin_monthly_bill_item", "lin_file",
    "lin_audit_record", "lin_operational_config",
}


def validate_connection(host, port, isolated_copy):
    if host not in {"127.0.0.1", "localhost", "::1"}:
        raise ValueError("只允许连接本机隔离副本，拒绝远程或其他主机")
    if not isolated_copy:
        raise ValueError("必须显式传入 --isolated-copy")
    if port == 3306:
        raise ValueError("拒绝默认 3306 端口；隔离副本必须使用独立端口")
    if not 1 <= port <= 65535:
        raise ValueError("端口无效")


def compare_snapshots(baseline, current):
    legacy_tables = set(baseline["legacy_tables"])
    differences = []
    for section, label in (("columns", "旧表列"), ("indexes", "旧表索引")):
        expected = [row for row in baseline.get(section, []) if row["table"] in legacy_tables]
        actual = [row for row in current.get(section, []) if row["table"] in legacy_tables]
        if expected != actual:
            differences.append(f"{label}发生变化")
    expected_triggers = [row for row in baseline.get("triggers", []) if row["table"] in legacy_tables]
    actual_triggers = [row for row in current.get("triggers", []) if row["table"] in legacy_tables]
    if expected_triggers != actual_triggers:
        differences.append("旧表触发器发生变化")
    return differences


def mysql_rows(args, query):
    command = [
        args.mysql_bin, "--protocol=TCP", f"--host={args.host}",
        f"--port={args.port}", f"--user={args.user}", "--batch", "--raw",
        "--skip-column-names", "--default-character-set=utf8mb4", f"--database={args.database}",
        f"--execute={query}",
    ]
    result = subprocess.run(command, check=True, text=True, capture_output=True)
    return [json.loads(line) for line in result.stdout.splitlines() if line.strip()]


def snapshot(args):
    validate_connection(args.host, args.port, args.isolated_copy)
    if not re.fullmatch(r"[A-Za-z0-9_]+", args.database):
        raise ValueError("数据库名只能包含字母、数字和下划线")
    excluded = ",".join(f"'{table}'" for table in sorted(PLATFORM_TABLES))
    columns = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', TABLE_NAME, 'position', ORDINAL_POSITION, 'name', COLUMN_NAME,
 'type', COLUMN_TYPE, 'nullable', IS_NULLABLE, 'default', COLUMN_DEFAULT,
 'extra', EXTRA, 'charset', CHARACTER_SET_NAME, 'collation', COLLATION_NAME,
 'generation', GENERATION_EXPRESSION)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='{args.database}' AND TABLE_NAME NOT IN ({excluded})
ORDER BY TABLE_NAME, ORDINAL_POSITION
""")
    indexes = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', TABLE_NAME, 'name', INDEX_NAME, 'non_unique', NON_UNIQUE,
 'position', SEQ_IN_INDEX, 'column', COLUMN_NAME, 'collation', COLLATION,
 'sub_part', SUB_PART, 'packed', PACKED, 'nullable', NULLABLE,
 'index_type', INDEX_TYPE, 'comment', COMMENT, 'index_comment', INDEX_COMMENT,
 'visible', IS_VISIBLE, 'expression', EXPRESSION)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA='{args.database}' AND TABLE_NAME NOT IN ({excluded})
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
""")
    triggers = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', EVENT_OBJECT_TABLE, 'name', TRIGGER_NAME, 'event', EVENT_MANIPULATION,
 'timing', ACTION_TIMING, 'orientation', ACTION_ORIENTATION, 'sql', ACTION_STATEMENT,
 'sql_mode', SQL_MODE, 'definer', DEFINER, 'client_charset', CHARACTER_SET_CLIENT,
 'connection_collation', COLLATION_CONNECTION, 'database_collation', DATABASE_COLLATION)
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA='{args.database}'
ORDER BY EVENT_OBJECT_TABLE, TRIGGER_NAME
""")
    legacy_tables = sorted({row["table"] for row in columns})
    return {
        "format_version": 1,
        "database": args.database,
        "legacy_tables": legacy_tables,
        "columns": columns,
        "indexes": indexes,
        "triggers": triggers,
    }


def add_connection_arguments(parser):
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--user", default="root")
    parser.add_argument("--database", required=True)
    parser.add_argument("--mysql-bin", default=os.environ.get("MYSQL_BIN", "mysql"))
    parser.add_argument("--isolated-copy", action="store_true")


def main(argv=None):
    parser = argparse.ArgumentParser(description="隔离 MySQL 副本的旧表列、索引、触发器快照守护")
    subparsers = parser.add_subparsers(dest="command", required=True)
    snapshot_parser = subparsers.add_parser("snapshot")
    add_connection_arguments(snapshot_parser)
    snapshot_parser.add_argument("--output", required=True)
    verify_parser = subparsers.add_parser("verify")
    add_connection_arguments(verify_parser)
    verify_parser.add_argument("--baseline", required=True)
    args = parser.parse_args(argv)
    try:
        if args.command == "snapshot":
            data = snapshot(args)
            Path(args.output).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"已保存 {len(data['legacy_tables'])} 张旧表的结构快照：{args.output}")
            return 0
        baseline = json.loads(Path(args.baseline).read_text(encoding="utf-8"))
        differences = compare_snapshots(baseline, snapshot(args))
        if differences:
            for difference in differences:
                print(difference, file=sys.stderr)
            return 1
        print(f"校验通过：{len(baseline['legacy_tables'])} 张旧表的列、索引、触发器均未变化")
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError, json.JSONDecodeError) as error:
        print(f"schema 守护失败：{error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
