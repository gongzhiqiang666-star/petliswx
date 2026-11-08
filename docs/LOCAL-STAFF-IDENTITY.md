# 本机员工账号与 PHP 实验室身份

## 实现边界

新增 ADMIN 专用 `POST /api/admin/staff` 与 PC“用户与医院 → 新增员工”。仅创建 LAB_STAFF 或 COURIER；不创建 ADMIN，不绑定医院，不计入医生注册审核申请。实验人员必须关联启用实验室，配送人员不能携带实验室。复用原账号、资料和角色表，无结构变更。密码 BCrypt 存储，审计不包含密码、哈希或令牌。

建号与医生注册共享账号命名锁，在 READ_COMMITTED 事务中核对账号/手机号冲突；并发同名仅一次成功。现有停用入口撤销会话。此入口不等于完成原 PHP 全员迁移。

## 持久本机联调身份

`scripts/local/setup-php-lab.py` 通过既有管理员正常登录和员工 API 创建 `petlis_local_lab_1`，仅 LAB_STAFF、实验室1、无医院。随机密码仅保存在本机0600私有文件，不进入仓库或测试输出。PHP 原管理员4的实验岗位桥接使用该独立身份，管理员业务桥接保持独立。

配置器验证固定隔离数据库、原管理员、实验室、正常登录和唯一有效角色；临时待验收映射存在时拒绝覆盖。使用文件锁、原子写、私有备份及待完成凭据恢复；同名无凭据时拒绝重置。重复执行不重复建号或覆盖映射。`--validate-only` 不建号/改映射，但正常登录会更新登录记录。

## 2026-11-08 验证

- 后端411项通过、0失败、0跳过；PC94项通过；配置器9项通过。
- 真实HTTP同名四并发返回200/409/409/409，仅一账号；COURIER正常登录可访问物流，不能创建员工；ADMIN建号接口拒绝创建ADMIN；无医院/医生注册申请，审计无凭据；结束停用合成账号，旧会话失效。
- 专用账号488首次创建成功；再次执行不创建、不变更映射；校验模式通过。
- `test-real-flow-fix.py --php-lab --use-local-lab --php-results --php-retest --php-unassigned --php-report` 使用永久账号488执行订单23935，从物流到PHP条码签收、结果、驳回修订、退样复检、待分配复检、报告审核发布33808及新端PDF鉴权通过。永久映射字节保持不变。
- 私有证据：`~/.local/share/petlis/qa/staff-creation-http.json`、`local-lab-setup.json`、`local-lab-setup-replay.json`、`local-lab-validation.json`、`permanent-lab-full-chain.json`。不包含密码或令牌。

原生浏览器新增员工表单、真实工作人员逐人身份迁移、真机及外部服务仍待验收；不将此结果视为全项目或PHP全量复刻完成。没有DDL，没有启用调度，没有写Mac mini或生产资源。
