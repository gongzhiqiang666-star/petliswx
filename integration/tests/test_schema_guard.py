import copy
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "schema_guard.py"


def load_guard():
    if not SCRIPT.is_file():
        return None
    spec = importlib.util.spec_from_file_location("schema_guard", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class SchemaGuardTest(unittest.TestCase):
    def setUp(self):
        self.guard = load_guard()
        self.baseline = {
            "database": "petlis_copy",
            "tables": ["lin_user", "lin_lb_listdiagnosisproject"],
            "legacy_tables": ["lin_user", "lin_lb_listdiagnosisproject"],
            "table_options": [{"table": "lin_user", "engine": "InnoDB", "collation": "utf8mb4_unicode_ci"}],
            "columns": [{"table": "lin_user", "name": "id", "type": "int"}],
            "indexes": [{"table": "lin_user", "name": "PRIMARY", "column": "id"}],
            "triggers": [{"table": "lin_lb_listdiagnosisproject", "name": "legacy_trigger", "sql": "SET NEW.x = 1"}],
            "foreign_keys": [{"table": "lin_lb_listdiagnosisproject", "name": "fk_hospital", "column": "hospital_id"}],
            "check_constraints": [{"table": "lin_user", "name": "ck_active", "clause": "`is_active` in (0,1)"}],
        }

    def test_guard_script_exists(self):
        self.assertIsNotNone(self.guard, "缺少 schema_guard.py")

    def test_only_loopback_explicit_isolated_copy_is_allowed(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        with self.assertRaises(ValueError):
            self.guard.validate_connection("mac-mini", 33306, True)
        with self.assertRaises(ValueError):
            self.guard.validate_connection("127.0.0.1", 3306, True)
        with self.assertRaises(ValueError):
            self.guard.validate_connection("127.0.0.1", 33306, False)
        self.guard.validate_connection("127.0.0.1", 33306, True)

    def test_compare_accepts_only_new_platform_tables(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        current = copy.deepcopy(self.baseline)
        current["tables"].append("lin_shipment")
        current["columns"].append({"table": "lin_shipment", "name": "id", "type": "bigint"})
        current["indexes"].append({"table": "lin_shipment", "name": "PRIMARY", "column": "id"})
        self.assertEqual([], self.guard.compare_snapshots(self.baseline, current))

    def test_compare_rejects_legacy_column_index_and_trigger_changes(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        for section, changed in (
            ("columns", {"table": "lin_user", "name": "id", "type": "bigint"}),
            ("indexes", {"table": "lin_user", "name": "PRIMARY", "column": "other_id"}),
            ("triggers", {"table": "lin_lb_listdiagnosisproject", "name": "legacy_trigger", "sql": "SET NEW.x = 2"}),
        ):
            with self.subTest(section=section):
                current = copy.deepcopy(self.baseline)
                current[section][0] = changed
                self.assertTrue(self.guard.compare_snapshots(self.baseline, current))

    def test_compare_rejects_dropped_legacy_table_or_new_trigger_on_legacy_table(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        dropped = copy.deepcopy(self.baseline)
        dropped["columns"] = []
        self.assertTrue(self.guard.compare_snapshots(self.baseline, dropped))
        added_trigger = copy.deepcopy(self.baseline)
        added_trigger["triggers"].append({"table": "lin_user", "name": "unexpected", "sql": "SET NEW.x = 2"})
        self.assertTrue(self.guard.compare_snapshots(self.baseline, added_trigger))

    def test_compare_rejects_every_unapproved_new_table(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        for table in ("users", "test_orders", "lin_unapproved_workflow"):
            with self.subTest(table=table):
                current = copy.deepcopy(self.baseline)
                current["tables"].append(table)
                current["columns"].append({"table": table, "name": "id", "type": "int"})
                differences = self.guard.compare_snapshots(self.baseline, current)
                self.assertTrue(any("未批准新增表" in difference and table in difference for difference in differences))

    def test_baseline_must_not_contain_platform_tables(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        baseline = copy.deepcopy(self.baseline)
        baseline["tables"].append("lin_shipment")
        baseline["legacy_tables"].append("lin_shipment")
        current = copy.deepcopy(baseline)
        differences = self.guard.compare_snapshots(baseline, current)
        self.assertTrue(any("基线已含平台表" in difference for difference in differences))

    def test_compare_rejects_legacy_table_option_foreign_key_and_check_changes(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        for section in ("table_options", "foreign_keys", "check_constraints"):
            with self.subTest(section=section):
                current = copy.deepcopy(self.baseline)
                current[section][0] = dict(current[section][0], changed=True)
                differences = self.guard.compare_snapshots(self.baseline, current)
                self.assertTrue(any(section == "table_options" and "表选项" in difference
                                    or section == "foreign_keys" and "外键" in difference
                                    or section == "check_constraints" and "CHECK" in difference
                                    for difference in differences))

    def test_compare_rejects_dropped_legacy_table_even_without_column_rows(self):
        self.assertIsNotNone(self.guard)
        if self.guard is None:
            return
        current = copy.deepcopy(self.baseline)
        current["tables"].remove("lin_user")
        current["columns"] = []
        current["table_options"] = []
        self.assertTrue(any("旧表缺失" in difference for difference in self.guard.compare_snapshots(self.baseline, current)))


if __name__ == "__main__":
    unittest.main()
