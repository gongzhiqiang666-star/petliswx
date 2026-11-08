import test from 'node:test';
import assert from 'node:assert/strict';
import {configSnapshot,openConfigHistory} from '../src/components/config-history.js';
const snap={scopeType:'GLOBAL',scopeId:0,version:0,value:{enabled:true}};
test('只接受有值的全局版本快照，旧记录不伪装成可恢复',()=>{
 assert.deepEqual(configSnapshot(JSON.stringify(snap)),snap);
 for(const raw of [null,'bad','{}',{...snap,scopeType:'HOSPITAL'},{...snap,value:{}},{...snap,value:[]},{...snap,version:'1'}])assert.equal(configSnapshot(raw),null);
});
test('恢复提交固定来源ID和打开时的版本，展示快照而不把内容解释为HTML',async()=>{
 let dialog,saved=0;const calls=[];
 const context={toast(){},dialog(value){dialog=value},api:{async request(path,options){calls.push({path,options});if(path.endsWith('/history'))return {current:{version:4},history:{items:[{id:19,afterSnapshot:JSON.stringify(snap),reason:'历史原因'}],total:1}}}}};
 await openConfigHistory(context,'points.rule',()=>saved++);
 const preview={},select={value:'19:AFTER',addEventListener(){}};
 dialog.onReady({querySelector:key=>key.includes('preview')?preview:key.includes('auditId')?select:null});
 assert.equal(preview.textContent,JSON.stringify(snap.value,null,2));
 const original=globalThis.FormData;globalThis.FormData=class{entries(){return [['auditId','19:AFTER'],['reason',' 恢复原因 ']][Symbol.iterator]()}};
 try{await dialog.onSubmit({})}finally{globalThis.FormData=original}
 assert.deepEqual(calls[1],{path:'/admin/operations/configs/points.rule/restore',options:{method:'POST',data:{auditId:19,snapshotSide:'AFTER',version:4,reason:'恢复原因'}}});assert.equal(saved,1);
});
test('没有快照时只展示历史，不提供恢复提交',async()=>{
 let dialog;await openConfigHistory({dialog(value){dialog=value},api:{async request(){return {current:{version:2},history:{items:[{id:1,afterSnapshot:null}],total:1}}}}},'points.rule',()=>{});
 assert.equal(dialog.onSubmit,undefined);assert.match(dialog.body,/早期未保存快照/);
});
