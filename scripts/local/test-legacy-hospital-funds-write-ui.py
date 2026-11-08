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
checkpoint=root/'qa/hospital-funds-write-private.json'
state=json.loads(checkpoint.read_text()) if checkpoint.exists() else {'name':'!QA_HFUNDS_'+secrets.token_hex(5)}
assert not state.get('completed'),'Completed fixture must not be replayed'
staff.save_private(checkpoint,state)
admin=staff.administrator(4)
r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout);actor=int(account['userId'])
key='legacy-hospital-funds';assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0")=='false'
name=state['name'];assert name.startswith('!QA_HFUNDS_') and all(c.isalnum() or c in '!_' for c in name)
ids=staff.config.sql("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE HospitalName='"+name+"'").splitlines()
if not ids:
 staff.config.sql("INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount,IsActive) VALUES('"+name+"','"+name+"',0,10.00,1)")
 ids=staff.config.sql("SELECT InfoHospitalId FROM lin_lb_infohospital WHERE HospitalName='"+name+"'").splitlines()
assert len(ids)==1;hospital=int(ids[0]);state['hospitalId']=hospital;staff.save_private(checkpoint,state)
personal=staff.config.sql('SELECT money FROM lin_user WHERE id='+str(actor))
def gate(enabled):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-hospital-funds' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':enabled,'adminMappings':{str(actor):4} if enabled else {}},'version':version,'reason':'本机临时医院资金页面验收'},'PUT')['code']==200
def balance():return request('/admin/legacy/hospital-funds/'+str(hospital),admin)['data']['amount']
def open_funds():
 b('click','[data-hospital-funds="'+str(hospital)+'"]');wait("!!document.querySelector('#active-dialog-form [name=amount]')")
proof={'scope':'one disposable local hospital','realHospitalBalancesTouched':False}
try:
 gate(True)
 b('newtab','http://127.0.0.1:4173/#/organizations');wait("!!document.querySelector('#login-form')||!!document.querySelector('#org-list')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('[data-tab=hospitals]')");b('click','[data-tab=hospitals]');wait('!!document.querySelector('+json.dumps('[data-hospital-funds="'+str(hospital)+'"]')+')')
 assert str(balance()) in ['10.0','10.00','10']
 for amount,expected in [('12.50','22.50'),('-5.00','17.50')]:
  open_funds();b('fill','#active-dialog-form [name=amount]',amount);b('fill','#active-dialog-form [name=remark]',name+' '+amount);b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
  from decimal import Decimal
  assert Decimal(str(balance()))==Decimal(expected)
 open_funds();b('fill','#active-dialog-form [name=amount]','-100.00');b('fill','#active-dialog-form [name=remark]',name+' insufficient');b('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('#dialog-error').textContent.includes('可用余额不足')")
 assert Decimal(str(balance()))==Decimal('17.50');b('press','Escape');wait("!document.querySelector('#active-dialog-form')")
 open_funds();assert b('js',"document.querySelector('[data-fund-ledger]').textContent.includes('12.5')&&document.querySelector('[data-fund-ledger]').textContent.includes('-5')")=='true'
 b('screenshot','/private/tmp/petlis-hospital-funds-write.png');(root/'qa/hospital-funds-write.png').write_bytes(Path('/private/tmp/petlis-hospital-funds-write.png').read_bytes());b('press','Escape')
 assert staff.config.sql('SELECT COUNT(*) FROM lin_infohospital_money WHERE infohospital_id='+str(hospital))=='2'
 assert staff.config.sql('SELECT money FROM lin_user WHERE id='+str(actor))==personal
 proof.update(hospitalId=hospital,pcCreditDebitAndOverdraft=True,ledgerRows=2,personalWalletUnchanged=True)
finally:
 gate(False);b('reload');wait("!!document.querySelector('#org-list')")
 assert staff.config.sql('SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId='+str(hospital))==name
 staff.config.sql('DELETE FROM lin_infohospital_money WHERE infohospital_id='+str(hospital))
 staff.config.sql("DELETE FROM lin_lb_infohospital WHERE InfoHospitalId="+str(hospital)+" AND HospitalName='"+name+"'")
 proof['gatesClosedAndOwnFixturesRemoved']=True
state['completed']=True;staff.save_private(checkpoint,state);staff.save_private(root/'qa/hospital-funds-write-ui.json',proof);print(json.dumps(proof))
