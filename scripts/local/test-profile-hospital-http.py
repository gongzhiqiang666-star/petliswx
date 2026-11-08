"""Two stages around native correction: concurrent request/rejection, then approval and scope checks."""
import importlib.util,json,secrets,sys,urllib.request,urllib.error
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/native-profile-fixture-private.json';f=json.loads(path.read_text());admin=staff.administrator(4)
def login(account):return staff.call('/auth/password/login',{'account':account['account'],'password':account['password']})['token']
def raw(path,token,body=None,method='GET',mime='application/json'):
 data=None if body is None else body if isinstance(body,bytes) else json.dumps(body).encode()
 request=urllib.request.Request(staff.config.BASE+path,data=data,headers={'Authorization':'Bearer '+token,'Content-Type':mime},method=method)
 try:
  with staff.opener.open(request,timeout=25) as response:return response.status,response.read()
 except urllib.error.HTTPError as error:return error.code,error.read()
def call(path,token,body=None,method='GET'):
 status,body=raw(path,token,body,method)
 try:result=json.loads(body)
 except ValueError:raise AssertionError('Expected local JSON response') from None
 result.setdefault('code',status);return result
def effective(result):
 status,body=result
 try:return json.loads(body).get('code',status)
 except ValueError:return status
def upload(path,token,filename,mime,content,extra=None):
 boundary='PetlisProfileHTTP';body=b''
 for key,value in (extra or {}).items():body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode()
 body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n').encode()+content+(f'\r\n--{boundary}--\r\n').encode()
 status,result=raw(path,token,body,'POST','multipart/form-data; boundary='+boundary);result=json.loads(result);assert result['code']==200;return result['data']
doctor=login(f);peer=login(f['peer']);pdf=(root/'qa/native-profile-qualification.pdf').read_bytes()
if '--prepare-rejection' in sys.argv:
 assert not f.get('rejectedChangeId'),'Inspect previous profile stage before replay'
 old=call('/account',doctor)['data'];shared=upload('/platform-files',peer,'old-hospital.pdf','application/pdf',pdf,{'bizType':'GENERAL','bizId':'0'});f['oldHospitalFileId']=shared['id']
 assert raw('/platform-files/'+str(shared['id'])+'/download',doctor)[0]==200
 change={'nickname':f['prefix']+'申请姓名','title':'申请职称','avatar':old.get('avatar') or '', 'hospitalId':f['targetHospitalId'],'qualificationFiles':json.loads(old['qualificationFiles']) if isinstance(old['qualificationFiles'],str) else old['qualificationFiles']}
 with ThreadPoolExecutor(max_workers=4) as pool:replies=list(pool.map(lambda _:call('/account/profile-changes',doctor,change,'POST'),range(4)))
 assert sorted(x['code'] for x in replies)==[200,409,409,409]
 history=call('/account/profile-changes',doctor)['data'];assert len(history)==1 and history[0]['status']=='pending';change_id=history[0]['id']
 assert call('/account',doctor)['data']['hospitalId']==f['hospitalId']
 assert call('/review/profile-changes/'+str(change_id),peer,{'approved':True},'POST')['code']==403
 assert call('/review/profile-changes/'+str(change_id),admin,{'approved':False,'reason':'本机验收：请补充头像及资质后重新提交'},'POST')['code']==200
 fresh=login(f);rejected=call('/account/profile-changes',fresh)['data'][0];assert rejected['status']=='rejected' and json.loads(rejected['requestedData'])['nickname']==change['nickname']
 f['rejectedChangeId']=change_id;staff.save_private(path,f)
 result={'userId':f['userId'],'changeId':change_id,'fourParallelOnlyOnePending':True,'pendingDoesNotChangeHospital':True,'peerReviewDenied':True,'rejectedApplicationRetainedForCorrection':True}
 (root/'qa/profile-rejection-http.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
elif '--approve' in sys.argv:
 change_id=int(f['pendingChangeId']);history=call('/account/profile-changes',doctor)['data'];assert history[0]['id']==change_id
 if history[0]['status']=='pending':
  assert raw('/platform-files/'+str(f['oldHospitalFileId'])+'/download',doctor)[0]==200
  assert call('/review/profile-changes/'+str(change_id),admin,{'approved':True,'reason':'本机验收：头像、资质及跨医院资料核对通过'},'POST')['code']==200
  assert call('/account',doctor)['code'] in [401,403]
  f['approvalRevocationVerified']=True;staff.save_private(path,f)
 else:assert history[0]['status']=='approved' and f.get('approvalRevocationVerified') is True
 fresh=login(f);profile=call('/account',fresh)['data'];assert profile['hospitalId']==f['targetHospitalId'] and profile['avatar']==f['requestedAvatar'] and profile['nickname']==f['requestedNickname']
 assert effective(raw('/platform-files/'+str(f['oldHospitalFileId'])+'/download',fresh))==403
 assert raw('/platform-files/'+str(f['oldHospitalFileId'])+'/download',peer)[0]==200
 new_file=upload('/platform-files',fresh,'new-hospital.pdf','application/pdf',pdf,{'bizType':'GENERAL','bizId':'0'});assert staff.config.sql(f'SELECT hospital_id FROM lin_file WHERE id={int(new_file["id"])}')==str(f['targetHospitalId'])
 assert effective(raw('/platform-files/'+str(new_file['id'])+'/download',peer))==403
 assert staff.config.sql(f"SELECT COUNT(*) FROM lin_user_role WHERE user_id={f['userId']} AND is_active=1 AND hospital_id={f['hospitalId']}")=='0'
 assert staff.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='PROFILE_HOSPITAL_CHANGED' AND resource_id='{f['userId']}'")=='1'
 f['approved']=True;staff.save_private(path,f)
 result={'userId':f['userId'],'changeId':change_id,'oldSessionRevoked':True,'newHospitalAvatarAndNameActive':True,'oldHospitalSharedFileDenied':True,'oldPeerStillHasOldAccess':True,'newUploadUsesNewHospital':True,'oldPeerCannotReadNewHospitalFile':True,'oneScopeMigrationAudit':True}
 (root/'qa/profile-hospital-approved-http.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
else:raise AssertionError('Select an explicit profile QA stage')
