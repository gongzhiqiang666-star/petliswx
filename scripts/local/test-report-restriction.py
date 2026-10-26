# Real local HTTP verification for report payment restriction; synthetic data only.
from pathlib import Path
import json, re, subprocess, sys, time, urllib.error, urllib.parse, urllib.request

scripts=Path(__file__).resolve().parent
root=Path.home()/'.local/share/petlis'
state=root/'simulator-fixture.json'
base='http://127.0.0.1:8082/api'
mysql=root/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
pdf_root=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/public/Content/FilePdf')
backend_root=Path(__file__).resolve().parents[2]/'petlis-backend'

def sql(statement):
    return subprocess.run([str(mysql),f'--defaults-extra-file={root}/mysql/root.cnf','--batch','--skip-column-names','laboklin'],input=statement.encode(),capture_output=True,check=True).stdout.decode().strip()

def call(path,data=None,token=None,method=None,raw=False):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    request=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
    try:
        with urllib.request.urlopen(request) as response:
            body=response.read();status=response.status
    except urllib.error.HTTPError as error:
        body=error.read();status=error.code
    if raw:return status,body
    return status,json.loads(body) if body else None

def ok(path,data=None,token=None,method=None):
    for attempt in range(20):
        status,result=call(path,data,token,method)
        if status!=429:break
        time.sleep(1)
    assert status==200 and result and result.get('code')==200,(path,status,result)
    return result.get('data')

def cleanup_business(uid):
    order_ids=sql(f'SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)};')
    sql(f'DELETE FROM lin_lb_diagnosisdetail_info WHERE diagnosisdetail_id IN (SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}));')
    sql(f'DELETE d FROM lin_lb_notification_log d JOIN lin_lb_notification n ON n.id=d.notification_id WHERE n.order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_lb_notification WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_invoice_order WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_wechat_payment WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_order_extension WHERE order_id IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId IN (SELECT ListDiagnosisProjectId FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)}); DELETE FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={int(uid)};')
    return order_ids

if state.exists():
    raise SystemExit('Refusing to replace an existing simulator fixture')

pdf_paths=[]
try:
    subprocess.run([sys.executable,str(scripts/'prepare-simulator.py')],check=True)
    fixture=json.loads(state.read_text())
    uid=int(fixture['userId']);pet_id=int(fixture['petId'])
    assert re.fullmatch(r'petlis_qa_[a-f0-9]+',fixture['account'])
    cash_hospital=int(sql('SELECT InfoHospitalId FROM lin_lb_infohospital WHERE IsActive=1 AND CityId>0 AND PayType=0 ORDER BY InfoHospitalId LIMIT 1;'))
    sql(f"UPDATE lin_user SET hospital_id={cash_hospital},openid='petlis_report_mock_{fixture['account']}' WHERE id={uid} AND username='{fixture['account']}'; UPDATE lin_lb_pet SET info_hospital_id={cash_hospital},info_variety_id=(SELECT IID FROM lin_lb_infovariety ORDER BY IID LIMIT 1) WHERE id={pet_id} AND animal_name='{fixture['account']} pet';")
    doctor=ok('/auth/password/login',{'account':fixture['account'],'password':fixture['password']})['token']
    reviewer_password=next(line.split('：',1)[1] for line in (root/'reviewer-credentials.txt').read_text().splitlines() if line.startswith('密码：'))
    reviewer=ok('/auth/password/login',{'account':'petlis_reviewer','password':reviewer_password})['token']
    pet=ok('/pets/'+str(pet_id),token=doctor)
    labs=ok('/laboratories',token=doctor);lab=labs[0]['id']
    tests=ok(f'/laboratories/{lab}/tests?page=0&size=100',token=doctor)['items']
    test=next(item for item in tests if item['infoTestTypeId']!=1225 and item.get('price') is not None and float(item['price'])>=0)
    sample=int(sql('SELECT InfoSampleTypeId FROM lin_lb_infosampletype WHERE IsActive=1 LIMIT 1;'))
    payload={
        'laboratoryId':lab,'infoAnimalId':pet['infoAnimalId'],'infoSampleTypeId':sample,
        'animalName':pet['animalName'],'animalNameEn':pet['animalNameEn'],
        'animalTypeId':pet['infoVarietyId'],'animalType':pet['infoVarietyName'],'animalTypeEn':pet['infoVarietyNameEn'],
        'dictSexId':pet['dictSexId'],'age':'3Y','animalWeight':'',
        'sampleArea':'report restriction fixture','masterName':pet['ownerName'],'masterNameEn':pet['ownerNameEn'],
        'doctorName':'Report Restriction QA','collectDt':'2026-10-25','remark':'',
        'diagnosisDetails':[{'infoTestTypeId':test['infoTestTypeId']}]
    }
    order_id=ok('/orders',payload,doctor)['orderId']
    ok('/review/orders/'+str(order_id),{'approved':True,'reason':'report restriction HTTP QA'},reviewer)
    assert ok('/orders/'+str(order_id),token=doctor)['sampleStatus']==-3
    payment=ok('/payment/create',{'orderId':order_id},doctor)
    assert payment['mock'] is True,payment
    print('1 Synthetic cash order approved and remains unpaid',flush=True)

    pdf_name=f'petlis-report-restriction-{order_id}-{int(time.time())}.pdf'
    pdf_paths=[pdf_root/pdf_name,backend_root/pdf_name]
    for pdf_path in pdf_paths:pdf_path.write_bytes(b'%PDF-1.4\n% synthetic report restriction QA\n%%EOF\n')
    sql(f"UPDATE lin_lb_diagnosisdetail SET DiagnosisStatus=3,IsSendMail=1,PDFHttpUrl='{pdf_name}',SendMailDt=NOW() WHERE ListDiagnosisProjectId={order_id} AND IsCheckPass=0;")
    report_id=int(sql(f'SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order_id} AND IsCheckPass=0 LIMIT 1;'))
    sql(f"INSERT INTO lin_lb_diagnosisdetail_info(diagnosisdetail_id,MailReport,ReportResult) VALUES({report_id},'QA secret mail','QA secret result');")

    reports=ok('/reports?pageNum=0&pageSize=100',token=doctor)
    row=next(item for item in reports['content'] if item['orderId']==order_id and item['reportId']==report_id)
    assert row['paymentRestricted'] is True,row
    assert not row.get('securePdfUrl'),row
    print('2 Unpaid report list entry is visible, paymentRestricted=true, without securePdfUrl',flush=True)

    detail=ok('/reports/'+str(report_id),token=doctor)
    assert detail['paymentRestricted'] is True and detail['orderId']==order_id,detail
    assert not detail.get('reportResult') and not detail.get('mailReport') and not detail.get('pdfHttpUrl'),detail
    print('3 Unpaid report detail keeps metadata but masks reportResult/mailReport/pdfHttpUrl',flush=True)

    status,_=call('/pdf/view/'+str(report_id),token=doctor,raw=True);assert status==403,status
    status,_=call('/reports/'+str(report_id)+'/download',token=doctor,raw=True);assert status==400,status
    status,result=call('/reports/'+str(report_id)+'/pdf-url',token=doctor);assert status==200 and result['code']!=200 and '订单未支付' in result['message'],result
    print('4 Unpaid /pdf/view=403, /reports/{id}/download=400, pdf-url business error',flush=True)

    ok('/payment/mock-callback/'+urllib.parse.quote(payment['paymentNo']),{},doctor)
    assert ok('/orders/'+str(order_id),token=doctor)['sampleStatus']==2

    reports=ok('/reports?pageNum=0&pageSize=100',token=doctor)
    row=next(item for item in reports['content'] if item['orderId']==order_id and item['reportId']==report_id)
    assert row['paymentRestricted'] is False,row
    detail=ok('/reports/'+str(report_id),token=doctor)
    assert detail['paymentRestricted'] is False and detail['reportResult']=='QA secret result' and detail['mailReport']=='QA secret mail' and detail['pdfHttpUrl']==pdf_name,detail
    print('5 After mock payment list/detail are unrestricted and report content is restored',flush=True)

    status,body=call('/pdf/view/'+str(report_id),token=doctor,raw=True);assert status==200 and body.startswith(b'%PDF-'),(status,body[:100])
    status,body=call('/reports/'+str(report_id)+'/download',token=doctor,raw=True);assert status==200 and body.startswith(b'%PDF-'),(status,body[:100])
    print('6 After mock payment both PDF endpoints return the fixture bytes; no real payment',flush=True)
    external_url='https://example.test/synthetic-paid-report.pdf'
    sql(f"INSERT INTO lin_lb_diagnosisdetail(ListDiagnosisProjectId,GenterNO,InfoTestTypeId,InfoTestTypeModelId,MailTitle,CreateDt,IsChangeModelTemple,IsChangeMailModelTemple,IsSendMail,RealFee,IsCheckPass,PDFHttpUrl,TestPrice,OrginalPrice,OrginalDiscount,SendMailDt,DetailRemark,PayStatus2,DiagnosisStatus,IsActive,is_invoiced) SELECT ListDiagnosisProjectId,CONCAT(GenterNO,'-paid-url'),InfoTestTypeId,InfoTestTypeModelId,MailTitle,CreateDt,IsChangeModelTemple,IsChangeMailModelTemple,IsSendMail,RealFee,IsCheckPass,'{external_url}',TestPrice,OrginalPrice,OrginalDiscount,SendMailDt,DetailRemark,PayStatus2,DiagnosisStatus,IsActive,is_invoiced FROM lin_lb_diagnosisdetail WHERE DiagnosisDetailId={report_id};")
    url_report_id=int(sql(f"SELECT DiagnosisDetailId FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order_id} AND GenterNO LIKE '%-paid-url' ORDER BY DiagnosisDetailId DESC LIMIT 1;"))
    url_status,url_result=call('/reports/'+str(url_report_id)+'/pdf-url',token=doctor)
    assert url_status==200 and url_result.get('code')==200 and url_result.get('message')==external_url,(url_status,url_result)
    print('7 After mock payment pdf-url is also released',flush=True)
    print('REPORT PAYMENT RESTRICTION HTTP PASSED',flush=True)
finally:
    if state.exists():
        fixture=json.loads(state.read_text())
        cleanup_business(int(fixture['userId']))
        subprocess.run([sys.executable,str(scripts/'cleanup-simulator.py')],check=True)
    for pdf_path in pdf_paths:pdf_path.unlink(missing_ok=True)
    print('Synthetic report/order/payment/user data and temporary PDFs cleaned',flush=True)
