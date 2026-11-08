"""Read-only clinician/foreign/anonymous verification of the completed SF synthetic report chain."""
import importlib.util,json,hashlib,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';chain=json.loads((root/'qa/sf-third-php-report-chain.json').read_text());assert chain['orderId']==23956
for _ in range(20):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=1) as response:assert response.status==200
  break
 except OSError:time.sleep(1)
else:raise AssertionError('Petlis backend unavailable')
f=json.loads((root/'qa/native-sf-history-private.json').read_text());doctor=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token'];orig=json.loads((root/'simulator-fixture.json').read_text());foreign=staff.call('/auth/password/login',{'account':orig['account'],'password':orig['password']})['token']
def get(path,token=None):
 req=urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+token} if token else {})
 try:
  with staff.opener.open(req,timeout=20) as response:content=response.read();ctype=response.headers.get('Content-Type','');status=response.status
 except urllib.error.HTTPError as error:content=error.read();ctype=error.headers.get('Content-Type','');status=error.code
 if 'json' in ctype and content:
  data=json.loads(content);return data.get('code',status),data.get('data')
 return status,content
assert get('/reports/33815/download',foreign)[0]==200,'Foreign identity must still access its own known report'
reports=[]
for report in chain['reportIds']:
 code,details=get(f'/reports/{report}',doctor);assert code==200 and not details.get('paymentRestricted',False)
 code,pdf=get(f'/reports/{report}/download',doctor);assert code==200 and pdf.startswith(b'%PDF-');path=root/f'qa/sf-third-report-{report}.pdf';path.write_bytes(pdf);path.chmod(0o600)
 assert get(f'/reports/{report}/download',foreign)[0]==403;assert get(f'/reports/{report}/download')[0] in [401,403]
 reports.append({'reportId':report,'bytes':len(pdf),'sha256':hashlib.sha256(pdf).hexdigest()})
code,history=get('/logistics/orders/23956/history',doctor);assert code==200 and len(history)==3 and history[0]['id']==chain['shipmentId'] and history[0]['status']=='RECEIVED';assert all(r['status']=='CANCELLED' and not r['current'] for r in history[1:])
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0")=='false';assert staff.config.sql('SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId=23956')=='3'
assert staff.config.sql("SELECT COUNT(*) FROM lin_notification_event WHERE event_type='report' AND business_key='23956:published'")=='1'
result={**chain,'doctorUserId':f['userId'],'doctorReportDownloadValidated':True,'foreignHospital403AndAnonymousDenied':True,'reports':reports,'historyRows':3,'oldCancelledAttemptsRetained':2,'courierNodesSyntheticNotOfficialDelivery':True,'gateDisabled':True,'publishedReportNotificationUnique':True};staff.save_private(root/'qa/sf-third-doctor-report-validation.json',result);print(json.dumps(result))
