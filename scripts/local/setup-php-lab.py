"""Provision the Petlis-only local lab identity via normal ADMIN APIs, then install its private PHP mapping."""
import argparse
import fcntl
import importlib.util
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import urllib.request

spec = importlib.util.spec_from_file_location('petlis_lab_config', Path(__file__).with_name('configure-php-lab.py'))
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())


def call(path, body, token=None):
    request = urllib.request.Request(config.BASE + path, data=json.dumps(body).encode(), headers={'Content-Type':'application/json', **({'Authorization':'Bearer '+token} if token else {})})
    try:
        with opener.open(request, timeout=20) as response:
            result = json.load(response)
        if result.get('code') != 200:
            raise ValueError()
        return result.get('data')
    except Exception:
        raise ValueError('本机账号接口未完成，请检查服务状态；未输出请求凭据') from None


def save_private(path, value):
    if path.is_symlink():
        raise ValueError('私有配置不可为符号链接')
    fd, name = tempfile.mkstemp(prefix='.petlis-staff-', dir=path.parent)
    try:
        with os.fdopen(fd, 'w') as target:
            json.dump(value, target, ensure_ascii=False)
            target.flush()
            os.fsync(target.fileno())
        os.replace(name,path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def administrator(legacy_id):
    # Only this project's existing private preview configuration; captured in memory, never printed.
    php = "$c=require '/srv/petlis/.petlis-private/bridge.php'; if(empty($c['enabled']))exit(1); $id=(int)$argv[1]; if(empty($c['admins'][$id]))exit(1); echo json_encode($c['admins'][$id]);"
    result = subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php,str(legacy_id)],capture_output=True,text=True)
    if result.returncode:
        raise ValueError('本机PHP管理员映射不可用')
    try:
        account = config.validate_account(json.loads(result.stdout))
        session = call('/auth/password/login',{'account':account['username'],'password':account['password']})
        if session['userId'] != account['userId'] or 'ADMIN' not in session['roles']:
            raise ValueError()
        return session['token']
    except Exception:
        raise ValueError('本机管理员身份校验失败') from None


def verify(legacy_id, lab_id, account):
    config.verify_identity(legacy_id,lab_id,account)
    session=call('/auth/password/login',{'account':account['username'],'password':account['password']})
    if set(session.get('roles',[])) != {'LAB_STAFF'}:
        raise ValueError('专用员工出现额外会话权限，未覆盖PHP映射')
    uid=account['userId']
    # Dedicated workers must not silently acquire an extra role or an unrelated hospital scope.
    if config.sql(f'SELECT COUNT(*) FROM lin_user_role WHERE user_id={uid} AND is_active=1') != '1':
        raise ValueError('联调账号存在额外角色，未覆盖PHP映射')
    if config.sql(f'SELECT hospital_id IS NULL FROM lin_user WHERE id={uid}') != '1':
        raise ValueError('专用员工不应绑定医生医院，未覆盖PHP映射')
    if config.sql(f'SELECT requested_hospital_id IS NULL FROM lin_doctor_profile WHERE user_id={uid}') != '1':
        raise ValueError('专用员工不应计入医生注册申请')


def setup(lab_id=1, legacy_id=4, validate_only=False):
    if type(lab_id) is not int or type(legacy_id) is not int or not 0<lab_id<=2147483647 or not 0<legacy_id<=2147483647:
        raise ValueError('实验室或管理员ID无效')
    username=f'petlis_local_lab_{lab_id}'
    credentials=config.PRIVATE/f'lab-bridge-{lab_id}-credentials.json'
    pending=config.PRIVATE/f'.lab-bridge-{lab_id}.pending.json'
    if credentials.is_symlink() or pending.is_symlink() or config.TARGET.is_symlink():
        raise ValueError('本机身份配置不可为符号链接')
    if (config.PRIVATE/'qa/php-ui-mapping-backup.json').exists():
        raise ValueError('还有未结束的PHP浏览器临时映射，请先完成该次验收')
    # Verifies the fixed isolated DB configuration before any API or file mutation.
    if config.sql(f'SELECT COUNT(*) FROM lin_laboratory WHERE id={lab_id} AND active=1') != '1':
        raise ValueError('目标实验室未启用')
    fd=os.open(config.PRIVATE/f'.lab-bridge-{lab_id}.setup.lock',os.O_CREAT|os.O_RDWR|os.O_NOFOLLOW,0o600)
    with os.fdopen(fd,'r+') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX)
        created=False
        if credentials.exists():
            account=config.validate_account(config.read_private(credentials))
            if account['username']!=username:
                raise ValueError('现有凭据不属于此专用账号')
        else:
            if validate_only:
                raise ValueError('尚未创建本机专用联调身份')
            existing=config.sql(f"SELECT id FROM lin_user WHERE username='{username}'")
            if pending.exists():
                state=config.read_private(pending)
                if state.get('username')!=username or state.get('laboratoryId')!=lab_id or not isinstance(state.get('password'),str):
                    raise ValueError('建号恢复文件与当前目标不匹配')
            else:
                if existing:
                    raise ValueError('同名账号已存在但没有本机凭据，未重置密码或覆盖账号')
                state={'username':username,'laboratoryId':lab_id,'password':secrets.token_urlsafe(28)+'A7!'}
                save_private(pending,state)
            if existing:
                session=call('/auth/password/login',{'account':username,'password':state['password']})
                uid=session['userId']
            else:
                token=administrator(legacy_id)
                uid=call('/admin/staff',{'username':username,'displayName':'本机实验室联调','password':state['password'],'roleCode':'LAB_STAFF','laboratoryId':lab_id,'reason':'仅用于Petlis本机实验室及PHP并行联调'},token)
                created=True
            account={'username':username,'password':state['password'],'userId':uid}
            config.validate_account(account)
            verify(legacy_id,lab_id,account)
            save_private(credentials,account)
        verify(legacy_id,lab_id,account)
        if validate_only:
            current=config.read_private(config.TARGET)
            if current.get(str(legacy_id)) != account:
                raise ValueError('PHP映射与专用账号不一致')
            changed=False
        else:
            changed=config.write_mapping(config.TARGET,legacy_id,account)
            if pending.exists():
                pending.unlink()
        return {'configured':True,'username':username,'userId':account['userId'],'laboratoryId':lab_id,'legacyAdminId':legacy_id,'created':created,'mappingChanged':changed,'validateOnly':validate_only,'credentialFile':str(credentials),'passwordPrinted':False}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--laboratory-id',type=int,default=1)
    parser.add_argument('--legacy-admin-id',type=int,default=4)
    parser.add_argument('--validate-only',action='store_true')
    args=parser.parse_args()
    try:
        print(json.dumps(setup(args.laboratory_id,args.legacy_admin_id,args.validate_only),ensure_ascii=False,indent=2))
    except Exception as error:
        print(str(error) if isinstance(error,ValueError) else '专用账号配置未完成；凭据未输出，请检查本机环境')
        raise SystemExit(1)
if __name__=='__main__':
    main()
