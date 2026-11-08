"""Real local API test: staff uniqueness, minimal scopes, no doctor registration, disable/revoke."""
import concurrent.futures
import importlib.util
import json
from pathlib import Path
import secrets
import urllib.error
import urllib.request
spec=importlib.util.spec_from_file_location('staff_setup',Path(__file__).with_name('setup-php-lab.py'))
setup=importlib.util.module_from_spec(spec);spec.loader.exec_module(setup)
admin=setup.administrator(4)
username='staff_qa_'+secrets.token_hex(6)
password=secrets.token_urlsafe(20)+'A7'
body={'username':username,'displayName':'本机合成员工测试','password':password,'roleCode':'COURIER','laboratoryId':None,'reason':'仅本机员工管理并发验收'}

def raw(path,body=None,token=None,method=None):
    req=urllib.request.Request(setup.config.BASE+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**({'Authorization':'Bearer '+token} if token else {})},method=method)
    try:
        with setup.opener.open(req,timeout=20) as response:return response.status,json.load(response)
    except urllib.error.HTTPError as error:return error.code,json.load(error)

uid=None
before=setup.config.sql('SELECT COUNT(*) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL')
try:
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as workers:
        replies=list(workers.map(lambda _:raw('/admin/staff',body,admin),range(4)))
    codes=sorted(value.get('code',status) for status,value in replies)
    assert codes==[200,409,409,409],codes
    uid=next(value['data'] for status,value in replies if value.get('code',status)==200)
    assert setup.config.sql(f"SELECT COUNT(*) FROM lin_user WHERE username='{username}'")=='1'
    assert setup.config.sql(f'SELECT hospital_id IS NULL FROM lin_user WHERE id={uid}')=='1'
    assert setup.config.sql(f'SELECT requested_hospital_id IS NULL FROM lin_doctor_profile WHERE user_id={uid}')=='1'
    session=setup.call('/auth/password/login',{'account':username,'password':password})
    assert session['userId']==uid and set(session['roles'])=={'COURIER'}
    status,account=raw('/logistics',token=session['token']);assert account.get('code',status)==200
    status,denied=raw('/admin/staff',{**body,'username':username+'_deny'},session['token']);assert denied.get('code',status)==403
    status,denied=raw('/admin/staff',{**body,'username':username+'_admin','roleCode':'ADMIN'},admin);assert denied.get('code',status)==400
    assert setup.config.sql('SELECT COUNT(*) FROM lin_doctor_profile WHERE requested_hospital_id IS NOT NULL')==before
    audit=setup.config.sql(f"SELECT COALESCE(before_json,''),COALESCE(after_json,''),reason FROM lin_audit_record WHERE action='STAFF_CREATED' AND resource_type='USER' AND resource_id='{uid}'")
    assert password not in audit and '$2a$' not in audit and '$2b$' not in audit
    status,disabled=raw(f'/admin/users/{uid}/status',{'active':False,'reason':'本机QA结束停用合成员工'},admin,'PATCH');assert disabled.get('code',status)==200
    status,denied=raw('/logistics',token=session['token']);assert denied.get('code',status) in [401,403]
    print(json.dumps({'userId':uid,'concurrentCreateCodes':codes,'oneUser':True,'onlyCourierRole':True,'hospitalScopeAbsent':True,'doctorRegistrationUnaffected':True,'staffCannotCreateStaff':True,'adminRoleCreationDenied':True,'auditExcludesCredentials':True,'normalLogin':True,'disabledAtEnd':True,'oldSessionRevoked':True},indent=2))
finally:
    if uid and setup.config.sql(f"SELECT COUNT(*) FROM lin_user WHERE id={uid} AND username='{username}' AND status='normal'")=='1':
        status,value=raw(f'/admin/users/{uid}/status',{'active':False,'reason':'本机员工测试收尾'},admin,'PATCH')
        assert value.get('code',status)==200
