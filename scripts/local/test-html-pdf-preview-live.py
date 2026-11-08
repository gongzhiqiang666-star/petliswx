"""Template maintenance UI -> real Java -> isolated MySQL; successful writes journaled before forwarding."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_pdf_live_'+secrets.token_hex(6)
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
# Read this project's existing local mapping without requiring the PHP container.
import re,os,stat
mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php'
info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077
content=mapping.read_text();assert re.search(r"\b4\s*=>",content)
credentials={}
for key in ['username','password']:
 match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",content)
 assert match,'Private mapping format unavailable'
 credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']})
assert 'ADMIN' in session['roles'];token=session['token']
old={}
for key in ['legacy-templates','legacy-html-pdf']:
 value=sql("SELECT config_value FROM lin_operational_config WHERE config_key="+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
 assert not value or json.loads(value).get('enabled') is False
 old[key]=value
output=Path.home()/'.local/share/petlis/reports/qa-html-pdf/http-preview.pdf';output.parent.mkdir(parents=True,exist_ok=True)
try:
 for key in old:
  config={'enabled':True,'writeEnabled':key=='legacy-templates'}
  sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES("+literal(key)+','+literal(json.dumps(config))+",'PDF preview UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 run=subprocess.run(['node','scripts/editor-parity/html-pdf-preview-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'output':str(output)}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('PDF preview browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 text=subprocess.run(['pdftotext',str(output),'-'],capture_output=True,text=True,check=True).stdout
 assert '12.50' in text and 'END' in text
 assert sql('SELECT COUNT(*) FROM lin_lb_infotesttypemodel WHERE ModelName='+literal(prefix))=='0'
finally:
 for key,value in old.items():
  if value:sql('UPDATE lin_operational_config SET config_value='+literal(value)+' WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+literal(key)+" AND scope_type='GLOBAL' AND scope_id=0 AND description='PDF preview UI fixture'")
proof.update(gatesRestored=True)
print(json.dumps(proof))
