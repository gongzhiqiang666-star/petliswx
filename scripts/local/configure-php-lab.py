"""Configure one existing local PHP -> LAB_STAFF identity. Never creates users or grants roles."""
import argparse
import fcntl
import json
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import time
import urllib.request

PRIVATE = Path.home() / '.local/share/petlis'
TARGET = PRIVATE / 'php-preview/app/.petlis-private/laboratory-accounts.json'
BASE = 'http://127.0.0.1:8082/api'


def read_private(path):
    with Path(path).open() as source:
        info = os.fstat(source.fileno())
        if not stat.S_ISREG(info.st_mode) or info.st_uid != os.getuid() or info.st_mode & 0o077:
            raise ValueError('凭据和映射必须为当前用户拥有的0600私有文件')
        value = json.load(source)
    if not isinstance(value, dict):
        raise ValueError('配置格式无效')
    return value


def validate_account(value):
    if set(value) != {'username', 'password', 'userId'}:
        raise ValueError('凭据仅允许username、password、userId')
    if type(value['userId']) is not int or not 0 < value['userId'] <= 2147483647:
        raise ValueError('账号ID无效')
    for key in ('username', 'password'):
        if not isinstance(value[key], str) or not value[key].strip() or len(value[key]) > 256:
            raise ValueError('账号凭据无效')
    return value


def sql(statement):
    mysql = PRIVATE / 'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
    # --defaults-extra-file is first; this is the project-isolated 13306 config, never a remote DB.
    config = PRIVATE / 'mysql/root.cnf'
    text = config.read_text()
    import re
    if not re.search(r'^host\s*=\s*(127\.0\.0\.1|localhost)\s*$', text, re.M) or not re.search(r'^port\s*=\s*13306\s*$', text, re.M):
        raise ValueError('仅允许Petlis本机13306隔离库')
    result = subprocess.run([str(mysql), f'--defaults-extra-file={config}', '--batch', '--skip-column-names', 'laboklin'],
                            input=statement, text=True, capture_output=True)
    if result.returncode:
        raise ValueError('本机身份校验查询失败')
    return result.stdout.strip()


def verify_identity(legacy_id, lab_id, account):
    if type(legacy_id) is not int or type(lab_id) is not int or not 0 < legacy_id <= 2147483647 or not 0 < lab_id <= 2147483647:
        raise ValueError('管理员或实验室ID无效')
    validate_account(account)
    count = sql(f"SELECT COUNT(*) FROM lin_admin a JOIN lin_auth_group_access x ON x.uid=a.id JOIN lin_auth_group g ON g.id=x.group_id WHERE a.id={legacy_id} AND a.status='normal' AND g.status='normal' AND g.rules='*'")
    if count == '0' or not count.isdecimal():
        raise ValueError('原PHP账号不是启用的超级管理员，不可绑定此管理桥接')
    uid = account['userId']
    count = sql(f"SELECT COUNT(*) FROM lin_user_role r JOIN lin_laboratory l ON l.id=r.laboratory_id JOIN lin_user u ON u.id=r.user_id WHERE r.user_id={uid} AND r.role_code='LAB_STAFF' AND r.is_active=1 AND r.laboratory_id={lab_id} AND l.active=1 AND u.status='normal'")
    if count == '0' or not count.isdecimal():
        raise ValueError('账号未获指定启用实验室的LAB_STAFF权限')
    request = urllib.request.Request(BASE + '/auth/password/login', data=json.dumps({'account': account['username'], 'password': account['password']}).encode(), headers={'Content-Type': 'application/json'})
    # Fixed local origin, with redirects and proxies disabled: credentials must never leave this service.
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args, **kwargs):
            return None
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    try:
        with opener.open(request, timeout=15) as response:
            result = json.load(response)
        session = result['data']
        if result['code'] != 200 or type(session.get('userId')) is not int or session['userId'] != uid or 'LAB_STAFF' not in session.get('roles', []) or not session.get('token'):
            raise ValueError()
    except Exception:
        raise ValueError('实验室账号正常登录验证失败；未修改映射') from None


def write_mapping(target, legacy_id, account):
    validate_account(account)
    if type(legacy_id) is not int or not 0 < legacy_id <= 2147483647:
        raise ValueError('PHP管理员ID无效')
    target = Path(target)
    if target.is_symlink() or target.parent.is_symlink():
        raise ValueError('映射路径不可为符号链接')
    if not target.parent.is_dir():
        raise ValueError('请先安装本机PHP桥接')
    lock = target.parent / '.laboratory-accounts.lock'
    fd = os.open(lock, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, 'r+') as handle:
        fcntl.flock(handle, fcntl.LOCK_EX)
        existing = read_private(target) if target.exists() else {}
        for key, value in existing.items():
            if not key.isdecimal() or int(key) <= 0:
                raise ValueError('已有映射含无效管理员ID，未覆盖')
            validate_account(value)
        if existing.get(str(legacy_id)) == account:
            return False
        if target.exists():
            backup = target.with_name(target.name + f'.{time.time_ns()}.bak')
            with open(backup, 'x', opener=lambda name, flags: os.open(name, flags, 0o600)) as output:
                output.write(json.dumps(existing, ensure_ascii=False))
        existing[str(legacy_id)] = account
        tmp_fd, tmp_path = tempfile.mkstemp(prefix='.lab-mapping-', dir=target.parent)
        try:
            with os.fdopen(tmp_fd, 'w') as output:
                json.dump(existing, output, ensure_ascii=False)
                output.flush()
                os.fsync(output.fileno())
            os.replace(tmp_path, target)
        finally:
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--legacy-admin-id', type=int, required=True)
    parser.add_argument('--laboratory-id', type=int, required=True)
    parser.add_argument('--credentials', type=Path, required=True, help='0600 JSON: username/password/userId; never pass passwords as CLI arguments')
    parser.add_argument('--validate-only', action='store_true')
    args = parser.parse_args()
    try:
        account = validate_account(read_private(args.credentials))
        verify_identity(args.legacy_admin_id, args.laboratory_id, account)
        if args.validate_only:
            print('PASS: PHP管理员、实验室范围及正常登录验证通过；未写配置')
        else:
            changed = write_mapping(TARGET, args.legacy_admin_id, account)
            print('PASS: 本机实验室映射已保存（原配置已私有备份）' if changed else 'PASS: 本机实验室映射一致，无需改写')
    except Exception as error:
        # Never print service response bodies or subprocess stderr; either may carry sensitive context.
        print(str(error) if isinstance(error, ValueError) else '配置失败，未泄露凭据；请检查本机服务及私有文件')
        raise SystemExit(1)


if __name__ == '__main__':
    main()
