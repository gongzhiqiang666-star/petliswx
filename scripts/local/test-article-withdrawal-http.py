"""Real administrative withdrawal and own QA inbox checks; no worker, scheduling or external sending."""
import importlib.util,json,re,secrets,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';editor=json.loads((root/'qa/pc-article-admin-ui.json').read_text());doctor=json.loads((root/'qa/native-invoice-fixture-private.json').read_text());old=json.loads((root/'simulator-fixture.json').read_text())
article=int(editor['articleId']);assert editor['completed'] and re.fullmatch('UI_EDITOR_[a-f0-9]{10}',editor['category']);assert doctor['userId']==583
assert staff.config.sql(f"SELECT category FROM lin_article WHERE id={article}")==editor['category'];assert staff.config.sql(f'SELECT status FROM lin_article WHERE id={article}')=='archived'
admin=staff.administrator(4);owner=staff.call('/auth/password/login',{'account':doctor['account'],'password':doctor['password']})['token']
def call(path,token,body=None,method='GET'):
 req=urllib.request.Request(staff.config.BASE+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method=method)
 try:
  with staff.opener.open(req,timeout=20) as response:return json.load(response)
 except urllib.error.HTTPError as error:
  value=json.load(error);value.setdefault('code',error.code);return value
baseline=staff.config.sql(f"SELECT COALESCE(DATE_FORMAT(published_at,'%Y-%m-%d %H:%i:%s'),'NULL') FROM lin_article WHERE id={article}");assert baseline=='NULL' or re.fullmatch(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}',baseline)
events=[];control=None;key='withdraw_control_'+secrets.token_hex(8)
try:
 staff.config.sql(f"UPDATE lin_article SET status='published',published_at=NOW() WHERE id={article} AND category='{editor['category']}'")
 for uid,state in [(doctor['userId'],'pending'),(int(old['session'].get('userId') or old['session']['id']),'failed')]:
  assert staff.config.sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={uid} AND event_type='article' AND business_key='article:{article}'")=='0','Inspect previous synthetic withdrawal evidence before rerun'
  staff.config.sql(f"INSERT INTO lin_notification_event(user_id,event_type,business_key,title,message,page_path,push_status) VALUES({uid},'article','article:{article}','本机资讯下架测试','仅合成验收，不调用外部推送','pages/article-detail/article-detail?id={article}','{state}')")
  events.append(int(staff.config.sql(f"SELECT id FROM lin_notification_event WHERE user_id={uid} AND event_type='article' AND business_key='article:{article}'")))
 staff.config.sql(f"INSERT INTO lin_notification_event(user_id,event_type,business_key,title,message,page_path) VALUES({doctor['userId']},'order','{key}','本机对照通知','仅合成验收','pages/orders/orders')")
 control=int(staff.config.sql(f"SELECT id FROM lin_notification_event WHERE user_id={doctor['userId']} AND business_key='{key}'"))
 current=call('/admin/articles/'+str(article),admin)['data'];command={'status':'archived','version':current['version'],'reason':'本机验收，下架后取消待发送通知','requestKey':'withdraw_'+secrets.token_hex(12)}
 assert call('/admin/articles/'+str(article)+'/status',admin,command,'POST')['code']==200
 assert call('/admin/articles/'+str(article)+'/status',admin,command,'POST')['code']==200
 for event in events:
  assert staff.config.sql(f'SELECT push_status FROM lin_notification_event WHERE id={event}')=='skipped'
  assert staff.config.sql(f"SELECT COUNT(*) FROM lin_notification_delivery WHERE event_id={event} AND error_code='CONTENT_UNAVAILABLE'")=='1'
 assert staff.config.sql(f'SELECT push_status FROM lin_notification_event WHERE id={control}')=='pending'
 assert call('/inbox/'+str(events[0])+'/retry',owner,{},'POST')['code']==409
 logs=call('/inbox/'+str(events[0])+'/deliveries',owner)['data'];assert len(logs)==1 and logs[0]['errorCode']=='CONTENT_UNAVAILABLE'
 assert call('/articles/'+str(article),owner)['code']==404
 result={'articleId':article,'eventIds':events,'pendingAndFailedSkipped':True,'oneCancellationLogEach':True,'repeatedWithdrawalIdempotent':True,'otherEventUnaffected':True,'ownerRetryRejected':True,'ownerSeesCancellationReason':True,'publicArticleUnavailable':True,'workerOrExternalPushCalled':False}
 (root/'qa/article-withdrawal-http.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
finally:
 timestamp='NULL' if baseline=='NULL' else "'"+baseline+"'"
 staff.config.sql(f"UPDATE lin_article SET status='archived',published_at={timestamp} WHERE id={article} AND category='{editor['category']}'")
 for event in events+([control] if control else []):staff.config.sql(f"UPDATE lin_notification_event SET push_status='skipped',last_error='本机合成验收完成，禁止外部发送' WHERE id={event} AND push_status IN ('pending','failed')")
