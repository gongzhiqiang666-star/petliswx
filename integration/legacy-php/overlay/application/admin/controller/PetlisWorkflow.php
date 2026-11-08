<?php
// PETLIS_LOCAL_WORKFLOW: local preview overlay; never deploy to the reference or production copy.
namespace app\admin\controller;
use app\common\controller\Backend;
use think\Db;

/** Shared-ID workflow adapter for legacy/new workflow verification. Existing login and rights remain required. */
class PetlisWorkflow extends Backend
{
    public function index()
    {
        if (!$this->auth->isSuperAdmin()) {
            $this->error('仅管理员可访问全链路联调视图');
        }
        $id = (int)$this->request->get('id', 0, 'intval');
        if ($id <= 0) { $this->error('请提供有效送检单ID'); }
        $order = Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)
            ->field('ListDiagnosisProjectId,CreateSampleId,UserNO,AnimalName,InfoHospitalId,SampleStatus,IsPayMoney,ReceiveSampleDt,SendReportDt,AnimalType,Age,ChipNO,SampleArea,MasterName,DoctorName')->find();
        if (!$order) { $this->error('送检单不存在'); }
        $items = Db::name('lb_diagnosisdetail')->alias('d')->join('lb_infotesttype t', 't.InfoTestTypeId=d.InfoTestTypeId', 'LEFT')->where('d.ListDiagnosisProjectId', $id)->where('d.IsActive', 1)->field('d.DiagnosisDetailId,d.TestPrice,t.TestName,t.TestNO')->select();
        $extension = Db::name('order_extension')->where('order_id', $id)->find();
        $shipment = Db::name('shipment')->where('order_id', $id)->where('is_current', 1)->find();
        $shipmentHistory = Db::name('shipment')->where('order_id', $id)->field('id,tracking_no,status,attempt_no,is_current,created_at')->order('attempt_no desc')->select();
        $requestAttempt = Db::name('sf_request')->where('order_id', $id)->where('operation', 'CREATE')->max('attempt_no');
        $requests = Db::name('sf_request')->where('order_id', $id)->where('attempt_no', $requestAttempt ? (int)$requestAttempt : 1)
            ->field('operation,state,tracking_no,platform_code,business_code,updated_at')->select();
        $routes = $shipment ? Db::name('sf_route_event')->where('shipment_id', $shipment['id'])
            ->field('carrier_code,event_time,mapped_status,description,applied_at')->order('event_time asc,id asc')->select() : [];
        $tasks = Db::name('lab_task')->where('order_id', $id)->field('id,task_type,status,laboratory_id,received_at,assigned_to,equipment_code,version')->order('id')->select();
        $assignees = $shipment ? Db::name('user_role')->alias('r')->join('user u', 'u.id=r.user_id')->where(['r.role_code' => 'LAB_STAFF', 'r.is_active' => 1, 'r.laboratory_id' => $shipment['laboratory_id'], 'u.status' => 'normal'])->field('u.id,u.username')->distinct(true)->select() : [];
        $reports = Db::name('report_workflow')->where('order_id', $id)
            ->field('id,detail_id,status,published_at,pdf_file_id')->order('id')->select();
        $states = ['PENDING_PICKUP' => '待揽收', 'PICKED_UP' => '已揽收', 'IN_TRANSIT' => '运输中',
                   'COMPLETED' => '已完成', 'INITIAL' => '首次检测', 'DELIVERED' => '快递已妥投', 'RECEIVED' => '实验室已签收', 'CANCELLED' => '寄件已取消',
                   'RETURNED' => '已退样', 'RETESTED' => '已转复检', 'RESULT_ENTERED' => '结果已录入', 'TESTING' => '检测中', 'PUBLISHED' => '已发布', 'REPORT_REVIEW' => '报告审核中',
                   'PENDING_REVIEW' => '待审核', 'DRAFT' => '草稿', 'APPROVED' => '审核通过', 'REJECTED' => '未通过',
                   'RESERVED' => '请求已记录', 'SENDING' => '结果待确认', 'UNKNOWN' => '结果待确认',
                   'SUCCEEDED' => '承运商已确认'];
        if ($shipment) { $shipment['statusLabel'] = isset($states[$shipment['status']]) ? $states[$shipment['status']] : $shipment['status']; }
        foreach ($shipmentHistory as &$past) { $past['statusLabel'] = isset($states[$past['status']]) ? $states[$past['status']] : $past['status']; }
        unset($past);
        foreach ($requests as &$item) {
            $item['operationLabel'] = $item['operation'] === 'CANCEL' ? '取消寄件' : '创建寄件';
            $item['stateLabel'] = isset($states[$item['state']]) ? $states[$item['state']] : $item['state'];
        }
        unset($item);
        foreach ($tasks as &$item) { $item['hasResults'] = Db::name('test_result')->where('lab_task_id', $item['id'])->count() > 0; $item['statusLabel'] = isset($states[$item['status']]) ? $states[$item['status']] : $item['status']; }
        unset($item);
        foreach ($reports as &$item) { $item['statusLabel'] = isset($states[$item['status']]) ? $states[$item['status']] : $item['status']; }
        unset($item);
        $labels = $shipment ? Db::name('file')->where(['biz_type' => 'SF_LABEL', 'biz_id' => (string)$shipment['id']])->whereNull('deleted_at')->field('id')->select() : [];
        $canGenerate = true;
        foreach ($reports as $report) { if (in_array($report['status'], ['PENDING_REVIEW', 'APPROVED', 'PUBLISHED'], true)) { $canGenerate = false; } }
        $laboratories = !$extension ? Db::name('laboratory')->where('active', 1)->field('id,name')->order('sort_order,id')->select() : [];
        $legacyDoctors = !$extension ? Db::name('user')->where(['hospital_id' => $order['InfoHospitalId'], 'status' => 'normal'])->field('id,username,nickname')->order('id')->select() : [];
        $legacyTotal = Db::name('lb_diagnosisdetail')->where(['ListDiagnosisProjectId' => $id, 'IsActive' => 1, 'IsCheckPass' => 0])->field('COALESCE(SUM(TestPrice),0) AS amount')->find();
        $clinical = null; $clinicalError = '';
        if ($extension) {
            try { $clinical = (new \app\admin\library\PetlisApiBridge((int)$this->auth->id))->clinicalSnapshot($id); }
            catch (\Exception $error) { $clinicalError = '资料暂不可编辑：' . $error->getMessage(); }
        }
        $exceptionEvents = []; $exceptionError = '';
        if ($shipment) {
            try {
                $snapshot = (new \app\admin\library\PetlisApiBridge((int)$this->auth->id))->shipmentSnapshot($id);
                $exceptionEvents = isset($snapshot['exceptions']) ? $snapshot['exceptions'] : [];
                foreach ($exceptionEvents as &$incident) { $incident += ['resolvedAt' => '', 'resolutionNote' => '', 'resolved' => false]; $exceptionLabels = ['DAMAGE' => '标本破损', 'LOSS' => '标本丢失', 'DELAY' => '超时未送达', 'OTHER' => '其他异常']; $incident['typeLabel'] = isset($exceptionLabels[$incident['exceptionType']]) ? $exceptionLabels[$incident['exceptionType']] : $incident['exceptionType']; }
                unset($incident);
            } catch (\Exception $error) { $exceptionError = '异常处理记录暂不可用：' . $error->getMessage(); }
        }
        $refund = Db::name('audit_record')->where(['resource_type' => 'MOCK_REFUND', 'resource_id' => (string)$id, 'action' => 'MOCK_REFUND_COMPLETED'])->field('reason,created_at,after_json')->order('id desc')->find();
        if ($refund) { $snapshot = json_decode($refund['after_json'], true); $refund['amount'] = isset($snapshot['amount']) && is_numeric($snapshot['amount']) ? number_format((float)$snapshot['amount'], 2, '.', '') : '待核实'; }
        $assignmentHistory = [];
        if ($tasks) {
            $assignmentHistory = Db::name('audit_record')->where('resource_type', 'LAB_TASK')->where('resource_id', 'in', array_column($tasks, 'id'))->where('action', 'in', ['LAB_TASK_ASSIGNED', 'LAB_TASK_REASSIGNED'])->field('resource_id,action,actor_user_id,reason,before_json,after_json,created_at')->order('id desc')->select();
            foreach ($assignmentHistory as &$entry) {
                foreach (['before', 'after'] as $side) { $snapshot = json_decode($entry[$side . '_json'], true); $entry[$side . 'Label'] = is_array($snapshot) ? '人员 ' . (isset($snapshot['assignedTo']) ? '#' . $snapshot['assignedTo'] : '未分配') . ' · 设备 ' . (!empty($snapshot['equipmentCode']) ? $snapshot['equipmentCode'] : '未指定') : '历史快照缺失'; }
                $entry['actionLabel'] = $entry['action'] === 'LAB_TASK_REASSIGNED' ? '改派' : '首次分配';
            }
            unset($entry);
        }
        $reshipOptions = ['enabled'=>false,'canReship'=>false];
        try { $reshipOptions = (new \app\admin\library\PetlisApiBridge((int)$this->auth->id))->reshipOptions($id); }
        catch (\Exception $error) { /* Existing status and recovery remain readable when options are unavailable. */ }
        $result = ['reshipOptions'=>$reshipOptions, 'assignmentHistory' => $assignmentHistory, 'refund' => $refund, 'exceptionEvents' => $exceptionEvents, 'exceptionError' => $exceptionError, 'clinical' => $clinical, 'clinicalError' => $clinicalError, 'legacyTotal' => (string)$legacyTotal['amount'], 'legacyDoctors' => $legacyDoctors, 'laboratories' => $laboratories, 'canGenerate' => $canGenerate, 'assignees' => $assignees, 'items' => $items, 'labels' => $labels, 'order' => $order, 'extension' => $extension, 'shipment' => $shipment,
                   'shipmentHistory' => $shipmentHistory, 'sfRequests' => $requests, 'routes' => $routes, 'tasks' => $tasks, 'reports' => $reports];
        if ($this->request->isAjax()) { return json($result); }
        $csrf = \think\Session::get('petlis_workflow_csrf');
        if (!$csrf) { $csrf = bin2hex(random_bytes(32)); \think\Session::set('petlis_workflow_csrf', $csrf); }
        $notice = \think\Session::get('petlis_workflow_notice');
        \think\Session::delete('petlis_workflow_notice');
        $this->view->assign('operationNotice', $notice ?: '');
        $this->view->assign('csrf', $csrf);
        $this->view->assign('refundRequestKey', hash('sha256', 'refund:' . $id . ':' . $csrf));
        $this->view->assign($result);
        $this->view->assign('orderStates', [-3 => '待支付', -2 => '草稿', 0 => '待审核', 1 => '审核未通过', 2 => '等待报告', 3 => '已完成', 4 => '已取消']);
        return $this->view->fetch('petlis_workflow/index');
    }
    public function operate()
    {
        if (!$this->auth->isSuperAdmin() || !$this->request->isPost()) { return $this->operationFailure('需要管理员登录并通过表单提交', 0); }
        $csrf = $this->request->post('csrf');
        $expected = \think\Session::get('petlis_workflow_csrf');
        if (!is_string($csrf) || !$expected || !hash_equals($expected, $csrf)) { return $this->operationFailure('表单已失效，请刷新后重试', (int)$this->request->post('order_id', 0, 'intval')); }
        $id = (int)$this->request->post('order_id', 0, 'intval');
        if ($id <= 0 || !Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId', $id)->find()) { return $this->operationFailure('送检单不存在', $id); }
        $action = $this->request->post('workflow_action/s', '');
        $reason = $this->request->post('reason/s', '');
        if (mb_strlen($reason, 'UTF-8') > 400) { return $this->operationFailure('操作说明最多400字', $id); }
        try {
            $bridge = new \app\admin\library\PetlisApiBridge((int)$this->auth->id);
            $results = [];
            if ($action === 'lab-results') {
                $posted = $this->request->post('results/a', []);
                if (count($posted) > 200) { throw new \RuntimeException('结果项目过多'); }
                foreach ($posted as $detailId => $row) {
                    if (!ctype_digit((string)$detailId) || !is_array($row) || !isset($row['abnormal']) || !in_array($row['abnormal'], ['0', '1'], true)) { throw new \RuntimeException('结果项目或异常标记无效'); }
                    $results[] = ['orderItemId' => (int)$detailId, 'name' => isset($row['name']) ? $row['name'] : '', 'value' => isset($row['value']) ? $row['value'] : '',
                        'unit' => isset($row['unit']) ? $row['unit'] : '', 'referenceRange' => isset($row['reference']) ? $row['reference'] : '', 'abnormalFlag' => $row['abnormal'] === '1'];
                }
            }
            $bridge->execute($action, $id, ['reason' => $reason, 'results' => $results,
                'clinicalFields' => $this->request->post('clinical/a', []), 'clinicalVersion' => $this->request->post('clinical_version/s', ''),
                'refundRequestKey' => $this->request->post('refund_request_key/s', ''),
                'exceptionEventId' => (int)$this->request->post('exception_event_id', 0, 'intval'),
                'taskVersion' => (int)$this->request->post('task_version', -1, 'intval'),
                'taskId' => (int)$this->request->post('task_id', 0, 'intval'),
                'laboratoryId' => (int)$this->request->post('laboratory_id', 0, 'intval'),
                'doctorId' => (int)$this->request->post('doctor_id', 0, 'intval'),
                'confirmedAmount' => $this->request->post('confirmed_amount/s', ''),
                'summary' => $this->request->post('summary/s', ''), 'conclusion' => $this->request->post('conclusion/s', ''),
                'barcode' => $this->request->post('barcode/s', ''),
                'previousCreateRequestId' => (int)$this->request->post('previous_create_request_id', 0, 'intval'),
                'sender' => $this->request->post('sender/a', []), 'recipient' => $this->request->post('recipient/a', []),
                'assigneeId' => (int)$this->request->post('assignee_id', 0, 'intval'),
                'equipmentCode' => $this->request->post('equipment_code/s', ''),
                'workflowId' => (int)$this->request->post('workflow_id', 0, 'intval')]);
        } catch (\Exception $error) { return $this->operationFailure($error->getMessage(), $id); }
        $notices = ['sf-reship'=>'再次寄件请求已记录，请核对当前运单；结果未知时先查询，不要重复下单', 'sf-reconcile' => '请求核对完成，请查看请求与运单状态；结果未知时不会重新寄件或推定取消成功', 'lab-reassign' => '检测人员和设备已调整，首次开始时间保留', 'mock-refund' => '开发模拟退款已记录，原送检进度保留', 'exception-resolve' => '异常处理结果已记录，其他未处理异常继续保留', 'clinical-save' => '送检资料已修正，原状态和核价保留', 'clinical-submit' => '资料已修正并重新送审，请继续审核', 'legacy-submit' => '已绑定医生并接入待审核流程，请继续审核', 'legacy-adopt' => '旧单已接入，请核对后审核；原单号和金额保留', 'lab-return' => '已退样，原结果及未发布报告已作废', 'lab-retest' => '复检任务已建立，请在新任务录入结果', 'lab-results' => '检测结果已保存，历史结果不会覆盖', 'report-generate' => '报告PDF已生成，等待审核', 'report-approve' => '报告审核通过，尚未发布给医生', 'report-reject' => '报告已退回修改', 'report-publish' => '报告已发布，医生可查看最新报告', 'lab-receive' => '实验室签收成功，检测任务已建立', 'lab-assign' => '检测人员和设备已分配', 'review-pass' => '审核已保存', 'review-reject' => '驳回结果已保存', 'sf-label' => '面单已生成或复用，可点击下方PDF查看', 'sf-routes' => '顺丰路由查询完成，当前状态以有效节点为准', 'sf-cancel' => '取消请求已记录，请核对下方结果'];
        \think\Session::set('petlis_workflow_notice', isset($notices[$action]) ? $notices[$action] : '操作已保存');
        \think\Session::set('petlis_workflow_csrf', bin2hex(random_bytes(32)));
        if ($this->request->isAjax()) { return json(['code' => 1, 'url' => '/laboklin.php/petlis_workflow/index?id=' . $id]); }
        $this->redirect('/laboklin.php/petlis_workflow/index?id=' . $id);
    }
    private function operationFailure($message, $orderId)
    {
        if ($this->request->isAjax()) { return json(['code' => 0, 'message' => (string)$message]); }
        $this->view->assign('operationError', $message);
        $this->view->assign('returnOrderId', (int)$orderId);
        return $this->view->fetch('petlis_workflow/error');
    }
    public function report()
    {
        if (!$this->auth->isSuperAdmin()) { $this->error('仅管理员可下载联调报告'); }
        $id = $this->request->get('id', 0, 'intval');
        $report = Db::name('report_workflow')->where('id', $id)->where('status', 'in', ['PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PUBLISHED'])->find();
        if (!$report || !$report['pdf_file_id']) { $this->error('报告PDF尚未生成'); }
        $file = Db::name('file')->where(['id' => $report['pdf_file_id'], 'biz_type' => 'REPORT', 'biz_id' => (string)$id])->whereNull('deleted_at')->find();
        return $this->privatePdf($file);
    }
    public function label()
    {
        if (!$this->auth->isSuperAdmin()) { $this->error('仅管理员可下载联调面单'); }
        $id = $this->request->get('id', 0, 'intval');
        $shipment = Db::name('shipment')->where('id', $id)->find();
        if (!$shipment || (int)$shipment['is_current'] !== 1 || $shipment['status'] === 'CANCELLED') { $this->error('运单不存在或已取消'); }
        $cancel = Db::name('sf_request')->where(['order_id' => $shipment['order_id'], 'attempt_no' => $shipment['attempt_no'], 'operation' => 'CANCEL'])->where('state', '<>', 'REJECTED')->find();
        if ($cancel) { $this->error('取消结果尚未确认，面单暂不可用'); }
        $file = Db::name('file')->where(['biz_type' => 'SF_LABEL', 'biz_id' => (string)$id])->whereNull('deleted_at')->order('id')->find();
        return $this->privatePdf($file);
    }
    private function privatePdf($file)
    {
        if (!$file || $file['mime_type'] !== 'application/pdf') { $this->error('PDF文件不存在'); }
        $name = $file['storage_path'];
        if (strpos($name, '/') !== false || strpos($name, chr(92)) !== false || substr($name, -4) !== '.pdf') { $this->error('文件路径无效'); }
        $root = realpath('/srv/petlis-private-reports');
        $path = $root ? realpath($root . DIRECTORY_SEPARATOR . $name) : false;
        if (!$path || strpos($path, $root . DIRECTORY_SEPARATOR) !== 0 || !is_file($path) || filesize($path) > 10485760) { $this->error('文件不可用'); }
        $handle = fopen($path, 'rb');
        if (!$handle || fread($handle, 5) !== '%PDF-') { if ($handle) { fclose($handle); } $this->error('文件类型无效'); }
        fclose($handle);
        header('Content-Type: application/pdf');
        header('Content-Disposition: inline; filename="petlis-' . (int)$file['id'] . '.pdf"');
        header('Cache-Control: private, no-store');
        header('X-Content-Type-Options: nosniff');
        readfile($path);
        exit;
    }
}
