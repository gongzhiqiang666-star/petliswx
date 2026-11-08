<?php
// Pure response contract probe: no authentication bypass, no business execution and no database writes.
define('APP_PATH', '/srv/petlis/application/');
require '/srv/petlis/thinkphp/base.php';
\think\App::initCommon();
$type = new \ReflectionClass('app\\admin\\controller\\PetlisWorkflow');
$controller = $type->newInstanceWithoutConstructor();
$request = new class { public function isAjax() { return true; } };
$property = $type->getProperty('request');
$property->setAccessible(true);
$property->setValue($controller, $request);
$method = $type->getMethod('operationFailure');
$method->setAccessible(true);
$response = $method->invoke($controller, '校验失败<script>', 23903);
$data = $response->getData();
if ($data !== ['code' => 0, 'message' => '校验失败<script>']) { throw new \RuntimeException('Unexpected inline failure contract'); }
echo "PASS: AJAX operation failure returns structured text, not a navigation or HTML error page\n";
