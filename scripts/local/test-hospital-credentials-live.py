"""Profile UI -> real Java -> isolated MySQL. Discovery list only is restricted to our fixture."""
import importlib.util,json,secrets,subprocess,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
staff.config.BASE='http://127.0.0.1:18082/api';sql=staff.config.sql
prefix='it_profile_live_'+secrets.token_hex(6)
def literal(value):return 'CONVERT(0x'+value.encode().hex()+' USING utf8mb4)'
# Read this project's existing local mapping without requiring the PHP container.
import re,os,stat
mapping=Path.home()/'.local/share/petlis/php-preview/app/.petlis-private/bridge.php'
info=mapping.stat();assert stat.S_ISREG(info.st_mode) and info.st_uid==os.getuid() and not info.st_mode&0o077
content=mapping.read_text();assert re.search(r"\b4\s*=>",content)
credentials={}
for key in ['username','password']:
 match=re.search(r"'"+key+r"'\s*=>\s*'((?:\\.|[^'])*)'",content)
 assert match,'Private mapping format unavailable'
 credentials[key]=match.group(1).replace("\\'","'").replace('\\\\','\\')
session=staff.call('/auth/password/login',{'account':credentials['username'],'password':credentials['password']})
assert 'ADMIN' in session['roles'];token=session['token']
old=sql("SELECT config_value FROM lin_operational_config WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0")
assert not old or json.loads(old).get('enabled') is False,'Existing active maintenance session; will not override'
hospital=None
try:
 hospital=int(sql('INSERT INTO lin_lb_infohospital(HospitalName,HospitalName_en,PayType,amount,LoginName,LoginPwd,IsActive) VALUES('+literal(prefix)+','+literal(prefix)+",0,37.21,'historical-login','historical-secret',1);SELECT LAST_INSERT_ID()"))
 sql("INSERT INTO lin_operational_config(config_key,config_value,description,updated_by) VALUES('legacy-hospital-profiles',"+literal(json.dumps({'enabled':True,'writeEnabled':True,'credentialWriteEnabled':True}))+",'profile UI fixture',(SELECT MIN(id) FROM lin_user)) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)")
 run=subprocess.run(['node','scripts/editor-parity/hospital-credentials-live.js'],cwd=ROOT,input=json.dumps({'token':token,'hospital':hospital,'prefix':prefix}),capture_output=True,text=True,timeout=120)
 if run.returncode:raise AssertionError('Profile browser failed: '+run.stderr[-1400:])
 proof=json.loads(run.stdout)
 assert sql(f"SELECT CONCAT(HospitalName,'|',PayType,'|',amount,'|',LoginName,'|',LoginPwd,'|',IsActive) FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}")==prefix+'|0|37.21|changed-login||1'
 assert sql("SELECT COUNT(*) FROM lin_audit_record WHERE resource_type='LEGACY_HOSPITAL_PROFILE' AND CAST(resource_id AS UNSIGNED)="+str(hospital))=='3'
 audit=sql("SELECT after_json FROM lin_audit_record WHERE resource_type='LEGACY_HOSPITAL_PROFILE' AND CAST(resource_id AS UNSIGNED)="+str(hospital))
 assert 'synthetic-new-password' not in audit and 'historical-secret' not in audit
 proof['credentialFinalStateAndRedactedAuditsVerified']=True

finally:
 if old:sql('UPDATE lin_operational_config SET config_value='+literal(old)+" WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0")
 else:sql("DELETE FROM lin_operational_config WHERE config_key='legacy-hospital-profiles' AND scope_type='GLOBAL' AND scope_id=0 AND description='profile UI fixture'")
 if hospital:
  assert sql(f'SELECT HospitalName FROM lin_lb_infohospital WHERE InfoHospitalId={hospital}') in [prefix,prefix+'_edited']
  sql(f"DELETE FROM lin_audit_record WHERE resource_type='LEGACY_HOSPITAL_PROFILE' AND CAST(resource_id AS UNSIGNED)={hospital}")
  sql(f'DELETE FROM lin_lb_infohospital WHERE InfoHospitalId={hospital} AND HospitalName IN ('+literal(prefix)+','+literal(prefix+'_edited')+')')

proof.update(gateRestored=True,fixturesRemoved=True)
print(json.dumps(proof))
