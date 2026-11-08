# M21 原财务明细与批量标记

## 原源码的两条路径必须区分

`application/admin/controller/finance/Project.php`：

- index使用vwdiagnosisdetail，排除InfoHospitalId=0；读取原财务字段，补医院/物种名称及DealPayMoneyShow状态。只把本页IsCheckPass==0的TestPrice按bcadd(...,2)累加，不能当全部筛选记录的总额。
- multi调用模型where后返回的查询对象update，直接更新字段，不经过明细模型保存回调。`application/admin/view/finance/project/index.html`实际提供is_invoiced=0/1、PayStatus2=0/1/2/4的批量操作；不把原泛化字段写入接口当成需要保留的任意改表能力。
- writeoff是另一条单条入口，viewMoney→1、viewTrance→2、viewPass→4、viewNoPay→0，find明细后调用模型save，因此会运行beforeUpdate及afterWrite。

模型`application/admin/model/LbDiagnosisdetail.php:88-117`：beforeUpdate含发送日期/父单状态/PDF路径副作用；afterWrite把MailReport和ReportResult插入正文副本表，缺少属性时用空串。实际两个正文仅存在lin_lb_diagnosisdetail_info，不在原find读取的主表，也没有同名属性读取器；ThinkPHP Model.__isset/getAttr缺少字段会返回false。因此单条核销存在清空正文副本的风险，不能用直接SQL冒充已完整迁移该入口。

用户已批准“保留正文，保留已核实状态回调”；单条入口按此方案继续实现，当前尚未开放。这里的未决项只影响单条writeoff路径；原批量multi本就不运行上述回调，按原规则迁移不需要假造/绕过回调。

## 本批实现

`LegacyFinanceService`、`LegacyFinanceController`：`/admin/legacy/finance`列表、详情版本及批量标记；PC“原业务财务明细”页面包含原可见业务列，其他原查询内容可看更多资料，查询分页、本页合计、勾选本页、批量确认及刷新。日常记录标记不等于实际收付款、退款、生成发票或资金池划转。

独立`legacy-finance`参数：enabled控制入口/读接口，writeEnabled控制批量标记，严格JSON布尔值并要求Java ADMIN。`legacySingleWriteoff=false`，没有开放未经确认的模型保存版核销。

- 财务显示沿用原标签：免除优先，否则0未结、1现金、2转账、3网银已支付、4忽略；原“是否已结款”仍按0否/非0是，不擅自重新解释忽略或免除。
- TestPrice/OrginalPrice等数值以字符串传输，避免JS大金额精度丢失；TestPrice原字段decimal(18,2)，本页合计BigDecimal逐笔加并按两位向零截断，排除IsCheckPass非0项。
- 只允许实际页面业务字段和值，不能手工标记3（网银支付）或写任意列。批次最多500条，超限明确拒绝不截断；PC当前一页50条选择。原超大跨页批次需要另行核对，不称无限批量等价。
- 新增请求号防重、整批版本校验、固定ID锁序、同事务审计；全部版本确认后再写，任一失败整批回滚。只改目标标记，不重算价格、不运行单条模型保存回调，不清空正文或改父单。
- 未执行DDL、修改PHP或开启定时任务。

## 结果验证

`LegacyFinanceLocalDatabaseTest`4项：

1. 90071992547409.91+0.09准确得到90071992547410.00，免除50.00不计入；分页合计不是全局合计；无医院主单仍被排除。
2. 现金、转账、忽略、重置及发票标记持久化，原父单整行、正文副本及医院余额均保持；同请求重放不重复处理。
3. 一条版本过期时整批不执行，冲突请求不能重放成另一动作。
4. 字段/值白名单、ADMIN和两级参数校验。

`LegacyFinanceAtomicLocalDatabaseTest`1项：真实Spring事务中，两条明细更新后注入审计失败，所有标记恢复且没有该批审计。

PC139项及新增JS/Python语法、diff检查通过。最初合成医院漏填原必填PayType导致测试setup失败，补全测试数据后上述5项通过，没有改原结构或默认值。

`python3 scripts/local/test-legacy-finance-live.py`通过：实际PC→专用Java18082→隔离MySQL13306，真实列表显示两条合成明细（其中一条免除），本页合计12.50；勾选两条、确认现金、重置未结、标记已开发票，每次都实际提交两条并刷新。SQL另验证最终PayStatus2=0/is_invoiced=1，原两份正文、主单状态2、医院余额20.00不变，页面错误0。列表仅通过真实查询参数限定本次医院及人工编号，没有替换接口响应。

成功响应在返回前记入0600恢复记录；本次创建明细可用专属请求号审计恢复。自己的合成医院/项目/主单/明细/正文和审计全部清理，两个模块原开关恢复，恢复记录删除，专用服务停止。未执行真实资金操作或外发。

## 缺口

单条writeoff按已批准方案实现和验收待完成；完整原筛选/排序、报表/打印、医院月报、全量历史数据及两轮模块复核尚未完成。旧发票标记与新电子发票工作流的对应还需完整核对；这里不把更新原标记说成开具电子发票。最终报告必须单列单条/批量路径差异和经确认的处理方式。

## 单条核销差异处理已获用户批准

用户已明确选择：**保留正文，保留已核实状态回调**。新系统单条writeoff不沿用缺少属性时清空正文副本的副作用；现有报告/邮件正文保持，其他beforeUpdate中的折扣、发送日期、父单状态和PDF命名规则仍按已核实PHP规则处理。此批准不涉及数据库结构、真实收付款、PHP源码或原生产环境。实现及专项验收尚需补齐，不能把批准当作功能已经完成。
