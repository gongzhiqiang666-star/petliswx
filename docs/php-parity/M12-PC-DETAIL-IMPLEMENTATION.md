# PC 检测明细首批实际操作交付

本批按“PHP 源码规则 → Java 实现 → 结果验证”执行，迁移实际业务，不模仿旧选择器和工具栏。以下是局部交付，不代表阶段1、M12、全部迁移或两轮模块复核完成。

## 原 PHP 依据

参考根目录：`/Users/apple/claudelife/petlis-reference/macmini/laboklin_cn`，全程只读。

| 来源 | 核对内容 | 新系统落点 |
|---|---|---|
| `application/admin/controller/project/LbDiagnosisdetail.php` add/edit | 默认模板、正文读取、项目标题和编辑字段 | PC明细表单、Java form-defaults/detail/template接口 |
| `application/admin/view/project/lb_diagnosisdetail/add.html`、`edit.html` | 项目/价格/折扣/实价、模板、标题/正文、备注、发送日期、作废计费标志 | PC表单与Tiptap、原业务含义提示 |
| `public/assets/js/backend/project/lb_diagnosisdetail.js` | 项目价格和模板关联、核价、标题 | 项目/价格选择、显式核价和模板载入；不模仿selectpage |
| `application/admin/model/LbDiagnosisdetail.php:33` beforeInsert | 默认值、价格、1225备注、全局COUNT+1编号、日期 | LegacyOrderDetailService.create |
| 同文件 `:88` beforeUpdate | 折扣转换、日期处理、主单报告日期和完成状态、PDF路径 | LegacyOrderDetailService.update、LegacyDetailUpdateRules |
| 同文件 `:111` afterWrite | 邮件/报告正文副本 | 与明细、父单、审计同事务保存 |
| `application/common.php:833` GenterPdf | 文件名和PDFHttpUrl规则 | LegacyDetailUpdateRules.filename；此函数不生成PDF文件 |

特别保留原条件：修改时提交的发送日期非空就更新主单报告日期并置SampleStatus=3，并非只有IsSendMail=1才更新。界面明确说明该原有副作用。留空且未发送时保持父单状态，发送日期保存1970哨兵。

## 本批可操作内容

- PC明细列表按严格能力参数展示新增/编辑入口；服务端同时检查参数及ADMIN权限。
- 项目搜索和分页、价格档位（保留原停用价格）、报告/邮件模板选择与载入。
- 金额使用字符串传输，由Java原核价规则计算；手工实价不在保存时自动覆盖。
- 使用本地Tiptap现有兼容扩展编辑，支持基础文字格式、撤销重做、增加表格行列；未修改的正文按原HTML保存。
- 模板替换已修改正文需确认；请求期间正文发生修改则保留当前内容，不覆盖。
- 保存后重新读取，正文在无脚本、无外网的sandbox预览中显示，另可查看源文；外部图片地址保留但本机预览不加载。
- 修改接口`PUT /api/admin/legacy/order-details/orders/{orderId}/{id}`；detail增加editToken，更新需expectedToken，拒绝旧版本覆盖。
- `legacy-order-details`的enabled、writeEnabled控制读/新增，修改额外需要updateEnabled严格布尔值。默认关闭，不因本批代码自动启用。
- 同次保存失败重试沿用请求号和内容；结果不明时禁止换内容继续提交。后端审计幂等，正文/父单/路径/审计失败一起回滚。
- 关闭、替换或导航离开弹窗时销毁编辑器，避免旧实例和事件残留。

## 验证结果

- PC逻辑回归134项通过；编辑器单元验证50项通过。
- Java全套：600项中482项通过、118项环境条件跳过，0失败/错误；跳过不计为通过。
- 另开启本机数据库条件的明细专项：15项业务、3项原子性、4项纯规则，共22项通过。包括空日期不改变父单、非空日期联动、修改幂等、旧版本冲突、跨单拒绝、审计失败回滚。
- 独立Chrome合成API页面验证：核价、模板、编辑/撤销重做、格式、表格保留、新增/修改保存重开、关闭再开、维护开关，页面错误0。
- **真实PC → 新Java → 本机127.0.0.1:13306/laboklin**：合成单完成新增、九折核价180.00、模板编辑、保存重开，再修改实价175.25与正文、保存重开，接口及数据库内容一致；空发送日期保持父单状态，PDF路径回调正确。没有用SQL直接改业务状态冒充操作。
- 合成单据/项目/模板/价格已清除，维护参数原值恢复关闭，临时18082服务已停止。未修改历史业务单、未执行DDL、未真实外发。596份PHP基线文件校验无变化。

可重跑：`node scripts/editor-parity/admin-detail-smoke.js`（合成API），`python3 scripts/local/test-legacy-detail-live.py`（需专用18082本机Java服务及本项目私有身份配置，自动恢复开关/清理自身合成数据）。真实验证服务使用公开测试短语和本地链接地址，只供合成验收，不是原历史链接兼容验收。

编辑器先在`scripts/editor-parity`执行`npm ci`，PC的`npm run build:editor`本地打包；`npm start`通过prestart自动构建，构建产物不提交，不依赖CDN或付费服务。

## 仍须完成，不能标全量通过

完整编辑工具及图片/素材能力、创建时间维护、关联附件与发送入口、自定义折扣原SQL模式、真实历史链接和历史未完成单接续、外部图片资源、PDF文件生成/下载/分页、跨PHP及Java写入者编号并发，以及逐模块两轮完整复核。

本次原GenterPdf的进一步源码核对纠正了“修改时即生成PDF”的旧表述：本批只迁移其路径和命名副作用，实际PDF文件能力在M17单独交付，不能拿路径存在当文件可用。
