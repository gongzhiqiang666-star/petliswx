const test=require('node:test'),assert=require('node:assert/strict');
const {loadPage}=require('./helpers');
test('弹窗按账号和文章去重，不同资讯遵守跨文章间隔',async()=>{
 let article={id:5,title:'活动',minimumIntervalHours:24},shown=0;
 const {instance:p,wx,storage,app}=loadPage('home/home',{get:async()=>article},{userId:7});wx.showModal=async()=>{shown++;return {confirm:false}};
 await p.checkArticlePopup();await p.checkArticlePopup();assert.equal(shown,1);assert.equal(storage['article-popup:7:5'],true);
 article={...article,id:6};await p.checkArticlePopup();assert.equal(shown,1);
 storage['article-popup-last:7']=Date.now()-25*3600000;await p.checkArticlePopup();assert.equal(shown,2);
 app.globalData.userInfo={userId:8};await p.checkArticlePopup();assert.equal(shown,3);assert.equal(storage['article-popup:8:6'],true);
});
test('弹窗显示失败不消耗频次，确认可正常进入详情',async()=>{
 const {instance:p,wx,storage}=loadPage('home/home',{get:async()=>({id:5,title:'活动'})},{userId:7});wx.showModal=async()=>{throw Error('系统弹窗失败')};
 await p.checkArticlePopup();assert.equal(storage['article-popup:7:5'],undefined);assert.equal(p._popupInFlight,false);
 let target;wx.navigateTo=x=>target=x.url;wx.showModal=async()=>({confirm:true});await p.checkArticlePopup();assert.equal(storage['article-popup:7:5'],true);assert.equal(target,'/pages/article-detail/article-detail?id=5');
});
test('离开首页或请求期间切换账号不弹出过期资讯',async()=>{
 let resolve;const {instance:p,wx,app}=loadPage('home/home',{get:()=>new Promise(r=>resolve=r)},{userId:7});let shown=0;wx.showModal=async()=>{shown++;return {confirm:false}};
 let task=p.checkArticlePopup();p.onHide();resolve({id:5});await task;assert.equal(shown,0);
 p._visible=true;task=p.checkArticlePopup();app.globalData.userInfo={userId:8};resolve({id:6});await task;assert.equal(shown,0);
});
test('并发检查只显示一个弹窗且支持服务端一小时间隔',async()=>{
 let finish,shown=0;const {instance:p,wx,storage}=loadPage('home/home',{get:async()=>({id:6,minimumIntervalHours:1})},{userId:7});storage['article-popup-last:7']=Date.now()-2*3600000;
 wx.showModal=()=>{shown++;return new Promise(r=>finish=r)};
 const first=p.checkArticlePopup();await Promise.resolve();const second=p.checkArticlePopup();assert.equal(shown,1);finish({confirm:false});await Promise.all([first,second]);assert.equal(shown,1);
});
