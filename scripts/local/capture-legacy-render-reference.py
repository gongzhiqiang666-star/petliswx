"""Original filterContents with synthetic read adapters and an explicit test-only URL adapter.
No framework, business writes, external requests or real encryption keys are used.
"""
from pathlib import Path
import hashlib,json,re,base64,subprocess,copy
root=Path(__file__).resolve().parents[2];source=Path('/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn/application/common.php');text=source.read_text()
def extract(name):
 m=re.search(r'\bfunction\s+'+name+r'\s*\(',text);start=m.start();opening=text.index('{',m.end());depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 return text[start:end]
rows={'ListDiagnosisProject':{'InfoHospitalId':1,'DictSexId':1,'InfoAnimalId':1,'InfoSampleTypeId':1,'AnimalName':'{医院名}','When':'2025-02-03 04:05:06','Zero':'0','Nullable':None,'True':True,'False':False},'InfoHospital':{'HospitalName':'测试医院'},'DictSex':{'SexName':'雄'},'InfoAnimal':{'AnimalName':'猫'},'InfoSampleType':{'SampleName':'血样'},'VwDiagnosisDetail':{'TestName':'PCR','Amount':'125.50'}}
bindings=[]
for table,column,label in [('ListDiagnosisProject','AnimalName','宠物'),('ListDiagnosisProject','When','日期'),('ListDiagnosisProject','Nullable','空值'),('ListDiagnosisProject','Zero','零'),('ListDiagnosisProject','True','真'),('ListDiagnosisProject','False','假'),('InfoHospital','HospitalName','医院名'),('DictSex','SexName','性别'),('InfoAnimal','AnimalName','物种'),('InfoSampleType','SampleName','采样类型'),('VwDiagnosisDetail','TestName','项目'),('VwDiagnosisDetail','Amount','金额')]:bindings.append({'table':table,'column':column,'description':label})
cases=[]
for body in ['{报告链接}','X{报告链接}','{用户付款链接}','X{用户付款链接}','{宠物}|{医院名}|{物种}|{性别}|{采样类型}|{项目}|{金额}','{日期}|{当前时间}|{空值}|{零}|{真}|{假}|{未知}','----分页符号----{诊断项名称}','X{报告链接}{报告链接}','\\u732b {宠物}',None]:
 for typ in [1,2]:cases.append({'body':body,'type':typ,'rows':copy.deepcopy(rows),'bindings':bindings,'diagnosticNames':'检测A,检测B'})
for date in ['1900-01-01 00:00:00','1970-01-01 00:00:00','1970-01-01 08:00:00','1970-01-01 08:00:01','2025-02-30 00:00:00','2025-13-01 00:00:00','2025-00-01 00:00:00','2025-01-00 00:00:00','2025-01-01 24:01:00','2025/01/02 03:04:05','0000-00-00 00:00:00','not a date value!!!','2025-01-02T03:04:05','2025-01-02t03:04:05','2025.01.02 03:04:05','02.01.2025 03:04:05','01/02/2025 03:04:05','02-01-2025 03:04:05','2025-01-02 03:04:60','2025-01-02 25:00:00']:
 row=copy.deepcopy(rows);row['ListDiagnosisProject']['When']=date;cases.append({'body':'{日期}|{诊断项名称}','type':2,'rows':row,'bindings':bindings,'diagnosticNames':None})
adapter=r'''
date_default_timezone_set('PRC');
class Env {static function get($key){return 'https://petlis.invalid/';}}
function encrypt_url($id){return 'TEST-'.$id;}
class RowAdapter {private $data;function __construct($data){$this->data=$data;}function toArray(){return $this->data;}}
function model($name){return new QueryAdapter('lb_listdiagnosisproject',true);}
class Db {static function name($name){return new QueryAdapter($name);}static function table($name){return new QueryAdapter($name);}}
class QueryAdapter {
 private $name;private $isModel;private $where=[];private $aggregate=false;
 function __construct($name,$isModel=false){$this->name=$name;$this->isModel=$isModel;}
 function where($values){$this->where=$values;return $this;}
 function field($fields){$this->aggregate=strpos($fields,'GROUP_CONCAT')!==false;return $this;}
 function select(){ $r=[];foreach($GLOBALS['case']['bindings'] as $b)if($b['table']==$this->where['BindTable'])$r[]=['ColumnName'=>$b['column'],'DescName'=>$b['description']];return $r; }
 function find(){
  if($this->aggregate){if($this->where['InfoTestTypeId'][1]!==246)throw new Exception('Original exclusion changed');return ['test_name'=>$GLOBALS['case']['diagnosticNames']];}
  $map=['lb_listdiagnosisproject'=>'ListDiagnosisProject','lb_infohospital'=>'InfoHospital','lb_dictsex'=>'DictSex','lb_infoanimal'=>'InfoAnimal','lb_infosampletype'=>'InfoSampleType','vwdiagnosisdetail'=>'VwDiagnosisDetail'];
  $r=$GLOBALS['case']['rows'][$map[$this->name]]??[];return $this->isModel?new RowAdapter($r):$r;
 }
}
'''
payload=base64.b64encode(json.dumps(cases,ensure_ascii=False).encode()).decode()
program='<?php\n'+adapter+'\n'+extract('decodeUnicode')+'\n'+extract('filterContents')+'\n$cases=json_decode(base64_decode("'+payload+'"),true);foreach($cases as $case){$GLOBALS["case"]=$case;$today=date("Y-m-d");$output=filterContents(1,$case["body"],7,$case["type"]);if($today!==date("Y-m-d"))throw new Exception("Date rollover");$case["today"]=$today;$case["url"]="https://petlis.invalid/Project/View/Detail.aspx?CID=TEST-7";echo json_encode(["input"=>$case,"output"=>$output],JSON_UNESCAPED_UNICODE|JSON_INVALID_UTF8_SUBSTITUTE),PHP_EOL;}'
r=subprocess.run(['docker','exec','-i','petlis-legacy-php-preview','php7.4'],input=program,text=True,capture_output=True,check=True);result=[json.loads(x) for x in r.stdout.splitlines()];assert len(result)==len(cases)
(root/'docs/php-parity/render-reference.json').write_text(json.dumps({'source':'application/common.php','sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runtime':'Original filterContents + decodeUnicode, synthetic reads; test-only URL/encryption adapter','rows':result,'realEncryptionVerified':False,'realDatabaseAggregationVerified':False,'completionClaimed':False},ensure_ascii=False,indent=2)+'\n');print(json.dumps({'originalRenderCases':len(result),'businessWrites':False,'realEncryptionVerified':False}))
