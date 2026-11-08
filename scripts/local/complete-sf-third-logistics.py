"""Synthetic internal courier progression for official sandbox attempt 3; no carrier pickup/delivery claim."""
import importlib.util,json,secrets,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';proof=json.loads((root/'qa/sf-reship-third-ui.json').read_text());assert proof['completed'] and proof['gateRestoredDisabled'];order=proof['orderId'];shipment=proof['shipmentId'];assert order==23956
admin=staff.administrator(4)
def get(path,token):
 with staff.opener.open(urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+token}),timeout=20) as response:r=json.load(response)
 assert r['code']==200,(path,r['code']);return r['data']
row=get(f'/logistics/{shipment}',admin);assert row['orderId']==order and row['attemptNo']==3 and row['current'] is True
assert staff.config.sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0")=='false'
# Independent official label, generated before any synthetic transport steps.
label=staff.call(f'/logistics/sf/orders/{order}/label',{},admin)
with staff.opener.open(urllib.request.Request(staff.config.BASE+label['url'],headers={'Authorization':'Bearer '+admin}),timeout=20) as response:pdf=response.read()
assert pdf.startswith(b'%PDF-');label_path=root/'qa/sf-reship-attempt3-label.pdf';label_path.write_bytes(pdf);label_path.chmod(0o600)
path=root/'qa/sf-third-courier-private.json'
if path.exists():courier=json.loads(path.read_text())
else:
 courier={'account':'petlis_sf_relay_23956','password':secrets.token_hex(16)+'A7','userId':None};staff.save_private(path,courier)
username=courier['account'];assert username=='petlis_sf_relay_23956'
existing=staff.config.sql(f"SELECT id FROM lin_user WHERE username='{username}'")
if not existing:
 uid=staff.call('/admin/staff',{'username':username,'displayName':'顺丰联调合成配送员','password':courier['password'],'roleCode':'COURIER','laboratoryId':None,'reason':'仅本机合成订单23956第三次寄件内部配送联调'},admin);courier['userId']=uid;staff.save_private(path,courier)
else:courier['userId']=int(existing);staff.save_private(path,courier)
session=staff.call('/auth/password/login',{'account':username,'password':courier['password']});assert session['userId']==courier['userId'] and set(session['roles'])=={'COURIER'};token=session['token']
row=get(f'/logistics/{shipment}',admin)
if row['status']=='PENDING_PICKUP':staff.call(f'/logistics/{shipment}/assign',{'courierUserId':courier['userId']},admin)
steps=[]
for expected,action in [('PENDING_PICKUP','pickup'),('PICKED_UP','transit'),('IN_TRANSIT','deliver')]:
 row=get(f'/logistics/{shipment}',token)
 if row['status']==expected:staff.call(f'/logistics/{shipment}/{action}',{},token);steps.append(action)
row=get(f'/logistics/{shipment}',admin);assert row['status'] in ['DELIVERED','RECEIVED']
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_shipment WHERE order_id={order} AND attempt_no<3 AND status='CANCELLED' AND is_current IS NULL")=='2'
result={'orderId':order,'shipmentId':shipment,'courierUserId':courier['userId'],'status':row['status'],'internalSyntheticTransitions':steps,'carrierPhysicalDeliveryClaimed':False,'officialLabelFileId':label['id'],'officialLabelBytes':len(pdf),'oldCancelledAttemptsRetained':2,'gateDisabled':True};staff.save_private(root/'qa/sf-third-logistics.json',result);print(json.dumps(result))
