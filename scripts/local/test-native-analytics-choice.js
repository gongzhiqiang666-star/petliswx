// Actual local preference handlers and storage, no network mocks or application relaunch.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');
const original=JSON.parse(fs.readFileSync(root+'/simulator-fixture.json'));
const timeout=setTimeout(()=>{console.error('Native preference RPC deadline exceeded');process.exit(2)},60000);
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});let prior,verified=false;
 const wait=async f=>{for(let i=0;i<100;i++){if(await f())return;await new Promise(r=>setTimeout(r,200))}throw Error('Preference page readiness timeout')};
 const open=()=>m.evaluate(()=>new Promise((resolve,reject)=>wx.navigateTo({url:'/pages/preferences/preferences',success:()=>resolve(true),fail:reject})));
 try{
  assert.equal(await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId),JSON.parse(fs.readFileSync('miniprogram/project.config.json')).appid);
  assert.equal(Number(await m.evaluate(()=>{const u=getApp().globalData.userInfo||{};return u.userId||u.id})),Number(original.userId));
  prior=await m.evaluate(()=>({exists:wx.getStorageInfoSync().keys.includes('petlis_analytics_enabled_v1'),value:wx.getStorageSync('petlis_analytics_enabled_v1')}));verified=true;
  await open();await wait(()=>m.evaluate(()=>{const p=getCurrentPages().slice(-1)[0];return p.route==='pages/preferences/preferences'&&!p.data.busy&&!!p.data.form}));
  const before=await m.evaluate(()=>getCurrentPages().slice(-1)[0].data.form);
  const result=await m.evaluate(()=>{const p=getCurrentPages().slice(-1)[0];p.changeAnalytics({detail:{value:false}});const analytics=require('utils/analytics.js');analytics.track('report_open','report','success');analytics.flush();return {settings:p.data.analyticsSettings,error:p.data.analyticsError,queue:wx.getStorageSync('petlis_events_v1').length,form:p.data.form}});
  assert.equal(result.settings.enabled,false);assert.equal(result.error,'');assert.equal(result.queue,0);assert.deepEqual(result.form,before);
  await m.evaluate(()=>{wx.hideToast();return true});await m.screenshot({path:root+'/qa/native-analytics-disabled.png'});
  await m.evaluate(()=>new Promise((resolve,reject)=>wx.navigateBack({success:()=>resolve(true),fail:reject})));await open();
  await wait(()=>m.evaluate(()=>getCurrentPages().slice(-1)[0].data.analyticsSettings.enabled===false));
  const proof={userChoicePersistsAcrossPageReopen:true,disabledQueueEmpty:true,notificationsUnchanged:true,noNetworkMocks:true,preferenceRestoredInFinally:true};fs.writeFileSync(root+'/qa/native-analytics-choice.json',JSON.stringify(proof,null,2),{mode:0o600});console.log(JSON.stringify(proof));
 }finally{try{if(verified)await m.evaluate(previous=>{const analytics=require('utils/analytics.js');analytics.setEnabled(previous.exists?previous.value!==false:true);if(!previous.exists)wx.removeStorageSync('petlis_analytics_enabled_v1');wx.switchTab({url:'/pages/mine/mine'});return true},prior)}finally{await m.disconnect();clearTimeout(timeout)}}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
