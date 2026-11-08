# M17 HTML报告PDF预览：已接通基础链路，分页差异待决定

## 原PHP源码依据

- `application/admin/controller/Base.php:182 showpdf`接收正文/文件名，指定明细时先调用GenterPdf获得路径，再调用真正的pdf函数；未指定文件名时使用时间文件名。GenterPdf本身不能证明文件已经生成。
- `application/common.php:700 pdf`处理旧域名、两类编辑器分页标记、特定Word样式及空o:p，再包裹HTML/CSS，通过wkhtmltopdf生成A4文件。左右18mm；新引擎上下暂取10mm，原命令未明确指定，仍需旧报告对照；原实现入队并sleep后返回，不代表任务成功。
- 当前Java `ReportPdfGenerator`是结果项目的简化文本排版，没有旧HTML表格图片能力。本批新增独立HTML渲染，不替换该服务、不冒充两者已经等价。

## 实现与参数

`LegacyHtmlPdfRenderer`通过固定配置的Node脚本与浏览器执行HTML→PDF，使用ProcessBuilder参数数组，不拼shell命令。脚本`legacy-html-pdf.js`复用已安装的playwright-core，保留原预处理和CSS；输入经stdin传入，不写模板源文件。按[原工具官方选项说明](https://wkhtmltopdf.org/usage/wkhtmltopdf.txt)的默认screen媒体模式渲染，未擅自改成print样式模式。结果在PDF_STORAGE_PATH内的专用临时目录，返回字节后删除本次临时文件。并发上限2、超时和容量超限明确拒绝，不截断输出。

独立配置`legacy-html-pdf`的enabled必须为JSON布尔true，并要求Java ADMIN；默认关闭。接口`/admin/legacy/html-pdf/capabilities`及POST `/preview`。模板编辑页按该参数显示“下载当前正文PDF预览”，对当前未保存的HTML进行认证POST下载，不保存模板、不写明细PDF路径、不审核、不发布或发送。客户端下载接口新增可选POST及JSON正文支持，旧GET兼容。

运行时须配置`app.reports.html-pdf-script`、`app.reports.html-pdf-browser`，可配置`app.reports.html-pdf-node`。脚本及浏览器路径不来自请求；本机专项使用已安装Chrome和项目自己的scripts/editor-parity依赖。部署所需Node/浏览器/字体准备仍需单独验证，不启动系统共享容器或其他项目环境。

浏览器JavaScript关闭，服务工作线程关闭，所有非data资源请求拦截；不自动读取本地file或访问生产图片。缺失/被拦截素材明确422停止生成；script/iframe/canvas等不支持内容也停止，不忽略内容后声称完整。图片迁入自有文件服务及历史素材映射尚未完成。

## 实测与未决分页差异

合成HTML覆盖中文/英文、表格、12.50 μg/L、内嵌SVG图及显式旧分页。新引擎直接沿用PHP CSS中的`tr { page-break-before:always; page-break-after:always; page-break-inside:avoid; }`会生成5页，表格被拆开。不能把内容存在/文本提取成功当作排版正确。

只在离线对照样例尾部覆盖tr前后分页为auto，保留inside:avoid和显式手动分页，得到2页且第一页表格及图片完整。该覆盖没有写入渲染器默认代码、原PHP或数据库模板。

依据主计划“差异较大、影响使用习惯或报告效果时，提供对比、影响和建议方案，由用户定夺”，已向用户提交选择，等待回答。两个PDF均为新引擎的合成对照，不是原wkhtmltopdf生成的历史基准；本机未找到wkhtmltopdf，未声称已证明与其完全一致。

证据仅存本机私有输出：`~/.local/share/petlis/reports/qa-html-pdf/acceptance.pdf`（5页）、`compatible-table.pdf`（2页），及各第一页渲染PNG。尚未批准分页适配，因此M17不标完成。

## 验证范围

- Node3项：原预处理、真实PDF文本与A4/分页内容检查、缺图/不支持内容拒绝。自建本机HTTP服务请求数0，证明缺图检查未访问该地址。PDF已用Poppler渲染并人工视觉检查，因发现上述分表问题而登记为未解决。
- Java3项：未配置/空正文拒绝，真实子进程生成及缺图422/临时文件清理，严格布尔参数与管理员权限。字体的PDF提取把“文”返回为兼容部首“⽂”，文本比较使用NFKC处理该提取差异；不改PDF正文或金额。跨平台字体及文本复制仍需完整历史报告验证。
- PC139项通过，含认证POST下载测试。
- `python3 scripts/local/test-html-pdf-preview-live.py`：真实PC→Java18082→渲染子进程→浏览器下载，当前未保存正文的中文/数字/END内容及PDF签名核对通过，模板写请求0，页面错误0；两个临时模块参数恢复，未创建业务模板或改报告状态。
- 调度/自动迁移关闭，专用服务停止，无数据库结构变更、无PHP修改或真实外发。

## 下一步

先等用户决定表格分页适配，然后检查长表、手动分页、页眉页脚、复杂历史正文与素材；接通实际明细生成、文件命名/路径、鉴权预览下载、失败原子恢复及审核发布流程。以上未完成前，基础预览成功不能扩写成正式报告PDF迁移完成。
