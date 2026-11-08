"""Execute the unchanged pure title helper with a synthetic model adapter, no database/framework."""
from pathlib import Path
import re,json,subprocess,hashlib,base64
root=Path(__file__).resolve().parents[2];source=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application/common.php');text=source.read_text()
m=re.search(r'\bfunction\s+GenterMailTitle\s*\(',text);start=m.start();opening=text.index('{',m.end());depth=1;end=opening+1
while depth:
 if text[end]=='{':depth+=1
 elif text[end]=='}':depth-=1
 end+=1
row={'InfoAnimalAnimalName':'猫','AnimalType':'短毛','AnimalName':' 糖 糖 ','MasterName':'王医生','UserNO':' ABC '}
cases=[{'project':row,'override':v} for v in ['',None,'0','0.0',',,自定义,,,',' ,保留空格, ','已指定标题']]
cases.append({'project':dict(row,InfoAnimalAnimalName=None,AnimalType='',AnimalName=',宠物',MasterName='',UserNO='X'),'override':''})
payload=base64.b64encode(json.dumps(cases,ensure_ascii=False).encode()).decode()
program='<?php\nclass TitleModel {function where($a){return $this;}function find(){return (object)$GLOBALS["project"];}} function model($name){return new TitleModel();}\n'+text[start:end]+'\n$cases=json_decode(base64_decode("'+payload+'"),true);foreach($cases as $case){$GLOBALS["project"]=$case["project"];echo json_encode(["input"=>$case,"output"=>GenterMailTitle(1,$case["override"])],JSON_UNESCAPED_UNICODE),PHP_EOL;}'
r=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4'],input=program,text=True,capture_output=True,check=True);rows=[json.loads(x) for x in r.stdout.splitlines()];assert len(rows)==len(cases)
(root/'docs/php-parity/title-reference.json').write_text(json.dumps({'source':'application/common.php','sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runtime':'existing PHP7.4; original helper; synthetic model adapter only','rows':rows,'completionClaimed':False},ensure_ascii=False,indent=2)+'\n');print(json.dumps({'originalTitleCases':len(rows),'businessWrites':False}))
