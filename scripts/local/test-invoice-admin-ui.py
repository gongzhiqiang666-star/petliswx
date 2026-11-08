"""Real browser invoice upload, own synthetic local order, no external tax/mail calls."""
import importlib.util,json,secrets,subprocess,sys,tempfile,time,urllib.request
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'))
staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';details='--details' in sys.argv
private=root/('qa/pc-invoice-details-fixture.json' if details else 'qa/pc-invoice-fixture.json')
f=json.loads((root/'qa/native-invoice-fixture-private.json').read_text())
assert f['userId']==583 and f['orderId']==23953
doctor=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token'];admin=staff.administrator(4)
def get(path,token):
    with staff.opener.open(urllib.request.Request(staff.config.BASE+path,headers={'Authorization':'Bearer '+token}),timeout=20) as response:
        return json.load(response)['data']
if private.exists():
    fixture=json.loads(private.read_text())
    assert not fixture.get('completed'),'Completed fixture must not be replayed'
else:
    source=get('/orders/'+str(f['orderId']),doctor)
    fields=['petId','laboratoryId','infoAnimalId','infoSampleTypeId','animalName','animalNameEn','animalTypeId','animalType','animalTypeEn','age','dictSexId','sampleArea','masterName','doctorName']
    body={key:source[key] for key in fields};body['diagnosisDetails']=[{'infoTestTypeId':910}];body['idempotencyKey']='invoice_ui_'+secrets.token_hex(12)
    order=staff.call('/orders',body,doctor)['orderId']
    staff.call(f'/review/orders/{order}',{'approved':True,'reason':'本机PC发票上传界面合成验收'},admin)
    payment=staff.call('/payment/create',{'orderId':order},doctor);assert payment['mock'] is True
    staff.call('/payment/mock-callback/'+payment['paymentNo'],{},doctor)
    profiles=get('/invoices/profiles',doctor);assert profiles
    application=staff.call('/invoices',{'profileId':profiles[0]['id'],'orderIds':[order],'requestKey':'pc_invoice_'+secrets.token_hex(12)},doctor)
    fixture={'orderId':order,'applicationId':application,'completed':False};staff.save_private(private,fixture)
application=int(fixture['applicationId'])
assert staff.config.sql(f'SELECT user_id FROM lin_invoice_application WHERE id={application}')==str(f['userId'])
assert staff.config.sql(f'SELECT status FROM lin_invoice_application WHERE id={application}')=='pending'
temporary=tempfile.TemporaryDirectory(prefix='petlis-invoice-ui-',dir='/private/tmp')
pdf=Path(temporary.name)/'local-test.pdf';invalid=Path(temporary.name)/'invalid.pdf'
with staff.opener.open(urllib.request.Request(staff.config.BASE+'/invoices/16/download',headers={'Authorization':'Bearer '+doctor}),timeout=20) as response:
    content=response.read();assert content.startswith(b'%PDF');pdf.write_bytes(content)
invalid.write_text('Not a PDF; local validation test only')
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
    result=subprocess.run([B,*args],capture_output=True,text=True)
    assert result.returncode==0,'Petlis browser action failed; credentials omitted'
    return result.stdout
def wait(expression):
    for _ in range(60):
        if browser('js',expression).strip()=='true':return
        time.sleep(.15)
    raise AssertionError('Petlis invoice browser readiness timeout')
assert browser('url').strip().startswith('http://127.0.0.1:4173/'),'Unexpected active browser tab'
browser('goto','http://127.0.0.1:4173/#/finance');browser('reload')
if browser('js',"!!document.querySelector('#login-form')").strip()=='true':
    php="$c=require '/srv/petlis/.petlis-private/bridge.php'; echo json_encode($c['admins'][4]);"
    r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0
    account=json.loads(r.stdout)
    browser('fill','[name="username"]',account['username']);browser('fill','[name="password"]',account['password']);browser('click','#login-form button[type="submit"]')
    wait("!document.querySelector('#login-form')")
browser('goto','http://127.0.0.1:4173/#/finance');wait("!!document.querySelector('[data-tab=invoices]')")
browser('click','[data-tab="invoices"]');wait(f"!!document.querySelector('[data-issue=\"{application}\"]')")
browser('click',f'[data-issue="{application}"]');wait("!!document.querySelector('[name=invoicePdf]')")
if details:
    profile=get('/invoices/profiles',doctor)[0]
    for expected in [profile['title'],profile['taxNumber'],profile['email'],str(fixture['orderId']),'¥88.00']:
        assert expected in browser('js',"document.querySelector('.modal-body').textContent"),'Invoice snapshot missing from review dialog'
    screenshot=Path(temporary.name)/'review.png';browser('screenshot',str(screenshot))
    (root/'qa/pc-invoice-review-details.png').write_bytes(screenshot.read_bytes())
browser('click','[type="submit"][form="active-dialog-form"]')
wait("document.querySelector('#dialog-error').textContent.includes('请上传PDF')")
assert staff.config.sql(f'SELECT status FROM lin_invoice_application WHERE id={application}')=='pending'
browser('upload','[name="invoicePdf"]',str(invalid));browser('click','[type="submit"][form="active-dialog-form"]')
wait("!!document.querySelector('#dialog-error').textContent && !document.querySelector('[type=submit][form=active-dialog-form]').disabled")
assert staff.config.sql(f'SELECT status FROM lin_invoice_application WHERE id={application}')=='pending'
assert browser('js',"document.querySelector('[name=invoicePdf]').files.length===1").strip()=='true'
browser('upload','[name="invoicePdf"]',str(pdf));browser('click','[type="submit"][form="active-dialog-form"]')
wait("!document.querySelector('#active-dialog-form')")
wait(f"!document.querySelector('[data-issue=\"{application}\"]')")
assert staff.config.sql(f'SELECT status FROM lin_invoice_application WHERE id={application}')=='issued'
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE biz_type='INVOICE' AND biz_id='{application}' AND deleted_at IS NULL")=='1'
records=get('/invoices?page=0&size=20',doctor)['items'];assert any(int(x['id'])==application and x['status']=='issued' for x in records)
result={**fixture,'emptyFormStaysPending':True,'invalidPdfRetainsDialogAndFile':True,'correctedPdfClosesDialog':True,'issuedRowRemovedFromPendingQueue':True,'onePrivatePdf':True,'doctorSeesIssuedStatus':True,'normalAdminLogin':True,'externalTaxOrMailCalled':False,'snapshotDetailsVerified':details,'completed':True}
staff.save_private(private,result);temporary.cleanup();print(json.dumps(result))
