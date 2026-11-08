"""Run only the frozen pure PHP age helper, never bootstrap PHP or connect to a database."""
from pathlib import Path
import hashlib,json,re,subprocess
repo=Path(__file__).resolve().parents[2];root=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn');source=root/'application/index/common.php';raw=source.read_bytes();text=raw.decode();start=text.index('    function GetBirthdayByAge($age)');end=text.index('\n    }',start)+len('\n    }');function=text[start:end]
assert 'Db::' not in function and 'GetBirthdayByAge' in function and 'strtotime($strtotime)' in function
# Existing application configuration chooses the time zone; no credentials or whole config printed.
config=(root/'application/config.php').read_text();match=re.search(r"['\"]default_timezone['\"]\s*=>\s*['\"]([^'\"]+)['\"]",config);assert match;zone=match.group(1);assert zone in ('PRC','Asia/Shanghai')
cases=['0岁0个月','3岁7个月','0岁5个月','3岁','3Y7M','5M','', '1岁12个月']
code="<?php\ndate_default_timezone_set("+json.dumps(zone)+");\n"+function+"\n$start=date('Y-m-d');$rows=[];foreach(json_decode("+json.dumps(json.dumps(cases,ensure_ascii=True))+",true) as $age){$rows[]=['input'=>$age,'birthday'=>GetBirthdayByAge($age)];}if($start!==date('Y-m-d'))throw new RuntimeException('Reference crossed midnight');echo json_encode(['referenceDate'=>$start,'cases'=>$rows]);"
result=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4'],input=code.encode(),capture_output=True,check=True);data=json.loads(result.stdout);data.update(source='application/index/common.php:GetBirthdayByAge',sourceSha256=hashlib.sha256(raw).hexdigest(),timezone=zone,method='Original helper body copied into stdin without changes; only PHP date builtins; no bootstrap or DB',runtime='existing local PHP 7.4 CLI',status='reference_results_captured_java_not_yet_implemented')
(repo/'docs/php-parity/birthday-reference.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
assert hashlib.sha256(source.read_bytes()).hexdigest()==data['sourceSha256'];print(json.dumps({'referenceDate':data['referenceDate'],'cases':len(data['cases']),'sourceUnchanged':True,'noDatabaseCalls':True}))
