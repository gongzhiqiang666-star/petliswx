"""Run only the frozen pure decodeUnicode helper, not the PHP application or business hooks."""
import base64,hashlib,json,re,subprocess
from pathlib import Path
SOURCE=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application/common.php')
def original_decode(bodies):
 identity=subprocess.run(['docker','inspect','--format','{{.Name}} {{.Config.Image}}','petlis-legacy-php-preview'],capture_output=True,text=True,check=True).stdout.strip()
 if identity!='/petlis-legacy-php-preview petlis-legacy-php-local:7.4':raise RuntimeError('Unexpected Petlis reference container')
 text=SOURCE.read_text();match=re.search(r'\bfunction\s+decodeUnicode\s*\(',text);start=match.start();opening=text.index('{',match.end());depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 payload=base64.b64encode(json.dumps(bodies).encode()).decode()
 program='<?php\n'+text[start:end]+'\n$items=json_decode(base64_decode("'+payload+'"),true);$out=[];foreach($items as $item)$out[]=decodeUnicode($item);echo json_encode($out,JSON_UNESCAPED_UNICODE|JSON_INVALID_UTF8_SUBSTITUTE);'
 result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4','-d','memory_limit=512M'],input=program,text=True,capture_output=True,timeout=120)
 if result.returncode:raise RuntimeError('Original pure display helper failed; content withheld')
 output=json.loads(result.stdout)
 if len(output)!=len(bodies):raise RuntimeError('Original helper result count mismatch')
 return output
def source_hash():return hashlib.sha256(SOURCE.read_bytes()).hexdigest()
