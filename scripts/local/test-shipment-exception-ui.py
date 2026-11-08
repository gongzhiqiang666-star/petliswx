"""Resolve all incidents through real browser forms; preserve transport state, then finish synthetic report flow."""
import importlib.util,json,subprocess,time,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('setup',Path(__file__).with_name('setup-php-lab.py'));setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
private=setup.config.PRIVATE;p=private/'qa/shipment-exception-ui-private.json';assert not p.is_symlink() and p.stat().st_mode&0o077==0
f=json.loads(p.read_text());assert not f.get('completed');oid=int(f['orderId']);sid=int(f['shipmentId']);assert f['prefix'].startswith('QA_REAL_')
assert setup.config.sql(f"SELECT AnimalName LIKE '{f['prefix']}%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 r=subprocess.run([B,*args],capture_output=True,text=True);assert r.returncode==0,'Local incident UI action failed';return r.stdout
def get(path,token):
 req=urllib.request.Request(setup.config.BASE+path,headers={'Authorization':'Bearer '+token})
 with setup.opener.open(req,timeout=20) as r:data=json.load(r)
 assert data['code']==200;return data['data']
def closed():
 for _ in range(50):
  if 'true' in browser('js',"!document.querySelector('#active-dialog-form')"):return
  time.sleep(.1)
 raise AssertionError('Incident dialog did not complete')
a=setup.administrator(4);summary={'orderId':oid,'shipmentId':sid,'browser':'independent headless Chromium','resolvedKinds':[]}
evidence=private/'qa/shipment-exception-ui.json'
setup.save_private(evidence,summary)
browser('goto','http://127.0.0.1:4173/#/logistics');browser('reload');browser('wait','--networkidle');browser('select','#shipment-filter select','IN_TRANSIT');browser('click','#shipment-filter button');browser('wait',f'[data-action="detail"][data-id="{sid}"]')
for kind in ['DELAY','LOSS','DAMAGE']:
 before=get(f'/logistics/{sid}',a);assert before['status']=='IN_TRANSIT'
 event=next(row for row in before['exceptions'] if row['exceptionType']==kind);assert not event.get('resolvedAt')
 browser('click',f'[data-action="detail"][data-id="{sid}"]');browser('wait',f'[data-resolve-exception="{event["id"]}"]')
 assert '运输中' in browser('js',"document.querySelector('#modal-root').textContent")
 browser('click',f'[data-resolve-exception="{event["id"]}"]');browser('wait','#active-dialog-form')
 if not summary['resolvedKinds']:
  browser('click','[type="submit"][form="active-dialog-form"]')
  assert 'false' in browser('js',"document.querySelector('#active-dialog-form').checkValidity()")
  assert get(f'/logistics/{sid}',a)['version']==before['version'];summary['emptyReasonBlockedWithoutMutation']=True
 browser('fill','[name="reason"]','本机界面核实'+kind+'已处理，继续保留其他异常')
 browser('click','[type="submit"][form="active-dialog-form"]');closed()
 after=get(f'/logistics/{sid}',f['doctorToken']);assert after['status']=='IN_TRANSIT' and after['version']==before['version']+1
 assert len(after['exceptions'])==3
 remaining=[row for row in after['exceptions'] if not row.get('resolvedAt')]
 assert len(remaining)==2-len(summary['resolvedKinds'])
 if remaining:assert after.get('exceptionNote')
 else:assert not after.get('exceptionNote') and not after.get('exceptionType')
 summary['resolvedKinds'].append(kind);setup.save_private(evidence,summary)
browser('click',f'[data-action="detail"][data-id="{sid}"]')
assert 'true' in browser('js',"document.querySelectorAll('[data-resolve-exception]').length===0")
assert '已处理' in browser('js',"document.querySelector('#modal-root').textContent")
summary.update(allHistoryPreserved=True,doctorReadsResolvedHistory=True,remainingWarningsPreservedUntilLast=True,allResolvedClearsCurrentWarning=True,transportNotAutoAdvanced=True,allResolvedRemovesButtons=True)
setup.save_private(evidence,summary)
# Explicit normal courier and lab operations, never infer receipt from resolving an incident.
setup.call(f'/logistics/{sid}/deliver',{},f['courierToken'])
account=setup.config.read_private(private/'lab-bridge-1-credentials.json');lab=setup.call('/auth/password/login',{'account':account['username'],'password':account['password']})['token']
setup.call(f'/logistics/{sid}/scan-receive',{'barcode':f['userNO']},lab)
task=setup.call(f'/lab/tasks/order/{oid}/assign',{'assigneeId':account['userId'],'equipmentCode':'UI-EXCEPTION-PCR'},lab)
results=[{'orderItemId':row['itemId'],'name':'异常处理后指标','value':'5.20','unit':'mg/L','referenceRange':'3-6'} for row in f['items']]
setup.call(f'/lab/tasks/{task["id"]}/results',{'results':results},lab)
wid=setup.call('/report-workflows',{'taskId':task['id'],'summary':'异常处理后受控送检摘要','conclusion':'本机合成验证结论'},lab)[0]
setup.call(f'/report-workflows/{wid}/review',{'decision':'APPROVE','reason':'本机异常UI闭环验证'},a)
published=setup.call(f'/report-workflows/{wid}/publish',{},a);assert published['status']=='PUBLISHED';did=published['detail_id']
req=urllib.request.Request(setup.config.BASE+f'/reports/{did}/download',headers={'Authorization':'Bearer '+f['doctorToken']})
with setup.opener.open(req,timeout=20) as r:assert r.read().startswith(b'%PDF-')
summary.update(taskId=task['id'],workflowId=wid,reportId=did,explicitTransportAndLabSteps=True,publishedDoctorPdfVerified=True)
f.pop('doctorToken',None);f.pop('courierToken',None);f['completed']=True;setup.save_private(p,f);setup.save_private(evidence,summary)
print(json.dumps(summary,ensure_ascii=False,indent=2))
