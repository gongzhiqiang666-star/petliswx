// Invoke the actual compiled request helper; real local responses, no PDF viewer or remote requests.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator'),file=root+'/simulator-fixture.json',original=JSON.parse(fs.readFileSync(file)),chain=JSON.parse(fs.readFileSync(root+'/qa/native-full-chain.json'));
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});let authCleared=false;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Download QA native readiness timeout')};
 try{
 assert.equal(await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId),JSON.parse(fs.readFileSync('miniprogram/project.config.json')).appid);
 await wait(async()=>(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username))===original.account);
 const denied=await m.evaluate(async()=>{try{await require('utils/request.js').downloadFile('/platform-files/117/download');return {rejected:false}}catch(e){return {rejected:true,message:e.message}}});assert.equal(denied.rejected,true);assert.match(denied.message,/权限|管理员/);
 const valid=await m.evaluate(async id=>{const path=await require('utils/request.js').downloadFile('/reports/'+id+'/download'),fs=wx.getFileSystemManager();try{return {prefix:fs.readFileSync(path,'utf8',0,5),size:fs.statSync(path).size}}finally{try{fs.unlinkSync(path)}catch(e){}}},chain.reportIds[0]);assert.equal(valid.prefix,'%PDF-');assert.ok(valid.size>5);
 await m.evaluate(()=>{getApp().globalData.token='petlis-local-qa-invalid-token';return true});authCleared=true;
 const expired=await m.evaluate(async id=>{try{await require('utils/request.js').downloadFile('/reports/'+id+'/download');return {rejected:false}}catch(e){return {rejected:true,message:e.message,tokenCleared:!getApp().globalData.token}}},chain.reportIds[0]);assert.equal(expired.rejected,true);assert.equal(expired.tokenCleared,true);
 const result={realBusiness403Rejected:true,errorMessage:denied.message,realPdfDownloadValidated:true,pdfBytes:valid.size,controlledInvalidTokenHTTP401Rejected:true,expiredLoginCleared:true,externalRequestMade:false,viewerOpened:false};fs.writeFileSync(root+'/qa/native-download-error-validation.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{
 try{if(authCleared){let p;await wait(async()=>{p=await m.currentPage();return p.path==='pages/login/login'&&await p.$('input[data-field="account"]')});for(const field of ['account','password'])await(await p.$('input[data-field="'+field+'"]')).trigger('input',{value:original[field]});await(await p.$('.login-button')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/home/home'});assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),original.account);original.session=await m.evaluate(()=>({...getApp().globalData.userInfo,token:getApp().globalData.token}));fs.writeFileSync(file,JSON.stringify(original,null,2),{mode:0o600});}}finally{await m.disconnect()}
 }
})().catch(e=>{console.error(e.stack);process.exitCode=1});
