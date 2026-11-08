# 报告打印状态与旧系统兼容验证

## 问题与修改

2026-11-08检查发现：管理打印入口只检查订单已完成和现结付款，未检查明细作废/启停、当前报告工作流及结果作废。使用新建合成23937/明细33810逐次临时注入IsCheckPass=1、IsActive=0、工作流CANCELLED、结果VOIDED，原入口均返回报告。每次注入后finally恢复字段，不对历史业务批量改写。

PrintService现在同时检查有效且未作废明细；已接入新流程或已有工作流的报告必须是该订单/明细最新工作流PUBLISHED，且关联同单同明细结果为ENTERED。取消/未发布/失去结果关联时拒绝，失败不记录成功打印审计。角色范围及退款/未支付现结限制保留，不把订单完成状态视为各项报告均有效的替代。

原PHP未接入工作流的历史报告不要求凭空补新工作流，仍按旧完成状态、有效明细、医院结算与既有打印角色检查。结算读取优先新快照，缺失时沿用医院原PayType（月结/现结），修正原入口统一默认cash而误拦历史月结的问题。

## 验证证据

- 后端418项通过，0失败/0跳过；新增4项覆盖有效发布、作废/停用/取消/缺结果拒绝、历史月结、缺扩展仍不能绕过已有工作流、退款现结拒绝及审计边界。
- 本机真实HTTP在合成23937/33810上逐项注入上述四状态，修复后业务码均409；所有字段恢复后打印正常。该API错误可能以HTTP200+JSON业务码返回，验证器按Content-Type和业务码判断，不将HTTP200直接当成功。
- 新合成23938使用永久实验室488：PHP签收、结果、驳回修订、退样/未分配复检、报告33811审核发布，新端ADMIN/本实验室打印成功、医生管理打印403，PDF鉴权链通过，永久映射保持不变。
- 本机旧月结明细33586无新扩展/工作流、订单完成、IsPayMoney=0，管理员实际打印正常；只读取业务数据并写正常打印审计，没有改旧记录或复制报告内容到证据。

证据在私有qa/report-print-before.json、report-print-after.json、report-print-php-full-chain.json、report-print-legacy-monthly.json。复现负例：`python3 scripts/local/test-report-print-guards.py --order-id <已发布合成订单>`；跨端组合：`python3 scripts/local/test-real-flow-fix.py --php-lab --use-local-lab --php-results --php-retest --php-unassigned --php-report --print-report`。

没有结构变更、没有写Mac mini/生产、没有启用调度。此验证是API/桥接和受控状态注入，未证明真实浏览器打印、实体标签/报告纸张排版、硬件扫码或微信真机全量完成。
