<?php
// Pure command boundary tests. No database, credentials, login, or carrier calls.
require '/srv/petlis/application/admin/library/PetlisApiBridge.php';
function check($value, $message) { if (!$value) { throw new \RuntimeException($message); } }
function rejected($fn) { try { $fn(); } catch (\RuntimeException $e) { return; } throw new \RuntimeException('Expected rejection'); }
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'review-pass', 23846, ['reason' => '验证中文说明']);
check($path === '/review/orders/23846' && $body['approved'] === true, 'Wrong review request');
check($body['reason'] === 'PHP管理员#4：验证中文说明', 'Legacy actor or unicode reason lost');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'review-reject', 1, ['reason' => ' ']); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'https://untrusted.example', 1, []); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'review-pass', -1, []); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'review-pass', 1, ['reason' => str_repeat('字', 401)]); });
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'sf-label', 23850, []);
check($path === '/logistics/sf/orders/23850/label' && $body instanceof \stdClass, 'Wrong label request');
echo "PASS: fixed API routes, legacy actor attribution, UTF-8 reason and invalid commands verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-receive', 123, ['shipmentId' => 9, 'barcode' => ' SAMPLE-123 ']);
check($path === '/logistics/9/scan-receive' && $body['barcode'] === 'SAMPLE-123', 'Wrong receipt command');
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-assign', 123, ['assigneeId' => 7, 'equipmentCode' => ' PCR ']);
check($path === '/lab/tasks/order/123/assign' && $body['equipmentCode'] === 'PCR', 'Wrong assignment command');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-receive', 1, ['shipmentId' => 9, 'barcode' => ' ']); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-assign', 1, ['assigneeId' => 7, 'equipmentCode' => str_repeat('字', 65)]); });
echo "PASS: receipt and assignment command boundaries verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'report-approve', 123, ['workflowId' => 9, 'reason' => '已核对']);
check($path === '/report-workflows/9/review' && $body['decision'] === 'APPROVE' && strpos($body['reason'], 'PHP管理员#4') === 0, 'Wrong report review request');
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'report-publish', 123, ['workflowId' => 9]);
check($path === '/report-workflows/9/publish' && $body instanceof \stdClass, 'Wrong report publish request');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'report-reject', 1, ['workflowId' => 9, 'reason' => ' ']); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'report-publish', 1, ['workflowId' => '9/review']); });
echo "PASS: report actions and input boundaries verified\n";

$rows = [['orderItemId' => 8, 'name' => '指标', 'value' => '5', 'unit' => 'mg', 'referenceRange' => '3-8', 'abnormalFlag' => false]];
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-results', 123, ['taskId' => 9, 'results' => $rows]);
check($path === '/lab/tasks/9/results' && $body['results'] === $rows, 'Result fields changed');
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'report-generate', 123, ['taskId' => 9, 'summary' => ' 摘要 ', 'conclusion' => ' 结论 ']);
check($path === '/report-workflows' && $body['summary'] === '摘要' && $body['taskId'] === 9, 'Wrong report generation');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-results', 1, ['taskId' => 9, 'results' => []]); });
rejected(function () use ($rows) { $rows[0]['abnormalFlag'] = 'false'; \app\admin\library\PetlisApiBridge::command(4, 'lab-results', 1, ['taskId' => 9, 'results' => $rows]); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'report-generate', 1, ['taskId' => 9, 'summary' => '', 'conclusion' => '结论']); });
echo "PASS: result and report generation input boundaries verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-return', 123, ['taskId' => 9, 'reason' => '标本异常']);
check($path === '/lab/tasks/9/return' && $body['reason'] === 'PHP管理员#4：标本异常', 'Wrong return request');
list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-retest', 123, ['taskId' => 9, 'reason' => '复核后复检']);
check($path === '/lab/tasks/9/retest', 'Wrong retest request');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-return', 1, ['taskId' => 9, 'reason' => ' ']); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-retest', 1, ['taskId' => -9, 'reason' => '复检']); });
echo "PASS: return and retest require valid task and explicit reason\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'legacy-adopt', 123, ['laboratoryId' => 2]);
check($path === '/legacy/orders/123/adopt' && $body['laboratoryId'] === 2, 'Wrong legacy adoption route');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'legacy-adopt', 1, ['laboratoryId' => 0]); });
echo "PASS: legacy adoption requires explicit laboratory\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'legacy-submit', 123, ['confirmedAmount' => '88.00', 'laboratoryId' => 2, 'doctorId' => 7]);
check($path === '/legacy/orders/123/submit' && $body['doctorId'] === 7, 'Wrong prepared submission');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'legacy-submit', 123, ['laboratoryId' => 2, 'doctorId' => 0]); });
echo "PASS: prepared submission requires explicit doctor\n";

rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'legacy-submit', 123, ['laboratoryId' => 2, 'doctorId' => 7, 'confirmedAmount' => '88.001']); });
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'legacy-submit', 123, ['laboratoryId' => 2, 'doctorId' => 7, 'confirmedAmount' => '-88.00']); });
echo "PASS: prepared submission amount precision verified\n";

$clinicalFields = array_fill_keys(['animalName','animalNameEn','animalWeight','chipNO','age','sampleArea','masterName','masterNameEn','doctorName','remark','medicalHistory','clinicalSymptoms','diagnosis','medication'], '');
list($path,$body) = \app\admin\library\PetlisApiBridge::command(4,'clinical-submit',123,['clinicalVersion'=>str_repeat('a',64),'clinicalFields'=>$clinicalFields,'reason'=>'已核实']);
check($path === '/legacy/orders/123/clinical' && $body['resubmit'] === true, 'Wrong clinical request');
$clinicalFields['TestFee'] = '0';
rejected(function () use ($clinicalFields) { \app\admin\library\PetlisApiBridge::command(4,'clinical-save',123,['clinicalVersion'=>str_repeat('a',64),'clinicalFields'=>$clinicalFields,'reason'=>'已核实']); });
echo "PASS: clinical correction fixed route, explicit resubmission and protected financial fields verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'exception-resolve', 123, ['shipmentId' => 9, 'exceptionEventId' => 71, 'reason' => '已核实处理']);
check($path === '/logistics/9/exceptions/71/resolve' && $body['reason'] === 'PHP管理员#4：已核实处理', 'Wrong exception resolution command');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'exception-resolve', 123, ['shipmentId' => 9, 'exceptionEventId' => 71, 'reason' => ' ']); });
echo "PASS: exception resolution command identity and boundaries verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'mock-refund', 123, ['refundRequestKey' => 'refund-request-123', 'reason' => '异常退款']);
check($path === '/admin/refunds/123' && $body['reason'] === 'PHP管理员#4：异常退款' && $body['requestKey'] === 'refund-request-123', 'Wrong refund command');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'mock-refund', 123, ['refundRequestKey' => '', 'reason' => '原因']); });
echo "PASS: cash mock refund fixed route, request key and actor identity verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'lab-reassign', 123, ['taskId' => 9, 'taskVersion' => 2, 'assigneeId' => 7, 'equipmentCode' => ' PCR-2 ', 'reason' => '交接']);
check($path === '/lab/tasks/9/reassign' && $body['version'] === 2 && $body['assigneeId'] === 7 && $body['equipmentCode'] === 'PCR-2' && $body['reason'] === 'PHP管理员#4：交接', 'Wrong reassignment command');
rejected(function () { \app\admin\library\PetlisApiBridge::command(4, 'lab-reassign', 123, ['taskId' => 9, 'taskVersion' => -1, 'assigneeId' => 7, 'equipmentCode' => 'PCR', 'reason' => '交接']); });
echo "PASS: laboratory reassignment version, equipment and identity verified\n";

list($path, $body) = \app\admin\library\PetlisApiBridge::command(4, 'sf-reconcile', 123, []);
check($path === '/logistics/sf/orders/123/reconcile' && $body instanceof \stdClass, 'Wrong reconciliation command');
echo "PASS: reconciliation uses fixed scoped route without resending input\n";
