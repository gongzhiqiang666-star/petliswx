# Local Petlis development integration tests only. Creates synthetic fixtures and cleans them up.
# SMS verification is a seeded fixture; no real SMS is sent. Business test payments are development fixtures.
from pathlib import Path
import urllib.request,urllib.error,json,secrets,subprocess,base64
b=Path.home()/'.local/share/petlis';base='http://127.0.0.1:8082/api'
mysql=b/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
def sql(s):return subprocess.run([str(mysql),f'--defaults-extra-file={b}/mysql/root.cnf','--batch','--skip-column-names','laboklin'],input=s.encode(),capture_output=True,check=True).stdout.decode().strip()
def call(path,data=None,token=None,method=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
 try:
  with urllib.request.urlopen(req) as r:return r.status,json.load(r)
 except urllib.error.HTTPError as e:return e.code,None
pw=next(l.split('：',1)[1] for l in (b/'reviewer-credentials.txt').read_text().splitlines() if l.startswith('密码：'))
_,r=call('/auth/password/login',{'account':'petlis_reviewer','password':pw});assert r['code']==200;reviewer=r['data']['token']
name='petlis_it_'+secrets.token_hex(5);phone='199'+str(secrets.randbelow(10**8)).zfill(8);password=secrets.token_hex(15)+'A7';code=str(secrets.randbelow(900000)+100000)
hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 ORDER BY InfoHospitalId LIMIT 1;'))
file_id=secrets.token_hex(16)+'.png';folder=Path(__file__).resolve().parents[2]/'petlis-backend/uploads/qualifications';folder.mkdir(parents=True,exist_ok=True);fixture=folder/file_id;fixture.write_bytes(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII='))
user_id=None
try:
 # Test fixture only: exercise verification consumption without sending real SMS.
 assert sql("SELECT COUNT(*) FROM lin_user WHERE mobile='"+phone+"';")=='0'
 sql("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','"+phone+"','"+code+"',0,'127.0.0.1',UNIX_TIMESTAMP());")
 _,r=call('/auth/register',{'username':name,'password':password,'mobile':phone,'code':code,'hospitalId':hospital,'nickname':'自动化测试医生','title':'测试资质','qualificationFiles':['/files/qualifications/'+file_id]});assert r['code']==200,r;user_id=r['data']
 _,r=call('/auth/password/login',{'account':name,'password':password});assert r['code']==403,r
 _,r=call('/review/registrations/'+str(user_id),{'approved':True,'reason':'自动化测试'},reviewer);assert r['code']==200,r
 _,r=call('/auth/password/login',{'account':name,'password':password});assert r['code']==200,r;doctor=r['data']['token'];assert not r['data']['reviewer']
 status,_=call('/review/orders',token=doctor);assert status==403,status
 _,r=call('/account',token=doctor);assert r['code']==200,r
 _,r=call('/laboratories',token=doctor);assert r['code']==200 and len(r['data'])>=1,r;lab=r['data'][0]['id']
 _,r=call('/laboratories/'+str(lab)+'/tests',token=doctor);assert r['code']==200 and r['data']['total']>0,r;test=next(x['infoTestTypeId'] for x in r['data']['items'] if x.get('price') is not None and float(x['price'])>=0)
 _,r=call('/coupons/quote',{'laboratoryId':lab,'testTypeIds':[test]},doctor);assert r['code']==200 and float(r['data']['payableAmount'])>=0,r
 _,r=call('/inbox',token=doctor);assert r['code']==200 and len(r['data']['items'])>=1,r
 notification=r['data']['items'][0]['id']
 _,r=call('/inbox/'+str(notification)+'/read',{},doctor,method='PUT');assert r['code']==200,r
 _,r=call('/inbox/unread-count',token=doctor);assert r['code']==200 and r['data']==0,r
 _,r=call('/auth/logout',{},doctor);assert r['code']==200,r
 status,_=call('/account',token=doctor);assert status in [401,403],status
 print('Verified registration -> pending login denied -> reviewer approval -> doctor login -> reviewer denial -> account/catalog/server quote -> logout token revoked; SMS was seeded test fixture only')
finally:
 if user_id:sql(f'DELETE d FROM lin_notification_delivery d JOIN lin_notification_event e ON e.id=d.event_id WHERE e.user_id={int(user_id)}; DELETE FROM lin_notification_event WHERE user_id={int(user_id)}; DELETE FROM lin_doctor_profile WHERE user_id={int(user_id)}; DELETE FROM lin_user WHERE id={int(user_id)};')
 sql("DELETE FROM lin_sms WHERE mobile='"+phone+"';")
 fixture.unlink(missing_ok=True)
