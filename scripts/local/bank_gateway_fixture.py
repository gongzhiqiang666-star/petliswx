"""Owned loopback bank fixture. Generates disposable keys; never loads supplier credentials."""
import json,os,socket,threading,re
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from urllib.parse import parse_qs
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa,padding
class BankGatewayFixture:
 def __init__(self,private,prefix,fail_first_query=False,before_second_query=None):
  self.folder=private/(prefix+'_gateway');self.folder.mkdir(mode=0o700)
  self.merchant=rsa.generate_private_key(public_exponent=65537,key_size=2048);self.gateway=rsa.generate_private_key(public_exponent=65537,key_size=2048);self.prepay=0;self.queries=0;self.errors=[];self.order=None;self.prefix=prefix
  self.private_key=self.folder/'merchant.der';self.public_key=self.folder/'gateway.der'
  for path,value in [(self.private_key,self.merchant.private_bytes(serialization.Encoding.DER,serialization.PrivateFormat.PKCS8,serialization.NoEncryption())),(self.public_key,self.gateway.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo))]:
   fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL|os.O_NOFOLLOW,0o600)
   with os.fdopen(fd,'wb') as f:f.write(value)
  fixture=self
  class Handler(BaseHTTPRequestHandler):
   def log_message(self,*args):pass
   def do_GET(self):
    if self.path!='/notification' or not fixture.order:self.send_error(404);return
    response={'tradeStatus':'00','oriMerOrdrNo':fixture.order,'trxTtlAmt':'1250','chnlMerOrdrNo':fixture.prefix+'_channel','buyerLgnId':'owned-notice-buyer','subOpenId':'wrong-notice-query-buyer'}
    raw=json.dumps({'response':{'msgBody':response}},separators=(',',':')).encode();signature=fixture.gateway.sign(raw,padding.PKCS1v15(),hashes.SHA256()).hex();packet=raw[:-1]+b',"sign":"'+signature.encode()+b'"}'
    self.send_response(200);self.send_header('Content-Length',str(len(packet)));self.end_headers();self.wfile.write(packet)
   def do_POST(self):
    try:
     length=int(self.headers['Content-Length']);assert 0<length<=1024*1024
     packet=self.rfile.read(length);unsigned,signature=packet.rsplit(b'&sign=',1);fixture.merchant.public_key().verify(bytes.fromhex(signature.decode()),unsigned,padding.PKCS1v15(),hashes.SHA256())
     form=parse_qs(unsigned.decode());business=json.loads(form['biz_content'][0]);body=business['msgBody'];method=form['method'][0]
     if method.endswith('.prePay'):
      fixture.prepay+=1;assert fixture.prepay==1,'Unexpected duplicate prePay';assert body['trxTtlAmt']=='1250';assert isinstance(body['userNo'],int);fixture.order=body['merOrdrNo'];assert fixture.order.startswith('TESTLOCAL')
      self.close_connection=True;self.connection.shutdown(socket.SHUT_RDWR);self.connection.close();return
     assert method.endswith('.payQuery');fixture.queries+=1;assert body['oriMerOrdrNo']==fixture.order;assert re.fullmatch('[0-9]{14}',body['oriTrxDt'])
     if fixture.queries==2 and before_second_query:before_second_query(fixture.order)
     response={'retCode':'00000','tradeStatus':'00','oriMerOrdrNo':fixture.order,'trxTtlAmt':'1250','chnlMerOrdrNo':fixture.prefix+'_channel','subOpenId':('x'*251 if fail_first_query and fixture.queries==1 else 'owned-test-buyer')}
     raw=json.dumps({'response':{'msgBody':response}},ensure_ascii=True,separators=(',',':')).encode();signature=fixture.gateway.sign(raw,padding.PKCS1v15(),hashes.SHA256()).hex();packet=raw[:-1]+b',"sign":"'+signature.encode()+b'"}'
     self.send_response(200);self.send_header('Content-Length',str(len(packet)));self.end_headers();self.wfile.write(packet)
    except Exception as error:fixture.errors.append(type(error).__name__);self.close_connection=True
  self.server=ThreadingHTTPServer(('127.0.0.1',0),Handler);self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start();self.endpoint='http://127.0.0.1:'+str(self.server.server_port)+'/gateway'
 def close(self):
  self.server.shutdown();self.server.server_close();self.thread.join(timeout=3)
  for path in [self.private_key,self.public_key]:path.unlink()
  self.folder.rmdir()
