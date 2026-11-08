"""PC draft/edit/archive lifecycle against real APIs. Never publish valid content or broadcast."""
import importlib.util,json,secrets,subprocess,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';evidence=root/'qa/pc-article-admin-ui.json';assert not evidence.exists(),'Inspect existing editorial UI evidence before rerunning'
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis editorial browser action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(70):
  if browser('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Editorial UI readiness timeout')
def call(path,token,body=None,method='GET'):
 req=urllib.request.Request(staff.config.BASE+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method=method)
 try:
  with staff.opener.open(req,timeout=20) as r:return json.load(r)
 except urllib.error.HTTPError as e:
  result=json.load(e);result.setdefault('code',e.code);return result
def submit():browser('click','[type=submit][form=active-dialog-form]')
def closed():wait("!document.querySelector('#active-dialog-form')")
def close():browser('click','.modal-footer [data-action=close-modal]')
def state(target,reason):
 browser('click',f'[data-article-status="{article}"][data-status="{target}"]');wait("!!document.querySelector('[name=reason]')");browser('fill','[name=reason]',reason);submit();closed()
assert browser('url').startswith('http://127.0.0.1:4173/')
admin=staff.administrator(4);old=json.loads((root/'simulator-fixture.json').read_text());doctor=old['session']['token']
assert call('/admin/articles',doctor)['code']==403
prefix='UI_EDITOR_'+secrets.token_hex(5);before=int(staff.config.sql('SELECT COUNT(*) FROM lin_notification_event'))
browser('goto','http://127.0.0.1:4173/#/operations');browser('reload');wait("!!document.querySelector('[data-tab=articles]')");browser('click','[data-tab=articles]');wait("!!document.querySelector('#article-draft-new')")
browser('click','#article-draft-new')
for name,text in [('category',prefix),('title',prefix)]:browser('fill',f'[name={name}]',text)
submit();closed();article=int(staff.config.sql(f"SELECT id FROM lin_article WHERE category='{prefix}'"));fixture={'articleId':article,'category':prefix,'completed':False};staff.save_private(evidence,fixture)
assert call('/articles/'+str(article),doctor)['code']==404
browser('click',f'[data-article-status="{article}"][data-status=published]');wait("!!document.querySelector('[name=reason]')");browser('fill','[name=reason]','空正文不应发布');submit();wait("document.querySelector('#dialog-error').textContent.includes('正文')");close()
assert staff.config.sql(f'SELECT status FROM lin_article WHERE id={article}')=='draft'
browser('click',f'[data-article-edit="{article}"]');wait("!!document.querySelector('[name=reason]')");browser('fill','[name=content]','本机编辑测试正文');browser('fill','[name=reason]','补全正文');submit();closed()
assert staff.config.sql(f'SELECT content FROM lin_article WHERE id={article}')=='本机编辑测试正文'
browser('click',f'[data-article-edit="{article}"]');wait("!!document.querySelector('[name=reason]')")
current=call('/admin/articles/'+str(article),admin)['data']
body={'category':prefix,'type':'article','title':prefix+'另一编辑结果','summary':'','content':'并发提交的正文','coverUrl':'','liveAt':None,'liveUrl':None,'popupEnabled':False}
assert call('/admin/articles/'+str(article),admin,{'article':body,'version':current['version'],'reason':'模拟另一位编辑先保存','requestKey':'editor_'+secrets.token_hex(14)},'PUT')['code']==200
browser('fill','[name=title]',prefix+'旧窗口覆盖');browser('fill','[name=reason]','旧版本提交');submit();wait("document.querySelector('#dialog-error').textContent.includes('其他操作修改')")
assert staff.config.sql(f'SELECT title FROM lin_article WHERE id={article}')==body['title'];close()
browser('reload');wait("!!document.querySelector('[data-tab=articles]')");browser('click','[data-tab=articles]');wait(f"!!document.querySelector('[data-article-edit=\"{article}\"]')")
state('archived','合成草稿归档');browser('select','[data-article-filter]','archived');wait(f"!!document.querySelector('[data-article-status=\"{article}\"][data-status=draft]')")
state('draft','检查归档恢复为草稿');browser('select','[data-article-filter]','draft');wait(f"!!document.querySelector('[data-article-status=\"{article}\"][data-status=archived]')")
state('archived','界面验收完成，仅归档本次合成资讯')
assert staff.config.sql(f'SELECT status FROM lin_article WHERE id={article}')=='archived'
assert call('/articles/'+str(article),doctor)['code']==404
assert int(staff.config.sql('SELECT COUNT(*) FROM lin_notification_event'))==before
result={**fixture,'completed':True,'doctorAdminAccessDenied':True,'draftCreatedThroughUI':True,'draftPublicDetailDenied':True,'invalidPublicationRetainsDraft':True,'editSavedThroughUI':True,'staleDialogCannotOverwriteOtherEditor':True,'archiveFilterAndRestorePassed':True,'finalState':'archived','notificationEventsCreated':0,'validLivePublicationTested':False}
staff.save_private(evidence,result);print(json.dumps(result))
