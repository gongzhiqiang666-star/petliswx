<?php
// Private authenticated bridge only. Exact synthetic case supplied by the resumable local QA harness.
define('APP_PATH','/srv/petlis/application/');require '/srv/petlis/thinkphp/base.php';\think\App::initCommon();
$id=(int)$argv[1];$sid=(int)$argv[2];
$order=\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();
$s=\think\Db::name('shipment')->where(['id'=>$sid,'order_id'=>$id,'is_current'=>1])->find();
if(!$order || strpos($order['AnimalName'],'QA_RECEIPT_')!==0 || !$s || strpos($s['tracking_no'],'IT_RECEIPT_LIVE_')!==0)throw new \RuntimeException('Not the isolated receipt-date synthetic case');
$bridge=new \app\admin\library\PetlisApiBridge(4);
$read=function()use($id,$sid){$o=\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();$s=\think\Db::name('shipment')->where('id',$sid)->find();return [$o,$s];};
if($s['status']==='DELIVERED')$bridge->execute('lab-receive',$id,['barcode'=>$order['UserNO']]);
list($after,$signed)=$read();
if($signed['status']!=='RECEIVED' || strtotime($after['ReceiveSampleDt'])!==strtotime($signed['signed_at']))throw new \RuntimeException('PHP/shared receipt date mismatch');
$date=$after['ReceiveSampleDt'];$bridge->execute('lab-receive',$id,['barcode'=>$order['UserNO']]);
list($again,$second)=$read();$tasks=\think\Db::name('lab_task')->where('shipment_id',$sid)->count();
if($again['ReceiveSampleDt']!==$date || $second['signed_at']!==$signed['signed_at'] || (int)$tasks!==1)throw new \RuntimeException('Receipt replay changed date/task');
echo json_encode(['orderId'=>$id,'shipmentId'=>$sid,'phpReceiveSampleDt'=>$date,'signedAt'=>$signed['signed_at'],'taskCount'=>$tasks,'phpRepeatPreservesDate'=>true]);
