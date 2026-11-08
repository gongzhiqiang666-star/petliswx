<?php
// Existing local synthetic order, real private bridge auth and read-only DB/template access.
define('APP_PATH','/srv/petlis/application/');require '/srv/petlis/thinkphp/base.php';\think\App::initCommon();
$order=\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',23956)->find();if(!$order || strpos($order['AnimalName'],'QA_REAL_')!==0)throw new \RuntimeException('Synthetic order only');
$current=\think\Db::name('shipment')->where('order_id',23956)->where('is_current',1)->find();$history=\think\Db::name('shipment')->where('order_id',23956)->order('attempt_no desc')->select();if(count($history)!==2 || (int)$current['attempt_no']!==2 || (int)$history[1]['id']!==70)throw new \RuntimeException('Wrong current/history rows');
$snapshot=(new \app\admin\library\PetlisApiBridge(4))->shipmentSnapshot(23956);if((int)$snapshot['id']!==(int)$current['id'] || (int)$snapshot['attemptNo']!==2)throw new \RuntimeException('PHP and Java disagree about current attempt');
foreach($history as &$row){$row['statusLabel']=$row['status']==='CANCELLED'?'已取消':$row['status'];}unset($row);
$html=file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');$start=strpos($html,' {if condition="isset($shipmentHistory)');$end=strpos($html,' {if condition="$shipment && $shipment.status == \'DELIVERED\'"}',$start);if($start===false || $end===false)throw new \RuntimeException('Missing history panel');
$template=new \think\Template(['cache_path'=>'/srv/petlis/runtime/temp/','layout_on'=>false]);ob_start();$template->display(substr($html,$start,$end-$start),['shipmentHistory'=>$history]);$out=ob_get_clean();foreach([$history[0]['tracking_no'],$history[1]['tracking_no'],'当前运单','历史运单','已取消'] as $expected){if(strpos($out,htmlentities($expected))===false && strpos($out,$expected)===false)throw new \RuntimeException('Real history not rendered');}
echo "PASS: PHP real two-attempt history, shared current ID and template output\n";
