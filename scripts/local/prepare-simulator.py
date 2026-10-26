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
state=b/'simulator-fixture.json'
if state.exists():raise SystemExit('Clean the existing simulator fixture before preparing another')
pw=next(l.split('：',1)[1] for l in (b/'reviewer-credentials.txt').read_text().splitlines() if l.startswith('密码：'))
_,r=call('/auth/password/login',{'account':'petlis_reviewer','password':pw});assert r['code']==200;reviewer=r['data']['token']
name='petlis_qa_'+secrets.token_hex(5);phone='199'+str(secrets.randbelow(10**8)).zfill(8);password=secrets.token_hex(15)+'A7';code=str(secrets.randbelow(900000)+100000)
hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 ORDER BY InfoHospitalId LIMIT 1;'))
file_id=secrets.token_hex(16)+'.png';folder=Path(__file__).resolve().parents[2]/'petlis-backend/uploads/qualifications';folder.mkdir(parents=True,exist_ok=True);fixture=folder/file_id;fixture.write_bytes(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII='))
# Creates only a synthetic local account for simulator acceptance. Credentials stay outside Git.
state=b/'simulator-fixture.json'
if state.exists():raise SystemExit('An existing simulator fixture must be cleaned before preparing another')
user_id=None
try:
 sql("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','"+phone+"','"+code+"',0,'127.0.0.1',UNIX_TIMESTAMP());")
 _,r=call('/auth/register',{'username':name,'password':password,'mobile':phone,'code':code,'hospitalId':hospital,'nickname':'模拟器测试医生','title':'测试资质','qualificationFiles':['/files/qualifications/'+file_id]});assert r['code']==200,r;user_id=r['data']
 # Write cleanup information before approval/login so a later failure remains recoverable.
 state.write_text(json.dumps({'userId':user_id,'account':name,'password':password,'phone':phone,'qualification':str(fixture)}));state.chmod(0o600)
 _,r=call('/review/registrations/'+str(user_id),{'approved':True,'reason':'本机模拟器测试专用'},reviewer);assert r['code']==200,r
 _,r=call('/auth/password/login',{'account':name,'password':password});assert r['code']==200,r
 data=json.loads(state.read_text());data['session']=r['data'];state.write_text(json.dumps(data));state.chmod(0o600)
 animal=int(sql('SELECT InfoAnimalId FROM lin_lb_infoanimal WHERE IsActive=1 LIMIT 1;'))
 _,r=call('/pets',{'animalName':name+' pet','animalNameEn':'Simulator Pet','infoAnimalId':animal,'dictSexId':1,'birthday':'2023-01-01','ownerName':'模拟畜主','ownerNameEn':'Test Owner'},data['session']['token']);assert r['code']==200,r
 pet=r['data'];data['petId']=pet.get('id') or pet.get('petId');assert data['petId'],pet
 state.write_text(json.dumps(data));state.chmod(0o600)
 print('Synthetic simulator doctor prepared; credentials stored privately outside Git')
finally:
 sql("DELETE FROM lin_sms WHERE mobile='"+phone+"';")
 if user_id is None:fixture.unlink(missing_ok=True)
