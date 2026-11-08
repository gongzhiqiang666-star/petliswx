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
for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);key='legacy-test-catalog';checkpoint=root/'qa/price-maintenance-private.json'
assert not checkpoint.exists(),'Existing price-maintenance checkpoint requires review before new writes'
state={'prefix':'QA_PRICE_MAINT_'+secrets.token_hex(4)};staff.save_private(checkpoint,state)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(enabled,deleting=False):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-test-catalog' AND scope_type='GLOBAL' AND scope_id=0"))
 assert request('/admin/operations/configs/'+key,admin,{'value':{'enabled':enabled,'writeEnabled':enabled,'deleteEnabled':deleting},'version':version,'reason':'本机临时价段编辑删除验收'},'PUT')['code']==200
prefix=state['prefix'];proof={'scope':'one own disposable test/price','realBusinessRowsTouched':False}
try:
 gate(True)
 created=request('/admin/legacy/test-catalog/items',admin,{'name':prefix,'code':prefix,'requestKey':secrets.token_hex(16)},'POST');assert created['code']==200;item=int(created['data']);state['itemId']=item;staff.save_private(checkpoint,state)
 created=request('/admin/legacy/test-catalog/items/'+str(item)+'/prices',admin,{'price':'125.50','requestKey':secrets.token_hex(16)},'POST');assert created['code']==200;price=int(created['data']);state['priceId']=price;staff.save_private(checkpoint,state)
 endpoint='/admin/legacy/test-catalog/items/'+str(item)+'/prices/'+str(price)
 assert request(endpoint,admin)['data']['price']=='125.50'
 b('newtab','http://127.0.0.1:4173/#/legacy-test-catalog');wait("!!document.querySelector('#login-form')||!!document.querySelector('[data-test-prices]')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout);b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait('!!document.querySelector('+json.dumps('[data-test-prices="'+str(item)+'"]')+')')
 b('click','[data-test-prices="'+str(item)+'"]');wait('!!document.querySelector('+json.dumps('[data-price-edit="'+str(price)+'"]')+')')
 assert b('js',"!document.querySelector('[data-price-delete]')&&document.querySelector('[data-catalog-prices]').textContent.includes('125.50')")=='true'
 b('click','[data-price-edit="'+str(price)+'"]');wait("document.querySelector('#active-dialog-form [name=price]')?.step==='0.0001'")
 b('fill','#active-dialog-form [name=price]','150.5555');b('select','#active-dialog-form [name=active]','0');b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 row=request(endpoint,admin)['data'];assert row['price']=='150.56' and row['IsActive']==0 and row.get('createtime') is None and row.get('updatetime') is None
 assert request(endpoint+'/delete',admin,{'requestKey':secrets.token_hex(16),'expectedToken':row['editToken']},'POST')['code']==409
 gate(True,True);b('reload');wait('!!document.querySelector('+json.dumps('[data-test-prices="'+str(item)+'"]')+')');b('click','[data-test-prices="'+str(item)+'"]');wait('!!document.querySelector('+json.dumps('[data-price-delete="'+str(price)+'"]')+')')
 b('click','[data-price-delete="'+str(price)+'"]');wait("document.querySelector('#active-dialog-form')?.textContent.includes('永久删除')")
 assert b('js',"document.querySelector('#active-dialog-form').textContent.includes('150.56')")=='true'
 b('screenshot','/private/tmp/petlis-price-delete-confirm.png');(root/'qa/price-delete-confirm.png').write_bytes(Path('/private/tmp/petlis-price-delete-confirm.png').read_bytes())
 b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 assert request(endpoint,admin)['code']==404;assert request('/admin/legacy/test-catalog/items/'+str(item),admin)['data']['DealPrice']=='无'
 proof.update(itemId=item,priceId=price,pcFourDecimalEditRoundsLikeOriginalColumn=True,decimalTextDisplay=True,deleteRequiresOwnGate=True,pcPhysicalDelete=True,parentRetained=True)
finally:
 gate(False);b('reload');wait("!!document.querySelector('#page-content')");wait("!document.querySelector('[data-route=legacyTestCatalog]')")
 if state.get('itemId'):
  item=int(state['itemId']);assert staff.config.sql('SELECT TestNO FROM lin_lb_infotesttype WHERE InfoTestTypeId='+str(item))==prefix
  staff.config.sql('DELETE FROM lin_lb_infotesttype_price WHERE InfoTestTypeid='+str(item));staff.config.sql("DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId="+str(item)+" AND TestNO='"+prefix+"'")
 state['fixturesRemoved']=True;staff.save_private(checkpoint,state);proof['gatesClosedAndOwnFixturesRemoved']=True
state['completed']=True;staff.save_private(checkpoint,state);staff.save_private(root/'qa/price-maintenance-ui.json',proof);print(json.dumps(proof))
