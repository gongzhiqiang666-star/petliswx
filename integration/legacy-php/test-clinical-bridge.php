<?php
// Local synthetic service-level probe; normal bridge credentials and Java authorization are still used.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$id = isset($argv[1]) ? (int)$argv[1] : 0;
$order = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();
if (!$order || strpos($order['AnimalName'],'QA_REAL_') !== 0 || (int)$order['SampleStatus'] !== 0) { throw new \RuntimeException('Expected fresh synthetic pending order'); }
$details = \think\Db::name('lb_diagnosisdetail')->where('ListDiagnosisProjectId',$id)->order('DiagnosisDetailId')->field('DiagnosisDetailId,ListDiagnosisProjectId,InfoTestTypeId,TestPrice')->select();
$bridge = new \app\admin\library\PetlisApiBridge(4);
$before = $bridge->clinicalSnapshot($id);
$fields = $before['fields'];
$fields['sampleArea'] = 'QA修正采样部位'; $fields['medicalHistory'] = 'QA既往病史';
$fields['clinicalSymptoms'] = 'QA临床症状'; $fields['diagnosis'] = 'QA临床诊断'; $fields['medication'] = 'QA用药信息';
$bridge->execute('clinical-save',$id,['clinicalVersion'=>$before['version'],'clinicalFields'=>$fields,'reason'=>'核对合成采样单后修正']);
$after = $bridge->clinicalSnapshot($id);
if ($after['version'] === $before['version'] || $after['fields']['medicalHistory'] !== $fields['medicalHistory'] || (int)$after['status'] !== 0) { throw new \RuntimeException('Correction snapshot mismatch'); }
$staleRejected = false;
try { $bridge->execute('clinical-save',$id,['clinicalVersion'=>$before['version'],'clinicalFields'=>$fields,'reason'=>'过期页面重放']); }
catch (\Exception $error) { $staleRejected = strpos($error->getMessage(),'刷新') !== false; }
if (!$staleRejected) { throw new \RuntimeException('Stale correction was not rejected'); }
$bridge->execute('review-reject',$id,['reason'=>'合成退回后修正测试']);
$rejected = $bridge->clinicalSnapshot($id);
if ((int)$rejected['status'] !== 1 || !$rejected['editable']) { throw new \RuntimeException('Rejected order should be correctable'); }
$fields = $rejected['fields'];$fields['clinicalSymptoms'] = 'QA修订后症状';
$bridge->execute('clinical-submit',$id,['clinicalVersion'=>$rejected['version'],'clinicalFields'=>$fields,'reason'=>'已补全并重新送审']);
$final = $bridge->clinicalSnapshot($id);
$saved = \think\Db::name('lb_listdiagnosisproject')->where('ListDiagnosisProjectId',$id)->find();
$extension = \think\Db::name('order_extension')->where('order_id',$id)->find();
$currentDetails = \think\Db::name('lb_diagnosisdetail')->where('ListDiagnosisProjectId',$id)->order('DiagnosisDetailId')->field('DiagnosisDetailId,ListDiagnosisProjectId,InfoTestTypeId,TestPrice')->select();
foreach (['UserNO','CreateSampleId','InfoHospitalId','TestFee'] as $key) { if ((string)$saved[$key] !== (string)$order[$key]) { throw new \RuntimeException('Protected field changed: '.$key); } }
if ($details !== $currentDetails || (int)$saved['SampleStatus'] !== 0 || $extension['review_status'] !== 'pending' || $final['fields']['clinicalSymptoms'] !== 'QA修订后症状') { throw new \RuntimeException('Resubmission or original price/detail preservation failed'); }
$count = \think\Db::name('audit_record')->where(['resource_type'=>'ORDER','resource_id'=>(string)$id,'action'=>'LEGACY_CLINICAL_UPDATED'])->count();
if ((int)$count !== 2) { throw new \RuntimeException('Expected two clinical correction audits'); }
echo json_encode(['passed'=>true,'orderId'=>$id,'staleRejected'=>true,'auditCount'=>2,'priceAndItemsUnchanged'=>true,'status'=>0,'reviewStatus'=>'pending'],JSON_UNESCAPED_UNICODE);
