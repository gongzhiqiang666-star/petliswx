# M07 物种/性别选择器：后端首批迁移

当前为局部实现，未完成整个M07、页面验收或两轮全模块复核。

## 原依据

- `application/common/controller/Backend.php:467–600`：selectpage。
- 同文件98：selectpageFields默认`*`，实际输出主键、显示字段、pid，而非整条记录。
- `application/admin/view/project/lb_listdiagnosisproject/edit.html:75–83,129–136`：原送检单按InfoAnimalId/DictSexId升序，分别展示AnimalName/SexName。
- `application/common.php:486–507`：decodeUnicode，除了Unicode转换还含旧URL及字体内容替换。

## 已落地

Java接口：GET `/api/admin/legacy/dictionaries/{species|sex}/select`。

- 受同一管理员身份及基础字典只读开关约束，不需要开写入/删除开关。
- words多词之间OR，searchFields内按andOr组合；去重并保留PHP过滤空值和字符串0的规则。
- page从1开始，返回list和total；普通查询按原送检单主键升序。
- selectedIds非null代表回填：跳过搜索词、状态过滤与普通分页，去重后按首次选择顺序输出；空集合返回空，不误返回全库。
- 停用项可以回填，不强制过滤IsActive=1；active过滤仅用于普通搜索。
- 返回原主键、原显示字段与pid=0，不携带编辑快照、英文列等额外字段。
- 表/字段白名单、值参数绑定；没有DDL、PHP修改或真实业务记录改动。

## 验证

先编写单元契约及调用测试，确认缺实现失败，再实现。新增代理字符例先出现断言失败，再根据原PHP实际运行结果修正。

定向回归36项：选择器6、字典服务17、本机隔离库13，全部通过，无跳过。日志 `/tmp/petlis-selector-db-final.log`。隔离库用例在事务中插入独立随机前缀数据并回滚，覆盖分页/总数、禁用项回填、顺序/去重、AND字段与OR词组合、参数注入文本、Unicode显示；未开启运行服务全局参数。

原PHP纯算法实测（既有本机容器php -r，无业务请求）：`\\u732b`→猫，`\\u41`→A；分开的代理码输出通过JSON_INVALID_UTF8_SUBSTITUTE观察为两个U+FFFD，Java保留这一API可表示结果，不擅自合成emoji。

## 尚需迁移/复核，不得标完成

- 接入实际业务表单和浏览器交互验证；当前只有Java接口，未声称已上页面。
- 原generic selectpage支持自定义showField、orderBy、多种custom比较/树形；当前只覆盖已核实的平面物种/性别中文名、主键升序、active条件。其他实际调用逐一盘点迁移，不能当作已全量迁移。
- 普通页size上限100属于新接口技术限制，原PHP无此上限，需在完整调用盘点时评估兼容。
- 常规trim/HTML标签/实体过滤已实现；畸形标签、PHP strip_tags与Java规则边界、非默认mb配置、原响应对非法UTF8的处理须专项差异验证，不宣称完全等价。
- 多查询一致性、非管理员原权限映射、其他字典和导入仍待迁移；全套回归和真实HTTP/页面验收待下一步。
