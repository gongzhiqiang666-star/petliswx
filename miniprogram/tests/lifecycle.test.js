const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path')
function load(file,values){const context={module:{exports:{}},Set,Date,...values};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../utils',file),'utf8'),context);return context.module.exports}
function diagnostics(){const storage=new Map();const wx={getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,JSON.parse(JSON.stringify(v))),removeStorageSync:k=>storage.delete(k)};return {api:load('diagnostics.js',{wx,getCurrentPages:()=>[{route:'pages/order-detail/order-detail'}],require:()=>['pages/order-detail/order-detail']}),storage,wx}}
test('错误诊断不保存原始消息、查询、身份及完整路径，最近20条有界',()=>{
 const h=diagnostics();for(let i=0;i<25;i++)h.api.capture('rejection',{name:'TypeError',stack:'患者15900000000 token=secret\n at fn (https://example.test/private/order-detail.js:12:3?token=secret)'});
 const rows=h.api.read();assert.equal(rows.length,20);assert.equal(rows[0].type,'TypeError');assert.equal(rows[0].kind,'rejection');assert.deepEqual(Array.from(rows[0].frames),['order-detail.js:12:3']);assert.doesNotMatch(JSON.stringify(rows),/secret|15900000000|example|private|患者/);
 h.api.clear();assert.equal(h.api.read().length,0);
});
test('污染缓存和异常存储不泄露任意字段也不阻断业务',()=>{
 const h=diagnostics();h.api.capture('error','Error: synthetic');const good=h.api.read()[0];
 const bad=[{...good,message:'private-clinical-data'},{...good,route:'pages/orders/orders?token=secret'},{...good,frames:['https://example.test/file.js:1:2']},{...good,frames:['x'.repeat(100)+'.js:1:2']},{...good,time:'invalid'},{...good,type:'patient-name'}];
 h.storage.set('petlis_diagnostics_v1',[good,...bad]);assert.equal(h.api.read().length,1);assert.doesNotMatch(JSON.stringify(h.api.read()),/secret|private-clinical-data/);
 h.wx.getStorageSync=()=>{throw Error('storage unavailable')};h.wx.setStorageSync=()=>{throw Error('full')};assert.doesNotThrow(()=>h.api.capture('error',null));assert.equal(h.api.read().length,0);
});
function updates(){const callbacks={},prompts=[];let applied=0;const wx={getUpdateManager:()=>({onUpdateReady:fn=>callbacks.ready=fn,onUpdateFailed:fn=>callbacks.failed=fn,applyUpdate:()=>applied++}),showModal:options=>new Promise((resolve,reject)=>prompts.push({options,resolve,reject})),showToast:()=>{}};return {api:load('updates.js',{wx}),callbacks,prompts,wx,applied:()=>applied}}
test('更新仅确认后重启，重复就绪事件只显示一次，取消后可稍后更新',async()=>{
 const h=updates();h.api.installUpdates();const pending=h.callbacks.ready();await h.callbacks.ready();assert.equal(h.prompts.length,1);assert.equal(h.applied(),0);h.prompts[0].resolve({confirm:false});await pending;assert.equal(h.applied(),0);
 const retry=h.callbacks.ready();h.prompts[1].resolve({confirm:true});await retry;assert.equal(h.applied(),1);
});
test('更新弹窗失败不强制重启，旧运行环境不支持更新时可启动',async()=>{
 const h=updates();h.api.installUpdates();const pending=h.callbacks.ready();h.prompts[0].reject(Error('dismissed'));await pending;assert.equal(h.applied(),0);assert.doesNotThrow(()=>load('updates.js',{wx:{}}).installUpdates());
});
