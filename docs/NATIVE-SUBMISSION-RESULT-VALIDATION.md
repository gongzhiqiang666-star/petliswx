# 微信原生送检成功结果与流程验证

2026-11-08。本轮直接连接既有9420自动化端口，核对AppID匹配本项目且当前为simulator-fixture中的合成账号；没有重新打开微信工具，没有操作其他项目。

## 用户流程改进

原提交成功依赖原生模态弹窗，只有查看订单一个动作且跳到通用列表。按已批准UX方案改为确认页内的成功卡片：专业brand-check图标、持久申请号、暖米色下一步说明、“查看本次送检”及“返回首页”。成功后隐藏原表单/提交栏，立即清本地草稿及缓存；清理存储异常也不把已成功提交误报为失败。当前页已成功状态阻止再调用提交。查看按钮reLaunch至该单详情，避免去列表重新找单；返回首页用原生Tab。提交前按钮明确为“提交审核”，支付仍等服务端审核通过。

此处不宣称所有回退/退出组合或弱网断电防重均已验收；已有服务端幂等/草稿消费机制继续使用，未改变价格或支付规则。

## 原生模拟器实测

- Home读取合成医生身份与真实待办，截图native-home-current.png。
- 先选项目→采样→返回项目再进入采样，输入保留；保存服务端草稿→草稿列表→恢复，采样部位与医生、project-first流转保持。
- 首次验收脚本误把空价格当0元；确认页正确返回“签约缺少检测项目协议价”，未绕过配置。修正脚本排除空价并可指定项目ID，以本账号既有合同1/实验室1/项目1协议价420元继续，未修改价格或合同。
- 原弹窗版本实际提交23941/草稿38，待审核、草稿消费通过；Native.confirmModal在当前锁屏条件下没有关闭弹窗，不能将其记为查看动作通过。
- 改为结果卡后，新原生提交23942及对应草稿消费通过；success card实际显示申请号23942与等待审核，旧提交按钮不存在，点击“查看本次送检”后路由order-detail且orderData.orderId=23942。无付款。
- 采样/确认页面切换有渲染动画，原截图曾抓到过渡帧；脚本新增等待确认按钮渲染及短动画完成后截图，不将过渡图当确认页。成功卡截图已实际查看，保留qa/native-submission-result.png。

复现：`PETLIS_QA_PROJECT_SEARCH='七折 1061' PETLIS_QA_PROJECT_ID=1 PETLIS_QA_SUBMIT=1 node scripts/local/test-project-first.js`。仅针对原有本机合成fixture，开启SUBMIT会创建新的本机合成申请。证据在私有qa/native-project-first-latest.log、ux-v1-order.json、native-submission-result.png。截图与日志不含登录凭据。

小程序53项通过、JS69/JSON53检查通过。新增单测验证成功保存申请号、清本地草稿、再次提交不走请求、精确导航，以及存储/缓存清理失败不影响成功状态。无DDL、未写Mac mini或生产、没有真实支付/寄件/调度。微信真机、物理样本/打印、完整F01–F42与顺丰回调仍待完成，不把本项视为总目标完成。

## 原生提交到PHP报告的同单接续（2026-11-08）

对原生提交23942继续正常业务：版本校验的临床修正入口补充合成临床资料→管理员审核（月结合同快照，无付款）→独立合成配送员创建本机运单69、揽收运输、登记并处理一条延误→明确送达。PHP以永久实验身份488签收、分配任务79、录入、生成报告、管理员审核发布；工作流98、报告33815。所有操作走已有API/桥接，没有直接SQL回写状态、价格或报告。临时配送员结束停用。

重新进入微信原生订单详情23942，实际验证sampleStatus=3、临床症状一致、物流RECEIVED、异常历史已处理且当前预警为空、报告预览按钮存在；打开原生报告页33815验证内容、付款限制false及合成结论。原生截图发现时间线PICKUP/TRANSIT/DELIVER/RECEIVE未翻译，修正仅内置操作码的中文展示，人工说明原样保留；复验内部操作码为0项。追加小程序单测，54项全部通过，静态JS69/JSON53通过。

点击原生报告页“查看PDF”，跳转pdf-view后busy=false、error为空，真实wx.downloadFile/wx.openDocument路径没有报错（没有mock平台方法）。独立HTTP已核对同医生下载为PDF字节。该结果仅证明模拟器接受打开操作；没有看到外部文档查看器的实际PDF渲染画面，也没有完成微信手机真机或实体打印，不能把无报错当视觉验收通过。

新增证据私有qa/native-full-chain.json、native-completed-view.json、native-completed-order.png、native-completed-logistics.png、native-report-detail.png、native-pdf-open-attempt.json。只读原生复验脚本test-native-completed-order.js；完成脚本complete-native-order.py仅允许当前模拟器合成账号新待审核单，终态不可直接重放；PHP probe限定petlis_qa_账号及匹配宠物名前缀。没有生产寄件、真实付款、DDL或调度。
