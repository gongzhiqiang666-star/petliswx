import test from 'node:test';import assert from 'node:assert/strict';
import {dictionaryQuery,legacyTimestamp,createDictionaryReader} from '../src/components/legacy-dictionaries.js';
test('物种查询保留原状态0、独立中英文条件和分页，日期转换为秒',()=>{
 const q=dictionaryQuery({name:'猫',englishName:'Cat',active:'0',sort:'InfoAnimalId',order:'ASC',createdFrom:'2026-11-08T00:00:00'},0);
 assert.equal(q.active,0);assert.equal(q.name,'猫');assert.equal(q.englishName,'Cat');assert.equal(q.page,0);assert.equal(q.createdFrom,new Date('2026-11-08T00:00:00').getTime()/1000);assert.equal(q.size,20)
});
test('非法日期与倒置时间不能发出查询，原Unix秒按上海时间显示到秒',()=>{
 assert.throws(()=>dictionaryQuery({createdFrom:'not-a-date'},0));assert.throws(()=>dictionaryQuery({createdFrom:'2026-11-09T00:00',createdTo:'2026-11-08T00:00'},0));assert.equal(legacyTimestamp(100),'1970-01-01 08:01:40')
});
test('切换字典后旧响应/旧错误不覆盖当前列表',async()=>{
 const pending=[],visible=[];const api={request:(path)=>new Promise((resolve,reject)=>pending.push({path,resolve,reject}))};const read=createDictionaryReader(api,x=>visible.push(x),e=>visible.push(e.message));
 const old=read('species',{}),current=read('sex',{});pending[1].resolve({items:[{DictSexId:1}]});await current;pending[0].reject(Error('old failure'));await old;assert.deepEqual(visible,[{items:[{DictSexId:1}]}]);assert.equal(pending[1].path,'/admin/legacy/dictionaries/sex')
});

test('新增默认禁用、名称原文保留、超长或空值不能提交',async()=>{
 const {dictionaryCommand}=await import('../src/components/legacy-dictionary-editor.js');const schema={maxNameLength:50};
 const body=dictionaryCommand(schema,{name:' 猫 ',englishName:' Cat ',active:'0'},'request_1234567890');assert.equal(body.name,' 猫 ');assert.equal(body.englishName,' Cat ');assert.equal(body.active,0);
 assert.throws(()=>dictionaryCommand(schema,{name:' ',englishName:'Cat',active:'0'},'key'));assert.throws(()=>dictionaryCommand(schema,{name:'猫'.repeat(51),englishName:'Cat',active:'0'},'key'))
});
test('新建网络失败重试使用相同请求号，编辑固定原快照且失败保留输入',async()=>{
 const {openDictionaryEditor}=await import('../src/components/legacy-dictionary-editor.js');const Previous=globalThis.FormData;globalThis.FormData=class{constructor(form){this.form=form}entries(){return Object.entries(this.form.values)}};
 try{const schema={kind:'species',label:'物种',maxNameLength:250,createDefaultIsActive:0,columns:[{key:'InfoAnimalId'},{key:'AnimalName',label:'物种名称'},{key:'AnimalName_en',label:'物种名称(英文)'}]};let modal,fail=true;const writes=[];
 const context={dialog:x=>modal=x,toast(){},api:{request:async(url,options)=>{if(!options)return {InfoAnimalId:9,AnimalName:'旧值',AnimalName_en:'Old',IsActive:1,editToken:'a'.repeat(64)};writes.push({url,...options});if(fail)throw Error('网络失败');return 9}}};
 await openDictionaryEditor(context,schema,null,async()=>{});assert.match(modal.body,/value="0" selected/);const form={values:{name:'猫',englishName:'Cat',active:'0'}};await assert.rejects(modal.onSubmit(form),/网络失败/);fail=false;await modal.onSubmit(form);assert.equal(writes[0].data.requestKey,writes[1].data.requestKey);assert.equal(form.values.name,'猫');
 await openDictionaryEditor(context,schema,{InfoAnimalId:9},async()=>{});await modal.onSubmit(form);assert.equal(writes[2].method,'PUT');assert.equal(writes[2].data.expectedToken,'a'.repeat(64));assert.equal(writes[2].url,'/admin/legacy/dictionaries/species/9')
 }finally{globalThis.FormData=Previous}
});

test('批量确认展示当前原值，网络重试使用同一快照与请求号',async()=>{
 const {openDictionaryBatch}=await import('../src/components/legacy-dictionary-batch.js');let modal,fail=true;const writes=[];
 const schema={kind:'species',label:'物种',canWrite:true,canDelete:true,columns:[{key:'InfoAnimalId',label:'Id'},{key:'AnimalName',label:'物种名称'},{key:'AnimalName_en',label:'物种名称(英文)'}]};
 const context={dialog:x=>modal=x,toast(){},api:{request:async(url,options)=>{if(!options)return {InfoAnimalId:Number(url.split('/').pop()),AnimalName:'<b>原名</b>',AnimalName_en:'Original',IsActive:0,editToken:'a'.repeat(64)};writes.push({url,...options});if(fail)throw Error('网络失败');return {count:2}}}};
 await openDictionaryBatch(context,schema,[{InfoAnimalId:1},{InfoAnimalId:2}],1,async()=>{});assert.ok(modal.body.includes('&lt;b&gt;原名&lt;/b&gt;'));assert.ok(!modal.body.includes('<b>原名</b>'));
 await assert.rejects(modal.onSubmit(),/网络失败/);fail=false;await modal.onSubmit();assert.equal(writes[0].data.requestKey,writes[1].data.requestKey);assert.deepEqual(writes[0].data.targets,writes[1].data.targets);assert.equal(writes[1].data.active,1);
 await openDictionaryBatch(context,schema,[{InfoAnimalId:1}],'delete',async()=>{});assert.match(modal.body,/永久删除/);await modal.onSubmit();assert.equal(writes[2].url,'/admin/legacy/dictionaries/species/delete');assert.equal(writes[2].data.active,null)
});
test('无删除开关或重复选择不打开确认，也不发送写请求',async()=>{
 const {openDictionaryBatch}=await import('../src/components/legacy-dictionary-batch.js');let calls=0;const context={dialog(){throw Error('unexpected')},api:{request:async()=>calls++}};const schema={kind:'sex',canWrite:true,canDelete:false,columns:[{key:'DictSexId'}]};
 await assert.rejects(openDictionaryBatch(context,schema,[{DictSexId:1}],'delete',()=>{}));schema.canDelete=true;await assert.rejects(openDictionaryBatch(context,schema,[{DictSexId:1},{DictSexId:1}],0,()=>{}));assert.equal(calls,0)
});

test('品种编辑不新增原表不存在的状态字段',async()=>{
 const {dictionaryCommand,openDictionaryEditor}=await import('../src/components/legacy-dictionary-editor.js');
 const schema={kind:'variety',label:'品种',hasStatus:false,maxNameLength:50,columns:[{key:'IID',label:'Id'},{key:'Variety_CN',label:'中文名称'},{key:'Variety_EN',label:'英文名称'}]};
 assert.equal(dictionaryCommand(schema,{name:'猫',englishName:'Cat'},'request_1234567890').active,null);
 let modal;await openDictionaryEditor({dialog:x=>modal=x,api:{},toast(){}},schema,null,()=>{});assert.ok(!modal.body.includes('name="active"'));
});

test('single-name check-received dictionary does not invent a required English value',async()=>{
 const {dictionaryCommand,openDictionaryEditor}=await import('../src/components/legacy-dictionary-editor.js');
 const schema={kind:'checkReceived',label:'验收方式',hasEnglishName:false,hasStatus:true,maxNameLength:250,createDefaultIsActive:0,columns:[{key:'InfoCheckReceivedId',label:'Id'},{key:'CheckType',label:'确认方式'},{key:'IsActive',label:'状态'}]};
 const command=dictionaryCommand(schema,{name:'签字确认',active:'1'},'request_1234567890');assert.equal(command.name,'签字确认');assert.equal(command.active,1);
 let dialog;await openDictionaryEditor({api:{},dialog:value=>dialog=value},schema,null,()=>{});
 assert.ok(dialog.body.includes('name="name"'));assert.ok(!dialog.body.includes('name="englishName"'));assert.ok(dialog.body.includes('name="active"'));
});
