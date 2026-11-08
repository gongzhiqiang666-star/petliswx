"""Real browser reassignment on a PHP-prepared synthetic task, followed by normal API completion."""
import importlib.util,json,secrets,subprocess,time,urllib.request,os
from pathlib import Path
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
private=setup.config.PRIVATE;fixture_path=private/'qa/lab-reassignment-ui-private.json';assert not fixture_path.is_symlink() and fixture_path.stat().st_mode&0o077==0
f=json.loads(fixture_path.read_text());oid=int(f['orderId']);tid=int(f['taskId']);assert f['prefix'].startswith('QA_REAL_')
assert setup.config.sql(f"SELECT AnimalName LIKE '{f['prefix']}%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Local task UI action failed';return r.stdout
def get(path,token):
 req=urllib.request.Request(setup.config.BASE+path,headers={'Authorization':'Bearer '+token})
 with setup.opener.open(req,timeout=20) as response:data=json.load(response)
 assert data['code']==200;return data['data']
def closed():
 for _ in range(50):
  if 'true' in browser('js',"!document.querySelector('#active-dialog-form')"):return
  time.sleep(.1)
 raise AssertionError('Reassignment dialog did not complete')
admin=setup.administrator(4);relief=None
try:
 current=get(f'/lab/tasks/{tid}',admin);assert current['status']=='TESTING' and current['version']==f['version']
 relief=setup.call('/admin/staff',{'username':'ui_relief_'+secrets.token_hex(5),'displayName':'本机UI合成交接人员','password':secrets.token_urlsafe(24)+'A7','roleCode':'LAB_STAFF','laboratoryId':1,'reason':'仅本机改派UI验收'},admin)
 browser('goto','http://127.0.0.1:4173/#/laboratory');browser('reload');browser('wait','--networkidle');browser('select','#task-filter select','TESTING');browser('click','#task-filter button');browser('wait',f'[data-task="{tid}"]')
 for index,assignee in enumerate([relief,f['originalAssignee']]):
  browser('click',f'[data-task="{tid}"]');browser('wait','#task-reassign')
  assert '检测中' in browser('js',"document.querySelector('#modal-root').textContent")
  assert '首次分配' in browser('js',"document.querySelector('#modal-root').textContent")
  browser('click','#task-reassign');browser('wait','#active-dialog-form')
  browser('select','[name="assigneeId"]',str(assignee));browser('fill','[name="equipmentCode"]','UI-PCR-'+str(index+1))
  if index==0:
   browser('click','[type="submit"][form="active-dialog-form"]')
   assert 'false' in browser('js',"document.querySelector('#active-dialog-form').checkValidity()")
   assert get(f'/lab/tasks/{tid}',admin)['version']==f['version']
  browser('fill','[name="reason"]','本机UI交接验证'+str(index+1));browser('click','[type="submit"][form="active-dialog-form"]');closed()
  task=get(f'/lab/tasks/{tid}',admin);assert task['assigned_to']==assignee and task['equipmentName']=='UI-PCR-'+str(index+1)
  assert task['version']==f['version']+index+1 and task['started_at']==f['startedAt']
  assert len(task['assignmentHistory'])==index+2
 account=setup.config.read_private(private/'lab-bridge-1-credentials.json');lab=setup.call('/auth/password/login',{'account':account['username'],'password':account['password']})['token']
 results=[{'orderItemId':row['itemId'],'name':'UI交接指标','value':'5.20','unit':'mg/L','referenceRange':'3-6'} for row in f['items']]
 setup.call(f'/lab/tasks/{tid}/results',{'results':results},lab)
 browser('reload');browser('wait','--networkidle');browser('select','#task-filter select','RESULT_ENTERED');browser('click','#task-filter button');browser('wait',f'[data-task="{tid}"]');browser('click',f'[data-task="{tid}"]')
 assert 'true' in browser('js',"!document.querySelector('#task-reassign')")
 assert '结果已录入' in browser('js',"document.querySelector('#modal-root').textContent")
 workflow=setup.call('/report-workflows',{'taskId':tid,'summary':'浏览器交接后生成摘要','conclusion':'合成UI验证结论'},lab)[0]
 setup.call(f'/report-workflows/{workflow}/review',{'decision':'APPROVE','reason':'本机UI交接后报告验证'},admin)
 published=setup.call(f'/report-workflows/{workflow}/publish',{},admin);assert published['status']=='PUBLISHED'
 did=published['detail_id'];req=urllib.request.Request(setup.config.BASE+f'/reports/{did}/download',headers={'Authorization':'Bearer '+f['doctorToken']})
 with setup.opener.open(req,timeout=20) as response:assert response.read().startswith(b'%PDF-')
 result={'orderId':oid,'taskId':tid,'reportId':did,'workflowId':workflow,'phpFirstAssignment':True,'browserReassignedTwice':True,'emptyReasonNoMutation':True,'versionIncreasedTwice':True,'startTimePreserved':True,'historyCount':3,'assignmentLabelsChinese':True,'resultsRemoveReassignButton':True,'reportPublishedAndDoctorPdfVerified':True}
 f.pop('doctorToken',None);f['completed']=True;setup.save_private(fixture_path,f)
finally:
 if relief:
  # Never disable the current worker if an interrupted UI test did not hand back the task.
  if get(f'/lab/tasks/{tid}',admin)['assigned_to']==relief:
   task=get(f'/lab/tasks/{tid}',admin)
   setup.call(f'/lab/tasks/{tid}/reassign',{'assigneeId':f['originalAssignee'],'equipmentCode':'UI-QA-RECOVERY','version':task['version'],'reason':'本机界面验收收尾归还原人员'},admin)
  req=urllib.request.Request(setup.config.BASE+f'/admin/users/{relief}/status',data=json.dumps({'active':False,'reason':'本机UI交接验收完成停用合成人员'}).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+admin},method='PATCH')
  with setup.opener.open(req,timeout=20) as response:assert json.load(response)['code']==200
if relief:result['reliefAccountDisabledAtEnd']=True
print(json.dumps(result,ensure_ascii=False,indent=2))
