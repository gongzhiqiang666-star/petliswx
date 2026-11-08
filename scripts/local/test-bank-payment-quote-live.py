"""Owned local quote, preparation and simulated settlement; never calls a payment provider."""
import urllib.parse,shlex,base64,sys,importlib.util,json,secrets,subprocess,socket,time,os,signal,re,stat,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
REPORT_DOWNLOAD='--customer-reports' in sys.argv
CUSTOMER='--customer-views' in sys.argv or REPORT_DOWNLOAD
REFUND='--refund' in sys.argv
NOTIFY='--notify' in sys.argv
DISPATCH_ROLLBACK='--dispatch-rollback' in sys.argv
DISPATCH='--dispatch' in sys.argv or DISPATCH_ROLLBACK or NOTIFY or REFUND
PREPARE='--prepare' in sys.argv or DISPATCH
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql;prefix='it_bank_quote_live_'+secrets.token_hex(5)
def lit(v):return 'CONVERT(0x'+v.encode().hex()+' USING utf8mb4) COLLATE utf8mb4_unicode_ci'
try:
 with socket.create_connection(('127.0.0.1',18082),timeout=.2):raise AssertionError('Dedicated port already in use')
except ConnectionRefusedError:pass
keys=['legacy-bank-payments','legacy-order-details']+(['legacy-customer-views'] if CUSTOMER else []);old={}
for key in keys:
 value=sql("SELECT HEX(JSON_OBJECT('config_value',config_value,'status',status,'updated_at',DATE_FORMAT(updated_at,'%Y-%m-%d %H:%i:%s'))) FROM lin_operational_config WHERE config_key="+lit(key)+" AND scope_type='GLOBAL' AND scope_id=0")
 old[key]=json.loads(bytes.fromhex(value).decode()) if value else None
 assert not old[key] or (old[key]['config_value'] if isinstance(old[key]['config_value'],dict) else json.loads(old[key]['config_value'])).get('enabled') is False,'Do not override an enabled development gate'
private=Path.home()/'.local/share/petlis/qa';private.mkdir(mode=0o700,parents=True,exist_ok=True);process=None;hospital=test=None;orders=[];details=[];payments=[];request_keys=[];gateway_fixture=None;animal=None;paid=None;customer_proof={};report_root=None;owned_report=None;downloaded_report=None
def make_order(number,date):
 columns=[line.split('\t') for line in sql("SELECT COLUMN_NAME,DATA_TYPE,IS_NULLABLE,IFNULL(COLUMN_DEFAULT,'<NULL>'),EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='lin_lb_listdiagnosisproject' ORDER BY ORDINAL_POSITION").splitlines()];names=[];values=[]
 for parts in columns:
  name,kind,nullable,default,extra=(parts+['']*5)[:5]
  if 'auto_increment' in extra:continue
  if name not in (['UserNO','AnimalName','MasterName','SampleStatus','TestFee','InfoHospitalId','ReceiveSampleDt']+(['InfoAnimalId'] if CUSTOMER else [])) and (nullable=='YES' or default!='<NULL>'):continue
  assert re.fullmatch('[A-Za-z0-9_]+',name);names.append('`'+name+'`')
  if name=='UserNO':v=lit(number)
  elif name in ['AnimalName','MasterName']:v=lit(prefix)
  elif name=='InfoHospitalId':v=str(hospital)
  elif name=='InfoAnimalId' and CUSTOMER:v=str(animal)
  elif name=='ReceiveSampleDt':v=lit(date)
  elif kind in ['int','tinyint','smallint','mediumint','bigint','decimal','double','float']:v='0'
  elif kind in ['datetime','timestamp']:v=lit('1900-01-01 00:00:00')
  elif kind=='date':v=lit('1900-01-01')
  elif kind in ['varchar','char','text','mediumtext','longtext']:v=lit(prefix)
  else:raise AssertionError('Unexpected fixture column')
  values.append(v)
 order=int(sql('INSERT INTO lin_lb_listdiagnosisproject('+','.join(names)+') VALUES('+','.join(values)+');SELECT LAST_INSERT_ID()'));orders.append((order,number));return order
TABLES=['lin_lb_infohospital','lin_lb_listdiagnosisproject','lin_lb_diagnosisdetail','lin_lb_diagnosisdetail_info','lin_lb_payordermain','lin_lb_payorderdetail','lin_lb_paylog']
def snapshot(ignore_payments=False):
 result={}
 for table in TABLES:
  where=''
  if ignore_payments and table=='lin_lb_payordermain':where=' WHERE Remark<>'+lit('订单:'+prefix)
  if ignore_payments and table=='lin_lb_payorderdetail':where=' WHERE MainOrderId NOT IN (SELECT OrderId FROM lin_lb_payordermain WHERE Remark='+lit('订单:'+prefix)+')'
  if DISPATCH and table=='lin_lb_paylog':where=' WHERE TradeNoUser NOT IN (SELECT OrderId FROM lin_lb_payordermain WHERE Remark='+lit('订单:'+prefix)+')'
  names=sql("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"+table+"' ORDER BY ORDINAL_POSITION").splitlines();assert names and all(re.fullmatch('[A-Za-z0-9_]+',n) for n in names)
  expressions={n:'`'+n+'`' for n in names}
  if DISPATCH and table=='lin_lb_listdiagnosisproject':expressions['IsPayMoney']='CASE WHEN ListDiagnosisProjectId='+str(unreceived)+' THEN '+str(original_parent_pay)+' ELSE IsPayMoney END'
  if DISPATCH and table=='lin_lb_diagnosisdetail':expressions['PayStatus2']='CASE WHEN DiagnosisDetailId='+str(details[0][0])+' THEN 0 ELSE PayStatus2 END'
  rows=sql('SELECT SHA2(CAST(JSON_OBJECT('+','.join("'"+n+"',"+expressions[n] for n in names)+') AS CHAR),256) FROM '+table+where).splitlines();result[table]=hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()
 return result
try:
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount) VALUES('+lit(prefix)+','+lit(prefix)+','+('0' if CUSTOMER else '1')+',20);SELECT LAST_INSERT_ID()'))
 test=int(sql('INSERT INTO lin_lb_infotesttype(TestName,TestNO,Price,CtoG,GtoC,DiscountReal,DiscountNext,TextModel,CreateDt,type,IsActive) VALUES('+lit(prefix)+','+lit(prefix)+",0,0,0,0,0,'',NOW(),0,1);SELECT LAST_INSERT_ID()"))
 if CUSTOMER:animal=int(sql('SELECT MIN(InfoAnimalId) FROM lin_lb_infoanimal'))
 unreceived=make_order(prefix,'1900-01-01 00:00:00');received=make_order(prefix+'_received','2024-02-29 23:59:58')
 if CUSTOMER:paid=make_order(prefix+'_paid','2024-03-01 12:00:00')
 for key in keys:
  value=json.dumps({'enabled':True,**({'writeEnabled':True} if key=='legacy-order-details' else ({'prepareEnabled':True,**({'sendEnabled':True,'queryEnabled':True,**({'notifyEnabled':True} if NOTIFY else {}),**({'refundEnabled':True,'refundQueryEnabled':True} if REFUND else {})} if DISPATCH else {})} if PREPARE else {}))})
  if key=='legacy-customer-views':value=json.dumps({'enabled':True,**({'reportDownloadEnabled':True} if REPORT_DOWNLOAD else {})})
  sql('INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('+lit(key)+','+lit(value)+",'bank quote UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),status='ACTIVE'")
 command='set -a; source "$HOME/.local/share/petlis/mysql/backend.env"; set +a; [[ "$DB_URL" == "jdbc:mysql://127.0.0.1:13306/laboklin?"* ]] || exit 17; export JAVA_HOME="$HOME/.local/share/petlis/tools/Home" MAVEN_HOME="$HOME/.local/share/petlis/tools/libexec" APP_SCHEDULING_ENABLED=false DB_MIGRATIONS_ENABLED=false PDF_STORAGE_PATH="$HOME/.local/share/petlis/reports"; exec ./petlis-backend/mvnw -o -q -f petlis-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18082'
 if PREPARE:command=command.replace('-Dspring-boot.run.arguments=--server.port=18082',"'-Dspring-boot.run.arguments=--server.port=18082 --app.legacy-bank.merchant=TESTLOCAL'")
 if DISPATCH:
  from bank_gateway_fixture import BankGatewayFixture
  def verify_settlement_rollback(payment):
   assert payment.startswith('TESTLOCAL') and payment.isalnum()
   assert sql('SELECT AliTradeId FROM lin_lb_payordermain WHERE OrderId='+lit(payment))==''
   assert sql('SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser='+lit(payment))=='0'
   assert sql('SELECT PayStatus2 FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId='+str(details[0][0]))=='0'
   assert sql('SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(unreceived))==str(original_parent_pay)
  gateway_fixture=BankGatewayFixture(private,prefix,DISPATCH_ROLLBACK,verify_settlement_rollback if DISPATCH_ROLLBACK else None)
  command=command.replace("--app.legacy-bank.merchant=TESTLOCAL'","--app.legacy-bank.merchant=TESTLOCAL --app.legacy-bank.mode=TEST_LOCAL --app.legacy-bank.app-id=owned-test --app.legacy-bank.endpoint="+gateway_fixture.endpoint+" --app.legacy-bank.private-key="+str(gateway_fixture.private_key)+" --app.legacy-bank.public-key="+str(gateway_fixture.public_key)+" --app.legacy-bank.timeout-ms=1500'")
 if REPORT_DOWNLOAD:
  report_root=private/(prefix+'_report_root');report_root.mkdir(mode=0o700);owned_report=report_root/'owned-客户报告.pdf';downloaded_report=private/(prefix+'_download.pdf')
  stream=b'BT /F1 12 Tf 50 780 Td (OWNED CUSTOMER REPORT) Tj ET'
  objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream']
  pdf=bytearray(b'%PDF-1.4\n');offsets=[0]
  for i,obj in enumerate(objects,1):offsets.append(len(pdf));pdf+=str(i).encode()+b' 0 obj\n'+obj+b'\nendobj\n'
  xref=len(pdf);pdf+=b'xref\n0 6\n0000000000 65535 f \n'+b''.join(('%010d 00000 n \n'%offset).encode() for offset in offsets[1:])+b'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n'+str(xref).encode()+b'\n%%EOF\n'
  fd=os.open(owned_report,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
  with os.fdopen(fd,'wb') as artifact:artifact.write(pdf)
  valid=subprocess.run(['/opt/homebrew/bin/pdfinfo',str(owned_report)],capture_output=True,text=True,timeout=10);assert valid.returncode==0 and re.search(r'Pages:\s+1\b',valid.stdout),'Owned PDF fixture must parse as one page'
  command=command.replace('exec ./petlis-backend/mvnw','export APP_LEGACY_REPORTS_STORAGE_PATH='+shlex.quote(str(report_root))+'; exec ./petlis-backend/mvnw')
 if CUSTOMER:command=command.replace('exec ./petlis-backend/mvnw','export APP_LEGACY_LINK_KEY_PHRASE=public-customer-view-test; exec ./petlis-backend/mvnw')
 fd=os.open(private/(prefix+'.log'),os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
 with os.fdopen(fd,'w') as log:process=subprocess.Popen(['/bin/zsh','-c',command],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
 for attempt in range(60):
  assert process.poll() is None,'Owned backend exited'
  try:
   with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as r:
    if r.status==200 and json.load(r).get('code')==200:break
  except Exception:time.sleep(.5)
 else:raise AssertionError('Owned backend readiness timeout')
 mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php';info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077;text=mapping.read_text();credentials={}
 for key in ['username','password']:
  match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",text);assert match;credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
 session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']});assert 'ADMIN' in session['roles']
 fixture_details=[(unreceived,246,'12.50',0,0),(unreceived,test,'9.99',0,0),(received,test,'7.25',0,0),(received,test,'1.00',1,0),(received,test,'2.00',0,1)]
 if CUSTOMER:fixture_details.append((paid,test,'4.50',1,0))
 for order,kind,price,paid_status,exempt in fixture_details:
  request=prefix+'_detail_'+str(len(details));body={'testTypeId':kind,'originalPrice':price,'originalDiscount':'10','testPrice':price,'detailRemark':prefix,'modelId':0,'mailTitle':'','isSendMail':0,'isCheckPass':exempt,'payStatus':paid_status,'diagnosisStatus':0,'mailReport':'','reportResult':'','requestKey':request};detail=int(staff.call(f'/admin/legacy/order-details/orders/{order}',body,session['token']));details.append((detail,order,request))
 if CUSTOMER:
  report_reference=('https://old.example.invalid/Content/FilePdf/'+urllib.parse.quote(owned_report.name) if REPORT_DOWNLOAD else 'https://petlis.invalid/owned.pdf')
  sql('UPDATE lin_lb_diagnosisdetail SET PDFHttpUrl='+lit(report_reference)+' WHERE DiagnosisDetailId='+str(details[-1][0]))
  def cipher(value):
   key=hashlib.md5(b'DDMpublic-customer-view-testDDM').hexdigest()[:8].encode().hex();r=subprocess.run(['/usr/bin/openssl','enc','-des-cbc','-nosalt','-K',key,'-iv',key],input=str(value).encode(),capture_output=True,timeout=10);assert r.returncode==0,'Owned public-key cipher fixture failed';return base64.b64encode(r.stdout).decode().replace('+','!').replace('=','-')
  customer_proof={'detail':details[0][0],'paidDetail':details[-1][0],'detailToken':cipher(details[0][0]),'hospitalToken':cipher(hospital),'reportDownload':REPORT_DOWNLOAD,'reportReference':report_reference,**({'downloadPath':str(downloaded_report),'reportName':owned_report.name,'reportSha':hashlib.sha256(owned_report.read_bytes()).hexdigest(),'reportSize':owned_report.stat().st_size} if REPORT_DOWNLOAD else {})}
 original_parent_pay=int(sql(f'SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={unreceived}'))
 before=snapshot(ignore_payments=PREPARE);run=subprocess.run(['node','scripts/editor-parity/'+('legacy-customer-views-live.js' if CUSTOMER else 'bank-payment-quote-live.js')],cwd=ROOT,input=json.dumps({'token':session['token'],'prefix':prefix,'hospital':hospital,'prepare':PREPARE,'dispatch':DISPATCH,'dispatchRollback':DISPATCH_ROLLBACK,'notify':NOTIFY,'refund':REFUND,'noticeUrl':(gateway_fixture.endpoint.rsplit('/',1)[0]+'/notification' if NOTIFY else None),'screenshot':str(private/(prefix+'.png')),**customer_proof}),capture_output=True,text=True,timeout=90);assert run.returncode==0,'Browser failed: '+run.stderr[-1000:];proof=json.loads(run.stdout)
 if REPORT_DOWNLOAD:
  assert downloaded_report.is_file() and hashlib.sha256(downloaded_report.read_bytes()).hexdigest()==customer_proof['reportSha']
  text=subprocess.run(['/opt/homebrew/bin/pdftotext',str(downloaded_report),'-'],capture_output=True,text=True,timeout=10);assert text.returncode==0 and 'OWNED CUSTOMER REPORT' in text.stdout
  proof['downloadedPdfBytesAndTextVerified']=True
 if PREPARE:
  rows=sql('SELECT OrderId FROM lin_lb_payordermain WHERE Remark='+lit('订单:'+prefix)).splitlines();payments.extend(rows);assert len(rows)==1,'Expected one payment despite uncertain first response'
  request_keys.extend(proof['requestKeys']);assert len(set(request_keys))==1
  assert sql('SELECT CONCAT(HostId,":",IsSuccess,":",AliTradeId,":",CHKMoney) FROM lin_lb_payordermain WHERE OrderId='+lit(rows[0]))==('0:0:'+prefix+'_channel:12.5000' if DISPATCH else '0:0::12.5000')
  assert sql('SELECT COUNT(*) FROM lin_lb_payorderdetail WHERE MainOrderId='+lit(rows[0]))=='1'
 if DISPATCH:
  assert not gateway_fixture.errors,'Gateway fixture failed: '+str(gateway_fixture.errors)
  assert gateway_fixture.prepay==1 and gateway_fixture.queries in ([0,1] if NOTIFY else [2 if DISPATCH_ROLLBACK else 1]),'Duplicate network execution'
  assert sql('SELECT COUNT(*) FROM lin_lb_paylog WHERE TradeNoUser='+lit(payments[0]))=='1'
  assert sql('SELECT CONCAT(PayStatus2) FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId='+str(details[0][0]))=='3'
  assert sql('SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId='+str(unreceived))=='3'
  if REFUND:
   assert gateway_fixture.refunds==1 and gateway_fixture.refund_queries==1
   assert sql("SELECT COUNT(*) FROM lin_order_idempotency WHERE operation='LEGACY_BANK_REFUND' AND status='BANK_RESPONSE' AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.paymentOrderId'))="+lit(payments[0]))=='1'
   proof['refundSentOnceQueriedOnceNoClinicalReversal']=True
  if NOTIFY:assert sql('SELECT BuyerId FROM lin_lb_paylog WHERE TradeNoUser='+lit(payments[0])) in ['owned-notice-buyer','owned-test-buyer']
  proof.update(providerSendCount=gateway_fixture.prepay,providerQueryCount=gateway_fixture.queries,ledgerRows=1,settlementRollbackRecovered=DISPATCH_ROLLBACK)
 assert snapshot(ignore_payments=PREPARE)==before,'Clinical, fund or unrelated business records changed';proof['onlyExpectedClinicalPaymentFlagsChanged' if DISPATCH else 'clinicalAndFundRecordsUnchanged']=True;proof['unrelatedPaymentRecordsUnchanged']=True;proof['fundAndReportRecordsUnchanged']=True
finally:
 if process and process.poll() is None:
  os.killpg(process.pid,signal.SIGTERM)
  try:process.wait(timeout=8)
  except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=5)
 if gateway_fixture:gateway_fixture.close()
 if report_root:
  if owned_report and owned_report.exists():owned_report.unlink()
  report_root.rmdir()
 # Discover only owned rows even when the browser failed before returning its proof.
 if PREPARE and hospital:
  payments=sql('SELECT OrderId FROM lin_lb_payordermain WHERE Remark='+lit('订单:'+prefix)).splitlines()
  for payment in payments:
   assert payment.startswith('TESTLOCAL') and payment.isalnum()
   if REFUND:
    refund_rows=sql("SELECT HEX(JSON_OBJECT('key',idempotency_key,'order',JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.refundOrderId')))) FROM lin_order_idempotency WHERE operation='LEGACY_BANK_REFUND' AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.paymentOrderId'))="+lit(payment)).splitlines()
    for encoded in refund_rows:
     refund=json.loads(bytes.fromhex(encoded).decode());assert re.fullmatch('[A-Za-z0-9_-]{1,128}',refund['key']) and refund['order'].startswith('TESTLOCAL') and refund['order'].isalnum()
     sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_BANK_REFUND' AND resource_id="+lit(refund['order'])+' AND request_id='+lit(refund['key']))
    sql("DELETE FROM lin_order_idempotency WHERE operation='LEGACY_BANK_REFUND' AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.paymentOrderId'))="+lit(payment))
   owned=sql("SELECT idempotency_key FROM lin_order_idempotency WHERE operation='LEGACY_BANK_PREPARE' AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.orderId'))="+lit(payment)).splitlines()
   for request in owned:
    assert re.fullmatch('[A-Za-z0-9_-]{1,128}',request)
    sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_BANK_PAYMENT' AND resource_id="+lit(payment)+' AND request_id='+lit(request))
   sql("DELETE FROM lin_order_idempotency WHERE operation='LEGACY_BANK_PREPARE' AND JSON_UNQUOTE(JSON_EXTRACT(response_body,'$.orderId'))="+lit(payment))
   if DISPATCH:sql('DELETE FROM lin_lb_paylog WHERE TradeNoUser='+lit(payment)+' AND TradeNoAli='+lit(prefix+'_channel'))
   sql('DELETE FROM lin_lb_payorderdetail WHERE MainOrderId='+lit(payment)+';DELETE FROM lin_lb_payordermain WHERE OrderId='+lit(payment)+' AND Remark='+lit('订单:'+prefix))
 for detail,order,request in details:
  assert detail>0 and order>0 and sql(f'SELECT DetailRemark FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={detail} AND ListDiagnosisProjectId={order}')==prefix
  sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id={detail};DELETE FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={detail} AND ListDiagnosisProjectId={order}')
  sql("DELETE FROM lin_audit_record WHERE resource_type='LEGACY_ORDER_DETAIL' AND resource_id='"+str(detail)+"' AND request_id="+lit(request))
 for order,number in orders:
  assert order>0 and sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}')==number
  sql(f'DELETE FROM lin_lb_notification WHERE order_id={order} AND animal_name='+lit(prefix));sql(f'DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order} AND UserNO='+lit(number))
 if test:sql(f'DELETE FROM lin_lb_infotesttype WHERE InfoTestTypeId={test} AND TestNO='+lit(prefix))
 if hospital:sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName='+lit(prefix))
 for key,previous in old.items():
  if previous:sql('UPDATE lin_operational_config SET config_value='+lit(json.dumps(previous['config_value']) if isinstance(previous['config_value'],dict) else previous['config_value'])+',status='+lit(previous['status'])+',updated_at='+lit(previous['updated_at'])+' WHERE config_key='+lit(key)+" AND scope_type='GLOBAL' AND scope_id=0")
  else:sql('DELETE FROM lin_operational_config WHERE config_key='+lit(key)+" AND description='bank quote UI fixture'")
proof.update(gatesRestored=True,fixturesRemoved=True,backendStopped=True);print(json.dumps(proof))
