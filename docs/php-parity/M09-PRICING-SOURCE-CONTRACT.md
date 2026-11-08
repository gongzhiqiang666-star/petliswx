# M09 原折扣与价格算法合同（参考及核价首批实现，明细接续待实施）

## 调用链

`admin/view/project/lb_diagnosisdetail/{add,edit}.html`选择OrginalPrice、OrginalDiscount，输入TestPrice允许人工修改；后台JS调用`admin/controller/Base.php::price`。价格不是只根据一个项目Price列统一计算。

Base.price：折扣属于“原价”、数值10或“其他折扣”时直接showPrice(price)；否则先bcdiv(discount,10,2)，再bcmul(所得系数,price,2)，最后showPrice。两次截断顺序不能合为乘除后一次舍入。

`common.php::OrginalDiscount`只把“原价”转10、“其它折扣”转-1，其余原样；而UI/Base使用“其他折扣”。原拼写差异必须保留为兼容差异，不能自作主张统一。

`DiscountToInt`对“原价”/“其它折扣”返回10，其他intval。`showPrice`为sprintf('%.2f',value)，有PHP二进制浮点格式化边界，不能拿任意BigDecimal HALF_UP替代并声称全等价。

原明细beforeInsert还含1225备注必填、缺价格时取首条价段、发送日期/默认标志、GenterNO生成及afterWrite正文副表；这些归M12/M18并一并追踪，不能只迁价格函数。

## 可复现原运行证据

脚本`scripts/local/capture-legacy-price-reference.py`只从冻结common.php抽取3个原样纯函数，通过既有本机php7.4 stdin执行，不加载框架、数据库或网络；223例写入`pricing-reference.json`，记录源文件哈希。

重要例子：字符串“0”返回字符串0；数值0因PHP7.4宽松比较会变为10。字符串“其他折扣”不被OrginalDiscount转为-1。showPrice('1.005')=1.00，showPrice('2.675')=2.67，showPrice('-0.005')=-0.01。

既有PHP缺bcmath保持原样，现已使用一次性独立容器补齐原Base.price的100个BCMath核价及7个医院bcadd参考；Java已迁核价与显示首批并逐例通过。详见M09-PRICING-RULES-VALIDATION.md。参考通过仍不代表核心明细全业务已接续。

## 下一步

在Java实现原价段选择/折扣输入及显示兼容，绑定原明细维护和PC实价字段；对每一计算步骤、手工实价、特殊项目、默认值及副作用进行差分验证。旧缺陷/特殊输入按原生产来源保留并单独说明，不无声改规则。M09及两轮复核未完成。
