const test=require('node:test'),assert=require('node:assert/strict');const {loadPage}=require('./helpers');
test('注册匿名上传并随申请提交凭据，成功清掉凭据且不能重复提交',async()=>{
 let uploads=0,posts=0,body;const f=loadPage('register/register',{post:async(url,data)=>{posts++;body=data}}),p=f.instance;
 f.wx.chooseMessageFile=async()=>({tempFiles:[{name:'医生.pdf',path:'tmp.pdf',size:10}]});
 f.request.uploadFile=async(...args)=>{uploads++;assert.equal(args[4],false);return {url:'/files/qualifications/qa.pdf',uploadToken:'private-once-token'}};
 await p.upload();assert.equal(uploads,1);assert.equal(p.data.files[0].uploadToken,'private-once-token');
 Object.assign(p.data,{username:'qa_doctor',password:'Password123',confirmPassword:'Password123',nickname:'医生',title:'兽医',hospital:{hospitalId:1},mobile:'19900000000',code:'123456'});
 await p.submit();assert.equal(body.qualificationTokens['/files/qualifications/qa.pdf'],'private-once-token');assert.equal(p.data.files.length,0);assert.equal(p.data.password,'');await p.submit();assert.equal(posts,1);
});
test('注册失败保留本次上传凭据供纠正重试，选择器打开期间禁止再次上传',async()=>{
 const f=loadPage('register/register',{post:async()=>{throw Error('手机号已注册')}}),p=f.instance;let resolve,chooses=0;
 f.wx.chooseMessageFile=()=>{chooses++;return new Promise(r=>resolve=r)};f.request.uploadFile=async()=>({url:'qa.pdf',uploadToken:'once'});
 const pending=p.upload();await p.upload();assert.equal(chooses,1);resolve({tempFiles:[{name:'a.pdf',path:'tmp',size:1}]});await pending;
 Object.assign(p.data,{username:'qa_doctor',password:'Password123',confirmPassword:'Password123',nickname:'医生',title:'兽医',hospital:{hospitalId:1},mobile:'19900000000',code:'123456'});await p.submit();assert.equal(p.data.files[0].uploadToken,'once');assert.equal(p.data.submitted,false);assert.match(p.data.error,/手机号/);
});
test('本人资质使用鉴权下载后的临时路径预览，失败重试只预览不提交',async()=>{
 let downloads=0,posts=0,opens=0;const f=loadPage('profile/profile',{post:async()=>posts++}),p=f.instance;p.data.files=['/files/qualifications/qa.pdf'];
 f.request.downloadFile=async()=>{downloads++;if(downloads===1)throw Error('下载失败');return 'private.tmp'};
 f.wx.openDocument=async options=>{opens++;assert.equal(options.filePath,'private.tmp');assert.equal(options.showMenu,false)};
 await p.previewQualification({currentTarget:{dataset:{index:0}}});assert.equal(p.data.errorAction,'preview');await p.retry();assert.equal(downloads,2);assert.equal(opens,1);assert.equal(posts,0);
});
