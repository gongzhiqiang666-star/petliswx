import test from 'node:test';
import assert from 'node:assert/strict';
import {canRemindBill} from '../src/pages/finance.js';
const overdue={status:'OVERDUE',overdueDays:1,outstandingAmount:'60.00',lastOverdueRemindedAt:null};
test('仅未结清且尚未提醒的逾期账单显示催款入口',()=>{
 assert.equal(canRemindBill(overdue),true);
 for(const patch of [{status:'SETTLED'},{overdueDays:0},{overdueDays:-1},{outstandingAmount:0},{lastOverdueRemindedAt:'2026-11-07 12:00:00'}])assert.equal(canRemindBill({...overdue,...patch}),false);
 assert.equal(canRemindBill({status:'PENDING_RECONCILIATION',overdue_days:2,outstanding_amount:'10',last_overdue_reminded_at:null}),true);
});
