"""Run the synthetic P0 simulator flow; always remove its business fixtures."""
from pathlib import Path
import json, os, re, subprocess, sys, urllib.request

scripts=Path(__file__).resolve().parent
root=Path.home()/'.local/share/petlis'
state=root/'simulator-fixture.json'
base='http://127.0.0.1:8082/api'
mysql=root/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'

def sql(statement):
    return subprocess.run([str(mysql),f'--defaults-extra-file={root}/mysql/root.cnf','--batch','--skip-column-names','laboklin'],input=statement.encode(),capture_output=True,check=True).stdout.decode().strip()

def python(name):
    subprocess.run([sys.executable,str(scripts/name)],check=True)

def login(account,password):
    request=urllib.request.Request(base+'/auth/password/login',data=json.dumps({'account':account,'password':password}).encode(),headers={'Content-Type':'application/json'},method='POST')
    with urllib.request.urlopen(request) as response:
        result=json.load(response)
    assert result['code']==200,result
    return result['data']

def use_cash_hospital():
    fixture=json.loads(state.read_text())
    uid=int(fixture['userId'])
    assert re.fullmatch(r'petlis_qa_[a-f0-9]+',fixture['account'])
    hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 AND PayType=0 ORDER BY InfoHospitalId LIMIT 1;'))
    sql(f"UPDATE lin_user SET hospital_id={hospital},openid='petlis_qa_mock_{fixture['account']}' WHERE id={uid} AND username='{fixture['account']}';")
    pet=int(fixture['petId'])
    sql(f"UPDATE lin_lb_pet SET info_hospital_id={hospital},info_variety_id=(SELECT IID FROM lin_lb_infovariety ORDER BY IID LIMIT 1) WHERE id={pet} AND animal_name='{fixture['account']} pet';")
    fixture['session']=login(fixture['account'],fixture['password'])
    assert int(fixture['session']['hospitalId'])==hospital
    state.write_text(json.dumps(fixture));state.chmod(0o600)
    print(f'Synthetic doctor uses cash hospital {hospital}; mock payment path enabled')

def cleanup_business():
    if not state.exists():return
    fixture=json.loads(state.read_text())
    uid=int(fixture['userId'])
    assert re.fullmatch(r'petlis_qa_[a-f0-9]+',fixture['account'])
    references=sql(f"SELECT PDFHttpUrl FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid});")
    sql(f'DELETE d FROM lin_lb_notification_log d JOIN lin_lb_notification n ON n.id=d.notification_id WHERE n.order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_lb_notification WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_invoice_order WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_wechat_payment WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_order_extension WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid}); DELETE FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid};')
    pdf_root=Path(os.environ.get('PETLIS_PDF_ROOT','/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/public/Content/FilePdf'))
    candidates=[Path(fixture['fullFlowPdf'])] if fixture.get('fullFlowPdf') else []
    candidates += [pdf_root/Path(line).name for line in references.splitlines() if line]
    for candidate in candidates:
        if re.fullmatch(r'petlis-full-flow-\d+-\d+\.pdf',candidate.name) and candidate.parent==pdf_root:
            candidate.unlink(missing_ok=True)

if state.exists():
    cleanup_business()
    python('cleanup-simulator.py')
try:
    python('prepare-simulator.py')
    use_cash_hospital()
    env=dict(os.environ)
    for key in ('NODE_OPTIONS','HTTP_PROXY','HTTPS_PROXY','ALL_PROXY','http_proxy','https_proxy','all_proxy'):
        env.pop(key,None)
    result=subprocess.run(['node',str(scripts/'test-full-flow.js')],env=env,timeout=600)
    raise SystemExit(result.returncode)
finally:
    if state.exists():
        cleanup_business()
        python('cleanup-simulator.py')
