<?php
// Verify recovery remains available before a carrier result has linked any local shipment.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$html = file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');
$start = strpos($html, ' <div class="panel panel-default"><div class="panel-heading">物流与顺丰');
$end = strpos($html, ' {if condition="$shipment && $shipment.status == \'DELIVERED\'"}', $start);
if ($start === false || $end === false) { throw new \RuntimeException('Missing logistics panel'); }
$content = substr($html, $start, $end - $start);
$template = new \think\Template(['cache_path' => '/srv/petlis/runtime/temp/', 'layout_on' => false]);
$vars = ['shipment' => null, 'labels' => [], 'sfRequests' => [['operationLabel'=>'创建寄件','stateLabel'=>'结果待确认','platform_code'=>'','business_code'=>'','updated_at'=>'2026-11-08 00:00:00']], 'order'=>['ListDiagnosisProjectId'=>123], 'csrf'=>'<invalid-csrf>'];
ob_start(); $template->display($content, $vars); $unknown = ob_get_clean();
if (strpos($unknown, 'value="sf-reconcile"') === false || strpos($unknown, 'value="sf-cancel"') !== false || strpos($unknown, '<invalid-csrf>') !== false || strpos($unknown, '&lt;invalid-csrf&gt;') === false) { throw new \RuntimeException('Unlinked unknown request must expose only scoped, escaped recovery form'); }
$vars['shipment']=['id'=>123,'tracking_no'=>'SYNTHETIC','status'=>'CANCELLED','statusLabel'=>'已取消'];
ob_start(); $template->display($content, $vars); $cancelled = ob_get_clean();
if (strpos($cancelled, 'value="sf-reconcile"') === false || strpos($cancelled, 'value="sf-cancel"') !== false || strpos($cancelled, 'value="sf-label"') !== false) { throw new \RuntimeException('Cancelled request actions mismatch'); }
$vars['sfRequests']=[];
ob_start(); $template->display($content, $vars); $absent = ob_get_clean();
if (strpos($absent, 'value="sf-reconcile"') !== false) { throw new \RuntimeException('Recovery shown without any carrier request'); }
echo "PASS: unlinked unknown/cancelled/no-request recovery forms and CSRF escaping verified\n";
