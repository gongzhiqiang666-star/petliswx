"""One synthetic same-hospital doctor for native viewing of official sandbox history; no real SMS."""
import importlib.util,json,secrets,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/native-sf-history-private.json';assert not path.exists(),'Reuse existing native SF identity'
proof=json.loads((root/'qa/sf-reship-official.json').read_text());assert proof['completed'] and proof['gateRestoredDisabled'];hospital=int(staff.config.sql('SELECT InfoHospitalId FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=23956'));admin=staff.administrator(4)
boundary='PetlisSfNative'+secrets.token_hex(8);pdf=(root/'qa/native-profile-qualification.pdf').read_bytes();body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qualification.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+pdf+(f'\r\n--{boundary}--\r\n').encode()
req=urllib.request.Request(staff.config.BASE+'/auth/qualifications',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
with staff.opener.open(req,timeout=15) as response:uploaded=json.load(response)
assert uploaded['code']==200;uploaded=uploaded['data'];account='qa_sf_history_'+secrets.token_hex(4);password=secrets.token_hex(16)+'A7';mobile='199'+str(secrets.randbelow(10**8)).zfill(8);code=str(secrets.randbelow(900000)+100000)
staff.config.sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{mobile}','{code}',0,'127.0.0.1',UNIX_TIMESTAMP())")
uid=staff.call('/auth/register',{'username':account,'password':password,'mobile':mobile,'code':code,'hospitalId':hospital,'nickname':'顺丰原生历史验收医生','title':'兽医','qualificationFiles':[uploaded['url']],'qualificationTokens':{uploaded['url']:uploaded['uploadToken']}})
staff.save_private(path,{'account':account,'password':password,'userId':uid,'hospitalId':hospital,'orderId':23956})
staff.call(f'/review/registrations/{uid}',{'approved':True,'reason':'仅本机顺丰官方沙箱历史展示验收'},admin);staff.call(f'/admin/users/{uid}/roles',{'roleCode':'DOCTOR','hospitalId':hospital},admin)
print(json.dumps({'userId':uid,'hospitalId':hospital,'realSmsSent':False}))
