const test=require('node:test'),assert=require('node:assert/strict');const {loadPage}=require('./helpers');
test('资料加载失败重试只重载，不误提交空资料',async()=>{
 let posts=0,fail=true;const {instance:p}=loadPage('profile/profile',{post:async()=>posts++,get:async url=>{if(fail)throw Error('加载失败');return url==='/account'?{nickname:'医生',hospitalId:1}:[]}});
 await p.onLoad();assert.equal(p.data.loaded,false);assert.equal(typeof p.retry,'function');fail=false;await p.retry();assert.equal(posts,0);assert.equal(p.data.loaded,true);
});
test('提交已成功但历史读取失败时保持成功并只重试历史',async()=>{
 let posts=0,fail=true;const {instance:p}=loadPage('profile/profile',{post:async()=>posts++,get:async()=>{if(fail)throw Error('历史失败');return [{id:7,status:'pending'}]}});p.data.loaded=true;
 await p.submit();assert.equal(p.data.submitted,true);assert.equal(p.data.errorAction,'history');fail=false;await p.retry();await p.submit();assert.equal(posts,1);assert.equal(p.data.history.length,1);
});
test('已有待审核申请时不能重复提交',async()=>{
 let posts=0;const {instance:p}=loadPage('profile/profile',{post:async()=>posts++,get:async url=>url==='/account'?{nickname:'医生',hospitalId:1}:url==='/account/profile-changes'?[{id:7,status:'pending'}]:[]});await p.onLoad();await p.submit();assert.equal(p.data.submitted,true);assert.equal(posts,0);
});
test('被驳回资料重新进入保留上次申请内容供修改',async()=>{
 const requested={nickname:'待修正姓名',title:'职称',avatar:'avatar',hospitalId:2,qualificationFiles:['qa.pdf']};const {instance:p}=loadPage('profile/profile',{get:async url=>url==='/account'?{nickname:'已生效姓名',hospitalId:1}:url==='/account/profile-changes'?[{status:'rejected',requestedData:JSON.stringify(requested)}]:[{hospitalId:2,hospitalName:'申请医院'}]});
 await p.onLoad();assert.equal(p.data.form.nickname,'待修正姓名');assert.equal(p.data.hospitalName,'申请医院');assert.deepEqual(p.data.files,['qa.pdf']);assert.equal(p.data.submitted,false);
});
