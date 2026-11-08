const test=require('node:test'),assert=require('node:assert/strict'),fs=require('fs'),vm=require('vm'),path=require('path');
const {loadPage}=require('./helpers');
test('返回资讯列表重新读取收藏状态，首屏未就绪时不重复请求',async()=>{
 const {instance:p}=loadPage('articles/articles');let loads=0;p.load=async reset=>{assert.equal(reset,true);loads++};
 assert.equal(typeof p.onShow,'function');await p.onShow();assert.equal(loads,0);p.loaded=true;await p.onShow();assert.equal(loads,1);
});
test('资讯加载中更改分类或收藏筛选不污染当前请求条件',()=>{
 const {instance:p}=loadPage('articles/articles');p.data.busy=true;p.category({detail:{value:2}});p.bookmarks();assert.equal(p.data.categoryIndex,0);assert.equal(p.data.bookmarked,false);
});
test('直播重试清除前一个倒计时且页面卸载不残留定时器',async()=>{
 let definition,created=0;const cleared=[];const article={type:'live',live_at:'2099-01-01 12:00:00'};
 vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../pages/article-detail/article-detail.js'),'utf8'),{require:()=>({get:async()=>article}),Page:x=>definition=x,setInterval:()=>++created,clearInterval:id=>cleared.push(id),Date,Number,Math});
 const p={...definition,data:{...definition.data,id:1},setData(patch){Object.assign(this.data,patch)}};await p.load();await p.load();assert.ok(cleared.includes(1));p.onUnload();assert.ok(cleared.includes(2));
});
test('已下架资讯重新加载失败时清除旧内容及倒计时',async()=>{
 const {instance:p}=loadPage('article-detail/article-detail',{get:async()=>{throw Error('记录不存在或无权访问')}});
 p.data.article={title:'旧内容'};p.data.countdown='旧倒计时';await p.load();assert.equal(p.data.article,null);assert.equal(p.data.countdown,'');assert.ok(p.data.error);
});
