# 医院联系人维护首批实现

按“PHP源码规则→Java实现→结果验证”推进，原PHP未修改，未执行DDL。

来源：application/admin/controller/basedata/LbInfohospitalcontact.php医院范围查询；model/basedata/LbInfohospitalcontact.php创建时间与不更新修改时间；view/basedata/lb_infohospitalcontact/add.html与edit.html的财务专用、姓名、电话、邮箱、传真及备注。邮箱原表单必填，不自行加强为新邮件格式规则。姓名等字段保留原始内容，按原varchar(250)限制字符数。

Java：LegacyHospitalContactService及Controller提供医院范围列表、读取、新增、修改；能力参数legacy-hospital-contacts的enabled/writeEnabled均需严格布尔值，缺省关闭；ADMIN权限保护；修改携带editToken；请求号及审计处理幂等。PC医院列表提供按参数显示的联系人入口，维护上述字段。LoginName/LoginPwd是原表历史字段，普通维护不读出、不展示、不修改。

验证：3项规则测试及3项本机127.0.0.1:13306/laboklin事务测试通过，包含重复保存、修改重放、历史创建时间及登录字段保留、跨医院拒绝、旧版本冲突、关闭参数与非管理员拒绝。数据库专项使用合成医院/用户和事务回滚，不保留测试数据或开启参数；这些测试不代表真实页面或完整M05已通过。

尚缺：实际页面操作验收、审计失败整笔回滚验证、验收方式维护、报告/财务收件规则联调，以及原删除/导入/批量业务核对和两轮完整复核。以上保持未完成，不以维护API通过替代。

## 收件规则及原子性增量

进一步核对`application/admin/controller/base.php:234`与`finance/Hospmonthpeport.php:104–107、145–146`：报告发送按选中的联系人取邮箱并trim；财务月报按医院和联系人双重定位，候选列表的财务专用过滤原代码已注释，并非只能选finance=1。新增受控`GET /api/admin/legacy/hospital-contacts/{hospital}/{id}/recipient`承接选择后的地址读取，不进行外发、不返回历史登录字段，强制联系人属于当前医院。

本机专项现为9项：3项字段规则、4项事务内业务测试、2项经Spring事务代理的审计失败原子性测试，全部通过。新增/修改在审计失败时均整笔回滚；修改前后字段及版本令牌一致。合成数据按自身ID及随机名称确认后清理，未开启常驻参数、未改结构。

该读取入口尚未与完整报告/财务发送服务串联，不能将接口验证记为发送全流程完成。实际页面验收、验收方式维护、删除/批量/导入核对及模块两轮复核继续保留未完成。

## 浏览器页面增量

`node scripts/editor-parity/hospital-contacts-smoke.js`通过。独立Chrome加载实际admin应用和联系人表单，以合成API验证：新增、编辑回填/保存重开；503不明结果重试保持相同请求号和内容；换内容提交被拦截；联系人名称作为文本显示不执行HTML；维护参数关闭隐藏新增/编辑。页面错误0，无数据库写入或外网请求。

页面补齐HTTP200业务拒绝时允许纠正后重试；网络/5xx不明结果仍必须核对或同内容重试。页面API替身与前述真实MySQL专项分别留证，尚不等于页面→真实Java→数据库的完整验收，更不等于模块两轮复核完成。

## 验收方式基础维护增量

核对原LbInfocheckreceived模型、add/edit表单和本机lin_lb_infocheckreceived结构：确认方式CheckType单名称、IsActive、整数秒createtime/updatetime。复用现有LegacyDictionaryService接入checkReceived，不新增表，不编造英文名字段；PC字典表单按hasEnglishName能力隐藏英文名输入，保留原默认禁用值及现有维护/审计/防重/冲突处理。

本机字典专项新增单名称新增读取和整数时间测试通过；字典服务及选择器25项单测通过，PC134项回归通过。此增量仅证明基础维护和兼容测试，尚未核对所有医院/订单对验收方式的业务引用，也未完成该字典实际页面→真实后端联合验收。

验收方式补充验证：新增后修改名称/停用、创建时间不重写、正常查询过滤停用值、历史已选停用ID仍能回填均通过。单名称字典表单新增测试确认无英文名输入也能提交，现PC135项通过。修正了通用比较分支，单名称字典不再因不存在的英文值而错误判断内容已变化。

对冻结PHP application目录检索InfoCheckReceivedId/InfoCheckReceived/CheckReceived，目前只发现后台和医院端主单新增模型均初始化为0，未发现对应业务编辑回填入口。此为当前源码证据范围，不虚构医院关联或验收审批流程；后续仍核对视图/SQL及历史字段使用情况。

## 联系人删除增量

原PHP联系人控制器继承Backend删除，模型无软删除字段/删除回调；本批保留物理删除能力。Java新增医院范围DELETE接口，独立deleteEnabled严格开关依赖enabled/writeEnabled，旧版本拒绝，审计请求重放，删除失败与审计一起回滚。PC增加确认提示，取消不删除，结果不明保留原请求重试；返回名单不再可选已删除联系人。此功能尚未启用常驻参数，未操作历史联系人。

本机联系人专项11项通过（规则3、业务5、原子性3），包含删除开关、跨医院拒绝、重复删除重放、审计失败恢复原记录。页面删除确认和刷新另用合成API浏览器验证，实际发送/历史引用兼容仍待后续完整联调。

## 真实联系人页面联合验收

`python3 scripts/local/test-hospital-contacts-live.py`通过：独立Chrome真实admin页面→专用本机Java18082→本机13306/laboklin，合成医院下新增联系人、编辑邮箱后重开、读取收件地址、确认删除及刷新均成功，数据库确认记录已删除且新增/修改/删除三条审计正确。页面错误0。仅医院发现列表被限定为合成医院（避免操作真实医院），联系人API未替换。

凭据仅在内存读取本项目0600私有映射，经正常Java登录验证ADMIN；不依赖PHP运行，不打印或保存凭据。用后恢复参数并清除自身合成医院/联系人及审计，未改原PHP和任何表结构。实际邮件发送、批量/导入与历史接续等剩余能力仍未完成，不能标M05完整两轮通过。
