# M24 网银发送、原交易查询与付款回写

## PHP 源码规则 → Java 实现

冻结只读依据 `application/index/controller/Project/Pay.php::index/query`（第123–204行）及 `application/common/library/gnete/Pay.php`。未修改PHP、覆盖层或旧结构。

- `prePay` 使用原商户订单号、字符串分金额、bizType=14900、trxChnlType=01、subject/ordrDesc、医院整数userNo、USER_ID和111011。准备时不扣医院/个人钱包，实际网银收款也不当充值。
- `payQuery` 使用原支付订单号作为oriMerOrdrNo，新的查询请求号及当前上海时间yyyyMMddHHmmss作为oriTrxDt；未擅自改成原订单创建时间。
- 只有验签通过、retCode=00000、tradeStatus=00且原交易号/分金额与本地一致时才能回写。记录渠道号AliTradeId；仅PayStatus2=0的明细置3，并把这些明细的父单IsPayMoney置3。
- 使用直接SQL，不走明细模型保存回调；不改报告/邮件正文、PDF、发送日期、医院资金或个人钱包，不重算其他父单状态，不把IsSuccess置1，不修改原支付主单UpdateDt。
- 原网银流水：Subject/TradeBody取原Remark；TradeNoUser/TradeNoAli取原单号/渠道号，TradeStatus=TRADE_SUCCESS，Exterface/BuyMail空，查询入口BuyerId取subOpenId。保留PHP `sprintf('%.2f', trxTtlAmt / 100)` 两步IEEE浮点显示规则，仅用于旧流水格式；金额归属验证仍用整数分/精确十进制，不能把旧日志格式拿去计算钱包。极端金额9999999999999999分的旧日志格式回归为100000000000000.00，不擅自“修正”为其他值。

## Java 状态与控制

`LegacyBankPaymentExecutionService` 事务方法、`LegacyBankPaymentDispatchService` 无外层事务的编排、原GatewayClient配合：

1. READY → SENDING先提交，包含原商户身份、发送次数及唯一executionToken，再在事务外调用网关。
2. 有效收银台响应 → CASHIER_READY；仅表示银行受理，不能显示已付款。PC提供安全的“打开收银台”链接，未自动跳转。
3. 超时、坏签名、丢失响应或本地处理失败 → UNKNOWN。SENDING/UNKNOWN不可再次prePay，接口和页面都受控；只查询原交易。
4. 查询先持久化QUERYING及70秒租约，再发请求。租约有效时拒绝并行查询；租约到期可重新查询，不能改为重新下单。
5. 查询成功并通过业务校验 → SUCCEEDED；重复操作直接复用结果，不再向银行发送或新增流水。旧executionToken的迟到响应不能覆盖新的查询/结算状态。
6. 验签通过但金额/订单/渠道号异常、原单编号重复、既有渠道号与流水不一致等保留待核对；不强行标成功。

复用 `lin_order_idempotency` 的LEGACY_BANK_PREPARE命名空间和JSON状态，无DDL。准备重放返回当前持久状态，而非永远READY。原参数行锁串行化本机新Java写入；并未证明冻结PHP也遵守这些锁，新旧并发仍待专门复核。

PC使用原核价/保存页的本人请求记录：按状态显示发送确认、查询原交易、受理后打开收银台。接口为POST `/admin/legacy/bank-payments/requests/{key}/send`、`/query`。当前只有ADMIN且本人请求可操作；客户授权尚未接续。

`enabled`、`sendEnabled`、`queryEnabled` 均须严格布尔true；前后端都检查。Runtime默认DISABLED；当前只允许显式TEST_LOCAL、127.0.0.1端口、自有私密目录内600权限的临时PKCS8/X509 DER密钥。禁止把生产凭据放进参数表/仓库。真实HTTPS通道不能通过现有配置直接开启，原PFX/生产证书、供应商沙箱及启用另行接续。

## 验证

- Java35项通过，0失败/错误/跳过：既有核价/落库/协议/互通/HTTP异常，加发送领取、禁止重发、查询结算字段与正文副本不变、重复结算、迟到响应与错误金额拒绝、正常收银台受理不记付款、流水故障回滚及恢复、默认关闭、拒绝外层事务网络、原日志浮点格式。
- JDBC专项使用独立测试事务管理器及真实savepoint，修正原测试嵌套事务误回滚夹具的问题；明确检查原临床夹具仍存在，不把“整测试回滚后查不到行”当业务原子性证明。生产事务配置没有修改。
- PC155项通过，包括发送确认、只查原交易、安全收银台链接及原准备重试。
- `python3 scripts/local/test-bank-payment-quote-live.py --dispatch`：实际Chrome→Java→本机13306→自建127.0.0.1签名网关，银行受理prePay后断开响应；PC进入待核对，直接重复发送接口409；一次查询按原规则结算。重复查询不再次访问网关。仅一次prePay、一次payQuery、一条流水。
- `--dispatch-rollback`：同流程先令查询响应subOpenId超出原字段容量，在实际默认Java事务下先触发回写失败；第二次查询前网关夹具只读检查AliTradeId空、临床及父单付款标志未变、流水0条，证明全部回滚；随后同原单查询恢复。最终一次prePay、两次payQuery、一条流水。
- 两种真实页面验收都包含准备成功响应丢失后的同键重试，保存仅一单；刷新恢复本人记录。除自有单两处付款标志及新增支付/流水/审计，临床其他字段、正文、资金和既有支付记录全行哈希一致。夹具、临时参数、合成密钥、模拟网关和专用18082均已清理/恢复/停止。

上述均为本机合成网关，不是供应商真实收款或退款测试；无DDL、真实外发、调度或生产操作。

## 未完成与差异

- 供应商真实沙箱、原证书/私密参数、真实HTTPS启用；原PHP SDK运行差分仍未完成。
- 银行主动通知Callback（包括buyerLgnId来源）、退款/退款查询实际业务、历史旧订单接续及客户/小程序付款授权仍未完成。底层四方法封包存在，不当退款业务已交付。
- 浏览器刷新时原准备请求仍在进行的持久待确认恢复、跨请求键同明细收款冲突、新旧双写并发、进程中断及完整状态/重复/并发场景继续补齐。
- 新准备记录保存商户身份；此前缺少身份的旧准备记录不能盲目发送，保留人工核对，不重建原单。原Md5Id字段容量、医院整数范围与宽松日期参数差异仍见准备契约。
- 对原PHP未做的订单/金额归属检查采用失败保留待核对；不改变正常相符交易的业务结果，也不删除原交易能力。
- M30混合状态选择仍待用户决定，未顺带执行汇总状态修复；M24整模块两轮复核尚未完成，不能宣布网银迁移完成。
