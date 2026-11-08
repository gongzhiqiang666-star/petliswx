import test from 'node:test';import assert from 'node:assert/strict';
import {catalogItemCommand,catalogPriceCommand} from '../src/components/legacy-test-catalog.js';
test('原项目默认内部/正常，保留名称原文且不填入目录价格',()=>{
 const c=catalogItemCommand({name:' 检测 ',code:' CODE '},'request_1234567890');assert.equal(c.type,0);assert.equal(c.active,1);assert.equal(c.name,' 检测 ');assert.ok(!('price' in c));assert.throws(()=>catalogItemCommand({name:'',code:'X'},'key'));
});
test('价格独立维护、默认正常、保留字符串金额与原允许的负价',()=>{
 const p=catalogPriceCommand({price:'-12.50'},'request_1234567890');assert.equal(p.price,'-12.50');assert.equal(p.active,1);assert.throws(()=>catalogPriceCommand({price:'1.009'},'key'));
});
test('价格编辑/删除使用当前快照，失败重试保持请求号且金额显示两位',async()=>{
 const {openCatalogPriceChange}=await import('../src/components/legacy-test-catalog.js');const Previous=globalThis.FormData;globalThis.FormData=class{constructor(f){this.f=f}entries(){return Object.entries(this.f.values)}};
 try{let modal;const writes=[];let fail=true;const context={dialog:d=>modal=d,toast(){},api:{request:async(url,options)=>{if(!options)return {IID:1,InfoTestTypeid:2,price:'120.50',IsActive:1,editToken:'a'.repeat(64)};writes.push({url,...options});if(fail)throw Error('timeout');return 1;}}};
 await openCatalogPriceChange(context,2,1,false,async()=>{});assert.match(modal.body,/120.50/);await assert.rejects(modal.onSubmit({values:{price:'135.00',active:'0'}}));fail=false;await modal.onSubmit({values:{price:'135.00',active:'0'}});assert.equal(writes[0].data.requestKey,writes[1].data.requestKey);assert.equal(writes[1].data.expectedToken,'a'.repeat(64));assert.equal(writes[1].method,'PUT');
 await openCatalogPriceChange(context,2,1,true,async()=>{});assert.match(modal.body,/永久删除/);assert.match(modal.body,/120.50/);await modal.onSubmit();assert.match(writes[2].url,/prices\/1\/delete$/);assert.equal(writes[2].data.expectedToken,'a'.repeat(64));
 }finally{globalThis.FormData=Previous}
});
test('价格编辑沿用原四位输入精度，新增仍为两位',async()=>{const {catalogPriceEditCommand}=await import('../src/components/legacy-test-catalog.js');assert.equal(catalogPriceEditCommand({price:'125.5555'},'request_1234567890','token').price,'125.5555');assert.throws(()=>catalogPriceCommand({price:'125.5555'},'request_1234567890'));assert.throws(()=>catalogPriceEditCommand({price:'125.55555'},'request_1234567890','token'));});
