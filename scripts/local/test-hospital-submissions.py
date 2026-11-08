"""Real local HTTP self-service/review/history/concurrency. Only synthetic fixtures, no DDL."""
import concurrent.futures,hashlib,importlib.util,json,secrets,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';sql=staff.config.sql;base=staff.config.BASE;assert base=='http://127.0.0.1:8082/api';admin=staff.administrator(4)
def req(path,data=None,token=None,method=None):
 headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})};r=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
 try:
  with staff.opener.open(r,timeout=25) as response:status=response.status;content=response.read();ctype=response.headers.get('Content-Type','')
 except urllib.error.HTTPError as e:status=e.code;content=e.read();ctype=e.headers.get('Content-Type','')
 if 'json' in ctype:
  result=json.loads(content);return result.get('code',status),result.get('data')
 return status,content
def ok(path,data=None,token=None):
 code,result=req(path,data,token);assert code==200,(path,code);return result
pdf=(root/'qa/native-profile-qualification.pdf').read_bytes();assert pdf.startswith(b'%PDF-')
def upload(path,token=None,fields=None,content=None):
 boundary='PetlisDoc'+secrets.token_hex(12);body=b''
 for key,value in (fields or {}).items():body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode()
 body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qualification.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+(content if content is not None else pdf)+(f'\r\n--{boundary}--\r\n').encode()
 r=urllib.request.Request(base+path,data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary,**({'Authorization':'Bearer '+token} if token else {})})
 try:
  with staff.opener.open(r,timeout=25) as response:result=json.load(response)
  return result['code'],result.get('data')
 except urllib.error.HTTPError as e:return e.code,None
prefix='QA_DOC_'+secrets.token_hex(4);hospitals=[]
for name in ['自助医院','跨院对照']:
 sql(f"INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES('{prefix}{name}','{prefix}',874,1,0)")
 hospitals.append(int(sql(f"SELECT InfoHospitalId FROM lin_lb_infohospital WHERE HospitalName='{prefix}{name}'")))
hospital,other=hospitals;code,u=upload('/auth/qualifications');assert code==200
mobile='199'+str(secrets.randbelow(10**8)).zfill(8);sms=str(secrets.randbelow(900000)+100000);account='qa_doc_'+secrets.token_hex(6);password=secrets.token_hex(16)+'A7'
sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{mobile}','{sms}',0,'127.0.0.1',UNIX_TIMESTAMP())")
user=ok('/auth/register',{'username':account,'password':password,'mobile':mobile,'code':sms,'hospitalId':hospital,'nickname':'医院资料联调管理员','title':'医院管理员','qualificationFiles':[u['url']],'qualificationTokens':{u['url']:u['uploadToken']}})
staff.call(f'/review/registrations/{user}',{'approved':True,'reason':'本机医院资料合成测试账号'},admin)
staff.call(f'/admin/users/{user}/roles',{'roleCode':'HOSPITAL_ADMIN','hospitalId':hospital},admin)
staff.call(f'/admin/users/{user}/roles',{'roleCode':'DOCTOR','hospitalId':other},admin)
token=ok('/auth/password/login',{'account':account,'password':password})['token'];fixture={'account':account,'password':password,'userId':user,'hospitalId':hospital,'otherHospitalId':other,'prefix':prefix};staff.save_private(root/'qa/hospital-submissions-private.json',fixture)
assert [int(x['id']) for x in ok('/hospital-documents/hospitals',token=token)]==[hospital]
assert req(f'/hospitals/{other}/document-submissions',token=token)[0]==403
assert req('/admin/hospital-documents/pending',token=token)[0]==403
fields={'purpose':'LICENSE','reason':'初始生效执照','requestKey':secrets.token_hex(16)};code,old=upload(f'/admin/hospitals/{hospital}/documents',admin,fields);assert code==200;old=old['id']
fields={'purpose':'LICENSE','reason':'医院提交更新执照','requestKey':secrets.token_hex(16),'previousFileId':old};endpoint=f'/hospitals/{hospital}/document-submissions'
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(lambda _:upload(endpoint,token,fields),range(4)))
assert all(code==200 for code,_ in results);ids={item['id'] for _,item in results};assert len(ids)==1;pending=ids.pop()
assert upload(endpoint,token,{**fields,'reason':'改内容复用请求号'})[0]==409
assert upload(endpoint,token,{**fields,'requestKey':secrets.token_hex(16)})[0]==409
before=ok(endpoint,token=token)['items'];assert next(x for x in before if x['id']==old)['replaced']==0;assert next(x for x in before if x['id']==pending)['reviewStatus']=='PENDING'
assert req(f'/platform-files/{pending}/download',token=token)==(200,pdf)
peer=json.loads((root/'simulator-fixture.json').read_text());doctor=ok('/auth/password/login',{'account':peer['account'],'password':peer['password']})['token']
assert req(f'/platform-files/{pending}/download',token=doctor)[0]==403;assert req(f'/platform-files/{pending}/download')[0] in [401,403]
assert any(x['id']==pending for x in ok('/admin/hospital-documents/pending',token=admin)['items'])
decision={'approved':True,'reason':'核对执照清晰有效','requestKey':secrets.token_hex(16)}
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:reviewed=list(pool.map(lambda _:req(f'/admin/hospital-documents/{pending}/review',decision,admin),range(4)))
assert all(code==200 for code,_ in reviewed)
assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='HOSPITAL_DOCUMENT_REVIEWED' AND resource_id='{pending}'")=='1'
after=ok(endpoint,token=token)['items'];assert next(x for x in after if x['id']==old)['replaced']==1;assert next(x for x in after if x['id']==pending)['reviewStatus']=='APPROVED'
assert req(f'/platform-files/{old}/download',token=token)==(200,pdf)
assert req(f'/admin/hospital-documents/{pending}/review',{**decision,'requestKey':secrets.token_hex(16),'approved':False},admin)[0]==409
code,bad=upload(endpoint,token,{'purpose':'OTHER','reason':'初次补充资料','requestKey':secrets.token_hex(16)});assert code==200;bad=bad['id'];ok(f'/admin/hospital-documents/{bad}/review',{'approved':False,'reason':'请提供清晰版本','requestKey':secrets.token_hex(16)},admin)
assert next(x for x in ok(endpoint,token=token)['items'] if x['id']==bad)['reviewStatus']=='REJECTED'
code,corrected=upload(endpoint,token,{'purpose':'OTHER','reason':'已补清晰件供浏览器审核','requestKey':secrets.token_hex(16)});assert code==200
fixture.update({'oldFileId':old,'approvedFileId':pending,'rejectedFileId':bad,'uiPendingFileId':corrected['id']});staff.save_private(root/'qa/hospital-submissions-private.json',fixture)
evidence={'hospitalId':hospital,'managerUserId':user,'oldFileId':old,'approvedFileId':pending,'uiPendingFileId':corrected['id'],'fourUploadRequestsOneFile':True,'fourReviewRequestsOneAudit':True,'sameKeyDifferentContentRejected':True,'oldVersionEffectiveUntilApproval':True,'historyBytesRetained':True,'doctorScopeNotBorrowedForAdministration':True,'doctorAnonymousDenied':True,'rejectionAndCorrectedSubmission':True,'ddlExecuted':False,'externalRequestMade':False,'pdfSha256':hashlib.sha256(pdf).hexdigest()};(root/'qa/hospital-submissions-http.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2));print(json.dumps(evidence,ensure_ascii=False))
