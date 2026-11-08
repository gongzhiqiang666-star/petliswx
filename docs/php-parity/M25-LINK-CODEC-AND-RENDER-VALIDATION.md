# M25/M16 链接编码与渲染HTTP首批

## 已迁入

LegacyLinkCodec按原DDMMD5Encrypt、encrypt_url/decrypt_url及URL字符替换实现：派生密钥/IV、DES-CBC原填充、Base64及+、=替换；旧解码还含ASCII小写/去标点/原UCS-2行为。实际来源中的调用均用于明细/医院数字ID；13例兼容参考包括数字边界、前导0及若干旧文本解码特性，不宣称任意Unicode/畸形Base64输入已穷尽。该编码不作为新系统登录凭证。

原生产短语没有复制进仓库。capture-legacy-link-reference.py仅把两个原函数的硬编码密钥依赖替换为公开测试短语，其他算法保持原样；PHP7.4输出与Java逐例匹配。link-reference.json只含公开测试短语及测试向量，productionKeyIncluded=false。

LegacyReportLinks从私有环境变量APP_LEGACY_PUBLIC_BASE_URL、APP_LEGACY_LINK_KEY_PHRASE取值，默认均空；缺参数受控409，不默认生产站点、不填假链接。只构造字符串，不访问URL。按原拼接保留尾斜杠由配置决定，CID路径与加密参数一致；不接受嵌入凭据、非HTTP地址或带查询/片段的基址。

LegacyOrderDetailService.renderReport/renderMail把原正文、原绑定及服务端构造链接组合为报告/邮件HTML，新增两个受控只读HTTP接口。原Base.loadpdf此处实际是报告模板正文加载，并非文件生成；本轮未把正文渲染当PDF生成。

## 验证

- 后端全量589项通过，0失败/错误/跳过，/tmp/petlis-links-render-full.log。
- 链接PHP参考13例；配置缺失/原路径/ID0及边界、无效地址/嵌入凭据拒绝，数据库渲染不改原模板/已存正文测试通过。
- 真实HTTP脚本test-legacy-render-http.py：只在一个临时后端进程注入公开测试短语与本机基址，普通密码登录后用全合成主单24563、报告模板807、邮件模板19和独立绑定生成两种正文。返回CID与原PHP公开测试向量一致，绑定替换正确、邮件包含附件链接、普通医生403；无明细创建、原模板不变。
- 首次脚本遇mysql输出去掉末尾空列而拆列失败；finally无业务遗留，检查后归档失败检查点，补空列处理再以新随机前缀重试。没有把脚本失败当功能通过或第二轮复核。
- 临时主单/模板/绑定已精确删除，参数读/写false；公开测试环境仅进程级注入，结束已重启回正常后端，没有写入私有.env。未打开任何生成链接，未访问生产目标，未发邮件/支付。
- 私有证据~/.local/share/petlis/qa/render-http.json及已完成检查点render-http-private.json；禁止直接重放。合成数据没有复制真实患者信息。

## 未完成与差异

生产短语/基址的实际配置与验证、链接所指向的公开报告/付款查看页面、HospID各调用方、付款状态/加解密入口授权、旧链接历史接续仍待迁移。当前只提供CID构造器；编码器支持通用数字但不等于HospID业务已交付。

真实渲染HTTP用的是公开测试密钥，不能宣称已验证生产密钥/线上旧链接。默认运行未配置私有链接参数时，开启明细模块后渲染仍会明确409；需要接私有本机配置和最终目标页面后才可完整验收。

模板不存在等失败文案与原接口还需逐项对齐；新接口使用新HTTP/JSON协议并增加跨单校验，不能把正常内容匹配等同于全部异常路径兼容。原GET默认模板/正文预填、标题追加项目名、Tiptap/PC明细表单、邮件与PDF全过程、全32模块及两轮复核均未完成。原PHP、镜像和数据库结构不改。
