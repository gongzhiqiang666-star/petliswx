"""Issue only the current synthetic local invoice; never call an external tax/mail service."""
import importlib.util
import json
from pathlib import Path
import urllib.error
import urllib.request

spec=importlib.util.spec_from_file_location('local_staff',Path(__file__).with_name('setup-php-lab.py'))
staff=importlib.util.module_from_spec(spec)
spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis'
f=json.loads((root/'qa/native-invoice-fixture-private.json').read_text())
assert f['orderId']==23953 and f['userId']==583, 'Inspect synthetic fixture before changing scope'
invoice=int(f['applicationId'])
admin=staff.administrator(4)
owner=staff.call('/auth/password/login',{'account':f['account'],'password':f['password']})['token']
old=json.loads((root/'simulator-fixture.json').read_text())
other=staff.call('/auth/password/login',{'account':old['account'],'password':old['password']})['token']

def request(path,token=None,body=None,content_type=None):
    headers={}
    if token: headers['Authorization']='Bearer '+token
    if content_type: headers['Content-Type']=content_type
    req=urllib.request.Request(staff.config.BASE+path,data=body,headers=headers)
    try:
        with staff.opener.open(req,timeout=25) as response:
            return response.status,response.read()
    except urllib.error.HTTPError as error:
        return error.code,error.read()

def code(result):
    status,body=result
    try: return json.loads(body).get('code',status)
    except (ValueError,AttributeError): return status

assert staff.config.sql(f'SELECT COUNT(*) FROM lin_invoice_order WHERE application_id={invoice} AND order_id={f["orderId"]}')=='1'
assert code(request(f'/invoices/{invoice}/download',owner))==404
stream=b'BT /F1 14 Tf 36 760 Td (PETLIS LOCAL INVOICE TEST - NOT A TAX INVOICE) Tj ET'
objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream']
pdf=b'%PDF-1.4\n';offsets=[0]
for index,obj in enumerate(objects,1):
    offsets.append(len(pdf));pdf+=str(index).encode()+b' 0 obj\n'+obj+b'\nendobj\n'
xref=len(pdf)
pdf+=b'xref\n0 6\n0000000000 65535 f \n'+b''.join(f'{offset:010d} 00000 n \n'.encode() for offset in offsets[1:])
pdf+=b'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n'+str(xref).encode()+b'\n%%EOF\n'
boundary='PetlisLocalInvoiceQA'
body=('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="local-test.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode()+pdf+('\r\n--'+boundary+'--\r\n').encode()
content_type='multipart/form-data; boundary='+boundary
assert code(request(f'/invoices/{invoice}/file',owner,body,content_type))==403
assert code(request(f'/invoices/{invoice}/file',admin,body,content_type))==200
assert code(request(f'/invoices/{invoice}/file',admin,body,content_type))==409
status,download=request(f'/invoices/{invoice}/download',owner)
assert status==200 and download==pdf
assert code(request(f'/invoices/{invoice}/download',other))==403
assert code(request(f'/invoices/{invoice}/download')) in (401,403)
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_file WHERE biz_type='INVOICE' AND biz_id='{invoice}' AND deleted_at IS NULL")=='1'
assert staff.config.sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='INVOICE_ISSUED' AND resource_id='{invoice}'")=='1'
result={'orderId':f['orderId'],'applicationId':invoice,'orderLinkVerified':True,'pendingDownloadDenied':True,'doctorUploadDenied':True,'adminUploadSucceeded':True,'repeatUploadConflict':True,'ownerDownloadIdentical':True,'crossDoctorDenied':True,'anonymousDenied':True,'oneFileAndAudit':True,'externalTaxOrEmailCalled':False}
(root/'qa/native-invoice-file.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result))
