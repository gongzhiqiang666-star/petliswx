"""Complete the simulator's latest synthetic submission via normal APIs and the PHP bridge, no real payments/shipping."""
import importlib.util,json,secrets,subprocess,urllib.request,re
from pathlib import Path
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
private=s.config.PRIVATE;f=s.config.read_private(private/'simulator-fixture.json');case=json.loads((private/'qa/ux-v1-order.json').read_text());oid=int(case['orderId']);assert case['account']==f['account'] and re.fullmatch(r'petlis_qa_[a-f0-9]+',f['account'])
admin=s.administrator(4);session=s.call('/auth/password/login',{'account':f['account'],'password':f['password']});doctor=session['token'];uid=session['userId']
assert s.config.sql(f"SELECT CreateSampleId={uid} AND SampleStatus=0 AND AnimalName LIKE '{f['account']}%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
def get(path,token):
 req=urllib.request.Request(s.config.BASE+path,headers={'Authorization':'Bearer '+token})
 with s.opener.open(req,timeout=20) as r:data=json.load(r)
 assert data['code']==200;return data['data']
# Clinical changes use the governed versioned correction endpoint, never direct SQL.
clinical=get(f'/legacy/orders/{oid}/clinical',admin)
fields={**clinical['fields'],'medicalHistory':'本机原生联调：无既往异常','clinicalSymptoms':'本机原生联调：采样后状态稳定','diagnosis':'本机合成病例，仅用于界面验证','medication':'本机原生联调：未用药'}
s.call(f'/legacy/orders/{oid}/clinical',{'version':clinical['version'],'fields':fields,'reason':'补充本机合成临床资料用于原生显示验收','resubmit':False},admin)
s.call(f'/review/orders/{oid}',{'approved':True,'reason':'原生提交后本机合成审核'},admin)
order=get(f'/orders/{oid}',doctor);assert order['sampleStatus']==2
courier_id=None
try:
 name='native_courier_'+secrets.token_hex(4);password=secrets.token_urlsafe(24)+'A7'
 courier_id=s.call('/admin/staff',{'username':name,'displayName':'原生联调合成配送员','password':password,'roleCode':'COURIER','laboratoryId':None,'reason':'仅本机原生同单全链路验收'},admin)
 courier=s.call('/auth/password/login',{'account':name,'password':password})['token']
 sid=s.call('/logistics',{'orderId':oid,'trackingNo':'NATIVE_QA_'+str(oid),'laboratoryId':1,'pickupAddress':'本机合成测试地址','courierUserId':courier_id},admin)
 for action in ['pickup','transit']:s.call(f'/logistics/{sid}/{action}',{},courier)
 incident=s.call(f'/logistics/{sid}/exception',{'exceptionType':'DELAY','note':'本机合成延误，已核实可继续送检','requestKey':'native_exception_'+secrets.token_hex(16)},courier)
 event=incident['exceptions'][0]['id'];s.call(f'/logistics/{sid}/exceptions/{event}/resolve',{'reason':'本机已核实合成标本安全，恢复正常运输'},admin)
 s.call(f'/logistics/{sid}/deliver',{},courier)
 account=s.config.read_private(private/'lab-bridge-1-credentials.json');s.verify(4,1,account)
 probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-native-order-chain.php'
 result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(oid),str(account['userId'])],input=probe.read_bytes(),capture_output=True,check=True)
 evidence=json.loads(result.stdout);final=get(f'/orders/{oid}',doctor);shipment=get(f'/logistics/{sid}',doctor)
 assert final['sampleStatus']==3 and final['clinicalSymptoms']==fields['clinicalSymptoms']
 assert shipment['status']=='RECEIVED' and len(shipment['exceptions'])==1 and shipment['exceptions'][0].get('resolvedAt')
 for did in evidence['reportIds']:
  req=urllib.request.Request(s.config.BASE+f'/reports/{did}/download',headers={'Authorization':'Bearer '+doctor})
  with s.opener.open(req,timeout=20) as r:assert r.read().startswith(b'%PDF-')
 evidence.update(shipmentId=sid,doctorClinicalMatches=True,doctorOrderCompleted=True,doctorExceptionHistoryResolved=True,doctorPdfVerified=True,realPayment=False,realCarrier=False)
finally:
 if courier_id:
  req=urllib.request.Request(s.config.BASE+f'/admin/users/{courier_id}/status',data=json.dumps({'active':False,'reason':'本机原生全链验收后停用合成配送员'}).encode(),headers={'Authorization':'Bearer '+admin,'Content-Type':'application/json'},method='PATCH')
  with s.opener.open(req,timeout=20) as r:assert json.load(r)['code']==200
if courier_id:evidence['syntheticCourierDisabledAtEnd']=True
s.save_private(private/'qa/native-full-chain.json',evidence);print(json.dumps(evidence,ensure_ascii=False,indent=2))
