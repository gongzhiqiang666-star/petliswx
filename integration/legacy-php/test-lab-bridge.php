<?php
// Local service integration only: accepts a freshly delivered synthetic specimen, never historical orders.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$assignee = isset($argv[2]) ? (int)$argv[2] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
$shipment = \think\Db::name('shipment')->where('order_id', $id)->find();
if (!$order || (strpos($order['AnimalName'], 'QA_REAL_') !== 0 && strpos($order['AnimalName'], 'QA_LEGACY_') !== 0) || !$shipment || $shipment['status'] !== 'DELIVERED') { throw new \RuntimeException('Expected freshly delivered synthetic specimen'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
try { $bridge->execute('lab-receive', $id, ['barcode' => 'WRONG_BARCODE']); throw new \LogicException('Wrong barcode accepted'); } catch (\RuntimeException $e) { }
if (\think\Db::name('lab_task')->where('order_id', $id)->count() != 0) { throw new \RuntimeException('Invalid barcode created task'); }
$bridge->execute('lab-receive', $id, ['barcode' => $order['UserNO']]);
$bridge->execute('lab-receive', $id, ['barcode' => $order['UserNO']]);
if (\think\Db::name('lab_task')->where('order_id', $id)->count() != 1) { throw new \RuntimeException('Duplicate receipt created duplicate task'); }
if (isset($argv[5]) && $argv[5] === 'unassigned') {
    $original = (int)\think\Db::name('lab_task')->where('order_id', $id)->value('id');
    $bridge->execute('lab-return', $id, ['taskId' => $original, 'reason' => '合成测试：分配前退样']);
    $child = (int)$bridge->execute('lab-retest', $id, ['taskId' => $original, 'reason' => '合成测试：分配前复检']);
    if (\think\Db::name('lab_task')->where('id', $child)->value('status') !== 'RECEIVED') { throw new \RuntimeException('Unassigned retest must wait for assignment'); }
}
$bridge->execute('lab-assign', $id, ['assigneeId' => $assignee, 'equipmentCode' => 'PHP_QA_PCR']);
$task = \think\Db::name('lab_task')->where(['order_id' => $id, 'status' => 'TESTING'])->find();
if ($task['status'] !== 'TESTING' || (int)$task['assigned_to'] !== $assignee) { throw new \RuntimeException('Assignment not persisted'); }
echo "PASS: PHP receipt barcode validation, replay dedup and task assignment\n";

if (isset($argv[3]) && $argv[3] === 'results') {
    $items = \think\Db::name('lb_diagnosisdetail')->where(['ListDiagnosisProjectId' => $id, 'IsActive' => 1, 'IsCheckPass' => 0])->field('DiagnosisDetailId')->select();
    $rows = [];
    foreach ($items as $item) { $rows[] = ['orderItemId' => (int)$item['DiagnosisDetailId'], 'name' => 'PHP测试指标', 'value' => '5.20', 'unit' => '10^9/L', 'referenceRange' => '3.5-5.5', 'abnormalFlag' => false]; }
    $taskId = (int)$task['id'];
    $bridge->execute('lab-results', $id, ['taskId' => $taskId, 'results' => $rows]);
    $rejected = false;
    try { $bridge->execute('lab-results', $id, ['taskId' => $taskId, 'results' => $rows]); } catch (\RuntimeException $e) { $rejected = true; }
    if (!$rejected) { throw new \RuntimeException('Historical result overwritten'); }
    $generated = $bridge->execute('report-generate', $id, ['taskId' => $taskId, 'summary' => 'PHP合成摘要', 'conclusion' => 'PHP合成结论']);
    $first = (int)$generated[0];
    $bridge->execute('report-reject', $id, ['workflowId' => $first, 'reason' => '合成测试：补充结论说明']);
    $generated = $bridge->execute('report-generate', $id, ['taskId' => $taskId, 'summary' => 'PHP合成摘要第二版', 'conclusion' => 'PHP合成结论第二版']);
    if ((int)$generated[0] === $first || \think\Db::name('report_workflow')->where('id', $first)->value('status') !== 'REJECTED') { throw new \RuntimeException('Rejected report history lost'); }
    echo "PASS: PHP results, immutable replay, generation, rejection and revision verified\n";
    if (isset($argv[4]) && $argv[4] === 'retest') {
        $previous = (int)$generated[0];
        $bridge->execute('lab-return', $id, ['taskId' => $taskId, 'reason' => '合成测试：标本复核']);
        if (\think\Db::name('test_result')->where('lab_task_id', $taskId)->value('result_status') !== 'VOIDED' || \think\Db::name('report_workflow')->where('id', $previous)->value('status') !== 'CANCELLED') { throw new \RuntimeException('Return did not void result/report'); }
        $rejected = false;
        try { $bridge->execute('report-publish', $id, ['workflowId' => $previous]); } catch (\RuntimeException $e) { $rejected = true; }
        if (!$rejected) { throw new \RuntimeException('Cancelled report published'); }
        $reason = '合成测试：复核后复检';
        $child = (int)$bridge->execute('lab-retest', $id, ['taskId' => $taskId, 'reason' => $reason]);
        $replay = (int)$bridge->execute('lab-retest', $id, ['taskId' => $taskId, 'reason' => $reason]);
        if ($child !== $replay || \think\Db::name('lab_task')->where('parent_task_id', $taskId)->count() != 1) { throw new \RuntimeException('Duplicate retest task'); }
        $bridge->execute('lab-results', $id, ['taskId' => $child, 'results' => $rows]);
        $generated = $bridge->execute('report-generate', $id, ['taskId' => $child, 'summary' => 'PHP复检摘要', 'conclusion' => 'PHP复检结论']);
        echo "PASS: PHP return voids results and reports; retest is independent and deduplicated\n";
    }

}
