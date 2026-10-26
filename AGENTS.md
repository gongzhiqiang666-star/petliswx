# Petlis 项目约定

## 提交与 PR 规范

- 所有提交信息（commit message）和 PR 标题、正文一律使用中文。
- 提交类型前缀可使用 Conventional Commits 的英文缩写（feat、fix、refactor、docs、test、chore、build、ci、perf），主题和正文必须中文。
- PR 正文使用 `## 概述`、`## 验证`、`## 风险` 三个固定小节，内容简洁、事实清楚。
- 提交前运行可用的静态检查和客户端逻辑检查；通过后再提交。

## 其他项目约定

- 页面与组件视觉、间距、图标、表单、按钮、空态等统一遵循 `docs/DESIGN-SYSTEM.md`。
- 小程序仅使用 `assets/icons/brand-*` 图标，不使用 emoji 或文字箭头充当图标。
- 选择类交互优先当前页底部弹层，非必要不跳新页面。
- 不执行数据库结构变更（CREATE/ALTER/DROP）前必须先获得用户批准；本机只使用 MySQL 副本，禁止写 Mac mini 原库。
- 不使用真实支付；本地开发仅使用 mock 支付与合成测试数据。
