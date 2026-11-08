# 本机 laboklin 副本 QA 全链验收；不连接原库，不使用真实支付。
from pathlib import Path
import urllib.request, urllib.error, json, secrets, subprocess, uuid, sys, hashlib
from concurrent.futures import ThreadPoolExecutor
if '--php-unassigned' in sys.argv and '--php-retest' not in sys.argv:
    raise SystemExit('--php-unassigned requires --php-retest')
if '--php-retest' in sys.argv and '--php-results' not in sys.argv:
    raise SystemExit('--php-retest requires --php-results')
if '--php-results' in sys.argv and '--php-lab' not in sys.argv:
    raise SystemExit('--php-results requires --php-lab')
if '--template-snapshot' in sys.argv and '--php-results' in sys.argv:
    raise SystemExit('--template-snapshot uses native generation; run separately from --php-results')
if '--shipment-resolutions' in sys.argv and '--shipment-exceptions' not in sys.argv:
    raise SystemExit('--shipment-resolutions requires --shipment-exceptions')
if '--use-local-lab' in sys.argv and ('--php-lab' not in sys.argv or '--prepare-php-ui' in sys.argv or '--lab-reassign' in sys.argv):
    raise SystemExit('--use-local-lab requires --php-lab and excludes temporary UI/reassignment mapping modes')
if '--coupon-cash' in sys.argv and '--coupon-lifecycle' not in sys.argv:
    raise SystemExit('--coupon-cash requires --coupon-lifecycle')
sf_cancel = "--sf-cancel" in sys.argv
sf_label = "--sf-label" in sys.argv
if "--sf-cancel-recovery" in sys.argv and not sf_cancel:
    raise SystemExit("--sf-cancel-recovery requires --sf-cancel")
sf_sandbox = "--sf-sandbox" in sys.argv or sf_cancel or sf_label

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


def upload_qualification(token=None):
    boundary = '----QA' + uuid.uuid4().hex
    content = b'%PDF-1.4\nQA synthetic qualification\n%%EOF'
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="qa.pdf"\r\n'
            f'Content-Type: application/pdf\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
    request = urllib.request.Request(base + '/auth/qualifications', data=body, headers={
        'Content-Type': 'multipart/form-data; boundary=' + boundary,
        **({'Authorization': 'Bearer ' + token} if token else {})}, method='POST')
    with urllib.request.urlopen(request) as response:
        result = json.load(response)
    assert result['code'] == 200, result
    return result['data']['url'] if token else result['data']


def login(account, password):
    return call('/auth/password/login', {'account': account, 'password': password})['token']


admin = login('merge_admin', 'Admin123!')
hospital = int(sql("SELECT h.InfoHospitalId FROM lin_lb_infohospital h WHERE h.IsActive=1 AND h.CityId>0 AND h.PayType=1 AND NOT EXISTS(SELECT 1 FROM lin_contract c WHERE c.hospital_id=h.InfoHospitalId) ORDER BY h.InfoHospitalId LIMIT 1"))
if '--credit-concurrency' in sys.argv:
    source_hospital = hospital
    hospital = int(sql(f"INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,IsActive,PayType,CityId) SELECT '{prefix}月结并发医院','{prefix} Monthly QA',1,1,CityId FROM lin_lb_infohospital WHERE InfoHospitalId={source_hospital}; SELECT LAST_INSERT_ID();"))
if '--exception-refund' in sys.argv or '--coupon-cash' in sys.argv or '--prepare-native-invoice' in sys.argv:
    source_hospital = hospital
    hospital = int(sql(f"INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,IsActive,PayType,CityId) SELECT '{prefix}现结合成医院','{prefix} Cash QA',1,0,CityId FROM lin_lb_infohospital WHERE InfoHospitalId={source_hospital}; SELECT LAST_INSERT_ID();"))
lab_id = 1
accounts = {}
phones = []
foreign_lab_id = None

try:
    if '--lab-reassign' in sys.argv:
        foreign_lab_id = call('/laboratories', {'code':prefix+'_FOREIGN_LAB','name':prefix+'隔离实验室','description':'仅本机跨实验室权限测试'}, admin)
    for role in ['doctor', 'courier', 'lab'] + (['peer'] if '--account-inbox' in sys.argv or '--profile-review' in sys.argv or '--points-ledger' in sys.argv or '--invoice-file' in sys.argv or '--admin-lifecycle' in sys.argv or '--doctor-resubmit' in sys.argv or '--mall-concurrency' in sys.argv or '--wallet-isolation' in sys.argv or '--coupon-lifecycle' in sys.argv else []) + (['reg_pending', 'reg_rejected'] if '--registration-summary' in sys.argv else []) + (['relief_lab','foreign_lab'] if '--lab-reassign' in sys.argv else []):
        if role == 'lab' and '--use-local-lab' in sys.argv:
            import importlib.util
            spec = importlib.util.spec_from_file_location('local_lab_setup', Path(__file__).with_name('setup-php-lab.py'))
            local_setup = importlib.util.module_from_spec(spec); spec.loader.exec_module(local_setup)
            checked = local_setup.setup(lab_id, 4, validate_only=True)
            saved = local_setup.config.read_private(Path(checked['credentialFile']))
            accounts[role] = {'id': saved['userId'], 'username': saved['username'], 'password': saved['password'], 'token': login(saved['username'], saved['password'])}
            summary['permanentLabIdentity'] = {'userId': saved['userId'], 'laboratoryId': lab_id, 'mappingUnchanged': False}
            continue
        account = prefix + '_' + role
        password = secrets.token_hex(18) + 'A7'
        phone = '199' + str(secrets.randbelow(10 ** 8)).zfill(8)
        phones.append(phone)
        code = str(secrets.randbelow(900000) + 100000)
        sql(f"INSERT INTO lin_sms(event,mobile,code,times,ip,createtime) VALUES('petlis_register','{phone}','{code}',0,'127.0.0.1',UNIX_TIMESTAMP())")
        uploaded = upload_qualification()
        qualification = uploaded['url']
        user_id = call('/auth/register', {'username': account, 'password': password, 'mobile': phone,
                                          'code': code, 'hospitalId': hospital, 'nickname': prefix + role,
                                          'title': 'QA', 'qualificationFiles': [qualification], 'qualificationTokens': {qualification: uploaded['uploadToken']}})
        if role in ['reg_pending', 'reg_rejected']:
            if role == 'reg_rejected':
                call(f'/review/registrations/{user_id}', {'approved': False, 'reason': prefix + '统计驳回测试'}, admin)
            accounts[role] = {'id': user_id, 'username': account, 'password': password}
            continue
        call(f'/review/registrations/{user_id}', {'approved': True, 'reason': prefix + '注册审核'}, admin)
        role_body = {'roleCode': {'doctor': 'DOCTOR', 'courier': 'COURIER', 'lab': 'LAB_STAFF', 'peer': 'DOCTOR','relief_lab':'LAB_STAFF','foreign_lab':'LAB_STAFF'}[role]}
        if role in ['lab','relief_lab','foreign_lab']:
            role_body['laboratoryId'] = foreign_lab_id if role=='foreign_lab' else lab_id
        else:
            role_body['hospitalId'] = hospital
        call(f'/admin/users/{user_id}/roles', role_body, admin)
        accounts[role] = {'id': user_id, 'token': login(account, password), 'username': account, 'password': password}
    summary['users'] = {role: value['id'] for role, value in accounts.items()}
    if '--registration-summary' in sys.argv:
        from datetime import date, timedelta
        start = date.today().isoformat()
        end = (date.today() + timedelta(days=1)).isoformat()
        path = f'/admin/dashboard/registrations?from={start}&to={end}'
        data = call(path, token=admin)
        expected = [int(x) for x in sql(f"SELECT COUNT(*),COALESCE(SUM(review_status='pending'),0),COALESCE(SUM(review_status='approved'),0),COALESCE(SUM(review_status='rejected'),0) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL AND created_at>='{start}' AND created_at<'{end}'").split('\t')]
        assert [int(data[key]) for key in ['submittedCount','pendingCount','approvedCount','rejectedCount']] == expected
        assert int(data['pendingCount']) >= 1 and int(data['rejectedCount']) >= 1
        assert int(data['totalPending']) == int(sql("SELECT COUNT(*) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL AND review_status='pending'"))
        for token in [None, accounts['doctor']['token'], accounts['lab']['token']]:
            status, denied = raw(path, token=token)
            assert status in [401,403] or denied.get('code') in [401,403]
        status, invalid = raw('/admin/dashboard/registrations?from=2026-02-30&to=2026-03-01', token=admin)
        assert status == 400 or invalid.get('code') == 400
        summary['registrationSummary'] = {'matchesDatabase': True, 'pendingAndRejectedCovered': True, 'nonAdminDenied': True, 'invalidCalendarDenied': True, 'counts': data}
        summary['steps'].append('注册申请提交期间/当前状态及全部待审与数据库一致，匿名医生实验室拒绝，非法日历拒绝')
    doctor = accounts['doctor']['token']
    courier = accounts['courier']['token']
    lab = accounts['lab']['token']

    if '--wallet-isolation' in sys.argv:
        uid = accounts['doctor']['id']
        assert sql(f'SELECT openid IS NULL OR openid=\'\' FROM lin_user WHERE id={uid}') == '1'
        before_balance = sql(f'SELECT money FROM lin_user WHERE id={uid}')
        key = 'recharge_' + uuid.uuid4().hex
        body = {'amount':'12.34','requestKey':key}
        with ThreadPoolExecutor(max_workers=4) as workers:
            created = list(workers.map(lambda _: call('/wallet/recharges',body,doctor),range(4)))
        payment_no = created[0]['paymentNo']
        assert all(row['mock'] is True and row['paymentNo']==payment_no for row in created)
        assert sql(f'SELECT money FROM lin_user WHERE id={uid}') == before_balance
        assert sql(f'SELECT COUNT(*) FROM lin_user_money_log WHERE user_id={uid}') == '0'
        assert sql(f'SELECT COUNT(*) FROM lin_recharge_order WHERE user_id={uid}') == '1'
        status, denied = raw('/wallet/recharges',{**body,'amount':'12.35'},doctor)
        assert denied.get('code',status) == 409
        peer = accounts['peer']['token']
        for suffix in ['', '/resume','/close']:
            status, denied = raw('/wallet/recharges/'+payment_no+suffix,None if not suffix else {},peer)
            assert denied.get('code',status) == 404
        assert call('/wallet/recharges',token=peer)['total'] == 0
        assert call('/wallet/recharges/'+payment_no,token=doctor)['status'] == 'pending'
        for invalid in ['0.99','50000.01','1.001','-1']:
            status, denied = raw('/wallet/recharges',{'amount':invalid,'requestKey':'invalid_'+uuid.uuid4().hex},doctor)
            assert denied.get('code',status) != 200
        with ThreadPoolExecutor(max_workers=4) as workers:
            closed = list(workers.map(lambda _: raw('/wallet/recharges/'+payment_no+'/close',{},doctor),range(4)))
        assert all(row.get('code',status)==200 for status,row in closed)
        assert call('/wallet/recharges/'+payment_no,token=doctor)['status'] == 'closed'
        status, denied = raw('/wallet/recharges/'+payment_no+'/resume',{},doctor)
        assert denied.get('code',status) != 200
        for _ in range(3):
            row=call('/wallet/recharges',{'amount':'1.00','requestKey':'pending_'+uuid.uuid4().hex},doctor)
            assert row['mock'] is True
        status, denied = raw('/wallet/recharges',{'amount':'1.00','requestKey':'overflow_'+uuid.uuid4().hex},doctor)
        assert denied.get('code',status) != 200
        assert sql(f"SELECT COUNT(*) FROM lin_recharge_order WHERE user_id={uid} AND status='pending'") == '3'
        pages = [call(f'/wallet/recharges?page={i}&size=2',token=doctor) for i in range(2)]
        numbers=[row['paymentNo'] for page in pages for row in page['items']]
        assert len(numbers)==4 and len(set(numbers))==4
        for number in numbers:
            if number != payment_no: call('/wallet/recharges/'+number+'/close',{},doctor)
        assert sql(f'SELECT money FROM lin_user WHERE id={uid}') == before_balance
        assert call('/account/wallet/transactions',token=doctor)['total'] == 0
        summary.update(paymentNo=payment_no,createConcurrency=4,oneRechargePerRequest=True,changedAmountDenied=True,otherDoctorStatusResumeCloseDenied=True,invalidAmountsDenied=True,concurrentCloseIdempotent=True,closedCannotResume=True,pendingLimitEnforced=True,paginationNoDuplicates=True,noPrepaymentLedger=True,noRealPayment=True)
        summary['steps'].append('充值四并发同请求仅一笔、金额冲突、跨医生查询续付关闭拒绝、金额边界、关闭幂等、待支付上限及分页、预支付不入账')
        print(json.dumps(summary,ensure_ascii=False,indent=2))
        sys.exit(0)

    if '--mall-concurrency' in sys.argv:
        gift_id = call('/mall/gifts', {'name': prefix + '最后一件礼品', 'description':'本机合成并发兑换测试', 'pointsCost':100,'stock':1}, admin)
        candidates = [accounts['doctor'],accounts['peer']]
        for account in candidates:
            call('/admin/operations/points-adjustments', {'userId':account['id'],'delta':200,'reason':prefix+'兑换测试积分','requestKey':'award_'+uuid.uuid4().hex}, admin)
        address = {'recipient':'合成收件人','phone':'13800000000','address':'本机测试地址，不实际寄送'}
        requests = [{'giftId':gift_id,'quantity':1,'deliveryAddress':address,'requestKey':'mall_'+uuid.uuid4().hex} for _ in candidates]
        with ThreadPoolExecutor(max_workers=2) as workers:
            responses = list(workers.map(lambda pair: raw('/mall/redemptions',pair[1],pair[0]['token']),zip(candidates,requests)))
        successes = [i for i,(status,body) in enumerate(responses) if body.get('code',status)==200]
        assert len(successes)==1, responses
        winner_index = successes[0]
        winner = candidates[winner_index]
        winner_body = requests[winner_index]
        redemption_id = responses[winner_index][1]['data']
        assert int(sql(f'SELECT stock FROM lin_gift WHERE id={gift_id}')) == 0
        assert int(sql(f'SELECT COUNT(*) FROM lin_redemption WHERE gift_id={gift_id}')) == 1
        assert sorted(int(sql(f'SELECT score FROM lin_user WHERE id={account["id"]}')) for account in candidates) == [100,200]
        assert call('/mall/redemptions', winner_body, winner['token']) == redemption_id
        status, denied = raw('/mall/redemptions', {**winner_body,'deliveryAddress':{**address,'recipient':'换收件人'}}, winner['token'])
        assert denied.get('code',status)==409
        status, denied = raw(f'/mall/redemptions/{redemption_id}/cancel', {'reason':'越权取消'}, winner['token'])
        assert denied.get('code',status)==403
        with ThreadPoolExecutor(max_workers=4) as workers:
            cancellations = list(workers.map(lambda _: raw(f'/mall/redemptions/{redemption_id}/cancel',{'reason':prefix+'取消合成兑换'},admin),range(4)))
        assert all(body.get('code',status)==200 for status,body in cancellations), cancellations
        assert int(sql(f'SELECT stock FROM lin_gift WHERE id={gift_id}')) == 1
        assert all(int(sql(f'SELECT score FROM lin_user WHERE id={account["id"]}'))==200 for account in candidates)
        assert int(sql(f"SELECT COUNT(*) FROM lin_user_score_log WHERE user_id={winner['id']} AND memo='取消礼品兑换 #{redemption_id}'")) == 1
        assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE action='REDEMPTION_CANCELLED' AND resource_id='{redemption_id}'")) == 1
        assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={winner['id']} AND business_key='redemption:cancelled:{redemption_id}'")) == 1
        poor_request = {**winner_body,'requestKey':'poor_'+uuid.uuid4().hex}
        status, denied = raw('/mall/redemptions',poor_request,courier)
        assert denied.get('code',status)!=200
        assert int(sql(f'SELECT stock FROM lin_gift WHERE id={gift_id}')) == 1
        assert int(sql(f'SELECT score FROM lin_user WHERE id={accounts["courier"]["id"]}')) == 0
        assert int(sql(f'SELECT COUNT(*) FROM lin_redemption WHERE gift_id={gift_id}')) == 1
        assert call('/mall/redemptions',winner_body,winner['token']) == redemption_id
        assert sql(f'SELECT status FROM lin_redemption WHERE id={redemption_id}') == 'cancelled'
        history = call('/mall/redemptions?page=0&size=1',token=winner['token'])
        assert history['total']==1 and history['items'][0]['id']==redemption_id
        outsider_history = call('/mall/redemptions?page=0&size=1',token=courier)
        assert outsider_history['total']==0
        summary.update(giftId=gift_id,redemptionId=redemption_id,lastStockWinnerCount=1,concurrentCancellationCount=4,oneRefundLedger=True,oneRefundAudit=True,oneRefundNotification=True,changedAddressReplayDenied=True,insufficientPointsRollback=True,cancelledReplayNoNewDebit=True,historyOwnership=True)
        summary['steps'].append('最后一件库存双人竞争仅一人成功、原请求幂等、换地址冲突、四并发取消仅退一次、积分不足回滚及历史隔离')
        print(json.dumps(summary,ensure_ascii=False,indent=2))
        sys.exit(0)

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

    order_body = {'petId': pet_id, 'laboratoryId': lab_id, 'infoAnimalId': 6,
                             'infoSampleTypeId': samples[0]['infoSampleTypeId'],
                             'animalName': prefix + '猫', 'animalNameEn': prefix + 'Cat',
                             'animalTypeId': variety_id,
                             'animalType': variety.get('varietyCn') or variety.get('varietyName') or '猫品种',
                             'animalTypeEn': variety.get('varietyEn') or variety.get('varietyNameEn') or 'Cat',
                             'age': '3Y', 'dictSexId': sexes[0]['dictSexId'], 'sampleArea': 'QA前肢静脉',
                             'masterName': prefix + '畜主', 'doctorName': prefix + '医生',
                             'diagnosisDetails': [{'infoTestTypeId': item_id}]}
    if '--doctor-resubmit' in sys.argv:
        order_body.update({'medicalHistory':prefix+'病史','clinicalSymptoms':prefix+'症状','diagnosis':prefix+'诊断','medication':prefix+'用药'})
    if '--prepare-native-invoice' in sys.argv:
        import os
        target=private/'qa/native-invoice-fixture-private.json'
        assert not target.exists(),'Use the previous native invoice fixture before preparing another'
        invoice_order=call('/orders',order_body,doctor)['orderId']
        call(f'/review/orders/{invoice_order}',{'approved':True,'reason':prefix+'原生发票现结准备'},admin)
        sql(f"UPDATE lin_user SET openid='qa_{uuid.uuid4().hex}' WHERE id={accounts['doctor']['id']} AND username='{accounts['doctor']['username']}'")
        payment=call('/payment/create',{'orderId':invoice_order},doctor);assert payment['mock'] is True
        call('/payment/mock-callback/'+payment['paymentNo'],{},doctor)
        assert sql(f'SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={invoice_order}')=='1'
        fixture={'account':accounts['doctor']['username'],'password':accounts['doctor']['password'],'userId':accounts['doctor']['id'],'hospitalId':hospital,'orderId':invoice_order,'completed':False}
        fd=os.open(target,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
        with os.fdopen(fd,'w') as output:json.dump(fixture,output)
        summary['orderId']=invoice_order;summary['steps'].append('独立合成现结医院/医生，正常审核及mock支付，准备原生发票申请')
        print(json.dumps(summary,ensure_ascii=False,indent=2));sys.exit(0)
    if '--coupon-lifecycle' in sys.argv:
        from datetime import datetime,timedelta
        from decimal import Decimal
        now=datetime.now();templates=[]
        base_rule={'name':prefix+'抵扣边界券','amount':10,'minimumAmount':88,'laboratoryId':lab_id,'startsAt':(now-timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+timedelta(days=1)).isoformat(timespec='seconds')}
        try:
            cid=call('/admin/coupons',base_rule,admin);templates.append(cid)
            def rule(**updates):
                command={**base_rule,**updates}
                call(f'/admin/coupons/{cid}?totalQuantity=2',command,admin,method='PUT')
            rule()
            uc=call(f'/admin/coupons/{cid}/users/{accounts["doctor"]["id"]}',{'requestKey':'coupon_lifecycle_'+uuid.uuid4().hex},admin)
            quote_body={'laboratoryId':lab_id,'testTypeIds':[item_id],'userCouponId':uc}
            exact=call('/coupons/quote',quote_body,doctor)
            assert [Decimal(str(exact[k])) for k in ['originalAmount','discountAmount','payableAmount']]==[Decimal('88'),Decimal('10'),Decimal('78')]
            status,denied=raw('/coupons/quote',quote_body,accounts['peer']['token']);assert denied.get('code',status)!=200
            for changed,category in [({'minimumAmount':Decimal('88.01')},'available'),({'laboratoryId':2},'available'),({'startsAt':(now+timedelta(days=1)).isoformat(timespec='seconds'),'expiresAt':(now+timedelta(days=2)).isoformat(timespec='seconds')},'upcoming'),({'startsAt':(now-timedelta(days=2)).isoformat(timespec='seconds'),'expiresAt':(now-timedelta(minutes=1)).isoformat(timespec='seconds')},'expired')]:
                # Decimal is represented as exact JSON text by a string accepted by the BigDecimal DTO.
                rule(**{k:str(v) if isinstance(v,Decimal) else v for k,v in changed.items()})
                status,denied=raw('/coupons/quote',quote_body,doctor);assert denied.get('code',status)!=200
                assert any(int(row['id'])==uc for row in call('/coupons?category='+category,token=doctor))
            rule(amount=100)
            capped=call('/coupons/quote',quote_body,doctor)
            assert Decimal(str(capped['discountAmount']))==Decimal('88') and Decimal(str(capped['payableAmount']))==0
            rule()
            with ThreadPoolExecutor(max_workers=4) as workers:
                replies=list(workers.map(lambda _:raw('/orders',{**order_body,'userCouponId':uc,'idempotencyKey':'coupon_order_'+uuid.uuid4().hex},doctor),range(4)))
            winners=[body['data']['orderId'] for status,body in replies if body.get('code',status)==200];assert len(winners)==1,replies
            assert all(body.get('code',status) in [200,400,409] for status,body in replies),replies
            winner=winners[0]
            assert sql(f'SELECT COUNT(*) FROM lin_order_extension WHERE user_coupon_id={uc}')=='1'
            assert sql(f"SELECT status FROM lin_user_coupon WHERE id={uc}")=='reserved'
            call(f'/orders/{winner}/status?status=4',{},doctor,method='PUT')
            assert sql(f"SELECT status,order_id IS NULL FROM lin_user_coupon WHERE id={uc}")=='available\t1'
            retry=call('/orders',{**order_body,'userCouponId':uc,'idempotencyKey':'coupon_retry_'+uuid.uuid4().hex},doctor)['orderId']
            call(f'/review/orders/{retry}',{'approved':False,'reason':prefix+'优惠券释放合成验证'},admin)
            assert sql(f"SELECT status,order_id IS NULL FROM lin_user_coupon WHERE id={uc}")=='available\t1'
            call(f'/orders/{retry}/resubmit',{**order_body,'userCouponId':uc},doctor)
            assert sql(f"SELECT status,order_id FROM lin_user_coupon WHERE id={uc}")==f'reserved\t{retry}'
            call(f'/review/orders/{retry}',{'approved':True,'reason':prefix+'月结消费合成验证'},admin)
            settlement=call(f'/orders/{retry}',token=doctor)
            cash_callbacks=False
            if settlement['sampleStatus']==-3:
                assert sql(f"SELECT status FROM lin_user_coupon WHERE id={uc}")=='reserved'
                sql(f"UPDATE lin_user SET openid='qa_{uuid.uuid4().hex}' WHERE id={accounts['doctor']['id']} AND username='{accounts['doctor']['username']}'")
                payment=call('/payment/create',{'orderId':retry},doctor);assert payment['mock'] is True
                assert sql(f"SELECT status FROM lin_user_coupon WHERE id={uc}")=='reserved'
                with ThreadPoolExecutor(max_workers=4) as workers:
                    callbacks=list(workers.map(lambda _:raw('/payment/mock-callback/'+payment['paymentNo'],{},doctor),range(4)))
                assert all(body.get('code',status)==200 for status,body in callbacks)
                assert sql(f"SELECT COUNT(*) FROM lin_wechat_payment WHERE order_id={retry} AND status=1")=='1'
                cash_callbacks=True
            else:assert settlement['sampleStatus']==2
            assert sql(f"SELECT status,order_id,used_at IS NOT NULL FROM lin_user_coupon WHERE id={uc}")==f'used\t{retry}\t1'
            paid=call(f'/orders/{retry}',token=doctor)
            assert Decimal(str(paid['testFee']))==Decimal('78')
            status,denied=raw('/coupons/quote',quote_body,doctor);assert denied.get('code',status)!=200
            assert any(int(row['id'])==uc for row in call('/coupons?category=used',token=doctor))
            summary['couponLifecycle']={'couponId':cid,'userCouponId':uc,'cancelledOrderId':winner,'resubmittedOrderId':retry,'exactMinimumPrice':'78.00','crossDoctorDenied':True,'belowThresholdAndWrongLabDenied':True,'upcomingAndExpiredDeniedAndClassified':True,'fourConcurrentOrdersSingleReservation':True,'cancelAndRejectionRelease':True,'resubmitReservesSameCoupon':True,'discountCappedAtOrderAmount':True,'monthlyApprovalConsumesOnce':not cash_callbacks,'cashKeepsReservedUntilMockPayment':cash_callbacks,'fourMockCallbacksOnePaymentAndConsumption':cash_callbacks,'usedCannotBeQuotedAgain':True}
            summary['steps'].append('券归属/门槛/实验室/前后有效期、同券四单并发仅一保留、取消驳回释放、原单重提再保留及月结审核或现结mock回调消费')
            print(json.dumps(summary,ensure_ascii=False,indent=2));sys.exit(0)
        finally:
            for cid in templates:call(f'/admin/coupons/{cid}',{},admin,method='DELETE')
    if '--exception-refund' in sys.argv:
        cash_order=call('/orders',order_body,doctor)['orderId']
        refund_body={'reason':prefix+'标本丢失申请退款','requestKey':'refund_'+uuid.uuid4().hex}
        status,denied=raw(f'/admin/refunds/{cash_order}',refund_body,admin)
        assert denied.get('code',status)!=200
        call(f'/review/orders/{cash_order}',{'approved':True,'reason':prefix+'现结审核'},admin)
        assert call(f'/orders/{cash_order}',token=doctor)['sampleStatus']==-3
        sql(f"UPDATE lin_user SET openid='qa_{uuid.uuid4().hex}' WHERE id={accounts['doctor']['id']} AND username='{accounts['doctor']['username']}'")
        payment=call('/payment/create',{'orderId':cash_order},doctor)
        assert payment['mock'] is True
        call('/payment/mock-callback/'+payment['paymentNo'],{},doctor)
        sid=call('/logistics',{'orderId':cash_order,'trackingNo':prefix+'_REFUND','laboratoryId':lab_id,'pickupAddress':'QA测试地址','courierUserId':accounts['courier']['id']},admin)
        call(f'/logistics/{sid}/pickup',{},courier)
        call(f'/logistics/{sid}/exception',{'exceptionType':'LOSS','note':prefix+'合成丢失异常','requestKey':'exception_'+uuid.uuid4().hex},courier)
        status,denied=raw(f'/admin/refunds/{cash_order}',refund_body,doctor)
        assert denied.get('code',status)==403
        with ThreadPoolExecutor(max_workers=4) as workers:
            refunds=list(workers.map(lambda _:raw(f'/admin/refunds/{cash_order}',refund_body,admin),range(4)))
        codes=[body.get('code',status) for status,body in refunds]
        summary.update(orderId=cash_order,shipmentId=sid,paymentNo=payment['paymentNo'],concurrentRefundCodes=codes)
        assert all(code==200 for code in codes),summary
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND resource_id='{cash_order}'")=='1'
        assert sql(f"SELECT status FROM lin_wechat_payment WHERE payment_no='{payment['paymentNo']}'")=='3'
        assert sql(f'SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={cash_order}')=='0'
        detail=call(f'/orders/{cash_order}',token=doctor)
        summary['doctorAfterRefund']={'sampleStatus':detail['sampleStatus'],'refunded':detail.get('refunded'),'refundReason':detail.get('refundReason')}
        from decimal import Decimal
        assert detail['refunded'] is True and detail['refundMock'] is True and Decimal(str(detail['refundAmount']))==Decimal('88') and detail['refundReason']==refund_body['reason']
        doctor_orders=call('/orders?size=100',token=doctor)
        assert next(row for row in doctor_orders['content'] if row['orderId']==cash_order)['refunded'] is True
        admin_orders=call('/admin/orders?size=100',token=admin)
        assert next(row for row in admin_orders['items'] if row['orderId']==cash_order)['refunded']
        assert sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']} AND business_key='refund:{cash_order}'")=='1'
        def new_paid_cash():
            oid=call('/orders',order_body,doctor)['orderId']
            call(f'/review/orders/{oid}',{'approved':True,'reason':prefix+'发票退款边界'},admin)
            pay=call('/payment/create',{'orderId':oid},doctor)
            assert pay['mock'] is True
            call('/payment/mock-callback/'+pay['paymentNo'],{},doctor)
            return oid
        invoice_order=new_paid_cash()
        invoice_profile=call('/invoices/profiles',{'title':prefix+'合成开票抬头','email':'qa@example.test'},doctor)
        invoice=call('/invoices',{'profileId':invoice_profile,'orderIds':[invoice_order],'requestKey':'invoice_'+uuid.uuid4().hex},doctor)
        for issued in [False,True]:
            if issued: call(f'/invoices/{invoice}/issue',{'invoiceUrl':'https://example.test/petlis-synthetic-invoice.pdf'},admin)
            status,denied=raw(f'/admin/refunds/{invoice_order}',{'reason':'开票订单禁止直接退款','requestKey':'refund_'+uuid.uuid4().hex},admin)
            assert denied.get('code',status)!=200
            assert sql(f'SELECT IsPayMoney FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={invoice_order}')=='1'
            assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MOCK_REFUND' AND resource_id='{invoice_order}'")=='0'
        php_order=new_paid_cash()
        paid_detail=call(f'/orders/{php_order}',token=doctor)
        paid_ship=call('/logistics',{'orderId':php_order,'trackingNo':prefix+'_PAID_REPORT','laboratoryId':lab_id,'pickupAddress':'QA测试地址','courierUserId':accounts['courier']['id']},admin)
        for step in ['pickup','transit','deliver']:call(f'/logistics/{paid_ship}/{step}',{},courier)
        call(f'/logistics/{paid_ship}/scan-receive',{'barcode':paid_detail['userNO']},lab)
        paid_task=call(f'/lab/tasks/order/{php_order}/assign',{'assigneeId':accounts['lab']['id'],'equipmentCode':'QA_REFUND_PCR'},lab)
        paid_results=[{'orderItemId':row['itemId'],'name':'QA退款权限指标','value':'5.20','unit':'mg/L','referenceRange':'1-10','abnormalFlag':False} for row in paid_detail['items']]
        call(f'/lab/tasks/{paid_task["id"]}/results',{'results':paid_results},lab)
        workflow=call('/report-workflows',{'taskId':paid_task['id'],'summary':prefix+'退款权限摘要','conclusion':prefix+'退款权限结论'},lab)[0]
        call(f'/report-workflows/{workflow}/review',{'decision':'APPROVE','reason':'QA报告审核'},admin)
        published=call(f'/report-workflows/{workflow}/publish',{},admin)
        paid_report=published['detail_id']
        assert raw(f'/pdf/view/{paid_report}',token=doctor)[1].startswith(b'%PDF')
        probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-refund-bridge.php'
        result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(php_order)],input=probe.read_bytes(),capture_output=True,check=True)
        assert b'PASS: PHP cash mock refund' in result.stdout
        php_detail=call(f'/orders/{php_order}',token=doctor)
        assert php_detail['refunded'] is True and php_detail['refundReason'].startswith('PHP管理员#4：')
        restricted=call(f'/reports/{paid_report}',token=doctor)
        assert restricted['paymentRestricted'] is True
        assert all(not restricted.get(key) for key in ['pdfHttpUrl','reportResult','mailReport'])
        for url in [f'/reports/{paid_report}/pdf-url',f'/reports/{paid_report}/download',f'/pdf/view/{paid_report}',f'/report-workflows/{workflow}/download']:
            status,content=raw(url,token=doctor)
            if isinstance(content,dict):assert content.get('code',status)!=200
            else:assert status!=200 and not content.startswith(b'%PDF')
        summary.update(refundedReportId=paid_report,reportAccessRevokedAfterRefund=True)
        summary.update(oneRefundAuditAndMessage=True,doctorAndAdminListsShowRefund=True,pendingAndIssuedInvoiceBlockRefund=True,phpRefundOrder=php_order,phpRefundVisibleInNewSystem=True,noRealPayment=True)

        summary['steps'].append('现结mock付款、丢失异常、退款越权、四并发一次及医生退款状态读取')
        print(json.dumps(summary,ensure_ascii=False,indent=2))
        sys.exit(0)

    if '--credit-concurrency' in sys.argv:
        contract_command = {'contractNo': prefix + '_CREDIT', 'hospitalId': hospital, 'laboratoryId': lab_id, 'settlementType': 'MONTHLY', 'billingCycleDays': 30, 'creditAmount': 100, 'effectiveFrom': '2026-01-01', 'effectiveTo': '2027-01-01'}
        contract_id = call('/contracts', contract_command, admin)
        call(f'/contracts/{contract_id}/prices', {'infoTestTypeId': item_id, 'listPrice': 88, 'agreedPrice': 60, 'effectiveFrom': '2026-01-01', 'effectiveTo': '2027-01-01', 'status': 'ACTIVE'}, admin)
        credit_orders = [call('/orders', order_body, doctor)['orderId'] for _ in range(2)]
        with ThreadPoolExecutor(max_workers=2) as workers:
            reviews = list(workers.map(lambda oid: raw(f'/review/orders/{oid}', {'approved': True, 'reason': prefix + '额度并发'}, admin), credit_orders))
        states = {oid: int(sql(f'SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={oid}')) for oid in credit_orders}
        assert sorted(states.values()) == [0,2], (reviews, states)
        approved = next(oid for oid,status in states.items() if status == 2)
        pending = next(oid for oid,status in states.items() if status == 0)
        assert sql(f'SELECT review_status FROM lin_order_extension WHERE order_id={pending}') == 'pending'
        def used_credit():
            return sql(f"SELECT COALESCE(SUM(s.payable_amount),0) FROM lin_order_pricing_snapshot s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id WHERE s.contract_id={contract_id} AND o.SampleStatus IN (2,3) AND NOT EXISTS(SELECT 1 FROM lin_monthly_bill_item i JOIN lin_monthly_bill b ON b.id=i.bill_id WHERE i.order_id=s.order_id AND b.status='SETTLED')")
        from decimal import Decimal
        assert Decimal(used_credit()) == Decimal('60')
        status, denied = raw(f'/review/orders/{approved}', {'approved': True, 'reason': '重复审核'}, admin)
        assert denied.get('code', status) != 200 and Decimal(used_credit()) == Decimal('60')
        status, denied = raw(f'/contracts/{contract_id}?version=0', {**contract_command, 'creditAmount': 59}, admin, method='PUT')
        assert denied.get('code', status) != 200
        assert Decimal(sql(f'SELECT credit_amount FROM lin_contract WHERE id={contract_id}')) == Decimal('100')
        status, denied = raw(f'/orders/{approved}/status?status=4', {}, doctor, method='PUT')
        assert denied.get('code', status) != 200 and Decimal(used_credit()) == Decimal('60')
        call(f'/admin/users/{accounts["doctor"]["id"]}/roles', {'roleCode': 'HOSPITAL_ADMIN', 'hospitalId': hospital}, admin)
        doctor = login(accounts['doctor']['username'], accounts['doctor']['password'])
        from datetime import date, timedelta
        period_end = date.today().replace(day=1) - timedelta(days=1)
        bills = []
        def settle_qa_order(oid):
            # Backdate only this run's synthetic order to exercise a completed billing period.
            sql(f"UPDATE lin_lb_listdiagnosisproject SET CreateDt='{period_end.isoformat()} 12:00:00' WHERE ListDiagnosisProjectId={oid} AND AnimalName='{prefix}猫' AND InfoHospitalId={hospital}")
            bill = call('/bills', {'hospitalId': hospital, 'laboratoryId': lab_id, 'month': period_end.strftime('%Y-%m')}, admin)
            bid = bill['id']
            assert Decimal(str(bill['due_amount'])) == Decimal('60')
            reference = 'mock-' + prefix + '_' + str(bid)
            status, denied = raw(f'/bills/{bid}/settlement', {'reference': reference}, admin)
            assert denied.get('code',status) == 409 and Decimal(used_credit()) == Decimal('60')
            if '--overdue-boundaries' in sys.argv and not bills:
                event_key = f'bill:{bid}:overdue'
                for offset in [1,0]:
                    sql(f"UPDATE lin_monthly_bill SET due_date=DATE_ADD(CURDATE(),INTERVAL {offset} DAY) WHERE id={bid} AND hospital_id={hospital} AND contract_id={contract_id}")
                    status, denied = raw(f'/bills/{bid}/overdue-reminder', {}, admin)
                    assert denied.get('code',status) == 409
                    assert sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE business_key='{event_key}'") == '0'
                sql(f"UPDATE lin_monthly_bill SET due_date=DATE_SUB(CURDATE(),INTERVAL 1 DAY) WHERE id={bid} AND hospital_id={hospital} AND contract_id={contract_id}")
                for token in [doctor,courier]:
                    status, denied = raw(f'/bills/{bid}/overdue-reminder', {}, token)
                    assert denied.get('code',status) == 403
                with ThreadPoolExecutor(max_workers=4) as workers:
                    reminders = list(workers.map(lambda token: raw(f'/bills/{bid}/overdue-reminder',{},token),[admin,lab,admin,lab]))
                assert all(body.get('code',status)==200 for status,body in reminders), reminders
                recipients = int(sql(f"SELECT COUNT(*) FROM lin_user u LEFT JOIN lin_doctor_profile p ON p.user_id=u.id WHERE u.hospital_id={hospital} AND u.status='normal' AND (p.review_status IS NULL OR p.review_status='approved')"))
                assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE business_key='{event_key}'")) == recipients
                assert sql(f"SELECT COUNT(*) FROM lin_notification_event e JOIN lin_user u ON u.id=e.user_id WHERE e.business_key='{event_key}' AND (u.hospital_id IS NULL OR u.hospital_id<>{hospital})") == '0'
                assert sql(f'SELECT status FROM lin_monthly_bill WHERE id={bid}') == 'OVERDUE'
                assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MONTHLY_BILL' AND resource_id='{bid}' AND action='MONTHLY_BILL_OVERDUE_REMINDER'") == '1'
                summary['overdueBoundaries'] = {'tomorrowAndTodayDenied':True,'nonLabDenied':True,'concurrentReminders':4,'eventsPerRecipient':1,'crossHospitalEvents':0,'billId':bid}
            call(f'/bills/{bid}/reconciliation', {'action':'CONFIRM','reason':'QA医院核对'}, doctor)
            call(f'/bills/{bid}/reconciliation', {'action':'CONFIRM','reason':'QA实验室核对'}, admin)
            settled = call(f'/bills/{bid}/settlement', {'reference':reference}, admin)
            assert settled['status'] == 'SETTLED'
            call(f'/bills/{bid}/settlement', {'reference':reference}, admin)
            assert Decimal(used_credit()) == 0
            if '--overdue-boundaries' in sys.argv and not bills:
                call(f'/bills/{bid}/overdue-reminder',{},admin)
                assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE business_key='bill:{bid}:overdue'")) == recipients
                original_dates = sql(f'SELECT due_date,settled_at FROM lin_monthly_bill WHERE id={bid}').split('\t')
                try:
                    sql(f"UPDATE lin_monthly_bill SET due_date=DATE_SUB(CURDATE(),INTERVAL 10 DAY),settled_at=DATE_SUB(NOW(),INTERVAL 2 DAY) WHERE id={bid} AND hospital_id={hospital} AND contract_id={contract_id}")
                    historical = call(f'/bills/{bid}',token=admin)
                    assert int(historical['overdueDays']) == 8
                finally:
                    due, settled = original_dates
                    import re
                    assert re.fullmatch(r'\d{4}-\d{2}-\d{2}',due) and re.fullmatch(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d{1,6})?',settled)
                    sql(f"UPDATE lin_monthly_bill SET due_date='{due}',settled_at='{settled}' WHERE id={bid} AND hospital_id={hospital} AND contract_id={contract_id}")
                summary['overdueBoundaries'].update(overdueReconciliationContinued=True,settledReplayNoNewEvents=True,oneReminderAudit=True,settledOverdueDaysFrozen=True)

            bills.append(bid)
        settle_qa_order(approved)

        call(f'/review/orders/{pending}', {'approved': True, 'reason': prefix + '结算释放后审核'}, admin)
        assert Decimal(used_credit()) == Decimal('60')
        call(f'/contracts/{contract_id}?version=0', {**contract_command, 'creditAmount': 60}, admin, method='PUT')
        assert Decimal(sql(f'SELECT credit_amount FROM lin_contract WHERE id={contract_id}')) == Decimal('60')
        settle_qa_order(pending)
        assert Decimal(used_credit()) == 0
        racing_order = call('/orders', order_body, doctor)['orderId']
        with ThreadPoolExecutor(max_workers=2) as workers:
            review_job = workers.submit(raw, f'/review/orders/{racing_order}', {'approved': True, 'reason': prefix + '调额并发审核'}, admin)
            limit_job = workers.submit(raw, f'/contracts/{contract_id}?version=1', {**contract_command, 'creditAmount': 30}, admin, 'PUT')
            review_result, limit_result = review_job.result(), limit_job.result()
        final_limit = Decimal(sql(f'SELECT credit_amount FROM lin_contract WHERE id={contract_id}'))
        exposure = Decimal(used_credit())
        assert exposure <= final_limit, (review_result, limit_result, final_limit, exposure)
        assert (final_limit,exposure) in [(Decimal('30'),Decimal('0')),(Decimal('60'),Decimal('60'))]
        summary.update(hospitalId=hospital,contractId=contract_id,creditOrders=credit_orders,statesAfterConcurrentReview=states,usedCredit=str(exposure),finalLimit=str(final_limit),duplicateReviewDenied=True,creditDecreaseBelowUseDenied=True,approvedCancellationDenied=True,settlementReleasesCredit=True,billIds=bills,exactUsedLimitAccepted=True,concurrentLimitAndApprovalSafe=True)

        summary['steps'].append('独立合成医院月结并发审核不超额、失败回滚、重复审核不占用、降额边界、月结双边对账结算释放及接续审核')
        print(json.dumps(summary,ensure_ascii=False,indent=2))
        sys.exit(0)
    order = call('/orders', order_body, doctor)
    order_id = order['orderId']
    assert int(sql(f'SELECT COUNT(*) FROM lin_order_pricing_snapshot WHERE order_id={order_id} AND contract_id IS NOT NULL')) == 0
    summary['orderId'] = order_id
    summary['steps'].append('只中文畜主下单且无合同定价快照')

    if '--doctor-resubmit' in sys.argv:
        initial_detail = call(f'/orders/{order_id}', token=doctor)
        assert int(initial_detail['petId']) == pet_id and initial_detail['clinicalSymptoms'] == order_body['clinicalSymptoms']
        original_barcode = initial_detail['userNO']
        call(f'/review/orders/{order_id}', {'approved':False,'reason':prefix+'补全资料'}, admin)
        def order_digest():
            content = '\n'.join(sql(statement) for statement in [
                f'SELECT * FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order_id}',
                f'SELECT * FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order_id} ORDER BY DiagnosisDetailId',
                f'SELECT * FROM lin_order_extension WHERE order_id={order_id}',
                f'SELECT * FROM lin_order_clinical WHERE order_id={order_id}',
                f'SELECT * FROM lin_order_pricing_snapshot WHERE order_id={order_id}'])
            return hashlib.sha256(content.encode()).hexdigest()
        before_rejected = order_digest()
        peer_status, peer_body = raw(f'/orders/{order_id}/resubmit', order_body, accounts['peer']['token'])
        assert peer_status == 403 or peer_body.get('code') == 403
        assert order_digest() == before_rejected
        invalid_status, invalid_body = raw(f'/orders/{order_id}/resubmit', {**order_body, 'petId':2147483647}, doctor)
        assert invalid_status != 200 or invalid_body.get('code') != 200
        assert order_digest() == before_rejected, 'Invalid pet association left partial order/item/pricing changes'
        invalid_status, invalid_body = raw(f'/orders/{order_id}/resubmit', {**order_body, 'laboratoryId':2147483647}, doctor)
        assert invalid_status != 200 or invalid_body.get('code') != 200
        assert order_digest() == before_rejected, 'Invalid laboratory left partial changes'
        revised_body = {**order_body, 'birthday':'2023-01-02', 'sampleArea':prefix+'修订采样部位', 'clinicalSymptoms':prefix+'重提后症状'}
        with ThreadPoolExecutor(max_workers=2) as executor:
            concurrent = list(executor.map(lambda _: raw(f'/orders/{order_id}/resubmit', revised_body, doctor), [1,2]))
        assert sum(1 for status, body in concurrent if status == 200 and body.get('code') == 200) == 1, concurrent
        assert sum(1 for status, body in concurrent if status == 409 or body.get('code') == 409) == 1, concurrent
        after_revised = order_digest()
        repeated_status, repeated_body = raw(f'/orders/{order_id}/resubmit', revised_body, doctor)
        assert repeated_status == 409 or repeated_body.get('code') == 409
        assert order_digest() == after_revised
        trigger_id = sql(f"SELECT id FROM lin_lb_notification WHERE order_id={order_id} AND new_status=0 ORDER BY id DESC LIMIT 1")
        assert trigger_id and sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE event_type='order' AND business_key='legacy:{trigger_id}'") == '1'
        summary['doctorResubmitSecurity'] = {'sameHospitalPeerDenied':True,'invalidPetAtomicRollback':True,'invalidLaboratoryAtomicRollback':True,'concurrentSingleWinner':True,'repeatNoChanges':True,'pendingNotificationUnique':True}
        revised = call(f'/orders/{order_id}', token=doctor)
        assert revised['sampleStatus'] == 0 and int(revised['petId']) == pet_id and revised['userNO'] == original_barcode
        assert revised['clinicalSymptoms'] == revised_body['clinicalSymptoms'] and revised['medicalHistory'] == order_body['medicalHistory']
        assert str(revised['birthday']).startswith('2023-01-02') and revised['sampleArea'] == revised_body['sampleArea']
        assert int(sql(f"SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order_id} AND IsCheckPass=1")) == 1
        assert int(sql(f"SELECT COUNT(*) FROM lin_lb_diagnosisdetail WHERE ListDiagnosisProjectId={order_id} AND IsCheckPass=0")) == 1
        health = call(f'/pets/{pet_id}/health', token=doctor)
        assert any(row.get('clinicalSymptoms') == revised_body['clinicalSymptoms'] for row in health['medicalHistory'])
        summary['doctorResubmit'] = {'petLinkPreserved':True,'clinicalUpdated':True,'historyPreserved':True,'birthdayUpdated':True,'oldItemVoided':True,'barcodePreserved':True}
        summary['steps'].append('医生退回重提保留宠物档案关联和临床资料，生日/采样更新，原条码与历史明细保留')
    if '--php-clinical' in sys.argv:
        probe = Path(__file__).resolve().parents[2] / 'integration/legacy-php/test-clinical-bridge.php'
        result = subprocess.run(['docker', 'exec', '-i', 'petlis-legacy-php-preview', 'php7.4', '/dev/stdin', str(order_id)], input=probe.read_bytes(), capture_output=True, check=True)
        clinical_probe = json.loads(result.stdout.decode())
        assert clinical_probe['passed'] is True
        concurrent_snapshot = call(f'/legacy/orders/{order_id}/clinical', token=admin)
        def submit_correction(number):
            return raw(f'/legacy/orders/{order_id}/clinical', {'version': concurrent_snapshot['version'],
                'fields': {**concurrent_snapshot['fields'], 'remark': 'QA并发修正' + str(number)},
                'reason': '合成并发修正测试', 'resubmit': False}, admin)
        with ThreadPoolExecutor(max_workers=2) as executor:
            correction_results = list(executor.map(submit_correction, [1, 2]))
        assert sum(1 for status, body in correction_results if status == 200 and body.get('code') == 200) == 1, correction_results
        assert sum(1 for status, body in correction_results if status == 409 or body.get('code') == 409) == 1, correction_results
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='ORDER' AND resource_id='{order_id}' AND action='LEGACY_CLINICAL_UPDATED'") == '3'
        clinical_probe['auditCount'] = 3
        clinical_probe['concurrentSingleWinner'] = True
        snapshot = call(f'/orders/{order_id}/clinical', token=doctor)
        assert snapshot['fields']['clinicalSymptoms'] == 'QA修订后症状'
        detail_after = call(f'/orders/{order_id}', token=doctor)
        assert detail_after['sampleArea'] == 'QA修正采样部位' and detail_after['sampleStatus'] == 0
        status, body = raw(f'/legacy/orders/{order_id}/clinical', {'version': snapshot['version'], 'fields': snapshot['fields'], 'reason': '越权修正', 'resubmit': False}, doctor)
        assert status == 403 or body.get('code') == 403
        summary['clinicalCorrection'] = clinical_probe
        summary['steps'].append('PHP待审资料修正、过期写拒绝、退回再送审、医生端同单读取、单价和身份不变')
    if '--prepare-review' in sys.argv:
        summary['doctorToken']=doctor
        evidence=private/'qa/php-review-case-private.json'
        evidence.write_text(json.dumps(summary,ensure_ascii=False,indent=2));evidence.chmod(0o600)
        print('Prepared one synthetic pending review fixture:',order_id)
        sys.exit(0)
    if '--legacy-adoption' in sys.argv or '--legacy-submit' in sys.argv:
        probe = Path(__file__).resolve().parents[2] / 'integration/legacy-php/test-legacy-adoption.php'
        result = subprocess.run(['docker', 'exec', '-i', 'petlis-legacy-php-preview', 'php7.4', '/dev/stdin', str(order_id)] + (['submit'] if '--legacy-submit' in sys.argv else []), input=probe.read_bytes(), capture_output=True, check=True)
        adopted = json.loads(result.stdout.decode())
        assert adopted['passed'] is True
        order_id = adopted['orderId']
        summary['orderId'] = order_id
        summary['steps'].append('PHP原表合成旧单显式接入并审核，原价与原单ID保留')
    else:
        call(f'/review/orders/{order_id}', {'approved': True, 'reason': prefix + '旧月结审核'}, admin)
    detail = call(f'/orders/{order_id}', token=doctor)
    assert detail['sampleStatus'] == 2
    summary['userNO'] = detail['userNO']
    summary['steps'].append('旧月结无合同审核兼容挂账')
    if '--php-clinical' in sys.argv:
        locked = call(f'/orders/{order_id}/clinical', token=doctor)
        assert locked['editable'] is False
        status, body = raw(f'/legacy/orders/{order_id}/clinical', {'version': locked['version'], 'fields': locked['fields'], 'reason': '已审核后禁止修正', 'resubmit': False}, admin)
        assert status == 409 or body.get('code') == 409
        health = call(f'/pets/{pet_id}/health', token=doctor)
        assert any(row.get('clinicalSymptoms') == 'QA修订后症状' for row in health['medicalHistory'])
        summary['clinicalCorrection']['approvedWriteDenied'] = True
        summary['clinicalCorrection']['doctorHealthUpdated'] = True


    if sf_sandbox:
        sf_contact = {'contact': '接口测试', 'mobile': '13800000000', 'province': '广东省',
                      'city': '深圳市', 'county': '南山区', 'address': '沙箱接口测试地址（不真实寄件）'}
        sf_body = {'sender': sf_contact, 'recipient': {**sf_contact, 'mobile': '13900000000'}}
        forbidden_status, forbidden_body = raw(f'/logistics/sf/orders/{order_id}', sf_body, doctor)
        assert forbidden_status == 403 or forbidden_body.get('code') == 403
        with ThreadPoolExecutor(max_workers=4) as workers:
            replies=list(workers.map(lambda _: call(f'/logistics/sf/orders/{order_id}', sf_body, admin),range(4)))
        sf_result=next(r for r in replies if r.get('shipmentId'))
        assert sf_result['state'] == 'SUCCEEDED' and sf_result['trackingNo'] and sf_result['sandbox']
        shipment_id = sf_result['shipmentId']
        replay = call(f'/logistics/sf/orders/{order_id}', sf_body, admin)
        assert replay['shipmentId'] == shipment_id and replay['trackingNo'] == sf_result['trackingNo']
        assert int(sql(f"SELECT COUNT(*) FROM lin_sf_request WHERE order_id={order_id} AND operation='CREATE'")) == 1
        assert int(sql(f"SELECT COUNT(*) FROM lin_shipment WHERE order_id={order_id}")) == 1
        assert int(sql(f"SELECT version FROM lin_sf_request WHERE order_id={order_id} AND operation='CREATE'")) == 2
        with ThreadPoolExecutor(max_workers=4) as workers:
            route_results=list(workers.map(lambda _: call(f'/logistics/sf/orders/{order_id}/routes/refresh',{},admin),range(4)))
        for route_result in route_results:
            assert route_result['status']=='PENDING_PICKUP' and route_result['sandbox'] is True
        assert int(sql(f"SELECT COUNT(*) FROM lin_sf_route_event WHERE shipment_id={shipment_id}")) == 15
        assert int(sql(f"SELECT COUNT(*) FROM lin_sf_route_event WHERE shipment_id={shipment_id} AND applied_at IS NOT NULL")) == 0
        assert call(f'/logistics/sf/orders/{order_id}', token=doctor)['trackingNo'] == sf_result['trackingNo']
        call(f'/logistics/{shipment_id}/assign', {'courierUserId': accounts['courier']['id']}, admin)
        summary['steps'].append('顺丰官方沙箱并发下单同单同号、路由四并发查询15节点去重、历史节点不推进及医生越权拦截')
    else:
        shipment_id = call('/logistics', {'orderId': order_id, 'trackingNo': prefix + '_SHIP',
                                          'laboratoryId': lab_id, 'pickupAddress': 'QA测试地址',
                                          'courierUserId': accounts['courier']['id']}, admin)
    if sf_label:
        label=call(f'/logistics/sf/orders/{order_id}/label',{},admin)
        assert label['sandbox'] is True and 'token' not in label and 'sf-express.com' not in label['url']
        assert call(f'/logistics/sf/orders/{order_id}/label',{},admin)['id']==label['id']
        assert call(f'/logistics/sf/orders/{order_id}/label',token=doctor)['id']==label['id']
        denied_status,denied=raw(f'/logistics/sf/orders/{order_id}/label',{},doctor)
        assert denied_status==403 or denied.get('code')==403
        status,pdf=raw(label['url'],token=doctor)
        assert status==200 and pdf.startswith(b'%PDF-')
        anonymous_status,_=raw(label['url'])
        assert anonymous_status in (401,403)
        assert int(sql(f"SELECT COUNT(*) FROM lin_file WHERE biz_type='SF_LABEL' AND biz_id='{shipment_id}'"))==1
        summary['labelFileId']=label['id']
        summary['steps'].append('官方沙箱面单PDF生成落库、缓存复用、同院下载、医生生成及匿名下载拦截')
    if sf_cancel:
        for _ in range(2):
            cancelled=call(f'/logistics/sf/orders/{order_id}/cancel',{},admin)
            assert cancelled['cancellationState']=='SUCCEEDED'
        assert call(f'/logistics/{shipment_id}',token=doctor)['status']=='CANCELLED'
        if sf_label:
            denied_status,denied=raw(label['url'],token=doctor)
            assert denied_status==409 or denied.get('code')==409
            summary['steps'].append('取消后已缓存面单下载被拦截')
        assert int(sql(f"SELECT version FROM lin_sf_request WHERE order_id={order_id} AND operation='CANCEL'"))==2
        assert int(sql(f"SELECT SampleStatus FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order_id}"))==2
        blocked_status,blocked=raw(f'/logistics/{shipment_id}/pickup',{},courier)
        assert blocked_status==409 or blocked.get('code')==409
        if '--sf-cancel-recovery' in sys.argv:
            # Fault injection is restricted to this run's fresh synthetic order and its own two rows.
            assert sql(f"SELECT AnimalName LIKE '{prefix}%' FROM lin_lb_listdiagnosisproject WHERE ListDiagnosisProjectId={order_id}") == '1'
            cancel_request = int(sql(f"SELECT id FROM lin_sf_request WHERE order_id={order_id} AND operation='CANCEL' AND state='SUCCEEDED'"))
            request_version = int(sql(f'SELECT version FROM lin_sf_request WHERE id={cancel_request}'))
            before_events = int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}'"))
            try:
                sql(f"UPDATE lin_sf_request SET state='UNKNOWN' WHERE id={cancel_request} AND order_id={order_id} AND state='SUCCEEDED'; UPDATE lin_shipment SET status='PENDING_PICKUP' WHERE id={shipment_id} AND order_id={order_id} AND status='CANCELLED'")
                for suffix in ['cancel']:
                    pending = call(f'/logistics/sf/orders/{order_id}/{suffix}',{},admin)
                    assert pending['cancellationState']=='UNKNOWN'
                assert call(f'/logistics/{shipment_id}',token=doctor)['status']=='PENDING_PICKUP'
                for path,body,token in [(f'/logistics/{shipment_id}/pickup',{},courier),(f'/logistics/sf/orders/{order_id}',sf_body,admin)]:
                    status,denied=raw(path,body,token);assert denied.get('code',status)==409
                assert int(sql(f'SELECT version FROM lin_sf_request WHERE id={cancel_request}'))==request_version
                assert int(sql(f"SELECT COUNT(*) FROM lin_sf_request WHERE order_id={order_id}"))==2
                # PHP performs a real read-only carrier lookup: only explicit A1000/8037 may confirm cancellation.
                probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-sf-reconcile.php'
                result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(order_id)],input=probe.read_bytes(),capture_output=True,check=True)
                assert b'PASS: PHP durable cancellation recovery' in result.stdout
                assert call(f'/logistics/{shipment_id}',token=doctor)['status']=='CANCELLED'
                after_php=int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}'"))
                assert after_php==before_events+1
                with ThreadPoolExecutor(max_workers=4) as workers:
                    restored=list(workers.map(lambda _:call(f'/logistics/sf/orders/{order_id}/reconcile',{},admin),range(4)))
                assert all(row['cancellationState']=='SUCCEEDED' for row in restored)
                assert int(sql(f'SELECT version FROM lin_sf_request WHERE id={cancel_request}'))==request_version+1
                assert sql(f'SELECT business_code FROM lin_sf_request WHERE id={cancel_request}')=='8037'
                assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}'"))==after_php
                status,denied=raw(f'/logistics/sf/orders/{order_id}',sf_body,admin);assert denied.get('code',status)==409
                summary['cancellationRecovery']={'faultInjectionSyntheticOnly':True,'unknownNotInferredSuccessful':True,'unknownCancelNotResent':True,'unknownBlocksPickupAndReship':True,'phpDurableResultRecovery':True,'phpReplayAndFourParallelRecoveryIdempotent':True,'carrierCancellationQueriedNotResent':True,'explicit8037Confirmed':True,'requestVersionAdvancedOnce':True,'confirmedCancellationBlocksReship':True}
            finally:
                sql(f"UPDATE lin_sf_request SET state='SUCCEEDED' WHERE id={cancel_request} AND order_id={order_id} AND state='UNKNOWN'")
                call(f'/logistics/sf/orders/{order_id}/reconcile',{},admin)
        summary['shipmentId']=shipment_id
        summary['steps'].append('官方沙箱取消成功、重复取消不重发、本地运单取消且送检单保留、阻止继续取件')
        print(json.dumps(summary,ensure_ascii=False,indent=2))
        sys.exit(0)
    if '--shipment-exceptions' in sys.argv:
        for step in ['pickup','transit']:
            call(f'/logistics/{shipment_id}/{step}',{},courier)
        exception_path = f'/logistics/{shipment_id}/exception'
        original_exception = {'exceptionType':'DAMAGE','note':prefix+'合成试管破损','requestKey':'exception_'+uuid.uuid4().hex}
        for token in [doctor,lab,admin]:
            status, denied = raw(exception_path,original_exception,token)
            assert denied.get('code',status)==403
        before_version = int(sql(f'SELECT version FROM lin_shipment WHERE id={shipment_id}'))
        for bad in [{**original_exception,'note':' '},{**original_exception,'exceptionType':'x'*33},{**original_exception,'requestKey':'short'}]:
            status, denied = raw(exception_path,bad,courier)
            assert denied.get('code',status)==400
        assert int(sql(f'SELECT version FROM lin_shipment WHERE id={shipment_id}')) == before_version
        with ThreadPoolExecutor(max_workers=4) as workers:
            outcomes=list(workers.map(lambda _:raw(exception_path,original_exception,courier),range(4)))
        assert all(body.get('code',status)==200 for status,body in outcomes),outcomes
        assert int(sql(f'SELECT version FROM lin_shipment WHERE id={shipment_id}')) == before_version+1
        assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}' AND action='EXCEPTION'")) == 1
        status, denied = raw(exception_path,{**original_exception,'note':'同请求改内容'},courier)
        assert denied.get('code',status)==409
        for kind in ['LOSS','DELAY']:
            call(exception_path,{'exceptionType':kind,'note':prefix+'合成异常'+kind,'requestKey':'exception_'+uuid.uuid4().hex},courier)
        view=call(f'/logistics/orders/{order_id}',token=doctor)
        assert view['status']=='IN_TRANSIT' and view['exceptionType']=='DELAY'
        assert len([row for row in view['timeline'] if row['action']=='EXCEPTION'])==3
        assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']} AND business_key LIKE 'shipment:{shipment_id}:exception:%'"))==3
        inbox=call('/inbox?size=100',token=doctor)
        assert len([row for row in inbox['items'] if row['title']=='标本物流异常'])==3
        assert any(prefix+'合成异常DELAY' in row['message'] for row in inbox['items'])
        summary['shipmentExceptions']={'types':['DAMAGE','LOSS','DELAY'],'concurrentSameRequest':4,'oneAuditPerRequest':True,'oneNotificationPerIncident':True,'changedPayloadDenied':True,'permissionAndValidationDenied':True,'doctorTimelineAndInboxVisible':True,'transportStatePreserved':True}
        summary['steps'].append('三类物流异常留痕和医生同步、同请求四并发一次、不同事件独立通知及越权/非法请求拒绝')
        if '--prepare-exception-ui' in sys.argv:
            import os
            fixture={'orderId':order_id,'shipmentId':shipment_id,'userNO':detail['userNO'],'doctorToken':doctor,'courierToken':courier,'items':detail['items'],'prefix':prefix}
            target=private/'qa/shipment-exception-ui-private.json'
            fd=os.open(target,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
            with os.fdopen(fd,'w') as output:json.dump(fixture,output)
            summary['shipmentId']=shipment_id;summary['steps'].append('运输中三条未处理异常已准备，等待浏览器处理；恢复会话仅存本机私有文件')
            print(json.dumps(summary,ensure_ascii=False,indent=2));sys.exit(0)
        if '--shipment-resolutions' in sys.argv:
            events = {event['exceptionType']:event['id'] for event in view['exceptions']}
            path = f'/logistics/{shipment_id}/exceptions/{events["DELAY"]}/resolve'
            reason = prefix+'延误已核实恢复'
            status, denied = raw(path,{'reason':reason},doctor)
            assert denied.get('code',status)==403
            status, denied = raw(path,{'reason':' '},admin)
            assert denied.get('code',status)==400
            foreign_order=call('/orders',order_body,doctor)['orderId']
            call(f'/review/orders/{foreign_order}',{'approved':True,'reason':'合成跨单边界'},admin)
            foreign_shipment=call('/logistics',{'orderId':foreign_order,'trackingNo':prefix+'_FOREIGN','laboratoryId':lab_id,'pickupAddress':'QA测试地址','courierUserId':accounts['courier']['id']},admin)
            foreign_view=call(f'/logistics/{foreign_shipment}/exception',{'exceptionType':'OTHER','note':'合成其他单据异常','requestKey':'exception_'+uuid.uuid4().hex},courier)
            foreign_event=foreign_view['exceptions'][0]['id']
            status, denied=raw(f'/logistics/{shipment_id}/exceptions/{foreign_event}/resolve',{'reason':'禁止跨运单处理'},admin)
            assert denied.get('code',status)==404
            version_before = int(sql(f'SELECT version FROM lin_shipment WHERE id={shipment_id}'))
            with ThreadPoolExecutor(max_workers=4) as workers:
                resolutions=list(workers.map(lambda _:raw(path,{'reason':reason},admin),range(4)))
            assert all(body.get('code',status)==200 for status,body in resolutions),resolutions
            assert int(sql(f'SELECT version FROM lin_shipment WHERE id={shipment_id}'))==version_before+1
            after=call(f'/logistics/{shipment_id}',token=doctor)
            assert after['status']=='IN_TRANSIT' and after['exceptionType']=='LOSS'
            assert len([row for row in after['exceptions'] if row.get('resolvedAt')])==1
            status, denied=raw(path,{'reason':'不能覆盖已处理原因'},admin)
            assert denied.get('code',status)==409
            probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-exception-resolution.php'
            result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(order_id),str(events['LOSS'])],input=probe.read_bytes(),capture_output=True,check=True)
            assert b'PASS: PHP shared-ID resolution' in result.stdout
            assert call(f'/logistics/{shipment_id}',token=doctor)['exceptionType']=='DAMAGE'
            cleared=call(f'/logistics/{shipment_id}/exceptions/{events["DAMAGE"]}/resolve',{'reason':prefix+'实验室已核实标本可用'},lab)
            assert cleared.get('exceptionNote') is None and cleared.get('exceptionType') is None
            assert len(cleared['exceptions'])==3 and all(row.get('resolvedAt') for row in cleared['exceptions'])
            assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}' AND action='EXCEPTION_RESOLVED'"))==3
            assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']} AND business_key LIKE 'shipment:{shipment_id}:resolved:%'"))==3
            summary['shipmentResolutions']={'sameIncidentConcurrency':4,'oneResolutionPerIncident':True,'remainingIncidentPreserved':True,'phpResolvedSharedId':True,'labResolutionScoped':True,'doctorAndWrongShipmentDenied':True,'changedResolutionDenied':True,'historyPreserved':True,'allResolvedClearsWarning':True}
            summary['steps'].append('异常逐条处理、新端四并发一次、PHP同ID处理与新端同步、实验室清理最后异常、跨单/越权/改已处理内容拒绝')

    for action in (['deliver'] if '--shipment-exceptions' in sys.argv else ['pickup', 'transit', 'deliver']):
        call(f'/logistics/{shipment_id}/{action}', {}, courier)
    scanned_barcode=detail['userNO']
    if '--decode-label' in sys.argv:
        verifier=Path(__file__).with_name('verify-specimen-label.py')
        python=private/'tools/barcode-qa311/bin/python'
        decoded=subprocess.run([str(python),str(verifier),'--order-id',str(order_id)],capture_output=True,text=True,check=True)
        evidence=json.loads(decoded.stdout)
        assert evidence['matches'] and evidence['decoded']==detail['userNO']
        scanned_barcode=evidence['decoded']
        summary['decodedLabel']=evidence
        summary['steps'].append('真实鉴权标签经独立ZXing-C++解码准确得到原订单号')
    if '--php-lab' not in sys.argv:
        call(f'/logistics/{shipment_id}/scan-receive', {'barcode': scanned_barcode}, lab)
    summary['shipmentId'] = shipment_id
    summary['steps'].append('物流揽收运输送达扫码签收')

    assignees = call(f'/lab/tasks/assignees?labId={lab_id}', token=lab)
    assert any(x['id'] == accounts['lab']['id'] and x.get('username') for x in assignees['items'])
    if '--php-lab' in sys.argv:
        mapping = private / 'php-preview/app/.petlis-private/laboratory-accounts.json'
        previous_mapping = mapping.read_bytes() if mapping.exists() else None
        account = accounts['lab']
        import tempfile
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', prefix='petlis-lab-credentials-') as credential_file:
            json.dump({'username': account['username'], 'password': account['password'], 'userId': account['id']}, credential_file)
            credential_file.flush()
            validator = Path(__file__).resolve().parent / 'configure-php-lab.py'
            checked = subprocess.run([sys.executable, str(validator), '--legacy-admin-id', '4', '--laboratory-id', str(lab_id), '--credentials', credential_file.name, '--validate-only'], capture_output=True, text=True)
            assert checked.returncode == 0, 'PHP岗位身份配置校验未通过（未打印凭据）'
        summary['steps'].append('岗位配置器校验原PHP管理员、启用实验室范围及LAB_STAFF正常登录')
        if '--use-local-lab' in sys.argv:
            assert json.loads(previous_mapping)['4'] == {'username': account['username'], 'password': account['password'], 'userId': account['id']}
        else:
            mapping.write_text(json.dumps({'4': {'username': account['username'], 'password': account['password'], 'userId': account['id']}}))
            mapping.chmod(0o600)
        if '--prepare-php-ui' in sys.argv:
            backup = private / 'qa/php-ui-mapping-backup.json'
            if backup.exists():
                if previous_mapping is not None: mapping.write_bytes(previous_mapping)
                else: mapping.unlink()
                raise RuntimeError('Previous browser mapping handoff must be restored first')
            import base64
            backup.write_text(json.dumps({'previous': base64.b64encode(previous_mapping).decode() if previous_mapping is not None else None, 'userId': account['id']}))
            backup.chmod(0o600)
            summary.update(orderId=order_id, userNO=detail['userNO'], laboratoryUserId=account['id'], laboratoryUsername=account['username'])
            summary['steps'].append('Prepared delivered synthetic specimen for native PHP forms; restore private mapping after verification')
            print(json.dumps(summary, ensure_ascii=False, indent=2))
            sys.exit(0)
        probe = Path(__file__).resolve().parents[2] / 'integration/legacy-php/test-lab-bridge.php'
        try:
            result = subprocess.run(['docker', 'exec', '-i', 'petlis-legacy-php-preview', 'php7.4', '/dev/stdin', str(order_id), str(accounts['lab']['id'])] + (['results'] + (['retest'] + (['unassigned'] if '--php-unassigned' in sys.argv else []) if '--php-retest' in sys.argv else []) if '--php-results' in sys.argv else []), input=probe.read_bytes(), capture_output=True, check=True)
            assert b'PASS: PHP receipt' in result.stdout, result.stdout.decode()
        finally:
            if '--use-local-lab' in sys.argv:
                assert mapping.read_bytes() == previous_mapping, 'Permanent PHP mapping unexpectedly changed'
                summary['permanentLabIdentity']['mappingUnchanged'] = True
            elif previous_mapping is not None:
                mapping.write_bytes(previous_mapping)
            else:
                mapping.unlink()

        task_id = int(sql(f"SELECT id FROM lin_lab_task WHERE order_id={order_id} AND status <> 'RETESTED' ORDER BY id DESC LIMIT 1"))
        task = call(f'/lab/tasks/{task_id}', token=lab)
        assert task['status'] == ('REPORT_REVIEW' if '--php-results' in sys.argv else 'TESTING')
        summary['steps'].append('PHP条码签收、错误条码阻止、重复签收去重、分配任务后新系统接续')
    else:
        task = call(f'/lab/tasks/order/{order_id}/assign',
                    {'assigneeId': accounts['lab']['id'], 'equipmentCode': prefix + '_PCR'}, lab)
    if '--lab-reassign' in sys.argv:
        original_task=call(f'/lab/tasks/{task["id"]}',token=lab)
        version=int(original_task['version']);started=original_task['started_at']
        path=f'/lab/tasks/{task["id"]}/reassign'
        change={'assigneeId':accounts['relief_lab']['id'],'equipmentCode':'QA_RELIEF_PCR','version':version,'reason':prefix+'检测人员交接'}
        for token in [doctor,accounts['foreign_lab']['token']]:
            status,denied=raw(path,change,token);assert denied.get('code',status)==403
        status,denied=raw(path,{**change,'assigneeId':accounts['foreign_lab']['id']},lab);assert denied.get('code',status)==400
        status,denied=raw(path,{**change,'reason':' '},lab);assert denied.get('code',status)==400
        with ThreadPoolExecutor(max_workers=4) as workers:
            replies=list(workers.map(lambda _:raw(path,change,lab),range(4)))
        codes=sorted(body.get('code',status) for status,body in replies)
        assert codes==[200,409,409,409],replies
        changed=call(f'/lab/tasks/{task["id"]}',token=lab)
        assert changed['assigned_to']==accounts['relief_lab']['id'] and changed['equipmentName']=='QA_RELIEF_PCR'
        assert int(changed['version'])==version+1 and changed['started_at']==started
        assert len(changed['assignmentHistory'])==2
        blocked_results=[{'orderItemId':row['itemId'],'name':'旧人员禁止写入','value':'1','unit':'mg/L','referenceRange':'0-2','abnormalFlag':False} for row in detail['items']]
        status,denied=raw(f'/lab/tasks/{task["id"]}/results',{'results':blocked_results},lab)
        assert denied.get('code',status)==403
        assert sql(f'SELECT COUNT(*) FROM lin_test_result WHERE lab_task_id={task["id"]}')=='0'
        mapping=private/'php-preview/app/.petlis-private/laboratory-accounts.json'
        previous=mapping.read_bytes() if mapping.exists() else None
        account=accounts['lab']
        try:
            mapping.write_text(json.dumps({'4':{'username':account['username'],'password':account['password'],'userId':account['id']}}));mapping.chmod(0o600)
            probe=Path(__file__).resolve().parents[2]/'integration/legacy-php/test-lab-reassign.php'
            result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','/dev/stdin',str(order_id),str(task['id']),str(account['id'])],input=probe.read_bytes(),capture_output=True,check=True)
            assert b'PASS: PHP task reassignment' in result.stdout
        finally:
            if previous is None:mapping.unlink()
            else:mapping.write_bytes(previous)
        task=call(f'/lab/tasks/{task["id"]}',token=lab)
        assert task['assigned_to']==account['id'] and task['equipmentName']=='QA_PHP_REASSIGN'
        assert int(task['version'])==version+2 and task['started_at']==started
        assert len(task['assignmentHistory'])==3
        summary['labReassignment']={'concurrentCodes':codes,'crossLabAndDoctorDenied':True,'foreignAssigneeDenied':True,'oldWorkerWriteDenied':True,'noResultWrittenByOldWorker':True,'firstStartTimePreserved':True,'phpReassignmentVisible':True,'historyCountBeforeResults':3}
        summary['steps'].append('检测任务改派四并发仅一次、版本/跨实验室/旧人员结果限制、PHP改派回同ID并保留首次开始时间')

    if '--prepare-lab-ui' in sys.argv:
        assert '--php-lab' in sys.argv and '--use-local-lab' in sys.argv and '--php-results' not in sys.argv
        import os
        fixture={'orderId':order_id,'taskId':task['id'],'originalAssignee':accounts['lab']['id'],'version':task['version'],'startedAt':task['started_at'],'doctorToken':doctor,'items':detail['items'],'prefix':prefix}
        target=private/'qa/lab-reassignment-ui-private.json'
        fd=os.open(target,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
        with os.fdopen(fd,'w') as output:json.dump(fixture,output)
        summary['taskId']=task['id'];summary['steps'].append('PHP首次分配完成，准备新后台改派UI验证；恢复数据仅存本机私有文件')
        print(json.dumps(summary,ensure_ascii=False,indent=2));sys.exit(0)

    results = [{'orderItemId': item['itemId'], 'name': 'QA指标', 'value': '5.20',
                'unit': '10^9/L', 'referenceRange': '3.5-5.5'} for item in detail['items']]
    if '--php-results' not in sys.argv:
        call(f"/lab/tasks/{task['id']}/results", {'results': results}, lab)
    else:
        assert '--php-lab' in sys.argv
        summary['steps'].append('PHP结果保存不可覆盖、报告生成、驳回后修订新版本保留历史')
        if '--php-retest' in sys.argv:
            summary['steps'].append('PHP退样作废原结果及报告、禁止旧报告发布、复检独立任务及重复请求去重')
        if '--php-unassigned' in sys.argv:
            summary['steps'].append('未分配退样后的复检先待分配，分配正确子任务后全链完成')
    summary['taskId'] = task['id']
    summary['steps'].append('实验人员列表任务分派与结果录入')

    if '--template-snapshot' in sys.argv:
        assert '--php-results' not in sys.argv, 'template snapshot probe uses the native generation branch'
        template_body = {'laboratoryId': lab_id, 'templateName': prefix + '模板', 'templateCode': prefix + '_TEMPLATE', 'content': prefix + '第一版内容'}
        template_one = call(f'/report-workflows/templates/{item_id}', template_body, lab, method='PUT')
        status, body = raw(f'/report-workflows/templates/{item_id}', {**template_body, 'laboratoryId': 2147483647}, lab, method='PUT')
        assert status == 403 or body.get('code') == 403
    if '--php-results' in sys.argv:
        workflows = [int(sql(f"SELECT id FROM lin_report_workflow WHERE order_id={order_id} AND status='PENDING_REVIEW'"))]
    else:
        workflows = call('/report-workflows', {'taskId': task['id'], 'summary': prefix + '摘要',
                                               'conclusion': prefix + '结论'}, lab)
    assert len(workflows) == 1
    workflow_id = workflows[0]
    if '--template-snapshot' in sys.argv:
        first_workflow = workflow_id
        first = call(f'/report-workflows/{first_workflow}', token=admin)
        assert int(first['template_id']) == template_one and int(first['template_version']) == 1
        status, first_pdf = raw(f'/report-workflows/{first_workflow}/download', token=admin)
        assert status == 200 and first_pdf.startswith(b'%PDF')
        template_two = call(f'/report-workflows/templates/{item_id}', {**template_body, 'content': prefix + '第二版内容'}, lab, method='PUT')
        unchanged = call(f'/report-workflows/{first_workflow}', token=admin)
        assert int(unchanged['template_id']) == template_one and int(unchanged['template_version']) == 1
        assert raw(f'/report-workflows/{first_workflow}/download', token=admin)[1] == first_pdf
        call(f'/report-workflows/{first_workflow}/review', {'decision': 'REJECT', 'reason': prefix + '修订模板后重出'}, admin)
        replacement = call('/report-workflows', {'taskId': task['id'], 'summary': prefix + '新版摘要', 'conclusion': prefix + '新版结论'}, lab)
        assert len(replacement) == 1
        workflow_id = replacement[0]
        latest = call(f'/report-workflows/{workflow_id}', token=admin)
        assert int(latest['template_id']) == template_two and int(latest['template_version']) == 2
        assert raw(f'/report-workflows/{first_workflow}/download', token=admin)[1] == first_pdf
        assert sql(f"SELECT status FROM lin_report_template WHERE id={template_one}") == 'ARCHIVED'
        assert sql(f"SELECT status FROM lin_report_template WHERE id={template_two}") == 'ACTIVE'
        summary['templateSnapshots'] = {'oldTemplateId': template_one, 'newTemplateId': template_two,
                                        'oldWorkflowId': first_workflow, 'oldPdfUnchanged': True,
                                        'oldVersion': 1, 'newVersion': 2, 'foreignLabDenied': True}
        summary['steps'].append('模板更新归档旧版、新报告使用第二版、旧报告版本与PDF字节保持不变、跨实验室更新拒绝')
        listed = call('/report-workflows/templates?laboratoryId=' + str(lab_id) + '&infoTestTypeId=' + str(item_id), token=lab)
        assert {int(row['id']) for row in listed['items']} == {template_one, template_two}
        denied_status, denied_body = raw('/report-workflows/templates', token=doctor)
        assert denied_status == 403 or denied_body.get('code') == 403, (denied_status, denied_body)
        hidden_templates = call('/report-workflows/templates?laboratoryId=2147483647', token=lab)
        assert hidden_templates['items'] == []
        with ThreadPoolExecutor(max_workers=2) as executor:
            concurrent_versions = list(executor.map(lambda content: call(f'/report-workflows/templates/{item_id}',
                {**template_body, 'content': content}, lab, method='PUT'), [prefix + '并发甲', prefix + '并发乙']))
        versions = call('/report-workflows/templates?laboratoryId=' + str(lab_id) + '&infoTestTypeId=' + str(item_id), token=lab)['items']
        assert sorted(int(row['version']) for row in versions) == [1, 2, 3, 4]
        assert len([row for row in versions if row['status'] == 'ACTIVE']) == 1
        assert raw(f'/report-workflows/{first_workflow}/download', token=admin)[1] == first_pdf
        summary['templateSnapshots']['concurrentIds'] = concurrent_versions
        summary['templateSnapshots']['versions'] = [1, 2, 3, 4]
        summary['templateSnapshots']['oneActiveVersion'] = True
        summary['steps'].append('模板列表岗位/实验室隔离、同实验室并发保存得到第三四版且只一版使用中')

        options = call('/report-workflows/templates/options?laboratoryId=' + str(lab_id), token=lab)
        assert any(int(row['id']) == lab_id for row in options['laboratories'])
        assert any(int(row['id']) == item_id for row in options['items'])
        option_status, option_body = raw('/report-workflows/templates/options?laboratoryId=2147483647', token=lab)
        assert option_status == 403 or option_body.get('code') == 403
        active_template = next(int(row['id']) for row in versions if row['status'] == 'ACTIVE')
        denied_status, denied_body = raw(f'/report-workflows/templates/{active_template}/archive', {}, doctor, method='POST')
        assert denied_status == 403 or denied_body.get('code') == 403
        call(f'/report-workflows/templates/{active_template}/archive', {}, lab, method='POST')
        call(f'/report-workflows/templates/{active_template}/archive', {}, lab, method='POST')
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='REPORT_TEMPLATE' AND resource_id='{active_template}' AND action='ARCHIVED'") == '1'
        assert raw(f'/report-workflows/{first_workflow}/download', token=admin)[1] == first_pdf
        call(f'/report-workflows/{workflow_id}/review', {'decision': 'REJECT', 'reason': prefix + '验证停用后生成'}, admin)
        default_workflow = call('/report-workflows', {'taskId': task['id'], 'summary': prefix + '默认模板摘要', 'conclusion': prefix + '默认模板结论'}, lab)[0]
        default_report = call(f'/report-workflows/{default_workflow}', token=admin)
        assert default_report.get('template_id') is None and default_report.get('template_version') is None
        call(f'/report-workflows/{default_workflow}/review', {'decision': 'REJECT', 'reason': prefix + '恢复模板'}, admin)
        restored_template = call(f'/report-workflows/templates/{item_id}', {**template_body, 'content': prefix + '恢复使用第五版'}, lab, method='PUT')
        workflow_id = call('/report-workflows', {'taskId': task['id'], 'summary': prefix + '恢复摘要', 'conclusion': prefix + '恢复结论'}, lab)[0]
        restored_report = call(f'/report-workflows/{workflow_id}', token=admin)
        assert int(restored_report['template_id']) == restored_template and int(restored_report['template_version']) == 5
        summary['templateLifecycle'] = {'archivedId': active_template, 'archiveAuditCount': 1, 'defaultWorkflowId': default_workflow,
            'restoredTemplateId': restored_template, 'restoredVersion': 5, 'optionsScoped': True, 'oldPdfUnchanged': True}
        summary['steps'].append('模板选择器隔离、停用幂等/医生拒绝、旧PDF不变、停用后默认模板和另存第五版恢复')

        query_base = '/report-workflows/templates?q=' + prefix + '&infoTestTypeId=' + str(item_id)
        searched = call(query_base + '&size=2&page=0', token=lab)
        second_page = call(query_base + '&size=2&page=1', token=lab)
        assert int(searched['total']) == 5 and len(searched['items']) == 2 and len(second_page['items']) == 2
        assert not ({int(row['id']) for row in searched['items']} & {int(row['id']) for row in second_page['items']})
        active_page = call(query_base + '&status=ACTIVE', token=lab)
        archived_page = call(query_base + '&status=ARCHIVED', token=lab)
        assert int(active_page['total']) == 1 and int(archived_page['total']) == 4
        assert int(active_page['items'][0]['id']) == restored_template
        assert call('/report-workflows/templates?q=' + prefix + '%25', token=lab)['items'] == []
        invalid_status, invalid_body = raw(query_base + '&status=DELETED', token=lab)
        assert invalid_status == 400 or invalid_body.get('code') == 400
        summary['templateSearch'] = {'total':5,'active':1,'archived':4,'pageSize':2,'pagesDisjoint':True,'literalPercent':True,'invalidStatusRejected':True}
        summary['steps'].append('模板检索与状态组合分页、字面百分号及非法状态验证通过')



    if '--prepare-report' in sys.argv:
        summary['orderId'] = order_id
        summary['workflowId'] = workflow_id
        summary['reportId'] = int(sql(f"SELECT detail_id FROM lin_report_workflow WHERE id={workflow_id}"))
        summary['steps'].append('已准备合成待审核报告供浏览器原生表单验收')
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        sys.exit(0)
    if '--php-report' in sys.argv:
        probe = Path(__file__).resolve().parents[2] / 'integration/legacy-php/test-report-bridge.php'
        result = subprocess.run(['docker', 'exec', '-i', 'petlis-legacy-php-preview', 'php7.4', '/dev/stdin', str(order_id), str(workflow_id)], input=probe.read_bytes(), capture_output=True, check=True)
        assert b'PASS: PHP report' in result.stdout, result.stdout.decode()
        summary['steps'].append('PHP报告审核发布、跨订单拒绝、未审禁止发布、重复发布通知去重')
        published = call(f'/report-workflows/{workflow_id}', token=admin)
    else:
        call(f'/report-workflows/{workflow_id}/review', {'decision': 'APPROVE', 'reason': prefix + '报告审核'}, admin)
        published = call(f'/report-workflows/{workflow_id}/publish', {}, admin)
    assert published['status'] == 'PUBLISHED'
    report_id = published['detail_id']
    summary['workflowId'] = workflow_id
    summary['reportId'] = report_id
    if '--print-report' in sys.argv:
        for token in [admin,lab]:
            status,html=raw(f'/admin/print/reports/{report_id}',token=token)
            assert status==200 and '检验报告'.encode() in html
        status,denied=raw(f'/admin/print/reports/{report_id}',token=doctor)
        assert denied.get('code',status)==403
        summary['reportPrinting']={'adminAndOwnLabAllowed':True,'doctorDenied':True,'publishedReportContentReturned':True}
        summary['steps'].append('已发布报告管理员/本实验室打印可用，医生直接管理打印接口拒绝')
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

    if '--account-inbox' in sys.argv:
        peer = accounts['peer']['token']
        original_preferences = call('/account/preferences', token=doctor)
        peer_preferences = call('/account/preferences', token=peer)
        updated_preferences = {'orderNotifications': False, 'reportNotifications': False,
                               'invoiceNotifications': True, 'newsNotifications': True,
                               'preferredLaboratoryId': lab_id}
        call('/account/preferences', updated_preferences, doctor, method='PUT')
        saved = call('/account/preferences', token=doctor)
        assert all(saved[key] == value for key, value in updated_preferences.items())
        assert call('/account/preferences', token=peer) == peer_preferences
        bad_status, bad_body = raw('/account/preferences', {**updated_preferences, 'preferredLaboratoryId': 2147483647}, doctor, method='PUT')
        assert bad_status >= 400 or bad_body.get('code') != 200
        assert call('/account/preferences', token=doctor) == saved
        call('/account/preferences', original_preferences, doctor, method='PUT')
        unread_before = call('/inbox/unread-count', token=doctor)
        messages = call('/inbox?unread=true&page=0&size=100', token=doctor)
        assert unread_before > 0 and messages['items']
        event_id = int(messages['items'][0]['id'])
        for suffix, method in [('read', 'PUT'), ('retry', 'POST'), ('deliveries', 'GET')]:
            status, body = raw(f'/inbox/{event_id}/{suffix}', {} if method != 'GET' else None, peer, method=method)
            assert status in [403, 404] or body.get('code') in [403, 404], (suffix, status)
        call(f'/inbox/{event_id}/read', {}, doctor, method='PUT')
        call(f'/inbox/{event_id}/read', {}, doctor, method='PUT')
        assert call('/inbox/unread-count', token=doctor) == unread_before - 1
        event_count = int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']}"))
        # Only this newly created fixture event; no processor or external delivery is invoked.
        sql(f"UPDATE lin_notification_event SET push_status='failed',retry_count=1 WHERE id={event_id} AND user_id={accounts['doctor']['id']}")
        call(f'/inbox/{event_id}/retry', {}, doctor)
        status, body = raw(f'/inbox/{event_id}/retry', {}, doctor)
        assert status >= 400 or body.get('code') != 200
        assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']}")) == event_count
        assert sql(f"SELECT push_status FROM lin_notification_event WHERE id={event_id}") == 'pending'
        call('/inbox/read-all', {}, doctor, method='PUT')
        assert call('/inbox/unread-count', token=doctor) == 0
        summary['accountInbox'] = {'eventId': event_id, 'eventCount': event_count, 'preferencesIsolated': True,
                                   'readIdempotent': True, 'foreignAccessDenied': True,
                                   'retryQueuedOnce': True, 'externalDeliveryInvoked': False}
        summary['steps'].append('消息已读/全部已读/跨用户隔离、通知偏好持久化与无效实验室回滚、失败消息重试入队幂等')
    if '--profile-review' in sys.argv:
        before = call('/user/profile', token=doctor)
        changed_nickname = prefix + '资料更新'
        own_qualification = upload_qualification(doctor)
        change = {'nickname': changed_nickname, 'title': 'QA复核职称', 'hospitalId': hospital,
                  'qualificationFiles': [own_qualification]}
        call('/account/profile-changes', change, doctor)
        history = call('/account/profile-changes', token=doctor)
        change_id = int(history[0]['id'])
        assert history[0]['status'] == 'pending'
        assert call('/user/profile', token=doctor)['nickname'] == before['nickname']
        status, body = raw('/account/profile-changes', change, doctor)
        assert status >= 400 or body.get('code') != 200
        peer = accounts['peer']['token']
        assert all(int(row['id']) != change_id for row in call('/account/profile-changes', token=peer))
        status, body = raw(f'/review/profile-changes/{change_id}', {'approved': True}, peer)
        assert status == 403 or body.get('code') == 403
        call(f'/review/profile-changes/{change_id}', {'approved': True, 'reason': prefix + '资料重审'}, admin)
        status, body = raw('/account', token=doctor)
        assert status in [401, 403] or body.get('code') in [401, 403], (status, body)
        doctor = login(accounts['doctor']['username'], accounts['doctor']['password'])
        accounts['doctor']['token'] = doctor
        updated = call('/user/profile', token=doctor)
        assert updated['nickname'] == changed_nickname
        assert sql(f"SELECT title FROM lin_doctor_profile WHERE user_id={accounts['doctor']['id']}") == 'QA复核职称'
        history = call('/account/profile-changes', token=doctor)
        assert history[0]['status'] == 'approved'
        assert int(sql(f"SELECT COUNT(*) FROM lin_profile_change WHERE user_id={accounts['doctor']['id']} AND id={change_id} AND previous_data IS NOT NULL AND requested_data IS NOT NULL")) == 1
        status, body = raw(f'/review/profile-changes/{change_id}', {'approved': False, 'reason': '重复审核'}, admin)
        assert status >= 400 or body.get('code') != 200
        summary['profileReview'] = {'changeId': change_id, 'pendingPreservesProfile': True,
                                   'duplicatePendingDenied': True, 'historyIsolated': True,
                                   'peerReviewDenied': True, 'oldSessionRevoked': True,
                                   'approvedProfileVisibleAfterLogin': True, 'snapshotsPreserved': True}
        summary['steps'].append('资料变更待审不提前生效、重复申请/越权审核拒绝、历史隔离、审核生效并撤销旧会话')
    if '--points-ledger' in sys.argv:
        peer = accounts['peer']['token']
        starting = int(call('/account', token=doctor)['points'])
        grant = {'userId': accounts['doctor']['id'], 'delta': 100, 'reason': prefix + '积分增加', 'requestKey': prefix + '_POINTS_IN'}
        call('/admin/operations/points-adjustments', grant, admin)
        call('/admin/operations/points-adjustments', grant, admin)
        status, body = raw('/admin/operations/points-adjustments', {**grant, 'delta': 999}, admin)
        assert status == 409 or body.get('code') == 409, ('changed replay must conflict', status, body)
        call('/admin/operations/points-adjustments', {**grant, 'delta': -25, 'reason': prefix + '积分减少', 'requestKey': prefix + '_POINTS_OUT'}, admin)
        assert int(call('/account', token=doctor)['points']) == starting + 75
        income = call('/account/points/transactions?category=income&page=0&size=1', token=doctor)
        expense = call('/account/points/transactions?category=expense&page=0&size=1', token=doctor)
        assert income['total'] == 1 and int(income['items'][0]['amount']) == 100
        assert expense['total'] == 1 and int(expense['items'][0]['amount']) == -25
        first = call('/account/points/transactions?page=0&size=1', token=doctor)
        second = call('/account/points/transactions?page=1&size=1', token=doctor)
        assert first['items'][0]['id'] != second['items'][0]['id'] and first['total'] == 2
        status, exported = raw('/account/transactions/export?points=true&category=income', token=doctor)
        assert status == 200 and grant['reason'] in exported.decode('utf-8') and (prefix + '积分减少') not in exported.decode('utf-8')
        status, peer_export = raw('/account/transactions/export?points=true&category=all', token=peer)
        assert status == 200 and prefix not in peer_export.decode('utf-8')
        assert call('/account/points/transactions', token=peer)['total'] == 0
        status, body = raw('/admin/operations/points-adjustments', {**grant, 'requestKey': prefix + '_POINTS_DENIED'}, peer)
        assert status == 403 or body.get('code') == 403
        summary['steps'].append('积分调整同请求幂等与变参冲突、余额流水一致、分类分页导出及跨医生隔离')
        race_before = {role: int(call('/account', token=accounts[role]['token'])['points']) for role in ['doctor', 'peer']}
        race_key = prefix + '_POINTS_RACE'
        def adjust_race(role):
            return raw('/admin/operations/points-adjustments', {'userId': accounts[role]['id'], 'delta': 7, 'reason': prefix + '并发积分', 'requestKey': race_key}, admin)
        with ThreadPoolExecutor(max_workers=2) as pool:
            outcomes = list(pool.map(adjust_race, ['doctor', 'peer']))
        assert sorted(body.get('code', status) for status, body in outcomes) == [200, 409], outcomes
        race_delta = sum(int(call('/account', token=accounts[role]['token'])['points']) - race_before[role] for role in ['doctor', 'peer'])
        assert race_delta == 7
        assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='POINTS_ADJUSTMENT' AND request_id='{race_key}'")) == 1
        summary['pointsLedger'] = {'balanceDeltaBeforeRace': 75, 'incomeRows': 1, 'expenseRows': 1, 'exportIsolated': True,
                                   'concurrentDifferentTargetsSingleAdjustment': True, 'raceTotalDelta': race_delta}

    if '--invoice-file' in sys.argv:
        peer = accounts['peer']['token']
        # Mark only this new synthetic fixture paid to isolate invoice-file acceptance; no real funds.
        sql(f"UPDATE lin_lb_listdiagnosisproject SET IsPayMoney=1 WHERE ListDiagnosisProjectId={order_id} AND CreateSampleId={accounts['doctor']['id']} AND AnimalName LIKE 'QA_REAL_%'")
        profile_id = call('/invoices/profiles', {'title': prefix + '合成开票抬头', 'email': 'qa@example.test'}, doctor)
        invoice_id = call('/invoices', {'profileId': profile_id, 'orderIds': [order_id], 'requestKey': prefix + '_INV_FILE'}, doctor)
        def upload_invoice(content, token):
            boundary = 'PetlisInvoice' + secrets.token_hex(8)
            body = ('--' + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="synthetic-invoice.pdf"\r\nContent-Type: application/pdf\r\n\r\n').encode() + content + ('\r\n--' + boundary + '--\r\n').encode()
            req = urllib.request.Request(base + f'/invoices/{invoice_id}/file', data=body, headers={'Authorization': 'Bearer ' + token, 'Content-Type': 'multipart/form-data; boundary=' + boundary})
            try:
                with urllib.request.urlopen(req) as response: return response.status, json.loads(response.read())
            except urllib.error.HTTPError as error: return error.code, json.loads(error.read())
        status, body = upload_invoice(b'not a PDF', admin)
        assert status >= 400 or body.get('code') != 200
        assert call(f'/invoices/{invoice_id}', token=doctor)['status'] == 'pending'
        _, pdf_content = raw(f'/reports/{report_id}/download', token=doctor)
        status, body = upload_invoice(pdf_content, peer)
        assert status == 403 or body.get('code') == 403
        status, body = upload_invoice(pdf_content, admin)
        assert status == 200 and body['code'] == 200, (status, body)
        invoice = call(f'/invoices/{invoice_id}', token=doctor)
        assert invoice['status'] == 'issued' and invoice['invoiceUrl'] == f'/api/invoices/{invoice_id}/download'
        status, downloaded = raw(f'/invoices/{invoice_id}/download', token=doctor)
        assert status == 200 and downloaded == pdf_content
        for token in [None, peer]:
            status, body = raw(f'/invoices/{invoice_id}/download', token=token)
            assert status in [401, 403] or body.get('code') in [401, 403]
        file_id = int(sql(f"SELECT id FROM lin_file WHERE biz_type='INVOICE' AND biz_id='{invoice_id}'"))
        status, body = raw(f'/platform-files/{file_id}/download', token=peer)
        assert status in [403, 404] or body.get('code') in [403, 404]
        status, body = upload_invoice(pdf_content, admin)
        assert status == 409 or body.get('code') == 409
        assert int(sql(f"SELECT COUNT(*) FROM lin_file WHERE biz_type='INVOICE' AND biz_id='{invoice_id}'")) == 1
        assert int(sql(f"SELECT COUNT(*) FROM lin_notification_event WHERE user_id={accounts['doctor']['id']} AND event_type='invoice' AND business_key='issued:{invoice_id}'")) == 1
        summary['invoiceFile'] = {'applicationId': invoice_id, 'fileId': file_id, 'privateDownloadVerified': True,
                                  'invalidPdfRejected': True, 'foreignUserDenied': True, 'duplicateIssueRejected': True,
                                  'paidStateSyntheticFixture': True, 'externalTaxServiceInvoked': False}
        summary['steps'].append('私有发票PDF上传、文件校验、鉴权下载、跨医生和匿名拒绝、重复开具不新增文件或通知')
    if '--admin-lifecycle' in sys.argv:
        peer_account = accounts['peer']; peer_id = peer_account['id']; peer = peer_account['token']
        status, body = raw(f'/admin/users/{peer_id}/status', {'active': False, 'reason': prefix + '禁用测试'}, doctor, method='PATCH')
        assert status == 403 or body.get('code') == 403
        call(f'/admin/users/{peer_id}/status', {'active': False, 'reason': prefix + '禁用测试'}, admin, method='PATCH')
        status, body = raw('/account', token=peer)
        assert status in [401,403] or body.get('code') in [401,403]
        status, body = raw('/auth/password/login', {'account': peer_account['username'], 'password': peer_account['password']})
        assert status >= 400 or body.get('code') != 200
        call(f'/admin/users/{peer_id}/status', {'active': True, 'reason': prefix + '恢复测试'}, admin, method='PATCH')
        peer = login(peer_account['username'], peer_account['password'])
        call('/account', token=peer)
        call(f'/admin/users/{peer_id}/roles', {'roleCode': 'HOSPITAL_ADMIN', 'hospitalId': hospital}, admin)
        status, body = raw('/account', token=peer)
        assert status in [401,403] or body.get('code') in [401,403]
        elevated = call('/auth/password/login', {'account': peer_account['username'], 'password': peer_account['password']})
        assert 'HOSPITAL_ADMIN' in elevated['roles']
        assignments = call(f'/admin/users/{peer_id}/roles', token=admin)
        assignment = next(row for row in assignments if row['roleCode'] == 'HOSPITAL_ADMIN')
        call(f'/admin/users/{peer_id}/roles/{assignment["id"]}', {}, admin, method='DELETE')
        status, body = raw('/account', token=elevated['token'])
        assert status in [401,403] or body.get('code') in [401,403]
        restored = call('/auth/password/login', {'account': peer_account['username'], 'password': peer_account['password']})
        assert 'HOSPITAL_ADMIN' not in restored['roles']
        config_key = prefix + '_isolated_config'
        call('/admin/operations/configs', {'key': config_key, 'value': {'qaVersion': 1}, 'description': 'Synthetic QA only; no business consumer'}, admin)
        initial_version = int(sql(f"SELECT version FROM lin_operational_config WHERE config_key='{config_key}'"))
        call('/admin/operations/configs/' + config_key, {'value': {'qaVersion': 2}, 'version': initial_version, 'reason': prefix + '更新'}, admin, method='PUT')
        status, body = raw('/admin/operations/configs/' + config_key, {'value': {'qaVersion': 999}, 'version': initial_version, 'reason': '过期写入'}, admin, method='PUT')
        assert status == 409 or body.get('code') == 409
        assert int(sql(f"SELECT JSON_EXTRACT(config_value,'$.qaVersion') FROM lin_operational_config WHERE config_key='{config_key}'")) == 2
        status, body = raw('/admin/operations/configs/' + config_key, {'value': {'qaVersion': 999}, 'version': initial_version + 1, 'reason': '越权写入'}, restored['token'], method='PUT')
        assert status == 403 or body.get('code') == 403
        history_path = '/admin/operations/configs/' + config_key + '/history'
        history = call(history_path, token=admin)
        assert int(history['current']['version']) == initial_version + 1
        assert int(history['history']['total']) == 2
        rows = history['history']['items']
        def snapshot(row):
            value = row['afterSnapshot']
            return json.loads(value) if isinstance(value, str) else value
        original = next(row for row in rows if int(snapshot(row)['version']) == initial_version)
        assert snapshot(original)['value'] == {'qaVersion': 1}
        status, denied = raw(history_path, token=doctor)
        assert status == 403 or denied.get('code') == 403
        restore_path = '/admin/operations/configs/' + config_key + '/restore'
        restore_body = {'auditId': original['id'], 'version': initial_version + 1, 'reason': prefix + '历史恢复'}
        status, denied = raw(restore_path, restore_body, doctor)
        assert status == 403 or denied.get('code') == 403
        other_key = config_key + '_other'
        call('/admin/operations/configs', {'key': other_key, 'value': {'qaVersion': 55}, 'description': 'Synthetic isolated wrong-source test'}, admin)
        other = call('/admin/operations/configs/' + other_key + '/history', token=admin)['history']['items'][0]
        status, denied = raw(restore_path, {**restore_body, 'auditId': other['id']}, admin)
        assert status == 404 or denied.get('code') == 404
        with ThreadPoolExecutor(max_workers=2) as workers:
            outcomes = list(workers.map(lambda _: raw(restore_path, restore_body, admin), range(2)))
        codes = sorted(body.get('code', status) for status, body in outcomes)
        assert codes == [200,409], outcomes
        after_history = call(history_path, token=admin)
        assert int(after_history['history']['total']) == 3
        restored_row = next(row for row in after_history['history']['items'] if row['action'] == 'OPERATIONAL_CONFIG_RESTORED')
        assert int(snapshot(restored_row)['restoredFromAuditId']) == int(original['id'])
        status, denied = raw(restore_path, restore_body, admin)
        assert status == 409 or denied.get('code') == 409
        summary['configHistory'] = {'snapshotsPreserved': True, 'wrongKeyDenied': True, 'doctorDenied': True, 'concurrentRestoreCodes': codes, 'newVersion': initial_version + 2, 'historyCount': 3}
        summary['steps'].append('配置前后快照、历史查询、跨配置来源拒绝、双并发恢复仅一次成功且生成新版本审计')
        assert int(sql(f"SELECT version FROM lin_operational_config WHERE config_key='{config_key}'")) == initial_version + 2
        assert int(sql(f"SELECT JSON_EXTRACT(config_value,'$.qaVersion') FROM lin_operational_config WHERE config_key='{config_key}'")) == 1
        update_source = next(row for row in rows if row['action'] == 'OPERATIONAL_CONFIG_UPDATED')
        call(restore_path, {'auditId': update_source['id'], 'snapshotSide': 'BEFORE', 'version': initial_version + 2, 'reason': prefix + '恢复修改前快照'}, admin)
        assert int(sql(f"SELECT version FROM lin_operational_config WHERE config_key='{config_key}'")) == initial_version + 3
        assert int(sql(f"SELECT JSON_EXTRACT(config_value,'$.qaVersion') FROM lin_operational_config WHERE config_key='{config_key}'")) == 1
        final_history = call(history_path, token=admin)
        assert int(final_history['history']['total']) == 4
        summary['configHistory'].update(beforeSnapshotRestored=True,newVersion=initial_version + 3,historyCount=4)
        summary['adminLifecycle'] = {'userId': peer_id, 'disabledSessionRevoked': True, 'disabledLoginDenied': True,
                                     'roleChangeRevokesSession': True, 'removedRoleAbsentAfterLogin': True,
                                     'configStaleWriteDenied': True, 'configRestoredWithNewVersion': True}
        summary['steps'].append('账号禁启用、角色授予撤销会话失效、配置版本冲突及按新版本恢复原值')
    if '--client-events' in sys.argv:
        event = {'id': str(uuid.uuid4()), 'event': 'order_step', 'step': 'pet', 'outcome': 'view', 'environment': 'develop'}
        assert raw('/client-events', {'events': [event]})[0] == 401
        before = call('/admin/client-events/summary?days=7', token=admin)
        def metric_count(rows):
            return sum(int(row['count']) for row in rows if row['event'] == 'order_step' and row['step'] == 'pet' and row['outcome'] == 'view' and row['environment'] == 'develop')
        with ThreadPoolExecutor(max_workers=2) as executor:
            deliveries = list(executor.map(lambda token: raw('/client-events', {'events': [event]}, token), [doctor, lab]))
        # A deadlock victim may retry the same event ID, never create a fresh ID.
        for status, body in deliveries:
            if status != 200 or body.get('code') != 200:
                call('/client-events', {'events': [event]}, doctor)
        call('/client-events', {'events': [event, event]}, doctor)
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MINIPROGRAM_EVENT' AND resource_id='{event['id']}'") == '1'
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MINIPROGRAM_EVENT' AND resource_id='{event['id']}' AND actor_user_id IS NULL AND actor_role IS NULL AND hospital_id IS NULL AND laboratory_id IS NULL AND ip_address IS NULL AND JSON_LENGTH(after_json)=4") == '1'
        invalid_id = str(uuid.uuid4())
        bad = {**event, 'id': str(uuid.uuid4()), 'mobile': 'synthetic-disallowed-field'}
        status, body = raw('/client-events', {'events': [{**event, 'id': invalid_id}, bad]}, doctor)
        assert status == 400 or body.get('code') == 400, (status, body)
        assert sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='MINIPROGRAM_EVENT' AND resource_id='{invalid_id}'") == '0'
        status, body = raw('/admin/client-events/summary', token=doctor)
        assert status == 403 or body.get('code') == 403
        status, body = raw('/admin/client-events/summary?days=31', token=admin)
        assert status == 400 or body.get('code') == 400
        after = call('/admin/client-events/summary?days=7', token=admin)
        assert metric_count(after) == metric_count(before) + 1
        summary['clientEvents'] = {'eventId': event['id'], 'anonymousRequestDenied': True, 'concurrentReplayCount': 1,
                                  'identityColumnsNull': True, 'storedAttributes': 4, 'invalidBatchAtomic': True,
                                  'doctorSummaryDenied': True, 'summaryIncrement': 1}
        summary['steps'].append('匿名事件白名单、并发重放去重、落盘无身份字段、批次原子拒绝、管理员汇总计数')
    if '--lab-reassign' in sys.argv:
        final_task=call(f'/lab/tasks/{task["id"]}',token=lab)
        status,denied=raw(f'/lab/tasks/{task["id"]}/reassign',{'assigneeId':accounts['relief_lab']['id'],'equipmentCode':'QA_LATE','version':final_task['version'],'reason':'已有结果禁止改派'},lab)
        assert denied.get('code',status)==409
        assert len(call(f'/lab/tasks/{task["id"]}',token=lab)['assignmentHistory'])==3
        summary['labReassignment']['resultHistoryPreventsReassignment']=True
    if '--shipment-exceptions' in sys.argv:
        status, denied=raw(exception_path,{'exceptionType':'LOSS','note':'签收后禁止新增异常','requestKey':'exception_'+uuid.uuid4().hex},courier)
        assert denied.get('code',status)==409
        call(exception_path,original_exception,courier)
        assert int(sql(f"SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='SHIPMENT' AND resource_id='{shipment_id}' AND action='EXCEPTION'"))==3
        summary['shipmentExceptions'].update(receivedNewIncidentDenied=True,oldRequestReplayReadOnly=True)
    print(json.dumps(summary, ensure_ascii=False, indent=2))
finally:
    if foreign_lab_id is not None:
        sql(f"UPDATE lin_laboratory SET active=0 WHERE id={foreign_lab_id} AND code='{prefix}_FOREIGN_LAB'")
    for phone in phones:
        sql(f"DELETE FROM lin_sms WHERE mobile='{phone}'")
