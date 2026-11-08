# M11 医院送检历史：查询及小程序展示

## PHP 源码规则

只读来源：`application/index/controller/User.php::Pros`（423行起）、`application/index/model/LbListdiagnosisproject.php::Detail`、`application/index/view/user/pros.html`，以及 `application/index/lang/zh-cn/user.php::SampleStatus_list`。

- 医院来自已关联身份；按 InfoHospitalId 查询，不按录入医生或 CreateSampleId 限制，所以同院其他医生和原系统订单也保留。
- 只排除 SampleStatus=-2 草稿。按主单 ID 倒序分页，不按创建时间重排。
- 样本编号 trim 后 LIKE 包含查询，保留原通配语义；PHP 空值“0”不加入编号条件。状态非负才筛选；时间区间 BETWEEN 包含两个端点。
- 展示提交时间、编号、送检医生、主人、动物、原状态；审核未通过时保留 Remark 原文。
- Detail 关系按原 INNER JOIN 检测项目取明细，不排除豁免或停用项目。父状态为2/3时，IsSendMail=0显示等待报告，其他值显示已出报告；没有以新报告工作流状态替换这个历史展示值。

## Java 与小程序落点

`LegacyHospitalHistoryService`、`LegacyHospitalHistoryController` 提供 GET `/orders/hospital-history` 与 `/capabilities`。使用现有 Java 会话/医院数据范围，独立 `legacy-hospital-history` 配置（GLOBAL/0、ACTIVE、enabled严格布尔值）；默认未开启，接口与入口同受控。

医生、医院管理员及平台管理员具备相应范围才可查，医院编号必须明确且被授权；实验人员不因此取得客户数据权限。安全配置只为该静态历史路径放行相应角色，其他订单接口权限不扩大。

小程序“订单”页在参数开启时显示“医院送检历史 · 含原系统记录”，跳转原生 `pages/hospital-history/hospital-history`。页面支持编号、状态、提交日期、重置、分页；保留医生/主人/动物、驳回原文及逐项报告状态。日期选择明确转换为首日00:00:00至末日23:59:59，Java仍支持精确日期时间区间。

这是医院历史查询功能，非旧登录机制搬运。原草稿编辑、报告链接仍分别核对（旧单取消已接，见 M11-HOSPITAL-CANCELLATION-CONTRACT.md），不能据页面能查而称整个送检业务完成。不输出旧 PDF URL，不打开生产站点，也未提前决定 M25 客户链接的匿名/登录策略。

## 验证

6项本机数据库/HTTP专项通过，0失败/错误/跳过：

1. 同医院范围、仅排除草稿、按ID而非创建时间排序、分页和查询前后源记录不变。
2. 编号/状态及闰月末秒的精确区间，医生和驳回原文保留，反向时间拒绝。
3. 豁免及停用检测项仍展示，报告状态沿用原字段，不输出文件地址。
4. 参数必须严格布尔值、关闭阻挡、无医院业务角色拒绝。
5. 真实 Java SessionService 签发合成医院用户JWT，经实际认证过滤器→控制器→数据库查询通过；匿名401、其他医院403、关闭409。
6. 合成医院管理员真实会话通过专用权限路径。旧的 /orders/** 仅USER限制问题已由这个测试先复现，再限定修复。

所有测试夹具为合成数据，通过本机13306事务回滚，不修改真实历史订单。另相关报告7项回归通过。

小程序106项测试通过；新增4项覆盖本院参数/完整日期/内容、关闭不请求/失败保留筛选、异步旧结果不能覆盖新结果、翻页失败保留当前记录及日期缺失拒绝。`python3 scripts/local/check-miniprogram.py` 通过：54个WXML、58个WXSS由微信编译器编译，90个JS语法解析，五个Tab及路由白名单核对通过。上述 HTTP 是真实认证与数据库的 MockMvc 请求链，**不是微信模拟器或真机页面验收**。

## 剩余项

原 SendPro1/2/3、项目添加删除及保存模型回调尚未完成与当前新送检服务的逐项映射；不能把已有新系统送检直接标为PHP迁移完成。报告访问接续及历史原件仍受M25缺口约束。小程序模拟器/真机与实际历史订单展示、全部权限历史边界和整模块两轮复核仍未完成。未执行DDL、真实外发或定时任务。
