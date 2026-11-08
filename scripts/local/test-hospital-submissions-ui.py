"""Actual PC self-service and platform review, own synthetic hospital and files only."""
import importlib.util,json,subprocess,tempfile,time,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';f=json.loads((root/'qa/hospital-submissions-private.json').read_text());target=root/'qa/hospital-submissions-ui.json';assert not target.exists(),'Inspect existing browser evidence before repeating writes'
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis hospital submission browser action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(100):
  if browser('js',expr)=='true':return
  time.sleep(.12)
 raise AssertionError('Hospital submission UI readiness timeout')
def login(account):
 if browser('js',"!!document.querySelector('#logout')")=='true':browser('click','#logout')
 wait("!!document.querySelector('#login-form')")
 browser('fill','[name=username]',account['account']);browser('fill','[name=password]',account['password']);browser('click','#login-form button[type=submit]');wait("!document.querySelector('#login-form')&&!!document.querySelector('#logout')")
def submit():browser('click','[type=submit][form=active-dialog-form]')
php="$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);";r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;p=json.loads(r.stdout);admin={'account':p['username'],'password':p['password']}
if not browser('url').startswith('http://127.0.0.1:4173/'):browser('newtab','http://127.0.0.1:4173/#/hospital-documents')
else:browser('goto','http://127.0.0.1:4173/#/hospital-documents');browser('reload')
try:
 login(f);browser('goto','http://127.0.0.1:4173/#/hospital-documents');wait("!!document.querySelector('[data-submission-download]')")
 assert browser('js',"!document.querySelector('[data-route=organizations]')&&!document.querySelector('[data-route=reviews]')")=='true'
 assert int(browser('js',"document.querySelector('#document-hospital').options.length"))==1
 content=browser('js',"document.querySelector('#document-list').textContent")
 for expected in ['待审核','已通过','已驳回','历史版本','请提供清晰版本']:assert expected in content
 with tempfile.TemporaryDirectory(prefix='petlis-self-doc-',dir='/private/tmp') as folder:
  invalid=Path(folder)/'bad.pdf';invalid.write_text('invalid');pdf=Path(folder)/'医院合作资料.pdf';pdf.write_bytes((root/'qa/native-profile-qualification.pdf').read_bytes())
  browser('click','#document-submit');wait("!!document.querySelector('[name=documentFile]')");browser('select','[name=purpose]','COOPERATION');browser('fill','[name=reason]','本机浏览器提交合作资料');browser('upload','[name=documentFile]',str(invalid));submit();wait("document.querySelector('#dialog-error').textContent.includes('魔数')")
  assert browser('js',"document.querySelector('[name=reason]').value==='本机浏览器提交合作资料'&&document.querySelector('[name=documentFile]').files.length===1")=='true'
  browser('upload','[name=documentFile]',str(pdf));submit();wait("!document.querySelector('#active-dialog-form')");wait("document.querySelector('#document-list').textContent.includes('医院合作资料.pdf')")
  new=int(staff.config.sql(f"SELECT f.id FROM lin_file f JOIN lin_audit_record a ON CAST(a.resource_id AS UNSIGNED)=f.id WHERE f.hospital_id={f['hospitalId']} AND a.action='HOSPITAL_DOCUMENT_SUBMITTED' AND f.original_name='医院合作资料.pdf' ORDER BY f.id DESC LIMIT 1"));f['uiUploadedFileId']=new;staff.save_private(root/'qa/hospital-submissions-private.json',f)
  browser('screenshot','/private/tmp/petlis-hospital-self-service.png');(root/'qa/hospital-self-service.png').write_bytes(Path('/private/tmp/petlis-hospital-self-service.png').read_bytes())
 login(admin);browser('goto','http://127.0.0.1:4173/#/reviews');wait("!!document.querySelector('[data-tab=hospitalDocuments]')");browser('click','[data-tab=hospitalDocuments]');wait(f"!!document.querySelector('[data-review=\"{new}\"]')")
 browser('click',f'[data-review="{new}"]');wait("!!document.querySelector('[data-review-document]')");assert '医院合作资料.pdf' in browser('js',"document.querySelector('.modal-body').textContent")
 browser('fill','[name=reason]',' ');submit();wait("document.querySelector('#dialog-error').textContent.includes('审核意见')");browser('fill','[name=reason]','合作资料清晰，核对通过');submit();wait("!document.querySelector('#active-dialog-form')")
 # The corrected HTTP fixture is rejected through the real UI to cover both decisions.
 pending=f['uiPendingFileId'];wait(f"!!document.querySelector('[data-review=\"{pending}\"]')");browser('click',f'[data-review="{pending}"]');wait("!!document.querySelector('[name=approved]')");browser('select','[name=approved]','false');browser('fill','[name=reason]','本机测试记录，实际资料无需重复归档');submit();wait("!document.querySelector('#active-dialog-form')")
 assert staff.config.sql(f"SELECT biz_type FROM lin_file WHERE id={new}")=='HOSPITAL_DOCUMENT'
 login(f);browser('goto','http://127.0.0.1:4173/#/hospital-documents');wait("document.querySelector('#document-list')&&document.querySelector('#document-list').textContent.includes('合作资料清晰，核对通过')")
 browser('screenshot','/private/tmp/petlis-hospital-reviewed.png');(root/'qa/hospital-reviewed.png').write_bytes(Path('/private/tmp/petlis-hospital-reviewed.png').read_bytes())
 evidence={'hospitalId':f['hospitalId'],'managerUserId':f['userId'],'uiUploadedFileId':new,'approvedViaActualUi':True,'rejectedViaActualUi':True,'emptyReasonRetainsForm':True,'invalidPdfRetainsFileAndReasonThenCorrected':True,'scopedHospitalSelector':True,'ordinaryAdminMenusHiddenFromHospitalManager':True,'managerSeesReviewResultsAndHistory':True,'ddlExecuted':False};staff.save_private(target,evidence);print(json.dumps(evidence))
finally:
 login(admin);browser('goto','http://127.0.0.1:4173/#/reviews')
