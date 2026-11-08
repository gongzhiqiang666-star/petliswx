# 品种日常维护验收及采样类型写页面补验

按用户最新要求，字典以正常维护为目标，筛选等界面细节后补，不要求一比一复刻。

## 原业务与实现

原`application/admin/model/LbInfovariety.php`、同名basedata控制器、add/edit表单、语言及JS表格：lin_lb_infovariety，IID(bigint unsigned)，Variety_CN/Variety_EN各50字，createtime/updatetime为Unix秒。没有IsActive，没有物种关联。新Java/PC原字段维护，不添加虚构的状态开关或物种过滤。IID通过服务端long支持；超出Java signed long及浏览器安全整数的历史编号尚需专门兼容，目前未验证，不能声称完整unsigned bigint覆盖。

与其他字典共用只读、写入、删除三层开关、事务审计、防重复请求及编辑快照。物理删除符合原模型无软删除；不新增流水回收站。

## 实证

- 后端全回归534项、0失败/错误/跳过，日志`/tmp/petlis-variety-full.log`。
- PC122项通过，日志`/tmp/petlis-variety-admin.log`；页面及编辑组件语法检查通过。
- 本机事务测试覆盖品种新增、幂等、编辑、查询、旧ID回填、删除，不存在的状态输入拒绝。
- 脚本`scripts/local/test-legacy-dictionary-maintenance-ui.py`：实际新PC常规登录后，分别对采样类型、品种各新建一条临时记录，编辑、确认后删除；API确认删除后404。原业务记录未动。
- 临时采样类型126、品种442均已删除；创建/编辑/删除审计保留，记录为独立随机QA_MAINT_前缀。
- 采样类型新建默认禁用；品种新增/编辑/批量确认均没有状态控件或状态列。
- 截图`~/.local/share/petlis/qa/dictionary-maintenance-variety.png`已查看；私有证据`dictionary-maintenance-ui.json`与完成检查点`dictionary-maintenance-private.json`。完成检查点禁止直接重跑创建流程。
- 验收结束所有字典开关false、菜单隐藏。PHP源码/数据库结构不改。

## 后续

城市等必要基础资料仍待核实；业务表单接入选择器随送检迁移推进。字典导入/通用批量字段及筛选逐项复刻后置，不阻挡当前日常维护目标。范围内模块复核两轮仍需单独记录，此次功能验收不等于全32模块完成或两轮复核完成。
