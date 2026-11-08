"""SF sandbox-only probe/create. Credentials and detailed responses stay outside Git."""
import argparse,base64,hashlib,json,time,uuid,urllib.parse,urllib.request
from pathlib import Path
parser=argparse.ArgumentParser()
group=parser.add_mutually_exclusive_group()
group.add_argument('--create',action='store_true')
group.add_argument('--replay-last',action='store_true')
group.add_argument('--query-last',action='store_true')
group.add_argument('--cancel-last',action='store_true')
group.add_argument('--routes-last',action='store_true')
args=parser.parse_args()
folder=Path.home()/'.local/share/petlis/sf-express'
config=json.loads((folder/'sandbox.json').read_text())
assert config.get('enabled') is True
assert config['endpoint']=='https://sfapi-sbox.sf-express.com/std/service'
order_id='PETLIS_SANDBOX_'+uuid.uuid4().hex[:20]
payload={} if not args.create else {
 'language':'zh-CN','orderId':order_id,'expressTypeId':1,'payMethod':1,
 'isDocall':0,'parcelQty':1,'totalWeight':0.1,
 'cargoDetails':[{'name':'接口联调测试件','count':1,'unit':'件','weight':0.1}],
 'contactInfoList':[
 {'contactType':1,'contact':'沙箱寄件测试','mobile':'13800000000','country':'CN','province':'广东省','city':'深圳市','county':'南山区','address':'科技园测试地址1号'},
 {'contactType':2,'contact':'沙箱收件测试','mobile':'13900000000','country':'CN','province':'广东省','city':'深圳市','county':'福田区','address':'沙箱测试地址2号'}]}
if args.replay_last or args.query_last or args.cancel_last or args.routes_last:
 previous=json.loads(max(folder.glob('create-PETLIS_SANDBOX_*.json'),key=lambda p:p.stat().st_mtime).read_text())
 assert previous['sandboxOnly'] is True
 payload=previous['request'];order_id=previous['orderId']
service='EXP_RECE_UPDATE_ORDER' if args.cancel_last else ('EXP_RECE_SEARCH_ORDER_RESP' if args.query_last else 'EXP_RECE_CREATE_ORDER')
if args.routes_last:
 service='EXP_RECE_SEARCH_ROUTES'
 detail=previous['response']['apiResultData']
 if isinstance(detail,str):detail=json.loads(detail)
 detail=detail['msgData']
 if isinstance(detail,str):detail=json.loads(detail)
 tracking=next(w['waybillNo'] for w in detail['waybillNoInfoList'] if w['waybillType']==1)
 payload={'language':'0','trackingType':'1','trackingNumber':[tracking],'methodType':'1'}
if args.query_last:payload={'orderId':order_id,'searchType':'1','language':'zh-CN'}
if args.cancel_last:payload={'orderId':order_id,'dealType':2}
msg=json.dumps(payload,ensure_ascii=False,separators=(',',':'))
timestamp=str(int(time.time()*1000))
digest=base64.b64encode(hashlib.md5((msg+timestamp+config['checkword']).encode()).digest()).decode()
request_id=str(uuid.uuid4())
body=urllib.parse.urlencode({'partnerID':config['partnerId'],'requestID':request_id,'serviceCode':service,'timestamp':timestamp,'msgDigest':digest,'msgData':msg}).encode()
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
request=urllib.request.Request(config['endpoint'],data=body,headers={'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'})
with opener.open(request,timeout=20) as response: result=json.load(response)
business=result.get('apiResultData')
if isinstance(business,str):
 try:business=json.loads(business)
 except ValueError:business={}
safe={k:result.get(k) for k in ('apiResultCode','apiErrorMsg')}
if isinstance(business,dict):
 safe['business']={k:business.get(k) for k in ('success','errorCode','errorMsg')}
 if args.cancel_last:
  detail=business.get('msgData') or {}
  if isinstance(detail,str):detail=json.loads(detail)
  safe['cancelStatus']=detail.get('resStatus')
if args.create or args.replay_last or args.query_last or args.cancel_last or args.routes_last:
 evidence={'sandboxOnly':True,'serviceCode':service,'requestId':request_id,'orderId':order_id,'request':payload,'response':result}
 file=folder/(('routes-' if args.routes_last else ('cancel-' if args.cancel_last else ('query-' if args.query_last else ('replay-' if args.replay_last else 'create-'))))+order_id+'.json');file.write_text(json.dumps(evidence,ensure_ascii=False,indent=2));file.chmod(0o600)
 safe['orderId']=order_id
 safe['evidenceFile']=file.name
print(json.dumps(safe,ensure_ascii=False))
