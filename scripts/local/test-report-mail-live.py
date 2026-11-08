"""Actual PC -> Java -> isolated DB -> owned loopback SMTP. Never uses a real SMTP profile."""
import importlib.util,json,secrets,subprocess,urllib.request,socket,socketserver,threading,time,os,signal,re,stat,email,email.policy
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
assert len(sys.argv)==1,'This report test supports the normal path only'
prefix='it_report_live_'+secrets.token_hex(5)
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated test port already in use')
except ConnectionRefusedError:pass
old=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-report-mail' AND scope_type='GLOBAL' AND scope_id=0")
assert not old or json.loads(old).get('enabled') is False
old_details=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")
main=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-main-orders' AND scope_type='GLOBAL' AND scope_id=0")
assert not main or json.loads(main).get('enabled') is False,'Live test uses the existing unified ADMIN order list'
received=[];recipients=[];commands=[]
class Handler(socketserver.StreamRequestHandler):
 def respond(self,value):self.wfile.write((value+'\r\n').encode());self.wfile.flush()
 def handle(self):
  self.request.settimeout(15);self.respond('220 owned-test SMTP')
  while True:
   line=self.rfile.readline().decode('ascii').rstrip('\r\n');commands.append(line)
   if not line:return
   if line.startswith(('EHLO','HELO')):self.respond('250 owned-test')
   elif line.startswith('MAIL FROM:'):self.respond('250 OK')
   elif line.startswith('RCPT TO:'):recipients.append(line[8:]);self.respond('250 OK')
   elif line=='DATA':
    self.respond('354 End with dot');data=[]
    while True:
     part=self.rfile.readline()
     if part==b'.\r\n':break
     if not part:raise ValueError('Incomplete SMTP DATA')
     data.append(part[1:] if part.startswith(b'..') else part)
    received.append(email.message_from_bytes(b''.join(data),policy=email.policy.default));self.respond('250 accepted locally')
   elif line=='RSET':self.respond('250 reset')
   elif line=='QUIT':self.respond('221 bye');return
   else:self.respond('550 unsupported test command')
server=socketserver.TCPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
process=None;hospital=order=contact=template=profile=detail=None;pdf_path=None;private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True)
try:
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES('+literal(prefix)+','+literal(prefix)+',0,20);SELECT LAST_INSERT_ID()'))
 columns=[line.split('\t') for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()];names=[];values=[]
 for parts in columns:
  name,kind,nullable,default,extra=(parts+['']*5)[:5]
  if 'auto_increment' in extra:continue
  if name not in ['UserNO','AnimalName','MasterName','SampleStatus','TestFee'] and (nullable=='YES' or default!='<NULL>'):continue
  assert name.replace('_','').isalnum();names.append('`'+name+'`')
  if name in ['UserNO','AnimalName','MasterName']:value=literal(prefix)
  elif name=='SampleStatus':value='2'
  elif name=='TestFee':value='125.50'
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:value='0'
  elif kind in ['datetime','timestamp']:value=literal('1900-01-01 00:00:00')
  elif kind=='date':value=literal('1900-01-01')
  elif kind in ['varchar','char','text','mediumtext','longtext']:value=literal(prefix)
  else:raise AssertionError('Unsupported fixture schema')
  values.append(value)
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'))
 sql(f'UPDATE lin_lb_listdiagnosisproject SET InfoHospitalId={hospital},IsSendNoticeMail=0 WHERE ListDiagnosisProjectId={order}')
 snapshot_columns=[parts[0] for parts in columns]
 snapshot_query="SELECT HEX(JSON_OBJECT("+','.join("'"+name+"',`"+name+"`" for name in snapshot_columns)+f")) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}"
 before_order=json.loads(bytes.fromhex(sql(snapshot_query)).decode())
 contact=int(sql(f"INSERT INTO lin_lb_infohospitalcontact(InfoHospitalId,isFinanceContact,Name,Tel,Mail,Fax,ContactRemark,CreateDt) VALUES({hospital},0,"+literal(prefix)+",'','doctor@example.test','','',NOW());SELECT LAST_INSERT_ID()"))

 profile=int(sql('SELECT COALESCE(MAX(DictMailServerId),0)+1 FROM lin_lb_dictmailserver'));sql(f"INSERT INTO lin_lb_dictmailserver VALUES({profile},'owned-test.invalid',587,'sender@laboklin.cn','synthetic-only','copy@example.test')")
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-report-mail','{\"enabled\":true,\"writeEnabled\":true,\"sendEnabled\":true,\"reconcileEnabled\":true}','sample notice UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-order-details','{\"enabled\":true,\"writeEnabled\":true}','report detail UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev '+f"'-Dspring-boot.run.arguments=--server.port=18082 --app.legacy-mail.mode=TEST_LOCAL --app.legacy-mail.local-test-port={server.server_address[1]} --app.legacy-mail.profile-id={profile}'"
 logpath=private/(prefix+'.log')
 def start_backend(restart=False):
  flags=os.O_WRONLY|(os.O_APPEND if restart else os.O_CREAT|os.O_EXCL)|os.O_NOFOLLOW
  fd=os.open(logpath,flags,0o600)
  with os.fdopen(fd,'a' if restart else 'w') as log:return subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 def wait_ready():
  for attempt in range(60):
   assert process.poll() is None,'Dedicated backend exited; see private log'
   try:
    with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as response:
     if response.status==200 and json.load(response).get('code')==200:return
   except Exception:time.sleep(.5)
  raise AssertionError('Dedicated backend readiness timeout')
 process=start_backend();wait_ready()
 mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php';info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077
 text=mapping.read_text();credentials={}
 for key in ['username','password']:
  match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",text);assert match;credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
 session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']});assert 'ADMIN' in session['roles']
 detail=int(staff.call(f'/admin/legacy/order-details/orders/{order}',{'testTypeId':246,'originalPrice':'12.50','originalDiscount':'10','testPrice':'12.50','detailRemark':prefix,'modelId':0,'mailTitle':'Report '+prefix,'isSendMail':0,'isCheckPass':0,'payStatus':0,'diagnosisStatus':0,'mailReport':'<p>Saved mail '+prefix+'</p>','reportResult':'<p>Saved report '+prefix+'</p>','requestKey':prefix+'_detail'},session['token']))
 before_order=json.loads(bytes.fromhex(sql(snapshot_query)).decode())
 detail_columns=sql("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_diagnosisdetail' ORDER BY ORDINAL_POSITION").splitlines();assert all(name.replace('_','').isalnum() for name in detail_columns)
 detail_query="SELECT HEX(JSON_OBJECT("+','.join("'"+name+"',d.`"+name+"`" for name in detail_columns)+f",'MailReport',i.MailReport,'ReportResult',i.ReportResult)) FROM lin_lb_diagnosisdetail d LEFT JOIN lin_lb_diagnosisdetail_info i ON i.diagnosisdetail_id=d.DiagnosisDetailId WHERE d.DiagnosisDetailId={detail}"
 before_detail=json.loads(bytes.fromhex(sql(detail_query)).decode())
 pdf_title=prefix+'.pdf';pdf_path=Path.home()/f'.local/share/petlis/reports/{order}_{detail}'/pdf_title;pdf_path.parent.mkdir(parents=True,exist_ok=True);assert not pdf_path.exists()
 rendered=subprocess.run(['node','scripts/editor-parity/legacy-html-pdf.js',str(pdf_path),'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'],cwd=ROOT,input='<p>Synthetic report '+prefix+'</p>',text=True,capture_output=True,timeout=40);assert rendered.returncode==0,'Synthetic fixture PDF rendering failed'
 fixture={'token':session['token'],'order':order,'contact':contact,'prefix':prefix,'detail':detail,'pdfTitle':pdf_title}
 run=subprocess.run(['node','scripts/editor-parity/report-mail-live.js'],cwd=ROOT,input=json.dumps(fixture),capture_output=True,text=True,timeout=90)
 assert run.returncode==0,'Browser failed: '+run.stderr[-1200:]
 proof=json.loads(run.stdout);message=int(proof['id']);assert sql(f"SELECT CONCAT(status,'|',callback_status,'|',attempt_count) FROM lin_mail_message WHERE id={message} AND business_id='{detail}'")=='SENT|DONE|1'
 after_order=json.loads(bytes.fromhex(sql(snapshot_query)).decode())
 assert before_order.pop('SampleStatus')==2 and after_order.pop('SampleStatus')==3
 import datetime,zoneinfo
 day=datetime.datetime.now(zoneinfo.ZoneInfo('Asia/Shanghai')).date().isoformat();before_order.pop('SendReportDt');assert after_order.pop('SendReportDt').replace('T',' ').startswith(day+' 00:00:00')
 assert before_order==after_order,'An unrelated order field changed'
 after_detail=json.loads(bytes.fromhex(sql(detail_query)).decode());assert before_detail.pop('IsSendMail')==0 and after_detail.pop('IsSendMail')==1;before_detail.pop('SendMailDt');assert after_detail.pop('SendMailDt').replace('T',' ').startswith(day+' 00:00:00');assert before_detail==after_detail,'Report body, price or unrelated detail field changed' 
 assert len(received)==1 and len(recipients)==2 and not any(c.startswith('AUTH') for c in commands)
 assert prefix in received[0].get_body(preferencelist=('html',)).get_content();assert len(list(received[0].iter_attachments()))==0
 assert received[0]['Message-ID']==sql(f'SELECT smtp_message_id FROM lin_mail_attempt WHERE message_id={message} ORDER BY attempt_no DESC LIMIT 1')
 proof.update(smtpAcceptedExactlyOnce=True,noAttachmentsAsSource=True,onlyOriginalReportSendFieldsChanged=True,stableMessageIdVerified=True)
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 server.shutdown();server.server_close()
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old)+" WHERE config_key='legacy-report-mail' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-report-mail' AND description='sample notice UI fixture'")
 if old_details:sql('UPDATE lin_operational_config SET config_value='+literal(old_details)+" WHERE config_key='legacy-order-details' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-order-details' AND description='report detail UI fixture'")
 if order:
  assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==prefix
  mids=sql(f"SELECT id FROM lin_mail_message WHERE business_type='REPORT_MAIL' AND business_id='{detail}'").splitlines()
  for mid in mids:
   assert mid.isdecimal();sql(f"DELETE FROM lin_mail_attempt WHERE message_id={mid};DELETE FROM lin_audit_record WHERE resource_type='LEGACY_REPORT_MAIL' AND resource_id='{mid}';DELETE FROM lin_mail_message WHERE id={mid} AND business_id='{detail}'")
  if detail:
   sql(f"DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={detail};DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={detail} AND ListDiagnosisProjectId={order}")
   sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_ORDER_DETAIL' AND request_id="+literal(prefix+'_detail')+f" AND resource_id='{detail}'")
  sql(f'DELETE FROM lin_lb_notification WHERE order_id={order} AND animal_name='+literal(prefix))
  sql(f'DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+literal(prefix))
 if contact:sql(f'DELETE FROM lin_lb_infohospitalcontact WHERE InfoHospitalContactId={contact} AND InfoHospitalId={hospital}')
 if profile:sql(f"DELETE FROM lin_lb_dictmailserver WHERE DictMailServerId={profile} AND ServerHost='owned-test.invalid'")
 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+literal(prefix))
 if pdf_path and pdf_path.exists():
  pdf_path.unlink()
  try:pdf_path.parent.rmdir()
  except OSError:pass
proof.update(gateRestored=True,fixturesRemoved=True,backendStopped=True)
print(json.dumps(proof))
