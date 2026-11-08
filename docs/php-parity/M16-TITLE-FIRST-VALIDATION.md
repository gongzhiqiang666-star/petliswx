# M16 默认邮件标题首批与正文渲染合同

## 已迁移标题

原common.php::GenterMailTitle从vwlistdiagnosisproject取InfoAnimalAnimalName/AnimalType/AnimalName/MasterName/UserNO，拼接“物种品种宠物（畜主）（单号）的检测报告”。PHP empty对null、空串、字符串0使用默认标题；自定义标题只trim两端英文逗号，不trim空格，不改原字段空格/标点。旧注释中的“[重要]”前缀不是现行输出，未加入。

新LegacyTemplateRules.mailTitle及LegacyOrderDetailService.mailTitle直接读取原视图，不重写视图；GET `/admin/legacy/order-details/orders/{orderId}/mail-title`沿用明细只读开关和管理员校验，无邮件发送或保存。

脚本capture-legacy-title-reference.py抽取原样GenterMailTitle，通过仅替换model读取的合成适配器，在既有PHP7.4纯运行8例；title-reference.json记录原文件哈希及输出。Java逐例匹配；隔离库另验证原视图与字段空格、覆盖标题、参数关闭。标题服务要求实际主单存在，非法主单时受控404；不声称原任意非法输入等价。

本接口对应默认GenterMailTitle原函数；Base.mailtitle另外追加检测项目名的行为尚未迁入，不能把标题基础函数迁移记成整个Base.mailtitle接口已完成。

## filterContents实施合同（渲染首批已实现，链接/页面待接续）

按原函数顺序，不做通用模板引擎替代：

1. 读取原主单，先decodeUnicode，包括原URL/字体修复。
2. 生成付款链接及报告附件链接字符串。原strpos判断对位于索引0的占位符为false；报告链接替换和type=1的用户付款链接替换必须按原条件，不悄悄改成contains。
3. 删除`----分页符号----`；替换当前日期。
4. 按ListDiagnosisProject、InfoHospital、DictSex、InfoAnimal、InfoSampleType、VwDiagnosisDetail六组顺序取原绑定。表名固定白名单，ColumnName仅按原行字段取值，不把元数据拼进任意SQL；空行/NULL值保留未解析占位符。
5. 原值strtotime>0且UTF8字节长度恰好19时输出日期，否则原文；1900哨兵/非正Unix日期不会被统一裁成日期。PHP日期溢出/可解析非ISO文本需要参考用例，不用Java默认解析冒称完全等价。
6. type=1总会追加报告附件链接，即便前面已替换一次；不得自行去重而改变邮件内容。
7. 最后替换诊断项名称：原vwdiagnosisdetail GROUP_CONCAT(TestName)，按主单筛选且排除InfoTestTypeId=246，不强制加新状态过滤或改变顺序。

链接加密与付款/报告URL兼容归M25并需私有参数，不能拿模拟URL当实际业务链接已迁。纯渲染参考可注入明确标记的链接适配器，只验证内容替换，不能混称已验证加解密。

## 剩余与验证边界

未完成正文变量渲染、默认模板/邮件正文预填、项目名追加标题、PDF标题及生成、Tiptap往返保真、实际PC表单/HTTP标题验收。M16仍partial，两轮整模块复核未完成。新标题为只读纯计算，不修改原PHP、数据库结构或业务数据。

## 本轮最终结果

后端全量581项全部通过、无失败/错误/跳过，`/tmp/petlis-title-full.log`。运行服务重启后通过真实登录验证标题接口在默认关闭参数下返回409；临时主单残留0，事件调度OFF，596份原PHP基线哈希不变。尚未做启用参数下真实HTTP标题/页面验收，不把数据库单元/集成验证冒充页面通过。

最新进展：正文渲染纯规则与原表/视图取数已首批实现，原PHP40例及真实库组合验证见M16-BODY-RENDER-FIRST-VALIDATION.md；下文历史“尚未实现”描述不再代表现状，真实链接/预填/页面及两轮全模块复核仍待。
