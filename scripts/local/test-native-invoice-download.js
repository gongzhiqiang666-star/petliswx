// Finish the existing local invoice and restore the original simulator identity through normal login.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator');
const file=root+'/qa/native-invoice-fixture-private.json',f=JSON.parse(fs.readFileSync(file)),oldFile=root+'/simulator-fixture.json',old=JSON.parse(fs.readFileSync(oldFile));
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});const wait=async fn=>{for(let i=0;i<120;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Native invoice download readiness timeout')};
 try{
 assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),f.account);
 let p=await m.reLaunch('/pages/invoices/invoices');await wait(async()=>!await p.data('busy')&&await p.$('button[data-id="history"]'));
 await(await p.$('button[data-id="history"]')).tap();await wait(async()=>!await p.data('busy')&&(await p.data('items')).some(x=>Number(x.id)===f.applicationId&&x.status==='issued'));
 const button=await p.$('button[data-url="/api/invoices/'+f.applicationId+'/download"]');assert.ok(button);
 await m.screenshot({path:root+'/qa/native-invoice-issued.png'});
 await button.tap();await wait(async()=>!await p.data('busy'));assert.equal(await p.data('error'),'');
 const result={orderId:f.orderId,applicationId:f.applicationId,issuedHistoryVisible:true,authenticatedNativeDownloadAccepted:true,openDocumentReturnedWithoutError:true,pdfRendererVisuallyVerified:false};
 // openDocument may leave a native viewer open; reLaunch/login remains through app APIs.
 await m.evaluate(async()=>{await getApp().logout();return true});
 await wait(async()=>{p=await m.currentPage();return p.path==='pages/login/login'&&await p.$('input[data-field="account"]')});
 await(await p.$('input[data-field="account"]')).trigger('input',{value:old.account});await(await p.$('input[data-field="password"]')).trigger('input',{value:old.password});await(await p.$('.login-button')).tap();
 await wait(async()=>{p=await m.currentPage();return p.path==='pages/home/home'});
 assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),old.account);
 old.session=await m.evaluate(()=>({...getApp().globalData.userInfo,token:getApp().globalData.token}));
 fs.writeFileSync(oldFile,JSON.stringify(old,null,2),{mode:0o600});f.completed=true;fs.writeFileSync(file,JSON.stringify(f,null,2),{mode:0o600});result.originalSimulatorAccountRestored=true;
 fs.writeFileSync(root+'/qa/native-invoice-download.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{await m.disconnect()}
})().catch(e=>{console.error(e.stack);process.exitCode=1});
