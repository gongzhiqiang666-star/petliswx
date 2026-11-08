import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('staff_setup',Path(__file__).resolve().parents[1]/'setup-php-lab.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class StaffSetupTest(unittest.TestCase):
    def test_private_credentials_are_atomic_and_not_public(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'credentials.json'
            module.save_private(path,{'username':'test_only','password':'synthetic-value'})
            self.assertEqual(path.stat().st_mode&0o777,0o600)
            self.assertEqual(module.config.read_private(path)['username'],'test_only')
            link=Path(folder)/'link.json';link.symlink_to(path)
            with self.assertRaises(ValueError):module.save_private(link,{})
    def test_extra_session_roles_are_rejected(self):
        with patch.object(module.config,'verify_identity'),patch.object(module,'call',return_value={'roles':['LAB_STAFF','ADMIN']}):
            with self.assertRaises(ValueError):module.verify(4,1,{'userId':1,'username':'test_only','password':'synthetic-value'})
    def test_successful_void_command_is_not_misreported_as_a_failed_write(self):
        for response in [b'{"code":200}', b'{"code":200,"data":null}']:
            with patch.object(module.opener,'open',return_value=io.BytesIO(response)):
                self.assertIsNone(module.call('/report-workflows/1/review',{},'synthetic-token'))
        with patch.object(module.opener,'open',return_value=io.BytesIO(b'{"code":409}')):
            with self.assertRaises(ValueError):module.call('/report-workflows/1/review',{},'synthetic-token')
    def test_invalid_scope_never_reads_credentials_or_queries_database(self):
        with patch.object(module.config,'sql') as sql:
            with self.assertRaises(ValueError):module.setup(0,4)
            sql.assert_not_called()
if __name__=='__main__':unittest.main()
