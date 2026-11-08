<?php
// Server template contract only; does not authenticate, submit forms or touch business data.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$html = file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');
$start = strpos($html, '<div class="panel panel-default"><div class="panel-heading">物流异常处理');
$end = strpos($html, '<div class="panel panel-default"><div class="panel-heading">承运商路由记录', $start);
if ($start === false || $end === false) { throw new \RuntimeException('Missing exception panel'); }
$content = substr($html, $start, $end - $start);
$template = new \think\Template(['cache_path' => '/srv/petlis/runtime/temp/', 'layout_on' => false]);
ob_start();
$template->display($content, ['exceptionError' => '', 'csrf' => 'synthetic-csrf', 'order' => ['ListDiagnosisProjectId' => 123], 'exceptionEvents' => [
    ['id' => 71, 'exceptionType' => 'DELAY', 'typeLabel' => '超时未送达', 'note' => '<script>合成文本</script>', 'reportedAt' => '2026-11-08 00:00:00', 'resolved' => false, 'resolvedAt' => '', 'resolutionNote' => ''],
    ['id' => 72, 'exceptionType' => 'DAMAGE', 'typeLabel' => '标本破损', 'note' => '合成已处理记录', 'reportedAt' => '2026-11-08 00:00:00', 'resolved' => true, 'resolvedAt' => '2026-11-08 00:05:00', 'resolutionNote' => '已核实处理'],
]]);
$output = ob_get_clean();
if (substr_count($output, 'name="workflow_action" value="exception-resolve"') !== 1 || strpos($output, '&lt;script&gt;') === false || strpos($output, '<script>') !== false || strpos($output, '已处理：已核实处理') === false) { throw new \RuntimeException('Wrong pending/resolved or escaped template output'); }
echo "PASS: PHP pending/resolved exception forms and escaped content rendered\n";
