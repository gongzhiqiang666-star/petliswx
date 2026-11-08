const test=require('node:test'),assert=require('node:assert/strict');
const {loadPage}=require('./helpers');
test('全部已读失败保留错误和原列表而不自动刷新清错',async()=>{
 let reads=0;const {instance:p}=loadPage('notifications/notifications',{put:async()=>{throw Error('网络暂不可用')},get:async()=>{reads++;return {items:[],total:0}}});
 p.data.items=[{id:7,title:'未读消息'}];p.data.count=1;await p.all();
 assert.equal(p.data.error,'网络暂不可用');assert.equal(p.data.count,1);assert.equal(p.data.items.length,1);assert.equal(reads,0);assert.equal(p.data.busy,false);
});
test('全部已读忙碌时重复点击不产生第二个写入',async()=>{
 let writes=0;const {instance:p}=loadPage('notifications/notifications',{put:async()=>writes++});p.data.busy=true;await p.all();assert.equal(writes,0);
});
test('消息加载中不能改变未读筛选而把旧响应当作新筛选结果',()=>{
 const {instance:p}=loadPage('notifications/notifications');p.data.busy=true;p.filter();assert.equal(p.data.unread,false);
});
test('设置保存失败后重试发送原选择而不重新加载覆盖',async()=>{
 let gets=0;const writes=[];let fail=true;
 const {instance:p}=loadPage('preferences/preferences',{get:async()=>{gets++;return {invoiceNotifications:true}},put:async(url,body)=>{writes.push({...body});if(fail)throw Error('保存失败')}});
 p.data.form={invoiceNotifications:false,reportNotifications:true};await p.save();assert.equal(p.data.error,'保存失败');assert.equal(p.data.form.invoiceNotifications,false);
 assert.equal(typeof p.retry,'function');fail=false;await p.retry();assert.equal(gets,0);assert.equal(writes.length,2);assert.equal(writes[1].invoiceNotifications,false);assert.equal(p.data.error,'');
});
test('设置保存期间阻止更改开关及刷新覆盖表单',async()=>{
 let gets=0;const {instance:p}=loadPage('preferences/preferences',{get:async()=>gets++});p.data.busy=true;p.data.form={invoiceNotifications:false};
 p.change({currentTarget:{dataset:{key:'invoiceNotifications'}},detail:{value:true}});await p.load();assert.equal(gets,0);assert.equal(p.data['form.invoiceNotifications'],undefined);
});
test('本机统计开关即时保存，不改动通知表单或调用保存接口',()=>{
 let writes=0;const {instance:p,storage}=loadPage('preferences/preferences',{put:async()=>writes++});p.data.form={reportNotifications:true};p.onShow();p.changeAnalytics({detail:{value:false}});
 assert.equal(p.data.analyticsSettings.enabled,false);assert.equal(storage.petlis_analytics_enabled_v1,false);assert.equal(p.data.form.reportNotifications,true);assert.equal(writes,0)
});
test('本机统计设置失败保留错误并可直接重试，不清除通知选择',()=>{
 const {instance:p,wx}=loadPage('preferences/preferences');const original=wx.setStorageSync;p.data.form={reportNotifications:false};wx.setStorageSync=()=>{throw Error('disk full')};p.changeAnalytics({detail:{value:false}});assert.match(p.data.analyticsError,/重试/);assert.equal(p.data.form.reportNotifications,false);
 wx.setStorageSync=original;p.changeAnalytics({detail:{value:false}});assert.equal(p.data.analyticsError,'');assert.equal(p.data.analyticsSettings.enabled,false)
});
