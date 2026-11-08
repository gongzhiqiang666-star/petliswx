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

for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);key='legacy-hospital-funds'
if staff.config.sql("SELECT COUNT(*) FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0")=='0':
 staff.call('/admin/operations/configs',{'key':key,'value':{'enabled':False,'writeEnabled':False,'adminMappings':{}},'description':'PHP医院资金迁移模块，本机受控验收'},admin)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':False,'adminMappings':{}},'version':version,'reason':'医院资金只读页面验收临时开关'},'PUT')['code']==200
proof={'module':'M06','scope':'read-only PC/HTTP','businessWrites':False}
try:
 gate(True)
 b('newtab','http://127.0.0.1:4173/#/organizations');wait("!!document.querySelector('#login-form')||!!document.querySelector('#org-list')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout)
  b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('[data-tab=hospitals]')");b('click','[data-tab=hospitals]');wait("!!document.querySelector('[data-hospital-funds]')")
 hospital=int(b('js',"Number(document.querySelector('[data-hospital-funds]').dataset.hospitalFunds)"))
 b('click','[data-hospital-funds="'+str(hospital)+'"]');wait("!!document.querySelector('[data-fund-ledger]')")
 assert b('js',"document.body.textContent.includes('医院资金池')&&!document.querySelector('#modal-root [name=amount]')&&!document.querySelector('#modal-root [type=submit]')")=='true'
 summary=request('/admin/legacy/hospital-funds/'+str(hospital),admin);assert summary['code']==200 and summary['data']['fundPool']=='HOSPITAL'
 assert 'LoginPwd' not in summary['data'];proof['readPageAndSeparatePoolVisible']=True
 assert request('/admin/legacy/hospital-funds/'+str(hospital)+'/entries',admin,{'amount':'1.00','remark':'should not write','requestKey':'blocked_hfund_1234567890'},'POST')['code']==409;proof['readFlagCannotWrite']=True
 doctor=json.loads((root/'qa/native-sf-history-private.json').read_text());dt=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token']
 assert request('/admin/legacy/hospital-funds/'+str(hospital),dt)['code']==403;proof['doctorDenied']=True
 assert request('/account/hospital-wallets',dt)['data']==[]
 assert request('/account/hospital-wallets/'+str(hospital)+'/transactions',dt)['code']==403;proof['doctorCannotReadUnmanagedHospitalPool']=True
 b('screenshot','/private/tmp/petlis-hospital-funds-read.png');(root/'qa/hospital-funds-read.png').write_bytes(Path('/private/tmp/petlis-hospital-funds-read.png').read_bytes())
finally:
 gate(False);b('reload');wait("!!document.querySelector('#org-list')");b('click','[data-tab=hospitals]');wait("!!document.querySelector('[data-hospital]')")
 assert b('js',"!document.querySelector('[data-hospital-funds]')")=='true';proof['gateRestoredHidden']=True
 assert request('/admin/legacy/hospital-funds/'+str(hospital),admin)['code']==409
staff.save_private(root/'qa/hospital-funds-read-ui.json',proof);print(json.dumps(proof))
