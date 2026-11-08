<?php
// Normal LAB_STAFF bridge, synthetic task only; verifies PHP/new-system shared assignment.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$taskId = isset($argv[2]) ? (int)$argv[2] : 0;
$assignee = isset($argv[3]) ? (int)$argv[3] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
$task = \think\Db::name('lab_task')->where(['id' => $taskId, 'order_id' => $id])->find();
if (!$order || strpos($order['AnimalName'], 'QA_REAL_') !== 0 || !$task || $task['status'] !== 'TESTING') { throw new \RuntimeException('Expected synthetic testing task'); }
$started = $task['started_at'];
$bridge = new \app\admin\library\PetlisApiBridge(4);
$bridge->execute('lab-reassign', $id, ['taskId' => $taskId, 'taskVersion' => (int)$task['version'], 'assigneeId' => $assignee, 'equipmentCode' => 'QA_PHP_REASSIGN', 'reason' => '核对合成任务交接后改派']);
$after = \think\Db::name('lab_task')->where('id', $taskId)->find();
if ((int)$after['assigned_to'] !== $assignee || $after['equipment_code'] !== 'QA_PHP_REASSIGN' || (int)$after['version'] !== (int)$task['version'] + 1 || $after['started_at'] !== $started) { throw new \RuntimeException('Assignment or start time mismatch'); }
echo "PASS: PHP task reassignment preserves shared ID and first start time\n";
