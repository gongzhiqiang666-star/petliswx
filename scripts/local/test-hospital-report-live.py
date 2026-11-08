"""Own doctor/hospitals/drafts on local13306, real browser, exact cleanup and own backend only."""
import importlib.util,json,os,re,secrets,signal,socket,subprocess,time,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff);staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_hreport_pc_'+secrets.token_hex(5)
def lit(s):return 'CONVERT(0x'+s.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
key='legacy-hospital-history';where=" WHERE config_key='legacy-hospital-history' AND scope_type='GLOBAL' AND scope_id=0"
old_raw=sql("SELECT HEX(JSON_OBJECT('value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config"+where);old=json.loads(bytes.fromhex(old_raw).decode()) if old_raw else None
assert not old or old['status']!='ACTIVE' or (old['value'] if isinstance(old['value'],dict) else json.loads(old['value'])).get('enabled') is False,'Do not override an active gate'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated port occupied')
except ConnectionRefusedError:pass
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
report_root=private/(prefix+'-pdfs');report_root.mkdir(mode=0o700);report_file=report_root/'owned.pdf'
objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 200] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'];stream=b'BT /F1 12 Tf 20 160 Td (Owned historical report) Tj ET\n';objects.append(b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'endstream');pdf=b'%PDF-1.4\n';offsets=[0]
for index,obj in enumerate(objects,1):offsets.append(len(pdf));pdf+=str(index).encode()+b' 0 obj\n'+obj+b'\nendobj\n'
xref=len(pdf);pdf+=b'xref\n0 6\n0000000000 65535 f \n'+b''.join(('%010d 00000 n \n'%v).encode() for v in offsets[1:])+b'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n'+str(xref).encode()+b'\n%%EOF\n';report_file.write_bytes(pdf);os.chmod(report_file,0o600)
process=None;hospitals=[];orders=[];doctor=None;test=None;species=None;details=[];changed=False;proof={}
image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jGJkAAAAASUVORK5CYII='
html='<p><strong>合成送检备注</strong></p><table border="1"><tbody><tr><td colspan="2"><p>冷藏样本</p></td></tr></tbody></table><p><img src="'+image+'" width="12" height="12"></p>'
try:
 species=int(sql('INSERT INTO lin_lb_infoanimal(AnimalName,AnimalName_en,IsActive) VALUES('+lit(prefix)+','+lit(prefix)+',1);SELECT LAST_INSERT_ID()'))
 for suffix in ['own','foreign']:
  hospitals.append(int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,IsActive,CityId,PayType,amount) VALUES('+lit(prefix+'_'+suffix)+','+lit(prefix+'_'+suffix)+',1,1,1,0);SELECT LAST_INSERT_ID()')))
 password=secrets.token_hex(18)+'A7';salt=secrets.token_hex(5);first=hashlib.md5(password.encode()).hexdigest();hashed=hashlib.md5((first+salt).encode()).hexdigest()
 doctor=int(sql('INSERT INTO lin_user(username,nickname,status,hospital_id,password,salt,loginfailure) VALUES('+lit(prefix)+",'合成医院医生','normal',"+str(hospitals[0])+','+lit(hashed)+','+lit(salt)+',0);SELECT LAST_INSERT_ID()'))
 columns=[(line.split('\t')+['']*5)[:5] for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()]
 for index,hospital in enumerate([hospitals[0],hospitals[1],hospitals[0]]):
  names=[];values=[];given={'InfoAnimalId':str(species),'UserNO':lit(prefix+'_'+str(hospital)),'AnimalName':lit('合成动物'),'MasterName':lit('合成畜主'),'DoctorName':lit('合成医师'),'InfoHospitalId':str(hospital),'SampleStatus':'4' if index==2 else '2','IsPayMoney':'0','CreateSampleId':'0','TestFee':'0','Remark':lit(html)}
  for name,kind,nullable,default,extra in columns:
   if 'auto_increment' in extra:continue
   if name not in given and (nullable=='YES' or default!='<NULL>'):continue
   assert re.fullmatch('[A-Za-z0-9_]+',name);names.append('`'+name+'`')
   if name in given:value=given[name]
   elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
   elif kind in ['datetime','timestamp','date']:value=lit('1900-01-01')
   elif kind in ['varchar','char','text','mediumtext','longtext']:value=lit(prefix)
   else:raise AssertionError('Unexpected fixture field')
   values.append(value)
  orders.append(int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()')))
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+lit(prefix)+','+lit(prefix)+",0,0,0,0,0,'',NOW(),1,1);SELECT LAST_INSERT_ID()"))
 detail_columns=[(line.split('\t')+['']*5)[:5] for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_diagnosisdetail' ORDER BY ORDINAL_POSITION").splitlines()]
 for index,order in enumerate(orders):
  names=[];values=[];given={'ListDiagnosisProjectId':str(order),'InfoTestTypeId':str(test),'IsSendMail':'1','PDFHttpUrl':lit('https://old.invalid/Content/FilePdf/owned.pdf'),'IsActive':'1','IsCheckPass':'0','TestPrice':'8.25'}
  for name,kind,nullable,default,extra in detail_columns:
   if 'auto_increment' in extra:continue
   if name not in given and (nullable=='YES' or default!='<NULL>'):continue
   assert re.fullmatch('[A-Za-z0-9_]+',name);names.append('`'+name+'`')
   if name in given:value=given[name]
   elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
   elif kind in ['datetime','timestamp','date']:value=lit('1900-01-01')
   elif kind in ['varchar','char','text','mediumtext','longtext']:value=lit(prefix)
   else:raise AssertionError('Unexpected detail field')
   values.append(value)
  detail=int(sql('INSERT INTO lin_lb_diagnosisdetail('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'));details.append(detail)
  sql(f'INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES({detail},'+lit('合成邮件正文')+','+lit('合成报告正文')+')')
 sql('INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('+lit(key)+','+lit(json.dumps({'enabled':True,'writeEnabled':True,'draftEnabled':False,'reportDownloadEnabled':True}))+','+lit(prefix)+','+str(doctor)+") ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'");changed=True
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export APP_LEGACY_REPORTS_STORAGE_PATH="$PETLIS_QA_LEGACY_REPORT_ROOT"; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18082'
 fd=os.open(private/(prefix+'.log'),os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as log:process=subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True,env={**os.environ,'PETLIS_QA_LEGACY_REPORT_ROOT':str(report_root)})
 for attempt in range(60):
  assert process.poll() is None,'Owned backend exited'
  try:
   with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:
    if r.status==200:break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Readiness timeout')
 session=staff.call('/auth/password/login',{'account':prefix,'password':password});assert set(session['roles'])=={'DOCTOR'}
 all_names=[c[0] for c in columns];assert all(re.fullmatch('[A-Za-z0-9_]+',n) for n in all_names)
 expression='JSON_OBJECT('+','.join("'"+n+"',`"+n+'`' for n in all_names)+')'
 def parent(order):return json.loads(bytes.fromhex(sql('SELECT HEX(CAST('+expression+' AS CHAR)) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(order))).decode())
 before=[parent(i) for i in orders]
 def detail_state():return sql('SELECT DiagnosisDetailId,HEX(CAST(JSON_OBJECT('+','.join("'"+r[0]+"',`"+r[0]+'`' for r in detail_columns)+') AS CHAR)) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN ('+','.join(map(str,orders))+') ORDER BY DiagnosisDetailId')+'|'+sql('SELECT diagnosisdetail_id,HEX(MailReport),HEX(ReportResult) FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id IN ('+','.join(map(str,details))+') ORDER BY diagnosisdetail_id')
 detail_before=detail_state()
 funds_before=sql(f"SELECT HEX(JSON_OBJECT('money',money,'score',score)) FROM lin_user WHERE id={doctor}")+'|'+sql(f"SELECT HEX(JSON_OBJECT('amount',amount)) FROM lin_lb_infohospital WHERE InfoHospitalId={hospitals[0]}")

 f={'token':session['token'],'user':session['user'],'hospital':hospitals[0],'foreignHospital':hospitals[1],'order':orders[0],'foreignOrder':orders[1],'detail':details[0],'foreignDetail':details[1],'cancelledOrder':orders[2],'cancelledDetail':details[2],'sha256':hashlib.sha256(pdf).hexdigest(),'screenshot':str(private/(prefix+'.png'))}
 result=subprocess.run(['node','scripts/editor-parity/hospital-report-live.js'],cwd=ROOT,input=json.dumps(f),capture_output=True,text=True,timeout=100);assert result.returncode==0,'Chrome acceptance failed: '+result.stderr[-1300:];proof=json.loads(result.stdout)
 assert parent(orders[0])==before[0];assert parent(orders[1])==before[1];assert parent(orders[2])==before[2]
 assert detail_state()==detail_before
 assert sql(f"SELECT HEX(JSON_OBJECT('money',money,'score',score)) FROM lin_user WHERE id={doctor}")+'|'+sql(f"SELECT HEX(JSON_OBJECT('amount',amount)) FROM lin_lb_infohospital WHERE InfoHospitalId={hospitals[0]}")==funds_before
 assert sql("SELECT COUNT(*) FROM lin_audit_record WHERE actor_user_id="+str(doctor)+" AND resource_type IN ('LEGACY_HOSPITAL_CANCEL','LEGACY_HOSPITAL_DRAFT')")=='0'
 assert report_file.read_bytes()==pdf
 report_file.unlink()
 request=__import__('urllib.request',fromlist=['Request']).Request(staff.config.BASE+f'/orders/hospital-history/reports/{details[0]}/pdf?hospitalId={hospitals[0]}',headers={'Authorization':'Bearer '+session['token']})
 with staff.opener.open(request) as response:
  body=json.load(response);assert body['code']==404 and '尚未取回' in body['message']
 proof.update(allBusinessRowsAndBodiesUnchanged=True,twoFundBalancesAndScoreUnchanged=True,missingFileExplicitNoReplacement=True,noBusinessWrites=True)
finally:
 if report_file.exists():report_file.unlink()
 if report_root.exists():report_root.rmdir()
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 if changed:
  if old:
   value=old['value'];value=json.dumps(value,ensure_ascii=False) if isinstance(value,dict) else value
   sql('UPDATE lin_operational_config SET config_value='+lit(value)+',status='+lit(old['status'])+',updated_at='+lit(old['updated_at'])+where)
  else:sql('DELETE FROM lin_operational_config'+where+' AND description='+lit(prefix))
 for detail in details:sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={detail}')
 for index,order in enumerate(orders):
  hospital=hospitals[index] if index<2 else hospitals[0]
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix+'_'+str(hospital)
  sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_HOSPITAL_CANCEL' AND resource_id="+lit(str(order))+f' AND actor_user_id={doctor};DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order};DELETE FROM lin_lb_notification WHERE order_id={order};DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+lit(prefix+'_'+str(hospital)))
 if species:sql(f'DELETE FROM lin_lb_infoanimal WHERE InfoAnimalId={species} AND AnimalName='+lit(prefix))
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestName='+lit(prefix))
 if doctor:sql(f'DELETE FROM lin_doctor_profile WHERE user_id={doctor};DELETE FROM lin_user WHERE id={doctor} AND username='+lit(prefix))
 for hospital in hospitals:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName LIKE '+lit(prefix+'_%'))
assert sql("SELECT HEX(JSON_OBJECT('value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config"+where)==old_raw
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Owned port still open')
except ConnectionRefusedError:pass
assert sql('SELECT COUNT(*) FROM lin_user WHERE username='+lit(prefix))=='0';assert sql('SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE UserNO LIKE '+lit(prefix+'_%'))=='0';assert sql('SELECT COUNT(*) FROM lin_lb_infohospital WHERE HospitalName LIKE '+lit(prefix+'_%'))=='0'
proof.update(gateRestored=True,fixturesRemoved=True,backendStopped=True);print(json.dumps(proof))
