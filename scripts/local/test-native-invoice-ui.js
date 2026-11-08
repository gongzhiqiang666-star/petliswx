// Real local invoice APIs; only the OS confirmation is mocked, never payment or invoice responses.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');
const fixture=root+'/qa/native-invoice-fixture-private.json',f=JSON.parse(fs.readFileSync(fixture));
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});let mocked=false;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Invoice UI readiness timeout')};
 try{
 assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),f.account);
 const p=await m.reLaunch('/pages/invoices/invoices');await wait(async()=>!await p.data('busy')&&(await p.data('orders')).some(x=>Number(x.orderId)===f.orderId));
 assert.ok(!f.applicationId,'Synthetic invoice already submitted; inspect existing evidence');
 await(await p.$('.card-actions button:last-child')).tap();await wait(async()=>await p.data('adding'));
 const input=async(field,value)=>await(await p.$('input[data-field="'+field+'"]')).trigger('input',{value});
 const title='本机发票验收'+f.orderId;
 await input('title',title);await input('taxNumber','TESTNATIVE123456789');await input('email','invalid-email');
 await(await p.$('.petlis-modal-actions .petlis-btn-primary')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('error'));
 assert.ok(await p.$('.petlis-modal .field-error'));assert.equal(await p.data('title'),title);assert.equal(await p.data('email'),'invalid-email');
 const result={orderId:f.orderId,invalidEmailRejected:true,errorInsideModal:true,invalidFormRetained:true,systemConfirmationMocked:true};
 await m.screenshot({path:root+'/qa/native-invoice-profile-error-after.png'});
 await input('email','invoice@petlis.example.test');await(await p.$('.petlis-modal-actions .petlis-btn-primary')).tap();
 await wait(async()=>!await p.data('busy')&&!await p.data('adding')&&(await p.data('profiles')).some(x=>x.title===title));
 const index=(await p.data('profiles')).findIndex(x=>x.title===title);await(await p.$('.form-item picker')).trigger('change',{value:index});
 await(await p.$('.card[data-id="'+f.orderId+'"]')).tap();await wait(async()=>(await p.data('selected')).some(x=>Number(x)===f.orderId));
 await m.mockWxMethod('showModal',{confirm:true,cancel:false,errMsg:'showModal:ok'});mocked=true;
 await(await p.$('.bottom-actions .petlis-btn-primary')).tap();
 await wait(async()=>!await p.data('busy')&&await p.data('tab')==='history'&&(await p.data('items')).length>0);
 const items=await p.data('items');assert.equal(items.length,1);assert.equal(items[0].status,'pending');
 f.applicationId=Number(items[0].id);fs.writeFileSync(fixture,JSON.stringify(f,null,2),{mode:0o600});
 Object.assign(result,{profileSaved:true,applicationId:f.applicationId,pendingHistoryVisible:true});
 await m.screenshot({path:root+'/qa/native-invoice-pending.png'});fs.writeFileSync(root+'/qa/native-invoice-application.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{if(mocked)await m.restoreWxMethod('showModal');await m.disconnect()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
