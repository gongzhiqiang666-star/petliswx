"""Real PHP bridge receipt and Java read consistency on a new synthetic manual shipment; no SF calls."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/receipt-date-php-private.json'
a=staff.administrator(4);account=json.loads((root/'qa/native-sf-history-private.json').read_text());doctor=staff.call('/auth/password/login',{'account':account['account'],'password':account['password']})['token']
def get(url,token):
 with staff.opener.open(urllib.request.Request(staff.config.BASE+url,headers={'Authorization':'Bearer '+token}),timeout=15) as r:result=json.load(r)
 assert result['code']==200,(url,result['code']);return result['data']
if path.exists():state=json.loads(path.read_text())
else:
 source=get('/orders/23956',doctor);prefix='QA_RECEIPT_'+secrets.token_hex(6)
 keys=['infoAnimalId','infoSampleTypeId','animalTypeId','animalType','animalTypeEn','age','dictSexId','sampleArea']
 body={k:source[k] for k in keys};body.update(laboratoryId=source['laboratoryId'],animalName=prefix,animalNameEn=prefix,masterName=prefix+' owner',doctorName='Local synthetic doctor',diagnosisDetails=[{'infoTestTypeId':item['infoTestTypeId']} for item in source['items'] if item['isActive']==1],idempotencyKey=secrets.token_hex(16))
 assert body['diagnosisDetails'];state={'prefix':prefix,'body':body};staff.save_private(path,state)
if state.get('completed'):raise SystemExit('Completed evidence exists; do not repeat business writes')
if not state.get('orderId'):
 state['orderId']=staff.call('/orders',state['body'],doctor)['orderId'];staff.save_private(path,state)
oid=state['orderId'];assert staff.config.sql(f"SELECT AnimalName LIKE 'QA_RECEIPT_%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
order=get(f'/orders/{oid}',doctor)
if order['sampleStatus']==0:staff.call(f'/review/orders/{oid}',{'approved':True,'reason':'Local PHP receipt-date sync validation'},a)
assert get(f'/orders/{oid}',doctor)['sampleStatus']==2
courier=json.loads((root/'qa/sf-third-courier-private.json').read_text());session=staff.call('/auth/password/login',{'account':courier['account'],'password':courier['password']});assert session['userId']==courier['userId'];ct=session['token']
if not state.get('shipmentId'):
 # If a prior response was lost, query this exact order before dispatching another creation.
 existing=staff.config.sql(f'SELECT id FROM lin_shipment WHERE order_id={oid} AND is_current=1')
 state['shipmentId']=int(existing) if existing else staff.call('/logistics',{'orderId':oid,'trackingNo':'IT_RECEIPT_LIVE_'+str(oid),'laboratoryId':state['body']['laboratoryId'],'pickupAddress':'Local synthetic pickup','courierUserId':courier['userId']},a);staff.save_private(path,state)
sid=state['shipmentId']
for before,action in [('PENDING_PICKUP','pickup'),('PICKED_UP','transit'),('IN_TRANSIT','deliver')]:
 if get(f'/logistics/{sid}',a)['status']==before:staff.call(f'/logistics/{sid}/{action}',{},ct)
if get(f'/logistics/{sid}',a)['status']=='DELIVERED':assert staff.config.sql(f"SELECT YEAR(ReceiveSampleDt)<1901 FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-receipt-date-sync.php'
r=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(oid),str(sid)],input=probe.read_bytes(),capture_output=True,check=True)
proof=json.loads(r.stdout);current=get(f'/orders/{oid}',doctor);shipment=get(f'/logistics/{sid}',doctor)
assert current['receiveSampleDt'][:10]==proof['phpReceiveSampleDt'][:10]
assert shipment['status']=='RECEIVED';assert staff.config.sql(f'SELECT COUNT(*) FROM lin_sf_request WHERE order_id={oid}')=='0'
proof.update(doctorReadsSameDate=True,realCourier=False,realSfCalls=False,realPayment=False,completed=True)
staff.save_private(root/'qa/receipt-date-php.json',proof);state['completed']=True;staff.save_private(path,state);print(json.dumps(proof))
