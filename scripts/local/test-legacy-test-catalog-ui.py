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
from decimal import Decimal
for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);key='legacy-test-catalog';checkpoint=root/'qa/test-catalog-private.json'
state=json.loads(checkpoint.read_text()) if checkpoint.exists() else {'prefix':'QA_TESTCAT_'+secrets.token_hex(5)}
assert not state.get('completed'),'Completed evidence must not be replayed'
staff.save_private(checkpoint,state)
if staff.config.sql("SELECT COUNT(*) FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0")=='0':
 staff.call('/admin/operations/configs',{'key':key,'value':{'enabled':False,'writeEnabled':False},'description':'PHP检测项目迁移模块，本机受控验收'},admin)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled,writable):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':writable},'version':version,'reason':'本机临时项目资料PC验收'},'PUT')['code']==200
prefix=state['prefix'];assert prefix.startswith('QA_TESTCAT_') and all(c.isalnum() or c=='_' for c in prefix)
proof={'module':'M08','realBusinessRowsTouched':False}
try:
 gate(True,True)
 b('newtab','http://127.0.0.1:4173/#/legacy-test-catalog');wait("!!document.querySelector('#login-form')||!!document.querySelector('[data-test-new]')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout)
  b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('[data-test-new]')")
 ids=staff.config.sql("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE TestNO IN ('"+prefix+"','"+prefix+"X')").splitlines()
 if not ids:
  b('click','[data-test-new]');wait("!!document.querySelector('#active-dialog-form')");b('fill','#active-dialog-form [name=name]',prefix);b('fill','#active-dialog-form [name=code]',prefix);b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
  ids=staff.config.sql("SELECT InfoTestTypeId FROM lin_lb_infotesttype WHERE TestNO='"+prefix+"'").splitlines()
 assert len(ids)==1;item=int(ids[0]);state['itemId']=item;staff.save_private(checkpoint,state)
 row=request('/admin/legacy/test-catalog/items/'+str(item),admin)['data']
 for field in ['Price','CtoG','GtoC','DiscountReal','DiscountNext']:assert Decimal(str(row[field]))==0
 assert row['IsActive']==1 and row['type']==0 and row['TextModel']==''
 wait('!!document.querySelector('+json.dumps('[data-test-edit="'+str(item)+'"]')+')')
 b('click','[data-test-edit="'+str(item)+'"]');wait("!!document.querySelector('#active-dialog-form')");b('fill','#active-dialog-form [name=name]',prefix+' 改');b('fill','#active-dialog-form [name=code]',prefix+'X');b('select','#active-dialog-form [name=type]','2');b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 for price,active in [('125.50','1'),('99.00','0')]:
  existing=request('/admin/legacy/test-catalog/items/'+str(item)+'/prices',admin)['data']['items']
  if any(Decimal(str(r['price']))==Decimal(price) for r in existing):continue
  b('click','[data-test-prices="'+str(item)+'"]');wait("!!document.querySelector('#active-dialog-form [name=price]')");b('fill','#active-dialog-form [name=price]',price);b('select','#active-dialog-form [name=active]',active);b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 row=request('/admin/legacy/test-catalog/items/'+str(item),admin)['data'];assert row['TestName']==prefix+' 改' and row['type']==2 and '125.50' in row['DealPrice'] and '99.00' in row['DealPrice']
 assert Decimal(str(row['Price']))==0;assert request('/admin/legacy/test-catalog/items/'+str(item)+'/prices',admin)['data']['total']==2
 b('click','[data-test-prices="'+str(item)+'"]');wait("!!document.querySelector('#active-dialog-form [name=price]')");b('screenshot','/private/tmp/petlis-test-catalog-prices.png');(root/'qa/test-catalog-prices.png').write_bytes(Path('/private/tmp/petlis-test-catalog-prices.png').read_bytes());b('press','Escape')
 gate(True,False);b('reload');wait("!!document.querySelector('[data-test-prices]')");assert b('js',"!document.querySelector('[data-test-new]')&&!document.querySelector('[data-test-edit]')")=='true'
 doctor=json.loads((root/'qa/native-sf-history-private.json').read_text());dt=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token'];assert request('/admin/legacy/test-catalog/items/'+str(item),dt)['code']==403
 proof.update(itemId=item,pcCreateEditAndMultiPrices=True,callbackDefaultsKept=True,disabledPriceIncludedInOriginalDisplay=True,readFlagHidesWrites=True,doctorDenied=True)
finally:
 gate(False,False);b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyTestCatalog]')")
 if state.get('itemId'):
  own=int(state['itemId']);assert staff.config.sql('SELECT TestNO FROM lin_lb_infotesttype WHERE InfoTestTypeId='+str(own)) in [prefix,prefix+'X']
  staff.config.sql('DELETE FROM lin_lb_infotesttype_price WHERE InfoTestTypeid='+str(own));staff.config.sql("DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId="+str(own)+" AND TestNO IN ('"+prefix+"','"+prefix+"X')")
 proof['gatesClosedAndOwnFixturesRemoved']=True
state['completed']=True;staff.save_private(checkpoint,state);staff.save_private(root/'qa/test-catalog-ui.json',proof);print(json.dumps(proof))
