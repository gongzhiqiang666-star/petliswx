# M25 本机报告原件获取

## 来源与落点

只读参考原 PHP `application/index/controller/Project/View.php::Detail`：按原付款条件判断后，以非空 PDFHttpUrl 放行报告。继续由 LegacyCustomerViewService 核对原 CID、整单现金费用及月结欠款；本次不增加 IsSendMail=1 条件，不修改旧业务记录。

Java LegacyCustomerReportService 接入 POST `/admin/legacy/customer-views/report`，PC 核对页提供“下载本机报告”。仍仅 ADMIN，可查询和可下载分别受 enabled、reportDownloadEnabled 参数控制；不决定尚待用户选择的客户登录策略。

## 文件读取规则

- 原件根目录由 APP_LEGACY_REPORTS_STORAGE_PATH 显式配置，默认空。旧 HTTP/HTTPS 引用仅提取 Content/FilePdf 下的本机相对路径，绝不访问远程地址。
- 拒绝路径穿越、越界符号链接、非 PDF、缺文件、超限和基本头尾不完整文件。缺文件明确报错，不生成替代报告。头尾检查不等于所有历史 PDF 的结构或排版验收。
- 若该明细存在新报告工作流，只能获取匹配医院、父单、明细且已 PUBLISHED 的关联报告文件；不回退旧路径绕过发布。
- 保留原文件字节及中文文件名，响应为 PDF 附件、private/no-store、nosniff。复用现有平台文件服务读取新流程报告。
- 不改正式报告生成器、模板或分页方案；旧付款标记与新工作流的全部交叉场景仍需全流程复核。

## 验证证据

本次七组 Java 专项合计 24 项，0 失败/错误/跳过；PC 162 项通过。新增覆盖独立下载开关、付款阻挡、已发布关联校验、草稿不能回退、文件完整读取及路径边界。

`python3 scripts/local/test-bank-payment-quote-live.py --customer-reports` 已通过真实 Chrome → Java → 本机 13306：中文附件下载成功，字节 SHA 与原件一致；pdfinfo 确认为一页，pdftotext 核对合成正文；未付款被拒，匿名 401，页面零错误、零外部请求。临床/资金/报告及无关支付记录快照不变；夹具删除、开关恢复、专用后端停止。

这是自有有效 PDF 夹具的页面读取验证，不能冒充真实历史报告或完整生成发布流程验收。

## 历史原件缺口

只读盘点发现数据库有 27,374 条非空报告引用；参考源码 Content/FilePdf 目录只有 15 个 PDF，已检查的源码压缩包内该目录有 9 个 PDF。已识别的原路径引用与这两处文件均未匹配。另有 54 条引用不属于此次原路径映射，须另核对类型；以上为引用行数，不是独立缺失文件数。

已询问用户原报告备份位置，未收到位置前不连接未知生产服务器、不自动请求旧网址。客户授权策略、真实旧密钥/入口、实际历史原件、付款接续及整模块两轮复核仍未完成，M25 保持部分完成。
