"""Grant idempotency on fresh local synthetic coupon templates and existing synthetic doctors only."""
import importlib.util,json,secrets,urllib.request,urllib.error
from datetime import datetime,timedelta
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
a=s.administrator(4)
ids=s.config.sql("SELECT DISTINCT u.id FROM lin_user u JOIN lin_user_role r ON r.user_id=u.id AND r.role_code='DOCTOR' AND r.is_active=1 WHERE u.status='normal' AND (u.username LIKE 'QA_REAL_%' OR u.username REGEXP '^petlis_qa_[a-f0-9]+$') ORDER BY u.id DESC LIMIT 2").split();assert len(ids)==2;users=list(map(int,ids))
def raw(path,data,method='POST'):
 req=urllib.request.Request(s.config.BASE+path,data=json.dumps(data).encode(),headers={'Authorization':'Bearer '+a,'Content-Type':'application/json'},method=method)
 try:
  with s.opener.open(req,timeout=20) as r:return json.load(r)
 except urllib.error.HTTPError as e:return json.load(e)
now=datetime.now();body={'name':'本机并发发券_'+secrets.token_hex(5),'amount':10,'minimumAmount':0,'laboratoryId':None,'startsAt':(now-timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+timedelta(days=1)).isoformat(timespec='seconds')}
coupon=s.call('/admin/coupons',body,a);assert raw(f'/admin/coupons/{coupon}?totalQuantity=10',body,'PUT')['code']==200
key='coupon_race_'+secrets.token_hex(12)
other=None;deactivated=False
try:
 with ThreadPoolExecutor(max_workers=4) as pool:replies=list(pool.map(lambda _:raw(f'/admin/coupons/{coupon}/users/{users[0]}',{'requestKey':key}),range(4)))
 grants=[r.get('data') for r in replies if r['code']==200]
 changed=raw(f'/admin/coupons/{coupon}/users/{users[1]}',{'requestKey':key})
 other=s.call('/admin/coupons',{**body,'name':body['name']+'_另一模板'},a)
 changed_coupon=raw(f'/admin/coupons/{other}/users/{users[0]}',{'requestKey':key})
 racing_key='coupon_payload_race_'+secrets.token_hex(12)
 with ThreadPoolExecutor(max_workers=2) as pool:racing=list(pool.map(lambda uid:raw(f'/admin/coupons/{coupon}/users/{uid}',{'requestKey':racing_key}),users))
 race_codes=sorted(r['code'] for r in racing)
 count=int(s.config.sql(f'SELECT COUNT(*) FROM lin_user_coupon WHERE coupon_id={coupon}'))
 audit=int(s.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='COUPON_GRANTED' AND request_id='{key}'"))
 race_audit=int(s.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='COUPON_GRANTED' AND request_id='{racing_key}'"))
 stock=json.loads(s.config.sql(f"SELECT config_value FROM lin_operational_config WHERE config_key='coupon-stock:{coupon}' AND scope_type='GLOBAL' AND scope_id=0"))
 assert raw(f'/admin/coupons/{coupon}',{},'DELETE')['code']==200;deactivated=True
 replay=raw(f'/admin/coupons/{coupon}/users/{users[0]}',{'requestKey':key})
 result={'couponId':coupon,'concurrentCodes':[r['code'] for r in replies],'distinctReturnedIds':len(set(grants)),'grantedRows':count,'firstRequestGrantAudits':audit,'changedRecipientCode':changed['code'],'changedTemplateCode':changed_coupon['code'],'differentRecipientsSameKeyRaceCodes':race_codes,'racingKeyAuditCount':race_audit,'stockIssued':stock['issued'],'deactivatedTemplateReplayReturnsPriorGrant':replay['code']==200 and replay.get('data')==grants[0],'allSameGrant':len(grants)==4 and len(set(grants))==1}
 print(json.dumps(result,indent=2));assert result['allSameGrant'] and count==2 and audit==1 and changed['code']==409 and changed_coupon['code']==409 and race_codes==[200,409] and race_audit==1 and stock['issued']==2 and result['deactivatedTemplateReplayReturnsPriorGrant']

finally:
 if not deactivated:assert raw(f'/admin/coupons/{coupon}',{},'DELETE')['code']==200
 if other:assert raw(f'/admin/coupons/{other}',{},'DELETE')['code']==200
