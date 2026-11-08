<?php
// Pure local template rendering. No authentication bypass, business writes or carrier calls.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$html=file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');
$start=strpos($html,' {if condition="isset($shipmentHistory)');
$end=strpos($html,' {if condition="$shipment && $shipment.status == \'DELIVERED\'"}',$start);
if($start===false || $end===false)throw new \RuntimeException('Missing shipment history block');
$template=new \think\Template(['cache_path'=>'/srv/petlis/runtime/temp/','layout_on'=>false]);
$rows=[['id'=>2,'attempt_no'=>2,'tracking_no'=>'<script>NEW</script>','statusLabel'=>'待揽收','is_current'=>1,'created_at'=>'2026-11-08 12:00:00'],['id'=>1,'attempt_no'=>1,'tracking_no'=>'OLD','statusLabel'=>'已取消','is_current'=>null,'created_at'=>'2026-11-08 11:00:00']];
ob_start();$template->display(substr($html,$start,$end-$start),['shipmentHistory'=>$rows]);$out=ob_get_clean();
foreach(['当前运单','历史运单','已取消','待揽收','&lt;script&gt;NEW&lt;/script&gt;'] as $text){if(strpos($out,$text)===false)throw new \RuntimeException('History output missing expected state');}
if(strpos($out,'<script>')!==false || strpos($out,'<form')!==false)throw new \RuntimeException('History must be escaped and read only');
echo "PASS: PHP current/history shipment states, escaping and read-only output\n";
