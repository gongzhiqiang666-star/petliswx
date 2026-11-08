# M09 原折扣核价与金额显示迁移首批

## 已实现

`LegacyPricingRules`迁入common.php的OrginalDiscount/DiscountToInt/showPrice以及Base.price正常业务输入计算。

- 保留“其他折扣”与“其它折扣”分支差异、字符串/数值0在PHP7.4中的转换区别，不强行统一。
- 核价：原价/10/其他折扣直接显示原金额；其他数值折扣先除10截取两位，再乘价格截取两位；负值/小数沿原计算顺序。
- 金额计算使用BigDecimal，显示兼容单独以BigInteger精确有理数模拟原PHP二进制转换和sprintf两位格式，不用Double计算金额。1.005→1.00、2.675→2.67、-0.005→-0.01、负零及大额显示均按原参考。
- 新接口POST `/api/admin/legacy/test-catalog/quote`，仅管理员且模块读开关开启；返回price字符串，不写项目、订单、流水或业务审计，不要求开启写参数。
- 非法/超长输入受控400（长度128，常规十进制语法）；原PHP无效输入可能产生warning/隐式转换，该类非正常输入差异仍单列，不宣称全语法等价。

## 原运行时差分证据

1. `capture-legacy-price-reference.py`从冻结common.php抽取3个原样纯函数，在既有PHP7.4执行，223例：10个类型/文字转换、213个金额显示（固定种子随机及边界），`pricing-reference.json`。
2. `capture-legacy-bcmath-reference.py`使用独立一次性容器petlis-php-pricing-reference，原项目工具镜像7.4，安装与其已有common相同版本的php7.4-bcmath=7.4.33-1+deb11u5。无应用挂载、凭据、数据库连接、宿主端口或默认服务入口；原PHP服务/镜像未修改。
3. 直接抽取原Base.price方法和原showPrice，通过仅提供request.post与success返回值的测试外壳调用。100种price/discount组合原样执行BCMath乘除及显示，另执行7个原bcadd余额参考，结果`bcmath-reference.json`，含源文件SHA256及PHP版本。
4. Java差分逐例比较上述330个参考记录，全通过。BCMath参考中的负余额按原模型扣款拒绝条件验证，不将纯bcadd执行伪称为完整原医院模型运行。
5. 首两次一次性容器依赖获取失败（旧源有效期/安全仓库新版本404）；容器均已自动移除，最终固定既有版本后成功。脚本对自身唯一标签容器提供超时清理，不操作其他项目。

## 检查与边界

专项`LegacyPricingRulesTest`5项（每项可含参考表）：223个函数参考、100核价/7余额参考、分步截断、原价/自定义文字区别、非法格式。数据库测试另证只读接口不新增业务审计、关闭参数409。

当前尚未把该核价接口接入送检明细新增/编辑、手工实价覆盖、价格条目选择及特殊项目备注；没有替换现有新订单优惠/协议价计算，也未触发历史订单重算。这些属于M10/M12后续接续，不能把核价函数通过当作完整送检定价业务完成。

医院null余额、正款不存在医院、完整模型/权限差分仍待核实；BCMath原依赖缺失问题已补证，不能继续把“无原乘除运行证据”列为现状。两轮全模块复核尚未完成。

## 本轮最终验证

- 后端完整569项通过、无失败/错误/跳过，`/tmp/petlis-legacy-pricing-full.log`。
- 真实HTTP核价5个边界/模式通过，非法格式400、普通医生403、参数关闭409；没有新增项目/订单/流水/业务审计，`/tmp/petlis-legacy-pricing-http.log`，脚本`test-legacy-pricing-api.py`。
- 核价参数最终恢复关闭；一次性容器不存在；596份原PHP基线哈希复核不变。当前PC129项为上一批产品代码验证结果，本轮未改PC产品源码，不将其重复计为新一轮验收。
