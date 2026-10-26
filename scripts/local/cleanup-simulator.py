# Local Petlis development integration tests only. Creates synthetic fixtures and cleans them up.
# SMS verification is a seeded fixture; no real SMS is sent. Business test payments are development fixtures.
from pathlib import Path
import urllib.request,urllib.error,json,secrets,subprocess,base64
b=Path.home()/'.local/share/petlis';base='http://127.0.0.1:8082/api'
mysql=b/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
def sql(s):return subprocess.run([str(mysql),f'--defaults-extra-file={b}/mysql/root.cnf','--batch','--skip-column-names','laboklin'],input=s.encode(),capture_output=True,check=True).stdout.decode().strip()
def call(path,data=None,token=None,method=None):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
 try:
  with urllib.request.urlopen(req) as r:return r.status,json.load(r)
 except urllib.error.HTTPError as e:return e.code,None
# Cleanup is intentionally limited to the fixture created by prepare-simulator.py.
state=b/'simulator-fixture.json'
if not state.exists():raise SystemExit('No simulator fixture to clean')
f=json.loads(state.read_text());uid=int(f['userId']);name=f['account']
assert name.startswith('petlis_qa_') and name.replace('_','').isalnum()
assert sql(f"SELECT username FROM lin_user WHERE id={uid};")==name,'Fixture identity changed; refusing cleanup'
assert sql(f"SELECT COUNT(*) FROM lin_lb_listdiagnosisproject WHERE CreateSampleId={uid};")=='0','Fixture has orders; review and clean business fixtures first'
assert sql(f"SELECT COUNT(*) FROM lin_redemption WHERE user_id={uid};")=='0','Fixture has redemptions; review business fixtures first'
if f.get('petId'):
 pet=int(f['petId']);assert sql(f"SELECT animal_name FROM lin_lb_pet WHERE id={pet};")==name+' pet','Pet fixture identity changed'
 sql(f"DELETE FROM lin_lb_pet WHERE id={pet};")
sql(f"DELETE FROM lin_order_draft WHERE user_id={uid};")
sql(f"DELETE d FROM lin_notification_delivery d JOIN lin_notification_event e ON e.id=d.event_id WHERE e.user_id={uid}; DELETE FROM lin_notification_event WHERE user_id={uid}; DELETE FROM lin_invoice_profile WHERE user_id={uid}; DELETE FROM lin_user_preferences WHERE user_id={uid}; DELETE FROM lin_doctor_profile WHERE user_id={uid}; DELETE FROM lin_user WHERE id={uid};")
Path(f['qualification']).unlink(missing_ok=True);state.unlink()
print('Synthetic simulator doctor and its test-only setup records removed')
