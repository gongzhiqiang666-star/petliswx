<?php
// Local service integration test, not an HTTP login bypass. Only freshly prepared synthetic orders are accepted.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if (!$order || strpos($order['AnimalName'], 'QA_REAL_') !== 0 || (int)$order['SampleStatus'] !== 0) { throw new \RuntimeException('Expected fresh synthetic pending order only'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$bridge->execute('review-pass', $id, ['reason' => '合成数据桥接与触发器去重验证']);
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if ((int)$order['SampleStatus'] !== 2) { throw new \RuntimeException('Expected monthly order waiting for report'); }
$legacy = \think\Db::name('lb_notification')->where(['order_id' => $id, 'new_status' => 2])->order('id desc')->find();
$count = $legacy ? \think\Db::name('notification_event')->where(['user_id' => $order['CreateSampleId'], 'event_type' => 'order', 'business_key' => 'legacy:' . $legacy['id']])->count() : 0;
if ((int)$count !== 1) { throw new \RuntimeException('Expected one event linked to original trigger ID'); }
echo 'PASS: PHP adapter approved synthetic order #' . $id . "; shared state and trigger-key dedup verified\n";
