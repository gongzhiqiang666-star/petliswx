<?php
// Local synthetic shared-ID exception resolution probe; authenticates through the regular bridge.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$event = isset($argv[2]) ? (int)$argv[2] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if (!$order || strpos($order['AnimalName'], 'QA_REAL_') !== 0 || $event <= 0) { throw new \RuntimeException('Only fresh synthetic workflow test data'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$reason = '核对合成包裹后已找回标本';
$bridge->execute('exception-resolve', $id, ['exceptionEventId' => $event, 'reason' => $reason]);
$bridge->execute('exception-resolve', $id, ['exceptionEventId' => $event, 'reason' => $reason]);
$snapshot = $bridge->shipmentSnapshot($id);
$found = false;
foreach ($snapshot['exceptions'] as $row) {
    if ((int)$row['id'] === $event) {
        if (!$row['resolvedAt'] || $row['resolutionNote'] !== 'PHP管理员#4：' . $reason) { throw new \RuntimeException('Resolution identity or note missing'); }
        $found = true;
    }
}
if (!$found) { throw new \RuntimeException('Resolved event missing from shared snapshot'); }
echo "PASS: PHP shared-ID resolution, replay and new-system snapshot verified\n";
