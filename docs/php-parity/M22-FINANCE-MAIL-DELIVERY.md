# M22 财务账单邮件投递增量

## PHP源码规则

只读参考 finance/Hospmonthpeport.php::send：医院与联系人联合查询，不按isFinanceContact过滤；收件Mail不trim，环境app.email只覆盖To，BCC保留配置。SMTP配置编号1；用户名作发件人，显示名移除@laboklin.cn。dctcontents生成PDF，文件名为ReportpdfTitle加.pdf，作为附件；邮件使用ReportTitle与mailcontents。

成功后查HospId/PayMonth的首条通知记录，已有则设置IsSendMail=1，否则新增并置Remark为空。LbNoticemail模型时间类型为datetime；ThinkPHP Model::save在无字段变化时先返回，不更新UpdateDt；新增设置CreateDt/UpdateDt，0→1只更新标志及UpdateDt，不改CreateDt/Remark。任意期间仍使用空PayMonth，不能擅自改成新月份规则。财务路径未发现收样/报告的五秒全局限频，适配器不额外增加。

## 新系统落点

LegacyFinanceMailService接入现有lin_mail_message/lin_mail_attempt，通过共用LegacyBusinessMailService防重、领取、UNKNOWN核对、ACCEPTED回写恢复与审计。保留原收样/报告签名域及业务回调；新类型FINANCE_MAIL有独立legacy-finance-mail参数。enabled、writeEnabled、sendEnabled、reconcileEnabled均严格布尔，默认关闭；实际SMTP还受独立传输模式限制，无调度。

PC月报中先编辑两份正文，携带打开账单时的版本进入医院联系人确认窗口，可保存待发送、发送、查看期间记录、取消READY、恢复ACCEPTED回写和人工核对结果不明。模板切换不会用新版本偷偷替代原账单版本；资料变化须重新打开。接口位于admin/legacy/finance-mails，全部要求管理员及开关。

PDF在数据库事务之外生成，独立私有UUID目录保存原附件名和SHA256；同名不覆盖。读取在领取事务内校验并取得固定字节，SMTP在事务外使用这些字节，避免校验后再次读到另一份文件。源医院/联系人/账单明细变化或附件变化会BLOCKED，不能直接发送。数据库提交结果不明时宁可保留私有孤立文件，不删除可能已提交任务的附件；暂不做后台清扫。此项是技术隔离与恢复，不改变账单内容。

## 验证

8项财务数据库业务测试覆盖：本机SMTP原名/原字节附件、重复请求/重复dispatch只投递一次、结果不明核对不重发、成功后回写失败只恢复回调、已发送记录时间不变、未发送记录更新范围、源金额/附件变化拦截、医院联系人范围、期间空PayMonth。另有原报告6项、收样10项、传输7项、路由3项回归通过，共34项。附件文件专项2项覆盖同名独立、非法路径/控制字符/符号链接，两项通过；测试临时目录先取真实路径，避免macOS系统/var符号链接被正确的附件防护拒绝。

182项前端测试通过。真实Chrome→Java→本机127.0.0.1:13306/laboklin→自建127.0.0.1 SMTP：从月报编辑账单和邮件、选医院联系人、生成真实PDF并作为附件投递；原名、附件SHA256、正文修改标记和12.50保持，邮件专属文字未混入PDF。仅接受一次，Message-ID与尝试记录一致，重放SENT不再发。原订单与检测明细全部字段/正文不变，原通知表新增成功标志。浏览器异常和外部HTTP请求均0，测试未发送AUTH、未连接源SMTP主机。参数恢复、合成数据清理、专用后端与SMTP停止。首次清理脚本目录名称错误导致残留，已按精确自有ID恢复清理并修正，复验清理通过。

## 差异、剩余与启用边界

原文件同名覆盖改为私有独立快照，保留原附件名和内容。联系人限本院；源版本和未知结果核对是并行及防重保护。没有修改PHP、旧表结构、触发器或视图，没有新增DDL。

M17原tr强制分页适配仍待已有用户决定，本轮实际PDF字节与内容检查不能替代正式版式验收。真实供应商TLS/回执、财务多进程恢复、旧PHP同时外发的去重、复杂历史及完整两轮模块复核仍未完成。新队列不能阻止未改造PHP自行另发邮件，真实外发与生产启用单独安排。
