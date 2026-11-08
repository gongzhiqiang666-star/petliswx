<?php
// Pure PHP template rendering; no browser, login, business write or external service.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$html = file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');
$start = strpos($html, '{if condition="$refund"}');
$end = strpos($html, '{if condition="$clinicalError"}', $start);
if ($start === false || $end === false) { throw new \RuntimeException('Missing refund panel'); }
$content = substr($html, $start, $end - $start);
$template = new \think\Template(['cache_path' => '/srv/petlis/runtime/temp/', 'layout_on' => false]);
$vars = ['refund' => null, 'extension' => ['settlement_mode' => 'cash'], 'order' => ['IsPayMoney' => 1, 'SampleStatus' => 2, 'ListDiagnosisProjectId' => 123], 'csrf' => 'synthetic-csrf', 'refundRequestKey' => 'synthetic-refund-request'];
ob_start(); $template->display($content, $vars); $pending = ob_get_clean();
if (strpos($pending, 'name="workflow_action" value="mock-refund"') === false) { throw new \RuntimeException('Paid cash order lacks refund form'); }
$vars['refund'] = ['amount' => '88.00', 'reason' => '<script>合成退款原因</script>', 'created_at' => '2026-11-08 00:00:00'];
ob_start(); $template->display($content, $vars); $refunded = ob_get_clean();
if (strpos($refunded, '已退款（开发模拟）') === false || strpos($refunded, 'value="mock-refund"') !== false || strpos($refunded, '<script>') !== false || strpos($refunded, '&lt;script&gt;') === false) { throw new \RuntimeException('Refund banner, hidden action or escaping mismatch'); }
$vars['refund'] = null; $vars['extension']['settlement_mode'] = 'monthly';
ob_start(); $template->display($content, $vars); $monthly = ob_get_clean();
if (strpos($monthly, 'value="mock-refund"') !== false) { throw new \RuntimeException('Monthly account shows cash refund action'); }
echo "PASS: PHP paid/refunded/monthly refund panels and escaped reason rendered\n";
