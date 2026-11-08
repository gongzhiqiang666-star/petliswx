# M16 模板变量绑定维护

## 源码规则与新落点

PHP来源：`application/admin/controller/data/LbDictbindkey.php`继承Backend的日常CRUD；`application/admin/model/LbDictbindkey.php`关闭所有自动时间；add/edit表单维护BindTable、ColumnName、DescName三个必填字段；validate类没有附加规则。原表三个字段均varchar(50)，没有名称唯一约束。不添加旧系统没有的重复名称或表列枚举限制。

原`application/common.php:562`由预定业务表循环读取绑定字典，再以`{DescName}`替换对应列值。Java原有`LegacyOrderDetailService.renderBody`使用六个固定业务来源，绑定值只用作结果Map的字段键，不拼接SQL。新维护保留自由文本/历史记录，未知表或字段不会自动产生替换结果，不给其任意SQL执行能力。

新落点：`LegacyTemplateBindingService`、`LegacyTemplateBindingController`，接口`/admin/legacy/template-bindings`；PC“模板变量绑定”页面支持三字段维护、查询分页、保存重开、删除确认。使用新系统的标准表格表单，不模仿旧页面组件。

独立参数`legacy-template-bindings`：enabled控制读和前端入口；writeEnabled控制新增修改；deleteEnabled在前两项开启后控制删除；严格布尔值、Java ADMIN。默认关闭。每次维护使用配置/用户锁、请求防重、版本校验和同事务审计。无DDL、无时间字段写入、无正文或主单数据回写；替换服务直接读字典，无须新增作业或同步任务。

## 已验证

- `LegacyTemplateBindingLocalDatabaseTest` 4项：维护后由实际数据库读取并替换报告正文；AnimalName→MasterName改绑生效，删除后原占位符保留，原主单不变；修改版本/请求号冲突/重放；自由字符串和重复项保留；字段长度/必填、权限与三层开关。
- `LegacyTemplateBindingAtomicLocalDatabaseTest` 2项：审计失败时新增回滚、修改/删除恢复原记录，使用实际Spring事务，不用手动改状态冒充业务验收。
- `LegacyTemplateRenderRulesTest` 1项（原40条正文规则样例）回归通过；PC现有138项通过；JS/Python语法、diff检查通过。
- `python3 scripts/local/test-template-binding-live.py`：实际PC→专用Java18082→隔离MySQL13306，新增绑定AnimalName、改为MasterName、保存重开核对、确认删除，页面错误0。发现列表按真实查询参数限定合成名称，不替换接口响应。首次脚本误匹配同名meta description，限定input后完整复验通过；产品代码无须为此修改。
- 成功写响应返回前记入0600私有恢复文件；凭请求号及HEX读取的审计JSON验证合成记录归属，再清理自己的记录和审计，模块原参数恢复，恢复文件删除，专用服务停止。未触发真实发送、生成或发布报告，未改PHP或数据库结构。

## 仍待完成

这是变量绑定日常维护子项，不是M16两轮模块复核完成。完整历史模板、日期异常样例、重名变量的历史实际用法、编辑器相关业务素材能力、PDF标题/生成与发布、真实旧链接及发送链路继续纳入原主计划。导入/批量等继承能力按实际业务依赖核对，不因本页没有入口而删除能力。维护后替换正文已在真实数据库服务测试验证，PC验收本身只证明维护操作，不冒充全流程报告发布。
