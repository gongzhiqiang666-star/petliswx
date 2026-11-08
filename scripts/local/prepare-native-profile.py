"""Prepare two synthetic doctors and two hospitals through isolated fixtures/normal registration; no real SMS."""
import importlib.util,json,secrets,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';target=root/'qa/native-profile-fixture-private.json';assert not target.exists(),'Use existing profile fixture before creating another'
admin=staff.administrator(4);prefix='QA_PROFILE_'+secrets.token_hex(4)
def sql(q):return staff.config.sql(q)
hospitals=[]
for suffix in ['原医院','新医院']:
 sql(f"INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,CityId,IsActive,PayType) VALUES('{prefix}{suffix}','{prefix}',874,1,0)")
 hospitals.append(int(sql(f"SELECT InfoHospitalId FROM lin_lb_infohospital WHERE HospitalName='{prefix}{suffix}'")))
with staff.opener.open(urllib.request.Request(staff.config.BASE+'/invoices/16/download',headers={'Authorization':'Bearer '+admin}),timeout=20) as response:pdf=response.read()
assert pdf.startswith(b'%PDF-');(root/'qa/native-profile-qualification.pdf').write_bytes(pdf)
boundary='PetlisProfileQualification';body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qualification.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+pdf+(f'\r\n--{boundary}--\r\n').encode()
accounts=[]
for kind in ['doctor','peer']:
 req=urllib.request.Request(staff.config.BASE+'/auth/qualifications',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
 with staff.opener.open(req,timeout=20) as response:uploaded=json.load(response)
 assert uploaded['code']==200;qualification=uploaded['data']['url']
 account=prefix+'_'+kind;password=secrets.token_hex(16)+'A7';mobile='199'+str(secrets.randbelow(10**8)).zfill(8);code=str(secrets.randbelow(900000)+100000)
 sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{mobile}','{code}',0,'127.0.0.1',UNIX_TIMESTAMP())")
 uid=staff.call('/auth/register',{'username':account,'password':password,'mobile':mobile,'code':code,'hospitalId':hospitals[0],'nickname':prefix+kind,'title':'原职称','qualificationFiles':[qualification],'qualificationTokens':{qualification:uploaded['data']['uploadToken']}})
 staff.call(f'/review/registrations/{uid}',{'approved':True,'reason':'本机原生资料重审合成账号'},admin)
 staff.call(f'/admin/users/{uid}/roles',{'roleCode':'DOCTOR','hospitalId':hospitals[0]},admin)
 accounts.append({'account':account,'password':password,'userId':uid})
staff.save_private(target,{**accounts[0],'peer':accounts[1],'hospitalId':hospitals[0],'targetHospitalId':hospitals[1],'targetHospitalName':prefix+'新医院','prefix':prefix,'completed':False})
print(json.dumps({'userIds':[x['userId'] for x in accounts],'hospitalIds':hospitals,'realSmsSent':False}))
