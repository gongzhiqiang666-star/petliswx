"""Normal new-system login, capability gate and read-only original dictionary UI. No PHP business bridge."""
import importlib.util,json,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 p=subprocess.run([B,*args],capture_output=True,text=True);assert p.returncode==0,'Local dictionary browser action failed';return p.stdout.strip()
def wait(expr):
 for _ in range(80):
  if b('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Dictionary UI did not reach expected state')
def request(path,token,data=None,method=None):
 req=urllib.request.Request(staff.config.BASE+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method=method)
 try:
  with staff.opener.open(req,timeout=15) as response:r=json.load(response)
 except urllib.error.HTTPError as error:
  raw=error.read()
  try:r=json.loads(raw)
  except (ValueError,UnicodeError):r={'code':error.code}
  if not isinstance(r,dict):r={}
  r['code']=error.code
 return r

import secrets
checkpoint=root/'qa/dictionary-maintenance-private.json'
state=json.loads(checkpoint.read_text()) if checkpoint.exists() else {'prefix':'QA_MAINT_'+secrets.token_hex(4),'rows':{}}
assert not state.get('completed'),'Completed evidence exists; do not replay disposable writes'
staff.save_private(checkpoint,state)
admin=staff.administrator(4)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"))
 flags={'enabled':enabled,'writeEnabled':enabled,'deleteEnabled':enabled}
 assert request('/admin/operations/configs/legacy-dictionaries',admin,{'value':flags,'version':version,'reason':'本机采样类型/品种维护验收临时开关'},'PUT')['code']==200
proof={'scope':'sampleType/variety own disposable maintenance rows','realBusinessRowsTouched':False}
try:
 gate(True)
 b('newtab','http://127.0.0.1:4173/#/legacy-dictionaries');wait("!!document.querySelector('#login-form')||!!document.querySelector('#dictionary-filter')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  result=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(result.stdout)
  b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('#dictionary-filter')")
 for kind,pk,namefield in [('sampleType','InfoSampleTypeId','SampleName'),('variety','IID','Variety_CN')]:
  name=state['prefix']+'_'+kind
  b('click','[data-kind='+kind+']');wait("!!document.querySelector('#dictionary-list table')")
  if kind=='variety':assert b('js',"!document.querySelector('[name=active]')&&!document.querySelector('[data-batch-status]')")=='true'
  existing=request('/admin/legacy/dictionaries/'+kind+'?name='+name,admin);assert existing['code']==200
  rows=[r for r in existing['data']['items'] if r[namefield] in [name,name+' edited']]
  if not rows:
   b('click','[data-dictionary-new]');wait("!!document.querySelector('#active-dialog-form')")
   if kind=='variety':assert b('js',"!document.querySelector('#active-dialog-form [name=active]')")=='true'
   b('fill','#active-dialog-form [name=name]',name);b('fill','#active-dialog-form [name=englishName]',name+' EN')
   b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
   rows=request('/admin/legacy/dictionaries/'+kind+'?name='+name,admin)['data']['items']
  assert len(rows)==1;rid=int(rows[0][pk]);state['rows'][kind]=rid;staff.save_private(checkpoint,state)
  assert rows[0][namefield] in [name,name+' edited']
  if kind=='sampleType':assert rows[0]['IsActive']==0
  else:assert 'IsActive' not in rows[0]
  b('fill','#dictionary-filter [name=name]',name);b('click','#dictionary-filter button[type=submit]');wait("document.querySelectorAll('[data-dictionary-edit]').length===1")
  b('click',f'[data-dictionary-edit="{rid}"]');wait("!!document.querySelector('#active-dialog-form')")
  b('fill','#active-dialog-form [name=name]',name+' edited');b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
  saved=request('/admin/legacy/dictionaries/'+kind+'/'+str(rid),admin);assert saved['code']==200 and saved['data'][namefield]==name+' edited'
  b('click',f'[data-dictionary-delete="{rid}"]');wait("!!document.querySelector('#active-dialog-form')")
  assert b('js','document.querySelector("#active-dialog-form").textContent.includes('+json.dumps(name+' edited')+')')=='true'
  if kind=='variety':assert b('js',"!document.querySelector('#active-dialog-form').textContent.includes('当前状态')")=='true'
  b('screenshot','/private/tmp/petlis-maintenance-'+kind+'.png');(root/('qa/dictionary-maintenance-'+kind+'.png')).write_bytes(Path('/private/tmp/petlis-maintenance-'+kind+'.png').read_bytes())
  b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
  assert request('/admin/legacy/dictionaries/'+kind+'/'+str(rid),admin)['code']==404
  proof[kind]={'id':rid,'createdEditedDeletedViaPc':True,'originalStatusShape':True}
finally:
 gate(False);b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyDictionaries]')");proof['allGatesRestoredDisabled']=True
state['completed']=True;staff.save_private(checkpoint,state);staff.save_private(root/'qa/dictionary-maintenance-ui.json',proof);print(json.dumps(proof))
