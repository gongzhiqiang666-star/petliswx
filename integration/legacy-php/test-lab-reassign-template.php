<?php
// Pure template contract for versioned reassignment; no authentication or business writes.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$html = file_get_contents('/srv/petlis/application/admin/view/petlis_workflow/index.html');
$start = strpos($html, '{volist name="tasks" id="reassignTask"}');
$end = strpos($html, '<div class="panel panel-default"><div class="panel-heading">物流异常处理', $start);
if ($start === false || $end === false) { throw new \RuntimeException('Missing reassignment panel'); }
$content = substr($html, $start, $end - $start);
$template = new \think\Template(['cache_path' => '/srv/petlis/runtime/temp/', 'layout_on' => false]);
$vars = ['csrf' => 'synthetic-csrf', 'order' => ['ListDiagnosisProjectId' => 123], 'assignmentHistory' => [], 'assignees' => [['id' => 6, 'username' => '合成检测人员']], 'tasks' => [['id' => 9, 'version' => 3, 'status' => 'TESTING', 'hasResults' => false, 'assigned_to' => 6, 'equipment_code' => '<script>设备</script>']]];
ob_start(); $template->display($content, $vars); $testing = ob_get_clean();
if (strpos($testing, 'name="task_version" value="3"') === false || strpos($testing, 'value="lab-reassign"') === false || strpos($testing, '<script>') !== false || strpos($testing, '&lt;script&gt;') === false) { throw new \RuntimeException('Version/form/equipment escaping mismatch'); }
$vars['tasks'][0]['hasResults'] = true;
ob_start(); $template->display($content, $vars); $result = ob_get_clean();
if (strpos($result, 'value="lab-reassign"') !== false) { throw new \RuntimeException('Existing result must hide reassignment'); }
echo "PASS: PHP reassignment version, active task guard and escaped equipment rendered\n";
