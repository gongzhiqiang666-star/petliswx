# M23 网银交易流水迁移

## 源码契约

只读来源为 `petlis-reference/macmini/laboklin_cn`：

- `application/admin/controller/finance/Lbpaylog.php`：使用 `finance/Lb_paylog` 模型，导入委托父类；查询、新增、修改、删除和批量为继承动作。
- `application/admin/model/finance/Lb_paylog.php`：`lb_paylog`，关闭自动创建/更新时间，无模型保存回调；校验器规则为空。
- `application/admin/view/finance/lbpaylog/{add,edit}.html`、`public/assets/js/backend/finance/lbpaylog.js`：十一列含主键；金额输入步长 0.0001，时间明确输入，新增默认当前时间；主键倒序。
- `application/index/controller/Project/Pay.php`：两处支付回调写流水。商户订单号写 TradeNoUser，渠道交易号写 TradeNoAli，金额由分除100并保留两位，TradeStatus=TRADE_SUCCESS；BuyMail、Exterface 等可以是空字符串。回调的扣款/支付状态更新属于 M24，不属于流水维护本身。
- `application/admin/library/traits/Backend.php::import`：CSV/XLS/XLSX、首个工作表、首行按数据库列注释匹配，未知列忽略；模型 saveAll。`thinkphp/library/think/Model.php::saveAll`：带主键时走更新，否则新增，整批事务。不能把这项缩成只新增或只支持 CSV。

原表 `lin_lb_paylog` 的金额为 DECIMAL(18,2)，文本长度按现有字段，交易号没有唯一约束，不能自动去重删除。负数输入按原字段允许；这不表示已证实 PHP 支付回调会写退款负数。旧页面将 TradeNoAli/TradeNoUser 标作“报告编号/样本编号”，新页面同时说明真实含义并保留原标签说明，不改变数据。

## 新落点与当前实现

- Java `LegacyPaymentLogService` / `LegacyPaymentLogController`：分页查询、读取、新增、修改、物理删除。
- PC `#/legacy-payment-logs`：原全部业务列、查询、表单、保存重开、独立删除确认；复用现有页面与表单风格。
- 参数 `legacy-payment-logs`（GLOBAL/0/ACTIVE），严格布尔 `enabled`、`writeEnabled`、`deleteEnabled`；默认关闭，前端菜单及接口双重控制；管理员权限。
- 复用 Java 审计表、账号和事务，不新增表、不修改旧表。请求防重按 JSON 结构比较，行快照控制覆盖，审计与业务写入同事务。
- 金额使用 BigDecimal，最多四位输入、HALF_UP 两位存储，拒绝溢出/非十进制输入；保留负数、空文本、原时间及未知交易状态。金额 API 始终以字符串返回，避免浏览器精度损失。
- 不调用支付、退款、财务核销或资金池服务。相同平台/商户交易号可保留多条记录；请求重试不重复新增。

## 验证与待办

已运行数据库专项：5 项维护/边界/权限/请求防重测试，2 项真实服务事务代理的审计失败回滚测试，全部通过、无跳过；PC 141 项测试通过。预期删除后读取404和合成审计失败会在后端测试日志中输出异常，不是测试失败。

真实 PC → Java → 本机13306已通过新增负数、四位输入两位存储、编辑、刷新重开及页面物理删除。医院、主单、检测明细及支付主记录全部字段逐行 SHA-256 前后匹配；测试开关已恢复，合成数据及专用后端已清理。截图在本机私有 QA 目录 `it_paylog_live_b7794554e9.png`。验收脚本首次发现同地址片段跳转不刷新页面、原弹窗仍挡住后续点击，已改为真实页面刷新后重开验证；没有改业务代码或绕过页面点击。源码对照只是本子项契约记录，不是整个 M23 第一轮或第二轮复核完成。

未完成：CSV/XLS/XLSX 批量预览/导入（包括原注释表头、带主键更新）、批量维护的实际业务核验、导出/精确查询条件及历史交易接续、与 M24/M30 的异常对账和防重复记账联调；模块两轮完整复核。真实供应商及生产付款未测试。定时任务和真实外发保持关闭。
