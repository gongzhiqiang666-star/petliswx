import test from 'node:test';
import assert from 'node:assert/strict';
import { dateInputValue, nextCalendarDate } from '../src/utils.js';

test('上海时区月初和今天的输入不被UTC转换前移一天',()=>{
 const previous=process.env.TZ;process.env.TZ='Asia/Shanghai';
 try {
  assert.equal(dateInputValue(new Date(2026,10,1)), '2026-11-01');
  assert.equal(dateInputValue(new Date(2026,10,7,23,59)), '2026-11-07');
  assert.equal(nextCalendarDate('2026-11-07'),'2026-11-08');
 } finally { if(previous===undefined)delete process.env.TZ;else process.env.TZ=previous; }
});
test('含当天的结束日期按日历推进，跨月/闰日/年末不漏日',()=>{
 assert.equal(nextCalendarDate('2024-02-28'),'2024-02-29');
 assert.equal(nextCalendarDate('2024-02-29'),'2024-03-01');
 assert.equal(nextCalendarDate('2026-12-31'),'2027-01-01');
 assert.throws(()=>nextCalendarDate('2026-02-31'));
 assert.throws(()=>nextCalendarDate('not-a-date'));
});
