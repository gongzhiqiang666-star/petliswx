<?php
// Test only a known synthetic browser order; all writes are rolled back.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = 23872;
$row = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find();
if (!$row || strpos($row['AnimalName'], 'QA_BROWSER_') !== 0) { throw new \RuntimeException('Synthetic browser fixture required'); }
foreach (['55550-23872' => '条码已被其他', '55550-' => '完整'] as $barcode => $expected) {
    \think\Db::startTrans();
    try {
        $model = \app\admin\model\LbListdiagnosisproject::get($id);
        $model->UserNO = $barcode;
        $rejected = false;
        try { $model->save(); } catch (\think\exception\ValidateException $e) { $rejected = strpos($e->getMessage(), $expected) !== false; }
        if (!$rejected) { throw new \RuntimeException('Expected barcode validation rejection'); }
    } finally { \think\Db::rollback(); }
}
$after = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->value('UserNO');
if ($after !== $row['UserNO']) { throw new \RuntimeException('Synthetic barcode changed'); }
echo "PASS: duplicate and incomplete manual barcodes blocked; original value unchanged\n";
