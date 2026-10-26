# 待办

## 真实短信接入（用户指定稍后处理，2026-10-24）

- 沿用原 PHP 系统短信服务。
- 资料库中 `laboklin_cn/application/common/library/Sms.php` 通过 `sms_send` Hook 调用短信插件；当前取回的 `addons` 未包含短信插件，`lin_config` 未发现供应商配置。
- 另一个历史 Java 项目里的 AlibabaSmsServiceImpl 为模拟发送实现，不能当作真实服务复用。
- 后续从线上 PHP 后台确认插件名、实际供应商、签名及验证码模板；密钥仅存本机私有配置，不进 Git、不发聊天。
- 当前接口未配置时明确报错，不回传验证码，不模拟发送成功。
- 验收：真实手机收到验证码；注册、找回密码、换绑手机号成功；过期、错误次数、频控和重复使用校验通过。

## 原库月报视图兼容（需先申请结构变更批准）

- `vw_hospmonthreport` 与 `vw_hospmonthreport3_sendmaildt` 在原库和本机副本均报 SQL 1055。
- 保留原定义和 SQL 模式。提出修复 SQL 后再申请批准。

## 原订单通知触发器状态文案（用户指定暂不修改，2026-10-24）

- `trg_order_status_change` 将状态 -3（实际待支付）显示为“已取消”，将状态 1（实际审核未通过）显示为“已审核”，且没有状态 4 的“已取消”文案。
- 用户明确决定暂不修改，保持本机和 Mac mini 原触发器不变。
- 待审修复 SQL：`database/pending/004-order-notification-status.sql`，不得自动执行。
- 后续业务通知应以应用内状态映射生成正确文案，不能直接展示触发器的错误文案。数据库触发器修复仍需再次批准。
