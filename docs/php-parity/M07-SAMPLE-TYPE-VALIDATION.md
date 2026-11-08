# M07 采样类型及字典缓存增量

## 原PHP对应

`application/admin/controller/basedata/LbInfosampletype.php`继承基础CRUD、multi与选择器；`model/LbInfosampletype.php`为int时间戳、无软删除。原add/edit表单要求SampleName、SampleName_en，IsActive默认0；原lang及JS显示采样类型、采样类型(英文)、添加时间、更新时间。旧表lin_lb_infosampletype，InfoSampleTypeId，名称varchar(250)，无DDL。

## 新系统本次变化

- 基础数据页面新增采样类型标签，与物种/性别使用同一受控CRUD、批量状态、物理删除、审计与防重实现。
- 保留原字段、标签、默认状态和时间戳；列表、排序、时间范围过滤及原始编号不改。
- 平面选择器同样支持采样类型；普通分页与旧编号回填由Java直接访问旧表，非PHP业务桥接。
- 新字典维护成功提交后清除本应用dictionary缓存，避免小程序已有缓存一小时不更新；数据库回滚不清缓存。此为新旧实现接续所需技术处理，不改变采样类型业务计算。

## 证据

- `/tmp/petlis-dictionary-selector-sample-full.log`：后端532项全部通过，无失败/跳过。
- `/tmp/petlis-sampletype-admin.log`：PC121项全部通过。
- 本机隔离库随机事务fixture：采样类型新增默认禁用、幂等、查询、回填、编辑、启用、物理删除全程通过且回滚。
- 真提交并发测试新增缓存断言：提交后旧缓存清除，模拟审计失败时数据回滚且缓存保留；7项通过。先确认旧实现缓存断言失败再修复。
- 真实HTTP和PC只读验收：物种/性别/采样类型标签，采样类型原列及添加时间，计数与原表一致；选择器HTTP分页/回填；医生直接访问403，读开关不暴露写按钮。
- 浏览器截图已查看：`/Users/apple/.local/share/petlis/qa/legacy-dictionary-sample.png`。
- 私有结果：`/Users/apple/.local/share/petlis/qa/legacy-dictionary-read-ui.json`。
- 验收结束读/写/删开关均关闭，页面菜单隐藏；全局事件调度保持关闭。

## 未完成

本次采样类型写操作以真实数据库事务测试验证，尚未单独执行采样类型的浏览器新增/编辑/删除流程，不以复用组件代替该项证据。导入、通用multi其他实际字段、完整原权限/筛选语义、其他字典及送检表单集成待迁移；M07仍为partial，两轮全模块复核仍未完成。
