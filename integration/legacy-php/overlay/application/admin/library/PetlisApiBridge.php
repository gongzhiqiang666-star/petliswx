<?php
// PETLIS_LOCAL_WORKFLOW: this adapter never grants roles or bypasses the Java API authorization.
namespace app\admin\library;
use think\Db;

class PetlisApiBridge
{
    const BASE = 'http://host.lima.internal:8082/api';
    private $actor;
    private $token;
    private $legacyAdminId;
    private $adminActor;
    private $adminToken;
    public function __construct($legacyAdminId)
    {
        $this->legacyAdminId = (int)$legacyAdminId;
        $file = dirname(rtrim(APP_PATH, DIRECTORY_SEPARATOR)) . '/.petlis-private/bridge.php';
        if (!is_file($file)) { throw new \RuntimeException('本机联调账号映射未配置'); }
        $config = require $file;
        if (empty($config['enabled']) || empty($config['admins'][(int)$legacyAdminId])) { throw new \RuntimeException('当前PHP管理员尚未关联联调账号'); }
        $account = $config['admins'][(int)$legacyAdminId];
        $login = $this->http('/auth/password/login', ['account' => $account['username'], 'password' => $account['password']], null);
        if (empty($login['token']) || (int)$login['userId'] !== (int)$account['userId'] || !in_array('ADMIN', $login['roles'])) { throw new \RuntimeException('关联账号身份或管理员权限不匹配'); }
        $this->actor = (int)$login['userId'];
        $this->token = $login['token'];
        $this->adminActor = $this->actor;
        $this->adminToken = $this->token;
    }
    public static function command($legacyAdminId, $action, $orderId, array $params)
    {
        if (!is_int($orderId) || $orderId <= 0 || $orderId > 2147483647) { throw new \RuntimeException('送检单编号无效'); }
        $input = isset($params['reason']) ? $params['reason'] : '';
        if (!is_string($input) || mb_strlen($input, 'UTF-8') > 400) { throw new \RuntimeException('操作说明无效或过长'); }
        $params['reason'] = $input;
        $reason = 'PHP管理员#' . (int)$legacyAdminId . '：' . trim($input);
        switch ($action) {
            case 'sf-reship':
                $previous = isset($params['previousCreateRequestId']) ? $params['previousCreateRequestId'] : 0;
                if (!is_int($previous) || $previous <= 0 || trim($input) === '') { throw new \RuntimeException('请选择前次寄件请求并填写再次寄件原因'); }
                $contacts = [];
                foreach (['sender', 'recipient'] as $side) {
                    $value = isset($params[$side]) ? $params[$side] : [];
                    if (!is_array($value)) { throw new \RuntimeException('寄收件资料无效'); }
                    $contact = [];
                    foreach (['contact'=>100,'mobile'=>11,'province'=>50,'city'=>50,'county'=>50,'address'=>200] as $key=>$limit) {
                        if (!isset($value[$key]) || !is_string($value[$key]) || trim($value[$key]) === '' || mb_strlen($value[$key], 'UTF-8') > $limit) { throw new \RuntimeException('请完整填写有效寄收件资料'); }
                        $contact[$key] = trim($value[$key]);
                    }
                    if (!preg_match('/^1[3-9][0-9]{9}$/D', $contact['mobile'])) { throw new \RuntimeException('寄收件手机号无效'); }
                    $contacts[$side] = $contact;
                }
                $path = '/logistics/sf/orders/' . $orderId . '/reship';
                $body = ['previousCreateRequestId'=>$previous,'reason'=>$reason,'sender'=>$contacts['sender'],'recipient'=>$contacts['recipient']];
                break;
            case 'mock-refund':
                $key = isset($params['refundRequestKey']) ? $params['refundRequestKey'] : '';
                if (!is_string($key) || !preg_match('/^[A-Za-z0-9_-]{8,100}$/D', $key) || trim($input) === '') { throw new \RuntimeException('退款请求号和原因不能为空'); }
                $path = '/admin/refunds/' . $orderId;
                $body = ['requestKey' => $key, 'reason' => $reason];
                break;
            case 'exception-resolve':
                $shipmentId = isset($params['shipmentId']) ? $params['shipmentId'] : 0;
                $eventId = isset($params['exceptionEventId']) ? $params['exceptionEventId'] : 0;
                if (!is_int($shipmentId) || $shipmentId <= 0 || !is_int($eventId) || $eventId <= 0 || trim($input) === '') { throw new \RuntimeException('请选择当前运单的异常并填写处理说明'); }
                $path = '/logistics/' . $shipmentId . '/exceptions/' . $eventId . '/resolve';
                $body = ['reason' => $reason];
                break;
            case 'clinical-save': case 'clinical-submit':
                $version = isset($params['clinicalVersion']) ? $params['clinicalVersion'] : '';
                $fields = isset($params['clinicalFields']) ? $params['clinicalFields'] : [];
                $allowed = ['animalName','animalNameEn','animalWeight','chipNO','age','sampleArea','masterName','masterNameEn','doctorName','remark','medicalHistory','clinicalSymptoms','diagnosis','medication'];
                if (!is_string($version) || !preg_match('/^[0-9a-f]{64}$/D', $version) || !is_array($fields) || count($fields) !== count($allowed) || trim($input) === '' || mb_strlen($reason, 'UTF-8') > 400) { throw new \RuntimeException('请刷新资料并填写修正原因'); }
                foreach ($fields as $key => $value) {
                    $limit = in_array($key, ['medicalHistory','clinicalSymptoms','diagnosis','medication'], true) ? 2000 : 250;
                    if (!in_array($key, $allowed, true) || !is_string($value) || mb_strlen($value, 'UTF-8') > $limit) { throw new \RuntimeException('临床资料字段无效或过长'); }
                }
                $path = '/legacy/orders/' . $orderId . '/clinical';
                $body = ['version' => $version, 'fields' => $fields, 'reason' => $reason, 'resubmit' => $action === 'clinical-submit'];
                break;
            case 'legacy-submit': case 'legacy-adopt':
                $laboratoryId = isset($params['laboratoryId']) ? $params['laboratoryId'] : 0;
                if (!is_int($laboratoryId) || $laboratoryId <= 0) { throw new \RuntimeException('请选择接收实验室'); }
                $path = '/legacy/orders/' . $orderId . ($action === 'legacy-submit' ? '/submit' : '/adopt');
                $body = ['laboratoryId' => $laboratoryId];
                if ($action === 'legacy-submit') {
                    $doctorId = isset($params['doctorId']) ? $params['doctorId'] : 0;
                    if (!is_int($doctorId) || $doctorId <= 0) { throw new \RuntimeException('请选择所属医院医生'); }
                    $body['doctorId'] = $doctorId;
                    $amount = isset($params['confirmedAmount']) ? $params['confirmedAmount'] : '';
                    if (!is_string($amount) || !preg_match('/^\d{1,10}(?:\.\d{1,2})?$/D', $amount)) { throw new \RuntimeException('请确认原明细合计金额'); }
                    $body['confirmedAmount'] = $amount;
                }
                break;
            case 'review-pass': case 'review-reject':
                if ($action === 'review-reject' && trim($params['reason']) === '') { throw new \RuntimeException('驳回时必须填写原因'); }
                $path = '/review/orders/' . (int)$orderId;
                $body = ['approved' => $action === 'review-pass', 'reason' => $reason];
                break;
            case 'lab-reassign':
                $taskId = isset($params['taskId']) ? $params['taskId'] : 0;
                $version = isset($params['taskVersion']) ? $params['taskVersion'] : -1;
                $assignee = isset($params['assigneeId']) ? $params['assigneeId'] : 0;
                $equipment = isset($params['equipmentCode']) ? $params['equipmentCode'] : '';
                if (!is_int($taskId) || $taskId <= 0 || !is_int($version) || $version < 0 || !is_int($assignee) || $assignee <= 0 || !is_string($equipment) || trim($equipment) === '' || mb_strlen($equipment, 'UTF-8') > 64 || trim($input) === '') { throw new \RuntimeException('请刷新任务，选择人员设备并填写改派原因'); }
                $path = '/lab/tasks/' . $taskId . '/reassign';
                $body = ['assigneeId' => $assignee, 'equipmentCode' => trim($equipment), 'version' => $version, 'reason' => $reason];
                break;
            case 'lab-return': case 'lab-retest':
                $taskId = isset($params['taskId']) ? $params['taskId'] : 0;
                if (!is_int($taskId) || $taskId <= 0 || trim($input) === '') { throw new \RuntimeException('请选择检测任务并填写原因'); }
                $path = '/lab/tasks/' . $taskId . ($action === 'lab-return' ? '/return' : '/retest');
                $body = ['reason' => $reason];
                break;
            case 'lab-results': case 'report-generate':
                $taskId = isset($params['taskId']) ? $params['taskId'] : 0;
                if (!is_int($taskId) || $taskId <= 0) { throw new \RuntimeException('检测任务编号无效'); }
                if ($action === 'lab-results') {
                    $results = isset($params['results']) ? $params['results'] : null;
                    if (!is_array($results) || !$results || count($results) > 200) { throw new \RuntimeException('检测结果无效'); }
                    foreach ($results as $row) {
                        if (!is_array($row) || !isset($row['orderItemId']) || !is_int($row['orderItemId']) || $row['orderItemId'] <= 0) { throw new \RuntimeException('结果项目编号无效'); }
                        foreach (['name', 'value', 'unit', 'referenceRange'] as $key) {
                            if (!isset($row[$key]) || !is_string($row[$key]) || trim($row[$key]) === '' || mb_strlen($row[$key], 'UTF-8') > 500) { throw new \RuntimeException('结果字段不能为空且最多500字'); }
                        }
                        if (!array_key_exists('abnormalFlag', $row) || !is_bool($row['abnormalFlag'])) { throw new \RuntimeException('必须明确选择异常标记'); }
                    }
                    $path = '/lab/tasks/' . $taskId . '/results';
                    $body = ['results' => $results];
                } else {
                    foreach (['summary', 'conclusion'] as $key) {
                        if (!isset($params[$key]) || !is_string($params[$key]) || trim($params[$key]) === '' || mb_strlen($params[$key], 'UTF-8') > 2000) { throw new \RuntimeException('样本摘要和结论必填，最多2000字'); }
                    }
                    $path = '/report-workflows';
                    $body = ['taskId' => $taskId, 'summary' => trim($params['summary']), 'conclusion' => trim($params['conclusion'])];
                }
                break;
            case 'report-approve': case 'report-reject': case 'report-publish':
                $workflowId = isset($params['workflowId']) ? $params['workflowId'] : 0;
                if (!is_int($workflowId) || $workflowId <= 0) { throw new \RuntimeException('报告工作流编号无效'); }
                if ($action === 'report-reject' && trim($input) === '') { throw new \RuntimeException('驳回报告必须填写原因'); }
                $path = '/report-workflows/' . $workflowId . ($action === 'report-publish' ? '/publish' : '/review');
                $body = $action === 'report-publish' ? new \stdClass() : ['decision' => $action === 'report-approve' ? 'APPROVE' : 'REJECT', 'reason' => $reason];
                break;
            case 'lab-receive':
                $shipmentId = isset($params['shipmentId']) ? $params['shipmentId'] : 0;
                $barcode = isset($params['barcode']) ? $params['barcode'] : '';
                if (!is_int($shipmentId) || $shipmentId <= 0 || !is_string($barcode) || trim($barcode) === '' || strlen($barcode) > 128) { throw new \RuntimeException('运单或采样条码无效'); }
                $path = '/logistics/' . $shipmentId . '/scan-receive';
                $body = ['barcode' => trim($barcode)];
                break;
            case 'lab-assign':
                $assigneeId = isset($params['assigneeId']) ? $params['assigneeId'] : 0;
                $equipment = isset($params['equipmentCode']) ? $params['equipmentCode'] : '';
                if (!is_int($assigneeId) || $assigneeId <= 0 || !is_string($equipment) || trim($equipment) === '' || mb_strlen($equipment, 'UTF-8') > 64) { throw new \RuntimeException('请选择检测人员并填写设备编号（最多64字）'); }
                $path = '/lab/tasks/order/' . $orderId . '/assign';
                $body = ['assigneeId' => $assigneeId, 'equipmentCode' => trim($equipment)];
                break;
            case 'sf-cancel': case 'sf-routes': case 'sf-label': case 'sf-reconcile':
                $suffix = ['sf-cancel' => 'cancel', 'sf-routes' => 'routes/refresh', 'sf-label' => 'label', 'sf-reconcile' => 'reconcile'][$action];
                $path = '/logistics/sf/orders/' . (int)$orderId . '/' . $suffix;
                $body = new \stdClass();
                break;
            default: throw new \RuntimeException('不支持的联调操作');
        }
        return [$path, $body];
    }
    public function execute($action, $orderId, array $params)
    {
        $legacyAdminId = $this->legacyAdminId;
        $this->actor = $this->adminActor;
        $this->token = $this->adminToken;
        if (in_array($action, ['lab-receive', 'lab-assign', 'lab-reassign', 'lab-results', 'report-generate', 'lab-return', 'lab-retest'], true)) {
            $file = dirname(rtrim(APP_PATH, DIRECTORY_SEPARATOR)) . '/.petlis-private/laboratory-accounts.json';
            $accounts = is_file($file) ? json_decode(file_get_contents($file), true) : [];
            $account = isset($accounts[(string)$legacyAdminId]) ? $accounts[(string)$legacyAdminId] : null;
            if (!$account) { throw new \RuntimeException('当前PHP管理员尚未关联实验室操作账号'); }
            $login = $this->http('/auth/password/login', ['account' => $account['username'], 'password' => $account['password']], null);
            if (empty($login['token']) || (int)$login['userId'] !== (int)$account['userId'] || $login['roles'] !== ['LAB_STAFF']) { throw new \RuntimeException('实验室账号身份或权限不匹配'); }
            $this->actor = (int)$login['userId'];
            $this->token = $login['token'];
        }
        if (in_array($action, ['lab-results', 'report-generate', 'lab-return', 'lab-retest', 'lab-reassign'], true)) {
            $taskId = isset($params['taskId']) ? $params['taskId'] : 0;
            if (!is_int($taskId) || $taskId <= 0 || !Db::name('lab_task')->where(['id' => $taskId, 'order_id' => $orderId])->find()) { throw new \RuntimeException('任务不属于当前送检单'); }
        }
        if ($action === 'exception-resolve') {
            $shipment = Db::name('shipment')->where('order_id', $orderId)->where('is_current', 1)->field('id')->find();
            if (!$shipment) { throw new \RuntimeException('该送检单尚无运单'); }
            $params['shipmentId'] = (int)$shipment['id'];
        }
        if ($action === 'lab-receive') {
            $shipment = Db::name('shipment')->where('order_id', $orderId)->where('is_current', 1)->field('id')->find();
            if (!$shipment) { throw new \RuntimeException('该送检单尚无运单'); }
            $params['shipmentId'] = (int)$shipment['id'];
        }
        if (in_array($action, ['report-approve', 'report-reject', 'report-publish'], true)) {
            $workflowId = isset($params['workflowId']) ? $params['workflowId'] : 0;
            if (!is_int($workflowId) || $workflowId <= 0 || !Db::name('report_workflow')->where(['id' => $workflowId, 'order_id' => $orderId])->find()) { throw new \RuntimeException('报告不属于当前送检单'); }
        }
        list($path, $body) = self::command($legacyAdminId, $action, $orderId, $params);
        $audit = ['legacyAdminId' => (int)$legacyAdminId, 'operation' => $action, 'state' => 'SENDING'];
        if (isset($params['taskId'])) { $audit['taskId'] = (int)$params['taskId']; }
        if (isset($params['workflowId'])) { $audit['workflowId'] = (int)$params['workflowId']; }
        $id = Db::name('audit_record')->insertGetId(['actor_user_id' => $this->actor, 'action' => 'LEGACY_ADMIN_ACTION', 'resource_type' => 'ORDER', 'resource_id' => (string)(int)$orderId, 'reason' => json_encode($audit)]);
        try {
            $result = $this->http($path, $body, $this->token);
            $audit['state'] = 'CONFIRMED';
            Db::name('audit_record')->where('id', $id)->update(['reason' => json_encode($audit)]);
            return $result;
        } catch (\Exception $error) {
            $audit['state'] = 'CHECK_REQUIRED';
            Db::name('audit_record')->where('id', $id)->update(['reason' => json_encode($audit)]);
            throw $error;
        }
    }
    public function clinicalSnapshot($orderId)
    {
        if (!is_int($orderId) || $orderId <= 0) { throw new \RuntimeException('送检单编号无效'); }
        return $this->http('/legacy/orders/' . $orderId . '/clinical', null, $this->adminToken);
    }
    public function reshipOptions($orderId)
    {
        if (!is_int($orderId) || $orderId <= 0) { throw new \RuntimeException('送检单编号无效'); }
        return $this->http('/logistics/sf/orders/' . $orderId . '/reship-options', null, $this->adminToken);
    }
    public function shipmentSnapshot($orderId)
    {
        if (!is_int($orderId) || $orderId <= 0) { throw new \RuntimeException('送检单编号无效'); }
        return $this->http('/logistics/orders/' . $orderId, null, $this->adminToken);
    }
    private function http($path, $data, $token)
    {
        $curl = curl_init(self::BASE . $path);
        $headers = ['Content-Type: application/json'];
        if ($token) { $headers[] = 'Authorization: Bearer ' . $token; }
        $options = [CURLOPT_HTTPHEADER => $headers, CURLOPT_RETURNTRANSFER => true, CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => 30, CURLOPT_FOLLOWLOCATION => false, CURLOPT_PROXY => ''];
        if ($data === null) { $options[CURLOPT_HTTPGET] = true; }
        else { $options[CURLOPT_POST] = true; $options[CURLOPT_POSTFIELDS] = json_encode($data); }
        curl_setopt_array($curl, $options);
        $response = curl_exec($curl);
        $status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
        $failed = curl_errno($curl);
        curl_close($curl);
        if ($failed || $response === false) { throw new \RuntimeException('接口结果尚未确认，请刷新当前状态，勿连续重复提交'); }
        $result = json_decode($response, true);
        if ($status !== 200 || !is_array($result) || !isset($result['code']) || $result['code'] !== 200) {
            $message = is_array($result) && isset($result['message']) && is_string($result['message']) ? $result['message'] : '接口未接受本次操作';
            throw new \RuntimeException($message);
        }
        return isset($result['data']) ? $result['data'] : null;
    }
}
