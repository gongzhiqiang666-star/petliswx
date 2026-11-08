"""Read-only isolated local DB -> offline schema probe. Original bodies remain in memory."""
import importlib.util,json,re,subprocess,sys
from pathlib import Path
if '--java-display' in sys.argv and '--php-display' not in sys.argv:raise SystemExit('Java corpus verification requires original PHP display reference')
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
bodies=[]
for query in ["SELECT HEX(IFNULL(ModelInfo,'')) FROM lin_lb_infotesttypemodel","SELECT HEX(IFNULL(model_content,'')) FROM lin_lb_dictmaildiagnosisreport"]:
 for line in staff.config.sql(query).splitlines():
  if line:
   text=bytes.fromhex(line).decode('utf-8')
   if '--php-display' not in sys.argv:text=re.sub(r'\\u([0-9a-fA-F]{4})',lambda m:chr(int(m[1],16)),text)
   bodies.append(text)
display_hash=None;java_proof=None
raw_bodies=list(bodies)
if '--php-display' in sys.argv:
 display_spec=importlib.util.spec_from_file_location('editor_display',Path(__file__).with_name('legacy-editor-display.py'));display=importlib.util.module_from_spec(display_spec);display_spec.loader.exec_module(display)
 bodies=display.original_decode(bodies);display_hash=display.source_hash()
if '--java-display' in sys.argv:
 java_spec=importlib.util.spec_from_file_location('java_display',Path(__file__).with_name('verify-legacy-java-display.py'));java=importlib.util.module_from_spec(java_spec);java_spec.loader.exec_module(java)
 java_proof=java.verify_java_display(raw_bodies,bodies,display_hash)
 Path('docs/php-parity/java-display-corpus-parity.json').write_text(json.dumps(java_proof,ensure_ascii=False,indent=2)+'\n')
result=subprocess.run(['node','scripts/editor-parity/browser-corpus-probe.js' if '--browser' in sys.argv else 'scripts/editor-parity/corpus-probe.js']+(['--compat'] if '--compat' in sys.argv else [])+(['--only-errors'] if '--only-errors' in sys.argv else [])+(['--only-text'] if '--only-text' in sys.argv else [])+(['--hierarchy'] if '--hierarchy' in sys.argv else [])+(['--layout'] if '--layout' in sys.argv else [])+[arg for arg in sys.argv if arg.startswith('--case-index=')],input=json.dumps(bodies),text=True,capture_output=True,timeout=240)
if result.returncode:raise RuntimeError('Offline editor probe failed; content withheld')
report=json.loads(result.stdout);report['inputStage']='original PHP decodeUnicode edit-view output' if display_hash else 'raw stored HTML with Unicode escape decoding only';report['originalDisplaySourceHash']=display_hash;report['javaDisplayParity']=java_proof;out=Path('docs/php-parity/editor-browser-layout-case-probe.json' if any(arg.startswith('--case-index=') for arg in sys.argv) else ('docs/php-parity/editor-browser-layout-probe.json' if '--php-display' in sys.argv else 'docs/php-parity/editor-browser-raw-layout-probe.json') if '--layout' in sys.argv else 'docs/php-parity/editor-browser-display-hierarchy-probe.json' if '--php-display' in sys.argv else 'docs/php-parity/editor-browser-hierarchy-probe.json' if '--hierarchy' in sys.argv else 'docs/php-parity/editor-browser-text-probe.json' if '--only-text' in sys.argv else 'docs/php-parity/editor-browser-errors-probe.json' if '--browser' in sys.argv and '--only-errors' in sys.argv else 'docs/php-parity/editor-browser-corpus-probe.json' if '--browser' in sys.argv else 'docs/php-parity/editor-errors-probe.json' if '--only-errors' in sys.argv else 'docs/php-parity/editor-corpus-compat-probe.json' if '--compat' in sys.argv else 'docs/php-parity/editor-corpus-probe.json');out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k not in ('missing','fontSamples','layoutSamples','textChanges')},ensure_ascii=False))
