"""Use the shared browse daemon's active Petlis-only tab; normal form submission, private passwords, own QA employee disabled at end."""
import importlib.util,json,secrets,subprocess,urllib.request,time
from pathlib import Path
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis UI action failed; credentials omitted';return r.stdout
assert 'http://127.0.0.1:4173/#/organizations' in browser('url')
username='ui_staff_'+secrets.token_hex(5);password=secrets.token_urlsafe(22)+'A7';uid=None
before=setup.config.sql('SELECT COUNT(*) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL')
try:
 browser('click','#staff-new')
 browser('select','[name="roleCode"]','COURIER')
 hidden=browser('js',"document.querySelector('[name=laboratoryId]').disabled && getComputedStyle(document.querySelector('[name=laboratoryId]').closest('.form-field')).display==='none'")
 assert 'true' in hidden
 browser('select','[name="roleCode"]','LAB_STAFF')
 required=browser('js',"document.querySelector('[name=laboratoryId]').required && !document.querySelector('[name=laboratoryId]').disabled")
 assert 'true' in required
 browser('select','[name="roleCode"]','COURIER')
 for name,value in [('username',username),('displayName','本机界面验收配送员'),('password',password),('confirmPassword',password+'mismatch'),('reason','仅本机合成UI验收，完成后停用')]:browser('fill',f'[name="{name}"]',value)
 browser('click','[type="submit"][form="active-dialog-form"]')
 assert '两次密码输入不一致' in browser('js',"document.querySelector('#dialog-error').textContent")
 assert setup.config.sql(f"SELECT COUNT(*) FROM lin_user WHERE username='{username}'")=='0'
 browser('fill','[name="confirmPassword"]',password)
 browser('click','[type="submit"][form="active-dialog-form"]');browser('wait','--networkidle')
 for _ in range(50):
  if 'true' in browser('js',"!document.querySelector('#active-dialog-form')"):break
  time.sleep(0.1)
 else:raise AssertionError('Staff dialog did not close after request completed')
 uid=int(setup.config.sql(f"SELECT id FROM lin_user WHERE username='{username}'"))
 assert setup.config.sql(f'SELECT hospital_id IS NULL FROM lin_user WHERE id={uid}')=='1'
 assert setup.config.sql(f"SELECT GROUP_CONCAT(role_code) FROM lin_user_role WHERE user_id={uid} AND is_active=1")=='COURIER'
 assert setup.config.sql('SELECT COUNT(*) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL')==before
 result={'userId':uid,'courierLabHiddenAndDisabled':True,'labRoleMakesLabRequired':True,'mismatchedPasswordRetainsFormAndCreatesNoUser':True,'correctedFormCreatesOneCourier':True,'dialogClosedOnSuccess':True,'noHospitalOrDoctorApplication':True,'passwordPrinted':False}
finally:
 if not uid:
  found=setup.config.sql(f"SELECT id FROM lin_user WHERE username='{username}'")
  if found:uid=int(found)
 if uid:
  token=setup.administrator(4)
  req=urllib.request.Request(setup.config.BASE+f'/admin/users/{uid}/status',data=json.dumps({'active':False,'reason':'本机界面验收完成，停用本次合成配送员'}).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method='PATCH')
  with setup.opener.open(req,timeout=20) as response:assert json.load(response)['code']==200
if uid:result['syntheticAccountDisabledAtEnd']=True
print(json.dumps(result,ensure_ascii=False,indent=2))
