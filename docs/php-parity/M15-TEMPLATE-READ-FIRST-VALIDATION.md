# M15/M12 明细表单模板与选项读取首批

原依据：明细add.html项目选择custom IsActive=1；价段选择按项目、无启停过滤；tpl/LbInfotesttypemodel.index按InfoTestTypeId列出模板ID/名称/创建时间并decodeUnicode；模板edit.html的名称/ModelInfo解码后交给编辑器；邮件模板表保存独立model_type/model_content。

## 新接口

均在LegacyOrderDetailService/Controller，沿用legacy-order-details读开关和管理员权限：

- GET `/admin/legacy/order-details/tests`：名称/编号搜索、分页、启用项目按原ID升序；没有固定截取一份列表供下拉框冒充完整目录。
- GET `/tests/{testId}/prices`：价段分页，保留正常/禁用，金额文本保留精度和尾零。
- GET `/tests/{testId}/templates`：只读该项目绑定模板，列表不带大正文，名称和ModelName1按原decodeUnicode处理。
- GET `/templates/{id}`：原ModelName/ModelInfo完整保留，另提供displayName/displayHtml供原解码显示；不保存、不清理或改写原HTML。
- GET `/mail-templates`及`/{id}`：列表不携带正文，详情原样读取model_content。没有读取邮箱服务密钥、发信或加载远程页面。

## 验证及限制

新增2项隔离库测试：项目停用后不进入新选择但其原价段/模板关系仍可读；项目模板范围正确，列表不泄漏/搬运大正文；报告/邮件HTML原文不变且显示解码正确；模块关闭后模板接口409。事务fixture仅独立随机数据，结束回滚。

首次测试源码有HTML属性引号转义错误，编译失败；已修正测试字符串后重跑。该次编译失败不计为功能测试通过或复核轮次。完整回归结果见本文件最终追加及`/tmp/petlis-template-read-full-final.log`。

当前为服务/API，不是PC模板页面已完成。未实现模板编辑、新建删除/绑定迁移写入、Tiptap原HTML往返保真、filterContents变量替换、GenterMailTitle默认标题和明细GET原预填内容。源代码核查已确认filterContents的替换顺序、strpos位于0的条件、1900日期规则、246项目名称排除、链接追加等，需要逐项保留而非做通用字符串替换替代。

模板/原价搜索与详情没有接入真实PC表单或进行真实HTTP选项验收，本轮证据是本机真实数据库回归。M15、M16及M12均未整体完成，两轮逐模块复核未完成。无DDL或PHP修改。

## 最终结果

完整后端579项全部通过、无失败/错误/跳过；临时主单、报告模板及邮件模板残留0。明细模块参数行仍未配置（默认关闭），事件调度OFF；仅重启已确认属于本仓库的8082后端；596份原PHP基线哈希不变。
