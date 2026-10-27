import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]


class MergeE2EContractTest(unittest.TestCase):
    def test_repeatable_isolated_entrypoint_and_drivers_exist(self):
        script = ROOT / "integration/run-merge-e2e.sh"
        self.assertTrue(script.is_file(), "缺少可重复集成验收入口")
        self.assertTrue((ROOT / "integration/merge-e2e.js").is_file())
        self.assertTrue((ROOT / "integration/db/legacy-fixture.sql").is_file())
        text = script.read_text(encoding="utf-8")
        self.assertIn("--bind-address=127.0.0.1", text)
        self.assertRegex(text, r'SOCKET="/tmp/petlis-merge-\$\$\.sock"')
        self.assertIn("DB_MIGRATIONS_ENABLED=false", text)
        self.assertRegex(text, r'schema_guard\.py" snapshot')
        self.assertRegex(text, r'schema_guard\.py" verify')

    def test_driver_loads_actual_miniprogram_and_real_browser(self):
        text = (ROOT / "integration/merge-e2e.js").read_text(encoding="utf-8")
        self.assertIn("miniprogram/utils/request.js", text)
        self.assertIn("chromium.launch", text)
        self.assertIn("pageerror", text)
        self.assertIn("app.json", text)
        self.assertIn("mock-callback", text)


if __name__ == "__main__":
    unittest.main()
