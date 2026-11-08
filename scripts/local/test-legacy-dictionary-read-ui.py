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
for _ in range(30):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:assert r.status==200
  break
 except OSError:time.sleep(.25)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4)
key='legacy-dictionaries'
if staff.config.sql("SELECT COUNT(*) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='0':staff.call('/admin/operations/configs',{'key':key,'value':{'enabled':False},'description':'PHP基础字典迁移模块；当前只读能力，本机受控验收'},admin)
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0")=='false'
def gate(value):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='legacy-dictionaries' AND scope_type='GLOBAL' AND scope_id=0"));r=request('/admin/operations/configs/'+key,admin,{'value':{'enabled':value},'version':version,'reason':'本机基础字典只读界面验收临时开启' if value else '本机只读验收结束，恢复模块关闭'},'PUT');assert r['code']==200
proof={'module':'M07','scope':'species/sex/sampleType read-only subset','writeBusinessRows':False}
try:
 assert request('/admin/legacy/capabilities',admin)['data']['dictionaries'] is False
 assert request('/admin/legacy/dictionaries/species',admin)['code']==409
 b('newtab','http://127.0.0.1:4173/#/dashboard');wait("!!document.querySelector('#login-form')||!!document.querySelector('#page-content')")
 if b('js',"!!document.querySelector('#login-form')")=='true':
  # Read existing private local account only to log in through the new PC's normal password form.
  r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',"$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"],capture_output=True,text=True,check=True);account=json.loads(r.stdout)
  b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]')
 wait("!!document.querySelector('#page-content')")
 assert b('js',"!document.querySelector('[data-route=legacyDictionaries]')")=='true';proof['disabledMenuHidden']=True
 gate(True);b('reload');wait("!!document.querySelector('[data-route=legacyDictionaries]')");b('click','[data-route=legacyDictionaries]');wait("!!document.querySelector('#dictionary-list table')")
 assert b('js',"!document.querySelector('[data-dictionary-new]')&&!document.querySelector('[data-dictionary-edit]')")=='true';proof['readFlagDoesNotExposeWrites']=True
 for label in ['物种名称','物种名称(英文)','状态','创建时间','更新时间']:
  assert b('js','document.querySelector("#dictionary-list").textContent.includes('+json.dumps(label)+')')=='true'
 b('select','[name=active]','0');b('click','#dictionary-filter button[type=submit]');wait("!!document.querySelector('#dictionary-list table')||document.querySelector('#dictionary-list').textContent.includes('暂无')")
 filtered=request('/admin/legacy/dictionaries/species?active=0&page=0&size=20',admin);assert filtered['code']==200 and all(row['IsActive']==0 for row in filtered['data']['items'])
 assert int(filtered['data']['total'])==int(staff.config.sql('SELECT COUNT(*) FROM lin_lb_infoanimal WHERE IsActive=0'))
 proof['disabledRowsAndCountMatchOriginalTable']=True
 b('click','[data-kind=sex]');wait("!!document.querySelector('#dictionary-list table')&&document.querySelector('#dictionary-list').textContent.includes('性别名称(英文)')")
 b('screenshot','/private/tmp/petlis-legacy-dictionary-sex.png');(root/'qa/legacy-dictionary-sex.png').write_bytes(Path('/private/tmp/petlis-legacy-dictionary-sex.png').read_bytes())
 proof['originalSexColumnsVisible']=True
 b('click','[data-kind=sampleType]');wait("!!document.querySelector('#dictionary-list table')&&document.querySelector('#dictionary-list').textContent.includes('采样类型(英文)')")
 for label in ['采样类型','采样类型(英文)','添加时间','更新时间']:
  assert b('js','document.querySelector("#dictionary-list").textContent.includes('+json.dumps(label)+')')=='true'
 assert b('js',"document.querySelector('label[for=name]')?.textContent.includes('采样类型')||Array.from(document.querySelectorAll('label')).some(x=>x.textContent.includes('采样类型'))")=='true'
 sample=request('/admin/legacy/dictionaries/sampleType?page=0&size=20',admin);assert sample['code']==200
 assert int(sample['data']['total'])==int(staff.config.sql('SELECT COUNT(*) FROM lin_lb_infosampletype'))
 picker=request('/admin/legacy/dictionaries/sampleType/select?page=1&size=1',admin);assert picker['code']==200
 assert int(picker['data']['total'])==int(sample['data']['total'])
 if picker['data']['list']:
  first=picker['data']['list'][0];assert set(first)=={'InfoSampleTypeId','SampleName','pid'}
  hydrated=request('/admin/legacy/dictionaries/sampleType/select?selectedIds='+str(first['InfoSampleTypeId'])+'&words=impossible-match&page=9&size=1',admin)
  assert hydrated['code']==200 and hydrated['data']['list']==[first]
 proof['sampleTypeOriginalColumnsAndCount']=True;proof['selectorHttpSearchAndHydration']=True
 b('screenshot','/private/tmp/petlis-legacy-dictionary-sample.png');(root/'qa/legacy-dictionary-sample.png').write_bytes(Path('/private/tmp/petlis-legacy-dictionary-sample.png').read_bytes())
 doctor=json.loads((root/'qa/native-sf-history-private.json').read_text());dt=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token'];assert request('/admin/legacy/dictionaries/sex',dt)['code']==403;proof['doctorDirectApiDenied']=True
finally:
 gate(False)
 b('reload');wait("!!document.querySelector('#page-content')")
 wait("!document.querySelector('[data-route=legacyDictionaries]')")
 assert request('/admin/legacy/dictionaries/sex',admin)['code']==409;proof['gateRestoredDisabled']=True
staff.save_private(root/'qa/legacy-dictionary-read-ui.json',proof);print(json.dumps(proof))
