<?php
// Authenticated private PHP bridge, synthetic SF attempt 3. Internal courier nodes are simulated separately.
define('APP_PATH','/srv/petlis/application/');require '/srv/petlis/thinkphp/base.php';\think\App::initCommon();
$id=(int)$argv[1];$expectedShipment=(int)$argv[2];$worker=(int)$argv[3];
$order=\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();$shipment=\think\Db::name('shipment')->where(['order_id'=>$id,'is_current'=>1])->find();
if($id!==23956 || !$order || strpos($order['AnimalName'],'QA_REAL_')!==0 || !$shipment || (int)$shipment['id']!==$expectedShipment || (int)$shipment['attempt_no']!==3 || $worker!==488)throw new \RuntimeException('Only exact local synthetic third-attempt case');
$bridge=new \app\admin\library\PetlisApiBridge(4);
if($shipment['status']==='DELIVERED')$bridge->execute('lab-receive',$id,['barcode'=>$order['UserNO']]);
$shipment=\think\Db::name('shipment')->where('id',$expectedShipment)->find();if($shipment['status']!=='RECEIVED')throw new \RuntimeException('PHP receipt did not reach current shipment');
$task=\think\Db::name('lab_task')->where(['shipment_id'=>$expectedShipment,'order_id'=>$id,'task_type'=>'INITIAL'])->find();if(!$task)throw new \RuntimeException('Current shipment task missing');$taskId=(int)$task['id'];
if($task['assigned_to']===null || (int)$task['assigned_to']===0)$bridge->execute('lab-assign',$id,['assigneeId'=>$worker,'equipmentCode'=>'SF_RESHIP_QA']);
$items=\think\Db::name('lb_diagnosisdetail')->where(['ListDiagnosisProjectId'=>$id,'IsActive'=>1,'IsCheckPass'=>0])->select();$rows=[];foreach($items as $item)$rows[]=['orderItemId'=>(int)$item['DiagnosisDetailId'],'name'=>'顺丰接续合成指标','value'=>'5.20','unit'=>'mg/L','referenceRange'=>'3-6','abnormalFlag'=>false];
if(count($rows)===0)throw new \RuntimeException('Synthetic order items missing');
if(\think\Db::name('test_result')->where('lab_task_id',$taskId)->count()===0)$bridge->execute('lab-results',$id,['taskId'=>$taskId,'results'=>$rows]);
$workflows=\think\Db::name('report_workflow')->alias('w')->join('test_result r','r.id=w.test_result_id')->where('r.lab_task_id',$taskId)->column('w.id');
if(!$workflows)$workflows=$bridge->execute('report-generate',$id,['taskId'=>$taskId,'summary'=>'顺丰第三次寄件PHP接续合成摘要','conclusion'=>'仅本机功能联调，非真实诊断结论']);
$reports=[];foreach($workflows as $workflowId){$workflowId=(int)$workflowId;$workflow=\think\Db::name('report_workflow')->where('id',$workflowId)->find();if($workflow['status']==='PENDING_REVIEW')$bridge->execute('report-approve',$id,['workflowId'=>$workflowId,'reason'=>'第三次寄件后PHP审核合成报告']);$workflow=\think\Db::name('report_workflow')->where('id',$workflowId)->find();if($workflow['status']==='APPROVED')$bridge->execute('report-publish',$id,['workflowId'=>$workflowId]);$workflow=\think\Db::name('report_workflow')->where('id',$workflowId)->find();if($workflow['status']!=='PUBLISHED')throw new \RuntimeException('Report not published');$reports[]=(int)$workflow['detail_id'];}
if((int)\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->value('SampleStatus')!==3)throw new \RuntimeException('Legacy status not completed');
echo json_encode(['orderId'=>$id,'shipmentId'=>$expectedShipment,'attemptNo'=>3,'taskId'=>$taskId,'workerUserId'=>$worker,'reportIds'=>$reports,'workflowIds'=>array_map('intval',$workflows),'phpReceiptResultsReviewPublication'=>true,'legacyOrderStatus'=>3],JSON_UNESCAPED_UNICODE);
