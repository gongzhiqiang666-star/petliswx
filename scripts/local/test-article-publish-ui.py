"""Real PC form invalid submissions only: no article broadcast or valid publication is triggered."""
import importlib.util,json,secrets,subprocess,time
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
    r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis article browser action failed';return r.stdout.strip()
def wait(expression):
    for _ in range(60):
        if browser('js',expression)=='true':return
        time.sleep(.15)
    raise AssertionError('Article publish form readiness timeout')
assert browser('url').startswith('http://127.0.0.1:4173/')
prefix='UI_ARTICLE_'+secrets.token_hex(6)
before=int(staff.config.sql('SELECT COUNT(*) FROM lin_notification_event'))
browser('goto','http://127.0.0.1:4173/#/operations');browser('reload')
if browser('js',"!!document.querySelector('#login-form')")=='true':
    php="$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"
    r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;account=json.loads(r.stdout)
    browser('fill','[name=username]',account['username']);browser('fill','[name=password]',account['password']);browser('click','#login-form button[type=submit]');wait("!document.querySelector('#login-form')")
browser('goto','http://127.0.0.1:4173/#/operations');wait("!!document.querySelector('[data-tab=articles]')");browser('click','[data-tab=articles]');wait("!!document.querySelector('#article-new')")
def fill_form():
    for name,value in [('category',prefix),('title',prefix),('summary','本机错误校验'),('content','本机测试正文，不实际发布')]:browser('fill',f'[name={name}]',value)
def submit():
    browser('click','[type=submit][form=active-dialog-form]');wait("!!document.querySelector('#dialog-error').textContent && !document.querySelector('[type=submit][form=active-dialog-form]').disabled")
try:
    browser('click','#article-new');fill_form();browser('fill','[name=coverUrl]','javascript:alert(1)');submit()
    assert prefix in browser('js',"document.querySelector('[name=title]').value")
    browser('fill','[name=coverUrl]','https://');submit()
    browser('click','.modal-footer [data-action=close-modal]');browser('click','#article-new');fill_form();browser('fill','[name=summary]','x'*1001);submit()
    assert staff.config.sql(f"SELECT COUNT(*) FROM lin_article WHERE title='{prefix}'")=='0'
    assert int(staff.config.sql('SELECT COUNT(*) FROM lin_notification_event'))==before
    result={'invalidCoverRejected':True,'incompleteHttpsRejected':True,'overlongSummaryRejected':True,'failedFormRetained':True,'articleRowsCreated':0,'notificationEventsCreated':0,'realPublishSuccessTested':False}
    (Path.home()/'.local/share/petlis/qa/pc-article-publish-validation.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
finally:
    if browser('js',"!!document.querySelector('#active-dialog-form')")=='true':browser('click','.modal-footer [data-action=close-modal]')

browser('reload')
