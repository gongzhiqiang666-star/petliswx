"""Actual PC before/after review and rejection; same-hospital synthetic correction only."""
import importlib.util,json,secrets,subprocess,time,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';f=json.loads((root/'qa/native-profile-fixture-private.json').read_text());assert f['completed'] and f['approved'];evidence=root/'qa/profile-review-ui.json';assert not evidence.exists(),'Inspect completed PC profile review evidence before replay'
admin=staff.administrator(4);doctor=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token']
def get(path,token):
 with staff.opener.open(urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+token}),timeout=20) as response:
  result=json.load(response);assert result['code']==200;return result['data']
profile=get('/account',doctor);files=json.loads(profile['qualificationFiles']) if isinstance(profile['qualificationFiles'],str) else profile['qualificationFiles']
avatar=profile['avatar'];assert avatar.startswith(staff.config.BASE+'/files/avatars/')
with staff.opener.open(avatar,timeout=20) as response:assert response.read()==Path('miniprogram/assets/icons/brand-mine.png').read_bytes()
for path in files:
 assert path.startswith('/files/qualifications/')
 with staff.opener.open(urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+admin}),timeout=20) as response:assert response.read().startswith(b'%PDF-')
body={'nickname':f['prefix']+'PC审核测试','title':profile['title'],'avatar':avatar,'hospitalId':profile['hospitalId'],'qualificationFiles':files}
staff.call('/account/profile-changes',body,doctor);change=int(get('/account/profile-changes',doctor)[0]['id'])
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 result=subprocess.run([B,*args],capture_output=True,text=True);assert result.returncode==0,'Petlis profile review browser action failed';return result.stdout.strip()
def wait(expression):
 for _ in range(70):
  if browser('js',expression)=='true':return
  time.sleep(.15)
 raise AssertionError('Profile review UI readiness timeout')
assert browser('url').startswith('http://127.0.0.1:4173/')
browser('goto','http://127.0.0.1:4173/#/reviews');browser('reload');wait("!!document.querySelector('[data-tab=profiles]')");browser('click','[data-tab=profiles]');wait(f"!!document.querySelector('[data-review=\"{change}\"]')")
browser('click',f'[data-review="{change}"]');wait("!!document.querySelector('#active-dialog-form')")
content=browser('js',"document.querySelector('.modal-body').textContent")
for expected in [f['requestedNickname'],body['nickname'],f['targetHospitalName'],'修改前','申请修改为','下载资质']:assert expected in content
assert browser('js',"document.querySelectorAll('[data-profile-qualification]').length")=='2'
browser('select','[name=approved]','false');browser('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('#dialog-error').textContent.includes('驳回原因')")
browser('fill','[name=reason]','仅本机PC审核界面验收，保留现有已生效资料');browser('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
assert staff.config.sql(f'SELECT status FROM lin_profile_change WHERE id={change}')=='rejected'
fresh=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token'];after=get('/account',fresh);assert after['nickname']==f['requestedNickname'] and after['hospitalId']==f['targetHospitalId']
result={'userId':f['userId'],'changeId':change,'beforeAfterNamesAndHospitalVisible':True,'twoQualificationDownloadsVisible':True,'reviewerQualificationPDFVerified':True,'avatarBytesAccessible':True,'emptyRejectionReasonRetainsForm':True,'normalUIRejectionSaved':True,'activeProfilePreserved':True};staff.save_private(evidence,result);print(json.dumps(result))
