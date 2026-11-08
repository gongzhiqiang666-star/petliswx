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
- 参数 `legacy-payment-logs`（GLOBAL/0/ACTIVE），严格布尔 `enabled`、`writeEnabled`、`deleteEnabled`、`importEnabled`；导入必须同时开启读取、写入和独立导入开关；默认关闭，前端菜单及接口双重控制；管理员权限。
- 复用 Java 审计表、账号和事务，不新增表、不修改旧表。请求防重按 JSON 结构比较，行快照控制覆盖，审计与业务写入同事务。
- 金额使用 BigDecimal，最多四位输入、HALF_UP 两位存储，拒绝溢出/非十进制输入；保留负数、空文本、原时间及未知交易状态。金额 API 始终以字符串返回，避免浏览器精度损失。
- 不调用支付、退款、财务核销或资金池服务。相同平台/商户交易号可保留多条记录；请求重试不重复新增。

## 验证与待办

已运行数据库专项：5 项维护/边界/权限/请求防重测试，2 项真实服务事务代理的审计失败回滚测试，全部通过、无跳过；PC 141 项测试通过。预期删除后读取404和合成审计失败会在后端测试日志中输出异常，不是测试失败。

真实 PC → Java → 本机13306已通过新增负数、四位输入两位存储、编辑、刷新重开及页面物理删除。医院、主单、检测明细及支付主记录全部字段逐行 SHA-256 前后匹配；测试开关已恢复，合成数据及专用后端已清理。截图在本机私有 QA 目录 `it_paylog_live_b7794554e9.png`。验收脚本首次发现同地址片段跳转不刷新页面、原弹窗仍挡住后续点击，已改为真实页面刷新后重开验证；没有改业务代码或绕过页面点击。源码对照只是本子项契约记录，不是整个 M23 第一轮或第二轮复核完成。

未完成：批量维护的实际业务核验、导出/精确查询条件及历史交易接续、与 M24/M30 的异常对账和防重复记账联调；模块两轮完整复核。真实供应商及生产付款未测试。定时任务和真实外发保持关闭。


## CSV / XLS / XLSX 导入增量

Java `LegacyPaymentLogImportFile` 读取 CSV 和 Excel 首张工作表的原值；公式保留原文而不求值；CSV 支持 UTF-8（含 BOM）、GBK、Big5、Latin1，自动模式按 UTF-8、GBK、Latin1 顺序尝试。编码存在歧义时，用户可在预览中指定编码重读，不以自动识别作中文正确性的保证。日期字段保留文本，Excel 数值日期不会被擅自解释成另一日期。

`LegacyPaymentLogImportService` / `LegacyPaymentLogImportController` 提供 `/import/preview`、`/import` 和字段名模板下载。PC 导入弹窗先分页展示最终字段及新增/更新数量，再提交：

- 字段名模式支持 `PayLogId` 更新及缺列保留；原注释模式按真实列注释映射，重复表头最后一列优先、未知列提示并忽略。原库主键和日期列注释为空，原 PHP 映射中最后的空注释实际指向 CreateDt；兼容该映射，不假设空表头能导入主键。
- 无主键列新增；有主键列则更新已有主键，重复主键按文件顺序依次应用。不同文件行可保留相同交易号，展示提醒而不合并。金额先使用 BigDecimal 按原数据库两位精度舍入，预览即显示最终存储结果；不会经过 JavaScript 数值转换。
- 新增仍需全部非空结构字段（文本内容允许空串）；更新未提供的列保留旧值。主键为空/0/不存在时明确拒绝预览，避免 PHP 原无匹配更新仍返回成功的含糊结果；不转为新增。仅提示失败方式不同，没有新增或删除实际交易。
- 预览签名绑定管理员、全部最终字段、源版本和30分钟期限；无预览持久表。提交锁定全部更新源并核对版本后才开始写入；每行及整批审计与业务同事务。结果不明时复用同一请求号重试，整批回执恢复，不重复新增。
- 单批文件上限2MB/1000行/64列，较大文件拆批；XLSX 另有限制解压体积及资源数量。文件只在内存解析，不保存上传文件、不调用外部链接、不执行公式。

技术依赖采用 Apache POI 6.0.0（XLS/XLSX）及 Commons CSV 1.14.1；POI 官方 FAQ 明确支持 Java 17，未升级 JDK。为 POI 的依赖要求对齐 Log4j API 与 Boot 日志桥接版本2.26.1。出处：[POI 官方下载](https://poi.apache.org/download.html)、[Java要求](https://poi.apache.org/help/faq.html)、[Commons CSV](https://commons.apache.org/proper/commons-csv/)。不沿用旧 PHPExcel 技术外壳。

验证：本轮相关 Java 31 项通过、零失败/错误/跳过，其中流水18项（文件4、导入业务6、维护5、事务回滚3），邮件传输7、认证5、授权规则1；PC144项通过。最终整批审计故障在真实 Spring 事务代理中注入，两条业务记录及已写行审计一并回滚。

真实 PC → Java → 13306：上传 CSV、二进制 XLS、XLSX 分别预览并确认，原始大金额 `9999999999999999.99` 精确保留，负数舍入、引号及换行正文、公式原文、重复交易号、首工作表、同一主键连续两次部分更新、刷新重开、重复提交回执与非法预览不写入均通过。医院/订单/检测明细/支付主记录全行哈希一致。仅使用合成交易，测试后恢复开关、删除自有夹具并停止专用后端，未改表结构或启用真实支付。

执行入口：`python3 scripts/local/test-payment-logs-live.py --import`。源码对照和这些操作证据仅覆盖导入子项，不代表整个 M23 或全流程迁移已完成。历史异常格式、完整批量维护/导出查询及 M24/M30 对账联动仍待核对。
