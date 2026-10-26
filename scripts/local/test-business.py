# Local Petlis development integration tests only. Creates synthetic fixtures and cleans them up.
# SMS verification is a seeded fixture; no real SMS is sent. Business test payments are development fixtures.
from pathlib import Path
import urllib.request,urllib.error,json,secrets,subprocess,base64,os,datetime,time
from decimal import Decimal
b=Path.home()/'.local/share/petlis';base='http://127.0.0.1:8082/api'
mysql=b/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
def sql(s):return subprocess.run([str(mysql),f'--defaults-extra-file={b}/mysql/root.cnf','--batch','--skip-column-names','laboklin'],input=s.encode(),capture_output=True,check=True).stdout.decode().strip()
def call(path,data=None,token=None,method=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
 try:
  with urllib.request.urlopen(req) as r:return r.status,json.load(r)
 except urllib.error.HTTPError as e:return e.code,None
pw=next(l.split('：',1)[1] for l in (b/'reviewer-credentials.txt').read_text().splitlines() if l.startswith('密码：'))
_,r=call('/auth/password/login',{'account':'petlis_reviewer','password':pw});assert r['code']==200;reviewer=r['data']['token']
name='petlis_it_'+secrets.token_hex(5);phone='199'+str(secrets.randbelow(10**8)).zfill(8);password=secrets.token_hex(15)+'A7';code=str(secrets.randbelow(900000)+100000)
settlement=1 if os.environ.get('PETLIS_TEST_SETTLEMENT')=='monthly' else 0
hospital=int(sql(f'SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 AND PayType={settlement} ORDER BY InfoHospitalId LIMIT 1;'))
file_id=secrets.token_hex(16)+'.png';folder=Path(__file__).resolve().parents[2]/'petlis-backend/uploads/qualifications';folder.mkdir(parents=True,exist_ok=True);fixture=folder/file_id;fixture.write_bytes(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII='))
user_id=None;gift=None;order_id=None;invoice_id=None;profile_id=None;fixture_lab=None;fixture_coupon=None
try:
 # Test fixture only: exercise verification consumption without sending real SMS.
 assert sql("SELECT COUNT(*) FROM lin_user WHERE mobile='"+phone+"';")=='0'
 sql("INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','"+phone+"','"+code+"',0,'127.0.0.1',UNIX_TIMESTAMP());")
 _,r=call('/auth/register',{'username':name,'password':password,'mobile':phone,'code':code,'hospitalId':hospital,'nickname':'自动化测试医生','title':'测试资质','qualificationFiles':['/files/qualifications/'+file_id]});assert r['code']==200,r;user_id=r['data']
 _,r=call('/auth/password/login',{'account':name,'password':password});assert r['code']==403,r
 _,r=call('/review/registrations/'+str(user_id),{'approved':True,'reason':'自动化测试'},reviewer);assert r['code']==200,r
 _,r=call('/auth/password/login',{'account':name,'password':password});assert r['code']==200,r;doctor=r['data']['token'];assert not r['data']['reviewer']
 status,_=call('/review/orders',token=doctor);assert status==403,status
 _,r=call('/account',token=doctor);assert r['code']==200,r
 _,r=call('/laboratories',token=doctor);assert r['code']==200 and len(r['data'])>=1,r;lab=r['data'][0]['id']
 _,r=call('/laboratories/'+str(lab)+'/tests',token=doctor);assert r['code']==200 and r['data']['total']>0,r;test=next(x['infoTestTypeId'] for x in r['data']['items'] if x.get('price') is not None and float(x['price'])>=0)
 _,r=call('/coupons/quote',{'laboratoryId':lab,'testTypeIds':[test]},doctor);assert r['code']==200 and float(r['data']['payableAmount'])>=0,r
 _,r=call('/inbox',token=doctor);assert r['code']==200 and len(r['data']['items'])>=1,r
 notification=r['data']['items'][0]['id']
 _,r=call('/inbox/'+str(notification)+'/read',{},doctor,method='PUT');assert r['code']==200,r
 _,r=call('/inbox/unread-count',token=doctor);assert r['code']==200 and r['data']==0,r
 sql(f"UPDATE lin_user SET openid='petlis_test_{secrets.token_hex(12)}' WHERE id={int(user_id)};")
 # Gift redemption: server price, stock/points rollback and replay protection.
 _,r=call('/mall/gifts',{'name':name+' gift','description':'isolated test fixture','pointsCost':100,'stock':2},reviewer);assert r['code']==200,r;gift=r['data']
 sql(f'UPDATE lin_user SET score=1000 WHERE id={int(user_id)};')
 redemption={'giftId':gift,'quantity':1,'deliveryAddress':{'recipient':'Test','phone':phone,'address':'isolated test address'},'requestKey':'redeem_'+secrets.token_hex(12)}
 _,r=call('/mall/redemptions',redemption,doctor);assert r['code']==200,r;redemption_id=r['data']
 _,r=call('/mall/redemptions',redemption,doctor);assert r['code']==200 and r['data']==redemption_id,r
 _,r=call('/mall/redemptions',{**redemption,'deliveryAddress':{**redemption['deliveryAddress'],'address':'different address'}},doctor);assert r['code']==409,r
 _,r=call('/mall/redemptions',{**redemption,'quantity':2,'requestKey':'redeem_'+secrets.token_hex(12)},doctor);assert r['code']!=200,r
 assert sql(f'SELECT score FROM lin_user WHERE id={int(user_id)};')=='900'
 assert sql(f'SELECT stock FROM lin_gift WHERE id={int(gift)};')=='1'
 # Create -> review -> development-only payment callback -> invoice. No real WeChat funds.
 sample=int(sql('SELECT InfoSampleTypeId FROM lin_lb_infosampletype WHERE IsActive=1 LIMIT 1;'))
 animal=int(sql('SELECT InfoAnimalId FROM lin_lb_infoanimal WHERE IsActive=1 LIMIT 1;'))
 variety=int(sql('SELECT IID FROM lin_lb_infovariety LIMIT 1;'))
 test=int(sql(f'SELECT lt.test_type_id FROM lin_lab_test lt JOIN lin_lb_infotesttype t ON t.InfoTestTypeId=lt.test_type_id WHERE lt.laboratory_id={int(lab)} AND t.IsActive=1 AND COALESCE(lt.price,(SELECT p.price FROM lin_lb_infotesttype_price p WHERE p.InfoTestTypeid=t.InfoTestTypeId AND p.IsActive=1 ORDER BY p.IID DESC LIMIT 1),t.Price)>0 AND lt.test_type_id<>1225 LIMIT 1;'))
 _,r=call('/laboratories',{'code':'IT'+secrets.token_hex(6).upper(),'name':name+' lab','description':'isolated test fixture'},reviewer);assert r['code']==200,r;fixture_lab=r['data'];lab=fixture_lab
 _,r=call('/laboratories/'+str(lab)+'/tests/'+str(test),{'price':50},reviewer,method='PUT');assert r['code']==200,r
 now=datetime.datetime.now()
 _,r=call('/coupons',{'name':name+' coupon','amount':1,'minimumAmount':10,'laboratoryId':lab,'startsAt':(now-datetime.timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+datetime.timedelta(days=1)).isoformat(timespec='seconds')},reviewer);assert r['code']==200,r;fixture_coupon=r['data']
 _,r=call('/coupons/'+str(fixture_coupon)+'/users/'+str(user_id),{},reviewer);assert r['code']==200,r;user_coupon=r['data']
 payload={'userCouponId':user_coupon,'laboratoryId':lab,'infoAnimalId':animal,'infoSampleTypeId':sample,'animalName':name,'animalNameEn':'Test Pet','animalTypeId':variety,'animalType':'测试品种','animalTypeEn':'Test Breed','age':'3Y','dictSexId':1,'sampleArea':'test sample','masterName':'测试畜主','masterNameEn':'Test Owner','diagnosisDetails':[{'infoTestTypeId':test}],'collectDt':'2026-10-24'}
 _,r=call('/drafts',{'laboratoryId':lab,'payload':{'petId':1,'petInfo':{'animalName':name}}},doctor);assert r['code']==200,r;draft_id=r['data']
 _,r=call('/drafts/'+str(draft_id),{'laboratoryId':lab,'payload':{'petId':1,'petInfo':{'animalName':name}},'version':0},doctor,method='PUT');assert r['code']==200,r
 _,r=call('/drafts/'+str(draft_id),{'laboratoryId':lab,'payload':{},'version':0},doctor,method='PUT');assert r['code']==409,r
 payload.update({'draftId':draft_id,'draftVersion':1})
 _,r=call('/orders',payload,doctor);assert r['code']==200,r;order_id=r['data']['orderId'];assert r['data']['needPayment']==False,r
 _,r=call('/drafts/'+str(draft_id),token=doctor);assert r['code']==404,r
 _,r=call('/orders',payload,doctor);assert r['code']==409,r
 assert sql(f'SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(user_id)};')=='1'

 _,r=call('/payment/create',{'orderId':order_id,'amount':0.01,'openid':'forged'},doctor);assert r['code']!=200,r
 original_number=sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={int(order_id)};')
 _,r=call('/review/orders/'+str(order_id),{'approved':False,'reason':'请补充采样部位'},reviewer);assert r['code']==200,r
 assert sql(f'SELECT status FROM lin_user_coupon WHERE id={int(user_coupon)};')=='available'
 _,r=call('/orders/'+str(order_id),token=doctor);assert r['code']==200 and r['data']['canResubmit'] and r['data']['reviewReason']=='请补充采样部位' and len(r['data']['reviewHistory'])>=1,r
 _,r=call('/laboratories/'+str(lab)+'/tests/'+str(test),{'price':65},reviewer,method='PUT');assert r['code']==200,r
 payload.pop('draftId',None);payload.pop('draftVersion',None);payload['sampleArea']='corrected sample area'
 _,r=call('/orders/'+str(order_id)+'/resubmit',{**payload,'diagnosisDetails':[{'infoTestTypeId':999999999}]},doctor);assert r['code']!=200,r
 assert sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={int(order_id)};')=='1'
 _,r=call('/orders/'+str(order_id)+'/resubmit',payload,doctor);assert r['code']==200,r
 assert sql(f'SELECT UserNO FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={int(order_id)};')==original_number
 actual_amount=sql(f'SELECT ROUND(TestFee,2) FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={int(order_id)};')
 assert Decimal(actual_amount)==Decimal('64.00'),actual_amount
 assert sql(f'SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={int(order_id)} AND IsCheckPass=0;')=='1'
 assert sql(f'SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={int(order_id)} AND IsCheckPass=1;')=='1'
 _,r=call('/orders/'+str(order_id)+'/resubmit',payload,doctor);assert r['code']==409,r
 _,r=call('/orders?status=0&keyword='+name,token=doctor);assert r['code']==200 and r['data']['totalElements']==1,r
 _,r=call('/orders?status=3&keyword='+name,token=doctor);assert r['code']==200 and r['data']['totalElements']==0,r
 _,r=call('/orders?startDate=2999-01-01&keyword='+name,token=doctor);assert r['code']==200 and r['data']['totalElements']==0,r
 print('Verified scoped lab pricing, coupon release/reservation, rejected-order editing/repricing/history, preserved order number, retry rollback, server-side status/search/date filters')
 _,r=call('/review/orders/'+str(order_id),{'approved':True,'reason':'isolated integration'},reviewer);assert r['code']==200,r
 # Use isolated fixture hospital payment mode; if monthly, mark paid fixture for invoice without touching hospital settings.
 status=int(sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={int(order_id)};'))
 if status==-3:
  _,r=call('/payment/create',{'orderId':order_id,'amount':0.01,'openid':'forged'},doctor);assert r['code']==200,r;payment=r['data'];assert float(payment['amount'])!=0.01 and payment['mock']==True,r
  _,r=call('/payment/mock-callback/'+payment['paymentNo'],{},doctor);assert r['code']==200,r
 else:
  assert status==2,status
  sql(f'UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=1 WHERE ListDiagnosisProjectId={int(order_id)} AND CreateSampleId={int(user_id)};')
 _,r=call('/invoices/profiles',{'title':'Test invoice','email':'fixture@example.test','taxNumber':''},doctor);assert r['code']==200,r;profile_id=r['data']
 profile_update={'title':'Updated invoice','email':'fixture@example.test','taxNumber':'','address':'Test address','phone':phone,'bankName':'Test bank','bankAccount':'0012345'}
 _,r=call('/invoices/profiles/'+str(profile_id),profile_update,doctor,method='PUT');assert r['code']==200,r
 _,r=call('/invoices/profiles',token=doctor);assert r['code']==200,r
 saved=next(x for x in r['data'] if x['id']==profile_id);assert saved['title']=='Updated invoice' and saved['bankAccount']=='0012345',saved
 application={'profileId':profile_id,'orderIds':[order_id],'requestKey':'invoice_'+secrets.token_hex(12)}
 _,r=call('/invoices',application,doctor);assert r['code']==200,r;invoice_id=r['data']
 _,r=call('/invoices',application,doctor);assert r['code']==200 and r['data']==invoice_id,r
 _,r=call('/invoices',{**application,'profileId':profile_id+1},doctor);assert r['code']==409,r
 # Editing a reusable profile must never rewrite the submitted invoice snapshot.
 _,r=call('/invoices/profiles/'+str(profile_id),{**profile_update,'title':'Later profile edit'},doctor,method='PUT');assert r['code']==200,r
 _,r=call('/invoices/'+str(invoice_id),token=doctor);assert r['code']==200,r
 snapshot=r['data']['profileSnapshot'];snapshot=json.loads(snapshot) if isinstance(snapshot,str) else snapshot
 assert snapshot['title']=='Updated invoice' and snapshot['bank_account']=='0012345',snapshot
 _,r=call('/invoices/'+str(invoice_id)+'/issue',{'invoiceUrl':'https://example.test/fixture.pdf'},reviewer);assert r['code']==200,r
 _,r=call('/invoices/'+str(invoice_id),token=doctor);assert r['code']==200 and r['data']['status']=='issued',r
 # Simulate an external PHP status write. Do not call Java notification methods.
 sql(f'UPDATE lin_lb_listdiagnosisproject SET SampleStatus=3 WHERE ListDiagnosisProjectId={int(order_id)} AND CreateSampleId={int(user_id)};')
 legacy=int(sql(f'SELECT MAX(id) FROM lin_lb_notification WHERE order_id={int(order_id)} AND new_status=3;'))
 for attempt in range(16):
  _,r=call('/inbox',token=doctor);assert r['code']==200,r
  reports=[x for x in r['data']['items'] if x['eventType']=='report' and x['pagePath']=='pages/order-detail/order-detail?id='+str(order_id)]
  if reports:break
  time.sleep(1)
 assert len(reports)==1 and '已完成' in reports[0]['message'],r
 assert sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={int(user_id)} AND business_key='legacy:{legacy}';")=='1'
 time.sleep(6) # Observe another scheduled scan; a single transition must not create duplicates.
 assert sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={int(user_id)} AND business_key='legacy:{legacy}';")=='1'
 print('Verified external legacy report transition reaches own inbox with corrected wording and deterministic event key')
 print('Verified gift redemption/replay/address mismatch/stock rollback; order review-before-payment; development payment fixture; invoice eligibility/replay/issue; no real external payment')

 _,r=call('/auth/logout',{},doctor);assert r['code']==200,r
 status,_=call('/account',token=doctor);assert status in [401,403],status
 print('Verified registration -> pending login denied -> reviewer approval -> doctor login -> reviewer denial -> account/catalog/server quote -> logout token revoked; SMS was seeded test fixture only')
finally:
 if order_id:
  oid=int(order_id)
  sql(f'DELETE d FROM lin_lb_notification_log d JOIN lin_lb_notification n ON n.id=d.notification_id WHERE n.order_id={oid}; DELETE FROM lin_lb_notification WHERE order_id={oid}; DELETE FROM lin_invoice_order WHERE order_id={oid}; DELETE FROM lin_wechat_payment WHERE order_id={oid}; DELETE FROM lin_order_extension WHERE order_id={oid}; DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={oid}; DELETE FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid};')
 if user_id:
  uid=int(user_id)
  sql(f'DELETE FROM lin_user_coupon WHERE user_id={uid}; DELETE FROM lin_order_draft WHERE user_id={uid}; DELETE FROM lin_invoice_application WHERE user_id={uid}; DELETE FROM lin_invoice_profile WHERE user_id={uid}; DELETE FROM lin_redemption WHERE user_id={uid}; DELETE FROM lin_user_score_log WHERE user_id={uid};')
 if gift:sql(f'DELETE FROM lin_gift WHERE id={int(gift)};')
 if fixture_coupon:sql(f'DELETE FROM lin_coupon WHERE id={int(fixture_coupon)};')
 if fixture_lab:sql(f'DELETE FROM lin_lab_test WHERE laboratory_id={int(fixture_lab)}; DELETE FROM lin_laboratory WHERE id={int(fixture_lab)};')
 if user_id:sql(f'DELETE d FROM lin_notification_delivery d JOIN lin_notification_event e ON e.id=d.event_id WHERE e.user_id={int(user_id)}; DELETE FROM lin_notification_event WHERE user_id={int(user_id)}; DELETE FROM lin_doctor_profile WHERE user_id={int(user_id)}; DELETE FROM lin_user WHERE id={int(user_id)};')
 sql("DELETE FROM lin_sms WHERE mobile='"+phone+"';")
 fixture.unlink(missing_ok=True)
