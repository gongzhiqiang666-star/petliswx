# M08 全局检测项目及多段价格首批迁移

## 原来源与迁入规则

- `admin/model/LbInfotesttype.php` beforeInsert：Price/CtoG/GtoC/DiscountReal/DiscountNext全部0、TextModel空；CreateDt为创建datetime，原模型不自动写createtime/updatetime。
- `admin/view/tpl/lb_infotesttype/add.html`：名称/编号必填，所属大类默认0内部，状态默认1正常。edit表单另包含多段价格及模板绑定。
- `admin/controller/tpl/LbInfotesttype.php` index及DealPrice：多价格GROUP_CONCAT，未过滤停用价，无价显示“无”；原`str_replace('0000','00',...)`显示规则保留。该规则存在异常显示边界，当前不能擅自修改原内容。
- `admin/model/tpl/Lb_infotesttype_price.php`：不自动写时间；原价格表单默认IsActive=1、step0.01，没有min限制。

新`LegacyTestCatalogService/Controller`、PC`legacy-test-catalog`及组件直接访问原两表。全局项目维护与新系统实验室目录/签约价格分开，未修改新实验室特有创建逻辑，也不自动把旧全局项目挂到某实验室。

## 当前可用

独立参数`legacy-test-catalog`严格JSON布尔enabled/writeEnabled，默认关闭。管理员可查询、新增、编辑全局项目，查询/添加多段价格；保留主价格置零及独立价段、原状态/大类、原名称/编码。项目编辑不改旧价格、系数、正文模板及CreateDt。正常和禁用价格均保留在原价格汇总显示中。

事务审计、稳定请求防重、编辑快照冲突保护；提交后清字典缓存，失败不清缓存。价格列表以十进制文本返回，保留两位及大金额精度，避免JS JSON数值丢精度/尾零。

## 验证

- 首轮后端558项通过；随后补金额文本精度断言，先观察BigDecimal与字符串契约不符失败，再改SQL CAST(price AS CHAR)。最新完整回归559项全部通过、无失败/跳过，见`/tmp/petlis-testcatalog-precision-full.log`。
- PC127项通过，`/tmp/petlis-testcatalog-admin.log`；新增命令构建测试保留原文、不传主Price、默认大类/状态、独立字符串金额。
- 本机事务测试：创建回调/表单默认、时间字段、不自动挂实验室；两种状态价段、不覆盖主价格；编辑/陈旧快照409；读写开关/变更请求号409；大金额90071992547409.91文本不丢精度。
- 真提交原子性：新项目审计失败回滚整个项目；价段审计失败仅回滚价段、原项目保留；并发4次同请求只生成1项目/审计。只用独立随机it_test_atomic_账号/项目，结束精确清理。
- 实际PC：临时全局项目930新增、编辑为过敏原类别，添加125.50正常价和99.00禁用价，汇总仍包括两者；主Price及回调系数仍0；只读开关隐藏新建/编辑；普通医生接口403。脚本`scripts/local/test-legacy-test-catalog-ui.py`。
- 临时项目930及价段1529/1530已清理，审计与私有完成检查点保留；未动原业务记录。参数恢复关闭、菜单隐藏。
- 私有结果`~/.local/share/petlis/qa/test-catalog-ui.json`；截图test-catalog-prices.png已查看。截图为价格文本修正前（125.5/99），修正后的实际页面显示复核仍待执行，不冒称已经截到新版。

## 仍待迁移

价段编辑/删除、项目删除/批量动作、导入、模板绑定/维护、原选择器调用、送检明细价段选择与折扣核价接续及权限/历史数据完整差分。原Price/CtoG等double列只保持旧存储，不在新计算用Double。

M08仅局部实现，不能称完成；逐模块两轮复核尚未执行。
