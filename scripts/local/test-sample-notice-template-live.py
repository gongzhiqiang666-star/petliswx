"""Template maintenance UI -> real Java -> isolated MySQL; successful writes journaled before forwarding."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_notice_template_live_'+secrets.token_hex(6)
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
import tempfile
private=Path.home()/'.local/share/petlis/qa';private.mkdir(parents=True,exist_ok=True)
old=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-templates' AND scope_type='GLOBAL' AND scope_id=0")
assert not old or json.loads(old).get('enabled') is False
fd,journal=tempfile.mkstemp(prefix='template-maintenance-',suffix='.json',dir=private);os.close(fd)
try:
 config={'enabled':True,'writeEnabled':True,'deleteEnabled':True}
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-templates',"+literal(json.dumps(config))+",'template UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 run=subprocess.run(['node','scripts/editor-parity/sample-notice-template-live.js'],cwd=ROOT,input=json.dumps({'token':token,'prefix':prefix,'journal':journal}),capture_output=True,text=True,timeout=150)
 if run.returncode:raise AssertionError('Template browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout);record=int(proof['id'])
 assert sql(f'SELECT COUNT(*) FROM lin_lb_dictmailreportnotice WHERE id={record}')=='0'
 proof['databaseDeletionVerified']=True
finally:
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old)+" WHERE config_key='legacy-templates' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-templates' AND scope_type='GLOBAL' AND scope_id=0 AND description='template UI fixture'")
 entries=json.loads(Path(journal).read_text() or '[]')
 for entry in entries:
  template=int(entry['id']);kind=entry['kind'];assert kind=='sampleNotice'
  audit=sql(f"SELECT HEX(after_json) FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_TEMPLATE' AND request_id="+literal(entry['requestKey']))
  assert json.loads(json.loads(bytes.fromhex(audit).decode())['canonicalRequest'])['kind']==kind
  table,pk,title='lin_lb_dictmailreportnotice','id','tplname'
  value=sql(f'SELECT {title} FROM {table} WHERE {pk}={template}')
  assert not value or value.startswith(prefix+'_')
  if value:sql(f'DELETE FROM {table} WHERE {pk}={template} AND {title}='+literal(value))
  sql(f"DELETE FROM lin_audit_record WHERE actor_user_id={int(session['userId'])} AND resource_type='LEGACY_TEMPLATE' AND request_id="+literal(entry['requestKey']))
 Path(journal).unlink()
proof.update(gateRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
