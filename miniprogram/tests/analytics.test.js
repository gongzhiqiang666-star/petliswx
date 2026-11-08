const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path')
function load({token='',disabled=false,fail=false}={}) {
 const storage=new Map(),requests=[],timing={now:1000};
 const wx={getExtConfigSync:()=>({analyticsEnabled:!disabled}),getStorageSync:key=>key==='token'?token:storage.get(key),setStorageSync:(key,value)=>storage.set(key,JSON.parse(JSON.stringify(value))),request:options=>{requests.push(options);if(fail){options.fail();options.complete()}}};
 const context={module:{exports:{}},require:()=>({baseURL:'http://127.0.0.1:8082/api',environment:'develop',storageKeys:{TOKEN:'token'}}),wx,Date:{now:()=>timing.now},Math,Set,getApp:()=>({globalData:{}})};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../utils/analytics.js'),'utf8'),context);
 return {analytics:context.module.exports,storage,requests,timing,wx};
}
test('事件仅持久化白名单枚举、随机事件号，未登录不发送且队列有界',()=>{
 const h=load();for(let i=0;i<60;i++)h.analytics.track('order_step','pet','view');
 const rows=h.storage.get('petlis_events_v1');assert.equal(rows.length,50);assert.equal(h.requests.length,0);
 assert.deepEqual(Object.keys(rows[0]).sort(),['environment','event','id','outcome','step']);
 h.analytics.track('15900000000','pet','view');h.analytics.track('order_step','患者诊断','view');
 assert.equal(h.storage.get('petlis_events_v1').length,50);
});
test('成功应答才删除事件，批次最多20条，重复flush不并发发送',()=>{
 const h=load({token:'test-token'});h.analytics.track('order_step','pet','view');h.analytics.flush();
 assert.equal(h.requests.length,1);const req=h.requests[0];assert.equal(req.data.events.length,1);
 assert.equal(req.header.Authorization,'Bearer test-token');assert.equal(req.timeout,3000);
 assert.equal(h.storage.get('petlis_events_v1').length,1);
 req.success({statusCode:200,data:{code:200}});req.complete();assert.equal(h.storage.get('petlis_events_v1').length,0);
});
test('失败静默保留同一事件号，退避后重试；禁用不采集',()=>{
 const h=load({token:'test-token',fail:true});assert.doesNotThrow(()=>h.analytics.track('payment_result','checkout','failure'));
 const id=h.storage.get('petlis_events_v1')[0].id;h.analytics.flush();assert.equal(h.requests.length,1);
 h.timing.now+=31000;h.analytics.flush();assert.equal(h.requests.length,2);assert.equal(h.requests[1].data.events[0].id,id);
 const off=load({disabled:true,token:'test-token'});off.analytics.track('report_open','report','success');assert.equal(off.requests.length,0);assert.equal(off.storage.size,0);
});
test('污染的本地队列字段不会发往接口',()=>{
 const h=load({token:'test-token'});h.storage.set('petlis_events_v1',[{id:'a1234567-1234-4234-8234-123456789012',event:'order_step',step:'pet',outcome:'view',environment:'develop',mobile:'private'}]);
 h.analytics.flush();assert.equal(h.requests.length,0);
});

test('确认后本地存储写失败也不能形成立即重传循环',()=>{
 const h=load({token:'test-token'});h.analytics.track('report_open','report','success');
 h.wx.setStorageSync=()=>{throw new Error('storage unavailable')};
 h.requests[0].success({statusCode:200,data:{code:200}});h.requests[0].complete();
 h.analytics.flush();assert.equal(h.requests.length,1);assert.equal(h.storage.get('petlis_events_v1').length,1);
});
test('运行中关闭埋点清除待发队列且不再采集或发送',()=>{
 const h=load();h.analytics.track('order_step','pet','view');assert.equal(h.storage.get('petlis_events_v1').length,1);
 h.wx.getExtConfigSync=()=>({analyticsEnabled:false});h.analytics.flush();h.analytics.track('report_open','report','success');
 assert.equal(h.storage.get('petlis_events_v1').length,0);assert.equal(h.requests.length,0);
});

test('用户关闭本机统计后清队列并停止采集，重新开启不重放旧记录',()=>{
 const h=load();h.analytics.track('order_step','pet','view');h.analytics.setEnabled(false);
 assert.equal(h.analytics.getSettings().enabled,false);assert.equal(h.storage.get('petlis_events_v1').length,0);
 h.analytics.track('report_open','report','success');assert.equal(h.storage.get('petlis_events_v1').length,0);
 h.analytics.setEnabled(true);assert.equal(h.analytics.getSettings().enabled,true);h.analytics.flush();assert.equal(h.requests.length,0)
});
test('关闭时中止在途统计；部署关闭不能被用户开关覆盖',()=>{
 const h=load({token:'test-token'});let aborted=0;h.wx.request=options=>{h.requests.push(options);return {abort(){aborted++;options.fail();options.complete()}}};
 h.analytics.track('report_open','report','success');h.analytics.setEnabled(false);assert.equal(aborted,1);assert.equal(h.storage.get('petlis_events_v1').length,0);
 const off=load({disabled:true});off.analytics.setEnabled(true);assert.equal(off.analytics.getSettings().available,false);assert.equal(off.analytics.getSettings().enabled,false)
});
test('设置写入失败向页面反馈，不能假装已关闭',()=>{
 const h=load();h.wx.setStorageSync=()=>{throw Error('storage unavailable')};assert.throws(()=>h.analytics.setEnabled(false));assert.equal(h.analytics.getSettings().enabled,true)
});
test('清队列失败时保持停止状态，未成功清理前不能重新开启发送旧记录',()=>{
 const h=load();h.analytics.track('order_step','pet','view');const write=h.wx.setStorageSync;
 h.wx.setStorageSync=(key,value)=>{if(key==='petlis_events_v1')throw Error('queue unavailable');write(key,value)};
 assert.throws(()=>h.analytics.setEnabled(false));assert.equal(h.analytics.getSettings().enabled,false);
 assert.throws(()=>h.analytics.setEnabled(true));assert.equal(h.analytics.getSettings().enabled,false);assert.equal(h.requests.length,0);
 h.wx.setStorageSync=write;h.analytics.setEnabled(true);assert.equal(h.storage.get('petlis_events_v1').length,0)
});
