# M06 医院资金写页面与小程序双池首批验证

## 本次完成的实际操作

`scripts/local/test-legacy-hospital-funds-write-ui.py`通过正常PC入口，对独立随机前缀临时医院3838执行：初始10元，加款12.50→22.50，扣款5→17.50，再扣100被拒绝，余额仍17.50；流水2条，管理员个人余额不变。脚本结束关闭参数、清理该临时医院及其流水；保留新审计和私有证据。未变更任何原有医院余额。检查点`qa/hospital-funds-write-private.json`已completed，禁止直接重放。

截图`~/.local/share/petlis/qa/hospital-funds-write.png`及私有结果`hospital-funds-write-ui.json`。本次实际写页面证据补齐上一轮只读验收的缺项，不代表第二轮全模块复核已完成。

## 小程序双池读取

新增Java `HospitalWalletService/Controller`：GET `/api/account/hospital-wallets`仅返回AccessService.managedHospitalIds确认的医院管理范围；同范围的`/{id}/transactions`读旧医院流水，支持分页、收入/支出分类。全局enabled必须为JSON布尔true。无医院管理范围时列表为空，不能按医生hospital_id自行取得余额。

小程序账户页分别展示个人钱包、授权医院资金池（各医院独立卡片）；医院列表失败不影响个人钱包，失败时清除旧医院余额，支持重试。医院资金卡片跳转独立医院流水接口，不复用个人钱包流水。

原医院流水没有before/after余额：医院流水页面只展示原金额、备注、时间，不造“变动后余额”。医院流水未接导出时隐藏该入口并阻止调用个人导出，避免把个人资金数据当医院资金导出。没有修改个人充值或积分业务。

## 验证

- 后端全量551、0失败/错误/跳过，`/tmp/petlis-hospital-wallet-full.log`。
- 新医院钱包隔离库4项：只返回管理范围、不泄漏医院登录字段、越权403、字符串true不能开启、独立分类流水及不伪造balanceAfter。
- 实际普通医生HTTP：医院资金列表空，对未管理医院流水403；PC医院读取/读写参数权限再次通过，`/tmp/petlis-hwallet-read-ui.log`。
- 小程序102项通过，`/tmp/petlis-hwallet-mini-final.log`；新增4项覆盖分池、医院读取失败清除旧值但保留个人账户、入口授权、独立接口及不误导出个人流水、非法编号/忙碌分类切换。
- 静态JS/JSON检查通过；微信原生编译器编译53 WXML、57 WXSS，解析88 JS，5个tab/图标验证，`/tmp/petlis-hwallet-native-compile.log`。
- PC上一轮125项通过，本次仅新增验收脚本，无PC产品代码改动。

## 当前限制与下一步

- 医院管理用户的真实小程序模拟器/真机界面和完整授权账号联调未完成；当前为源编译、页面逻辑测试、数据库与普通医生真实HTTP证据，不能当作已验收真机双池。
- 两池用于送检支付的选择、扣款、退款归属仍待接续；未实现自动划转，不自动合并余额。
- 原PHP资金算法运行时差分、隐藏导入、极端精度/空余额边界见此前合同，仍待补证或说明；M06整体与两轮复核均未完成。
- 无DDL、PHP改动、生产/外发/作业；参数验收结束恢复关闭。
