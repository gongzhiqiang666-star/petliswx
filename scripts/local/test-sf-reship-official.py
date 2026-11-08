"""Official SF sandbox second-attempt create/label/cancel on the already-cancelled synthetic order.
No production credentials, courier pickup, payment or scheduler. Durable requests are never deleted/replayed blindly.
"""
import hashlib,importlib.util,json,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';target=root/'qa/sf-reship-official.json';base=staff.config.BASE;sql=staff.config.sql
config=json.loads((root/'sf-express/sandbox.json').read_text());assert config['enabled'] is True and config['endpoint']=='https://sfapi-sbox.sf-express.com/std/service'
for _ in range(20):
 try:
  with staff.opener.open(base+'/auth/capabilities',timeout=1) as response:assert response.status==200
  break
 except OSError:time.sleep(1)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);order=23956
assert sql(f"SELECT AnimalName LIKE 'QA_REAL_%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")=='1'
assert sql('SELECT @@event_scheduler')=='OFF'
def request(path,data=None,method=None):
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers={'Authorization':'Bearer '+admin,'Content-Type':'application/json'},method=method)
 try:
  with staff.opener.open(req,timeout=50) as response:result=json.load(response)
 except urllib.error.HTTPError as error:raise AssertionError('Local endpoint HTTP '+str(error.code)) from None
 assert result.get('code')==200,(path,result.get('code'),result.get('message'));return result.get('data')
def status():return request(f'/logistics/sf/orders/{order}')
previous=int(sql(f"SELECT id FROM lin_sf_request WHERE order_id={order} AND operation='CREATE' AND attempt_no=1 AND state='SUCCEEDED'"));assert sql(f"SELECT status FROM lin_shipment WHERE order_id={order} AND attempt_no=1")=='CANCELLED'
state=json.loads(target.read_text()) if target.exists() else {'orderId':order,'previousCreateRequestId':previous,'sandboxOnly':True,'completed':False};assert state['orderId']==order and state['previousCreateRequestId']==previous
if state.get('completed'):raise SystemExit('Official reship evidence already completed; inspect rather than dispatch again')
if sql("SELECT COUNT(*) FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0")=='0':request('/admin/operations/configs',{'key':'sf-reship','value':{'enabled':False,'maxAttempts':3},'description':'顺丰再次寄件开关，默认关闭；本机沙箱按受控测试临时开启'})
def gate(enabled,reason):
 version=int(sql("SELECT version FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0"));request('/admin/operations/configs/sf-reship',{'value':{'enabled':enabled,'maxAttempts':3},'version':version,'reason':reason},'PUT')
assert sql("SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value,'$.enabled')) FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0")=='false'
baseline=sql(f"SELECT CONCAT(UserNO,'|',TestFee,'|',SampleStatus,'|',IsPayMoney) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")
body={'previousCreateRequestId':previous,'reason':'本机顺丰官方沙箱验证取消后再次寄件','sender':{'contact':'沙箱寄件测试','mobile':'13800000000','province':'广东省','city':'深圳市','county':'南山区','address':'科技园测试地址1号'},'recipient':{'contact':'沙箱收件测试','mobile':'13900000000','province':'广东省','city':'深圳市','county':'福田区','address':'沙箱测试地址2号'}}
try:
 gate(True,'仅本机合成订单23956验证官方沙箱再次寄件，isDocall固定0');state['gateTemporarilyEnabled']=True;staff.save_private(target,state)
 existing=sql(f"SELECT id FROM lin_sf_request WHERE order_id={order} AND attempt_no=2 AND operation='CREATE'")
 if not existing:
  state['createDispatchStarted']=True;staff.save_private(target,state)
  created=request(f'/logistics/sf/orders/{order}/reship',body)
 else:
  created=status()
  if created['state'] in ['SENDING','UNKNOWN','SUCCEEDED'] and not created.get('shipmentId'):created=request(f'/logistics/sf/orders/{order}/reconcile',{})
  elif created['state']=='RESERVED':created=request(f'/logistics/sf/orders/{order}/reship',body)
 assert created['state']=='SUCCEEDED' and created.get('shipmentId') and int(created['attemptNo'])==2, 'Create is not confirmed; do not send again'
 state.update({'createRequestId':created['id'],'shipmentId':created['shipmentId'],'trackingNo':created['trackingNo'],'createConfirmed':True});staff.save_private(target,state)
 assert int(sql(f"SELECT COUNT(*) FROM lin_sf_request WHERE order_id={order} AND attempt_no=2 AND operation='CREATE'"))==1
 label=request(f'/logistics/sf/orders/{order}/label',{})
 with staff.opener.open(urllib.request.Request(base+label['url'],headers={'Authorization':'Bearer '+admin}),timeout=25) as response:pdf=response.read()
 assert pdf.startswith(b'%PDF-');path=root/'qa/sf-reship-attempt2-label.pdf';path.write_bytes(pdf);path.chmod(0o600)
 state.update({'labelFileId':label['id'],'labelBytes':len(pdf),'labelSha256':hashlib.sha256(pdf).hexdigest()});staff.save_private(target,state)
 current=status()
 if not current.get('cancellationState'):current=request(f'/logistics/sf/orders/{order}/cancel',{})
 elif current['cancellationState'] in ['SENDING','UNKNOWN','SUCCEEDED']:current=request(f'/logistics/sf/orders/{order}/reconcile',{})
 assert current.get('cancellationState')=='SUCCEEDED','Cancellation not confirmed; preserve durable request'
 assert sql(f"SELECT status FROM lin_shipment WHERE id={state['shipmentId']}")=='CANCELLED'
 history=request(f'/logistics/orders/{order}/history');assert len(history)==2 and history[0]['attemptNo']==2 and history[0]['current'] is True and history[1]['id']==70 and history[1]['current'] is False
 assert sql(f"SELECT CONCAT(UserNO,'|',TestFee,'|',SampleStatus,'|',IsPayMoney) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order}")==baseline
 state.update({'cancellationConfirmed':True,'historyRows':2,'oldShipmentId':70,'orderAndPriceUnchanged':True,'externalOrderNo':f'PETLIS_SANDBOX_{order}_A2','realCourierPickupRequested':False,'completed':True})
finally:
 gate(False,'本机官方沙箱验收阶段结束，恢复默认关闭；保留全部寄件历史')
 state['gateRestoredDisabled']=True;staff.save_private(target,state)
print(json.dumps({key:state[key] for key in ['orderId','shipmentId','trackingNo','createConfirmed','cancellationConfirmed','labelBytes','historyRows','orderAndPriceUnchanged','gateRestoredDisabled','completed']},ensure_ascii=False))
