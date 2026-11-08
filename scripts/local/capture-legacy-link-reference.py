"""Legacy link algorithms with ONLY the hardcoded key dependency replaced by a public test key.
No production phrase, derived key, credentials or real links are written to the repository.
"""
from pathlib import Path
import re,json,hashlib,subprocess,base64
root=Path(__file__).resolve().parents[2];source=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application/common.php');text=source.read_text()
def extract(name):
 m=re.search(r'\bfunction\s+'+name+r'\s*\(',text);start=m.start();opening=text.index('{',m.end());depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 value=text[start:end]
 if name in ['encrypt_url','decrypt_url']:
  value,n=re.subn(r'\$key\s*=\s*"[^"\n]*";',"$key = getenv('PETLIS_REFERENCE_PHRASE');",value,count=1);assert n==1
 return value
inputs=['0','1','7','1225','2147483647','4294967295','0007','AB-12_ Z','中文','A猫B','\\u4e2d','\\u00d8','']
payload=base64.b64encode(json.dumps(inputs,ensure_ascii=False).encode()).decode()
program='<?php\n'+'\n'.join(extract(n) for n in ['DDMMD5Encrypt','repalce_url','repalce','unicode_decode','encrypt_url','decrypt_url'])+'\n$cases=json_decode(base64_decode("'+payload+'"),true);foreach($cases as $value){$token=encrypt_url($value);echo json_encode(["input"=>$value,"token"=>$token,"decoded"=>decrypt_url($token)],JSON_UNESCAPED_UNICODE|JSON_INVALID_UTF8_SUBSTITUTE),PHP_EOL;}'
r=subprocess.run(['docker','exec','-i','-e','PETLIS_REFERENCE_PHRASE=petlis-public-test-key','petlis-legacy-php-preview','php7.4'],input=program,text=True,capture_output=True,check=True)
rows=[json.loads(x) for x in r.stdout.splitlines()];assert len(rows)==len(inputs)
(root/'docs/php-parity/link-reference.json').write_text(json.dumps({'source':'application/common.php','sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runtime':'PHP7.4 original algorithms; public test-key dependency adapter only','testPhrase':'petlis-public-test-key','rows':rows,'productionKeyIncluded':False,'completionClaimed':False},ensure_ascii=False,indent=2)+'\n');print(json.dumps({'legacyLinkCases':len(rows),'productionKeyIncluded':False,'businessWrites':False}))
