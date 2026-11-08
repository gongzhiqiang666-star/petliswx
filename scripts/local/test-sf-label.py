"""Generate an official sandbox PDF for one verified local synthetic shipment. No production fallback."""
from pathlib import Path
import argparse,base64,hashlib,json,os,subprocess,time,urllib.parse,urllib.request,uuid
os.umask(0o077)
parser=argparse.ArgumentParser()
parser.add_argument('order_id',type=int)
args=parser.parse_args()
root=Path.home()/'.local/share/petlis'
config=json.loads((root/'sf-express/sandbox.json').read_text())
assert config['endpoint']=='https://sfapi-sbox.sf-express.com/std/service' and config.get('enabled') is True
mysql=root/'mysql/runtime/opt/homebrew/Cellar/mysql/9.3.0/bin/mysql'
query=f"SELECT s.tracking_no FROM lin_shipment s JOIN lin_lb_listdiagnosisproject o ON o.ListDiagnosisProjectId=s.order_id JOIN lin_sf_request r ON r.order_id=s.order_id AND r.operation='CREATE' AND r.state='SUCCEEDED' AND r.tracking_no=s.tracking_no WHERE o.AnimalName LIKE 'QA_REAL_%' AND s.status<>'CANCELLED' AND s.order_id={args.order_id}"
value=subprocess.check_output([str(mysql),f'--defaults-extra-file={root}/mysql/app.cnf','--protocol=TCP','--host=127.0.0.1','--port=13306','--batch','--raw','--skip-column-names','laboklin','-e',query],text=True).strip()
assert value and '\n' not in value and value.startswith('SF'), 'Expected one synthetic sandbox shipment'
payload={'templateCode':'fm_76130_standard_'+config['partnerId'],'version':'2.0','fileType':'pdf','sync':True,'documents':[{'masterWaybillNo':value}]}
msg=json.dumps(payload,ensure_ascii=False,separators=(',',':'));stamp=str(int(time.time()*1000))
digest=base64.b64encode(hashlib.md5((msg+stamp+config['checkword']).encode()).digest()).decode()
form=urllib.parse.urlencode({'partnerID':config['partnerId'],'requestID':str(uuid.uuid4()),'serviceCode':'COM_RECE_CLOUD_PRINT_WAYBILLS','timestamp':stamp,'msgDigest':digest,'msgData':msg}).encode()
request=urllib.request.Request(config['endpoint'],data=form,headers={'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'})
with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(request,timeout=20) as response:result=json.load(response)
file=root/'sf-express'/f'label-order-{args.order_id}.json';file.write_text(json.dumps(result,ensure_ascii=False,indent=2))
business=result.get('apiResultData')
if isinstance(business,str):
 try:business=json.loads(business)
 except ValueError:business={}
def shape(v):
 if isinstance(v,dict):return {k:shape(x) for k,x in v.items()}
 if isinstance(v,list):return [shape(v[0])] if v else []
 return type(v).__name__
print(json.dumps({'platform':result.get('apiResultCode'),'business':{k:business.get(k) for k in ['success','errorCode','errorMsg']},'dataShape':shape(business.get('msgData'))},ensure_ascii=False))
