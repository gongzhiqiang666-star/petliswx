"""Isolated HTTP qualification ownership, registration rollback, and claim race; no real SMS."""
import concurrent.futures,hashlib,importlib.util,json,secrets,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('local_staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';base=staff.config.BASE;sql=staff.config.sql
assert base=='http://127.0.0.1:8082/api';admin=staff.administrator(4)
def request(path,body=None,token=None):
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with staff.opener.open(req,timeout=25) as r: status=r.status;content=r.read();ctype=r.headers.get('Content-Type','')
 except urllib.error.HTTPError as e:status=e.code;content=e.read();ctype=e.headers.get('Content-Type','')
 if 'json' in ctype:
  value=json.loads(content);return value.get('code',status),value.get('data')
 return status,content
pdf=(root/'qa/native-profile-qualification.pdf').read_bytes();assert pdf.startswith(b'%PDF-')
def upload(token=None):
 boundary='PetlisQualification'+secrets.token_hex(8);data=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qualification.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+pdf+(f'\r\n--{boundary}--\r\n').encode()
 req=urllib.request.Request(base+'/auth/qualifications',data=data,headers={'Content-Type':'multipart/form-data; boundary='+boundary,**({'Authorization':'Bearer '+token} if token else {})})
 with staff.opener.open(req,timeout=20) as response:result=json.load(response)
 assert result['code']==200;return result['data']
hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 ORDER BY InfoHospitalId LIMIT 1'))
def registration(uploaded):
 phone='199'+str(secrets.randbelow(10**8)).zfill(8);code=str(secrets.randbelow(900000)+100000)
 sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{phone}','{code}',0,'127.0.0.1',UNIX_TIMESTAMP())")
 return {'username':'qa_qual_'+secrets.token_hex(6),'password':secrets.token_hex(16)+'A7','mobile':phone,'code':code,'hospitalId':hospital,'nickname':'本机资质归属验证','title':'兽医','qualificationFiles':[uploaded['url']],'qualificationTokens':{uploaded['url']:uploaded['uploadToken']}}
def register(data):return request('/auth/register',data)
proof={};anonymous=upload();first=registration(anonymous);no_tokens={**first,'qualificationTokens':{}}
assert register(no_tokens)[0]==400
assert sql(f"SELECT COUNT(*) FROM lin_user WHERE username='{first['username']}'")=='0'
assert sql(f"SELECT times FROM lin_sms WHERE mobile='{first['mobile']}' ORDER BY id DESC LIMIT 1")=='0'
wrong={**first,'qualificationTokens':{anonymous['url']:'a'*43}};assert register(wrong)[0]==403
code,owner=register(first);assert code==200 and isinstance(owner,int)
proof['failedRegistrationRollsBackUserSmsAndClaim']=True
assert register(registration(anonymous))[0]==409;proof['claimedCredentialCannotRegisterAnotherUser']=True
# A separate anonymous upload is shared deliberately by two simultaneous registration attempts.
raced=upload();bodies=[registration(raced),registration(raced)]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(register,bodies))
assert sorted(result[0] for result in results)==[200,409]
assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='QUALIFICATION_CLAIMED' AND resource_id='{raced['url'].split('/')[-1]}'")=='1'
loser=bodies[next(i for i,result in enumerate(results) if result[0]==409)]
assert sql(f"SELECT COUNT(*) FROM lin_user WHERE username='{loser['username']}'")=='0'
assert sql(f"SELECT times FROM lin_sms WHERE mobile='{loser['mobile']}' ORDER BY id DESC LIMIT 1")=='0'
proof['concurrentRegistrationOneWinner']=True
staff.call(f'/review/registrations/{owner}',{'approved':True,'reason':'本机资质归属验证'},admin);staff.call(f'/admin/users/{owner}/roles',{'roleCode':'DOCTOR','hospitalId':hospital},admin)
session=staff.call('/auth/password/login',{'account':first['username'],'password':first['password']});token=session['token']
peer_config=json.loads((root/'simulator-fixture.json').read_text());peer=staff.call('/auth/password/login',{'account':peer_config['account'],'password':peer_config['password']})['token']
assert request(anonymous['url'],token=token)==(200,pdf)
assert request(anonymous['url'],token=peer)[0]==403
assert request(anonymous['url'],token=admin)==(200,pdf)
assert request(anonymous['url'])[0] in [401,403]
proof['ownerReviewerReadPeerAndAnonymousDenied']=True
owned=upload(token);assert 'uploadToken' not in owned
assert request(owned['url'],token=token)==(200,pdf);assert request(owned['url'],token=peer)[0]==403
body={'nickname':'本机资料归属验证','title':'兽医','avatar':'','hospitalId':hospital,'qualificationFiles':[owned['url']]}
assert request('/account/profile-changes',body,peer)[0]==403
assert request('/account/profile-changes',body,token)[0]==200
pending=int(sql(f"SELECT id FROM lin_profile_change WHERE user_id={owner} AND status='pending' ORDER BY id DESC LIMIT 1"))
staff.call(f'/review/profile-changes/{pending}',{'approved':False,'reason':'本机归属校验完成，保留原资料'},admin)
proof['profileCannotReferenceAnotherOwner']=True
assert request(owned['url'],token=token)[0]==401
token=staff.call('/auth/password/login',{'account':first['username'],'password':first['password']})['token']
unclaimed=upload();assert request(unclaimed['url'],token=token)[0]==403
expired=registration(unclaimed);name=unclaimed['url'].split('/')[-1]
sql(f"UPDATE lin_audit_record SET after_json=JSON_SET(after_json,'$.expiresAt',1) WHERE action='QUALIFICATION_UPLOADED' AND resource_type='QUALIFICATION' AND resource_id='{name}'")
assert register(expired)[0]==403;proof['unclaimedAndExpiredRejected']=True
staff.save_private(root/'qa/qualification-ownership-private.json',{'account':first['username'],'password':first['password'],'userId':owner,'qualification':anonymous['url'],'ownedQualification':owned['url'],'hospitalId':hospital,'profileChangeId':pending})
(root/'qa/qualification-ownership-http.json').write_text(json.dumps({'checks':proof,'ownerId':owner,'profileChangeId':pending,'pdfSha256':hashlib.sha256(pdf).hexdigest(),'ddlExecuted':False,'realSmsSent':False},ensure_ascii=False,indent=2))
print(json.dumps({'checks':proof,'ownerId':owner,'ddlExecuted':False},ensure_ascii=False))
