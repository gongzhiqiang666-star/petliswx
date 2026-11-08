"""Real local multipart replay/replace concurrency on the exact hospital used by the PC test."""
import importlib.util,json,secrets,urllib.request,urllib.error
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';fixture=json.loads((root/'qa/hospital-document-ui.json').read_text());assert fixture['completed']
hospital=int(fixture['hospitalId']);previous=int(fixture['fileIds'][-1]);assert staff.config.sql(f"SELECT HospitalName LIKE 'QA_REAL_%' FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}")=='1'
target=root/'qa/hospital-document-concurrency.json';assert not target.exists(),'Inspect previous concurrency evidence before replay'
admin=staff.administrator(4);doctor=json.loads((root/'simulator-fixture.json').read_text())['session']['token']
with staff.opener.open(urllib.request.Request(staff.config.BASE+f'/platform-files/{fixture["fileIds"][0]}/download',headers={'Authorization':'Bearer '+admin}),timeout=20) as response:pdf=response.read()
assert pdf.startswith(b'%PDF-');key='hospital_doc_'+secrets.token_hex(14);boundary='PetlisHospitalDocumentTest'
def upload(hospital_id,request_key,content=pdf+b'\n% concurrent replacement\n',prior=previous,token=admin):
 fields={'purpose':'LICENSE','reason':'本机并发替换验收','requestKey':request_key,'previousFileId':str(prior)}
 body=b''
 for name,value in fields.items():body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n').encode()
 body+=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="concurrent.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+content+(f'\r\n--{boundary}--\r\n').encode()
 request=urllib.request.Request(staff.config.BASE+f'/admin/hospitals/{hospital_id}/documents',data=body,headers={'Authorization':'Bearer '+token,'Content-Type':'multipart/form-data; boundary='+boundary})
 try:
  with staff.opener.open(request,timeout=25) as response:return json.load(response)
 except urllib.error.HTTPError as error:
  result=json.load(error);result.setdefault('code',error.code);return result
with ThreadPoolExecutor(max_workers=4) as pool:replies=list(pool.map(lambda _:upload(hospital,key),range(4)))
assert all(row['code']==200 for row in replies);ids={int(row['data']['id']) for row in replies};assert len(ids)==1;new_id=ids.pop()
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='HOSPITAL_DOCUMENT' AND request_id='{key}'")=='1'
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE hospital_id={hospital} AND biz_type='HOSPITAL_DOCUMENT'")=='3'
assert upload(hospital,key,pdf+b'\n% changed body\n')['code']==409
assert upload(hospital,'hospital_doc_'+secrets.token_hex(14))['code']==409
other=int(json.loads((root/'qa/native-invoice-fixture-private.json').read_text())['hospitalId']);assert other!=hospital and staff.config.sql(f"SELECT HospitalName LIKE 'QA_REAL_%' FROM lin_lb_infohospital WHERE InfoHospitalId={other}")=='1'
assert upload(other,'hospital_doc_'+secrets.token_hex(14),prior=new_id)['code']==404
assert upload(hospital,'hospital_doc_'+secrets.token_hex(14),prior=new_id,token=doctor)['code']==403
result={'hospitalId':hospital,'previousFileId':previous,'newFileId':new_id,'fourParallelOneFileAndAudit':True,'changedPayloadConflict':True,'stalePreviousConflict':True,'crossHospitalReplacementDenied':True,'doctorUploadDenied':True,'allHistoricalFilesRetained':True}
target.write_text(json.dumps(result,indent=2));print(json.dumps(result))
