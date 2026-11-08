"""Own synthetic hospital, real PC multipart upload/replacement; no remote services."""
import importlib.util,json,subprocess,tempfile,time,urllib.request,urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';target=root/'qa/hospital-document-ui.json';assert not target.exists(),'Inspect existing hospital document evidence before replay'
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis hospital document UI action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(70):
  if browser('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Hospital document UI readiness timeout')
def request(path,token=None):
 req=urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+token} if token else {})
 try:
  with staff.opener.open(req,timeout=20) as response:return response.status,response.read(),response.headers
 except urllib.error.HTTPError as error:return error.code,error.read(),error.headers
def code(result):
 try:return json.loads(result[1]).get('code',result[0])
 except ValueError:return result[0]
def submit():browser('click','[type=submit][form=active-dialog-form]')
def closed():wait("!document.querySelector('#active-dialog-form')")
if not browser('url').startswith('http://127.0.0.1:4173/'):browser('newtab','http://127.0.0.1:4173/#/organizations')
for _ in range(40):
 try:
  with staff.opener.open(staff.config.BASE+'/auth/capabilities',timeout=2) as ready:assert ready.status==200;break
 except OSError:time.sleep(.3)
else:raise AssertionError('Petlis backend not ready')
admin=staff.administrator(4);doctor=json.loads((root/'simulator-fixture.json').read_text())['session']['token']
browser('goto','http://127.0.0.1:4173/#/organizations');browser('reload')
if browser('js',"!!document.querySelector('#login-form')")=='true':
 php="$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);"
 r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;account=json.loads(r.stdout)
 browser('fill','[name=username]',account['username']);browser('fill','[name=password]',account['password']);browser('click','#login-form button[type=submit]');wait("!document.querySelector('#login-form')")
browser('goto','http://127.0.0.1:4173/#/organizations');wait("!!document.querySelector('[data-tab=hospitals]')");browser('click','[data-tab=hospitals]');wait("!!document.querySelector('[data-hospital-documents]')")
hospital=int(browser('js',"Array.from(document.querySelectorAll('[data-hospital-documents]')).find(x=>x.closest('tr').textContent.includes('QA_REAL_')).dataset.hospitalDocuments"));assert staff.config.sql(f"SELECT HospitalName LIKE 'QA_REAL_%' FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}")=='1'
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT'")=='0'
assert code(request(f'/admin/hospitals/{hospital}/documents',doctor))==403
status,pdf,_=request('/invoices/16/download',admin);assert status==200 and pdf.startswith(b'%PDF-')
with tempfile.TemporaryDirectory(prefix='petlis-hospital-document-',dir='/private/tmp') as folder:
 invalid=Path(folder)/'invalid.pdf';invalid.write_text('invalid local test');first=Path(folder)/'医院执照.pdf';first.write_bytes(pdf);second=Path(folder)/'医院执照更新.pdf';second.write_bytes(pdf+b'\n% local replacement test\n')
 browser('click',f'[data-hospital-documents="{hospital}"]');wait("!!document.querySelector('[name=documentFile]')");browser('fill','[name=reason]','本机界面验收首次归档');submit();wait("document.querySelector('#dialog-error').textContent.includes('请选择')")
 browser('upload','[name=documentFile]',str(invalid));submit();wait("document.querySelector('#dialog-error').textContent.includes('魔数')");assert browser('js',"document.querySelector('[name=documentFile]').files.length===1")=='true'
 assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT'")=='0'
 browser('upload','[name=documentFile]',str(first));submit();closed();first_id=int(staff.config.sql(f"SELECT id FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT' ORDER BY id DESC LIMIT 1"));staff.save_private(target,{'hospitalId':hospital,'firstFileId':first_id,'completed':False})
 browser('click',f'[data-hospital-documents="{hospital}"]');wait("!!document.querySelector('[name=previousFileId]')");browser('select','[name=previousFileId]',str(first_id));browser('fill','[name=reason]','本机界面验收替换，保留旧文件');browser('upload','[name=documentFile]',str(second));submit();closed()
 next_id=int(staff.config.sql(f"SELECT id FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT' ORDER BY id DESC LIMIT 1"));assert next_id!=first_id
 browser('click',f'[data-hospital-documents="{hospital}"]');wait("!!document.querySelector('#hospital-doc-history')");assert '历史版本' in browser('js',"document.querySelector('#hospital-doc-history').textContent");assert '替换 #'+str(first_id) in browser('js',"document.querySelector('#hospital-doc-history').textContent")
 assert browser('js',f"!document.querySelector('[name=previousFileId] option[value=\"{first_id}\"]')")=='true'
 for file_id,expected in [(first_id,pdf),(next_id,second.read_bytes())]:
  response,body,headers=request(f'/platform-files/{file_id}/download',admin);assert response==200 and body==expected;assert "filename*=UTF-8''" in headers.get('Content-Disposition','')
  assert code(request(f'/platform-files/{file_id}/download',doctor))==403
  assert code(request(f'/platform-files/{file_id}/download')) in (401,403)
 assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT'")=='2'
 result={'hospitalId':hospital,'fileIds':[first_id,next_id],'emptyFormAndInvalidPdfRetained':True,'realMultipartUploadAndReplacement':True,'twoVersionsVisible':True,'oldVersionNoLongerReplacementChoice':True,'oldAndNewFilesDownloadIdentically':True,'chineseFilenameHeaderValid':True,'doctorAndAnonymousDenied':True,'completed':True}
 staff.save_private(target,result);print(json.dumps(result));browser('click','.modal-footer [data-action=close-modal]')
