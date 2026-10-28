# 本机 laboklin 副本 QA 全链验收；不连接原库，不使用真实支付。
from pathlib import Path
import urllib.request, urllib.error, json, secrets, subprocess, uuid

private = Path.home() / '.local/share/petlis'
base = 'http://127.0.0.1:8082/api'
mysql = private / 'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
prefix = 'QA_REAL_' + secrets.token_hex(4).upper()
summary = {'prefix': prefix, 'steps': []}


def sql(statement):
    result = subprocess.run([str(mysql), f'--defaults-extra-file={private}/mysql/root.cnf',
                             '--batch', '--skip-column-names', 'laboklin'],
                            input=statement.encode(), capture_output=True, check=True)
    return result.stdout.decode().strip()


def raw(path, data=None, token=None, method=None):
    headers = {}
    body = None
    if data is not None:
        headers['Content-Type'] = 'application/json'
        body = json.dumps(data).encode()
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            content = response.read()
            if 'json' in response.headers.get('Content-Type', ''):
                return response.status, json.loads(content)
            return response.status, content
    except urllib.error.HTTPError as error:
        content = error.read()
        try:
            return error.code, json.loads(content)
        except Exception:
            return error.code, content


def call(path, data=None, token=None, method=None):
    status, result = raw(path, data, token, method)
    assert status == 200 and isinstance(result, dict) and result.get('code') == 200, (path, status, result)
    return result.get('data')


def upload_qualification(token):
    boundary = '----QA' + uuid.uuid4().hex
    content = b'%PDF-1.4\nQA synthetic qualification\n%%EOF'
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qa.pdf"\r\n'
            f'Content-Type: application/pdf\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
    request = urllib.request.Request(base + '/auth/qualifications', data=body, headers={
        'Content-Type': 'multipart/form-data; boundary=' + boundary,
        'Authorization': 'Bearer ' + token}, method='POST')
    with urllib.request.urlopen(request) as response:
        result = json.load(response)
    assert result['code'] == 200, result
    return result['data']['url']


def login(account, password):
    return call('/auth/password/login', {'account': account, 'password': password})['token']


admin = login('merge_admin', 'Admin123!')
qualification = upload_qualification(admin)
hospital = int(sql("SELECT h.InfoHospitalId FROM lin_lb_infohospital h WHERE h.IsActive=1 AND h.CityId>0 AND h.PayType=1 AND NOT EXISTS(SELECT 1 FROM lin_contract c WHERE c.hospital_id=h.InfoHospitalId) ORDER BY h.InfoHospitalId LIMIT 1"))
lab_id = 1
accounts = {}
phones = []

try:
    for role in ['doctor', 'courier', 'lab']:
        account = prefix + '_' + role
        password = secrets.token_hex(18) + 'A7'
        phone = '199' + str(secrets.randbelow(10 ** 8)).zfill(8)
        phones.append(phone)
        code = str(secrets.randbelow(900000) + 100000)
        sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{phone}','{code}',0,'127.0.0.1',UNIX_TIMESTAMP())")
        user_id = call('/auth/register', {'username': account, 'password': password, 'mobile': phone,
                                          'code': code, 'hospitalId': hospital, 'nickname': prefix + role,
                                          'title': 'QA', 'qualificationFiles': [qualification]})
        call(f'/review/registrations/{user_id}', {'approved': True, 'reason': prefix + '注册审核'}, admin)
        role_body = {'roleCode': {'doctor': 'DOCTOR', 'courier': 'COURIER', 'lab': 'LAB_STAFF'}[role]}
        if role == 'lab':
            role_body['laboratoryId'] = lab_id
        else:
            role_body['hospitalId'] = hospital
        call(f'/admin/users/{user_id}/roles', role_body, admin)
        accounts[role] = {'id': user_id, 'token': login(account, password)}
    summary['users'] = {role: value['id'] for role, value in accounts.items()}
    doctor = accounts['doctor']['token']
    courier = accounts['courier']['token']
    lab = accounts['lab']['token']

    animals = call('/dictionary/animals', token=doctor)
    assert any(x['infoAnimalId'] == 6 for x in animals) and any(x['infoAnimalId'] == 7 for x in animals)
    assert all(x.get('isActive') == 1 for x in animals)
    summary['steps'].append('猫犬字典仅返回启用项')

    item_id = call(f'/admin/catalog/laboratories/{lab_id}/items',
                   {'name': prefix + '检测项目', 'code': prefix + '_ITEM', 'price': 88}, admin)
    assert int(sql(f"SELECT COUNT(*) FROM lin_lb_infotesttype WHERE TestNO='{prefix}_ITEM'")) == 1
    summary['testTypeId'] = item_id
    summary['steps'].append('AdminCatalog新增项目且仅插入一次')

    varieties = call('/dictionary/varieties?animalId=6', token=doctor)
    variety = varieties[0]
    variety_id = variety.get('iid') or variety.get('infoVarietyId')
    sexes = call('/dictionary/sexes', token=doctor)
    samples = call('/dictionary/sample-types', token=doctor)
    pet = call('/pets', {'animalName': prefix + '猫', 'animalNameEn': prefix + 'Cat', 'infoAnimalId': 6,
                         'infoVarietyId': variety_id, 'dictSexId': sexes[0]['dictSexId'],
                         'ownerName': prefix + '畜主'}, doctor)
    pet_id = pet.get('id') or pet.get('petId')
    summary['petId'] = pet_id
    summary['steps'].append('使用猫id6创建档案')

    order = call('/orders', {'petId': pet_id, 'laboratoryId': lab_id, 'infoAnimalId': 6,
                             'infoSampleTypeId': samples[0]['infoSampleTypeId'],
                             'animalName': prefix + '猫', 'animalNameEn': prefix + 'Cat',
                             'animalTypeId': variety_id,
                             'animalType': variety.get('varietyCn') or variety.get('varietyName') or '猫品种',
                             'animalTypeEn': variety.get('varietyEn') or variety.get('varietyNameEn') or 'Cat',
                             'age': '3Y', 'dictSexId': sexes[0]['dictSexId'], 'sampleArea': 'QA前肢静脉',
                             'masterName': prefix + '畜主', 'doctorName': prefix + '医生',
                             'diagnosisDetails': [{'infoTestTypeId': item_id}]}, doctor)
    order_id = order['orderId']
    assert int(sql(f'SELECT COUNT(*) FROM lin_order_pricing_snapshot WHERE order_id={order_id} AND contract_id IS NOT NULL')) == 0
    summary['orderId'] = order_id
    summary['steps'].append('只中文畜主下单且无合同定价快照')

    call(f'/review/orders/{order_id}', {'approved': True, 'reason': prefix + '旧月结审核'}, admin)
    detail = call(f'/orders/{order_id}', token=doctor)
    assert detail['sampleStatus'] == 2
    summary['userNO'] = detail['userNO']
    summary['steps'].append('旧月结无合同审核兼容挂账')

    shipment_id = call('/logistics', {'orderId': order_id, 'trackingNo': prefix + '_SHIP',
                                      'laboratoryId': lab_id, 'pickupAddress': 'QA测试地址',
                                      'courierUserId': accounts['courier']['id']}, admin)
    for action in ['pickup', 'transit', 'deliver']:
        call(f'/logistics/{shipment_id}/{action}', {}, courier)
    call(f'/logistics/{shipment_id}/scan-receive', {'barcode': detail['userNO']}, lab)
    summary['shipmentId'] = shipment_id
    summary['steps'].append('物流揽收运输送达扫码签收')

    assignees = call(f'/lab/tasks/assignees?labId={lab_id}', token=lab)
    assert any(x['id'] == accounts['lab']['id'] and x.get('username') for x in assignees['items'])
    task = call(f'/lab/tasks/order/{order_id}/assign',
                {'assigneeId': accounts['lab']['id'], 'equipmentCode': prefix + '_PCR'}, lab)
    results = [{'orderItemId': item['itemId'], 'name': 'QA指标', 'value': '5.20',
                'unit': '10^9/L', 'referenceRange': '3.5-5.5'} for item in detail['items']]
    call(f"/lab/tasks/{task['id']}/results", {'results': results}, lab)
    summary['taskId'] = task['id']
    summary['steps'].append('实验人员列表任务分派与结果录入')

    workflows = call('/report-workflows', {'taskId': task['id'], 'summary': prefix + '摘要',
                                           'conclusion': prefix + '结论'}, lab)
    assert len(workflows) == 1
    workflow_id = workflows[0]
    call(f'/report-workflows/{workflow_id}/review', {'decision': 'APPROVE', 'reason': prefix + '报告审核'}, admin)
    published = call(f'/report-workflows/{workflow_id}/publish', {}, admin)
    assert published['status'] == 'PUBLISHED'
    report_id = published['detail_id']
    summary['workflowId'] = workflow_id
    summary['reportId'] = report_id
    summary['steps'].append('报告生成审核发布并返回PUBLISHED')

    reports = call('/reports?pageNum=0&pageSize=100', token=doctor)
    assert any(x['reportId'] == report_id for x in reports['content'])
    report_detail = call(f'/reports/{report_id}', token=doctor)
    assert report_detail['pdfHttpUrl'] == f'/api/pdf/view/{report_id}'
    pdf_url = call(f'/reports/{report_id}/pdf-url', token=doctor)
    assert f'/api/pdf/view/{report_id}' in pdf_url
    for path in [f'/reports/{report_id}/download', f'/pdf/view/{report_id}',
                 f'/report-workflows/{workflow_id}/download']:
        status, content = raw(path, token=doctor)
        assert status == 200 and content.startswith(b'%PDF'), (path, status)
    status, _ = raw(f'/pdf/view/{report_id}')
    assert status in [401, 403]
    summary['steps'].append('报告列表详情pdf-url/download/pdf-view鉴权与PDF字节')

    print(json.dumps(summary, ensure_ascii=False, indent=2))
finally:
    for phone in phones:
        sql(f"DELETE FROM lin_sms WHERE mobile='{phone}'")
