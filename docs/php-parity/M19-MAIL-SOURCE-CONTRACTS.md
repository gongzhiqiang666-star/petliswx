# M19 业务邮件源码契约

只读参考原 `application/admin/controller` 源码。以下为代码核对，不代表业务发送已经接通。

| 业务 | 来源 | 收件人和环境覆盖 | PDF 与成功回调 |
|---|---|---|---|
| 收样通知 | `project/LbListdiagnosisproject.php::sendmail` | 原联系人 Mail 不 trim；环境 app.email 为真时仅替换 To，BCC 保持配置 | 无附件；成功后直接更新主单 IsSendNoticeMail=1；原全局缓存 sendmail 限制5秒 |
| 报告邮件 | `Base.php::sendmail` | 原联系人 Mail 使用 PHP trim；环境 app.email 为真时同时替换 To 和 BCC | GenterPdf 计算路径后要求文件存在；实际 attachment 行已注释，不能按财务邮件默认附加文件。成功直接SQL更新明细 IsSendMail=1、SendMailDt=当天日期，再更新父单 SendReportDt=当天日期、SampleStatus=3；原全局缓存 base_send 限制5秒 |
| 财务月报 | `finance/Hospmonthpeport.php::send` | 按当前医院与联系人ID读取 Mail，不 trim；环境 app.email 为真时仅替换 To，BCC 保持配置 | 生成财务 PDF 并作为附件；发送成功后按 HospId/PayMonth 更新或新增 lin_lb_noticemail.IsSendMail=1 |

共同配置为原 SMTP 编号1；用户名同时为发件地址，显示名按字面替换移除 `@laboklin.cn`。PHP 环境字符串 `0` 为假，不发生覆盖。

## 已实现及证据

`LegacyMailRoutingRules` 按三类业务保留上述收件人与覆盖差异，包括 PHP trim 默认字符集合。`LegacyMailTransport` 新增显式业务类型入口；兼容旧调用的两参入口使用财务规则，不声称该入口适用于所有业务。报告本机 SMTP 信封验证：开启环境覆盖后不向原 BCC 地址投递。具体状态与附件业务约束仍待业务发送服务执行，不能由传输组件代替。

## 待接入及兼容边界

- 收样通知/报告原POST按联系人ID直接查询，存在跨医院选择风险；新系统应继续采用医院范围内联系人授权，保留正确收件能力并记录安全边界差异，不能将越权查询当作业务功能照搬。
- 三类成功回调均需独立核对直写SQL与模型回调的区别，报告成功路径不能额外触发明细模型回调清空正文或重算金额。
- 发送明确接受后数据库失败，只恢复业务回调；结果不明须核对，不自动再次发送。新增邮件队列表见 `docs/database/pending/007-mail-outbox.sql`，已批准且在本机执行，旧结构核对未变；队列业务代码仍待接入。

## 收样通知模板维护增量

原模型 LbDictmailreportnotice 字段为 id、tplname、tpltext、is_active、createtime。新增默认状态0禁用、1正常，只在新增时写秒级创建时间，编辑不改创建时间。原通知选择器未按状态过滤，不能因迁移丢掉历史模板。

已纳入现有模板维护白名单 sampleNotice，PC业务模板中可选择“收样通知模板”，复用Tiptap、权限、能力开关、幂等、版本、事务和审计。维护不发送邮件、不修改通知状态。

8项模板数据库业务测试及3项既有模板原子回滚测试通过，PC既有139项通过。真实PC→Java→隔离库完成新增、默认状态0、编辑正文和状态1、保存重开及删除，浏览器异常0。合成数据和审计已清理、参数已恢复、临时后端已停止。创建时间保留、空正文及非法状态拒绝已验证；变量替换、业务发送、历史数据与完整两轮复核仍待完成。
- 历史PDF、原报告链接、编辑后的最终正文、原限频行为、并发及失败恢复仍待真实业务页面/历史数据验证。三类业务均不能因传输专项通过而标记迁移完成。
