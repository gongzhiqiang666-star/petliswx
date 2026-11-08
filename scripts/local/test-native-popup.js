const fs=require('fs'),assert=require('assert/strict'),{execFileSync}=require('child_process');
const root=process.env.HOME+'/.local/share/petlis',a=require(root+'/tools/miniprogram-qa/node_modules/miniprogram-automator'),f=JSON.parse(fs.readFileSync(root+'/qa/native-article-fixture.json')),old=JSON.parse(fs.readFileSync(root+'/simulator-fixture.json'));
(async()=>{assert.equal((await fetch('http://127.0.0.1:8082/api/auth/capabilities')).status,200);const m=await a.connect({wsEndpoint:'ws://127.0.0.1:9420'});let prepared=false,baseline;
 const wait=async fn=>{for(let i=0;i<100;i++){if(await fn())return;await new Promise(r=>setTimeout(r,200))}throw Error('Native popup readiness timeout')};
 const stage=value=>execFileSync('python3',['scripts/local/prepare-native-popup.py',value],{stdio:['ignore','pipe','pipe']});
 try{
 assert.equal(await m.evaluate(()=>(getApp().globalData.userInfo||{}).username),old.account);
 baseline=await m.evaluate(ids=>{const user=getApp().globalData.userInfo,id=Number(user.userId||user.id),keys=['article-popup-last:'+id,...ids.map(x=>'article-popup:'+id+':'+x)];return keys.map(key=>({key,value:wx.getStorageSync(key)}))},f.articleIds);
 await m.mockWxMethod('showModal',{confirm:false,cancel:true,errMsg:'showModal:ok'});
 stage('first');prepared=true;
 const state=()=>m.evaluate(ids=>{const user=getApp().globalData.userInfo,id=Number(user.userId||user.id),p=getCurrentPages().slice(-1)[0];return {first:!!wx.getStorageSync('article-popup:'+id+':'+ids[0]),second:!!wx.getStorageSync('article-popup:'+id+':'+ids[1]),last:wx.getStorageSync('article-popup-last:'+id),inFlight:p._popupInFlight}},f.articleIds);
 const check=async()=>{await m.evaluate(()=>{getCurrentPages().slice(-1)[0].checkArticlePopup();return true});await wait(async()=>!(await state()).inFlight)};
 await m.reLaunch('/pages/home/home');await wait(async()=>{const s=await state();return s.first&&!s.inFlight});const first=(await state()).last;
 await check();assert.equal((await state()).last,first);
 stage('second');await check();assert.equal((await state()).second,false);assert.equal((await state()).last,first);
 await m.evaluate(()=>{const user=getApp().globalData.userInfo;wx.setStorageSync('article-popup-last:'+Number(user.userId||user.id),Date.now()-25*3600000)});
 await check();assert.equal((await state()).second,true);assert.ok((await state()).last>first);
 const result={syntheticArticleIds:f.articleIds,systemModalMocked:true,backendAndHomeLogicReal:true,sameArticleSuppressed:true,differentArticleWithin24HoursSuppressed:true,expiredIntervalAllowsNextArticle:true,failureRecoveryTestedInNative:false,broadcastCalled:false,visualNativeModalVerified:false};
 fs.writeFileSync(root+'/qa/native-popup-validation.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{
 if(prepared)stage('restore');await m.restoreWxMethod('showModal');
 if(baseline)await m.evaluate(rows=>{for(const row of rows){if(row.value==='')wx.removeStorageSync(row.key);else wx.setStorageSync(row.key,row.value)}delete getApp().__qaPopupCalls;delete getApp().__qaPopupFail},baseline);
 await m.disconnect();
 }
})().catch(e=>{console.error(e.stack);process.exitCode=1});
