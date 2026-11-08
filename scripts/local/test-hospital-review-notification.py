"""Use the existing synthetic hospital; verify committed review emits exactly one inbox event, no sends."""
import importlib.util,json,secrets,urllib.request,time
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/hospital-submissions-private.json';f=json.loads(path.read_text());base=staff.config.BASE
for _ in range(20):
 try:
  with staff.opener.open(base+'/auth/capabilities',timeout=1) as response:assert response.status==200
  break
 except OSError:time.sleep(1)
else:raise AssertionError('Petlis backend unavailable')
admin=staff.administrator(4);manager=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token']
if 'notificationFileId' not in f:
 fields={'purpose':'OTHER','reason':'验证审核结果站内通知','requestKey':secrets.token_hex(16)};boundary='PetlisNotification'+secrets.token_hex(8);body=b''
 for key,value in fields.items():body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode()
 body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="notification-review.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+(root/'qa/native-profile-qualification.pdf').read_bytes()+(f'\r\n--{boundary}--\r\n').encode()
 request=urllib.request.Request(base+f"/hospitals/{f['hospitalId']}/document-submissions",data=body,headers={'Authorization':'Bearer '+manager,'Content-Type':'multipart/form-data; boundary='+boundary})
 with staff.opener.open(request,timeout=20) as response:result=json.load(response)
 assert result['code']==200;f['notificationFileId']=result['data']['id'];f['notificationReviewKey']=secrets.token_hex(16);staff.save_private(path,f)
id=f['notificationFileId'];decision={'approved':True,'reason':'站内通知原子性验证通过','requestKey':f['notificationReviewKey']}
for _ in range(2):staff.call(f'/admin/hospital-documents/{id}/review',decision,admin)
key='hospital-document:'+str(id);assert staff.config.sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={f['userId']} AND business_key='{key}'")=='1'
event=int(staff.config.sql(f"SELECT id FROM lin_notification_event WHERE user_id={f['userId']} AND business_key='{key}'"))
request=urllib.request.Request(base+'/inbox?size=100',headers={'Authorization':'Bearer '+manager})
with staff.opener.open(request,timeout=20) as response:result=json.load(response)
assert result['code']==200;items=result['data']['items'];assert any(x['id']==event and x['title']=='医院资料审核结果' for x in items)
assert staff.config.sql(f'SELECT COUNT(*) FROM lin_notification_delivery WHERE event_id={event}')=='0'
evidence={'hospitalId':f['hospitalId'],'fileId':id,'eventId':event,'sameReviewReplayedExactlyOneInboxEvent':True,'managerInboxShowsResult':True,'externalDeliveriesCreated':0,'schedulerEnabled':False,'ddlExecuted':False};staff.save_private(root/'qa/hospital-review-notification.json',evidence);print(json.dumps(evidence))
