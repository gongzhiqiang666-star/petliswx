# M08 价段编辑/删除增量验证

## 原行为

`admin/view/tpl/lb_infotesttype_price/edit.html`输入step为0.0001（新增为0.01），原价格表decimal(18,2)，原模型不自动更新时间。原继承del物理删除，无SoftDelete。订单明细保存OrginalPrice/TestPrice金额，并非价段维护时自动重算历史订单；新维护接口不更新订单表。

## 新实现

- `LegacyTestCatalogService.price/updatePrice/deletePrice`及对应PC操作；路径同时限定项目ID和价格ID，不允许跨项目改删。
- 编辑允许四位小数，由旧decimal列执行原数据库舍入。新增仍按原step允许两位。编辑不会补写createtime/updatetime，不改项目主价格或历史订单。
- 详情/价格列表以十进制文本显示，保留125.50尾零及大金额精度；当前快照校验防覆盖，稳定请求号防重，审计与修改/删除同事务。
- 读写参数之外，独立deleteEnabled严格布尔开关；未开放时隐藏删除按钮且接口409。物理删除前确认价格/状态并提示不可恢复；审计保留原值。
- 原编辑表单只有价格，新增PC另提供状态维护，状态使用原IsActive整数，未创造新状态；其余通用multi/字段搬移能力仍待盘点。

## 验证

- 后端全量563项通过，无失败/错误/跳过：`/tmp/petlis-price-maint-full.log`。
- PC129项通过：`/tmp/petlis-price-maint-admin-final.log`。新增编辑四位/新增两位区分、显示文本、失败重试同号/同快照及删除确认测试。
- 隔离库：编辑/重试、陈旧快照409、跨项目拒绝、删除开关、重复删除防重，最后DealPrice回到“无”；NULL时间不被自动填充。125.5555入库125.56，-125.5555入库-125.56。
- 真提交测试：编辑/删除审计失败后价格及父项目原样恢复。原项目新增/价格新增/并发重试测试继续通过。
- 实际PC脚本`scripts/local/test-legacy-price-maintenance-ui.py`：新临时项目993/价段1571，125.50显示完整；输入150.5555并禁用，实际保存150.56；删除未授权按钮隐藏/API409；开启删除后实际确认物理删除、父项目保留且价格汇总“无”。结束清理临时父项目及价段，保留审计，无原业务记录改动。
- 证据`~/.local/share/petlis/qa/price-maintenance-ui.json`和已完成检查点price-maintenance-private.json；截图price-delete-confirm.png已查看。禁止盲目重放已完成检查点。
- 首次UI脚本误认为HTTP一定返回NULL时间键（当前JSON会省略NULL），断言失败；finally已精确清理，检查数据库无遗留后归档原检查点，改用缺省NULL兼容断言再用全新随机fixture完成。本项为验收脚本修正，未将失败当通过或作为第二轮复核。

## 剩余

项目删除/批量、导入、模板绑定与模板编辑、原选择器、送检明细价段/折扣及完整权限历史接续仍待迁移；M08未整体完成，两轮整模块复核未完成。价格IID超出signed long/JS安全整数的极端历史编号兼容仍须专项核实。无DDL或PHP修改，所有模块参数验收后关闭。
