// Real local inbox/preferences. Inject HTTP503 only for the two explicit recovery checks.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const automator=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');
const f=JSON.parse(fs.readFileSync(root+'/qa/native-invoice-fixture-private.json')),oldFile=root+'/simulator-fixture.json',old=JSON.parse(fs.readFileSync(oldFile));
(async()=>{const m=await automator.connect({wsEndpoint:'ws://127.0.0.1:9420'});let mocked=false,identityChecked=false;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Native notification readiness timeout')};
 const login=async account=>{await m.evaluate(async()=>{await getApp().logout();return true});let p;await wait(async()=>{p=await m.currentPage();return p.path==='pages/login/login'&&await p.$('input[data-field="account"]')});for(const field of ['account','password'])await(await p.$('input[data-field="'+field+'"]')).trigger('input',{value:account[field]});await(await p.$('.login-button')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/home/home'});};
 try{
 const project=JSON.parse(fs.readFileSync('miniprogram/project.config.json'));assert.equal(await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId),project.appid);
 const current=await m.evaluate(()=>(getApp().globalData.userInfo||{}).username);assert.ok([old.account,f.account].includes(current));identityChecked=true;if(current!==f.account)await login(f);
 let p=await m.navigateTo('/pages/preferences/preferences');await wait(async()=>!await p.data('busy')&&!!await p.data('form'));
 const baselineFile=root+'/qa/native-notification-preferences-baseline.json';const baseline=fs.existsSync(baselineFile)?JSON.parse(fs.readFileSync(baselineFile)):await p.data('form');if(!fs.existsSync(baselineFile))fs.writeFileSync(baselineFile,JSON.stringify(baseline,null,2));
 const desired=!baseline.invoiceNotifications;
 await(await p.$('switch[data-key="invoiceNotifications"]')).trigger('change',{value:desired});
 await m.mockWxMethod('request',{statusCode:503,data:{code:503,message:'本机验收注入服务暂不可用'}});mocked=true;
 await(await p.$('.bottom-actions .petlis-btn-primary')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('error'));
 assert.equal(await p.data('form.invoiceNotifications'),desired);assert.match(await(await p.$('.error button')).text(),/重试保存/);
 await m.screenshot({path:root+'/qa/native-notification-preferences-error.png'});
 await m.restoreWxMethod('request');mocked=false;await(await p.$('.error button')).tap();await wait(async()=>!await p.data('busy')&&await p.data('error')==='');
 p=await m.reLaunch('/pages/preferences/preferences');await wait(async()=>!await p.data('busy')&&!!await p.data('form'));assert.equal(Boolean(await p.data('form.invoiceNotifications')),desired);
 await(await p.$('switch[data-key="invoiceNotifications"]')).trigger('change',{value:baseline.invoiceNotifications});await(await p.$('.bottom-actions .petlis-btn-primary')).tap();await wait(async()=>!await p.data('busy')&&await p.data('error')==='');
 p=await m.switchTab('/pages/notifications/notifications');await wait(async()=>!await p.data('busy')&&(await p.data('items')).length>0);
 const before=Number(await p.data('count'));assert.ok(before>0);const item=(await p.data('items')).find(x=>!x.readAt);assert.ok(item);
 await(await p.$('.card[data-id="'+item.id+'"]')).tap();await wait(async()=>(await m.currentPage()).path==='pages/invoices/invoices');
 p=await m.switchTab('/pages/notifications/notifications');await wait(async()=>!await p.data('busy')&&Number(await p.data('count'))===before-1);
 await m.mockWxMethod('request',{statusCode:503,data:{code:503,message:'本机验收注入服务暂不可用'}});mocked=true;
 await(await p.$('.page > .row > button')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('error'));assert.equal(Number(await p.data('count')),before-1);
 await m.screenshot({path:root+'/qa/native-notification-readall-error.png'});
 await m.restoreWxMethod('request');mocked=false;await(await p.$('.page > .row > button')).tap();await wait(async()=>!await p.data('busy')&&Number(await p.data('count'))===0);
 await(await p.$('.notification-filter button')).tap();await wait(async()=>!await p.data('busy')&&await p.data('unread')===true&&(await p.data('items')).length===0);
 await m.screenshot({path:root+'/qa/native-notification-unread-empty.png'});
 const result={syntheticUserId:f.userId,preferenceFailureInjectedHTTP503:true,preferenceChoiceRetained:true,retrySavedRealBackend:true,reloadConfirmedPersistence:true,originalPreferenceRestored:true,readNavigatesToInvoice:true,singleReadCountDecreased:true,readAllFailureInjectedHTTP503:true,readAllErrorRetained:true,readAllRealBackendSucceeded:true,unreadFilterEmpty:true,externalPushCalled:false};
 fs.writeFileSync(root+'/qa/native-notification-ui.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{
 if(mocked)await m.restoreWxMethod('request');
 if(identityChecked){const current=await m.evaluate(()=>(getApp().globalData.userInfo||{}).username);if(current===f.account)await login(old);assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),old.account);old.session=await m.evaluate(()=>({...getApp().globalData.userInfo,token:getApp().globalData.token}));fs.writeFileSync(oldFile,JSON.stringify(old,null,2),{mode:0o600});}
 await m.disconnect();
 }
})().catch(e=>{console.error(e.stack);process.exitCode=1});
