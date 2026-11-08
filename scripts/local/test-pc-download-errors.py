"""Real browser API helper against local binary/business-error responses, without switching the UI user."""
import json,os,subprocess,tempfile,time
from pathlib import Path
root=Path.home()/'.local/share/petlis';fixture=json.loads((root/'simulator-fixture.json').read_text());chain=json.loads((root/'qa/native-full-chain.json').read_text())
B=str(Path.home()/'.codex/skills/gstack/browse/dist/browse')
def browser(*args):
 result=subprocess.run([B,*args],capture_output=True,text=True);assert result.returncode==0,'Petlis browser download verification failed; credentials omitted';return result.stdout.strip()
if not browser('url').startswith('http://127.0.0.1:4173/'):browser('newtab','http://127.0.0.1:4173/#/reviews')
assert browser('js',"!document.querySelector('#active-dialog-form')")=='true','Do not reload an active editing dialog'
browser('reload')
source="""(async()=>{
 const {createApi}=await import('/src/api.js');const token=TOKEN,api=createApi({getToken:()=>token});
 let denied=false;try{await api.download('/platform-files/117/download')}catch(e){denied=e.code===403&&e.status===403}
 if(!denied)throw Error('Business403 was treated as file');
 const file=await api.download('/reports/REPORT/download');if(await file.blob.slice(0,5).text()!=='%PDF-')throw Error('Valid PDF missing');
 let unauthorized=0;const expired=createApi({getToken:()=>'petlis-local-qa-invalid-token',onUnauthorized:()=>unauthorized++});try{await expired.download('/reports/REPORT/download');throw Error('Invalid token accepted')}catch(e){if(e.status!==401||unauthorized!==1)throw Error('Expired session handler not called')}
 return {realBusiness403Rejected:true,realPdfBlobValidated:true,pdfBytes:file.blob.size,invalidTokenHTTP401Rejected:true,unauthorizedCallbackOnce:true,uiAccountUnchanged:true,externalRequestMade:false};
})()""".replace('TOKEN',json.dumps(fixture['session']['token'])).replace('REPORT',str(chain['reportIds'][0]))
source="window.__petlisDownloadCheck={done:false};"+source+".then(result=>{window.__petlisDownloadCheck={done:true,result}}).catch(error=>{window.__petlisDownloadCheck={done:true,error:error.message}})"
with tempfile.TemporaryDirectory(prefix='petlis-download-qa-',dir='/private/tmp') as folder:
 script=Path(folder)/'check.js';script.write_text(source);os.chmod(script,0o600)
 try:
  browser('eval',str(script))
  for _ in range(70):
   if browser('js',"!!window.__petlisDownloadCheck && window.__petlisDownloadCheck.done===true")=='true':break
   time.sleep(.15)
  else:raise AssertionError('Browser download verification not complete')
  result=json.loads(browser('js','window.__petlisDownloadCheck'));assert 'error' not in result,result.get('error');result=result['result']
 finally:browser('js','delete window.__petlisDownloadCheck;true')
(root/'qa/pc-download-error-validation.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
