# M15 报告与邮件模板维护

## PHP 源码规则

- `application/admin/controller/tpl/LbInfotesttypemodel.php`：报告模板按 `InfoTestTypeId` 查询，名称按 `decodeUnicode` 显示；通用 Backend trait 提供新增、修改、删除。
- `application/admin/model/tpl/Lb_infotesttypemodel.php`：创建时自动写 `CreateDt`，日期时间语义；`updateTime=false`，不自动改旧 `updatetime`。没有保存后重生成历史检测正文的模型回调。
- 对应 add/edit 表单：模板名称、报告正文必填，所属项目在原表单隐藏字段中保存。这里换成可搜索项目选择，支持历史未关联项目0，不把0宣称为自动适用于所有项目。
- `application/admin/controller/tpl/LbDictmaildiagnosisreport.php` 及 `application/admin/model/LbDictmaildiagnosisreport.php`：名称/正文解码显示，创建和修改时间为整型秒。邮件模板表单正文不要求必填，空正文可保存。
- 两个 validate 类无附加规则，模型没有删除历史明细/文件等回调；本批不新增这类副作用。

## Java 与 PC 落点

`LegacyTemplateMaintenanceService` / `LegacyTemplateMaintenanceController`：`/admin/legacy/templates` 的能力、项目搜索、两类模板列表/详情/新增/修改/删除接口。表名和列名仅来自固定 kind 白名单，查询参数绑定，不接受任意表字段。

`legacy-templates.js`：统一风格的“业务模板”页面，报告/邮件切换、查询分页、项目搜索及历史ID回填、Tiptap正文编辑、保存重开、隔离正文预览及删除确认。正文无法通过现有编辑安全检查时保留源文并阻止保存，不静默删内容。预览不替换变量、不生成PDF，也不访问外部图片。

独立参数 `legacy-templates`：`enabled` 控制维护入口和读接口；`writeEnabled` 进一步控制新增修改；`deleteEnabled` 进一步控制删除。均须为JSON布尔值且当前用户为ADMIN。默认不启用，不自动插入生产配置。已有检测明细模板读取继续受其自己的 `legacy-order-details` 参数控制。

沿用旧表/字段，未新增DDL。报告更新不改创建时间和旧updatetime，邮件更新只改updatetime不改createtime。模板正文原样保存，不重新生成已有明细的正文副本。项目关联与原显示内容保留，UI可不同。

每次写操作锁定配置/当前用户并在同一事务写业务与审计，使用请求号防重和内容指纹校验；修改/删除使用完整原行版本指纹。超时后仅允许同内容同请求重试，不让不同内容复用未知结果请求。删除保留独立开关和确认，沿用旧硬删除模板规则，不清空历史检测明细。

## 验证证据（本机隔离副本）

- `LegacyTemplateMaintenanceLocalDatabaseTest` 5项通过：报告新增/修改/项目范围/时间语义/HTML内容、现有明细服务能读取已维护模板及项目选项、邮件空正文/秒级时间/创建时间保留、版本冲突/请求重放、删除开关/审计/重放、非法类型/必填/管理员/参数控制。
- `LegacyTemplateMaintenanceAtomicLocalDatabaseTest` 2项通过：新增审计失败整笔回滚；修改和删除审计失败恢复完整原行。本组通过真实Spring事务代理执行，清理仅限自己的合成记录。
- PC 现有138项测试通过；新增JS及Python语法检查、`git diff --check`通过。
- `python3 scripts/local/test-template-maintenance-live.py`通过：实际PC页面→Java18082→MySQL13306。新增报告、编辑正文、搜索关联项目246、保存重开；隔离正文预览；新增空正文邮件并删除。数据库核对中文、英文、变量文本及关联项目，页面错误0。发现列表通过真实查询参数限制为本次合成名称，没有替换业务接口返回数据。
- 成功写响应发回页面前将模板ID/类型/请求号写入0600私有恢复记录；不保存令牌。首次清理遇到mysql批量输出JSON转义，改为HEX读取审计JSON，核验创建归属后恢复清理，再完整复验通过；没有修改数据库结构或排序规则。
- 全部合成模板、自身审计和恢复记录删除，模块开关原值恢复；专用服务停止。未真实发送邮件、未生成或发布报告。

## 未完成与差异

本批是报告和邮件模板日常维护交付，不是M15/M16整体完成。尚需模板绑定字典完整维护、原编辑器中实际被业务使用的补充操作、原PDF预览/生成与发布、历史复杂模板编辑的完整验收及两轮模块复核。文件/图片维护复用Java能力但还需业务对照，不能以现有工具栏功能代表所有旧编辑业务已保留。导入/批量等继承操作继续按实际业务使用核对，不因没有本次页面按钮而移出台账。
