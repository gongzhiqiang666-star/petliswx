"""Apply narrowly scoped PHP compatibility fixes to the private local preview only."""
from pathlib import Path
import shutil
root=Path.home()/'.local/share/petlis/php-preview/app'
target=root/'application/admin/controller/project/LbListdiagnosisproject.php'
old=b'"SampleStatus"=>["EGT",0]'
new=b'"SampleStatus"=>["in",[-3,0,1,2,3,4]]'
source=target.read_bytes()
if new in source:
    print('PHP order-list compatibility already applied')
elif source.count(old)==1:
    backup=Path.home()/'.local/share/petlis/php-preview/compat-backups'
    backup.mkdir(exist_ok=True)
    shutil.copy2(target,backup/'LbListdiagnosisproject.php.before-status-compat')
    target.write_bytes(source.replace(old,new))
    print('Applied: pending-payment orders visible; draft/all sentinels excluded')
else:
    raise SystemExit('PHP source differs from expected baseline; no changes made')

lang=root/'application/admin/lang/zh-cn.php'
data=lang.read_bytes()
anchor=b"'SampleStatus_list' => ["
label='"-3" => "待支付",'.encode()
if label not in data:
    if data.count(anchor)!=1:raise SystemExit('Status language baseline differs')
    backup=Path.home()/'.local/share/petlis/php-preview/compat-backups'
    backup.mkdir(exist_ok=True)
    shutil.copy2(lang,backup/'zh-cn.php.before-status-compat')
    lang.write_bytes(data.replace(anchor,anchor+b"\n        "+label,1))
    print('Applied PHP pending-payment status label')
