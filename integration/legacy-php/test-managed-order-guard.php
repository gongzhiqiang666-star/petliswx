<?php
// Isolated local model test. Always rolls back; never invokes a browser login or external API.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', 23846)->find();
if (!$order || strpos($order['AnimalName'], 'petlis_qa_') !== 0) { throw new \RuntimeException('Expected synthetic fixture only'); }
\think\Db::startTrans();
$blocked = false;
try {
    $model = \app\admin\model\LbListdiagnosisproject::get(23846);
    $model->save(['SampleStatus' => 1]);
} catch (\think\exception\ValidateException $error) {
    $blocked = strpos($error->getMessage(), '共享工作流') !== false;
} finally {
    \think\Db::rollback();
}
if (!$blocked) { throw new \RuntimeException('Legacy generic update was not blocked'); }
$current = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', 23846)->find();
if ((int)$current['SampleStatus'] !== (int)$order['SampleStatus']) { throw new \RuntimeException('Fixture changed unexpectedly'); }
echo "PASS: managed order generic update blocked; original state preserved\n";
\think\Db::startTrans();
try {
    $blocked = false;
    try { $model = \app\admin\model\LbListdiagnosisproject::get(23846); $model->save(['ListDiagnosisProjectId' => 2100000000]); }
    catch (\think\exception\ValidateException $e) { $blocked = true; }
    if (!$blocked) { throw new \RuntimeException('Primary ID reassignment bypassed guard'); }
    $detail = \think\Db::name('lb_diagnosisdetail')->where('ListDiagnosisProjectId', 23846)->find();
    if (!$detail) { throw new \RuntimeException('Expected synthetic detail fixture'); }
    foreach ([['TestPrice' => 0], ['ListDiagnosisProjectId' => 0]] as $fields) {
        $blocked = false;
        try { $model = \app\admin\model\LbDiagnosisdetail::get($detail['DiagnosisDetailId']); $model->save($fields); }
        catch (\think\exception\ValidateException $e) { $blocked = true; }
        if (!$blocked) { throw new \RuntimeException('Detail update/reassignment bypassed guard'); }
    }
} finally { \think\Db::rollback(); }
echo "PASS: original order ID and detail parent/price cannot bypass shared workflow\n";
