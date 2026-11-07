# -*- coding: utf-8 -*-
"""干净版两个月开发上线计划：单个文件、3 张表、每表一张清晰表格。"""
import os
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "docs", "纳博科临微信小程序 开发计划.xlsx")

BRAND = "0D6E6E"
ZEBRA = "F7F9F9"
LINE = "D8DEDE"

TITLE_FONT = Font(name="微软雅黑", bold=True, color=BRAND, size=15)
SUB_FONT = Font(name="微软雅黑", color="6B7272", size=10)
HEAD_FONT = Font(name="微软雅黑", bold=True, color="FFFFFF", size=10.5)
BODY_FONT = Font(name="微软雅黑", color="222222", size=10)
HEAD_FILL = PatternFill("solid", fgColor=BRAND)
ZEBRA_FILL = PatternFill("solid", fgColor=ZEBRA)
THIN = Side(style="thin", color=LINE)
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
WRAP = Alignment(wrap_text=True, vertical="center")
CENTER = Alignment(horizontal="center", vertical="center", wrap_text=True)


def head_block(ws, ncols, title, subtitle):
    """第1行标题、第2行副标题、第3行留白、第4行表头。返回表头行号。"""
    ws.merge_cells(start_row=1, start_column=1, end_row=1, end_column=ncols)
    c = ws.cell(row=1, column=1, value=title)
    c.font = TITLE_FONT
    c.alignment = Alignment(vertical="center")
    ws.row_dimensions[1].height = 34

    ws.merge_cells(start_row=2, start_column=1, end_row=2, end_column=ncols)
    s = ws.cell(row=2, column=1, value=subtitle)
    s.font = SUB_FONT
    s.alignment = Alignment(vertical="center", wrap_text=True)
    ws.row_dimensions[2].height = 20
    ws.row_dimensions[3].height = 6
    return 4


def table(ws, header_row, headers, rows, widths, center_cols=()):
    for j, h in enumerate(headers, start=1):
        cell = ws.cell(row=header_row, column=j, value=h)
        cell.font = HEAD_FONT
        cell.fill = HEAD_FILL
        cell.alignment = CENTER
        cell.border = BORDER
    ws.row_dimensions[header_row].height = 28
    for i, row in enumerate(rows, start=header_row + 1):
        for j, v in enumerate(row, start=1):
            cell = ws.cell(row=i, column=j, value=v)
            cell.font = BODY_FONT
            cell.border = BORDER
            cell.alignment = CENTER if j in center_cols else WRAP
            if (i - header_row) % 2 == 0:
                cell.fill = ZEBRA_FILL
    for j, w in enumerate(widths, start=1):
        ws.column_dimensions[get_column_letter(j)].width = w
    ws.sheet_view.showGridLines = False
    ws.freeze_panes = ws.cell(row=header_row + 1, column=2)


wb = Workbook()

# ================= 1. 开发计划 =================
ws = wb.active
ws.title = "开发计划"
hr = head_block(
    ws, 7,
    "纳博科临微信小程序 · 开发计划",
    "周期：两个月 / 8 周（2026-11-09 起）    团队：宫志强（小程序端）· 李龙（PC 端）· 老韩（运维部署）    原 PHP 系统代码：petlis-reference",
)
plan = [
    ("W1", "11/09–11/15", "启动与主链路开发",
     "首页、档案、宠物病历与送检表单", "登录权限框架、医院/医生资质审核", "环境、测试库、账号准备", "M1 启动评审"),
    ("W2", "11/16–11/22", "主链路打通 + 长尾功能开发",
     "送检三步、核价、记录/详情、报告、现结支付、钱包/券、消息、发票、积分、五个底部菜单",
     "订单审核、报告发布、物流、实验室端、月结、签约、看板、退款、打印", "—", "M2 功能开发完成"),
    ("W3", "11/23–11/29", "前后端 + 第三方联调（接口对账、微信/支付/物流/发票）",
     "小程序全链路联调、微信登录/订阅消息", "管理端接口联调、支付/物流/发票对接", "测试环境、合法域名", "M3 前端冻结 / 联调完成"),
    ("W4", "11/30–12/06", "原 PHP 后台（管理员端）联调 · 1 周（petlis-reference）",
     "旧订单/报告/通知在小程序正确展示、旧 ID 一致", "新旧数据兼容、旧管理员操作与通知桥接、报告/PDF 与鉴权、切换并存方案", "旧库隔离副本、备份、联调环境", "M4 PHP 联调完成"),
    ("W5", "12/07–12/13", "系统集成测试（按功能清单 F01–F42 逐项、越权/重复提交/金额精度）",
     "逐项走查、真机、缺陷修复", "管理端走查、越权/重复提交/金额精度、修复", "—", "M5 系统集成测试通过"),
    ("W6", "12/14–12/20", "用户验收测试 + 预发布 + 性能/安全",
     "真机验收、缺陷修复", "业务验收、缺陷修复", "预发布部署、压测、安全", "M6 用户验收测试通过"),
    ("W7", "12/21–12/27", "上线准备：迁移、配置、提审、监控、回滚",
     "提审前自测、版本提交", "生产配置、初始化数据、迁移配合", "生产部署、迁移、监控、回滚", "M7 部署就绪"),
    ("W8", "12/28–01/03", "上线与观察：灰度→全量、值守、对账",
     "值守修复", "值守修复、数据对账", "上线发布、监控告警", "M8 正式上线"),
]
table(
    ws, hr,
    ["周次", "日期", "主要工作", "宫志强 · 小程序端", "李龙 · PC 端", "老韩 · 运维部署", "里程碑"],
    plan,
    [7, 14, 30, 30, 30, 20, 15],
    center_cols=(1, 2, 7),
)

# ================= 2. 功能索引（F01–F42）=================
ws = wb.create_sheet("功能索引")
hr = head_block(
    ws, 3,
    "功能索引（F01–F42）",
    "来源：docs/FUNCTION-MATRIX.md（功能合并矩阵，对应需求 Excel 的 42 个功能主项）",
)
f_items = [
    ("F01", "用户模块", "注册"),
    ("F02", "用户模块", "登录"),
    ("F03", "用户中心", "我的"),
    ("F04", "用户中心", "我的积分"),
    ("F05", "用户中心", "积分商城"),
    ("F06", "用户中心", "钱包"),
    ("F07", "用户中心", "我的优惠券"),
    ("F08", "送检中心", "我要送检"),
    ("F09", "送检中心", "送检记录"),
    ("F10", "送检中心", "发票管理"),
    ("F11", "资讯中心", "资讯中心"),
    ("F12", "消息通知", "微信小程序消息通知"),
    ("F13", "通用基础", "全局公共配套"),
    ("F14", "配送/物流", "标本揽收"),
    ("F15", "配送/物流", "配送追踪"),
    ("F16", "配送/物流", "实验室签收确认"),
    ("F17", "配送/物流", "异常处理"),
    ("F18", "实验室端", "标本签收管理"),
    ("F19", "实验室端", "检测任务分配"),
    ("F20", "实验室端", "检测结果录入"),
    ("F21", "实验室端", "报告生成与审核"),
    ("F22", "实验室端", "报告模板管理"),
    ("F23", "实验室端", "退样/复检处理"),
    ("F24", "账单与结算", "现结流程"),
    ("F25", "账单与结算", "月结流程"),
    ("F26", "账单与结算", "账单生成"),
    ("F27", "账单与结算", "欠款管理"),
    ("F28", "账单与结算", "账期配置"),
    ("F29", "医院签约与定价", "医院入驻签约"),
    ("F30", "医院签约与定价", "结算方式配置"),
    ("F31", "医院签约与定价", "检测项目定价"),
    ("F32", "医院签约与定价", "协议折扣"),
    ("F33", "医院签约与定价", "月结信用额度"),
    ("F34", "后台管理端", "审核管理"),
    ("F35", "后台管理端", "用户管理"),
    ("F36", "后台管理端", "检测项目管理"),
    ("F37", "后台管理端", "数据统计看板"),
    ("F38", "后台管理端", "运营配置"),
    ("F39", "其他细节", "宠物档案"),
    ("F40", "其他细节", "打印功能"),
    ("F41", "其他细节", "退款流程"),
    ("F42", "其他细节", "角色权限"),
]
table(ws, hr, ["编号", "模块", "功能"], f_items, [8, 22, 34], center_cols=(1,))

# ================= 3. 上线检查清单 =================
ws = wb.create_sheet("上线检查清单")
hr = head_block(
    ws, 6,
    "上线检查清单",
    "上线前逐项确认，状态列可下拉选择：未完成 / 进行中 / 完成",
)
checks = [
    ("生产环境、MySQL 数据库、域名、SSL 就绪", "环境", "老韩", "未完成", ""),
    ("微信合法域名、隐私协议、类目提审通过", "微信", "老韩", "未完成", ""),
    ("支付商户正式配置，回调与验签验证", "支付", "老韩、李龙", "未完成", ""),
    ("数据库迁移审批 + 备份 + 灰度执行 + 旧表结构前后快照对比", "数据", "老韩、李龙", "未完成", ""),
    ("订阅消息模板配置，推送开关策略确认", "消息", "宫志强、李龙", "未完成", ""),
    ("监控、告警、错误上报、健康检查接入", "运维", "老韩", "未完成", ""),
    ("回滚预案与数据备份演练", "运维", "老韩", "未完成", ""),
    ("初始化数据（医院、实验室、项目、价格）配置", "数据", "李龙", "未完成", ""),
    ("上线沟通群、值班表、应急联系人", "协作", "老韩", "未完成", ""),
]
rows = [(i,) + c for i, c in enumerate(checks, start=1)]
table(
    ws, hr,
    ["序号", "检查项", "类别", "责任人", "状态", "备注"],
    rows,
    [6, 52, 8, 16, 10, 22],
    center_cols=(1, 3, 5),
)
dv = DataValidation(type="list", formula1='"未完成,进行中,完成"', allow_blank=True)
ws.add_data_validation(dv)
dv.add(f"E{hr + 1}:E{hr + 60}")

# ================= 4. 风险与应对 =================
ws = wb.create_sheet("风险与应对")
hr = head_block(
    ws, 4,
    "风险与应对",
    "按影响程度列出主要风险与应对措施、责任人",
)
risks = [
    ("原 PHP 后台与新系统数据/接口不兼容", "联调返工、上线风险", "中", "W4 专项联调；旧库隔离副本；保留旧 ID 与契约（李龙）"),
    ("第三方（支付/物流/发票/短信）开通延迟", "影响联调/上线", "中", "先用模拟接口打通；老韩提前并行申请"),
    ("真实短信供应商缺失", "注册/找回依赖", "中", "暂缓，先用隔离验证码（老韩/李龙）"),
    ("数据库迁移审批慢、结构风险", "阻断上线", "中", "提前提交 SQL，老韩隔离副本演练"),
    ("开发产能不足", "周期超标", "中", "砍低优先级长尾，老韩协调资源"),
    ("仅单元测试覆盖的子项未在真实环境验证", "验收证据不足", "中", "W5 集中补真实接口/真机验证（宫志强、李龙）"),
    ("真机/机型兼容问题", "体验与提审", "低", "W6 真机周，宫志强多机型回归"),
    ("旧系统触发器/视图历史缺陷", "报表异常", "低", "保持原状，上线前李龙评估"),
    ("W8 上线窗口遇元旦假期", "值守与应急响应不足", "中", "避开节假日上线或提前排班（老韩）"),
]
table(
    ws, hr,
    ["风险", "影响", "概率", "应对 / 责任人"],
    risks,
    [34, 16, 8, 46],
    center_cols=(3,),
)

wb.save(OUT)
print("saved:", OUT)
print("sheets:", [s.title for s in wb.worksheets])
