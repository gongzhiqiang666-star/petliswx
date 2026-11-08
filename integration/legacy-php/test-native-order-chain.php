<?php
// Native-simulator-created synthetic doctor only. No direct state writes, migrations or external carrier calls.
define('APP_PATH','/srv/petlis/application/');require '/srv/petlis/thinkphp/base.php';\think\App::initCommon();
$id=(int)$argv[1];$worker=(int)$argv[2];
$order=\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();
$user=$order?\think\Db::name('user')->where('id',$order['CreateSampleId'])->find():null;
if(!$user || !preg_match('/^petlis_qa_[a-f0-9]+$/D',$user['username']) || strpos($order['AnimalName'],$user['username'])!==0 || $worker<=0)throw new \RuntimeException('Only simulator synthetic doctor/order');
$bridge=new \app\admin\library\PetlisApiBridge(4);
$bridge->execute('lab-receive',$id,['barcode'=>$order['UserNO']]);
$task=\think\Db::name('lab_task')->where(['order_id'=>$id,'status'=>'RECEIVED'])->find();
if(!$task)throw new \RuntimeException('Shared receipt task missing');
$tid=(int)$task['id'];$bridge->execute('lab-assign',$id,['assigneeId'=>$worker,'equipmentCode'=>'NATIVE_QA_PCR']);
$items=\think\Db::name('lb_diagnosisdetail')->where(['ListDiagnosisProjectId'=>$id,'IsActive'=>1,'IsCheckPass'=>0])->select();$rows=[];
foreach($items as $item)$rows[]=['orderItemId'=>(int)$item['DiagnosisDetailId'],'name'=>'原生联调指标','value'=>'5.20','unit'=>'mg/L','referenceRange'=>'3-6','abnormalFlag'=>false];
$bridge->execute('lab-results',$id,['taskId'=>$tid,'results'=>$rows]);
$workflows=$bridge->execute('report-generate',$id,['taskId'=>$tid,'summary'=>'原生送检PHP接续摘要','conclusion'=>'本机合成验证结论']);$reports=[];
foreach($workflows as $wid){$bridge->execute('report-approve',$id,['workflowId'=>(int)$wid,'reason'=>'原生小程序全链路合成验收']);$bridge->execute('report-publish',$id,['workflowId'=>(int)$wid]);$row=\think\Db::name('report_workflow')->where('id',$wid)->find();if($row['status']!=='PUBLISHED')throw new \RuntimeException('Publication not shared');$reports[]=(int)$row['detail_id'];}
echo json_encode(['orderId'=>$id,'taskId'=>$tid,'reportIds'=>$reports,'workflowIds'=>$workflows,'phpReceiptAssignmentResultsReviewPublish'=>true],JSON_UNESCAPED_UNICODE);
