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
            "legacy_tables": ["lin_user", "lin_lb_listdiagnosisproject"],
            "columns": [{"table": "lin_user", "name": "id", "type": "int"}],
            "indexes": [{"table": "lin_user", "name": "PRIMARY", "column": "id"}],
            "triggers": [{"table": "lin_lb_listdiagnosisproject", "name": "legacy_trigger", "sql": "SET NEW.x = 1"}],
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


if __name__ == "__main__":
    unittest.main()
