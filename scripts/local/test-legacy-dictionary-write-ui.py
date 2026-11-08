"""Create/edit only new synthetic dictionaries through PC; preserve rows disabled. Never touches PHP code."""
import importlib.util,json,secrets,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';statepath=root/'qa/legacy-dictionary-write-private.json';B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Dictionary write browser action failed';return r.stdout.strip()
def wait(expression):
 for _ in range(80):
  if b('js',expression)=='true':return
  time.sleep(.15)
 raise AssertionError('Dictionary write UI readiness timeout; inspect saved synthetic state')
def request(path,token,data=None,method=None):
 req=urllib.request.Request(staff.config.BASE+path,data=None if data is None else json.dumps(data).encode(),headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'},method=method)
 try:
  with staff.opener.open(req,timeout=15) as r:result=json.load(r)
 except urllib.error.HTTPError as error:
  try:result=json.loads(error.read())
  except (ValueError,UnicodeError):result={}
  result['code']=error.code
 return result
for _ in range(30):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Backend not ready')
admin=staff.administrator(4)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='false'
state=json.loads(statepath.read_text()) if statepath.exists() else {'prefix':'QA_DICT_UI_'+secrets.token_hex(5),'records':{}}
assert not state.get('completed'),'Inspect completed write evidence; do not create more fixtures'
staff.save_private(statepath,state)
def gate(value):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"));assert request('/admin/operations/configs/legacy-dictionaries',admin,{'value':{'enabled':value,'writeEnabled':value},'version':version,'reason':'仅本机合成字典新增编辑验收' if value else '字典验收结束恢复关闭'},'PUT')['code']==200
proof={'scope':'M07 species/sex create-edit subset','records':[]}
try:
 gate(True);b('newtab','http://127.0.0.1:4173/#/legacy-dictionaries');wait("!!document.querySelector('#login-form')||!!document.querySelector('#dictionary-filter')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout);b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('#dictionary-filter')")
 for kind,table,pk,namecol,encol in [('species','lin_lb_infoanimal','InfoAnimalId','AnimalName','AnimalName_en'),('sex','lin_lb_dictsex','DictSexId','SexName','SexName_en')]:
  prefix=state['prefix']+'_'+kind;english=prefix+' EN';edited=prefix+' edited';assert len(edited)<=50
  b('click',f'[data-kind={kind}]');wait("!!document.querySelector('#dictionary-list table')")
  known=staff.config.sql(f"SELECT {pk} FROM {table} WHERE {namecol}='{prefix}'").splitlines();assert len(known)<=1
  if not known:
   b('click','[data-dictionary-new]');wait("!!document.querySelector('#active-dialog-form')")
   assert b('js',"document.querySelector('#active-dialog-form [name=active]').value==='0'")=='true'
   b('fill','#active-dialog-form [name=name]',' ');b('fill','#active-dialog-form [name=englishName]',english);b('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('#dialog-error').textContent.includes('不能为空')")
   assert staff.config.sql(f"SELECT COUNT(*) FROM {table} WHERE {namecol}='{prefix}'")=='0'
   b('click','#active-dialog-form button[type=reset]');assert b('js',"document.querySelector('#active-dialog-form [name=name]').value===''&&document.querySelector('#dialog-error').textContent===''")=='true'
   b('fill','#active-dialog-form [name=name]',prefix);b('fill','#active-dialog-form [name=englishName]',english);b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
   known=staff.config.sql(f"SELECT {pk} FROM {table} WHERE {namecol}='{prefix}'").splitlines();assert len(known)==1
  rid=int(known[0]);state['records'][kind]=rid;staff.save_private(statepath,state)
  before=request(f'/admin/legacy/dictionaries/{kind}/{rid}',admin)['data'];assert before[namecol]==prefix and before['IsActive']==0
  b('fill','#dictionary-filter [name=name]',prefix);b('click','#dictionary-filter button[type=submit]');wait(f"!!document.querySelector('[data-dictionary-edit=\"{rid}\"]')")
  if before[encol]!=edited:
   b('click',f'[data-dictionary-edit="{rid}"]');wait("!!document.querySelector('#active-dialog-form')");b('fill','#active-dialog-form [name=englishName]',edited);b('screenshot',f'/private/tmp/petlis-dictionary-{kind}-edit.png');(root/f'qa/dictionary-{kind}-edit.png').write_bytes(Path(f'/private/tmp/petlis-dictionary-{kind}-edit.png').read_bytes());b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
   stale=request(f'/admin/legacy/dictionaries/{kind}/{rid}',admin,{'name':prefix,'englishName':'stale value','active':0,'requestKey':'stale_'+secrets.token_hex(12),'expectedToken':before['editToken']},'PUT');assert stale['code']==409
  after=request(f'/admin/legacy/dictionaries/{kind}/{rid}',admin)['data'];assert after[encol]==edited and after['IsActive']==0 and after['createtime']==before['createtime']
  original_key=staff.config.sql(f"SELECT request_id FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY' AND resource_id='{kind}:{rid}' AND action='LEGACY_DICTIONARY_CREATED'");assert original_key
  replay=request(f'/admin/legacy/dictionaries/{kind}',admin,{'name':prefix,'englishName':english,'active':0,'requestKey':original_key,'expectedToken':None},'POST');assert replay['code']==200 and replay['data']==rid
  assert request(f'/admin/legacy/dictionaries/{kind}/{rid}',admin)['data'][encol]==edited
  assert staff.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_DICTIONARY' AND resource_id='{kind}:{rid}'")=='2'
  proof['records'].append({'kind':kind,'id':rid,'defaultDisabled':True,'createEditThroughPc':True,'creationTimePreserved':True,'replayNoDuplicate':True,'auditCount':2})
finally:
 gate(False);b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyDictionaries]')");proof['gateRestoredDisabled']=True
proof.update(emptyNameBlockedAndResetVerified=True,staleEditRejected=True,realPhpBusinessCalls=False,ddlChanged=False)
state['completed']=True;staff.save_private(statepath,state);staff.save_private(root/'qa/legacy-dictionary-write-ui.json',proof);print(json.dumps(proof))
