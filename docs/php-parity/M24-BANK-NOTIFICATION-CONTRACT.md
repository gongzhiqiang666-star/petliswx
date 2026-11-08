# M24 主动付款通知：验签、原规则回写及重复处理

## PHP 源码规则 → Java 实现

冻结只读依据 `application/common/library/gnete/Pay.php::notice` 第94–119行、`application/index/controller/Project/Pay.php::Callback` 第238–296行。没有修改PHP或覆盖层。

- 原始请求字节RSA-SHA256验签，读取 `response.msgBody`；不把重新序列化后的JSON拿来验签。复用已核对的协议处理，缺签名、坏签名、重复JSON键、尾部未签内容等失败拒绝。
- 主动通知以 `tradeStatus=00` 为成功条件；不擅自加上查询入口才要求的 `retCode=00000`。原交易号、渠道号、整数分金额还须与已发送的本地支付请求相符。
- 通知的流水BuyerId取 `buyerLgnId`，查询仍取 `subOpenId`；入口不同不合并字段来源。先成功的入口保留其买方字段，后到通知/查询不能覆盖或新增流水。
- 共享原直接SQL回写：AliTradeId、仅未付明细PayStatus2=3及对应父单IsPayMoney=3、一条TRADE_SUCCESS流水。原IsSuccess/UpdateDt、正文/PDF/邮件、发送时间和两个资金池均不改。

## 接口、开关与事务

新增精确POST `/payment/legacy-bank/notify`，无需用户登录。只有这一POST路径免登录，其他银行管理操作继续原ADMIN/本人请求控制。原文最多1MB，超大413、篡改或业务不匹配400、通道/通知未开启503，均返回无敏感信息的fail。成功和相符重复通知返回HTTP200、纯文本 `success`，与原供应商确认格式一致。

Gateway仍默认DISABLED，只有已限定的自有TEST_LOCAL可配置。`legacy-bank-payments.enabled` 与 `notifyEnabled` 都必须严格布尔true。未注册的订单、未发送的READY请求、缺少/不一致商户身份、金额或渠道不符不能记付款；通知无权创建支付意图。

查询和通知共用原请求及配置行锁、同一结算事务。通知成功使旧executionToken失效；在途查询/发送的迟到结果不能降级已付款状态。重复成功通知只核对已有结果，不重复扣款/记账，不重写买方字段。不同金额的迟到通知也不能把SUCCEEDED改成UNKNOWN。

匿名处理不借用或修改用户登录上下文。审计归属原请求创建人，action/source明确为LEGACY_BANK_CALLBACK；不是伪造一次用户登录。

## 验证

Java39项通过，0失败/错误/跳过，包含此前35项加：原文通知验签且不发起网络、通知buyerLgnId/无登录上下文/重复及迟到发送、独立开关/未发送请求拒绝、金额/商户不匹配与已付款不降级；原查询先成功后通知也不覆盖subOpenId。

`python3 scripts/local/test-bank-payment-quote-live.py --notify` 实际Chrome→Java→13306与自建127.0.0.1网关：

- 自有支付准备首次响应丢失后同键重试，仅一单；prePay受理后断开响应进入UNKNOWN。
- 使用临时合成银行密钥签名，POST通知不带Authorization。篡改金额的报文400，1MB+1报文413，均不能记付款。
- 同时提交匿名通知与登录查询：本次通知先取得结果，查询复用SUCCEEDED而未再出网；一次prePay、零次payQuery、最终一条流水，付款标志正确。
- 相同通知再次HTTP200 success；之后再查询不出网、不增流水，刷新PC显示已核对付款成功。接口实际返回格式经过断言。
- 除自有临床两处付款标志及自有支付/流水/审计之外，既有支付、资金、正文及其他临床字段全行哈希一致。临时密钥、网关、专用18082、夹具和开关全部清理/停止/恢复。

本轮未改PC源码；PC155项最近一次检查保持通过。通知/查询两种先后顺序由数据库专项分别验证；真实页面验证本次只观察到通知先赢，不冒称所有并发时序/崩溃场景已验收。

## 差异与剩余项

- 原PHP重复通知返回status no ok；新系统对相符重复返回success确认，业务副作用仍一次，避免银行无限重投。原PHP原始报文日志不照搬，复用Java结构化审计。
- 未启用真实收款、外部通知注册、定时任务或DDL。实际生产/供应商证书与通知URL配置、真实沙箱及原SDK运行差分仍未完成；当前测试notify URL不作为真实供应商注册地址。
- 后续[退款/查询请求接续](M24-BANK-REFUND-CONTRACT.md)已做本机PC验证，但正式结果映射与退款财务/跨端联动仍未完成；历史旧支付、客户/小程序授权、新旧PHP并发及进程中断恢复仍未完成。新通知只能处理本系统登记且已发送的意图，不能拿此宣称旧历史通知已全部接续。
- M24整模块两轮复核仍未完成；不宣布网银迁移完成。
