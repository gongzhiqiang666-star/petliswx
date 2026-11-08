# M16 正文占位符渲染首批

## 原业务规则迁入

`LegacyTemplateRules.render`按原common.php::filterContents顺序：decodeUnicode；报告链接/付款链接按原strpos条件；去分页标记；当前日期；六组绑定替换；邮件type=1追加报告附件链接；最后诊断项名称。保留索引0占位符不替换、同一邮件可能重复链接、NULL/缺字段保留占位符、嵌套替换顺序及无HTML转义的原内容。

绑定值保持原PHP字符串转换（含0/布尔值）；仅UTF8长度19且原日期规则识别为正时间戳时截成日期。1900/非正Unix日期保留原完整文本；覆盖ISO空格/T、年首斜杠、欧式日首、美国月首、日期溢出、月/日0、非法月/小时及秒60。尚不宣称完整仿真PHP strtotime任意自由文本语法，其他19字节可解析形式需追加原样本。

## 原PHP差分

脚本capture-legacy-render-reference.py原样抽取filterContents和decodeUnicode，在既有PHP7.4执行。只对model/Db读操作提供合成适配器；URL使用明确标记的petlis.invalid/TEST-7测试适配器，无真实网络/密钥/加密操作。

render-reference.json有40例（先32，新增8种日期边界后修复ISO T及欧/美式解析），逐例Java输出完全匹配，包括完整HTML、空格、链接标签与顺序。此参考验证内容替换，不是假称真实数据库聚合/加密已运行。

## 真实数据库组合

LegacyOrderDetailService.renderBody为内部组合入口：读取原主单及医院/性别/物种/采样类型、原绑定字典与vwdiagnosisdetail；只有固定表/视图，ColumnName只做映射字段读取，不拼任意SQL。元数据字段查找保留PHP大小写敏感：已捕获Spring大小写不敏感行映射导致错误替换的失败用例，转换为普通原键映射后通过。

诊断项名称由原视图GROUP_CONCAT，按主单且排除246，无新增启停过滤或排序。隔离库fixture明确断言246明细存在于视图，再验证输出确实排除，未用一个已被视图丢弃的空样本冒充排除验证。主单/明细/绑定数据全合成，事务回滚；读取的原246目录记录未修改。

入口仍受legacy-order-details读参数和管理员校验；明细>0必须属于当前主单（跨单拒绝属于新数据范围保障）。serverLink只能由后续服务端链接构造器提供，不向HTTP开放任意链接字段；当前没有对外渲染HTTP接口。

## 验证与剩余

原PHP40例差分及实际本机库组合通过。新增内容和其他已有模块完整回归结果见下方追加及/tmp/petlis-template-render-full-final.log。

尚未接真实兼容链接/私有参数（M25）、HTTP渲染、原默认模板1/118内容与标题完整预填、Tiptap往返、PC明细页面、邮件/PDF输出。原HTML不通过不安全页面预览执行，不发送邮件或访问任何链接。M16、M12以及两轮整模块复核仍未完成。无DDL、原PHP/镜像修改。
