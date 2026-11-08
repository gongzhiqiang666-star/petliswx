"""Install Petlis-only local preview overlay without touching the reference PHP source or its schema."""
from pathlib import Path
import shutil
root=Path.home()/'.local/share/petlis/php-preview/app'
source=Path(__file__).resolve().parent/'overlay'
assert root.is_dir() and root.resolve()==root, 'Expected private local preview directory'
for file in source.rglob('*'):
    if not file.is_file(): continue
    target=root/file.relative_to(source)
    if target.exists() and b'PETLIS_LOCAL_WORKFLOW' not in target.read_bytes():
        raise SystemExit(f'Unrecognized existing overlay target: {target.name}')
    target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(file,target)
print('Installed local PHP shared-ID workflow read view; existing auth remains required')

# Add a discoverable link to the existing local PHP order editor, preserving its existing form.
edit=root/'application/admin/view/project/lb_listdiagnosisproject/edit.html'
content=edit.read_text()
marker='<!-- PETLIS_LOCAL_WORKFLOW_LINK -->'
if marker not in content:
    if not content.lstrip().startswith('<form id="add-form"'):
        raise SystemExit('Unexpected PHP order editor baseline; no editor changes made')
    backup=Path.home()/'.local/share/petlis/php-preview/compat-backups'
    backup.mkdir(exist_ok=True)
    original=backup/'edit.html.before-workflow-link'
    if not original.exists(): shutil.copy2(edit,original)
    link=marker+'\n<div class="alert alert-info"><a class="btn btn-default" target="_blank" rel="noopener" href="/laboklin.php/petlis_workflow/index?id={$row.ListDiagnosisProjectId|htmlentities}">查看同单全链路进度</a>　订单、物流、实验室与报告使用同一业务记录。</div>\n'
    edit.write_text(link+content)
    print('Added shared workflow link to local PHP order editor')

# Do not let generic legacy model writes bypass the unified workflow for managed orders.
model=root/'application/admin/model/LbListdiagnosisproject.php'
data=model.read_bytes()
marker=b'PETLIS_MANAGED_ORDER_GUARD'
if marker not in data:
    update=b'self::beforeUpdate(function ($model) {'
    after_delete=b'self::afterDelete(function ($model) {'
    if data.count(update)!=1 or data.count(after_delete)!=1:
        raise SystemExit('Unexpected legacy order model baseline; no model changes made')
    backup=Path.home()/'.local/share/petlis/php-preview/compat-backups'
    backup.mkdir(exist_ok=True)
    shutil.copy2(model,backup/'LbListdiagnosisproject.php.before-workflow-guard')
    check="""
            // PETLIS_MANAGED_ORDER_GUARD: use the authenticated shared workflow, not generic field writes.
            if (Db::name('order_extension')->where('order_id', (int)$model->ListDiagnosisProjectId)->find()) {
                throw new \\think\\exception\\ValidateException('该送检单已纳入新旧系统共享工作流，请通过同单全链路入口处理，不能直接覆盖状态或资料');
            }
""".encode()
    data=data.replace(update,update+check,1)
    delete_guard=b"self::beforeDelete(function ($model) {"+check+b"        });\n\n        "
    data=data.replace(after_delete,delete_guard+after_delete,1)
    model.write_bytes(data)
    print('Protected managed orders from generic legacy model updates and deletion')

# Route managed-order legacy editor GETs to its shared workflow rather than an unusable raw form.
controller=root/'application/admin/controller/project/LbListdiagnosisproject.php'
data=controller.read_bytes()
if b'PETLIS_MANAGED_EDITOR_ROUTE' not in data:
    anchor=b'    public function add()'
    if data.count(anchor)!=1: raise SystemExit('Unexpected PHP editor controller baseline')
    backup=Path.home()/'.local/share/petlis/php-preview/compat-backups'
    shutil.copy2(controller,backup/'LbListdiagnosisproject.php.before-managed-editor-route')
    method=b'''    // PETLIS_MANAGED_EDITOR_ROUTE
    public function edit($ids = null)
    {
        $id = (int)($ids ?: $this->request->param('ids', 0, 'intval'));
        if (!$this->request->isPost() && Db::name('order_extension')->where('order_id', $id)->find()) {
            $this->redirect('/laboklin.php/petlis_workflow/index?id=' . $id);
        }
        return parent::edit($ids);
    }

'''
    controller.write_bytes(data.replace(anchor,method+anchor,1))
    print('Routed local legacy managed-order editor to shared workflow')

# Preserve the original primary/parent IDs when protecting against reassignment through generic forms.
model=root/'application/admin/model/LbListdiagnosisproject.php'
data=model.read_bytes()
v1=b"if (Db::name('order_extension')->where('order_id', (int)$model->ListDiagnosisProjectId)->find()) {"
v2=b"if (Db::name('order_extension')->where('order_id', 'in', array_unique([(int)$model->ListDiagnosisProjectId, isset($model->origin['ListDiagnosisProjectId']) ? (int)$model->origin['ListDiagnosisProjectId'] : 0]))->find()) {"
if data.count(v1)==2:
    shutil.copy2(model,root.parent/'compat-backups/LbListdiagnosisproject.php.before-origin-guard')
    model.write_bytes(data.replace(v1,v2))
elif data.count(v2)!=2:
    raise SystemExit('Unexpected managed-order guard version')

detail=root/'application/admin/model/LbDiagnosisdetail.php'
data=detail.read_bytes()
if b'PETLIS_MANAGED_DETAIL_GUARD' not in data:
    insert=b'self::beforeInsert(function ($model) {'
    update=b'self::beforeUpdate(function ($model) {'
    if data.count(insert)!=1 or data.count(update)!=1:
        raise SystemExit('Unexpected legacy detail model baseline')
    shutil.copy2(detail,root.parent/'compat-backups/LbDiagnosisdetail.php.before-workflow-guard')
    guard="""
            // PETLIS_MANAGED_DETAIL_GUARD: protect both original and requested parent order.
            $parents = array_unique([(int)$model->ListDiagnosisProjectId, isset($model->origin['ListDiagnosisProjectId']) ? (int)$model->origin['ListDiagnosisProjectId'] : 0]);
            if (Db::name('order_extension')->where('order_id', 'in', $parents)->find()) {
                throw new \\think\\exception\\ValidateException('该检测明细已纳入共享工作流，请通过统一流程处理价格、结果和报告');
            }
""".encode()
    data=data.replace(update,update+guard,1)
    data=data.replace(insert,b'self::beforeDelete(function ($model) {'+guard+b'        });\n\n        '+insert+guard,1)
    detail.write_bytes(data)
    print('Protected managed-order detail inserts, updates, reassignment and deletion')

# Newly prepared legacy forms must not start as reviewed/awaiting-report orders.
controller=root/'application/admin/controller/project/LbListdiagnosisproject.php'
data=controller.read_bytes()
old=b'$model["SampleStatus"] = 2;'
new=b'$model["SampleStatus"] = 0; // PETLIS_LEGACY_PREPARE: save details, bind doctor and submit explicitly'
if b'PETLIS_LEGACY_PREPARE' not in data:
    if data.count(old)!=1: raise SystemExit('Unexpected legacy add status baseline')
    shutil.copy2(controller,root.parent/'compat-backups/LbListdiagnosisproject.php.before-prepare-flow')
    controller.write_bytes(data.replace(old,new,1))

# Keep the legacy editor for preparation only; review through the authenticated shared adapter.
edit=root/'application/admin/view/project/lb_listdiagnosisproject/edit.html'
content=edit.read_text()
if 'PETLIS_LEGACY_PREPARE_BUTTONS' not in content:
    start=content.index('            {if $row->SampleStatus == 0}')
    end=content.index('            {else}',start)
    shutil.copy2(edit,root.parent/'compat-backups/edit.html.before-prepare-buttons')
    content=content[:start]+'''            {if $row->SampleStatus == 0}
                <!-- PETLIS_LEGACY_PREPARE_BUTTONS -->
                <button type="submit" class="btn btn-success btn-embossed disabled">保存送检资料</button>
                <input type="hidden" name="row[SampleStatus]" value="0"/>
                <a class="btn btn-default" target="_blank" rel="noopener" href="/laboklin.php/petlis_workflow/index?id={$row.ListDiagnosisProjectId|htmlentities}">绑定医生并接入送审</a>
''' + content[end:]
    edit.write_text(content)

model=root/'application/admin/model/LbListdiagnosisproject.php'
data=model.read_bytes()
if b'PETLIS_UNADOPTED_REVIEW_GUARD' not in data:
    anchor=b'self::beforeUpdate(function ($model) {'
    if data.count(anchor)!=1: raise SystemExit('Unexpected legacy update model baseline')
    guard='''
            // PETLIS_UNADOPTED_REVIEW_GUARD: legacy preparation cannot silently approve an unadopted order.
            if (isset($model->origin['SampleStatus']) && (int)$model->origin['SampleStatus'] === 0 && (int)$model->SampleStatus !== 0) {
                throw new \\think\\exception\\ValidateException('待审核旧单须先绑定医生、接入实验室，再通过统一审核入口处理');
            }
'''.encode()
    shutil.copy2(model,root.parent/'compat-backups/LbListdiagnosisproject.php.before-unadopted-review-guard')
    model.write_bytes(data.replace(anchor,anchor+guard,1))

# Block duplicate manual barcode changes in the legacy model, not merely a client-side alert.
model=root/'application/admin/model/LbListdiagnosisproject.php'
data=model.read_bytes()
if b'PETLIS_BARCODE_UPDATE_GUARD' not in data:
    anchor=b'self::beforeUpdate(function ($model) {'
    if data.count(anchor)!=1: raise SystemExit('Unexpected legacy barcode guard baseline')
    guard='''
            // PETLIS_BARCODE_UPDATE_GUARD: preserve historical rows; validate only a changed barcode.
            $barcode = trim((string)$model->UserNO);
            $previous = isset($model->origin['UserNO']) ? trim((string)$model->origin['UserNO']) : '';
            if ($barcode !== $previous) {
                if (!preg_match('/^[A-Za-z0-9][A-Za-z0-9_.\\/ +%\\-]{1,62}[A-Za-z0-9]$/D', $barcode)) {
                    throw new \\think\\exception\\ValidateException('请填写完整、可扫描的送检条码');
                }
                if (Db::name('lb_listdiagnosisproject')->where('UserNO', $barcode)->where('ListDiagnosisProjectId', '<>', (int)$model->ListDiagnosisProjectId)->lock(true)->find()) {
                    throw new \\think\\exception\\ValidateException('条码已被其他送检单使用，未保存本次修改');
                }
            }
'''.encode()
    shutil.copy2(model,root.parent/'compat-backups/LbListdiagnosisproject.php.before-barcode-guard')
    model.write_bytes(data.replace(anchor,anchor+guard,1))

# Price-select initialization must respect the selected test even when keyValue is provided.
price_controller=root/'application/admin/controller/tpl/LbInfotesttypePrice.php'
data=price_controller.read_bytes()
if b'PETLIS_SCOPED_PRICE_SELECT' not in data:
    anchor=b"            if ($this->request->request('keyField')) {"
    if data.count(anchor)!=1: raise SystemExit('Unexpected legacy price selector baseline')
    code='''            // PETLIS_SCOPED_PRICE_SELECT: never initialize a price from another test.
            if ($this->request->request('keyField') === 'price') {
                $custom = $this->request->request('custom/a', []);
                $testId = isset($custom['InfoTestTypeId']) ? (int)$custom['InfoTestTypeId'] : (isset($custom['InfoTestTypeid']) ? (int)$custom['InfoTestTypeid'] : 0);
                if ($testId <= 0) { return json(['list' => [], 'total' => 0]); }
                $test = \\think\\Db::name('lb_infotesttype')->where(['InfoTestTypeId' => $testId, 'IsActive' => 1])->find();
                if (!$test) { return json(['list' => [], 'total' => 0]); }
                $prices = \\think\\Db::name('lb_infotesttype_price')->where(['InfoTestTypeid' => $testId, 'IsActive' => 1])->where('price', '>=', 0)->order('IID desc')->field('price')->limit(100)->select();
                if (!$prices && isset($test['Price']) && is_numeric($test['Price']) && $test['Price'] >= 0) { $prices = [['price' => (string)$test['Price']]]; }
                return json(['list' => $prices, 'total' => count($prices)]);
            }
'''.encode()
    shutil.copy2(price_controller,root.parent/'compat-backups/LbInfotesttypePrice.php.before-scoped-select')
    price_controller.write_bytes(data.replace(anchor,code+anchor,1))

legacy_js=root/'public/assets/js/backend/project/lb_diagnosisdetail.js'
data=legacy_js.read_bytes()
if b'PETLIS_SCOPED_PRICE_RESPONSE' not in data:
    anchor=b'v = $(this).val();\r\n                        $.post("tpl/lb_infotesttype_price/index",{'
    callback=b'if(typeof d.list != "undefined"){'
    if data.count(anchor)!=1 or data.count(callback)!=1: raise SystemExit('Unexpected legacy price JS baseline')
    shutil.copy2(legacy_js,root.parent/'compat-backups/lb_diagnosisdetail.js.before-scoped-price')
    data=data.replace(anchor,b'''// PETLIS_SCOPED_PRICE_RESPONSE: clear stale prices and ignore responses for a previous test.
                        var v = $(this).val();
                        $("#c-InfoTestTypePriceId").val("");
                        $("#c-txtResultPrice").val("");
                        $.post("tpl/lb_infotesttype_price/index",{''',1)
    data=data.replace(callback,b'''if (String($("#c-InfoTestTypeId").val()) !== String(v)) { return; }
                            if(Array.isArray(d.list) && d.list.length > 0){''',1)
    legacy_js.write_bytes(data)

# Ignore stale A->B->A responses as well as responses for a different selected test.
legacy_js=root/'public/assets/js/backend/project/lb_diagnosisdetail.js'
data=legacy_js.read_bytes()
if b'PETLIS_PRICE_REQUEST_SEQUENCE' not in data:
    anchor=b'var v = $(this).val();'
    stale=b'if (String($("#c-InfoTestTypeId").val()) !== String(v)) { return; }'
    price_call=b"$.post('base/price', {testtype: testtype, price: price, discount: discount}, function (data) {"
    if data.count(anchor)!=1 or data.count(stale)!=1 or data.count(price_call)!=1: raise SystemExit('Unexpected price sequencing baseline')
    shutil.copy2(legacy_js,root.parent/'compat-backups/lb_diagnosisdetail.js.before-price-sequence')
    data=data.replace(anchor,anchor+b'\n                        var selectionSequence = Controller.api.selectionSequence = (Controller.api.selectionSequence || 0) + 1; // PETLIS_PRICE_REQUEST_SEQUENCE',1)
    data=data.replace(stale,b'if (selectionSequence !== Controller.api.selectionSequence || String($("#c-InfoTestTypeId").val()) !== String(v)) { return; }',1)
    data=data.replace(price_call,b"var priceSequence = Controller.api.priceSequence = (Controller.api.priceSequence || 0) + 1;\n                    "+price_call+b'\n                        if (priceSequence !== Controller.api.priceSequence) { return; }',1)
    legacy_js.write_bytes(data)

legacy_js=root/'public/assets/js/backend/project/lb_diagnosisdetail.js'
data=legacy_js.read_bytes()
if b'PETLIS_CLEAR_PRICE_INVALIDATION' not in data:
    sequence=b'var priceSequence = Controller.api.priceSequence = (Controller.api.priceSequence || 0) + 1;'
    blank=b'if (price == "") {'
    selection=b'var selectionSequence = Controller.api.selectionSequence = (Controller.api.selectionSequence || 0) + 1;'
    if data.count(sequence)!=1 or data.count(blank)!=1 or data.count(selection)!=1: raise SystemExit('Unexpected price invalidation baseline')
    data=data.replace(sequence,b'',1)
    data=data.replace(blank,sequence+b' // PETLIS_CLEAR_PRICE_INVALIDATION\n                    '+blank,1)
    data=data.replace(selection,selection+b'\n                        Controller.api.priceSequence = (Controller.api.priceSequence || 0) + 1;',1)
    legacy_js.write_bytes(data)
