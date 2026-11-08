import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('php_lab_mapping', Path(__file__).resolve().parents[1] / 'configure-php-lab.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class MappingTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.target = Path(self.directory.name) / 'laboratory-accounts.json'
        self.account = {'username': 'local_lab', 'password': 'private-development-value', 'userId': 17}

    def test_preserves_other_mappings_private_backup_and_replay(self):
        self.assertTrue(module.write_mapping(self.target, 4, self.account))
        first = self.target.read_bytes()
        self.assertFalse(module.write_mapping(self.target, 4, self.account))
        self.assertEqual([], list(self.target.parent.glob('*.bak')))
        other = {**self.account, 'userId': 18, 'username': 'other_lab'}
        self.assertTrue(module.write_mapping(self.target, 5, other))
        data = json.loads(self.target.read_text())
        self.assertEqual(self.account, data['4'])
        self.assertEqual(other, data['5'])
        backup = list(self.target.parent.glob('*.bak'))[0]
        self.assertEqual(json.loads(first), json.loads(backup.read_text()))
        for path in (self.target, backup):
            self.assertEqual(0o600, path.stat().st_mode & 0o777)

    def test_bad_existing_or_public_file_is_never_overwritten(self):
        for text, mode in [('not json', 0o600), ('{}', 0o644), ('{"4":{"userId":17}}', 0o600)]:
            self.target.write_text(text)
            self.target.chmod(mode)
            with self.assertRaises((ValueError, json.JSONDecodeError)):
                module.write_mapping(self.target, 4, self.account)
            self.assertEqual(text, self.target.read_text())

    def test_symlink_target_is_rejected_without_changing_destination(self):
        destination = self.target.with_name('elsewhere')
        destination.write_text('{}')
        self.target.symlink_to(destination)
        with self.assertRaises(ValueError):
            module.write_mapping(self.target, 4, self.account)
        self.assertEqual('{}', destination.read_text())

    def test_invalid_credentials_and_ids_never_write(self):
        for account in ({**self.account, 'userId': True}, {**self.account, 'password': ''}, {**self.account, 'token': 'x'}):
            with self.assertRaises(ValueError):
                module.write_mapping(self.target, 4, account)
        with self.assertRaises(ValueError):
            module.write_mapping(self.target, -1, self.account)
        self.assertFalse(self.target.exists())

    def test_database_permission_failure_never_sends_credentials(self):
        for counts in (['0'], ['1', '0']):
            with patch.object(module, 'sql', side_effect=counts), patch.object(module.urllib.request, 'build_opener') as network:
                with self.assertRaises(ValueError):
                    module.verify_identity(4, 1, self.account)
                network.assert_not_called()

    def test_atomic_replace_failure_keeps_old_mapping(self):
        module.write_mapping(self.target, 4, self.account)
        before = self.target.read_bytes()
        with patch.object(module.os, 'replace', side_effect=OSError('simulated failure')):
            with self.assertRaises(OSError):
                module.write_mapping(self.target, 4, {**self.account, 'username': 'changed'})
        self.assertEqual(before, self.target.read_bytes())
        self.assertEqual([], list(self.target.parent.glob('.lab-mapping-*')))


if __name__ == '__main__':
    unittest.main()
