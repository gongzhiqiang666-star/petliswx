# Petlis 核心页 Pencil 视觉重对齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 首页、送检流程、报告详情、「我的」按 petlis_v2.pen 重做视觉与信息架构，全部接真实接口，图标与移动体验按规范统一。

**Architecture:** 先落地设计令牌（样式变量与共享类），再逐页重写 WXML/WXSS，页面 JS 只做视图映射与少量真实数据聚合，不改后端接口。每一步用现有静态检查、客户端逻辑检查与模拟器脚本验收。

**Tech Stack:** 原生微信小程序 WXML/WXSS/JS；设计基准 `/Users/apple/claudelife/petlis/petlis_v2.pen`；验收 `scripts/local/check-miniprogram.py`、`test-client-flows.js`、`run-simulator.py`、`run-tabs.py`、`run-full-flow.py`。

**设计令牌（从画布提取）:** 主色 `#0D6E6E`，文字 `#1A1A1A/#666666/#888888/#AAAAAA`，页面底 `#FAFAFA/#F8F8F8`，浅绿 `#EBF5F5`，强调橙 `#E07B54` + 底 `#FFF5F0`，成功绿 `#2E7D32` + 底 `#E8F5E9`，边框 `#E5E5E5`；卡片圆角 12、控件圆角 8（rpx 按 2 倍换算）；画布字号 11/12/13/14/16/18/20px → 22/24/26/28/32/36/40rpx。

---

### Task 1: 设计令牌、共享样式与图标规范

**Files:**
- Modify: `miniprogram/styles/native-theme.wxss`（如不存在则 Create）
- Modify: `miniprogram/app.wxss`
- Modify: `miniprogram/styles/common.wxss`
- Modify: `miniprogram/app.json`（tabBar selectedColor）
- Modify: `miniprogram/assets/icons/tab/*.png`（脚本重染）
- Modify: 全项目 WXSS 中 `#147D79→#0D6E6E`、`#116965→#0A5A5A`

- [ ] **Step 1: 写入统一令牌与共享类**

在 `styles/native-theme.wxss` 定义（页面通过 app.wxss `@import` 引入）：

```css
/* 设计令牌：petlis_v2.pen */
:root{
  --petlis-primary:#0D6E6E;--petlis-primary-dark:#0A5A5A;
  --petlis-text:#1A1A1A;--petlis-text-2:#666666;--petlis-text-3:#888888;--petlis-text-4:#AAAAAA;
  --petlis-bg:#FAFAFA;--petlis-bg-soft:#F8F8F8;--petlis-tint:#EBF5F5;
  --petlis-accent:#E07B54;--petlis-accent-bg:#FFF5F0;
  --petlis-success:#2E7D32;--petlis-success-bg:#E8F5E9;--petlis-border:#E5E5E5;
}
.petlis-card{background:#FFFFFF;border-radius:24rpx;padding:32rpx;box-sizing:border-box}
.petlis-section-title{font-size:32rpx;font-weight:600;color:var(--petlis-text);line-height:1.4}
.petlis-section-link{font-size:24rpx;color:var(--petlis-text-3)}
.petlis-icon-badge{width:88rpx;height:88rpx;border-radius:20rpx;background:var(--petlis-tint);display:flex;align-items:center;justify-content:center;flex-shrink:0}
.petlis-icon-badge image{width:44rpx;height:44rpx}
.petlis-chip{border-radius:16rpx;padding:16rpx 24rpx;display:flex;flex-direction:column;gap:8rpx}
.petlis-chip .chip-value{font-size:40rpx;font-weight:600;line-height:1.2}
.petlis-chip .chip-label{font-size:22rpx;color:var(--petlis-text)}
.petlis-chip-warn{background:var(--petlis-accent-bg)}.petlis-chip-warn .chip-value{color:var(--petlis-accent)}
.petlis-chip-primary{background:var(--petlis-tint)}.petlis-chip-primary .chip-value{color:var(--petlis-primary)}
.petlis-menu-item{min-height:96rpx;display:flex;align-items:center;gap:24rpx}
```

- [ ] **Step 2: 全局主色替换与 Tab 图标重染**

- 全部 WXSS：`#147D79→#0D6E6E`，`#116965→#0A5A5A`；app.json `selectedColor` 改为 `#0D6E6E`。
- 用既有 PIL 方式把 `assets/icons/tab/*-active.png` 重染为 `(13,110,110)`。
- 禁止新页面使用 emoji/文字箭头当图标；统一 `assets/icons/` 下 PNG，显示尺寸用 WXSS 固定。

- [ ] **Step 3: 静态与逻辑检查**

Run: `cd /Users/apple/claudelife/petliswx && python3 scripts/local/check-miniprogram.py && node scripts/local/test-client-flows.js`
Expected: 5 tab 校验、50 WXML、53 WXSS、62 JS、13 项逻辑检查全过。

### Task 2: 首页按画布重构

**Files:**
- Modify: `miniprogram/pages/home/home.wxml`
- Modify: `miniprogram/pages/home/home.wxss`
- Modify: `miniprogram/pages/home/home.js`
- Modify: `scripts/local/test-tabs.js`（首页截图断言不变，保留）

- [ ] **Step 1: 数据聚合（全部真实接口）**

在 home.js 保留现有 `loadProfile`/消息加载，新增：

```js
async loadWorkspace(){
  const statusOf=async status=>{const r=await api.orders.getList({status,page:0,size:1});return r.totalElements??r.total??0}
  const [pay,review,report,recent,account]=await Promise.all([
    statusOf(-3),statusOf(0),statusOf(2),api.orders.getList({page:0,size:3}),api.account.get()
  ])
  this.setData({
    todo:{pay,review,report},
    recentOrders:recent.content||[],
    balance:account.balance
  })
}
```

（以现有 `api/index.js` 实际方法名为准；若 orders 封装参数不同，按封装调整，禁止伪造计数。无数据模块不渲染。）

- [ ] **Step 2: 按画布写 WXML 结构**

顺序：`header`（医院名 40rpx 600 + 问候/医生 24rpx，右侧消息与帮助图标）→ `entryGrid` 白卡（两行四格：我要检测/档案中心/钱包/优惠券/送检记录/发票/报告中心/消息中心，浅绿 88rpx 图标格 + 22rpx 标签）→ `今日待办` 白卡（chips：待支付橙、待审核绿、待报告绿，点击跳对应过滤订单）→ `最近业务进度` 白卡（最近订单宠物名+状态+时间）→ `财务摘要` 白卡（余额 40rpx 主色 + 去充值按钮）→ `最新提醒` 白卡（真实最近两条消息）。
- 入口路由：我要检测 switchTab 检测；档案中心 navigateTo pets；钱包/优惠券 navigateTo account/coupons；送检记录 navigateTo orders；发票 navigateTo invoices；报告中心/消息中心 switchTab。
- 每卡间距 48rpx（画布 gap 24），卡片内边距 32rpx，圆角 24rpx。

- [ ] **Step 3: 模拟器验收**

Run: `python3 scripts/local/run-tabs.py`
Expected: FIVE TAB NAVIGATION PASSED；`qa/tabs-home.png` 与画布结构一致、无接口错误、无虚构指标。

### Task 3: 送检填写资料页视觉重构

**Files:**
- Modify: `miniprogram/pages/order-add-pet/order-add-pet.wxml`
- Modify: `miniprogram/pages/order-add-pet/order-add-pet.wxss`

- [ ] **Step 1: 三步步骤条 + 已选档案卡 + 分组表单卡**

- 步骤条：1 填写资料 / 2 选择项目 / 3 确认送检，当前步主色实心、其余浅绿，样式取画布 tsStep 的圆点+连线。
- 已选档案卡：宠物名、品种·年龄·性别、畜主，右侧「已选档案」浅绿标签。
- 宠物信息/采样信息/送检信息/其他 四张白卡，标签 26rpx 二级色、输入框 88rpx 高、浅灰底 `#F8F8F8`、边框 `#E5E5E5`、圆角 16rpx；必填星号用 `#E07B54`。
- 底部「保存草稿（浅绿）+ 选择检测项目（主色）」固定栏，按钮高 88rpx 以上，留安全区。
- 不改任何 JS 行为与校验。

- [ ] **Step 2: 验收**

Run: `python3 scripts/local/run-simulator.py`
Expected: 基线全过（含草稿保存恢复）；`qa/order-form.png`、`restored-order-draft.png` 视觉符合画布。

### Task 4: 检测项目与确认送检页视觉重构

**Files:**
- Modify: `miniprogram/pages/order-test-items/order-test-items.wxml`、`.wxss`
- Modify: `miniprogram/pages/order-preview/order-preview.wxml`、`.wxss`

- [ ] **Step 1: 项目选择**

实验室 picker 卡、搜索框、项目卡（名称 30rpx、价格主色、选中态浅绿底+主色描边+圆形勾选图标）、底部已选数量/合计/下一步固定栏；已选抽屉沿用并统一圆角 24rpx。

- [ ] **Step 2: 确认送检**

档案摘要卡、项目费用明细卡（逐项价格、优惠券、合计主色 40rpx）、订阅提示与提交按钮；提交后文案保持「待审核，审核通过后现结支付/月结挂账」。

- [ ] **Step 3: 验收**

Run: `python3 scripts/local/run-full-flow.py`
Expected: FULL FLOW PASSED；`qa/full-flow-04-test-items.png`、`05-order-preview.png` 视觉符合画布。

### Task 5: 报告详情视觉重构与图标清理

**Files:**
- Modify: `miniprogram/pages/report-view/report-view.wxml`、`.wxss`

- [ ] **Step 1: 重排并清除 emoji**

结构：宠物名+报告类型头部（未支付橙色「待支付解锁」标签）→ 未支付提示浅橙卡（说明+去支付主色按钮）→ 基础送检信息卡（申请单号/样本类型/实验室/检测项目/支付状态，右对齐值）→ 报告摘要预览卡（未支付显示锁定说明，已支付显示 PDF 卡与查看/下载按钮）。
- 删除 📋 等 emoji 与文字箭头，统一 icons；保留 `paymentRestricted` 锁定逻辑、字段视图映射与 1900 零日期过滤。

- [ ] **Step 2: 验收**

Run: `python3 scripts/local/run-full-flow.py`
Expected: 报告字段断言全过；`qa/full-flow-11-report-detail.png` 信息完整。

### Task 6: 「我的」按画布重构

**Files:**
- Modify: `miniprogram/pages/mine/mine.wxml`、`.wxss`

- [ ] **Step 1: 资料卡 + 菜单列表**

资料卡：头像、昵称、ID/手机号；菜单使用统一 `.petlis-menu-item`：宠物档案/送检记录/我的账户/积分商城/优惠券/开票管理/资讯与活动/通知偏好/个人资料/医院信息/退出登录；左侧浅绿图标格、右侧 chevron 图标（不使用 › 字符）；消息未读角标保留。
- 账户/商城等路由保持现状（account 聚合页不重复建设）。

- [ ] **Step 2: 验收**

Run: `python3 scripts/local/run-tabs.py`
Expected: 我的入口跳转全过，`qa/tabs-mine.png` 视觉符合画布。

### Task 7: 全量回归与真机预览

**Files:**
- Modify: `docs/IMPLEMENTATION-PLAN.md`（记录本轮）
- Output: `~/.local/share/petlis/qa/preview-qr.png`

- [ ] **Step 1: 全量串联验收**

Run: `python3 scripts/local/check-miniprogram.py && node scripts/local/test-client-flows.js && python3 scripts/local/run-simulator.py && python3 scripts/local/run-tabs.py && python3 scripts/local/run-full-flow.py`
Expected: 全部通过；查看 home/order-form/test-items/preview/report-view/mine 截图。

- [ ] **Step 2: 重新生成真机二维码**

Run: `/Applications/wechatwebdevtools.app/Contents/MacOS/cli preview --project /Users/apple/claudelife/petliswx/miniprogram --qr-format image --qr-output ~/.local/share/petlis/qa/preview-qr.png`
Expected: ✔ preview；打开图片供微信扫码（手机开调试模式）。

**纪律:** 不改后端接口与数据库；不提交 git（需用户另行批准）；不用虚构数据填充设计模块；保持现有业务行为与校验。
