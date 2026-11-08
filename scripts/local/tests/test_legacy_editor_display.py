import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('legacy_editor_display',Path(__file__).parents[1]/'legacy-editor-display.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class LegacyEditorDisplayTest(unittest.TestCase):
 def test_original_helper_repairs_old_font_markup_and_decodes_unicode(self):
  bad='<span style="font-family: " microsoft="" yahei";="" font-size:="" 16px;"="">测试</span>'
  self.assertEqual(module.original_decode([r'\u732b',bad]),['猫','<span style="font-family:Microsoft YaHei;font-size:16px">测试</span>'])
