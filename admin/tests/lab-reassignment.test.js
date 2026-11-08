import test from 'node:test';
import assert from 'node:assert/strict';
import {assignmentLabel,availableTaskActions} from '../src/pages/laboratory.js';
test('分配历史显示业务人员设备，缺失快照不会显示原始JSON错误',()=>{
 assert.equal(assignmentLabel('{"assignedTo":5,"equipmentCode":"PCR-1","version":3}'),'人员 #5 · 设备 PCR-1');
 assert.equal(assignmentLabel({assignedTo:null,equipmentCode:null}),'人员 未分配 · 设备 未指定');
 assert.equal(assignmentLabel('bad'),'历史快照缺失');
});
test('任务按钮与状态及当前录入人员一致',()=>{
 const task={status:'TESTING',assigned_to:5,results:[]},staff={id:5,roles:['LAB_STAFF']};
 assert.deepEqual(availableTaskActions(task,staff),{results:true,reassign:true,return:true,retest:false});
 assert.equal(availableTaskActions(task,{id:9,roles:['ADMIN']}).results,false);
 assert.equal(availableTaskActions({...task,results:[{id:1}]},staff).reassign,false);
 assert.equal(availableTaskActions({...task,status:'RESULT_ENTERED'},staff).reassign,false);
 assert.deepEqual(availableTaskActions({...task,status:'COMPLETED'},staff),{results:false,reassign:false,return:false,retest:false});
 assert.equal(availableTaskActions({...task,status:'RETURNED'},staff).retest,true);
 assert.deepEqual(availableTaskActions(task,{id:5,roles:['DOCTOR']}),{results:false,reassign:false,return:false,retest:false});
});
