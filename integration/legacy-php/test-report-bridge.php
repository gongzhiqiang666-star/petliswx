<?php
// Service test only. Fresh synthetic pending reports; same HTTP API as the authenticated PHP forms.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$workflowId = isset($argv[2]) ? (int)$argv[2] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
$report = \think\Db::name('report_workflow')->where(['id' => $workflowId, 'order_id' => $id])->find();
if (!$order || (strpos($order['AnimalName'], 'QA_REAL_') !== 0 && strpos($order['AnimalName'], 'QA_LEGACY_') !== 0) || !$report || $report['status'] !== 'PENDING_REVIEW') { throw new \RuntimeException('Expected fresh synthetic pending report'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$rejected = false;
try { $bridge->execute('report-approve', $id + 1, ['workflowId' => $workflowId]); } catch (\RuntimeException $e) { $rejected = true; }
if (!$rejected) { throw new \RuntimeException('Foreign order report accepted'); }
$rejected = false;
try { $bridge->execute('report-publish', $id, ['workflowId' => $workflowId]); } catch (\RuntimeException $e) { $rejected = true; }
if (!$rejected) { throw new \RuntimeException('Unreviewed report published'); }
$bridge->execute('report-approve', $id, ['workflowId' => $workflowId, 'reason' => '合成报告内容审核']);
$bridge->execute('report-publish', $id, ['workflowId' => $workflowId]);
$bridge->execute('report-publish', $id, ['workflowId' => $workflowId]);
$report = \think\Db::name('report_workflow')->where('id', $workflowId)->find();
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if ($report['status'] !== 'PUBLISHED' || (int)$order['SampleStatus'] !== 3) { throw new \RuntimeException('Published state not shared'); }
$count = \think\Db::name('notification_event')->where(['event_type' => 'report', 'business_key' => $id . ':published'])->count();
if ((int)$count !== 1) { throw new \RuntimeException('Duplicate publish notification'); }
echo "PASS: PHP report ownership, review gate, publication and replay verified\n";
