# M24 网银封包、验签及关闭默认通道

## 源码链

冻结只读：`gnete/Pay.php::to/notice` → `updc/bean/Client.php::apiRequest` → `Signature.php::sign/verify`、`CharacterUtil::encodeHex/decodeHex`、`RequestApiUtil::request`。未读取原私钥内容，未输出或提交原证书密码、商户号或AppID。Java源码仅使用运行时传入的密钥对象；测试密钥全部临时生成，与原生产证书无关。

`to`：业务JSON依次为sndDt、busiMerNo、msgBody、notifyUrl、remark，`json_encode(data,true)`的true按旧PHP转换为标志1（JSON_HEX_TAG）。因此中文采用Unicode转义，斜线转义，尖括号采用\u003C/\u003E；字符串金额、前导零和整数userNo类型保留。

`Client`：外层按app_id、method、timestamp、v、sign_alg、biz_content依次使用http_build_query默认RFC1738编码（空格为+，星号及波浪号转义），对该原文做SHA256withRSA/PKCS#1 v1.5签名并转小写十六进制，最后追加sign再编码。版本1.0.1、算法码1；真实业务四种方法为gnete.upbc.cashier.trade.prePay/payQuery/refund/refundQuery。请求头沿用text/json;charset=utf-8，不能因为实际是表单正文就未验证地更换旧接口协议。

依据：[PHP json_encode及标志示例](https://www.php.net/manual/en/function.json-encode.php)、[http_build_query默认编码](https://www.php.net/manual/en/function.http-build-query.php)，以及上述本机冻结源码。

## Java落点

- `LegacyBankProtocol`：固定外层字段及业务字段顺序，兼容原JSON/表单编码；RSA签名十六进制；原始UTF-8响应验签。用JSON词法位置识别最后一个顶层sign字段，取原文前缀重建末尾右花括号，保留内部空白、键顺序、Unicode等原字节，不反序列化再重新序列化签名内容。正文中的design/sign及嵌套sign属于数据，不误当顶层签名字段。
- 严格重复字段检测，拒绝无效UTF-8/非RSA/坏十六进制、尾随未签名内容和签名后的额外字段。只有验签成功才返回业务体；签名成功不等于支付成功，更不等于已核销，业务层还需核对订单归属、金额及状态。
- `LegacyBankGatewayClient`：默认DISABLED；TEST_LOCAL只允许明确的http://127.0.0.1:端口，不进行DNS代理转发；HTTPS模式只接受HTTPS并使用JDK正常证书与主机校验。当前未注册到下单调度器，没有生产配置加载/证书读取或HTTP支付入口；不因新增类自动启用通道。
- 网络调用拒绝处于数据库事务内，留待业务层先持久化意图再发送。无应用层自动重试、不跟随重定向、不记录原始签名报文/密钥。流式响应上限1MB；从发送到整个响应体完成有总超时，超限/超时/坏签名/异常HTTP均返回UnknownResult供后续核对原交易，不能据此新建订单或自动重发。

安全实现差异：旧to未检查apiRequest的验签返回值，旧HTTP工具跳过TLS验证并跟随跳转。新端不沿用这些信任边界缺陷；合法已验签业务内容仍按原规则处理，失败只进入待核对，不改动原支付状态。此处是实现选择说明，不声称用户另行批准了任何生产启用。

## 验证证据与边界

本轮Java18项：协议4、本机HTTP网关6、原核价3、原金额显示回归5；全部通过、零失败/错误/跳过。

- 手册编码向量及冻结源码规则：中文/emoji、尖括号、斜线、空格、引号、星号/波浪号、字符串金额/整数类型、前导零及外层顺序。
- 独立系统OpenSSL签名→Java验证、Java签名→OpenSSL验证双向通过，临时RSA2048密钥；未使用真实证书或网络。此证据验证加密算法互通，不冒充原PHP SDK完整运行差分。
- 自建127.0.0.1 HTTP网关逐个收到四种原方法，独立验请求RSA签名，返回另一套RSA密钥签名的响应供Java验证；每次只收到一次。HTTP302不跟随，坏签名不返回业务体；响应头已到但正文不结束时仍能总超时；超过1MB的未结束正文及时取消。默认关闭、错误本机地址及数据库事务内调用均未建立请求。

该次协议测试未运行原PHP SDK或真实供应商沙箱，也未建业务支付单。后续已接[支付单准备](M24-BANK-PREPARATION-CONTRACT.md)及[本机发送/原单查询回写](M24-BANK-EXECUTION-CONTRACT.md)，仍无真实收款、退款、客户通知、DDL或定时任务启用；M24未完成。

## 接续支付单持久化的已查事实

本机只读核对lin_order_idempotency：14列、唯一键(user_id,operation,idempotency_key)，无外键；order_id允许NULL，response_body是JSON，状态是VARCHAR。现有Java用途所有读取/更新明确限定operation=ORDER_CREATE。现已以独立LEGACY_BANK_PREPARE命名空间复用既有Java幂等表，实现支付准备、发送及查询结果状态；没有新增表结构。生产收款及整模块复核另列缺口。

需要继续实现及验证：银行卡订单编号/主从分配/请求快照、先提交再发送及未知结果恢复、PHP原AliTradeId门槛/直接SQL成功回写、金额与归属核对、历史订单查询和退款、真实跨写入者并发及页面接续。新旧同时写入的安全性不能仅靠新Java加锁宣布解决，原PHP读后无条件更新的并发风险仍需实际联调和切换控制。
