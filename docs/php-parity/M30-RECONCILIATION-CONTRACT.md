# M30 原付款汇总回写：源码契约与待决定项

冻结源码：`application/index/controller/Cron.php::index`。当前只读分析，未执行作业或状态回写。

原查询以 `vwdiagnosisdetail` 的 `IsPayMoney=0` 明细为基础，按 `UserNO` 分组，满足 `SUM(IF(PayStatus2=0,TestPrice,0))=0.00 AND MIN(PayStatus2)<>0` 后，按未聚合的主单编号降序取100组。把非聚合的 `PayStatus2` 回写到所有 `UserNO` 相同的主单。硬编码 hospid=434 没有用于过滤，不能误当医院范围限制；不修改明细付款标志、金额或资金池。

注意：更新语句只按UserNO，不再限制父单原IsPayMoney=0，重复单号可能涉及多张主单。不能擅自改成按主键更新一张，也不能隐瞒跨主单影响；后续PC应完整列出受影响父单，确认后受控写入。原PHP公开无登录作业入口不搬，改用Java管理员、业务开关、手动预览确认与事务审计，定时任务仍关闭。

## 本机只读核对

仅查询127.0.0.1:13306/laboklin，未输出真实单号、患者或医院：候选2543组，其中6组含不同付款状态，45组对应多个主单。该统计使用所有符合条件分组，不是原单次100组执行结果，也不代表线上库当前数量。

统计SQL：

```sql
SELECT COUNT(*), COALESCE(SUM(statusKinds>1),0), COALESCE(SUM(parentCount>1),0)
FROM (
  SELECT UserNO,
    COUNT(DISTINCT COALESCE(CAST(PayStatus2 AS CHAR),'NULL')) statusKinds,
    COUNT(DISTINCT ListDiagnosisProjectId) parentCount
  FROM vwdiagnosisdetail WHERE IsPayMoney=0
  GROUP BY UserNO
  HAVING SUM(IF(PayStatus2=0,TestPrice,0))=0.00 AND MIN(PayStatus2)<>0
) s;
```

## 待用户决定：混合状态如何处理

原PHP选择未聚合PayStatus2，没有稳定规则。合成例：同一单号下两个明细状态分别1和3且均已结，原SQL可能回写1或3，不能凭Java默认排序、MIN或MAX假装等价。

建议：一致状态按原条件回写；混合状态仅预览、默认不自动改，管理员核对原明细后明确选择目标状态，再按已确认范围写回。保留原实际手动核对和状态汇总能力，不发明统一已付状态。此项改变了不稳定状态的选择方式，需用户确认；未确认前只做只读预览与其他已授权模块。

尚未实现/验证：精确单次100组及未聚合排序兼容、受影响全部父单快照、手动决定混合状态、版本/并发/幂等/审计/回滚、新旧写入者与实际页面验收。不得称对账完成。无DDL，原PHP冻结。
