"""Verify separate read/write switches and a no-change edit of the existing exact synthetic record."""
import importlib.util,json,secrets,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';fixture=json.loads((root/'qa/legacy-dictionary-write-private.json').read_text());assert fixture['completed'];rid=int(fixture['records']['species']);prefix=fixture['prefix']+'_species';assert prefix.startswith('QA_DICT_UI_')
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Dictionary gate browser action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(80):
  if b('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Dictionary gate UI readiness timeout')
def req(path,data=None,method=None):
 request=urllib.request.Request(staff.config.BASE+path,data=None if data is None else json.dumps(data).encode(),headers={'Authorization':'Bearer '+admin,'Content-Type':'application/json'},method=method)
 try:
  with staff.opener.open(request,timeout=15) as r:return json.load(r)
 except urllib.error.HTTPError as error:
  try:r=json.loads(error.read())
  except (ValueError,UnicodeError):r={}
  r['code']=error.code;return r
for _ in range(30):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4)
assert staff.config.sql("SELECT AnimalName FROM lin_lb_infoanimal WHERE InfoAnimalId="+str(rid))==prefix
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(read,write):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"));assert req('/admin/operations/configs/legacy-dictionaries',{'value':{'enabled':read,'writeEnabled':write},'version':version,'reason':'本机验证字典读写能力隔离；结束恢复关闭'},'PUT')['code']==200
proof={'module':'M07','scope':'separate gates and no-op edit subset','speciesId':rid}
try:
 gate(True,False);b('newtab','http://127.0.0.1:4173/#/legacy-dictionaries');wait("!!document.querySelector('#login-form')||!!document.querySelector('#dictionary-filter')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);a=json.loads(r.stdout);b('fill','[name=username]',a['username']);b('fill','[name=password]',a['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('#dictionary-filter')")
 assert req('/admin/legacy/capabilities')['data']=={'dictionaries':True,'dictionaryWrites':False,'dictionaryDeletes':False}
 assert b('js',"!document.querySelector('[data-dictionary-new]')&&!document.querySelector('[data-dictionary-edit]')")=='true'
 blocked={'name':prefix+'_blocked','englishName':'Blocked','active':0,'requestKey':'blocked_'+secrets.token_hex(12)}
 assert req('/admin/legacy/dictionaries/species',blocked,'POST')['code']==409;proof['readOnlyDoesNotGrantWrites']=True
 gate(True,'true');assert req('/admin/legacy/capabilities')['data']['dictionaryWrites'] is False;assert req('/admin/legacy/dictionaries/species',blocked,'POST')['code']==409;proof['stringWriteFlagRejected']=True
 gate(True,True);b('reload');wait("!!document.querySelector('[data-dictionary-new]')")
 before=req(f'/admin/legacy/dictionaries/species/{rid}')['data'];assert before['AnimalName']==prefix and before['IsActive']==0
 b('fill','#dictionary-filter [name=name]',prefix);b('click','#dictionary-filter button[type=submit]');wait(f"!!document.querySelector('[data-dictionary-edit=\"{rid}\"]')")
 b('click',f'[data-dictionary-edit="{rid}"]');wait("!!document.querySelector('#active-dialog-form')");b('screenshot','/private/tmp/petlis-dictionary-write-final.png');(root/'qa/dictionary-write-final.png').write_bytes(Path('/private/tmp/petlis-dictionary-write-final.png').read_bytes());b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 assert req(f'/admin/legacy/dictionaries/species/{rid}')['data']==before;proof['normalPcNoopPreservesRecordAndTimestamp']=True
 assert staff.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY' AND resource_id='species:{rid}'")=='3'
finally:
 gate(False,False);b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyDictionaries]')");proof['bothGatesRestoredDisabled']=True
proof['noNewDictionaryRecords']=True;staff.save_private(root/'qa/legacy-dictionary-write-gates.json',proof);print(json.dumps(proof))
