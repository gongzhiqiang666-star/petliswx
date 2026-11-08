"""Create three synthetic coupon states for the existing native QA doctor, using only normal local APIs."""
import importlib.util,json,secrets,urllib.request
from pathlib import Path
from datetime import datetime,timedelta
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));s=importlib.util.module_from_spec(spec);spec.loader.exec_module(s)
p=s.config.PRIVATE/'qa/native-coupon-ui-fixture.json';assert not p.exists(),'Use or clean the previous native coupon fixture first'
f=s.config.read_private(s.config.PRIVATE/'simulator-fixture.json');assert f['account'].startswith('petlis_qa_');session=s.call('/auth/password/login',{'account':f['account'],'password':f['password']});a=s.administrator(4)
now=datetime.now();templates=[];coupons={};suffix=secrets.token_hex(4)
def update(cid,body):
 req=urllib.request.Request(s.config.BASE+f'/admin/coupons/{cid}?totalQuantity=1',data=json.dumps(body).encode(),headers={'Authorization':'Bearer '+a,'Content-Type':'application/json'},method='PUT')
 with s.opener.open(req,timeout=20) as r:assert json.load(r)['code']==200
for kind in ['available','upcoming','expired']:
 body={'name':'原生券验收_'+kind+'_'+suffix,'amount':20,'minimumAmount':400,'laboratoryId':1,'startsAt':(now-timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+timedelta(days=1)).isoformat(timespec='seconds')}
 cid=s.call('/admin/coupons',body,a);templates.append(cid);update(cid,body)
 uc=s.call(f'/admin/coupons/{cid}/users/{session["userId"]}',{'requestKey':'native_coupon_'+secrets.token_hex(16)},a);coupons[kind]=uc
 if kind=='upcoming':update(cid,{**body,'startsAt':(now+timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+timedelta(days=2)).isoformat(timespec='seconds')})
 if kind=='expired':update(cid,{**body,'startsAt':(now-timedelta(days=2)).isoformat(timespec='seconds'),'expiresAt':(now-timedelta(minutes=1)).isoformat(timespec='seconds')})
s.save_private(p,{'templates':templates,'coupons':coupons,'userId':session['userId'],'cleaned':False});print(json.dumps({'prepared':True,'userCouponIds':coupons}))
