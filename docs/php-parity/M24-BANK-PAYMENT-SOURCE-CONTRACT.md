# M24 原网银支付：源码契约与首批核价

## 来源和范围

冻结只读：`application/index/controller/Project/Pay.php`、`application/common/library/gnete/Pay.php`、`application/index/controller/Pay.php`。没有修改PHP，未调用原生产支付地址、原收银台或退款接口。证书/密码/商户配置不打印或提交。

原网银 `gnete.upbc.cashier.trade` 与现有 Java 微信JSAPI不是同一支付实现，不能用微信订单替换旧交易或丢失旧交易追溯。

## 原实际业务规则

- Project/Pay.index 按医院月份或送检单号查询 `VwDiagnosisDetail`；仅 IsCheckPass=0、PayStatus2=0。
- 月份：InfoHospitalId绑定指定医院，ReceiveSampleDt >= 起始日期且严格 < 月末23:59:59。原PHP接受更广的日期文本，当前首批新接口仅提供正常YYYY-MM月份，其他历史日期形态仍待核验，不冒充全参数等价。
- 单号：按UserNO查询；InfoTestTypeId=246可在未收样时参与，否则ReceiveSampleDt必须不等于1900-01-01。此分支原代码不按传入hospid过滤，首批仅ADMIN核价并显示实际医院及跨医院提醒；客户授权范围在M25接续，不能把ADMIN接口直接开放给医生。
- 费用：逐项bcadd(total,TestPrice,2)，源showPrice用于显示/金额对比，bcmul(total,100,0)作为分。Java BigDecimal每步向零截断两位，返回精确amount/amountFen，displayAmount复用已验证LegacyPricingRules.display保持原sprintf表现，PC显示该值。
- 原下单写lb_payordermain及lb_payorderdetail，HostId固定0、Md5Id构造、月份/单号备注、分金额及原商户编号生成还未迁移；不擅自把HostId改成医院编号。之后prePay返回chnlUrl跳转。原过程不是完整原子/防重复方案，需补可靠请求与未知结果核对，不能用本次只读核价冒充下单完成。
- query在retCode=00000、tradeStatus=00时处理；Callback先经notice解析/验签后处理成功状态。两者以AliTradeId非空作为已处理门槛，写渠道交易号；仅PayStatus2==0的明细置3，同时其父单IsPayMoney=3；写一条TRADE_SUCCESS流水。不经过明细模型保存回调，不改报告正文、报告发送日期或医院资金；原回调未把IsSuccess置1、未主动更新支付主单UpdateDt，不能凭字段名称另加状态副作用。原网银收款不等于医院或个人钱包充值，不合并两个资金池，也不擅自叠加新优惠券折扣。
- query流水BuyerId取subOpenId，Callback取buyerLgnId；金额为供应商trxTtlAmt/100后两位；Subject/TradeBody取支付主单Remark。不能统一这两个入口而丢失字段来源差异。
- Project/Pay.refund/refundQuery为硬编码旧订单的演示调用；必须迁移底层退款/查询能力并参数化、核对原交易，不复制硬编码订单与金额。根Pay.callback为空，根Pay.index是一分钱的集成演示且在var_dump后exit；无临床业务订单落库，不当作实际生产下单业务照搬。

## 当前落点与证据

`LegacyBankPaymentQuoteService`、`LegacyBankPaymentController`及PC `#/legacy-bank-payments`：按单号/医院月份核对待付项目、医院、动物、收样时间、各项金额与总额。GLOBAL/0/ACTIVE的legacy-bank-payments.enabled严格布尔控制菜单和接口，ADMIN独立权限。下单/供应商查询/退款能力明确false，没有假按钮或公开支付回调。

- 新库专项3项：未收样246、普通已收样、已付/免除排除、医院月份及闰年末秒排除、只读无回写、参数与权限/开关。
- 共用原金额显示回归5项，合计8项Java测试通过、0失败/错误/跳过；PC150项通过。
- `python3 scripts/local/test-bank-payment-quote-live.py` 真实PC→Java→13306：两张合成主单、五条明细，按单号12.50元及按医院月账7.25元均由页面实际核对；显示所选项目且无付款操作。医院、主单、明细、正文副本、支付主单/明细、交易流水全行哈希前后一致；测试后恢复两项开关、清理自有夹具/审计并关闭专用后端。
- 测试夹具的初始收样/付款标志只用于构造查询前置状态，未把直接改库当业务操作或全流程验收。没有创建真实或模拟银行支付记录，没有扣款/退款/外部网络请求/定时作业/DDL。

## 差异及未完成

月付款排除最后一秒，而现有原月报口径包含该秒，可能出现旧月报金额与付款核价不一致。当前按PHP保留并显示范围；是否统一应在全流程对照时由用户决定，不能暗改旧规则。

仍未完成：原下单持久化/编号/金额比对/分配、真实供应商封包签名验签和默认关闭适配器、成功/失败/重复/并发回调、支付查询对账/未知结果、退款与退款查询、原链接及客户端授权、双资金池/跨端付款接续、与M30联调及整模块两轮复核。不能声明M24支付已迁完或可收款。

协议接续已补[封包/验签及本机关闭默认通道](M24-BANK-WIRE-CONTRACT.md)：底层四种方法签名收发、OpenSSL互通和错误/超时处理已验证；未接入支付单持久化或付款回写，不等于下单/退款业务完成。
