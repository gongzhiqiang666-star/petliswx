"""Actual PC reship form -> official SF sandbox third attempt; bounded to synthetic order 23956."""
import importlib.util,json,subprocess,time
from pathlib import Path
spec=importlib.util.spec_from_file_location('staff',Path(__file__).with_name('setup-php-lab.py'));staff=importlib.util.module_from_spec(spec);spec.loader.exec_module(staff)
root=Path.home()/'.local/share/petlis';path=root/'qa/sf-reship-third-ui.json';assert not path.exists(),'Inspect existing third-attempt checkpoint instead of dispatching again'
prior=json.loads((root/'qa/sf-reship-official.json').read_text());assert prior['completed'] and prior['gateRestoredDisabled'];assert staff.config.sql("SELECT COUNT(*) FROM lin_sf_request WHERE order_id=23956 AND attempt_no=3")=='0'
admin=staff.administrator(4);binary=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def b(*args):
 r=subprocess.run([binary,*args],capture_output=True,text=True);assert r.returncode==0,'Petlis reship browser action failed';return r.stdout.strip()
def wait(expr):
 for _ in range(120):
  if b('js',expr)=='true':return
  time.sleep(.15)
 raise AssertionError('Reship form did not reach expected state; inspect durable request, never resubmit blindly')
def gate(value,reason):
 version=int(staff.config.sql("SELECT version FROM lin_operational_config WHERE config_key='sf-reship' AND scope_type='GLOBAL' AND scope_id=0"));import urllib.request
 req=urllib.request.Request(staff.config.BASE+'/admin/operations/configs/sf-reship',data=json.dumps({'value':{'enabled':value,'maxAttempts':3},'version':version,'reason':reason}).encode(),headers={'Authorization':'Bearer '+admin,'Content-Type':'application/json'},method='PUT')
 with staff.opener.open(req,timeout=15) as response:result=json.load(response)
 assert result['code']==200

def lookup():
 b('click','#sf-status');wait("!!document.querySelector('[name=orderId]')");b('fill','[name=orderId]','23956');b('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('.modal-body')&&document.querySelector('.modal-body').textContent.includes('顺丰已确认取消')")
if not b('url').startswith('http://127.0.0.1:4173/'):b('newtab','http://127.0.0.1:4173/#/logistics')
else:b('goto','http://127.0.0.1:4173/#/logistics');b('reload')
if b('js',"!!document.querySelector('#login-form')")=='true':
 php="$c=require '/srv/petlis/.petlis-private/bridge.php';echo json_encode($c['admins'][4]);";r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;account=json.loads(r.stdout);b('fill','[name=username]',account['username']);b('fill','[name=password]',account['password']);b('click','#login-form button[type=submit]');wait("!document.querySelector('#login-form')");b('goto','http://127.0.0.1:4173/#/logistics')
wait("!!document.querySelector('#sf-status')");lookup();assert b('js',"!document.querySelector('[data-sf-reship]')")=='true';b('click','.modal-footer [data-action=close-modal]')
state={'orderId':23956,'oldShipmentIds':[70,prior['shipmentId']],'defaultDisabledEntryHidden':True,'sandboxOnly':True,'completed':False};staff.save_private(path,state)
try:
 gate(True,'仅本机合成订单23956通过PC界面验证第三次官方沙箱寄件，isDocall固定0')
 # Actual PHP bridge reads the same enabled conditions; no PHP dispatch here.
 php="define('APP_PATH','/srv/petlis/application/');require '/srv/petlis/thinkphp/base.php';\\think\\App::initCommon();echo json_encode((new \\app\\admin\\library\\PetlisApiBridge(4))->reshipOptions(23956));"
 r=subprocess.run(['docker','exec','petlis-legacy-php-preview','php7.4','-r',php],capture_output=True,text=True);assert r.returncode==0;options=json.loads(r.stdout);assert options['enabled'] and options['canReship'] and options['previousCreateRequestId']==prior['createRequestId'];state['phpSamePredecessorAndGate']=True
 lookup();wait("!!document.querySelector('[data-sf-reship]')");b('click','[data-sf-reship]');wait("!!document.querySelector('[name=sender_contact]')")
 for side,contact,mobile,county,address in [('sender','沙箱寄件测试','13800000000','南山区','科技园测试地址1号'),('recipient','沙箱收件测试','13900000000','福田区','沙箱测试地址2号')]:
  for key,value in {'contact':contact,'mobile':mobile,'province':'广东省','city':'深圳市','county':county,'address':address}.items():b('fill',f'[name={side}_{key}]',value)
 b('fill','[name=reason]',' ');b('click','[type=submit][form=active-dialog-form]');wait("document.querySelector('#dialog-error').textContent.includes('再次寄件原因')");assert b('js',"document.querySelector('[name=sender_contact]').value==='沙箱寄件测试'")=='true';state['invalidReasonPreservesContacts']=True
 b('fill','[name=reason]','PC界面官方沙箱第三次寄件与PHP报告接续验收');b('screenshot','/private/tmp/petlis-sf-third-form.png');(root/'qa/sf-third-form.png').write_bytes(Path('/private/tmp/petlis-sf-third-form.png').read_bytes());state['dispatchStarted']=True;staff.save_private(path,state)
 b('click','[type=submit][form=active-dialog-form]');wait("!document.querySelector('#active-dialog-form')")
 row=staff.config.sql("SELECT r.id,s.id,r.state,r.external_order_no,r.platform_code,r.business_code FROM lin_sf_request r JOIN lin_shipment s ON s.order_id=r.order_id AND s.attempt_no=r.attempt_no AND s.tracking_no=r.tracking_no WHERE r.order_id=23956 AND r.attempt_no=3 AND r.operation='CREATE' AND s.is_current=1").split('\t');assert len(row)==6 and row[2:] == ['SUCCEEDED','PETLIS_SANDBOX_23956_A3','A1000','S0000']
 state.update({'createRequestId':int(row[0]),'shipmentId':int(row[1]),'officialCreateConfirmed':True,'completed':True,'realCourierPickupRequested':False});staff.save_private(path,state)
 b('screenshot','/private/tmp/petlis-sf-third-created.png');(root/'qa/sf-third-created.png').write_bytes(Path('/private/tmp/petlis-sf-third-created.png').read_bytes())
finally:
 gate(False,'PC第三次官方沙箱寄件验证结束，恢复默认关闭；保留已发请求和历次运单')
 state['gateRestoredDisabled']=True;staff.save_private(path,state)
print(json.dumps(state,ensure_ascii=False))
