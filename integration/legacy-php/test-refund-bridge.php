<?php
// Local synthetic cash refund only. Uses normal ADMIN bridge and backend mock-only refund policy.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if (!$order || strpos($order['AnimalName'], 'QA_REAL_') !== 0 || (int)$order['IsPayMoney'] !== 1) { throw new \RuntimeException('Expected paid synthetic order'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$params = ['reason' => '核实合成标本异常后模拟退款', 'refundRequestKey' => hash('sha256', 'php-synthetic-refund:' . $id)];
$bridge->execute('mock-refund', $id, $params);
$bridge->execute('mock-refund', $id, $params);
$refund = \think\Db::name('audit_record')->where(['resource_type' => 'MOCK_REFUND', 'resource_id' => (string)$id, 'action' => 'MOCK_REFUND_COMPLETED'])->select();
if (count($refund) !== 1 || strpos($refund[0]['reason'], 'PHP管理员#4：') !== 0 || (int)\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->value('IsPayMoney') !== 0) { throw new \RuntimeException('Refund identity, replay or paid flag mismatch'); }
echo "PASS: PHP cash mock refund, shared audit identity and replay verified\n";
