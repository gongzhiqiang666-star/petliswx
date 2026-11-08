"""Temporary fault injection on an explicitly synthetic published report; restore every field in finally."""
import argparse, importlib.util, json, urllib.error, urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff_setup',Path(__file__).with_name('setup-php-lab.py'));setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
p=argparse.ArgumentParser();p.add_argument('--order-id',type=int,required=True);args=p.parse_args();oid=args.order_id;assert oid>0
assert setup.config.sql(f"SELECT AnimalName LIKE 'QA_REAL_%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}")=='1'
row=setup.config.sql(f"SELECT w.id,w.detail_id,w.test_result_id FROM lin_report_workflow w WHERE w.order_id={oid} AND w.status='PUBLISHED' ORDER BY w.id DESC LIMIT 1").split('\t');assert len(row)==3
wid,did,rid=map(int,row);token=setup.administrator(4)
def get():
 req=urllib.request.Request(setup.config.BASE+f'/admin/print/reports/{did}',headers={'Authorization':'Bearer '+token})
 try:
  with setup.opener.open(req,timeout=20) as response:
   body=response.read().decode()
   if 'json' in response.headers.get('Content-Type',''):return json.loads(body).get('code',response.status),''
   return response.status,body
 except urllib.error.HTTPError as error:
  value=json.load(error);return value.get('code',error.code),''
assert get()[0]==200
checks=[]
for table,key,ident,column,before,after in [('lin_lb_diagnosisdetail','DiagnosisDetailId',did,'IsCheckPass','0','1'),('lin_lb_diagnosisdetail','DiagnosisDetailId',did,'IsActive','1','0'),('lin_report_workflow','id',wid,'status',"'PUBLISHED'","'CANCELLED'"),('lin_test_result','id',rid,'result_status',"'ENTERED'","'VOIDED'")]:
 assert setup.config.sql(f'SELECT {column}={before} FROM {table} WHERE {key}={ident}')=='1'
 try:
  setup.config.sql(f'UPDATE {table} SET {column}={after} WHERE {key}={ident} AND {column}={before}')
  code,_=get();checks.append({'field':column,'rejected':code==409,'code':code})

 finally:
  setup.config.sql(f'UPDATE {table} SET {column}={before} WHERE {key}={ident} AND {column}={after}')
assert get()[0]==200
print(json.dumps({'orderId':oid,'detailId':did,'checks':checks,'syntheticFaultInjectionOnly':True,'allFieldsRestored':True,'publishedPrintWorksAfterRestoration':True},indent=2))

assert all(check['rejected'] for check in checks),'Print lifecycle guards missing; see safe evidence above'
