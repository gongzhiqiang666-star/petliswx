"""Run unmodified pure legacy Base.price in a disposable PHP7.4 runtime with BCMath.
No mounted application, credentials, database, exposed ports, entrypoint or scheduled jobs.
Only the new disposable container installs the missing math extension; existing PHP remains frozen.
"""
from pathlib import Path
import hashlib,json,re,subprocess,uuid
root=Path(__file__).resolve().parents[2];reference=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application')
def extract(path,name):
 text=path.read_text();match=re.search(r'\bfunction\s+'+name+r'\s*\(',text);assert match
 start=match.start();opening=text.index('{',match.end());depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 return text[start:end]
base=reference/'admin/controller/Base.php';common=reference/'common.php'
prices=['100.00','125.50','12.59','1.005','2.675','-0.005','0.00','-125.50','9999999999999999.99','90071992547409.91']
discounts=['原价','其他折扣','10','10.00','9','7.55','1','0','-1','0.09']
cases=[{'price':p,'discount':d,'testtype':'1'} for p in prices for d in discounts]
funds=[['10.00','2.50'],['10.00','-10.00'],['10.00','-10.01'],['10.00','0.009'],['0.00','-0.009'],['1.009','0.009'],['-2.00','0.01']]
program='<?php\n'+extract(common,'showPrice')+'''\nclass RequestStub {private $values;function __construct($values){$this->values=$values;}function post($key){return $this->values[$key];}}
class LegacyReference {public $request;function success($result){return $result;}'''+extract(base,'price')+'''}
echo json_encode(['runtime'=>PHP_VERSION,'bcmath'=>extension_loaded('bcmath')]),PHP_EOL;
$cases=json_decode('''+"'"+json.dumps(cases,ensure_ascii=False)+"'"+''',true);
foreach($cases as $case){$ref=new LegacyReference();$ref->request=new RequestStub($case);echo json_encode(['kind'=>'quote','input'=>$case,'output'=>$ref->price()],JSON_UNESCAPED_UNICODE),PHP_EOL;}
$funds=json_decode('''+"'"+json.dumps(funds)+"'"+''',true);
foreach($funds as $case){echo json_encode(['kind'=>'hospital-bcadd','input'=>$case,'output'=>bcadd($case[0],$case[1],2)]),PHP_EOL;}
'''
run_label=uuid.uuid4().hex
command=['docker','run','--rm','-i','--platform','linux/amd64','--label','petlis.math-reference='+run_label,'--name','petlis-php-pricing-reference','--entrypoint','sh','petlis-legacy-php-local:7.4','-c','apt-get -o Acquire::Check-Valid-Until=false update -qq >/tmp/petlis-apt.log 2>&1 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends php7.4-bcmath=7.4.33-1+deb11u5 >>/tmp/petlis-apt.log 2>&1 && php7.4 || { tail -30 /tmp/petlis-apt.log >&2; exit 1; }']
try:
 result=subprocess.run(command,input=program,text=True,capture_output=True,timeout=240)
finally:
 owned=subprocess.run(['docker','ps','-aq','--filter','label=petlis.math-reference='+run_label],text=True,capture_output=True,check=True).stdout.split()
 for container_id in owned:
  label=subprocess.check_output(['docker','inspect','--format','{{index .Config.Labels \"petlis.math-reference\"}}',container_id],text=True).strip()
  assert label==run_label
  subprocess.run(['docker','rm','-f',container_id],check=True,capture_output=True)
if result.returncode:raise RuntimeError('Disposable PHP math reference failed; no business state changed: '+result.stderr[-1000:])
lines=[json.loads(line) for line in result.stdout.splitlines()];assert lines[0]['bcmath'] is True and len(lines)==1+len(cases)+len(funds)
output={'sourceBase':'application/admin/controller/Base.php','baseSha256':hashlib.sha256(base.read_bytes()).hexdigest(),'sourceCommon':'application/common.php','commonSha256':hashlib.sha256(common.read_bytes()).hexdigest(),'runtime':lines[0],'referenceContainer':'disposable petlis-php-pricing-reference --rm; no mounts/ports; source image unchanged','rows':lines[1:],'completionClaimed':False}
(root/'docs/php-parity/bcmath-reference.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'quoteCases':len(cases),'hospitalBcaddCases':len(funds),'originalBcMathExecuted':True,'existingPhpUnmodified':True}))
