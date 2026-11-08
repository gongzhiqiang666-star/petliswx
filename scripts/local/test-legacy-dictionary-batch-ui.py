"""Exercise batch status and deletion only on two newly created, disposable local dictionary fixtures."""
import importlib.util,json,secrets,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/dictionary-batch-private.json';B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Synthetic dictionary browser action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(80):
  if b('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Batch UI readiness timeout; inspect persistent checkpoint before resuming')
def request(url,data=None,method=None):
 req=urllib.request.Request(staff.config.BASE+url,data=None if data is None else json.dumps(data).encode(),headers={'Authorization':'Bearer '+admin,'Content-Type':'application/json'},method=method)
 try:
  with staff.opener.open(req,timeout=15) as r:return json.load(r)
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
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='false'
state=json.loads(path.read_text()) if path.exists() else {'prefix':'QA_DICT_BATCH_'+secrets.token_hex(5),'keys':[secrets.token_hex(16),secrets.token_hex(16)],'ids':[]}
assert not state.get('completed'),'Completed batch proof exists; do not create/delete more rows';staff.save_private(path,state)
def gate(delete):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"));assert request('/admin/operations/configs/legacy-dictionaries',{'value':{'enabled':True,'writeEnabled':True,'deleteEnabled':delete},'version':version,'reason':'本机两条新建合成字典批量/删除验收'},'PUT')['code']==200
def close_gate():
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"));assert request('/admin/operations/configs/legacy-dictionaries',{'value':{'enabled':False,'writeEnabled':False,'deleteEnabled':False},'version':version,'reason':'批量字典验证结束恢复全部关闭'},'PUT')['code']==200
def records():
 result=[]
 for rid in state['ids']:
  row=request(f'/admin/legacy/dictionaries/species/{rid}');assert row['code']==200;row=row['data'];assert row['AnimalName'] in [state['prefix']+'_1',state['prefix']+'_2'];result.append(row)
 return result
def select_rows():
 for rid in state['ids']:b('click',f'[data-dictionary-select="{rid}"]')
 assert b('js',"document.querySelector('[data-selected-count]').textContent.includes('2')")=='true'
proof={'scope':'M07 two disposable local species rows','realBusinessRowsTouched':False}
try:
 gate(False)
 if not state['ids']:
  for i in range(2):
   r=request('/admin/legacy/dictionaries/species',{'name':state['prefix']+'_'+str(i+1),'englishName':state['prefix']+' EN '+str(i+1),'active':0,'requestKey':state['keys'][i]},'POST');assert r['code']==200;state['ids'].append(int(r['data']))
  staff.save_private(path,state)
 assert len(state['ids'])==2
 b('newtab','http://127.0.0.1:4173/#/legacy-dictionaries');wait("!!document.querySelector('#login-form')||!!document.querySelector('#dictionary-filter')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout);b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('#dictionary-filter')")
 assert b('js',"!document.querySelector('[data-batch-delete]')&&!document.querySelector('[data-dictionary-delete]')")=='true'
 targets=[{'id':r['InfoAnimalId'],'editToken':r['editToken']} for r in records()]
 assert request('/admin/legacy/dictionaries/species/delete',{'targets':targets,'active':None,'requestKey':'blocked_'+secrets.token_hex(12)},'POST')['code']==409;proof['writeSwitchAloneCannotDelete']=True
 b('fill','#dictionary-filter [name=name]',state['prefix']);b('click','#dictionary-filter button[type=submit]');wait(f"document.querySelectorAll('[data-dictionary-select]').length===2")
 for active in [1,0]:
  if not all(row['IsActive']==active for row in records()):
   select_rows();b('click',f'[data-batch-status="{active}"]');wait("!!document.querySelector('#active-dialog-form')");assert b('js','document.querySelector("#active-dialog-form").textContent.includes('+json.dumps(state['prefix'])+')')=='true';b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
  assert all(row['IsActive']==active for row in records())
 proof['bulkNormalAndDisabledViaPc']=True
 select_rows();b('click','[data-batch-status="0"]');wait("!!document.querySelector('#active-dialog-form')");b('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('#dialog-error').textContent.includes('未更新任何行')");b('press','Escape');wait("!document.querySelector('#active-dialog-form')");proof['noChangeKeepsOriginalPhpError']=True
 gate(True);b('reload');wait("!!document.querySelector('[data-batch-delete]')");b('fill','#dictionary-filter [name=name]',state['prefix']);b('click','#dictionary-filter button[type=submit]');wait("document.querySelectorAll('[data-dictionary-select]').length===2")
 targets=[{'id':r['InfoAnimalId'],'editToken':r['editToken']} for r in records()];select_rows();b('click','[data-batch-delete]');wait("!!document.querySelector('#active-dialog-form')");assert b('js',"document.querySelector('#active-dialog-form').textContent.includes('永久删除')")=='true'
 b('screenshot','/private/tmp/petlis-dictionary-delete-confirm.png');(root/'qa/dictionary-delete-confirm.png').write_bytes(Path('/private/tmp/petlis-dictionary-delete-confirm.png').read_bytes())
 state['deleteTargets']=targets;state['deletionDispatchStarted']=True;staff.save_private(path,state)
 b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 for rid in state['ids']:assert request(f'/admin/legacy/dictionaries/species/{rid}')['code']==404
 ids=state['ids'];raw=staff.config.sql(f"SELECT JSON_OBJECT('request',request_id,'after',after_json) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY' AND action='LEGACY_DICTIONARY_DELETED' AND JSON_CONTAINS(after_json,'{json.dumps(ids)}','$.targetIds')")
 audits=[json.loads(line) for line in raw.splitlines() if line];assert len(audits)==1;assert sorted(audits[0]['after']['targetIds'])==sorted(ids)
 replay=request('/admin/legacy/dictionaries/species/delete',{'targets':targets,'active':None,'requestKey':audits[0]['request']},'POST');assert replay['code']==200 and replay['data']['count']==2
 proof.update(ids=ids,physicalDeleteViaPc=True,deleteReplayCount=2,deletedOnlyCreatedFixtures=True,completed=True)
finally:
 close_gate();b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyDictionaries]')");proof['allGatesRestoredDisabled']=True
state['completed']=True;staff.save_private(path,state);staff.save_private(root/'qa/dictionary-batch-ui.json',proof);print(json.dumps(proof))
