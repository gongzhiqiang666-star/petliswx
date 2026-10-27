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
    baseline_tables = set(baseline.get("tables", baseline["legacy_tables"]))
    current_tables = set(current.get("tables", current["legacy_tables"]))
    legacy_tables = set(baseline["legacy_tables"])
    differences = []
    baseline_platform = sorted(baseline_tables & PLATFORM_TABLES)
    if baseline_platform:
        differences.append("基线已含平台表：" + ",".join(baseline_platform))
    missing = sorted(legacy_tables - current_tables)
    if missing:
        differences.append("旧表缺失：" + ",".join(missing))
    unapproved = sorted(current_tables - baseline_tables - PLATFORM_TABLES)
    if unapproved:
        differences.append("未批准新增表：" + ",".join(unapproved))
    sections = (
        ("table_options", "旧表选项"), ("columns", "旧表列"),
        ("indexes", "旧表索引"), ("foreign_keys", "旧表外键"),
        ("check_constraints", "旧表CHECK约束"), ("triggers", "旧表触发器"),
    )
    for section, label in sections:
        expected = [row for row in baseline.get(section, []) if row["table"] in legacy_tables]
        actual = [row for row in current.get(section, []) if row["table"] in legacy_tables]
        if expected != actual:
            differences.append(f"{label}发生变化")
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


def snapshot(args, allow_platform=False):
    validate_connection(args.host, args.port, args.isolated_copy)
    if not re.fullmatch(r"[A-Za-z0-9_]+", args.database):
        raise ValueError("数据库名只能包含字母、数字和下划线")
    table_options = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', TABLE_NAME, 'type', TABLE_TYPE, 'engine', ENGINE,
 'row_format', ROW_FORMAT, 'collation', TABLE_COLLATION,
 'create_options', CREATE_OPTIONS, 'comment', TABLE_COMMENT)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA='{args.database}' AND TABLE_TYPE='BASE TABLE'
ORDER BY TABLE_NAME
""")
    tables = [row["table"] for row in table_options]
    platform_in_baseline = sorted(set(tables) & PLATFORM_TABLES)
    if platform_in_baseline and not allow_platform:
        raise ValueError("基线已含平台表，不能作为旧结构基线：" + ",".join(platform_in_baseline))
    columns = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', TABLE_NAME, 'position', ORDINAL_POSITION, 'name', COLUMN_NAME,
 'type', COLUMN_TYPE, 'nullable', IS_NULLABLE, 'default', COLUMN_DEFAULT,
 'extra', EXTRA, 'charset', CHARACTER_SET_NAME, 'collation', COLLATION_NAME,
 'generation', GENERATION_EXPRESSION)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='{args.database}'
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
WHERE TABLE_SCHEMA='{args.database}'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX
""")
    foreign_keys = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', kcu.TABLE_NAME, 'name', kcu.CONSTRAINT_NAME,
 'position', kcu.ORDINAL_POSITION, 'column', kcu.COLUMN_NAME,
 'referenced_table', kcu.REFERENCED_TABLE_NAME,
 'referenced_column', kcu.REFERENCED_COLUMN_NAME,
 'referenced_constraint', rc.UNIQUE_CONSTRAINT_NAME,
 'update_rule', rc.UPDATE_RULE, 'delete_rule', rc.DELETE_RULE)
FROM information_schema.KEY_COLUMN_USAGE kcu
JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
 ON rc.CONSTRAINT_SCHEMA=kcu.CONSTRAINT_SCHEMA AND rc.CONSTRAINT_NAME=kcu.CONSTRAINT_NAME
WHERE kcu.CONSTRAINT_SCHEMA='{args.database}' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME, kcu.ORDINAL_POSITION
""")
    check_constraints = mysql_rows(args, f"""
SELECT JSON_OBJECT(
 'table', tc.TABLE_NAME, 'name', tc.CONSTRAINT_NAME,
 'clause', cc.CHECK_CLAUSE, 'enforced', tc.ENFORCED)
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.CHECK_CONSTRAINTS cc
 ON cc.CONSTRAINT_SCHEMA=tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME=tc.CONSTRAINT_NAME
WHERE tc.CONSTRAINT_SCHEMA='{args.database}' AND tc.CONSTRAINT_TYPE='CHECK'
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME
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
    return {
        "format_version": 2,
        "database": args.database,
        "tables": tables,
        "legacy_tables": [table for table in tables if table not in PLATFORM_TABLES],
        "table_options": table_options,
        "columns": columns,
        "indexes": indexes,
        "foreign_keys": foreign_keys,
        "check_constraints": check_constraints,
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
    parser = argparse.ArgumentParser(description="隔离 MySQL 副本的完整旧表结构快照与新增表白名单守护")
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
        differences = compare_snapshots(baseline, snapshot(args, allow_platform=True))
        if differences:
            for difference in differences:
                print(difference, file=sys.stderr)
            return 1
        print(f"校验通过：{len(baseline['legacy_tables'])} 张旧表的表选项、列、索引、外键、CHECK约束、触发器均未变化，且无非白名单新增表")
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError, json.JSONDecodeError) as error:
        print(f"schema 守护失败：{error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
