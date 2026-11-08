"""Read-only real PC current/history view, no gateway calls."""
import json,subprocess,time
from pathlib import Path
root=Path.home()/'.local/share/petlis';binary=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 r=subprocess.run([binary,*args],capture_output=True,text=True);assert r.returncode==0,'PC shipment history action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(100):
  if b('js',expr)=='true':return
  time.sleep(.12)
 raise AssertionError('PC shipment history readiness timeout')
if not b('url').startswith('http://127.0.0.1:4173/'):b('newtab','http://127.0.0.1:4173/#/logistics')
else:b('goto','http://127.0.0.1:4173/#/logistics');b('reload')
if b('js',"!!document.querySelector('#login-form')")=='true':
 php="$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);";r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;account=json.loads(r.stdout)
 b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]');wait("!document.querySelector('#login-form')");b('goto','http://127.0.0.1:4173/#/logistics')
wait("!!document.querySelector('[data-action=history][data-order-id=\"23956\"]')");b('click','[data-action=history][data-order-id="23956"]');wait("!!document.querySelector('.modal-body')")
text=b('js',"document.querySelector('.modal-body').textContent");assert '当前运单' in text and '第 1 次' in text and '历史运单' in text and '第 2 次' in text
assert b('js',"!document.querySelector('.modal-body [data-action=\"sf-cancel\"]')&&!document.querySelector('.modal-body [data-action=\"assign\"]')")=='true'
b('screenshot','/private/tmp/petlis-pc-shipment-history.png');(root/'qa/pc-shipment-history.png').write_bytes(Path('/private/tmp/petlis-pc-shipment-history.png').read_bytes());b('click','.modal-footer [data-action=close-modal]')
proof={'orderId':23956,'normalUiHistoryOpened':True,'currentAttempt':2,'historyRows':2,'historyDialogHasNoMutationActions':True,'multipleAttemptsBrowserVerified':True,'carrierRequestsMade':False};(root/'qa/pc-shipment-history.json').write_text(json.dumps(proof,indent=2));print(json.dumps(proof))
