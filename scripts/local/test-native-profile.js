// Actual native page/form/upload APIs; only media pickers and one HTTP503 failure are mocked.
const fs=require('fs'),assert=require('assert/strict'),root=process.env.HOME+'/.local/share/petlis';
const a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator'),file=root+'/qa/native-profile-fixture-private.json',f=JSON.parse(fs.readFileSync(file)),oldFile=root+'/simulator-fixture.json',old=JSON.parse(fs.readFileSync(oldFile)),verify=process.argv.includes('--verify-approved');
(async()=>{const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});let checked=false,requestMock=false,mediaMock=false,fileMock=false,paths;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Native profile readiness timeout')};
 const login=async account=>{let p=await m.currentPage();if(p.path!=='pages/login/login'){await m.evaluate(async()=>{await getApp().logout();return true})}await wait(async()=>{p=await m.currentPage();return p.path==='pages/login/login'&&await p.$('input[data-field="account"]')});for(const key of ['account','password'])await(await p.$('input[data-field="'+key+'"]')).trigger('input',{value:account[key]});await(await p.$('.login-button')).tap();await wait(async()=>{p=await m.currentPage();return p.path==='pages/home/home'});};
 try{
 const project=JSON.parse(fs.readFileSync('miniprogram/project.config.json'));assert.equal(await m.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId),project.appid);
 const current=await m.evaluate(()=>(getApp().globalData.userInfo||{}).username);assert.ok([old.account,f.account].includes(current)||(!current&&(await m.currentPage()).path==='pages/login/login'));checked=true;if(current!==f.account)await login(f);
 let p=await m.navigateTo('/pages/profile/profile');await wait(async()=>!await p.data('busy')&&await p.data('loaded'));
 if(verify){
  assert.equal(f.approved,true);assert.equal(await p.data('form.hospitalId'),f.targetHospitalId);assert.equal(await p.data('form.nickname'),f.requestedNickname);assert.equal(await p.data('form.avatar'),f.requestedAvatar);assert.equal(await p.data('submitted'),false);assert.equal((await p.data('history'))[0].status,'approved');
  await m.screenshot({path:root+'/qa/native-profile-approved.png'});f.completed=true;fs.writeFileSync(file,JSON.stringify(f,null,2),{mode:0o600});console.log(JSON.stringify({userId:f.userId,approvedProfileNativeMatches:true,newHospitalVisible:true,historyApproved:true,originalAccountRestoredInFinally:true}));
 }else{
  assert.equal(await p.data('rejectedDraft'),true);assert.equal(await p.data('form.nickname'),f.prefix+'申请姓名');assert.equal(await p.data('form.hospitalId'),f.targetHospitalId);
  const png=fs.readFileSync('miniprogram/assets/icons/brand-mine.png'),pdf=fs.readFileSync(root+'/qa/native-profile-qualification.pdf');
  paths=await m.evaluate(data=>{const fs=wx.getFileSystemManager(),base=wx.env.USER_DATA_PATH+'/petlis-profile-qa';fs.writeFileSync(base+'.png',data.png,'base64');fs.writeFileSync(base+'.pdf',data.pdf,'base64');return {png:base+'.png',pdf:base+'.pdf'}},{png:png.toString('base64'),pdf:pdf.toString('base64')});
  await m.mockWxMethod('chooseMedia',{tempFiles:[{tempFilePath:paths.png,size:png.length}]});mediaMock=true;
  await(await p.$('button[data-action="avatar"]')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('form.avatar'));assert.equal(await p.data('error'),'');
  const before=(await p.data('files')).length;await m.mockWxMethod('chooseMessageFile',{tempFiles:[{path:paths.pdf,name:'qualification.pdf',size:pdf.length}]});fileMock=true;
  await(await p.$('button[data-action="qualification"]')).tap();await wait(async()=>!await p.data('busy')&&(await p.data('files')).length===before+1);
  const name=f.prefix+'原生修正姓名';await(await p.$('input[data-key="nickname"]')).trigger('input',{value:name});
  await m.mockWxMethod('request',{statusCode:503,data:{code:503,message:'本机资料提交失败注入'}});requestMock=true;
  await(await p.$('.bottom-actions .petlis-btn-primary')).tap();await wait(async()=>!await p.data('busy')&&!!await p.data('error'));assert.equal(await p.data('submitted'),false);assert.equal(await p.data('form.nickname'),name);assert.equal((await p.data('files')).length,before+1);
  await m.screenshot({path:root+'/qa/native-profile-submit-error.png'});await m.restoreWxMethod('request');requestMock=false;
  await(await p.$('.petlis-empty-retry')).tap();await wait(async()=>!await p.data('busy')&&await p.data('submitted')&&(await p.data('history'))[0].status==='pending');
  f.pendingChangeId=Number((await p.data('history'))[0].id);f.requestedAvatar=await p.data('form.avatar');f.requestedNickname=name;fs.writeFileSync(file,JSON.stringify(f,null,2),{mode:0o600});
  await m.screenshot({path:root+'/qa/native-profile-pending.png'});const result={userId:f.userId,changeId:f.pendingChangeId,rejectedInputsRestored:true,mediaAndFilePickersMocked:true,avatarAndQualificationUploadedToRealBackend:true,submissionFailureInjectedHTTP503:true,failedSubmitPreservesFieldsAndFiles:true,retryCreatesPendingApplication:true};fs.writeFileSync(root+'/qa/native-profile-submission.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }
 }catch(error){console.error('Native profile stage:',error.stack);throw error}finally{
 try {
 if(requestMock)await m.restoreWxMethod('request');if(mediaMock)await m.restoreWxMethod('chooseMedia');if(fileMock)await m.restoreWxMethod('chooseMessageFile');
 if(paths)await m.evaluate(paths=>{const fs=wx.getFileSystemManager();for(const path of Object.values(paths))try{fs.unlinkSync(path)}catch(e){}},paths);
 if(checked){const current=await m.evaluate(()=>(getApp().globalData.userInfo||{}).username);if(current===f.account || (!current&&(await m.currentPage()).path==='pages/login/login'))await login(old);assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),old.account);old.session=await m.evaluate(()=>({...getApp().globalData.userInfo,token:getApp().globalData.token}));fs.writeFileSync(oldFile,JSON.stringify(old,null,2),{mode:0o600});}
 }finally{await m.disconnect()}
 }
})().catch(e=>{console.error(e.stack);process.exitCode=1});
