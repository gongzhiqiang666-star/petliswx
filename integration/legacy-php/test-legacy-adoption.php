<?php
// Creates only a new synthetic legacy-form order. Never modifies the source or historical rows.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$sourceId = isset($argv[1]) ? (int)$argv[1] : 0;
$source = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $sourceId)->find();
if (!$source || strpos($source['AnimalName'], 'QA_REAL_') !== 0) { throw new \RuntimeException('Synthetic source only'); }
$extension = \think\Db::name('order_extension')->where('order_id', $sourceId)->find();
if (!$extension) { throw new \RuntimeException('Source laboratory missing'); }
$details = \think\Db::name('lb_diagnosisdetail')->where(['ListDiagnosisProjectId' => $sourceId, 'IsActive' => 1, 'IsCheckPass' => 0])->select();
if (!$details) { throw new \RuntimeException('Source details missing'); }
$doctorId = (int)$source['CreateSampleId'];
$submitPrepared = isset($argv[2]) && $argv[2] === 'submit';
if ($submitPrepared) { $source['CreateSampleId'] = 0; }
unset($source['ListDiagnosisProjectId']);
$source['AnimalName'] = 'QA_LEGACY_' . bin2hex(random_bytes(4));
$source['UserNO'] = 'QA_LEGACY_' . bin2hex(random_bytes(6));
$source['SampleStatus'] = 0;
$source['IsPayMoney'] = 0;
$source['SendReportDt'] = '1970-01-01 00:00:00';
$source['ReceiveSampleDt'] = '1970-01-01 00:00:00';
$id = \think\Db::transaction(function () use ($source, $details) {
    $newId = (int)\think\Db::name('lb_listdiagnosisproject')->insertGetId($source);
    foreach ($details as $detail) {
        unset($detail['DiagnosisDetailId']);
        $detail['ListDiagnosisProjectId'] = $newId;
        $detail['DiagnosisStatus'] = 0;
        $detail['IsSendMail'] = 0;
        $detail['PDFHttpUrl'] = '';
        $detail['SendMailDt'] = '1970-01-01 00:00:00';
        \think\Db::name('lb_diagnosisdetail')->insert($detail);
    }
    return $newId;
});
if (\think\Db::name('order_extension')->where('order_id', $id)->count()) { throw new \RuntimeException('Expected no extension before adoption'); }
$bridge = new \app\admin\library\PetlisApiBridge(4);
$rejected = false;
try { $bridge->execute('review-pass', $id, ['reason' => '合成测试：未接入不得审核']); } catch (\RuntimeException $e) { $rejected = true; }
if (!$rejected || (int)\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->value('SampleStatus') !== 0) { throw new \RuntimeException('Unadopted review changed legacy state'); }
$lab = (int)$extension['laboratory_id'];
if ($submitPrepared) {
    $failed = false;
    try { $bridge->execute('legacy-submit', $id, ['confirmedAmount' => (string)$source['TestFee'], 'doctorId' => $doctorId, 'laboratoryId' => 2147483647]); } catch (\RuntimeException $e) { $failed = true; }
    if (!$failed || (int)\think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->value('CreateSampleId') !== 0) { throw new \RuntimeException('Failed submission leaked doctor binding'); }
    $bridge->execute('legacy-submit', $id, ['confirmedAmount' => (string)$source['TestFee'], 'doctorId' => $doctorId, 'laboratoryId' => $lab]);
    $bridge->execute('legacy-submit', $id, ['confirmedAmount' => (string)$source['TestFee'], 'doctorId' => $doctorId, 'laboratoryId' => $lab]);
} else {
    $bridge->execute('legacy-adopt', $id, ['laboratoryId' => $lab]);
    $bridge->execute('legacy-adopt', $id, ['laboratoryId' => $lab]);
}
$adopted = \think\Db::name('order_extension')->where('order_id', $id)->find();
if (!$adopted || $adopted['review_status'] !== 'pending' || rtrim(rtrim($adopted['payable_amount'], '0'), '.') !== rtrim(rtrim($source['TestFee'], '0'), '.')) { throw new \RuntimeException('Adoption changed original price'); }
$bridge->execute('review-pass', $id, ['reason' => '合成旧单接入审核']);
$adopted = \think\Db::name('order_extension')->where('order_id', $id)->find();
if ($adopted['review_status'] !== 'approved' || \think\Db::name('order_extension')->where('order_id', $id)->count() != 1) { throw new \RuntimeException('Review/duplicate adoption failed'); }
echo json_encode(['passed' => true, 'orderId' => $id, 'laboratoryId' => $lab, 'amount' => $adopted['payable_amount'], 'reviewStatus' => $adopted['review_status']], JSON_UNESCAPED_UNICODE) . "\n";
