# M06 医院资金迁移合同（启动）

原来源：`admin/model/basedata/Infohospital_money.php:33` beforeInsert；`admin/controller/basedata/InfohospitalMoney.php` index/add/del/edit；对应add表单和JS。

## 必保留

1. 医院资金池lin_lb_infohospital.amount与个人钱包lin_user.money独立，医院流水lin_infohospital_money与个人money_log独立。不能挪用、自动划转、跨池扣款。
2. 新增资金流水时同事务更新医院余额：金额0拒绝“金额不允许为空”；扣款医院不存在拒绝“医院不存在”；原bcadd余额与delta相加保留2位后，若扣款结果负数，报“医院{医院名}可用余额不足”。正向加款不限制旧负余额。
3. 原表单金额step0.01、备注必填；管理员归属admin_id来自旧管理员身份。新系统Java用户ID不能直接伪装为旧admin_id，需要明确定义可信身份映射，审计另记新用户ID。
4. 流水列表按医院，显示ID、医院、金额、管理员ID、备注、创建时间。原edit/del方法返回false，不提供流水修改/删除；更正通过新流水处理。
5. 原控制器事务覆盖回调余额更新与流水保存；新系统需加医院行锁与请求防重，避免并发超扣/网络重试重复入账。差异明确记录。

## 当前落地

`LegacyHospitalFundsRules`迁入余额计算与零额/不足提示；只做纯算法，无数据库/外发副作用，尚无可用资金维护接口。单元用例先失败后实现。不得把纯算法测试算成资金池已交付。

bcadd参考官方文档：https://www.php.net/manual/en/function.bcadd.php 。Java精确decimal相加后向0截断两位，不用浮点。小于分的输入可能导致原流水decimal入库舍入与余额截断不一致；需按原表单2位规则及隐藏调用专项验证，不能无声改变原算法。

## 尚待证据/实施

- 现有本机PHP7.4/默认PHP均无bcmath扩展，尝试纯bcadd参考执行失败（undefined function），不修改冻结PHP环境。因此当前没有原运行时差分证据，原生产bcadd行为需独立参考运行或其他只读证据补齐。
- 空医院余额、正向加款到不存在医院：原实现依赖PHP运行错误/空值转换，尚未运行核实；当前纯规则对null余额显式409，属于待解释差异，不能声明完全保真。
- 新Java服务、医院行锁、可信旧管理员映射、双池权限、参数控制、流水只读、原子性/并发/幂等、实际PC入口与小程序双池展示/消费归属待实现。
- 原医院/个人资金对送检支付、退款的调用路径继续追踪，不仅迁移加款表单。无DDL申请或执行。

## 最新补证

原PHP服务保持冻结，已在一次性隔离PHP7.4+BCMath运行原余额bcadd的7个参考，并与Java结果及扣款拒绝条件对照通过；见M09-PRICING-RULES-VALIDATION.md及bcmath-reference.json。历史“缺bcmath尚无原算子参考”的限制已补证；null余额、无医院正款及全模型/隐藏导入/支付退款接续仍待完成。
