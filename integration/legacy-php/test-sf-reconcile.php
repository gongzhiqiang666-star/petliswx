<?php
// Synthetic local recovery probe: no direct database writes or carrier payload override.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if (!$order || strpos($order['AnimalName'], 'QA_REAL_') !== 0) { throw new \RuntimeException('Only synthetic workflow data'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$bridge->execute('sf-reconcile', $id, []);
$bridge->execute('sf-reconcile', $id, []);
$shipment = $bridge->shipmentSnapshot($id);
if ($shipment['status'] !== 'CANCELLED') { throw new \RuntimeException('Cancellation not restored'); }
echo "PASS: PHP durable cancellation recovery and replay verified\n";
